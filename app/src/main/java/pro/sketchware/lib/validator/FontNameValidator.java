package pro.sketchware.lib.validator;


import mod.hey.studios.util.Helper;
import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.regex.Pattern;

import pro.sketchware.core.BaseValidator;
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

    public FontNameValidator(Context context, TextInputLayout textInputLayout, String[] reservedKeywordsArr, ArrayList<String> fontNameList, String currentName) {
        super(context, textInputLayout);
        pattern = Pattern.compile("^[a-z][a-z0-9_]*");
        reservedKeywords = reservedKeywordsArr;
        fontNames = fontNameList;
        editingName = currentName;
    }

    @Override
    public void onTextChanged(CharSequence input, int start, int before, int count) {
        String errorMessage;
        int msgRes;
        String trim = input.toString().trim();
        if (trim.length() < 3) {
            errorMessage = Helper.getResString(R.string.invalid_value_min_lenth, 3);
        } else if (trim.length() > 70) {
            errorMessage = Helper.getResString(R.string.invalid_value_max_lenth, 70);
        } else if (trim.equals("default_image") || "NONE".equalsIgnoreCase(trim) || (!trim.equals(editingName) && (fontNames != null && fontNames.contains(trim)))) {
            errorMessage = Helper.getResString(R.string.common_message_name_unavailable);
        } else {
            int keywordIndex = 0;
            while (true) {
                if (keywordIndex < reservedKeywords.length) {
                    if (input.toString().equals(reservedKeywords[keywordIndex])) {
                        msgRes = R.string.logic_editor_message_reserved_keywords;
                        break;
                    }
                    keywordIndex++;
                } else if (Character.isLetter(input.charAt(0))) {
                    if (pattern.matcher(input.toString()).matches()) {
                        textInputLayout.setError(null);
                        valid = true;
                        return;
                    }
                    textInputLayout.setError(Helper.getResString(R.string.invalid_value_rule_4));
                    valid = false;
                    return;
                } else {
                    msgRes = R.string.logic_editor_message_variable_name_must_start_letter;
                }
            }
            errorMessage = context.getString(msgRes);
        }
        textInputLayout.setError(errorMessage);
        valid = false;
    }
}
