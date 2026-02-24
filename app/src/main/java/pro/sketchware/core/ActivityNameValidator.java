package pro.sketchware.core;


import mod.hey.studios.util.Helper;
import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.regex.Pattern;

import pro.sketchware.R;

public class ActivityNameValidator extends BaseValidator {

    private static final Pattern namePattern = Pattern.compile("^[a-z][a-z0-9_]*");
    private final String[] reservedNames;
    private final ArrayList<String> existingNames;
    private String originalName;

    public ActivityNameValidator(Context context, TextInputLayout textInputLayout, String[] reservedNames, ArrayList<String> existingNames) {
        super(context, textInputLayout);
        this.reservedNames = reservedNames;
        this.existingNames = existingNames;
    }

    public ActivityNameValidator(Context context, TextInputLayout textInputLayout, String[] reservedNames, ArrayList<String> existingNames, String originalName) {
        super(context, textInputLayout);
        this.reservedNames = reservedNames;
        this.existingNames = existingNames;
        this.originalName = originalName;
    }

    @Override
    public void onTextChanged(CharSequence input, int start, int before, int count) {
        if (input.toString().trim().length() < 3) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.invalid_value_min_lenth, 3));
            valid = false;
            return;
        }
        if (input.toString().trim().length() > 100) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.invalid_value_max_lenth, 100));
            valid = false;
            return;
        }
        String currentName = originalName;
        if (currentName != null && !currentName.isEmpty() && input.toString().equals(originalName)) {
            textInputLayout.setErrorEnabled(false);
            valid = true;
            return;
        }
        for (String reservedName : reservedNames) {
            if (input.toString().equals(reservedName)) {
                textInputLayout.setErrorEnabled(true);
                textInputLayout.setError(Helper.getResString(R.string.logic_editor_message_reserved_keywords));
                valid = false;
                return;
            }
        }
        if ("main".equals(input.toString()) || existingNames.contains(input.toString())) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.common_message_name_unavailable));
            valid = false;
        } else if (!Character.isLetter(input.charAt(0))) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.logic_editor_message_variable_name_must_start_letter));
            valid = false;
        } else if (ActivityNameValidator.namePattern.matcher(input.toString()).matches()) {
            textInputLayout.setErrorEnabled(false);
            valid = true;
        } else {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.invalid_value_rule_4));
            valid = false;
        }
    }
}
