package pro.sketchware.core;

import android.content.Context;
import android.text.Spanned;
import com.google.android.material.textfield.TextInputLayout;
import pro.sketchware.R;

/**
 * Validator for numeric input within a specified range.
 * Originally named OB, renamed to avoid Windows case collision with oB (FileUtil).
 */
public class NumberRangeValidator extends BaseValidator {
  public int minValue;
  
  public int maxValue;
  
  public NumberRangeValidator(Context context, TextInputLayout textInputLayout, int x, int y) {
    super(context, textInputLayout);
    minValue = x;
    maxValue = y;
  }
  
  public CharSequence filter(CharSequence text, int x, int y, Spanned spanned, int width, int height) {
    return null;
  }
  
  public void onTextChanged(CharSequence text, int x, int y, int width) {
    String input = text.toString().trim();
    if (input.isEmpty()) {
      valid = false;
      return;
    }
    try {
      int parsedValue = Integer.parseInt(input);
      if (parsedValue < minValue) {
        textInputLayout.setErrorEnabled(true);
        textInputLayout.setError(StringResource.getInstance().getTranslatedStringFormatted(context, R.string.invalid_value_min_lenth, new Object[] { Integer.valueOf(minValue) }));
        valid = false;
      } else if (parsedValue > maxValue) {
        textInputLayout.setErrorEnabled(true);
        textInputLayout.setError(StringResource.getInstance().getTranslatedStringFormatted(context, R.string.invalid_value_max_lenth, new Object[] { Integer.valueOf(maxValue) }));
        valid = false;
      } else {
        textInputLayout.setErrorEnabled(false);
        valid = true;
      }
    } catch (NumberFormatException numberFormatException) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.logic_editor_message_variable_name_must_start_letter));
      valid = false;
    }
  }
}
