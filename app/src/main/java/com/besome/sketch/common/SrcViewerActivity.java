package com.besome.sketch.common;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import androidx.core.view.WindowInsetsCompat;

import com.besome.sketch.beans.SrcCodeBean;
import com.besome.sketch.ctrls.CommonSpinnerItem;
import com.besome.sketch.lib.base.BaseAppCompatActivity;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pro.sketchware.core.ProjectBuilder;
import pro.sketchware.core.SketchToast;
import pro.sketchware.core.ProjectDataManager;
import pro.sketchware.core.ProjectFilePaths;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.SrcViewerBinding;
import pro.sketchware.utility.CodeEditorPreferences;
import pro.sketchware.utility.EditorUtils;
import pro.sketchware.utility.UI;

public class SrcViewerActivity extends BaseAppCompatActivity {

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private SrcViewerBinding binding;
    private String sc_id;
    private ArrayList<SrcCodeBean> sourceCodeBeans;

    private String currentFileName;
    private CodeEditorPreferences editorPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = SrcViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        currentFileName = getIntent().hasExtra("current") ? getIntent().getStringExtra("current") : "";
        sc_id = savedInstanceState != null ? savedInstanceState.getString("sc_id") : getIntent().getStringExtra("sc_id");

        UI.addWindowInsetToPadding(binding.getRoot(), WindowInsetsCompat.Type.systemBars(), false, true, false, true);

        editorPrefs = new CodeEditorPreferences(this, "viewer");

        configureEditor();

        binding.changeFontSize.setOnClickListener(v -> editorPrefs.showFontSizeDialog(this, binding.editor, null));

        binding.filesListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SrcCodeBean bean = sourceCodeBeans.get(position);
                binding.editor.setText(bean.source);
                currentFileName = bean.srcFileName;
                if (currentFileName.endsWith(".xml")) {
                    EditorUtils.loadXmlConfig(binding.editor);
                } else {
                    EditorUtils.loadJavaConfig(binding.editor);
                }
                editorPrefs.applyToEditor(binding.editor, false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        showLoadingDialog(); // show loading

        backgroundExecutor.execute(() -> {
            try {
                var ProjectFilePaths = new ProjectFilePaths(getBaseContext(), sc_id);
                var fileManager = ProjectDataManager.getFileManager(sc_id);
                var dataManager = ProjectDataManager.getProjectDataManager(sc_id);
                var libraryManager = ProjectDataManager.getLibraryManager(sc_id);
                ProjectFilePaths.initializeMetadata(libraryManager, fileManager, dataManager, pro.sketchware.core.ProjectFilePaths.ExportType.SOURCE_CODE_VIEWING);
                ProjectBuilder builder = new ProjectBuilder(this, ProjectFilePaths);
                builder.buildBuiltInLibraryInformation();
                sourceCodeBeans = ProjectFilePaths.generateSourceCodeBeans(fileManager, dataManager, builder.getBuiltInLibraryManager());

                runOnUiThread(() -> {
                    if (sourceCodeBeans == null) {
                        SketchToast.warning(getApplicationContext(), Helper.getResString(R.string.common_error_unknown), SketchToast.TOAST_NORMAL).show();
                    } else {
                        binding.filesListSpinner.setAdapter(new FilesListSpinnerAdapter());
                        for (SrcCodeBean src : sourceCodeBeans) {
                            if (src.srcFileName.equals(currentFileName)) {
                                binding.filesListSpinner.setSelection(sourceCodeBeans.indexOf(src));
                                break;
                            }
                        }
                        binding.editor.setText(sourceCodeBeans.get(binding.filesListSpinner.getSelectedItemPosition()).source);
                        dismissLoadingDialog(); // hide loading
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("SrcViewerActivity", "Failed to generate source code", e);
            }
        });

    }

    private void configureEditor() {
        binding.editor.setTypefaceText(EditorUtils.getTypeface(this));
        binding.editor.setEditable(false);
        binding.editor.setPinLineNumber(true);

        if (currentFileName.endsWith(".xml")) {
            EditorUtils.loadXmlConfig(binding.editor);
        } else {
            EditorUtils.loadJavaConfig(binding.editor);
        }
        editorPrefs.applyToEditor(binding.editor, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        backgroundExecutor.shutdownNow();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        super.onSaveInstanceState(outState);
    }

    public class FilesListSpinnerAdapter extends BaseAdapter {

        private View getCustomSpinnerView(int position, View view, boolean isCurrentlyViewingFile) {
            CommonSpinnerItem spinnerItem = view != null ? (CommonSpinnerItem) view :
                    new CommonSpinnerItem(SrcViewerActivity.this);
            spinnerItem.setData(sourceCodeBeans.get(position).srcFileName, isCurrentlyViewingFile);
            return spinnerItem;
        }

        @Override
        public int getCount() {
            return sourceCodeBeans.size();
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            boolean isCheckmarkVisible = binding.filesListSpinner.getSelectedItemPosition() == position;
            return getCustomSpinnerView(position, convertView, isCheckmarkVisible);
        }

        @Override
        public Object getItem(int position) {
            return sourceCodeBeans.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomSpinnerView(position, convertView, false);
        }
    }
}