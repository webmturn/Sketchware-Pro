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
  
  public ResourceNameValidator(Context paramContext, TextInputLayout paramTextInputLayout, String[] paramArrayOfString, ArrayList<String> paramArrayList) {
    super(paramContext, paramTextInputLayout);
    this.reservedNames = paramArrayOfString;
    this.existingNames = paramArrayList;
  }
  
  public ResourceNameValidator(Context paramContext, TextInputLayout paramTextInputLayout, String[] paramArrayOfString, ArrayList<String> paramArrayList, String paramString) {
    super(paramContext, paramTextInputLayout);
    this.reservedNames = paramArrayOfString;
    this.existingNames = paramArrayList;
    this.currentName = paramString;
  }
  
  public CharSequence filter(CharSequence paramCharSequence, int paramInt1, int paramInt2, Spanned paramSpanned, int paramInt3, int paramInt4) {
    return null;
  }
  
  public void onTextChanged(CharSequence paramCharSequence, int paramInt1, int paramInt2, int paramInt3) {
    String str = paramCharSequence.toString().trim();
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
    paramInt2 = arrayOfString.length;
    paramInt1 = 0;
    while (true) {
      if (paramInt1 < paramInt2) {
        str = arrayOfString[paramInt1];
        if (paramCharSequence.toString().equals(str)) {
          paramInt1 = 1;
          break;
        } 
        paramInt1++;
        continue;
      } 
      paramInt1 = 0;
      break;
    } 
    if (paramInt1 != 0) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.logic_editor_message_reserved_keywords));
      this.valid = false;
      return;
    } 
    if (!Character.isLetter(paramCharSequence.charAt(0))) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.logic_editor_message_variable_name_must_start_letter));
      this.valid = false;
      return;
    } 
    if (this.namePattern.matcher(paramCharSequence.toString()).matches()) {
      this.textInputLayout.setErrorEnabled(false);
      this.valid = true;
    } else {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.invalid_value_rule_4));
      this.valid = false;
    } 
  }
}
