package pro.sketchware.lib.validator;


import mod.hey.studios.util.Helper;
import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

import pro.sketchware.core.BaseValidator;
import pro.sketchware.R;

public class AppNameValidator extends BaseValidator {

    private static final Pattern APP_NAME_PATTERN = Pattern.compile(".*[&\"'<>].*");

    public AppNameValidator(Context context, TextInputLayout textInputLayout) {
        super(context, textInputLayout);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.toString().trim().length() == 0) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.invalid_value_min_lenth, 1));
            valid = false;
        } else if (s.toString().trim().length() > 50) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.invalid_value_max_lenth, 50));
            valid = false;
        } else if (APP_NAME_PATTERN.matcher(s.toString()).matches()) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.invalid_value_rule_5));
            valid = false;
        } else {
            textInputLayout.setErrorEnabled(false);
            valid = true;
        }

    }
}
