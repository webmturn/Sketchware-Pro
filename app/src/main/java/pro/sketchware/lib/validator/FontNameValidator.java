package pro.sketchware.lib.validator;

import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.regex.Pattern;

import a.a.a.BaseValidator;
import pro.sketchware.R;

public class FontNameValidator extends BaseValidator {
    public String[] reservedKeywords;
    public ArrayList<String> fontNames;
    public String editingName;
    public Pattern pattern;

    public FontNameValidator(Context context, TextInputLayout textInputLayout, String[] reservedKeywordsArr, ArrayList<String> arrayList) {
        super(context, textInputLayout);
        pattern = Pattern.compile("^[a-z][a-z0-9_]*");
        reservedKeywords = reservedKeywordsArr;
        fontNames = arrayList;
    }

    public FontNameValidator(Context context, TextInputLayout textInputLayout, String[] strArr, ArrayList<String> arrayList, String str) {
        super(context, textInputLayout);
        pattern = Pattern.compile("^[a-z][a-z0-9_]*");
        reservedKeywords = strArr;
        fontNames = arrayList;
        editingName = str;
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        String a2;
        int msgRes;
        String trim = charSequence.toString().trim();
        if (trim.length() < 3) {
            a2 = context.getString(R.string.invalid_value_min_lenth, 3);
        } else if (trim.length() > 70) {
            a2 = context.getString(R.string.invalid_value_max_lenth, 70);
        } else if (trim.equals("default_image") || "NONE".equalsIgnoreCase(trim) || (!trim.equals(editingName) && (fontNames != null && fontNames.contains(trim)))) {
            a2 = context.getString(R.string.common_message_name_unavailable);
        } else {
            int count = 0;
            while (true) {
                if (count < reservedKeywords.length) {
                    if (charSequence.toString().equals(reservedKeywords[count])) {
                        msgRes = R.string.logic_editor_message_reserved_keywords;
                        break;
                    }
                    count++;
                } else if (Character.isLetter(charSequence.charAt(0))) {
                    if (pattern.matcher(charSequence.toString()).matches()) {
                        textInputLayout.setError(null);
                        valid = true;
                        return;
                    }
                    textInputLayout.setError(context.getString(R.string.invalid_value_rule_4));
                    valid = false;
                    return;
                } else {
                    msgRes = R.string.logic_editor_message_variable_name_must_start_letter;
                }
            }
            a2 = context.getString(msgRes);
        }
        textInputLayout.setError(a2);
        valid = false;
    }
}
