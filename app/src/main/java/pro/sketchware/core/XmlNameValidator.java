package pro.sketchware.core;

import android.content.Context;
import android.text.Spanned;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.regex.Pattern;
import pro.sketchware.R;

public class XmlNameValidator extends BaseValidator {
  public String[] reservedNames;
  
  public ArrayList<String> xmlNames;
  
  public ArrayList<String> javaNames;
  
  public String currentName;
  
  public int batchCount;
  
  public Pattern namePattern = Pattern.compile("^[a-z][a-z0-9_]*");
  
  public XmlNameValidator(Context context, TextInputLayout textInputLayout, String[] strings, ArrayList<String> list1, ArrayList<String> list2) {
    super(context, textInputLayout);
    reservedNames = strings;
    xmlNames = list1;
    javaNames = list2;
    batchCount = 1;
  }
  
  public void setBatchCount(int index) {
    batchCount = index;
    validate(getText());
  }
  
  public void setJavaNames(ArrayList<String> list) {
    javaNames = list;
  }
  
  public final void validate(String value) {
    String conflictList = "";
    if (value.length() < 3) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedStringFormatted(context, R.string.invalid_value_min_lenth, new Object[] { Integer.valueOf(3) }));
      valid = false;
      return;
    } 
    if (value.length() > 70) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedStringFormatted(context, R.string.invalid_value_max_lenth, new Object[] { Integer.valueOf(70) }));
      valid = false;
      return;
    } 
    if (value.equals("default_image") || "NONE".toLowerCase().equals(value.toLowerCase())) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.common_message_name_unavailable));
      valid = false;
      return;
    } 
    if (batchCount == 1) {
      if (!value.equals(currentName) && xmlNames.indexOf(value) >= 0) {
        textInputLayout.setErrorEnabled(true);
        textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.common_message_name_unavailable));
        valid = false;
        return;
      } 
      if (!value.equals(currentName) && javaNames.indexOf(value) >= 0) {
        textInputLayout.setErrorEnabled(true);
        textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.common_message_name_unavailable));
        valid = false;
        return;
      } 
    } else {
      ArrayList<String> candidateNames = new ArrayList<>();
      for (int batchIdx = 1; batchIdx <= batchCount; batchIdx++) {
        candidateNames.add(value + "_" + batchIdx);
      } 
      ArrayList<String> conflictNames = new ArrayList<>();
      for (String candidateName : candidateNames) {
        if (xmlNames.indexOf(candidateName) >= 0)
          conflictNames.add(candidateName); 
      } 
      if (conflictNames.size() > 0) {
        textInputLayout.setErrorEnabled(true);
        String errorMessage = StringResource.getInstance().getTranslatedString(context, R.string.common_message_name_unavailable);
        value = "";
        for (String conflictName : conflictNames) {
          String accumulated = value;
          if (value.length() > 0) {
            accumulated = value + ", ";
          } 
          conflictList = accumulated + conflictName;
        } 
        textInputLayout.setError(errorMessage + "\n[" + conflictList + "]");
        valid = false;
        return;
      } 
    } 
    String[] parts = reservedNames;
    int nameCount = parts.length;
    int found = 0;
    while (true) {
      if (found < nameCount) {
        if (value.equals(parts[found])) {
          found = 1;
          break;
        } 
        found++;
        continue;
      } 
      found = 0;
      break;
    } 
    if (found != 0) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.logic_editor_message_reserved_keywords));
      valid = false;
      return;
    } 
    if (!Character.isLetter(value.charAt(0))) {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.logic_editor_message_variable_name_must_start_letter));
      valid = false;
      return;
    } 
    if (namePattern.matcher(value).matches()) {
      textInputLayout.setErrorEnabled(false);
      valid = true;
    } else {
      textInputLayout.setErrorEnabled(true);
      textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.invalid_value_rule_4));
      valid = false;
    } 
  }
  
  public void setCurrentName(String value) {
    currentName = value;
  }
  
  public CharSequence filter(CharSequence text, int x, int y, Spanned spanned, int width, int height) {
    return null;
  }
  
  public void onTextChanged(CharSequence text, int x, int y, int width) {
    validate(text.toString().trim());
  }
}
