package mod.hey.studios.moreblock;

import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.regex.Pattern;

import pro.sketchware.core.BaseValidator;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;

public class MoreblockValidator extends BaseValidator {

    private final String[] reservedKeywords;
    private final ArrayList<String> registeredVariables;
    private final Pattern j = Pattern.compile("^[a-zA-Z]\\w*(?:\\[[\\w\\[\\],<>?| ]*])?$");
    private String[] eventNames;
    private String i;

    public MoreblockValidator(Context context, TextInputLayout textInputLayout, String[] reservedKeywords, String[] eventNames, ArrayList<String> registeredVariables) {
        super(context, textInputLayout);
        this.reservedKeywords = reservedKeywords;
        this.eventNames = eventNames;
        this.registeredVariables = registeredVariables;
    }

    public void setReservedNames(String[] strArr) {
        eventNames = strArr;
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
        String name = charSequence.toString();
        int trimmedLength = name.trim().length();
        if (trimmedLength < 1) {
            textInputLayout.setError(context.getString(R.string.invalid_value_min_lenth, 1));
            valid = false;
        } else if (name.length() > 60) {
            textInputLayout.setError(context.getString(R.string.invalid_value_max_lenth, 60));
            valid = false;
        } else {
            if (i != null && !i.isEmpty() && name.equals(i)) {
                textInputLayout.setError(null);
                valid = true;
            } else if (registeredVariables.contains(name)) {
                textInputLayout.setError(context.getString(R.string.common_message_name_unavailable, 0));
                valid = false;
            } else {
                boolean z = false;
                for (String eventsName : eventNames) {
                    if (name.equals(eventsName)) {
                        z = true;
                        break;
                    }
                }
                if (z) {
                    textInputLayout.setError(Helper.getResString(R.string.common_message_name_unavailable));
                    valid = false;
                    return;
                }
                boolean isReservedKeyUsed = false;
                for (String keyword : reservedKeywords) {
                    if (name.equals(keyword)) {
                        isReservedKeyUsed = true;
                        break;
                    }
                }

                if (isReservedKeyUsed) {
                    textInputLayout.setError(Helper.getResString(R.string.logic_editor_message_reserved_keywords));
                    valid = false;
                } else if (!Character.isLetter(charSequence.charAt(0))) {
                    textInputLayout.setError(Helper.getResString(R.string.logic_editor_message_variable_name_must_start_letter));
                    valid = false;
                } else {
                    if (j.matcher(name).matches()) {
                        textInputLayout.setError(null);
                        valid = true;
                    } else {
                        textInputLayout.setError(Helper.getResString(R.string.invalid_value_rule_3));
                        valid = false;
                    }
                    if (name.trim().isEmpty()) {
                        textInputLayout.setError(context.getString(R.string.invalid_value_min_lenth, 1));
                        valid = false;
                    }
                }
            }
        }
    }
}
