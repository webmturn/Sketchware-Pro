package pro.sketchware.core.validation;

import pro.sketchware.core.codegen.StringResource;

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
  
  public NumberRangeValidator(Context context, TextInputLayout textInputLayout, int minValue, int maxValue) {
    super(context, textInputLayout);
    this.minValue = minValue;
    this.maxValue = maxValue;
  }
  
  @Override
  public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
    return null;
  }
  
  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    String input = s.toString().trim();
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
