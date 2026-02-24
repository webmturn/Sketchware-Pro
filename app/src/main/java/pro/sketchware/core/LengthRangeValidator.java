package pro.sketchware.core;


import mod.hey.studios.util.Helper;
import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import pro.sketchware.R;

public class LengthRangeValidator extends BaseValidator {

    private final int min;
    private final int max;

    public LengthRangeValidator(Context context, TextInputLayout textInputLayout, int min, int max) {
        super(context, textInputLayout);
        this.min = min;
        this.max = max;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.toString().trim().length() < min) {
            textInputLayout.setErrorEnabled(true);
            if (customErrorResId == 0) {
                textInputLayout.setError(Helper.getResString(R.string.invalid_value_min_lenth, min));
            } else {
                textInputLayout.setError(context.getString(customErrorResId, min));
            }

            valid = false;
        } else {
            if (s.toString().trim().length() > max) {
                textInputLayout.setErrorEnabled(true);
                if (customErrorResId == 0) {
                    textInputLayout.setError(Helper.getResString(R.string.invalid_value_max_lenth, max));
                } else {
                    textInputLayout.setError(context.getString(customErrorResId, max));
                }

                valid = false;
            } else {
                textInputLayout.setErrorEnabled(false);
                valid = true;
            }

        }
    }
}
