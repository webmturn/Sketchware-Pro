package a.a.a;

import android.content.Context;
import android.text.Spanned;
import com.google.android.material.textfield.TextInputLayout;

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
      this.d = false;
      return;
    }
    try {
      int i = Integer.parseInt(str);
      if (i < this.f) {
        this.b.setErrorEnabled(true);
        this.b.setError(xB.b().a(this.a, 2131625433, new Object[] { Integer.valueOf(this.f) }));
        this.d = false;
      } else if (i > this.g) {
        this.b.setErrorEnabled(true);
        this.b.setError(xB.b().a(this.a, 2131625432, new Object[] { Integer.valueOf(this.g) }));
        this.d = false;
      } else {
        this.b.setErrorEnabled(false);
        this.d = true;
      }
    } catch (NumberFormatException numberFormatException) {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, 2131625497));
      this.d = false;
    }
  }
}
