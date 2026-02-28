package pro.sketchware.core;

import android.content.Context;
import android.text.Spanned;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.regex.Pattern;
import pro.sketchware.R;

/**
 * Validator for resource names (sounds, images, icons).
 * Originally named WB, renamed to avoid Windows case collision with wB (ViewUtil).
 */
public class ResourceNameValidator extends BaseValidator {
  public String[] reservedNames;
  
  public ArrayList<String> existingNames;
  
  public String currentName;
  
  public Pattern namePattern = Pattern.compile("^[a-z][a-z0-9_]*");
  
  public ResourceNameValidator(Context context, TextInputLayout textInputLayout, String[] strings, ArrayList<String> list) {
    super(context, textInputLayout);
    reservedNames = strings;
    existingNames = list;
  }
  
  public ResourceNameValidator(Context context, TextInputLayout textInputLayout, String[] strings, ArrayList<String> list, String input) {
    super(context, textInputLayout);
    reservedNames = strings;
    existingNames = list;
    currentName = input;
  }
  
  public CharSequence filter(CharSequence text, int x, int y, Spanned spanned, int width, int height) {
    return null;
  }
  
  public void onTextChanged(CharSequence text, int x, int y, int width) {
    String input = text.toString().trim();
    if (input.length() < 3) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedStringFormatted(context, R.string.invalid_value_min_lenth, new Object[] { Integer.valueOf(3) }));
      valid = false;
      return;
    } 
    if (input.length() > 70) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedStringFormatted(context, R.string.invalid_value_max_lenth, new Object[] { Integer.valueOf(70) }));
      valid = false;
      return;
    } 
    if (input.equals("default_image") || "NONE".toLowerCase().equals(input.toLowerCase())) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.common_message_name_unavailable));
      valid = false;
      return;
    } 
    if (!input.equals(currentName) && existingNames.indexOf(input) >= 0) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.common_message_name_unavailable));
      valid = false;
      return;
    } 
    String[] parts = reservedNames;
    y = parts.length;
    x = 0;
    while (true) {
      if (x < y) {
        String reservedName = parts[x];
        if (text.toString().equals(reservedName)) {
          x = 1;
          break;
        } 
        x++;
        continue;
      } 
      x = 0;
      break;
    } 
    if (x != 0) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.logic_editor_message_reserved_keywords));
      valid = false;
      return;
    } 
    if (!Character.isLetter(text.charAt(0))) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.logic_editor_message_variable_name_must_start_letter));
      valid = false;
      return;
    } 
    if (namePattern.matcher(text.toString()).matches()) {
      textInputLayout.setErrorEnabled(false);
      valid = true;
    } else {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.invalid_value_rule_4));
      valid = false;
    } 
  }
}
