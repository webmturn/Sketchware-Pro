package pro.sketchware.lib.validator;

import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

import a.a.a.BaseValidator;
import a.a.a.BlockConstants;
import pro.sketchware.R;

public class PackageNameValidator extends BaseValidator {

    private static final Pattern packagePattern = Pattern.compile("([a-zA-Z][a-zA-Z\\d]*\\.)*[a-zA-Z][a-zA-Z\\d]*");

    public PackageNameValidator(Context context, TextInputLayout textInputLayout) {
        super(context, textInputLayout);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.toString().trim().length() > 50) {
            textInputLayout.setErrorEnabled(true);
            if (customErrorResId == 0) {
                textInputLayout.setError(context.getString(R.string.invalid_value_max_lenth, 50));
            } else {
                //what ???
                textInputLayout.setError(context.getString(customErrorResId, 50));
            }
            valid = false;
            return;
        }
        textInputLayout.setErrorEnabled(false);
        valid = true;
        if (!packagePattern.matcher(s.toString()).matches()) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.invalid_value_rule_2));
            valid = false;
        } else {
            if (!s.toString().contains(".")) {
                textInputLayout.setErrorEnabled(true);
                textInputLayout.setError(context.getString(R.string.myprojects_settings_message_contain_dot));
                valid = false;
                return;
            }
            textInputLayout.setErrorEnabled(false);
            valid = true;
        }
        boolean containsReservedWord = false;
        for (String packagePart : s.toString().split("\\.")) {
            String[] reservedWords = BlockConstants.RESERVED_KEYWORDS;
            int length = reservedWords.length;
            int reservedWordIndex = 0;
            while (true) {
                if (reservedWordIndex >= length) {
                    break;
                }
                if (reservedWords[reservedWordIndex].equals(packagePart)) {
                    containsReservedWord = true;
                    break;
                }
                reservedWordIndex++;
            }
        }
        if (containsReservedWord) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.logic_editor_message_reserved_keywords));
            valid = false;
        }
    }
}
