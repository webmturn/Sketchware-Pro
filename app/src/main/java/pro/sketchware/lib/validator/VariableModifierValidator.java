package pro.sketchware.lib.validator;

import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import pro.sketchware.core.BaseValidator;
import pro.sketchware.R;

public class VariableModifierValidator extends BaseValidator {
    public static final Pattern PATTERN_MODIFIER = Pattern.compile(
            "\\b(public|protected|private|static|final|transient|volatile)\\b"
    );

    public VariableModifierValidator(Context context, TextInputLayout textInputLayout) {
        super(context, textInputLayout);
    }

    @Override
    public void onTextChanged(CharSequence cs, int n, int n2, int n3) {
        String input = cs.toString();
        String trimmedInput = input.trim();
        String[] words = trimmedInput.split("\\s+");
        String reconsInput = String.join(" ", words);

        if (!input.equals(reconsInput)) {
            textInputLayout.setError(textInputLayout.getContext().getString(R.string.error_extra_spaces_words_not_allowed));
            valid = false;
            return;
        }
        Set<String> usedModifiers = new HashSet<>();
        boolean hasAccessModifier = false;

        for (String word : words) {
            if (!PATTERN_MODIFIER.matcher(word).matches()) {
                textInputLayout.setError(textInputLayout.getContext().getString(R.string.error_invalid_modifier_format, word));
                valid = false;
                return;
            }
            if (!usedModifiers.add(word)) {
                textInputLayout.setError(textInputLayout.getContext().getString(R.string.error_duplicate_modifier_format, word));
                valid = false;
                return;
            }
            if (isAccessModifier(word)) {
                if (hasAccessModifier) {
                    textInputLayout.setError(textInputLayout.getContext().getString(R.string.error_access_modifier_only_one));
                    valid = false;
                    return;
                }
                hasAccessModifier = true;
            }
        }
        textInputLayout.setError(null);
        valid = true;
    }

    private boolean isAccessModifier(String word) {
        return word.equals("public") || word.equals("protected") || word.equals("private");
    }

}
