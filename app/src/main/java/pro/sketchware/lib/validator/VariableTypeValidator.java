package pro.sketchware.lib.validator;

import android.content.Context;
import android.text.TextUtils;

import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

import pro.sketchware.core.BaseValidator;
import pro.sketchware.R;

public class VariableTypeValidator extends BaseValidator {
    public static final Pattern PATTERN_TYPE = Pattern.compile(
            "^[a-zA-Z0-9._]+(<[a-zA-Z0-9.,_ ?<>\\[\\]]+>)?(\\[\\])*?$"
    );

    public VariableTypeValidator(Context context, TextInputLayout textInputLayout) {
        super(context, textInputLayout);
    }

    @Override
    public void onTextChanged(CharSequence input, int start, int before, int count) {
        String variableType = input.toString();
        String trimmedInput = variableType.trim();
        String[] words = trimmedInput.split("\\s+");
        String reconsInput = String.join(" ", words);

        if (!variableType.equals(reconsInput)) {
            textInputLayout.setError(textInputLayout.getContext().getString(R.string.error_extra_spaces_not_allowed));
            valid = false;
            return;
        }

        if (!TextUtils.isEmpty(input)) {
            if (!Character.isLetter(input.charAt(0))) {
                textInputLayout.setError(textInputLayout.getContext().getString(R.string.error_type_must_start_letter));
                valid = false;
                return;
            }
        }

        if (!isValidAngleBracket(variableType)) {
            textInputLayout.setError(textInputLayout.getContext().getString(R.string.error_angle_bracket_not_matched));
            valid = false;
            return;
        }

        if (!isValidBoxBracket(variableType)) {
            textInputLayout.setError(textInputLayout.getContext().getString(R.string.error_box_bracket_not_matched));
            valid = false;
            return;
        }

        if (!PATTERN_TYPE.matcher(variableType).matches()) {
            textInputLayout.setError(textInputLayout.getContext().getString(R.string.error_invalid_variable_type));
            valid = false;
            return;
        }
        textInputLayout.setError(null);
        valid = true;
    }

    private boolean isValidBracket(String _input, char _openingBracket, char _closingBracket) {
        int bracketCount = 0;
        for (char c : _input.toCharArray()) {
            if (c == _openingBracket) {
                bracketCount++;
            } else if (c == _closingBracket) {
                bracketCount--;
                if (bracketCount < 0) {
                    return false;
                }
            }
        }
        return bracketCount == 0;
    }

    private boolean isValidBoxBracket(String _input) {
        return isValidBracket(_input, '[', ']');
    }

    private boolean isValidAngleBracket(String _input) {
        return isValidBracket(_input, '<', '>');
    }
}
