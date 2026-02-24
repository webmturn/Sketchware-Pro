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
        StringBuilder errorBuilder = new StringBuilder("invalid hex digit '");
        errorBuilder.append(ch);
        errorBuilder.append("'");
        throw new IllegalArgumentException(errorBuilder.toString());
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
    String formatted;
    if (index < 0)
      return "0"; 
    float f = index;
    if (f >= 1024.0F && f < 1048576.0F) {
      f /= 1024.0F;
      formatted = (new DecimalFormat("#.#KB")).format(f);
    } else if (f >= 1048576.0F && f < 1.07374182E9F) {
      f /= 1048576.0F;
      formatted = (new DecimalFormat("#.#MB")).format(f);
    } else if (f >= 1.07374182E9F && f < 1.09951163E12F) {
      f /= 1.07374182E9F;
      formatted = (new DecimalFormat("#.#GB")).format(f);
    } else {
      StringBuilder sizeBuilder = new StringBuilder();
      sizeBuilder.append(String.valueOf(index));
      sizeBuilder.append("B");
      formatted = sizeBuilder.toString();
    } 
    return formatted;
  }
  
  public static String bytesToHex(byte[] data) {
    StringBuffer hexBuffer = new StringBuffer(data.length * 2);
    for (int b = 0; b < data.length; b++) {
      if ((data[b] & 0xFF) < 16)
        hexBuffer.append("0"); 
      hexBuffer.append(Long.toString((data[b] & 0xFF), 16));
    } 
    return hexBuffer.toString();
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
    String formatted;
    float f = index;
    if (f >= 1000.0F && f < 1000000.0F) {
      f /= 1000.0F;
      formatted = (new DecimalFormat("#.#K")).format(f);
    } else if (f >= 1000000.0F && f < 1.0E9F) {
      f /= 1000000.0F;
      formatted = (new DecimalFormat("#.#M")).format(f);
    } else if (f >= 1.0E9F && f < 1.0E12F) {
      f /= 1.0E9F;
      formatted = (new DecimalFormat("#.#G")).format(f);
    } else {
      formatted = String.valueOf(index);
    } 
    return formatted;
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
      String token = scanner.nextToken();
      if (token.length() > 0)
        arrayList.add(token); 
    } 
    return arrayList;
  }
  
  public static String unescapeString(String value) {
    String result = "";
    for (int b = 0; b < value.length(); b++) {
      char c = value.charAt(b);
      if (c == '\\') {
        StringBuilder charBuilder = new StringBuilder();
        charBuilder.append(result);
        charBuilder.append(value.charAt(++b));
        result = charBuilder.toString();
      } else {
        StringBuilder charBuilder = new StringBuilder();
        charBuilder.append(result);
        charBuilder.append(c);
        result = charBuilder.toString();
      } 
    } 
    return result;
  }
  
  public static class StringScanner {
    public String input;
    
    public int position;
    
    public StringScanner(String input) {
      this.input = input;
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
      String token = "";
      if (atEnd)
        return ""; 
      boolean hasPercent = false;
      int i = this.position;
      while (this.position < this.input.length() && this.input.charAt(this.position) != ' ') {
        char c = this.input.charAt(this.position);
        if (c == '\\') {
          StringBuilder escapeBuilder = new StringBuilder();
          escapeBuilder.append(token);
          escapeBuilder.append(c + this.input.charAt(this.position + 1));
          token = escapeBuilder.toString();
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
        StringBuilder tokenBuilder = new StringBuilder();
        tokenBuilder.append(token);
        tokenBuilder.append(c);
        token = tokenBuilder.toString();
        this.position++;
      } 
      return token;
    }
    
    public void skipSpaces() {
      while (this.position < this.input.length() && this.input.charAt(this.position) == ' ')
        this.position++; 
    }
  }
}
