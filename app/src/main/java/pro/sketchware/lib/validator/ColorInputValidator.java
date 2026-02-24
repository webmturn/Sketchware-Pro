package pro.sketchware.lib.validator;


import mod.hey.studios.util.Helper;
import android.content.Context;
import android.graphics.Color;
import android.view.View;

import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

import pro.sketchware.core.BaseValidator;
import pro.sketchware.R;

public class ColorInputValidator extends BaseValidator {

    private final Pattern hexPattern = Pattern.compile("[A-Fa-f0-9]*");
    private final View colorPreview;

    public ColorInputValidator(Context var1, TextInputLayout textInputLayout, View colorPreview) {
        super(var1, textInputLayout);
        this.colorPreview = colorPreview;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String hexCode = s.toString().trim();
        if (hexCode.startsWith("#")) {
            hexCode = hexCode.substring(1);
        }
        if (hexCode.length() > 8) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(Helper.getResString(R.string.invalid_value_max_lenth, 8));
            valid = false;
        } else {
            if (hexPattern.matcher(hexCode).matches()) {
                try {
                    hexCode = String.format("#%8s", hexCode).replaceAll(" ", "F");
                    colorPreview.setBackgroundColor(Color.parseColor(hexCode));
                } catch (IllegalArgumentException var5) {
                    textInputLayout.setErrorEnabled(true);
                    textInputLayout.setError(Helper.getResString(R.string.invalid_value_format));
                    valid = false;
                    colorPreview.setBackgroundColor(0xfff6f6f6);
                }

                textInputLayout.setErrorEnabled(false);
                valid = true;
            } else {
                textInputLayout.setErrorEnabled(true);
                textInputLayout.setError(Helper.getResString(R.string.invalid_value_format));
                colorPreview.setBackgroundColor(0xfff6f6f6);
                valid = false;
            }

        }
    }
}
