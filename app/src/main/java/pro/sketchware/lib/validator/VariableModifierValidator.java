package pro.sketchware.lib.validator;

import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import a.a.a.BaseValidator;
import pro.sketchware.R;

public class VariableModifierValidator extends BaseValidator {
    public static final Pattern PATTERN_MODIFIER = Pattern.compile(
            "\\b(public|protected|private|static|final|transient|volatile)\\b"
    );

    public VariableModifierValidator(Context context, TextInputLayout textInputLayout) {
        super(context, textInputLayout);
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int n, int n2, int n3) {
        String input = charSequence.toString();
        String trimmedInput = input.trim();
        String[] words = trimmedInput.split("\\s+");
        String reconsInput = String.join(" ", words);

        if (!input.equals(reconsInput)) {
            b.setError(b.getContext().getString(R.string.error_extra_spaces_words_not_allowed));
            d = false;
            return;
        }
        Set<String> usedModifiers = new HashSet<>();
        boolean hasAccessModifier = false;

        for (String word : words) {
            if (!PATTERN_MODIFIER.matcher(word).matches()) {
                b.setError(b.getContext().getString(R.string.error_invalid_modifier_format, word));
                d = false;
                return;
            }
            if (!usedModifiers.add(word)) {
                b.setError(b.getContext().getString(R.string.error_duplicate_modifier_format, word));
                d = false;
                return;
            }
            if (isAccessModifier(word)) {
                if (hasAccessModifier) {
                    b.setError(b.getContext().getString(R.string.error_access_modifier_only_one));
                    d = false;
                    return;
                }
                hasAccessModifier = true;
            }
        }
        b.setError(null);
        d = true;
    }

    private boolean isAccessModifier(String word) {
        return word.equals("public") || word.equals("protected") || word.equals("private");
    }

    public boolean isValid() {
        return b();
    }
}
