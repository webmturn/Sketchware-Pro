package a.a.a;

import android.content.Context;
import android.text.Spanned;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;
import pro.sketchware.R;

public class FileNameValidator extends BaseValidator {
  public String[] f;
  
  public ArrayList<String> g;
  
  public String h;
  
  public int i;
  
  public Pattern j = Pattern.compile("^[a-z][a-z0-9_]*");
  
  public FileNameValidator(Context paramContext, TextInputLayout paramTextInputLayout, String[] paramArrayOfString, ArrayList<String> paramArrayList) {
    super(paramContext, paramTextInputLayout);
    this.f = paramArrayOfString;
    this.g = paramArrayList;
    this.i = 1;
  }
  
  public FileNameValidator(Context paramContext, TextInputLayout paramTextInputLayout, String[] paramArrayOfString, ArrayList<String> paramArrayList, String paramString) {
    super(paramContext, paramTextInputLayout);
    this.f = paramArrayOfString;
    this.g = paramArrayList;
    this.h = paramString;
    this.i = 1;
  }
  
  public void a(int paramInt) {
    this.i = paramInt;
    if (getText().length() > 0)
      b(getText()); 
  }
  
  public final void b(String paramString) {
    String str = "";
    if (paramString.length() < 3) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.b().a(this.context, R.string.invalid_value_min_lenth, new Object[] { Integer.valueOf(3) }));
      this.valid = false;
      return;
    } 
    if (paramString.length() > 70) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.b().a(this.context, R.string.invalid_value_max_lenth, new Object[] { Integer.valueOf(70) }));
      this.valid = false;
      return;
    } 
    if (paramString.equals("default_image") || "NONE".toLowerCase().equals(paramString.toLowerCase())) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.b().a(this.context, R.string.common_message_name_unavailable));
      this.valid = false;
      return;
    } 
    if (this.i == 1) {
      if (!paramString.equals(this.h) && this.g.indexOf(paramString) >= 0) {
        this.textInputLayout.setErrorEnabled(true);
        this.textInputLayout.setError(StringResource.b().a(this.context, R.string.common_message_name_unavailable));
        this.valid = false;
        return;
      } 
    } else {
      ArrayList<String> arrayList1 = new ArrayList<>();
      for (int b1 = 1; b1 <= this.i; b1++) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(paramString);
        stringBuilder.append("_");
        stringBuilder.append(b1);
        arrayList1.add(stringBuilder.toString());
      } 
      ArrayList<String> arrayList2 = new ArrayList<>();
      for (String str1 : arrayList1) {
        if (this.g.indexOf(str1) >= 0)
          arrayList2.add(str1); 
      } 
      if (arrayList2.size() > 0) {
        this.textInputLayout.setErrorEnabled(true);
        String str1 = StringResource.b().a(this.context, R.string.common_message_name_unavailable);
        Iterator<String> iterator = arrayList2.iterator();
        paramString = "";
        while (iterator.hasNext()) {
          String str3 = iterator.next();
          String str2 = paramString;
          if (paramString.length() > 0) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(paramString);
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
    String[] arrayOfString = this.f;
    int i = arrayOfString.length;
    int b = 0;
    while (true) {
      if (b < i) {
        if (paramString.equals(arrayOfString[b])) {
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
      this.textInputLayout.setError(StringResource.b().a(this.context, R.string.logic_editor_message_reserved_keywords));
      this.valid = false;
      return;
    } 
    if (!Character.isLetter(paramString.charAt(0))) {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.b().a(this.context, R.string.logic_editor_message_variable_name_must_start_letter));
      this.valid = false;
      return;
    } 
    if (this.j.matcher(paramString).matches()) {
      this.textInputLayout.setErrorEnabled(false);
      this.valid = true;
    } else {
      this.textInputLayout.setErrorEnabled(true);
      this.textInputLayout.setError(StringResource.b().a(this.context, R.string.invalid_value_rule_4));
      this.valid = false;
    } 
  }
  
  public CharSequence filter(CharSequence paramCharSequence, int paramInt1, int paramInt2, Spanned paramSpanned, int paramInt3, int paramInt4) {
    return null;
  }
  
  public void onTextChanged(CharSequence paramCharSequence, int paramInt1, int paramInt2, int paramInt3) {
    b(paramCharSequence.toString().trim());
  }
}
