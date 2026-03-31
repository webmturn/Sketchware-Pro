package pro.sketchware.core;

import android.content.Context;
import android.text.Spanned;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.regex.Pattern;
import pro.sketchware.R;

public class FileNameValidator extends BaseValidator {
  public String[] reservedNames;
  
  public ArrayList<String> existingNames;
  
  public String currentName;
  
  public int batchCount;
  
  public Pattern namePattern = Pattern.compile("^[a-z][a-z0-9_]*");
  
  public FileNameValidator(Context context, TextInputLayout textInputLayout, String[] reservedNames, ArrayList<String> existingNames) {
    super(context, textInputLayout);
    this.reservedNames = reservedNames;
    this.existingNames = existingNames;
    batchCount = 1;
  }
  
  public FileNameValidator(Context context, TextInputLayout textInputLayout, String[] reservedNames, ArrayList<String> existingNames, String currentName) {
    super(context, textInputLayout);
    this.reservedNames = reservedNames;
    this.existingNames = existingNames;
    this.currentName = currentName;
    batchCount = 1;
  }
  
  public void setBatchCount(int batchCount) {
    this.batchCount = batchCount;
    if (getText().length() > 0)
      validate(getText()); 
  }
  
  public final void validate(String candidateName) {
    String conflictList = "";
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
    if (batchCount == 1) {
      if (!candidateName.equals(currentName) && existingNames.indexOf(candidateName) >= 0) {
        textInputLayout.setErrorEnabled(true);
        textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.common_message_name_unavailable));
        valid = false;
        return;
      } 
    } else {
      ArrayList<String> candidateNames = new ArrayList<>();
      for (int batchIdx = 1; batchIdx <= batchCount; batchIdx++) {
        candidateNames.add(candidateName + "_" + batchIdx);
      } 
      ArrayList<String> conflictNames = new ArrayList<>();
      for (String generatedName : candidateNames) {
        if (existingNames.indexOf(generatedName) >= 0)
          conflictNames.add(generatedName);
      } 
      if (conflictNames.size() > 0) {
        textInputLayout.setErrorEnabled(true);
        String errorMessage = StringResource.getInstance().getTranslatedString(context, R.string.common_message_name_unavailable);
        conflictList = "";
        for (String conflictName : conflictNames) {
          String accumulated = conflictList;
          if (conflictList.length() > 0) {
            accumulated = conflictList + ", ";
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
        if (candidateName.equals(parts[found])) {
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
  
  public CharSequence filter(CharSequence text, int x, int y, Spanned spanned, int width, int height) {
    return null;
  }
  
  public void onTextChanged(CharSequence text, int x, int y, int width) {
    validate(text.toString().trim());
  }
}
