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
  
  public ResourceNameValidator(Context context, TextInputLayout textInputLayout, String[] reservedNames, ArrayList<String> existingNames) {
    super(context, textInputLayout);
    this.reservedNames = reservedNames;
    this.existingNames = existingNames;
  }
  
  public ResourceNameValidator(Context context, TextInputLayout textInputLayout, String[] reservedNames, ArrayList<String> existingNames, String currentName) {
    super(context, textInputLayout);
    this.reservedNames = reservedNames;
    this.existingNames = existingNames;
    this.currentName = currentName;
  }
  
  @Override
  public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
    return null;
  }
  
  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    String candidateName = s.toString().trim();
    if (candidateName.length() < 3) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedStringFormatted(context, R.string.invalid_value_min_lenth, new Object[] { Integer.valueOf(3) }));
      valid = false;
      return;
    } 
    if (candidateName.length() > 70) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedStringFormatted(context, R.string.invalid_value_max_lenth, new Object[] { Integer.valueOf(70) }));
      valid = false;
      return;
    } 
    if (candidateName.equals("default_image") || "NONE".toLowerCase().equals(candidateName.toLowerCase())) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.common_message_name_unavailable));
      valid = false;
      return;
    } 
    if (!candidateName.equals(currentName) && existingNames.indexOf(candidateName) >= 0) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.common_message_name_unavailable));
      valid = false;
      return;
    } 
    boolean isReserved = false;
    for (String reservedName : reservedNames) {
      if (candidateName.equals(reservedName)) {
        isReserved = true;
        break;
      }
    }
    if (isReserved) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.logic_editor_message_reserved_keywords));
      valid = false;
      return;
    } 
    if (!Character.isLetter(candidateName.charAt(0))) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.logic_editor_message_variable_name_must_start_letter));
      valid = false;
      return;
    } 
    if (namePattern.matcher(candidateName).matches()) {
      textInputLayout.setErrorEnabled(false);
      valid = true;
    } else {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.invalid_value_rule_4));
      valid = false;
    } 
  }
}
