package pro.sketchware.lib.validator;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

import a.a.a.BaseValidator;

public class MinMaxInputValidator extends BaseValidator {
    public int minValue;
    public int maxValue;

    public MinMaxInputValidator(Context context, TextInputLayout textInputLayout, int minValue, int maxValue) {
        super(context, textInputLayout);
        this.minValue = minValue;
        this.maxValue = maxValue;
        editText = textInputLayout.getEditText();
        editText.setFilters(new InputFilter[]{this});
        editText.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String inputString = s.toString();
        if (inputString.isEmpty()) {
            textInputLayout.setError(String.format(Locale.US, "%d ~ %d", minValue, maxValue));
            valid = false;
        } else {
            try {
                int inputNumber = Integer.parseInt(inputString);
                if (inputNumber >= minValue && inputNumber <= maxValue) {
                    textInputLayout.setError(null);
                    valid = true;
                } else {
                    textInputLayout.setError(String.format(Locale.US, "%d ~ %d", minValue, maxValue));
                    valid = false;
                }
            } catch (NumberFormatException e) {
                textInputLayout.setError(String.format(Locale.US, "%d ~ %d", minValue, maxValue));
                valid = false;
            }
        }
    }
}
