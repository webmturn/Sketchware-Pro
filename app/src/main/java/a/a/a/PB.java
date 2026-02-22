package a.a.a;

import android.content.Context;
import android.text.Spanned;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

public class PB extends BaseValidator {
  public String[] f;
  
  public ArrayList<String> g;
  
  public String h;
  
  public int i;
  
  public Pattern j = Pattern.compile("^[a-z][a-z0-9_]*");
  
  public PB(Context paramContext, TextInputLayout paramTextInputLayout, String[] paramArrayOfString, ArrayList<String> paramArrayList) {
    super(paramContext, paramTextInputLayout);
    this.f = paramArrayOfString;
    this.g = paramArrayList;
    this.i = 1;
  }
  
  public PB(Context paramContext, TextInputLayout paramTextInputLayout, String[] paramArrayOfString, ArrayList<String> paramArrayList, String paramString) {
    super(paramContext, paramTextInputLayout);
    this.f = paramArrayOfString;
    this.g = paramArrayList;
    this.h = paramString;
    this.i = 1;
  }
  
  public void a(int paramInt) {
    this.i = paramInt;
    if (a().length() > 0)
      b(a()); 
  }
  
  public final void b(String paramString) {
    String str = "";
    if (paramString.length() < 3) {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, 2131625433, new Object[] { Integer.valueOf(3) }));
      this.d = false;
      return;
    } 
    if (paramString.length() > 70) {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, 2131625432, new Object[] { Integer.valueOf(70) }));
      this.d = false;
      return;
    } 
    if (paramString.equals("default_image") || "NONE".toLowerCase().equals(paramString.toLowerCase())) {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, 2131624950));
      this.d = false;
      return;
    } 
    if (this.i == 1) {
      if (!paramString.equals(this.h) && this.g.indexOf(paramString) >= 0) {
        this.b.setErrorEnabled(true);
        this.b.setError(xB.b().a(this.a, 2131624950));
        this.d = false;
        return;
      } 
    } else {
      ArrayList<String> arrayList1 = new ArrayList<>();
      for (byte b1 = 1; b1 <= this.i; b1++) {
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
        String str1 = xB.b().a(this.a, 2131624950);
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
    byte b = 0;
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
      this.b.setError(xB.b().a(this.a, 2131625495));
      this.d = false;
      return;
    } 
    if (!Character.isLetter(paramString.charAt(0))) {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, 2131625497));
      this.d = false;
      return;
    } 
    if (this.j.matcher(paramString).matches()) {
      this.b.setErrorEnabled(false);
      this.d = true;
    } else {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, 2131625437));
      this.d = false;
    } 
  }
  
  public CharSequence filter(CharSequence paramCharSequence, int paramInt1, int paramInt2, Spanned paramSpanned, int paramInt3, int paramInt4) {
    return null;
  }
  
  public void onTextChanged(CharSequence paramCharSequence, int paramInt1, int paramInt2, int paramInt3) {
    b(paramCharSequence.toString().trim());
  }
}
