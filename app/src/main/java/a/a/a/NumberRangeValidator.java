package a.a.a;

import android.content.Context;
import android.text.Spanned;
import com.google.android.material.textfield.TextInputLayout;
import pro.sketchware.R;

/**
 * Validator for numeric input within a specified range.
 * Originally named OB, renamed to avoid Windows case collision with oB (FileUtil).
 */
public class NumberRangeValidator extends BaseValidator {
  public int f;
  
  public int g;
  
  public NumberRangeValidator(Context paramContext, TextInputLayout paramTextInputLayout, int paramInt1, int paramInt2) {
    super(paramContext, paramTextInputLayout);
    this.f = paramInt1;
    this.g = paramInt2;
  }
  
  public CharSequence filter(CharSequence paramCharSequence, int paramInt1, int paramInt2, Spanned paramSpanned, int paramInt3, int paramInt4) {
    return null;
  }
  
  public void onTextChanged(CharSequence paramCharSequence, int paramInt1, int paramInt2, int paramInt3) {
    String str = paramCharSequence.toString().trim();
    if (str.isEmpty()) {
      this.valid = false;
      return;
    }
    try {
      int i = Integer.parseInt(str);
      if (i < this.f) {
        this.textInputLayout.setErrorEnabled(true);
        this.textInputLayout.setError(xB.b().a(this.context, R.string.invalid_value_min_lenth, new Object[] { Integer.valueOf(this.f) }));
        this.valid = false;
      } else if (i > this.g) {
        this.textInputLayout.setErrorEnabled(true);
        this.textInputLayout.setError(xB.b().a(this.context, R.string.invalid_value_max_lenth, new Object[] { Integer.valueOf(this.g) }));
        this.valid = false;
      } else {
        this.textInputLayout.setErrorEnabled(false);
        this.valid = true;
      }
    } catch (NumberFormatException numberFormatException) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(xB.b().a(this.context, R.string.logic_editor_message_variable_name_must_start_letter));
      this.valid = false;
    }
  }
}
