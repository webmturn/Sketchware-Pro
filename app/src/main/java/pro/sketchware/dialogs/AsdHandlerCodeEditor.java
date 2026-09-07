package pro.sketchware.dialogs;


import android.util.Log;
import android.view.View;

import pro.sketchware.core.block.view.FieldBlockView;
import io.github.rosemoe.sora.widget.CodeEditor;

public class AsdHandlerCodeEditor implements View.OnClickListener {

    private final boolean enteringNumber;
    private final AsdDialog asdDialog;
    private final FieldValueSetter fieldValueSetter;
    private final CodeEditor codeEditor;
    private final FieldBlockView fieldBlockView;

    public AsdHandlerCodeEditor(FieldValueSetter fieldValueSetter, boolean enteringNumber, FieldBlockView fieldBlockView, AsdDialog asdDialog, CodeEditor codeEditor) {
        this.fieldValueSetter = fieldValueSetter;
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
        fieldValueSetter.setFieldValue(fieldBlockView, content);
        asdDialog.dismiss();
    }
}
