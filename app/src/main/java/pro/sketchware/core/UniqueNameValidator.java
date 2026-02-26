package pro.sketchware.core;


import mod.hey.studios.util.Helper;
import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

import pro.sketchware.R;

public class UniqueNameValidator extends BaseValidator {

    private final ArrayList<String> preDefNames;

    public UniqueNameValidator(Context context, TextInputLayout textInputLayout, ArrayList<String> names) {
        super(context, textInputLayout);
        preDefNames = names;
    }

    @Override
    public void onTextChanged(CharSequence input, int start, int before, int count) {
        String inputValue = input.toString();
        if (preDefNames.contains(inputValue)) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.common_message_name_unavailable));
            valid = false;
        } else {
            textInputLayout.setErrorEnabled(false);
            valid = true;
        }
    }
}
