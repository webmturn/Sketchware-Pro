package pro.sketchware.util.locale;

import static org.junit.Assert.*;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.appbar.MaterialToolbar;
import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import dev.pranav.filepicker.FilePickerOptions;
import dev.pranav.filepicker.SelectionMode;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.R;
import pro.sketchware.activities.editor.manage.image.AddImageActivity;
import pro.sketchware.activities.resourceseditor.components.adapters.ArraysAdapter;
import pro.sketchware.activities.resourceseditor.components.fragments.ArraysEditor;
import pro.sketchware.activities.resourceseditor.components.fragments.ArraysEditor.ARRAYS_TYPES;
import pro.sketchware.activities.resourceseditor.components.models.ArrayModel;
import pro.sketchware.activities.resourceseditor.components.utils.ArraysEditorManager;
import pro.sketchware.beans.ImageBean;
import pro.sketchware.databinding.ArraysEditorAddBinding;
import pro.sketchware.util.apk.ApkSignatures;
import pro.sketchware.util.apk.ApkUtils;

/** Verifies dependency UI resources and display labels alongside their original data. */
@RunWith(AndroidJUnit4.class)
public class RemainingDisplayLocalizationTest {
    private Context context(Locale locale) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(locale);
        return new ContextThemeWrapper(base.createConfigurationContext(configuration), R.style.Theme_SketchwarePro);
    }

    @Test public void filePickerUsesChineseForEveryModeAndKeepsFilePaths() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context base = instrumentation.getTargetContext();
        File fixture = new File(base.getCacheDir(), "FilePickerI18n." + UUID.randomUUID());
        assertTrue(fixture.mkdir());
        File file = new File(fixture, "Select File.txt");
        assertTrue(file.createNewFile());
        LocaleListCompat originalLocales = AppCompatDelegate.getApplicationLocales();
        AddImageActivity activity = null;
        try {
            onMain(() -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN")));
            Intent intent = new Intent(base, AddImageActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("dir_path", fixture.getAbsolutePath())
                    .putParcelableArrayListExtra("images", new ArrayList<ImageBean>());
            activity = (AddImageActivity) instrumentation.startActivitySync(intent);
            instrumentation.waitForIdleSync();
            AddImageActivity target = activity;
            onMain(() -> {
                for (SelectionMode mode : new SelectionMode[]{SelectionMode.FOLDER, SelectionMode.BOTH, SelectionMode.FILE}) {
                    FilePickerOptions options = new FilePickerOptions();
                    options.setSelectionMode(mode);
                    options.setInitialDirectory(fixture.getAbsolutePath());
                    options.setMultipleSelection(true);
                    AtomicReference<List<? extends File>> selected = new AtomicReference<>();
                    FilePickerDialogFragment picker = new FilePickerDialogFragment(options, new FilePickerCallback() {
                        @Override public void onFilesSelected(@NotNull List<? extends File> files) { selected.set(files); }
                    });
                    picker.showNow(target.getSupportFragmentManager(), "i18n_picker");
                    View root = picker.requireView();
                    String expected = mode == SelectionMode.FILE ? "选择文件" : mode == SelectionMode.FOLDER ? "选择文件夹" : "选择项目";
                    assertEquals(expected, ((TextView) root.findViewById(dev.pranav.filepicker.R.id.select)).getText().toString());
                    assertEquals("取消", ((TextView) root.findViewById(dev.pranav.filepicker.R.id.cancel)).getText().toString());
                    MaterialToolbar toolbar = root.findViewById(dev.pranav.filepicker.R.id.toolbar);
                    assertEquals(expected, toolbar.getTitle().toString());
                    assertEquals(fixture.getAbsolutePath(), toolbar.getSubtitle().toString());
                    String[] sortLabels = {"名称（A 到 Z）", "名称（Z 到 A）", "大小（从小到大）", "大小（从大到小）", "日期（从旧到新）", "日期（从新到旧）"};
                    assertEquals(sortLabels.length, toolbar.getMenu().size());
                    for (int i = 0; i < sortLabels.length; i++) assertEquals(sortLabels[i], toolbar.getMenu().getItem(i).getTitle().toString());
                    if (mode == SelectionMode.FILE) {
                        RecyclerView list = root.findViewById(dev.pranav.filepicker.R.id.files);
                        View row = null;
                        for (int i = 0; i < list.getAdapter().getItemCount(); i++) {
                            View candidate = bind(list.getAdapter(), list, i);
                            if (file.getName().equals(((TextView) candidate.findViewById(dev.pranav.filepicker.R.id.name)).getText().toString())) row = candidate;
                        }
                        assertNotNull("Fixture filename must remain unchanged", row);
                        row.findViewById(dev.pranav.filepicker.R.id.checkbox).performClick();
                        root.findViewById(dev.pranav.filepicker.R.id.select).performClick();
                        assertNotNull(selected.get());
                        assertEquals(List.of(file), selected.get());
                        target.getSupportFragmentManager().executePendingTransactions();
                    } else {
                        picker.dismissNow();
                    }
                }
                assertEquals("Select File", context(Locale.ENGLISH).getString(dev.pranav.filepicker.R.string.select_file));
            });
        } finally {
            if (activity != null) {
                AddImageActivity target = activity;
                onMain(target::finish);
                instrumentation.waitForIdleSync();
            }
            onMain(() -> AppCompatDelegate.setApplicationLocales(originalLocales));
            assertTrue(file.delete());
            assertTrue(fixture.delete());
        }
    }

    @Test public void arrayLabelsKeepTheirEnumsAndXmlTypes() {
        onMain(() -> {
            Context chinese = context(Locale.SIMPLIFIED_CHINESE);
            Method configure = ArraysEditor.class.getDeclaredMethod("configureArrayTypePicker", Context.class, ArraysEditorAddBinding.class, ARRAYS_TYPES.class);
            configure.setAccessible(true);
            String[] labels = {"字符串", "整数", "通用数组"};
            String[] xmlTags = {"string-array", "integer-array", "array"};
            for (ARRAYS_TYPES type : ARRAYS_TYPES.values()) {
                ArraysEditorAddBinding binding = ArraysEditorAddBinding.inflate(LayoutInflater.from(chinese));
                configure.invoke(null, chinese, binding, type);
                assertSame(type, binding.arrayType.getTag());
                assertEquals(labels[type.ordinal()], binding.arrayType.getText().toString());
                assertEquals(type.name(), ArraysEditor.getArrayTypeLabel(context(Locale.ENGLISH), type));
                ArrayModel array = new ArrayModel("Select_File", (ARRAYS_TYPES) binding.arrayType.getTag());
                ArrayList<ArrayModel> arrays = new ArrayList<>(List.of(array));
                ArraysEditorManager manager = new ArraysEditorManager();
                String xml = manager.convertArraysToXML(arrays, new HashMap<>());
                assertTrue(xml.contains("<" + xmlTags[type.ordinal()] + " name=\"Select_File\">"));
                assertEquals(type, manager.parseArraysFile(xml).get(0).getArrayType());
                RecyclerView list = new RecyclerView(chinese);
                list.setLayoutManager(new LinearLayoutManager(chinese));
                View row = bind(new ArraysAdapter(arrays, new ArraysEditor(), new HashMap<>()), list, 0);
                assertEquals(labels[type.ordinal()], ((TextView) row.findViewById(R.id.sub)).getText().toString());
                assertEquals("Select_File", ((TextView) row.findViewById(R.id.title)).getText().toString());
            }
            ArraysEditorAddBinding unselected = ArraysEditorAddBinding.inflate(LayoutInflater.from(chinese));
            configure.invoke(null, chinese, unselected, null);
            assertNull(unselected.arrayType.getTag());
            assertEquals("", unselected.arrayType.getText().toString());
        });
    }

    @Test public void apkDigestErrorsUseTheRequestedLocale() throws Exception {
        Field path = ApkUtils.class.getDeclaredField("apkPath");
        path.setAccessible(true);
        Object originalPath = path.get(null);
        try {
            ApkSignatures chinese = new ApkSignatures(context(Locale.SIMPLIFIED_CHINESE), "/missing/i18n-test.apk");
            assertEquals("未找到 SHA-1 摘要。", chinese.getSha1());
            assertEquals("未找到 SHA-256 摘要。", chinese.getSha256());
            Field message = ApkSignatures.class.getDeclaredField("abMsg");
            message.setAccessible(true);
            assertEquals("计算摘要前需要指定 APK 路径。", message.get(new ApkSignatures(context(Locale.SIMPLIFIED_CHINESE), "")));
            assertEquals("APK path must be set before computing signature.", message.get(new ApkSignatures(context(Locale.ENGLISH), "")));
        } finally {
            path.set(null, originalPath);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static View bind(RecyclerView.Adapter adapter, RecyclerView parent, int position) {
        RecyclerView.ViewHolder holder = adapter.createViewHolder(parent, adapter.getItemViewType(position));
        adapter.bindViewHolder(holder, position);
        return holder.itemView;
    }

    private interface Check { void run() throws Exception; }
    private static void onMain(Check check) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try { check.run(); } catch (Throwable error) { failure.set(error); }
        });
        Throwable error = failure.get();
        if (error instanceof Error) throw (Error) error;
        if (error != null) throw new AssertionError(error);
    }
}
