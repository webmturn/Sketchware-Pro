package pro.sketchware.lib.validator;


import mod.hey.studios.util.Helper;
import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.regex.Pattern;

import pro.sketchware.core.BaseValidator;
import pro.sketchware.R;

public class PropertyNameValidator extends BaseValidator {

    private final String[] reservedNames;
    private final String[] reservedMethodNames;
    private final ArrayList<String> fileNames;
    private final String value;
    private final Pattern pattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*");

    public PropertyNameValidator(Context context, TextInputLayout textInputLayout,
                                 String[] reservedNames, String[] reservedMethodNames,
                                 ArrayList<String> fileNames, String value) {

        super(context, textInputLayout);
        this.reservedNames = reservedNames;
        this.reservedMethodNames = reservedMethodNames;
        this.fileNames = fileNames;
        this.value = value;
    }

    @Override
    public void onTextChanged(CharSequence input, int start, int before, int count) {
        String trimmedLowerName = input.toString().trim().toLowerCase();
        if (trimmedLowerName.length() < 1) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.invalid_value_min_lenth, 1));
            valid = false;
            return;
        }
        if (trimmedLowerName.length() > 100) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.invalid_value_max_lenth, 100));
            valid = false;
            return;
        }
        if (value != null && value.length() > 0 && trimmedLowerName.equals(value.toLowerCase())) {
            textInputLayout.setErrorEnabled(false);
            valid = true;
            return;
        }
        if (fileNames.contains(trimmedLowerName)) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.common_message_name_unavailable));
            valid = false;
            return;
        }
        for (String reservedMethodName : reservedMethodNames) {
            if (trimmedLowerName.equals(reservedMethodName)) {
                textInputLayout.setErrorEnabled(true);
                textInputLayout.setError(Helper.getResString(R.string.common_message_name_unavailable));
                valid = false;
                return;
            }
        }
        for (String reservedName : reservedNames) {
            if (trimmedLowerName.equals(reservedName)) {
                textInputLayout.setErrorEnabled(true);
                textInputLayout.setError(Helper.getResString(R.string.logic_editor_message_reserved_keywords));
                valid = false;
                return;
            }
        }
        if (!Character.isLetter(trimmedLowerName.charAt(0))) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.logic_editor_message_variable_name_must_start_letter));
            valid = false;
        } else if (pattern.matcher(input).matches()) {
            textInputLayout.setErrorEnabled(false);
            valid = true;
        } else {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.invalid_value_rule_3));
            valid = false;
        }
    }
}
