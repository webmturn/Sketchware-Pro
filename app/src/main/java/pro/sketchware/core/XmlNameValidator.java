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
    String str = "";
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
      ArrayList<String> arrayList1 = new ArrayList<>();
      for (int b1 = 1; b1 <= this.batchCount; b1++) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(value);
        stringBuilder.append("_");
        stringBuilder.append(b1);
        arrayList1.add(stringBuilder.toString());
      } 
      ArrayList<String> arrayList2 = new ArrayList<>();
      for (String str1 : arrayList1) {
        if (this.xmlNames.indexOf(str1) >= 0)
          arrayList2.add(str1); 
      } 
      if (arrayList2.size() > 0) {
        this.textInputLayout.setErrorEnabled(true);
        String str1 = StringResource.getInstance().getTranslatedString(this.context, R.string.common_message_name_unavailable);
        Iterator<String> iterator = arrayList2.iterator();
        value = "";
        while (iterator.hasNext()) {
          String str3 = iterator.next();
          String str2 = value;
          if (value.length() > 0) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(value);
            stringBuilder2.append(", ");
            str2 = stringBuilder2.toString();
          } 
          StringBuilder stringBuilder1 = new StringBuilder();
          stringBuilder1.append(str2);
          stringBuilder1.append(str3);
          str = stringBuilder1.toString();
        } 
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str1);
        stringBuilder.append("\n[");
        stringBuilder.append(str);
        stringBuilder.append("]");
        this.textInputLayout.setError(stringBuilder.toString());
        this.valid = false;
        return;
      } 
    } 
    String[] parts = this.reservedNames;
    int i = parts.length;
    int b = 0;
    while (true) {
      if (b < i) {
        if (value.equals(parts[b])) {
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
