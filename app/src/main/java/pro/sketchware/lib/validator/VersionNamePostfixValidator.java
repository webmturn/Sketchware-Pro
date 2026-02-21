package pro.sketchware.lib.validator;

import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

import a.a.a.BaseValidator;
import pro.sketchware.R;

public class VersionNamePostfixValidator extends BaseValidator {

    private static final Pattern VERSION_NAME_POSTFIX_PATTERN = Pattern.compile("^[a-zA-Z0-9_]*");

    public VersionNamePostfixValidator(Context context, TextInputLayout textInputLayout) {
        super(context, textInputLayout);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String se = s.toString();
        if (VERSION_NAME_POSTFIX_PATTERN.matcher(se).matches()) {
            b.setError(null);
            d = true;
        } else if (se.contains(" ")) {
            b.setError(b.getContext().getString(R.string.error_no_spaces_allowed));
            d = false;
        } else {
            b.setError(b.getContext().getString(R.string.error_only_letters_numbers_special));
            d = false;
        }
    }
}
