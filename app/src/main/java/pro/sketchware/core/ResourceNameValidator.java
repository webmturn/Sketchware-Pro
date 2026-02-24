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
    this.reservedNames = strings;
    this.existingNames = list;
  }
  
  public ResourceNameValidator(Context context, TextInputLayout textInputLayout, String[] strings, ArrayList<String> list, String input) {
    super(context, textInputLayout);
    this.reservedNames = strings;
    this.existingNames = list;
    this.currentName = input;
  }
  
  public CharSequence filter(CharSequence text, int x, int y, Spanned spanned, int width, int height) {
    return null;
  }
  
  public void onTextChanged(CharSequence text, int x, int y, int width) {
    String str = text.toString().trim();
    if (str.length() < 3) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedStringFormatted(this.context, R.string.invalid_value_min_lenth, new Object[] { Integer.valueOf(3) }));
      this.valid = false;
      return;
    } 
    if (str.length() > 70) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedStringFormatted(this.context, R.string.invalid_value_max_lenth, new Object[] { Integer.valueOf(70) }));
      this.valid = false;
      return;
    } 
    if (str.equals("default_image") || "NONE".toLowerCase().equals(str.toLowerCase())) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.common_message_name_unavailable));
      this.valid = false;
      return;
    } 
    if (!str.equals(this.currentName) && this.existingNames.indexOf(str) >= 0) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.common_message_name_unavailable));
      this.valid = false;
      return;
    } 
    String[] arrayOfString = this.reservedNames;
    y = arrayOfString.length;
    x = 0;
    while (true) {
      if (x < y) {
        str = arrayOfString[x];
        if (text.toString().equals(str)) {
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
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.logic_editor_message_reserved_keywords));
      this.valid = false;
      return;
    } 
    if (!Character.isLetter(text.charAt(0))) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.logic_editor_message_variable_name_must_start_letter));
      this.valid = false;
      return;
    } 
    if (this.namePattern.matcher(text.toString()).matches()) {
      this.textInputLayout.setErrorEnabled(false);
      this.valid = true;
    } else {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.invalid_value_rule_4));
      this.valid = false;
    } 
  }
}
