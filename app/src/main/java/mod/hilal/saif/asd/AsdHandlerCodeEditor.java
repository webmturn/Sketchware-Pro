package mod.hilal.saif.asd;


import android.util.Log;
import android.view.View;

import com.besome.sketch.editor.LogicEditorActivity;

import pro.sketchware.core.FieldBlockView;
import io.github.rosemoe.sora.widget.CodeEditor;

public class AsdHandlerCodeEditor implements View.OnClickListener {

    private final boolean enteringNumber;
    private final AsdDialog asdDialog;
    private final LogicEditorActivity logicEditorActivity;
    private final CodeEditor codeEditor;
    private final FieldBlockView fieldBlockView;

    public AsdHandlerCodeEditor(LogicEditorActivity logicEditorActivity, boolean enteringNumber, FieldBlockView fieldBlockView, AsdDialog asdDialog, CodeEditor codeEditor) {
        this.logicEditorActivity = logicEditorActivity;
        this.enteringNumber = enteringNumber;
        this.fieldBlockView = fieldBlockView;
        this.asdDialog = asdDialog;
        this.codeEditor = codeEditor;
    }

    @Override
    public void onClick(View v) {
        String content = codeEditor.getText().toString();
        if (enteringNumber) {
            try {
                double parseDouble = Double.parseDouble(content);
                if (Double.isNaN(parseDouble) || Double.isInfinite(parseDouble)) {
                    content = "";
                }
            } catch (NumberFormatException e) {
                Log.e("AsdHandlerCodeEditor", e.getMessage(), e);
                content = "";
            }
        } else if (!content.isEmpty() && content.charAt(0) == '@') {
            content = " " + content;
        }
        logicEditorActivity.setFieldValue(fieldBlockView, content);
        asdDialog.dismiss();
    }
}
