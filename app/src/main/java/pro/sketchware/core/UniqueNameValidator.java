package pro.sketchware.core;

import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

import pro.sketchware.R;

public class UniqueNameValidator extends BaseValidator {

    private final ArrayList<String> preDefNames;

    public UniqueNameValidator(Context var1, TextInputLayout var2, ArrayList<String> names) {
        super(var1, var2);
        preDefNames = names;
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
        String inputValue = charSequence.toString();
        if (preDefNames.contains(inputValue)) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(context.getString(R.string.common_message_name_unavailable));
            valid = false;
        } else {
            textInputLayout.setErrorEnabled(false);
            valid = true;
        }
    }
}
