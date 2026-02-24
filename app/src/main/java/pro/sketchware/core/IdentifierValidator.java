package pro.sketchware.core;

import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.regex.Pattern;

import pro.sketchware.R;

public class IdentifierValidator extends BaseValidator {

    private static final Pattern validNamePattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*");
    private final String[] restrictedNames;
    private final ArrayList<String> excludedNames;
    private String[] reservedNames;
    private String lastValidName;

    public IdentifierValidator(Context context, TextInputLayout textInputLayout, String[] newReservedNames, String[] restrictedKeywords, ArrayList<String> existingNames) {
        super(context, textInputLayout);
        restrictedNames = newReservedNames;
        reservedNames = restrictedKeywords;
        excludedNames = existingNames;
    }

    public void setReservedNames(String[] newReservedNames) {
        reservedNames = newReservedNames;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.toString().trim().isEmpty()) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.invalid_value_min_lenth, 1));
            valid = false;
            return;
        }
        if (s.toString().trim().length() > 100) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.invalid_value_max_lenth, 100));
            valid = false;
            return;
        }
        String previousValidName = lastValidName;
        if (previousValidName != null && !previousValidName.isEmpty() && s.toString().equals(lastValidName)) {
            textInputLayout.setError(null);
            textInputLayout.setErrorEnabled(false);
            valid = true;
            return;
        }
        if (excludedNames.contains(s.toString())) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.common_message_name_unavailable));
            valid = false;
            return;
        }
        for (String reservedName : reservedNames) {
            if (s.toString().equals(reservedName)) {
                textInputLayout.setErrorEnabled(true);
                textInputLayout.setError(context.getString(R.string.common_message_name_unavailable));
                valid = false;
                return;
            }
        }
        for (String restrictedName : restrictedNames) {
            if (s.toString().equals(restrictedName)) {
                textInputLayout.setErrorEnabled(true);
                textInputLayout.setError(context.getString(R.string.logic_editor_message_reserved_keywords));
                valid = false;
                return;
            }
        }
        if (!Character.isLetter(s.charAt(0))) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.logic_editor_message_variable_name_must_start_letter));
            valid = false;
            return;
        }
        if (validNamePattern.matcher(s.toString()).matches()) {
            textInputLayout.setError(null);
            textInputLayout.setErrorEnabled(false);
            valid = true;
        } else {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.invalid_value_rule_3));
            valid = false;
        }
        if (s.toString().trim().isEmpty()) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.invalid_value_min_lenth, 1));
            valid = false;
        }
    }
}
