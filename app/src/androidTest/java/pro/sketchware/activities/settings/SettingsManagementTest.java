package pro.sketchware.activities.settings;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.view.View;
import android.view.inspector.WindowInspector;
import android.widget.ListView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import pro.sketchware.R;
import pro.sketchware.activities.editor.block.BlocksManager;
import pro.sketchware.activities.settings.fragments.block.selector.Selector;
import pro.sketchware.activities.settings.fragments.block.selector.details.BlockSelectorDetailsFragment;
import pro.sketchware.beans.CustomEvent;
import pro.sketchware.beans.CustomListener;
import pro.sketchware.core.codegen.EventsHandler;
import pro.sketchware.core.codegen.EventRegistry;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.CustomEventsStore;

/** Exercises settings with temporary definitions and restores the user's exact file contents. */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class SettingsManagementTest {
    private static final Gson GSON = new Gson();
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final Map<File, byte[]> originals = new LinkedHashMap<>();
    private final Map<File, Long> originalTimes = new LinkedHashMap<>();
    private File eventsFile, listenersFile, menusFile;
    private ActivityScenario<SettingsActivity> scenario;
    private ActivityScenario<AppSettings> appScenario;
    private ActivityScenario<BlocksManager> blocksScenario;

    @Before public void fixture() throws Exception {
        eventsFile = new File(SketchwarePaths.getAbsolutePathOf(SketchwarePaths.CUSTOM_EVENTS_FILE));
        listenersFile = new File(SketchwarePaths.getAbsolutePathOf(SketchwarePaths.CUSTOM_LISTENERS_FILE));
        menusFile = new File(SketchwarePaths.getBlockSelectorMenuPath());
        assertFalse("Do not interrupt pending definition recovery", new File(eventsFile.getParentFile(), "custom-events-rollback.json").exists());
        for (File file : List.of(eventsFile, listenersFile, menusFile)) {
            assertFalse(new File(file.getPath() + ".bak").exists());
            originals.put(file, file.exists() ? Files.readAllBytes(file.toPath()) : null);
            originalTimes.put(file, file.lastModified());
            if (!file.getParentFile().isDirectory()) assertTrue(file.getParentFile().mkdirs());
        }
        CustomListener a = listener("reviewA"), b = listener("reviewB");
        write(listenersFile, List.of(a, b));
        write(eventsFile, List.of(event("reviewA", "OriginalA();"), event("reviewB", "OriginalB();")));
        write(menusFile, List.of(new Selector("Builtin", "typeview", new ArrayList<>(List.of("View"))),
                new Selector("A", "reviewA", new ArrayList<>(List.of("A1"))),
                new Selector("B", "reviewB", new ArrayList<>(List.of("B1")))));
        EventsHandler.refreshCachedCustomEvents();
        EventsHandler.refreshCachedCustomListeners();
    }

    @After public void restore() throws Exception {
        if (blocksScenario != null) blocksScenario.close();
        if (appScenario != null) appScenario.close();
        if (scenario != null) scenario.close();
        for (Map.Entry<File, byte[]> entry : originals.entrySet()) {
            if (entry.getValue() == null) Files.deleteIfExists(entry.getKey().toPath());
            else {
                Files.write(entry.getKey().toPath(), entry.getValue());
                entry.getKey().setLastModified(originalTimes.get(entry.getKey()));
            }
        }
        EventsHandler.refreshCachedCustomEvents();
        EventsHandler.refreshCachedCustomListeners();
    }

    @Test public void legacyEventCollisionCannotOverwriteAnotherListener() throws Exception {
        openEventEditor("reviewB");
        scenario.onActivity(activity -> {
            ((TextView) activity.findViewById(R.id.events_creator_code)).setText("UpdatedB();");
            activity.findViewById(R.id.events_creator_save).performClick();
            assertNotNull(((TextInputLayout) activity.findViewById(R.id.events_creator_name_til)).getError());
        });
        assertEquals("OriginalA();", events().get(0).getCode());
        assertEquals("OriginalB();", events().get(1).getCode());
        scenario.onActivity(activity -> {
            ((TextView) activity.findViewById(R.id.events_creator_eventname)).setText("reviewUniqueB");
            activity.findViewById(R.id.events_creator_save).performClick();
        });
        settle();
        assertEquals("reviewA", events().get(0).getListener());
        assertEquals("OriginalA();", events().get(0).getCode());
        assertEquals("reviewB", events().get(1).getListener());
        assertEquals("UpdatedB();", events().get(1).getCode());
        assertEquals("UpdatedB();", EventsHandler.getEventCode("", "reviewUniqueB", ""));
    }

    @Test public void newEventNamesAreUniqueAcrossListeners() throws Exception {
        launch(SettingsActivity.EVENTS_MANAGER_FRAGMENT);
        row(R.id.listeners_recycler_view, 1, false);
        scenario.onActivity(activity -> {
            activity.findViewById(R.id.fab_new_event).performClick();
            activity.getSupportFragmentManager().executePendingTransactions();
            text(activity, R.id.events_creator_eventname, "reviewCollision");
            text(activity, R.id.events_creator_varname, "Button");
            text(activity, R.id.events_creator_icon, "2131165298");
            text(activity, R.id.events_creator_spec, "Review");
            text(activity, R.id.events_creator_code, "New();");
            activity.findViewById(R.id.events_creator_save).performClick();
        });
        settle();
        assertEquals(2, events().size());
        scenario.onActivity(activity -> {
            assertNotNull(((TextInputLayout) activity.findViewById(R.id.events_creator_name_til)).getError());
            text(activity, R.id.events_creator_eventname, "reviewNew");
            activity.findViewById(R.id.events_creator_save).performClick();
        });
        settle();
        assertEquals(3, events().size());
        assertEquals("New();", EventsHandler.getEventCode("", "reviewNew", ""));
    }

    @Test public void listenerRenameMigratesReferencesAndRefreshesGenerator() throws Exception {
        CustomListener independent = listener("reviewA");
        independent.setIndependentFlag("true");
        independent.setCode("//reviewA\nreviewA();");
        write(listenersFile, List.of(independent, listener("reviewB")));
        EventsHandler.refreshCachedCustomListeners();
        launch(SettingsActivity.EVENTS_MANAGER_FRAGMENT);
        row(R.id.listeners_recycler_view, 1, true);
        onMain(() -> listClick(0));
        settle();
        onMain(() -> {
            ((TextView) dialogView(R.id.listener_name)).setText("reviewB");
            dialogView(android.R.id.button1).performClick();
            assertNotNull(visibleDialogView(R.id.listener_name));
            ((TextView) dialogView(R.id.listener_name)).setText("reviewRenamed");
            dialogView(android.R.id.button1).performClick();
        });
        settle();
        assertEquals("reviewRenamed", events().get(0).getListener());
        assertEquals("reviewB", events().get(1).getListener());
        assertTrue(read(listenersFile).contains("reviewRenamed"));
        assertEquals(0, EventRegistry.getEventsForListener("reviewA").length);
        assertArrayEquals(new String[]{"reviewCollision"}, EventRegistry.getEventsForListener("reviewRenamed"));
        assertEquals("//reviewRenamed\nreviewA();", EventsHandler.getListenerCode("reviewRenamed", "", ""));
    }

    @Test public void importsRejectConflictsWithoutPartiallyAddingDefinitions() throws Exception {
        launch(SettingsActivity.EVENTS_MANAGER_FRAGMENT);
        String originalEvents = read(eventsFile), originalListeners = read(listenersFile);
        CustomEvent incoming = event("reviewImport", "Import();");
        invokeFragment("importEvents", new Class<?>[]{ArrayList.class, ArrayList.class},
                new ArrayList<>(List.of(listener("reviewImport"))), new ArrayList<>(List.of(incoming)));
        assertEquals(originalEvents, read(eventsFile));
        assertEquals(originalListeners, read(listenersFile));
        incoming.setName("reviewImportEvent");
        invokeFragment("importEvents", new Class<?>[]{ArrayList.class, ArrayList.class},
                new ArrayList<>(List.of(listener("reviewImport"))), new ArrayList<>(List.of(incoming, incoming)));
        assertEquals(originalEvents, read(eventsFile));
        assertEquals(originalListeners, read(listenersFile));
        invokeFragment("importEvents", new Class<?>[]{ArrayList.class, ArrayList.class},
                new ArrayList<>(List.of(listener("reviewImport"))), new ArrayList<>(List.of(incoming)));
        assertEquals(3, events().size());
        assertEquals("Import();", EventsHandler.getEventCode("", "reviewImportEvent", ""));
    }

    @Test public void selectorRenameChecksConflictsButAcceptsItsOwnName() throws Exception {
        launch(SettingsActivity.BLOCK_SELECTOR_MANAGER_FRAGMENT);
        row(R.id.list, 1, true);
        onMain(() -> dialogView(R.id.edit).performClick());
        settle();
        onMain(() -> {
            ((TextView) dialogView(R.id.palettesPath)).setText("REVIEWB");
            dialogView(android.R.id.button1).performClick();
            assertNotNull(((TextInputLayout) dialogView(R.id.tilPalettesPath)).getError());
        });
        assertEquals("reviewA", menus().get(1).getName());
        onMain(() -> {
            ((TextView) dialogView(R.id.palettesPath)).setText("reviewA");
            assertNull(((TextInputLayout) dialogView(R.id.tilPalettesPath)).getError());
            ((TextView) dialogView(R.id.blocksPath)).setText("Updated title");
            dialogView(android.R.id.button1).performClick();
        });
        settle();
        assertEquals("Updated title", menus().get(1).getTitle());
        assertEquals(List.of("A1"), menus().get(1).getData());
    }

    @Test public void selectorImportsValidateWholeBatchBeforeWriting() throws Exception {
        launch(SettingsActivity.BLOCK_SELECTOR_MANAGER_FRAGMENT);
        String original = read(menusFile);
        File imported = new File(context.getCacheDir(), "review-selector-import.json");
        try {
            write(imported, List.of(new Selector("New", "reviewNew", new ArrayList<>()),
                    new Selector("Conflict", "REVIEWB", new ArrayList<>())));
            invokeFragment("handleToImportFile", new Class<?>[]{File.class}, imported);
            assertEquals(original, read(menusFile));
            write(imported, List.of(new Selector("New", "reviewNew", new ArrayList<>()),
                    new Selector("Conflict", "REVIEWNEW", new ArrayList<>())));
            invokeFragment("handleToImportFile", new Class<?>[]{File.class}, imported);
            assertEquals(original, read(menusFile));
            write(imported, new Selector("New", "reviewNew", new ArrayList<>(List.of("RawEnglish"))));
            invokeFragment("handleToImportFile", new Class<?>[]{File.class}, imported);
            assertEquals(4, menus().size());
            assertEquals(List.of("RawEnglish"), menus().get(3).getData());
        } finally { Files.deleteIfExists(imported.toPath()); }
    }

    @Test public void invalidInputKeepsListenerSelectorAndSigningDialogsOpen() {
        launch(SettingsActivity.EVENTS_MANAGER_FRAGMENT);
        scenario.onActivity(activity -> activity.findViewById(R.id.fab_new_listener).performClick());
        settle();
        onMain(() -> {
            ((TextView) dialogView(R.id.listener_code)).setText("UnsavedCode();");
            dialogView(android.R.id.button1).performClick();
        });
        settle();
        onMain(() -> assertEquals("UnsavedCode();", ((TextView) dialogView(R.id.listener_code)).getText().toString()));
        scenario.close(); scenario = null;
        launch(SettingsActivity.BLOCK_SELECTOR_MANAGER_FRAGMENT);
        scenario.onActivity(activity -> activity.findViewById(R.id.create_new).performClick());
        settle();
        onMain(() -> {
            ((TextView) dialogView(R.id.palettesPath)).setText("reviewNew");
            dialogView(android.R.id.button1).performClick();
        });
        settle();
        onMain(() -> assertEquals("reviewNew", ((TextView) dialogView(R.id.palettesPath)).getText().toString()));
        scenario.close(); scenario = null;
        appScenario = ActivityScenario.launch(new Intent(context, AppSettings.class));
        appScenario.onActivity(activity -> invoke(activity, "signApkFileDialog", new Class<?>[]{}));
        settle();
        onMain(() -> dialogView(android.R.id.button1).performClick());
        settle();
        onMain(() -> assertNotNull(dialogView(R.id.select_file)));
    }

    @Test public void invalidPaletteColorKeepsDraftForCorrection() {
        blocksScenario = ActivityScenario.launch(new Intent(context, BlocksManager.class));
        blocksScenario.onActivity(activity -> invoke(activity, "showPaletteDialog",
                new Class<?>[]{boolean.class, Integer.class, String.class, String.class, Integer.class},
                false, null, null, null, null));
        settle();
        onMain(() -> {
            dialogView(android.R.id.button1).performClick();
            assertNotNull(((TextInputLayout) dialogView(R.id.nameEditText).getParent().getParent()).getError());
            ((TextView) dialogView(R.id.nameEditText)).setText("Review palette");
            ((TextView) dialogView(R.id.colorEditText)).setText("1");
            dialogView(android.R.id.button1).performClick();
            assertNotNull(((TextInputLayout) dialogView(R.id.colorEditText).getParent().getParent()).getError());
            assertEquals("Review palette", ((TextView) dialogView(R.id.nameEditText)).getText().toString());
            dialogView(android.R.id.button2).performClick();
        });
    }

    @Test public void eventEditorRecreationPreservesDraftAndNavigation() {
        openEventEditor("reviewA");
        scenario.onActivity(activity -> {
            text(activity, R.id.events_creator_desc, "Pending description");
            text(activity, R.id.events_creator_code, "PendingCode();");
            text(activity, R.id.events_creator_eventname, "PendingName");
        });
        scenario.recreate();
        settle();
        scenario.onActivity(activity -> {
            assertEquals("EventsManagerCreatorFragment", activity.getSupportFragmentManager()
                    .findFragmentById(R.id.settings_fragment_container).getClass().getSimpleName());
            assertEquals("Pending description", ((TextView) activity.findViewById(R.id.events_creator_desc)).getText().toString());
            assertEquals("PendingCode();", ((TextView) activity.findViewById(R.id.events_creator_code)).getText().toString());
            assertEquals("PendingName", ((TextView) activity.findViewById(R.id.events_creator_eventname)).getText().toString());
            assertEquals(2, activity.getSupportFragmentManager().getBackStackEntryCount());
            activity.getSupportFragmentManager().popBackStackImmediate();
            assertEquals("EventsManagerDetailsFragment", activity.getSupportFragmentManager()
                    .findFragmentById(R.id.settings_fragment_container).getClass().getSimpleName());
        });
    }

    @Test public void selectorBundleAndDetailsSurviveRecreation() {
        ArrayList<Selector> original = new ArrayList<>(List.of(new Selector("A", "reviewMenu", new ArrayList<>(List.of("item")))));
        Bundle args = BlockSelectorDetailsFragment.newInstance(0, original).getArguments();
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeBundle(args);
            byte[] bytes = parcel.marshall();
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            Bundle restored = parcel.readBundle(Selector.class.getClassLoader());
            assertEquals(0, restored.getInt("index", -1));
            assertEquals(original, restored.getParcelableArrayList("selectors"));
        } finally { parcel.recycle(); }
        launch(SettingsActivity.BLOCK_SELECTOR_MANAGER_FRAGMENT);
        row(R.id.list, 1, false);
        scenario.recreate();
        settle();
        scenario.onActivity(activity -> {
            Fragment current = activity.getSupportFragmentManager().findFragmentById(R.id.settings_fragment_container);
            assertEquals("BlockSelectorDetailsFragment", current.getClass().getSimpleName());
            assertEquals("reviewA", ((com.google.android.material.appbar.MaterialToolbar) activity.findViewById(R.id.toolbar)).getTitle());
            assertEquals(1, ((RecyclerView) activity.findViewById(R.id.list)).getAdapter().getItemCount());
            activity.findViewById(R.id.create_new).performClick();
        });
        settle();
        onMain(() -> {
            dialogView(android.R.id.button1).performClick();
            assertNotNull(((TextInputLayout) dialogView(R.id.activity_name_input_layout)).getError());
            ((TextView) dialogView(R.id.activity_name_input)).setText("RawEnglish");
            dialogView(android.R.id.button1).performClick();
        });
        settle();
        try { assertEquals(List.of("A1", "RawEnglish"), menus().get(1).getData()); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    @Test public void invalidSelectorDefinitionsCannotOverwriteExistingMenus() throws Exception {
        launch(SettingsActivity.BLOCK_SELECTOR_MANAGER_FRAGMENT);
        String original = read(menusFile);
        File imported = new File(context.getCacheDir(), "review-invalid-selector.json");
        try {
            Files.write(imported.toPath(), "{\"name\":\"reviewImport\",\"title\":\"Title\",\"data\":null}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            invokeFragment("handleToImportFile", new Class<?>[]{File.class}, imported);
            assertEquals(original, read(menusFile));
            Files.write(menusFile.toPath(), "[{\"name\":null,\"title\":\"Title\",\"data\":[]}]".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String invalid = read(menusFile);
            write(imported, new Selector("New", "reviewNew", new ArrayList<>()));
            invokeFragment("handleToImportFile", new Class<?>[]{File.class}, imported);
            assertEquals(invalid, read(menusFile));
        } finally { Files.deleteIfExists(imported.toPath()); }
    }

    @Test public void unreadableEventFileIsNeverOverwritten() throws Exception {
        openEventEditor("reviewA");
        Files.write(eventsFile.toPath(), "broken-json".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        scenario.onActivity(activity -> {
            text(activity, R.id.events_creator_eventname, "reviewUnique");
            activity.findViewById(R.id.events_creator_save).performClick();
            assertNotNull(activity.findViewById(R.id.events_creator_save));
        });
        assertEquals("broken-json", read(eventsFile));
    }

    @Test public void iconChooserHasAccessibleLabelAndTouchTarget() {
        openEventEditor("reviewA");
        scenario.onActivity(activity -> {
            View icon = activity.findViewById(R.id.events_creator_chooseicon);
            float density = activity.getResources().getDisplayMetrics().density;
            assertTrue(icon.getWidth() / density >= 48);
            assertTrue(icon.getHeight() / density >= 48);
            assertEquals(activity.getString(R.string.dialog_title_select_icon), icon.getContentDescription());
            assertTrue(icon.isFocusable());
            assertTrue(icon.isClickable());
        });
    }

    private static CustomListener listener(String name) {
        CustomListener listener = new CustomListener();
        listener.setName(name); listener.setCode(name + "();");
        return listener;
    }
    private static void text(android.app.Activity activity, int id, String value) {
        ((TextView) activity.findViewById(id)).setText(value);
    }
    private void invokeFragment(String method, Class<?>[] types, Object... args) {
        scenario.onActivity(activity -> invoke(activity.getSupportFragmentManager()
                .findFragmentById(R.id.settings_fragment_container), method, types, args));
        settle();
    }
    private static void invoke(Object target, String name, Class<?>[] types, Object... args) {
        try {
            var method = target.getClass().getDeclaredMethod(name, types);
            method.setAccessible(true); method.invoke(target, args);
        } catch (Exception e) { throw new AssertionError(e); }
    }

    private CustomEvent event(String listener, String code) {
        CustomEvent event = new CustomEvent();
        event.setName("reviewCollision"); event.setListener(listener); event.setVar("Button");
        event.setIcon("2131165298"); event.setHeaderSpec("on review"); event.setCode(code);
        return event;
    }

    private void launch(String tag) {
        scenario = ActivityScenario.launch(new Intent(context, SettingsActivity.class)
                .putExtra(SettingsActivity.FRAGMENT_TAG_EXTRA, tag));
        settle();
    }

    private void openEventEditor(String listener) {
        launch(SettingsActivity.EVENTS_MANAGER_FRAGMENT);
        row(R.id.listeners_recycler_view, "reviewB".equals(listener) ? 0 : 1, false);
        row(R.id.events_recycler_view, 0, false);
        scenario.onActivity(activity -> assertNotNull(activity.findViewById(R.id.events_creator_save)));
    }

    private void row(int id, int position, boolean longClick) {
        settle();
        scenario.onActivity(activity -> {
            RecyclerView list = activity.findViewById(id);
            assertNotNull(list);
            RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(position);
            assertNotNull(holder);
            if (longClick) holder.itemView.performLongClick(); else holder.itemView.performClick();
            activity.getSupportFragmentManager().executePendingTransactions();
        });
        settle();
    }

    private List<CustomEvent> events() throws Exception {
        return GSON.fromJson(read(eventsFile), new TypeToken<List<CustomEvent>>(){}.getType());
    }
    private List<Selector> menus() throws Exception {
        return GSON.fromJson(read(menusFile), new TypeToken<List<Selector>>(){}.getType());
    }
    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
    }
    private static void write(File file, Object data) throws Exception {
        Files.write(file.toPath(), GSON.toJson(data).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    private static View visibleDialogView(int id) {
        for (View root : WindowInspector.getGlobalWindowViews()) {
            View view = root.findViewById(id);
            if (view != null && view.isShown()) return view;
        }
        return null;
    }
    private static View dialogView(int id) {
        View view = visibleDialogView(id);
        assertNotNull("Dialog control " + id, view);
        return view;
    }
    private static void listClick(int position) {
        ListView list = (ListView) dialogView(androidx.appcompat.R.id.select_dialog_listview);
        list.performItemClick(list.getChildAt(position - list.getFirstVisiblePosition()),
                position, list.getAdapter().getItemId(position));
    }
    private static void settle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    private static void onMain(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }
}
