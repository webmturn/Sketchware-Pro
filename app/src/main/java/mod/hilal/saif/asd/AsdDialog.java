package mod.hilal.saif.asd;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;

import com.besome.sketch.editor.LogicEditorActivity;

import pro.sketchware.core.ComponentCodeGenerator;
import pro.sketchware.core.FieldBlockView;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.CodeEditorHsAsdBinding;
import pro.sketchware.utility.CodeEditorPreferences;
import pro.sketchware.utility.EditorUtils;
import pro.sketchware.utility.SketchwareUtil;

public class AsdDialog extends Dialog implements DialogInterface.OnDismissListener {
    private CodeEditorPreferences editorPrefs;
    private Activity act;
    private CodeEditorHsAsdBinding binding;
    private String content;

    public AsdDialog(Activity activity) {
        super(activity);
        act = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = CodeEditorHsAsdBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        binding.editor.setTypefaceText(EditorUtils.getTypeface(act));
        binding.editor.setText(content);

        EditorUtils.loadJavaConfig(binding.editor);
        editorPrefs = new CodeEditorPreferences(act, "dlg");
        editorPrefs.applyToEditor(binding.editor, false);

        Menu menu = binding.toolbar.getMenu();
        MenuItem itemWordwrap = menu.findItem(R.id.action_word_wrap);
        MenuItem itemAutocomplete = menu.findItem(R.id.action_autocomplete);
        MenuItem itemAutocompleteSymbolPair = menu.findItem(R.id.action_autocomplete_symbol_pair);

        itemWordwrap.setChecked(editorPrefs.getWordWrap());
        itemAutocomplete.setChecked(editorPrefs.getAutoComplete());
        itemAutocompleteSymbolPair.setChecked(editorPrefs.getSymbolPair());

        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_undo) {
                binding.editor.undo();
            } else if (id == R.id.action_redo) {
                binding.editor.redo();
            } else if (id == R.id.action_pretty_print) {
                StringBuilder sb = new StringBuilder();
                String[] split = binding.editor.getText().toString().split("\n");
                for (String s : split) {
                    String trim = (s + "X").trim();
                    sb.append(trim.substring(0, trim.length() - 1));
                    sb.append("\n");
                }
                boolean failed = false;
                String code = sb.toString();
                try {
                    code = ComponentCodeGenerator.formatCode(code, true);
                } catch (Exception e) {
                    failed = true;
                    SketchwareUtil.toastError(Helper.getResString(R.string.error_incorrect_parentheses));
                }
                if (!failed) {
                    binding.editor.setText(code);
                }
            } else if (id == R.id.action_word_wrap) {
                item.setChecked(!item.isChecked());
                binding.editor.setWordwrap(item.isChecked());
                editorPrefs.setWordWrap(item.isChecked());
            } else if (id == R.id.action_autocomplete_symbol_pair) {
                item.setChecked(!item.isChecked());
                binding.editor.getProps().symbolPairAutoCompletion = item.isChecked();
                editorPrefs.setSymbolPair(item.isChecked());
            } else if (id == R.id.action_autocomplete) {
                item.setChecked(!item.isChecked());
                binding.editor.getComponent(EditorAutoCompletion.class).setEnabled(item.isChecked());
                editorPrefs.setAutoComplete(item.isChecked());
            } else if (id == R.id.action_paste) {
                binding.editor.pasteText();
            } else if (id == R.id.action_find_replace) {
                binding.editor.getSearcher().stopSearch();
                binding.editor.beginSearchMode();
            }
            return true;
        });

        setOnDismissListener(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        editorPrefs.saveTextSizeFromEditor(binding.editor, act.getResources().getDisplayMetrics().scaledDensity);
        editorPrefs = null;
        act = null;
    }

    public void setOnSaveClickListener(LogicEditorActivity logicEditorActivity, boolean enteringNumber, FieldBlockView ss, AsdDialog asdDialog) {
        binding.btnSave.setOnClickListener(new AsdHandlerCodeEditor(logicEditorActivity, enteringNumber, ss, asdDialog, binding.editor));
    }

    public void setOnCancelClickListener(AsdDialog asdDialog) {
        binding.btnCancel.setOnClickListener(Helper.getDialogDismissListener(asdDialog));
    }

    public void setContent(String content) {
        this.content = content;
    }
}