package pro.sketchware.core;

import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

import pro.sketchware.R;

public class VariableNameValidator extends BaseValidator {

    private final Pattern PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*");

    public VariableNameValidator(Context context, TextInputLayout textInputLayout) {
        super(context, textInputLayout);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.toString().trim().length() == 0) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.invalid_value_min_lenth, 1));
            valid = false;
        } else if (s.toString().trim().length() > 20) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.invalid_value_max_lenth, 20));
            valid = false;
        } else if (!Character.isLetter(s.charAt(0))) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.logic_editor_message_variable_name_must_start_letter));
            valid = false;
        } else if (PATTERN.matcher(s.toString()).matches()) {
            textInputLayout.setErrorEnabled(false);
            valid = true;
        } else {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.invalid_value_rule_3));
            valid = false;
        }
    }
}
