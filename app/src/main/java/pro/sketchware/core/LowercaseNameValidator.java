package pro.sketchware.core;

import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

import pro.sketchware.R;

public class LowercaseNameValidator extends BaseValidator {

    private final Pattern NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_ ]*");

    public LowercaseNameValidator(Context context, TextInputLayout textInputLayout) {
        super(context, textInputLayout);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.toString().trim().length() == 0) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.invalid_value_min_lenth, 1));
            valid = false;
        } else if (NAME_PATTERN.matcher(s.toString()).matches()) {
            textInputLayout.setErrorEnabled(false);
            valid = true;
        } else {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.invalid_value_rule_4));
            valid = false;
        }

    }
}
