package a.a.a;

import android.content.Context;
import android.text.Spanned;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Validator for resource names (sounds, images, icons).
 * Originally named WB, renamed to avoid Windows case collision with wB (ViewUtil).
 */
public class ResourceNameValidator extends BaseValidator {
  public String[] f;
  
  public ArrayList<String> g;
  
  public String h;
  
  public Pattern i = Pattern.compile("^[a-z][a-z0-9_]*");
  
  public ResourceNameValidator(Context paramContext, TextInputLayout paramTextInputLayout, String[] paramArrayOfString, ArrayList<String> paramArrayList) {
    super(paramContext, paramTextInputLayout);
    this.f = paramArrayOfString;
    this.g = paramArrayList;
  }
  
  public ResourceNameValidator(Context paramContext, TextInputLayout paramTextInputLayout, String[] paramArrayOfString, ArrayList<String> paramArrayList, String paramString) {
    super(paramContext, paramTextInputLayout);
    this.f = paramArrayOfString;
    this.g = paramArrayList;
    this.h = paramString;
  }
  
  public CharSequence filter(CharSequence paramCharSequence, int paramInt1, int paramInt2, Spanned paramSpanned, int paramInt3, int paramInt4) {
    return null;
  }
  
  public void onTextChanged(CharSequence paramCharSequence, int paramInt1, int paramInt2, int paramInt3) {
    String str = paramCharSequence.toString().trim();
    if (str.length() < 3) {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, 2131625433, new Object[] { Integer.valueOf(3) }));
      this.d = false;
      return;
    } 
    if (str.length() > 70) {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, 2131625432, new Object[] { Integer.valueOf(70) }));
      this.d = false;
      return;
    } 
    if (str.equals("default_image") || "NONE".toLowerCase().equals(str.toLowerCase())) {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, 2131624950));
      this.d = false;
      return;
    } 
    if (!str.equals(this.h) && this.g.indexOf(str) >= 0) {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, 2131624950));
      this.d = false;
      return;
    } 
    String[] arrayOfString = this.f;
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
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, 2131625495));
      this.d = false;
      return;
    } 
    if (!Character.isLetter(paramCharSequence.charAt(0))) {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, 2131625497));
      this.d = false;
      return;
    } 
    if (this.i.matcher(paramCharSequence.toString()).matches()) {
      this.b.setErrorEnabled(false);
      this.d = true;
    } else {
      this.b.setErrorEnabled(true);
      this.b.setError(xB.b().a(this.a, 2131625437));
      this.d = false;
    } 
  }
}
