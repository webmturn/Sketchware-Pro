package pro.sketchware.core;

import android.content.Context;
import android.text.Spanned;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Iterator;
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
    this.reservedNames = strings;
    this.xmlNames = list1;
    this.javaNames = list2;
    this.batchCount = 1;
  }
  
  public void setBatchCount(int index) {
    this.batchCount = index;
    validate(getText());
  }
  
  public void setJavaNames(ArrayList<String> list) {
    this.javaNames = list;
  }
  
  public final void validate(String value) {
    String conflictList = "";
    if (value.length() < 3) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedStringFormatted(this.context, R.string.invalid_value_min_lenth, new Object[] { Integer.valueOf(3) }));
      this.valid = false;
      return;
    } 
    if (value.length() > 70) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedStringFormatted(this.context, R.string.invalid_value_max_lenth, new Object[] { Integer.valueOf(70) }));
      this.valid = false;
      return;
    } 
    if (value.equals("default_image") || "NONE".toLowerCase().equals(value.toLowerCase())) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.common_message_name_unavailable));
      this.valid = false;
      return;
    } 
    if (this.batchCount == 1) {
      if (!value.equals(this.currentName) && this.xmlNames.indexOf(value) >= 0) {
        this.textInputLayout.setErrorEnabled(true);
        this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.common_message_name_unavailable));
        this.valid = false;
        return;
      } 
      if (!value.equals(this.currentName) && this.javaNames.indexOf(value) >= 0) {
        this.textInputLayout.setErrorEnabled(true);
        this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.common_message_name_unavailable));
        this.valid = false;
        return;
      } 
    } else {
      ArrayList<String> candidateNames = new ArrayList<>();
      for (int batchIdx = 1; batchIdx <= this.batchCount; batchIdx++) {
        StringBuilder candidateBuilder = new StringBuilder();
        candidateBuilder.append(value);
        candidateBuilder.append("_");
        candidateBuilder.append(batchIdx);
        candidateNames.add(candidateBuilder.toString());
      } 
      ArrayList<String> conflictNames = new ArrayList<>();
      for (String candidateName : candidateNames) {
        if (this.xmlNames.indexOf(candidateName) >= 0)
          conflictNames.add(candidateName); 
      } 
      if (conflictNames.size() > 0) {
        this.textInputLayout.setErrorEnabled(true);
        String errorMessage = StringResource.getInstance().getTranslatedString(this.context, R.string.common_message_name_unavailable);
        Iterator<String> iterator = conflictNames.iterator();
        value = "";
        while (iterator.hasNext()) {
          String conflictName = iterator.next();
          String accumulated = value;
          if (value.length() > 0) {
            StringBuilder separatorBuilder = new StringBuilder();
            separatorBuilder.append(value);
            separatorBuilder.append(", ");
            accumulated = separatorBuilder.toString();
          } 
          StringBuilder nameBuilder = new StringBuilder();
          nameBuilder.append(accumulated);
          nameBuilder.append(conflictName);
          conflictList = nameBuilder.toString();
        } 
        StringBuilder errorBuilder = new StringBuilder();
        errorBuilder.append(errorMessage);
        errorBuilder.append("\n[");
        errorBuilder.append(conflictList);
        errorBuilder.append("]");
        this.textInputLayout.setError(errorBuilder.toString());
        this.valid = false;
        return;
      } 
    } 
    String[] parts = this.reservedNames;
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
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.logic_editor_message_reserved_keywords));
      this.valid = false;
      return;
    } 
    if (!Character.isLetter(value.charAt(0))) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.logic_editor_message_variable_name_must_start_letter));
      this.valid = false;
      return;
    } 
    if (this.namePattern.matcher(value).matches()) {
      this.textInputLayout.setErrorEnabled(false);
      this.valid = true;
    } else {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.invalid_value_rule_4));
      this.valid = false;
    } 
  }
  
  public void setCurrentName(String value) {
    this.currentName = value;
  }
  
  public CharSequence filter(CharSequence text, int x, int y, Spanned spanned, int width, int height) {
    return null;
  }
  
  public void onTextChanged(CharSequence text, int x, int y, int width) {
    validate(text.toString().trim());
  }
}
