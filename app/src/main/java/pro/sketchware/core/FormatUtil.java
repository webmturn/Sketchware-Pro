package pro.sketchware.core;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

public class FormatUtil {
  public static int hexCharToInt(char ch) {
    if (ch >= '0' && ch <= '9')
      return ch - 48; 
    byte b = 65;
    if (ch < 'A' || ch > 'F') {
      b = 97;
      if (ch < 'a' || ch > 'f') {
        StringBuilder stringBuilder = new StringBuilder("invalid hex digit '");
        stringBuilder.append(ch);
        stringBuilder.append("'");
        throw new IllegalArgumentException(stringBuilder.toString());
      } 
    } 
    return ch - b + 10;
  }
  
  public static String generateRandomId() {
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
  
  public static String formatFileSize(int index) {
    String str;
    if (index < 0)
      return "0"; 
    float f = index;
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
      stringBuilder.append(String.valueOf(index));
      stringBuilder.append("B");
      str = stringBuilder.toString();
    } 
    return str;
  }
  
  public static String bytesToHex(byte[] data) {
    StringBuffer stringBuffer = new StringBuffer(data.length * 2);
    for (int b = 0; b < data.length; b++) {
      if ((data[b] & 0xFF) < 16)
        stringBuffer.append("0"); 
      stringBuffer.append(Long.toString((data[b] & 0xFF), 16));
    } 
    return stringBuffer.toString();
  }
  
  public static void copyToClipboard(Context context, String key, String value) {
    ((ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(key, value));
  }
  
  public static byte[] hexStringToBytes(String value) {
    int i = value.length();
    byte[] bytes = new byte[(i + 1) / 2];
    int j = 0;
    byte b = 1;
    if (i % 2 == 1) {
      bytes[0] = (byte)hexCharToInt(value.charAt(0));
      j = 1;
    } else {
      b = 0;
    } 
    while (j < i) {
      int k = j + 1;
      bytes[b] = (byte)(hexCharToInt(value.charAt(j)) << 4 | hexCharToInt(value.charAt(k)));
      b++;
      j = k + 1;
    } 
    return bytes;
  }
  
  public static String formatNumber(int index) {
    String str;
    float f = index;
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
      str = String.valueOf(index);
    } 
    return str;
  }
  
  public static boolean isNumeric(String value) {
    try {
      Double.parseDouble(value);
      return true;
    } catch (NumberFormatException numberFormatException) {
      return false;
    } 
  }
  
  public static String formatWithCommas(int index) {
    return (new DecimalFormat("#,###")).format(index);
  }
  
  public static ArrayList<String> parseBlockSpec(String value) {
    ArrayList<String> arrayList = new ArrayList<>();
    StringScanner scanner = new StringScanner(value);
    while (!scanner.isAtEnd()) {
      String str = scanner.nextToken();
      if (str.length() > 0)
        arrayList.add(str); 
    } 
    return arrayList;
  }
  
  public static String unescapeString(String value) {
    String str = "";
    for (int b = 0; b < value.length(); b++) {
      char c = value.charAt(b);
      if (c == '\\') {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append(value.charAt(++b));
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
  
  public static class StringScanner {
    public String input;
    
    public int position;
    
    public StringScanner(String param1String) {
      this.input = param1String;
      this.position = 0;
    }
    
    public boolean isAtEnd() {
      boolean result;
      if (this.position >= this.input.length()) {
        result = true;
      } else {
        result = false;
      } 
      return result;
    }
    
    public String nextToken() {
      skipSpaces();
      boolean atEnd = isAtEnd();
      String str = "";
      if (atEnd)
        return ""; 
      boolean hasPercent = false;
      int i = this.position;
      while (this.position < this.input.length() && this.input.charAt(this.position) != ' ') {
        char c = this.input.charAt(this.position);
        if (c == '\\') {
          StringBuilder escapeBuilder = new StringBuilder();
          escapeBuilder.append(str);
          escapeBuilder.append(c + this.input.charAt(this.position + 1));
          str = escapeBuilder.toString();
          this.position += 2;
          continue;
        } 
        if (c == '%') {
          if (this.position > i)
            break; 
          hasPercent = true;
        } 
        if (hasPercent && (c == '?' || c == '-'))
          break; 
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append(c);
        str = stringBuilder.toString();
        this.position++;
      } 
      return str;
    }
    
    public void skipSpaces() {
      while (this.position < this.input.length() && this.input.charAt(this.position) == ' ')
        this.position++; 
    }
  }
}
