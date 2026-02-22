package a.a.a;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

public class FB {
  public static int a(char paramChar) {
    if (paramChar >= '0' && paramChar <= '9')
      return paramChar - 48; 
    byte b = 65;
    if (paramChar < 'A' || paramChar > 'F') {
      b = 97;
      if (paramChar < 'a' || paramChar > 'f') {
        StringBuilder stringBuilder = new StringBuilder("invalid hex digit '");
        stringBuilder.append(paramChar);
        stringBuilder.append("'");
        throw new IllegalArgumentException(stringBuilder.toString());
      } 
    } 
    return paramChar - b + 10;
  }
  
  public static String a() {
    Random random = new Random();
    int i = random.nextInt(100000);
    while (true) {
      if (i < 10000 || i > 99999) {
        i = random.nextInt(100000);
        continue;
      } 
      return String.valueOf(i);
    } 
  }
  
  public static String a(int paramInt) {
    String str;
    if (paramInt < 0)
      return "0"; 
    float f = paramInt;
    if (f >= 1024.0F && f < 1048576.0F) {
      f /= 1024.0F;
      str = (new DecimalFormat("#.#KB")).format(f);
    } else if (f >= 1048576.0F && f < 1.07374182E9F) {
      f /= 1048576.0F;
      str = (new DecimalFormat("#.#MB")).format(f);
    } else if (f >= 1.07374182E9F && f < 1.09951163E12F) {
      f /= 1.07374182E9F;
      str = (new DecimalFormat("#.#GB")).format(f);
    } else {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(String.valueOf(paramInt));
      stringBuilder.append("B");
      str = stringBuilder.toString();
    } 
    return str;
  }
  
  public static String a(byte[] paramArrayOfbyte) {
    StringBuffer stringBuffer = new StringBuffer(paramArrayOfbyte.length * 2);
    for (int b = 0; b < paramArrayOfbyte.length; b++) {
      if ((paramArrayOfbyte[b] & 0xFF) < 16)
        stringBuffer.append("0"); 
      stringBuffer.append(Long.toString((paramArrayOfbyte[b] & 0xFF), 16));
    } 
    return stringBuffer.toString();
  }
  
  public static void a(Context paramContext, String paramString1, String paramString2) {
    ((ClipboardManager)paramContext.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(paramString1, paramString2));
  }
  
  public static byte[] a(String paramString) {
    int i = paramString.length();
    byte[] arrayOfByte = new byte[(i + 1) / 2];
    int j = 0;
    byte b = 1;
    if (i % 2 == 1) {
      arrayOfByte[0] = (byte)a(paramString.charAt(0));
      j = 1;
    } else {
      b = 0;
    } 
    while (j < i) {
      int k = j + 1;
      arrayOfByte[b] = (byte)(a(paramString.charAt(j)) << 4 | a(paramString.charAt(k)));
      b++;
      j = k + 1;
    } 
    return arrayOfByte;
  }
  
  public static String b(int paramInt) {
    String str;
    float f = paramInt;
    if (f >= 1000.0F && f < 1000000.0F) {
      f /= 1000.0F;
      str = (new DecimalFormat("#.#K")).format(f);
    } else if (f >= 1000000.0F && f < 1.0E9F) {
      f /= 1000000.0F;
      str = (new DecimalFormat("#.#M")).format(f);
    } else if (f >= 1.0E9F && f < 1.0E12F) {
      f /= 1.0E9F;
      str = (new DecimalFormat("#.#G")).format(f);
    } else {
      str = String.valueOf(paramInt);
    } 
    return str;
  }
  
  public static boolean b(String paramString) {
    try {
      Double.parseDouble(paramString);
      return true;
    } catch (NumberFormatException numberFormatException) {
      return false;
    } 
  }
  
  public static String c(int paramInt) {
    return (new DecimalFormat("#,###")).format(paramInt);
  }
  
  public static ArrayList<String> c(String paramString) {
    ArrayList<String> arrayList = new ArrayList<>();
    a a = new a(paramString);
    while (!a.a()) {
      String str = a.b();
      if (str.length() > 0)
        arrayList.add(str); 
    } 
    return arrayList;
  }
  
  public static String d(String paramString) {
    String str = "";
    for (int b = 0; b < paramString.length(); b++) {
      char c = paramString.charAt(b);
      if (c == '\\') {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append(paramString.charAt(++b));
        str = stringBuilder.toString();
      } else {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append(c);
        str = stringBuilder.toString();
      } 
    } 
    return str;
  }
  
  public static class a {
    public String a;
    
    public int b;
    
    public a(String param1String) {
      this.a = param1String;
      this.b = 0;
    }
    
    public boolean a() {
      boolean bool;
      if (this.b >= this.a.length()) {
        bool = true;
      } else {
        bool = false;
      } 
      return bool;
    }
    
    public String b() {
      c();
      boolean bool = a();
      String str = "";
      if (bool)
        return ""; 
      boolean bool1 = false;
      int i = this.b;
      while (this.b < this.a.length() && this.a.charAt(this.b) != ' ') {
        char c = this.a.charAt(this.b);
        if (c == '\\') {
          StringBuilder stringBuilder1 = new StringBuilder();
          stringBuilder1.append(str);
          stringBuilder1.append(c + this.a.charAt(this.b + 1));
          str = stringBuilder1.toString();
          this.b += 2;
          continue;
        } 
        if (c == '%') {
          if (this.b > i)
            break; 
          bool1 = true;
        } 
        if (bool1 && (c == '?' || c == '-'))
          break; 
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append(c);
        str = stringBuilder.toString();
        this.b++;
      } 
      return str;
    }
    
    public void c() {
      while (this.b < this.a.length() && this.a.charAt(this.b) == ' ')
        this.b++; 
    }
  }
}
