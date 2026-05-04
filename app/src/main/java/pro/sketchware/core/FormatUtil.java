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
      return ch - '0';
    char baseChar = 'A';
    if (ch < 'A' || ch > 'F') {
      baseChar = 'a';
      if (ch < 'a' || ch > 'f') {
        throw new IllegalArgumentException("invalid hex digit '" + ch + "'");
      } 
    } 
    return ch - baseChar + 10;
  }
  
  public static String generateRandomId() {
    return String.valueOf(10000 + new Random().nextInt(90000));
  }
  
  public static String formatFileSize(int byteCount) {
    String formatted;
    if (byteCount < 0)
      return "0"; 
    float sizeValue = byteCount;
    if (sizeValue >= 1024.0F && sizeValue < 1048576.0F) {
      sizeValue /= 1024.0F;
      formatted = (new DecimalFormat("#.#KB")).format(sizeValue);
    } else if (sizeValue >= 1048576.0F && sizeValue < 1.07374182E9F) {
      sizeValue /= 1048576.0F;
      formatted = (new DecimalFormat("#.#MB")).format(sizeValue);
    } else if (sizeValue >= 1.07374182E9F && sizeValue < 1.09951163E12F) {
      sizeValue /= 1.07374182E9F;
      formatted = (new DecimalFormat("#.#GB")).format(sizeValue);
    } else {
      formatted = byteCount + "B";
    } 
    return formatted;
  }
  
  public static String bytesToHex(byte[] data) {
    StringBuilder hexBuffer = new StringBuilder(data.length * 2);
    for (int i = 0; i < data.length; i++) {
      if ((data[i] & 0xFF) < 16)
        hexBuffer.append("0"); 
      hexBuffer.append(Long.toString((data[i] & 0xFF), 16));
    } 
    return hexBuffer.toString();
  }
  
  public static void copyToClipboard(Context context, String label, String text) {
    ((ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(label, text));
  }
  
  public static byte[] hexStringToBytes(String hexString) {
    int length = hexString.length();
    byte[] bytes = new byte[(length + 1) / 2];
    int charIdx = 0;
    byte byteIdx = 1;
    if (length % 2 == 1) {
      bytes[0] = (byte)hexCharToInt(hexString.charAt(0));
      charIdx = 1;
    } else {
      byteIdx = 0;
    } 
    while (charIdx < length) {
      int nextCharIdx = charIdx + 1;
      bytes[byteIdx] = (byte)(hexCharToInt(hexString.charAt(charIdx)) << 4 | hexCharToInt(hexString.charAt(nextCharIdx)));
      byteIdx++;
      charIdx = nextCharIdx + 1;
    } 
    return bytes;
  }
  
  public static String formatNumber(int value) {
    String formatted;
    float numValue = value;
    if (numValue >= 1000.0F && numValue < 1000000.0F) {
      numValue /= 1000.0F;
      formatted = (new DecimalFormat("#.#K")).format(numValue);
    } else if (numValue >= 1000000.0F && numValue < 1.0E9F) {
      numValue /= 1000000.0F;
      formatted = (new DecimalFormat("#.#M")).format(numValue);
    } else if (numValue >= 1.0E9F && numValue < 1.0E12F) {
      numValue /= 1.0E9F;
      formatted = (new DecimalFormat("#.#G")).format(numValue);
    } else {
      formatted = String.valueOf(value);
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

  public static int safeParseInt(String value, int defaultValue) {
    if (value == null || value.isEmpty()) return defaultValue;
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public static float safeParseFloat(String value, float defaultValue) {
    if (value == null || value.isEmpty()) return defaultValue;
    try {
      return Float.parseFloat(value.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
  
  public static String formatWithCommas(int value) {
    return (new DecimalFormat("#,###")).format(value);
  }
  
  public static ArrayList<String> parseBlockSpec(String blockSpec) {
    ArrayList<String> tokens = new ArrayList<>();
    StringScanner scanner = new StringScanner(blockSpec);
    while (!scanner.isAtEnd()) {
      String token = scanner.nextToken();
      if (token.length() > 0)
        tokens.add(token); 
    } 
    return tokens;
  }
  
  public static String unescapeString(String value) {
    String result = "";
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '\\') {
        if (++i < value.length()) {
          result = result + value.charAt(i);
        }
      } else {
        result = result + c;
      } 
    } 
    return result;
  }
  
  public static class StringScanner {
    public String input;
    
    public int position;
    
    public StringScanner(String input) {
      this.input = input;
      position = 0;
    }
    
    public boolean isAtEnd() {
      return position >= input.length();
    }
    
    public String nextToken() {
      skipSpaces();
      boolean atEnd = isAtEnd();
      String token = "";
      if (atEnd)
        return ""; 
      boolean hasPercent = false;
      int startPos = position;
      while (position < input.length() && input.charAt(position) != ' ') {
        char c = input.charAt(position);
        if (c == '\\') {
          token = token + (c + input.charAt(position + 1));
          position += 2;
          continue;
        } 
        if (c == '%') {
          if (position > startPos)
            break; 
          hasPercent = true;
        } 
        if (hasPercent && (c == '?' || c == '-'))
          break; 
        token = token + c;
        position++;
      } 
      return token;
    }
    
    public void skipSpaces() {
      while (position < input.length() && input.charAt(position) == ' ')
        position++; 
    }
  }
}
