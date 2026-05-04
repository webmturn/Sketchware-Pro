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
  
  public XmlNameValidator(Context context, TextInputLayout textInputLayout, String[] reservedNames, ArrayList<String> xmlNames, ArrayList<String> javaNames) {
    super(context, textInputLayout);
    this.reservedNames = reservedNames;
    this.xmlNames = xmlNames;
    this.javaNames = javaNames;
    batchCount = 1;
  }
  
  public void setBatchCount(int batchCount) {
    this.batchCount = batchCount;
    validate(getText());
  }
  
  public void setJavaNames(ArrayList<String> javaNames) {
    this.javaNames = javaNames;
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
      if (!candidateName.equals(currentName) && xmlNames.indexOf(candidateName) >= 0) {
        textInputLayout.setErrorEnabled(true);
        textInputLayout.setError(StringResource.getInstance().getTranslatedString(context, R.string.common_message_name_unavailable));
        valid = false;
        return;
      } 
      if (!candidateName.equals(currentName) && javaNames.indexOf(candidateName) >= 0) {
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
        if (xmlNames.indexOf(generatedName) >= 0)
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
  
  public void setCurrentName(String currentName) {
    this.currentName = currentName;
  }
  
  @Override
  public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
    return null;
  }
  
  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    validate(s.toString().trim());
  }
}
