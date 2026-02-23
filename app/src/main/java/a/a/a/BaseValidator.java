package a.a.a;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

import mod.hey.studios.util.Helper;

public abstract class BaseValidator implements TextWatcher, InputFilter {

    public Context context;
    public TextInputLayout textInputLayout;
    public EditText editText;
    public boolean valid;
    public int customErrorResId;

    public BaseValidator(Context context, TextInputLayout textInputLayout) {
        this.context = context;
        this.textInputLayout = textInputLayout;
        editText = textInputLayout.getEditText();
        editText.setFilters(new InputFilter[]{this});
        editText.addTextChangedListener(this);
    }

    public String getText() {
        return Helper.getText(editText);
    }

    public void setText(String str) {
        valid = true;
        editText.setText(str);
    }

    @Override
    public CharSequence filter(CharSequence charSequence, int i, int i1, Spanned spanned, int i2, int i3) {
        return null;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (editable.toString().isEmpty()) {
            textInputLayout.setError(null);
            textInputLayout.setErrorEnabled(false);
        }
    }

    public boolean isValid() {
        if (!valid) editText.requestFocus();
        return valid;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }
}
