package a.a.a;

import android.content.Context;
import android.text.Spanned;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;
import pro.sketchware.R;

public class QB extends BaseValidator {
  public String[] f;
  
  public ArrayList<String> g;
  
  public ArrayList<String> h;
  
  public String i;
  
  public int j;
  
  public Pattern k = Pattern.compile("^[a-z][a-z0-9_]*");
  
  public QB(Context paramContext, TextInputLayout paramTextInputLayout, String[] paramArrayOfString, ArrayList<String> paramArrayList1, ArrayList<String> paramArrayList2) {
    super(paramContext, paramTextInputLayout);
    this.f = paramArrayOfString;
    this.g = paramArrayList1;
    this.h = paramArrayList2;
    this.j = 1;
  }
  
  public void a(int paramInt) {
    this.j = paramInt;
    b(a());
  }
  
  public void a(ArrayList<String> paramArrayList) {
    this.h = paramArrayList;
  }
  
  public final void b(String paramString) {
    String str = "";
    if (paramString.length() < 3) {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, R.string.invalid_value_min_lenth, new Object[] { Integer.valueOf(3) }));
      this.d = false;
      return;
    } 
    if (paramString.length() > 70) {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, R.string.invalid_value_max_lenth, new Object[] { Integer.valueOf(70) }));
      this.d = false;
      return;
    } 
    if (paramString.equals("default_image") || "NONE".toLowerCase().equals(paramString.toLowerCase())) {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, R.string.common_message_name_unavailable));
      this.d = false;
      return;
    } 
    if (this.j == 1) {
      if (!paramString.equals(this.i) && this.g.indexOf(paramString) >= 0) {
        this.b.setErrorEnabled(true);
        this.b.setError(xB.b().a(this.a, R.string.common_message_name_unavailable));
        this.d = false;
        return;
      } 
      if (!paramString.equals(this.i) && this.h.indexOf(paramString) >= 0) {
        this.b.setErrorEnabled(true);
        this.b.setError(xB.b().a(this.a, R.string.common_message_name_unavailable));
        this.d = false;
        return;
      } 
    } else {
      ArrayList<String> arrayList1 = new ArrayList<>();
      for (int b1 = 1; b1 <= this.j; b1++) {
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
        this.b.setErrorEnabled(true);
        String str1 = xB.b().a(this.a, R.string.common_message_name_unavailable);
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
        TextInputLayout textInputLayout = this.b;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str1);
        stringBuilder.append("\n[");
        stringBuilder.append(str);
        stringBuilder.append("]");
        textInputLayout.setError(stringBuilder.toString());
        this.d = false;
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
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, R.string.logic_editor_message_reserved_keywords));
      this.d = false;
      return;
    } 
    if (!Character.isLetter(paramString.charAt(0))) {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, R.string.logic_editor_message_variable_name_must_start_letter));
      this.d = false;
      return;
    } 
    if (this.k.matcher(paramString).matches()) {
      this.b.setErrorEnabled(false);
      this.d = true;
    } else {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, R.string.invalid_value_rule_4));
      this.d = false;
    } 
  }
  
  public void c(String paramString) {
    this.i = paramString;
  }
  
  public CharSequence filter(CharSequence paramCharSequence, int paramInt1, int paramInt2, Spanned paramSpanned, int paramInt3, int paramInt4) {
    return null;
  }
  
  public void onTextChanged(CharSequence paramCharSequence, int paramInt1, int paramInt2, int paramInt3) {
    b(paramCharSequence.toString().trim());
  }
}
