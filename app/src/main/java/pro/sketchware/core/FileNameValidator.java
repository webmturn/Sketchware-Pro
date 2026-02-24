package pro.sketchware.core;

import android.content.Context;
import android.text.Spanned;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;
import pro.sketchware.R;

public class FileNameValidator extends BaseValidator {
  public String[] reservedNames;
  
  public ArrayList<String> existingNames;
  
  public String currentName;
  
  public int batchCount;
  
  public Pattern namePattern = Pattern.compile("^[a-z][a-z0-9_]*");
  
  public FileNameValidator(Context context, TextInputLayout textInputLayout, String[] strings, ArrayList<String> list) {
    super(context, textInputLayout);
    this.reservedNames = strings;
    this.existingNames = list;
    this.batchCount = 1;
  }
  
  public FileNameValidator(Context context, TextInputLayout textInputLayout, String[] strings, ArrayList<String> list, String currentName) {
    super(context, textInputLayout);
    this.reservedNames = strings;
    this.existingNames = list;
    this.currentName = currentName;
    this.batchCount = 1;
  }
  
  public void setBatchCount(int index) {
    this.batchCount = index;
    if (getText().length() > 0)
      validate(getText()); 
  }
  
  public final void validate(String input) {
    String conflictList = "";
    if (conflictList.length() < 3) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedStringFormatted(this.context, R.string.invalid_value_min_lenth, new Object[] { Integer.valueOf(3) }));
      this.valid = false;
      return;
    } 
    if (conflictList.length() > 70) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedStringFormatted(this.context, R.string.invalid_value_max_lenth, new Object[] { Integer.valueOf(70) }));
      this.valid = false;
      return;
    } 
    if (conflictList.equals("default_image") || "NONE".toLowerCase().equals(conflictList.toLowerCase())) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.common_message_name_unavailable));
      this.valid = false;
      return;
    } 
    if (this.batchCount == 1) {
      if (!conflictList.equals(this.currentName) && this.existingNames.indexOf(conflictList) >= 0) {
        this.textInputLayout.setErrorEnabled(true);
        this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.common_message_name_unavailable));
        this.valid = false;
        return;
      } 
    } else {
      ArrayList<String> candidateNames = new ArrayList<>();
      for (int b1 = 1; b1 <= this.batchCount; b1++) {
        StringBuilder candidateBuilder = new StringBuilder();
        candidateBuilder.append(conflictList);
        candidateBuilder.append("_");
        candidateBuilder.append(b1);
        candidateNames.add(candidateBuilder.toString());
      } 
      ArrayList<String> conflictNames = new ArrayList<>();
      for (String candidateName : candidateNames) {
        if (this.existingNames.indexOf(candidateName) >= 0)
          conflictNames.add(candidateName); 
      } 
      if (conflictNames.size() > 0) {
        this.textInputLayout.setErrorEnabled(true);
        String errorMessage = StringResource.getInstance().getTranslatedString(this.context, R.string.common_message_name_unavailable);
        Iterator<String> iterator = conflictNames.iterator();
        conflictList = "";
        while (iterator.hasNext()) {
          String conflictName = iterator.next();
          String accumulated = conflictList;
          if (conflictList.length() > 0) {
            StringBuilder separatorBuilder = new StringBuilder();
            separatorBuilder.append(conflictList);
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
    int i = parts.length;
    int b = 0;
    while (true) {
      if (b < i) {
        if (conflictList.equals(parts[b])) {
          b = 1;
          break;
        } 
        b++;
        continue;
      } 
      b = 0;
      break;
    } 
    if (b != 0) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.logic_editor_message_reserved_keywords));
      this.valid = false;
      return;
    } 
    if (!Character.isLetter(conflictList.charAt(0))) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.logic_editor_message_variable_name_must_start_letter));
      this.valid = false;
      return;
    } 
    if (this.namePattern.matcher(conflictList).matches()) {
      this.textInputLayout.setErrorEnabled(false);
      this.valid = true;
    } else {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.getInstance().getTranslatedString(this.context, R.string.invalid_value_rule_4));
      this.valid = false;
    } 
  }
  
  public CharSequence filter(CharSequence text, int x, int y, Spanned spanned, int width, int height) {
    return null;
  }
  
  public void onTextChanged(CharSequence text, int x, int y, int width) {
    validate(text.toString().trim());
  }
}
