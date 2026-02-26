package pro.sketchware.activities.editor.view;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import com.besome.sketch.beans.HistoryViewBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pro.sketchware.core.ViewHistoryManager;
import pro.sketchware.core.ProjectDataManager;
import io.github.rosemoe.sora.widget.CodeEditor;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.activities.appcompat.ManageAppCompatActivity;
import pro.sketchware.activities.preview.LayoutPreviewActivity;
import pro.sketchware.databinding.ViewCodeEditorBinding;
import pro.sketchware.managers.inject.InjectRootLayoutManager;
import pro.sketchware.tools.ViewBeanParser;
import pro.sketchware.utility.CodeEditorPreferences;
import pro.sketchware.utility.EditorUtils;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.relativelayout.CircularDependencyDetector;

public class ViewCodeEditorActivity extends BaseAppCompatActivity {
    private ViewCodeEditorBinding binding;
    private CodeEditor editor;

    private SharedPreferences prefs;

    private String sc_id;

    private String content;

    private boolean isEdited = false;

    private ProjectFileBean projectFile;
    private ProjectLibraryBean projectLibrary;

    private InjectRootLayoutManager rootLayoutManager;

    private CodeEditorPreferences editorPrefs;

    private static final int MENU_UNDO = 0, MENU_REDO = 1, MENU_SAVE = 2,
            MENU_EDIT_APPCOMPAT = 3, MENU_RELOAD_COLORS = 4, MENU_LAYOUT_PREVIEW = 5,
            MENU_FIND_REPLACE = 6, MENU_WORD_WRAP = 7, MENU_FONT_SIZE = 8,
            MENU_LINE_NUMBERS = 9;

    private final OnBackPressedCallback onBackPressedCallback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (isContentModified()) {
                        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(ViewCodeEditorActivity.this);
                        dialog.setIcon(R.drawable.ic_warning_96dp);
                        dialog.setTitle(Helper.getResString(R.string.common_word_warning));
                        dialog.setMessage(Helper.getResString(
                                R.string
                                        .src_code_editor_unsaved_changes_dialog_warning_message));

                        dialog.setPositiveButton(Helper.getResString(R.string.common_word_exit), (v, which) -> {
                            v.dismiss();
                            exitWithEditedContent();
                            finish();
                        });

                        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel),
                                null);
                        dialog.show();
                    } else {
                        if (isEdited) {
                            exitWithEditedContent();
                            finish();
                            return;
                        }
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = ViewCodeEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        prefs = getSharedPreferences("dce", Activity.MODE_PRIVATE);
        if (savedInstanceState == null) {
            sc_id = getIntent().getStringExtra("sc_id");
        } else {
            sc_id = savedInstanceState.getString("sc_id");
        }
        rootLayoutManager = new InjectRootLayoutManager(sc_id);
        String title = getIntent().getStringExtra("title");
        projectFile = ProjectDataManager.getFileManager(sc_id).getFileByXmlName(title);
        projectLibrary = ProjectDataManager.getLibraryManager(sc_id).getCompat();
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(R.string.view_code_editor_title);
        getSupportActionBar().setSubtitle(title);
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (onBackPressedCallback.isEnabled()) {
                onBackPressedCallback.handleOnBackPressed();
            }
        });
        content = getIntent().getStringExtra("content");
        editor = binding.editor;
        editor.setTypefaceText(EditorUtils.getTypeface(this));
        editor.setText(content);
        EditorUtils.loadXmlConfig(editor);
        editorPrefs = new CodeEditorPreferences(this, "vce");
        editorPrefs.applyToEditor(editor, false);
        if (projectFile.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY
                && projectLibrary.isEnabled()) {
            setNote("Use AppCompat Manager to modify attributes for CoordinatorLayout, Toolbar, and other appcompat layout/widget.");
        }
        binding.close.setOnClickListener(v -> {
            prefs.edit().putInt("note_" + sc_id, 1).apply();
            setNote(null);
        });
        binding.noteCard.setOnClickListener(v -> toAppCompat());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_UNDO, Menu.NONE, Helper.getResString(R.string.menu_undo))
                .setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_mtrl_undo))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MENU_REDO, Menu.NONE, Helper.getResString(R.string.menu_redo))
                .setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_mtrl_redo))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MENU_SAVE, Menu.NONE, Helper.getResString(R.string.menu_save))
                .setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_mtrl_save))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        if (projectFile.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY
                && projectLibrary.isEnabled()) {
            menu.add(Menu.NONE, MENU_EDIT_APPCOMPAT, Menu.NONE, Helper.getResString(R.string.menu_edit_appcompat));
        }
        menu.add(Menu.NONE, MENU_RELOAD_COLORS, Menu.NONE, Helper.getResString(R.string.menu_reload_colors));
        menu.add(Menu.NONE, MENU_LAYOUT_PREVIEW, Menu.NONE, Helper.getResString(R.string.menu_layout_preview));
        menu.add(Menu.NONE, MENU_FIND_REPLACE, Menu.NONE, Helper.getResString(R.string.code_editor_menu_find_replace));
        menu.add(Menu.NONE, MENU_WORD_WRAP, Menu.NONE, Helper.getResString(R.string.code_editor_menu_word_wrap))
                .setCheckable(true).setChecked(editorPrefs.getWordWrap());
        menu.add(Menu.NONE, MENU_FONT_SIZE, Menu.NONE, Helper.getResString(R.string.code_editor_menu_font_size));
        menu.add(Menu.NONE, MENU_LINE_NUMBERS, Menu.NONE, Helper.getResString(R.string.code_editor_menu_line_numbers))
                .setCheckable(true).setChecked(editorPrefs.getLineNumbers());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case MENU_UNDO -> {
                editor.undo();
                return true;
            }
            case MENU_REDO -> {
                editor.redo();
                return true;
            }
            case MENU_SAVE -> {
                save();
                return true;
            }
            case MENU_EDIT_APPCOMPAT -> {
                toAppCompat();
                return true;
            }
            case MENU_RELOAD_COLORS -> {
                EditorUtils.loadXmlConfig(binding.editor);
                editorPrefs.applyToEditor(editor, false);
                return true;
            }
            case MENU_LAYOUT_PREVIEW -> {
                toLayoutPreview();
                return true;
            }
            case MENU_FIND_REPLACE -> {
                editor.getSearcher().stopSearch();
                editor.beginSearchMode();
                return true;
            }
            case MENU_WORD_WRAP -> {
                item.setChecked(!item.isChecked());
                editor.setWordwrap(item.isChecked());
                editorPrefs.setWordWrap(item.isChecked());
                return true;
            }
            case MENU_FONT_SIZE -> {
                editorPrefs.showFontSizeDialog(this, editor, null);
                return true;
            }
            case MENU_LINE_NUMBERS -> {
                item.setChecked(!item.isChecked());
                editor.setLineNumberEnabled(item.isChecked());
                editorPrefs.setLineNumbers(item.isChecked());
                return true;
            }
            default -> {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    private void toAppCompat() {
        var intent = new Intent(getApplicationContext(), ManageAppCompatActivity.class);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("file_name", getIntent().getStringExtra("title"));
        startActivity(intent);
    }

    private void toLayoutPreview() {
        var intent = new Intent(getApplicationContext(), LayoutPreviewActivity.class);
        intent.putExtras(getIntent());
        intent.putExtra("xml", editor.getText().toString());
        startActivity(intent);
    }

    private void setNote(String note) {
        if (prefs.getInt("note_" + sc_id, 0) < 1 && (note != null && !note.isEmpty())) {
            binding.noteCard.setVisibility(View.VISIBLE);
        } else {
            binding.noteCard.setVisibility(View.GONE);
            return;
        }
        binding.noteCard.setVisibility(View.VISIBLE);
        binding.note.setText(note);
        binding.note.setSelected(true);
    }

    private void save() {
        try {
            if (isContentModified()) {
                // Parse content to validate circular dependencies
                var parser = new ViewBeanParser(editor.getText().toString());
                parser.setSkipRoot(true);

                var parsedLayout = parser.parse();
                for (ViewBean viewBean : parsedLayout) {
                    CircularDependencyDetector detector = new CircularDependencyDetector(parsedLayout, viewBean);
                    for (String attr : viewBean.parentAttributes.keySet()) {
                        String targetId = viewBean.parentAttributes.get(attr);
                        if (!detector.isLegalAttribute(targetId, attr)) {
                            SketchwareUtil.toastError(String.format(Helper.getResString(R.string.error_circular_dependency), viewBean.name));
                            return;
                        }
                    }
                }

                // Update content only after validation
                content = editor.getText().toString();
                if (!isEdited) {
                    isEdited = true;
                }
                SketchwareUtil.toast(Helper.getResString(R.string.common_word_saved));
            } else {
                SketchwareUtil.toast(Helper.getResString(R.string.toast_no_changes));
            }
        } catch (Exception e) {
            SketchwareUtil.toastError(e.toString());
        }

    }

    private boolean isContentModified() {
        return !content.equals(editor.getText().toString());
    }

    @Override
    public void onStop() {
        super.onStop();
        editorPrefs.saveTextSizeFromEditor(editor, getResources().getDisplayMetrics().scaledDensity);
    }

    private void exitWithEditedContent() {
        String filename = getIntent().getStringExtra("title");
        try {
            var parser = new ViewBeanParser(content);
            parser.setSkipRoot(true);
            var parsedLayout = parser.parse();
            var root = parser.getRootAttributes();
            rootLayoutManager.set(filename, InjectRootLayoutManager.toRoot(root));
            HistoryViewBean bean = new HistoryViewBean();
            bean.actionOverride(parsedLayout, ProjectDataManager.getProjectDataManager(sc_id).getViews(filename));
            var viewHistoryMgr = ViewHistoryManager.getInstance(sc_id);
            if (!viewHistoryMgr.historyMap.containsKey(filename)) {
                viewHistoryMgr.initHistory(filename);
            }
            viewHistoryMgr.trimFutureHistory(filename);
            viewHistoryMgr.addHistoryEntry(filename, bean);
            // Replace the view beans with the parsed layout
            ProjectDataManager.getProjectDataManager(sc_id).viewMap.put(filename, parsedLayout);
            setResult(RESULT_OK);
        } catch (Exception e) {
            SketchwareUtil.toastError(e.toString());
        }
    }
}
