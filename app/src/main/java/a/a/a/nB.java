package a.a.a;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class nB {
  public long a() {
    return (new Date()).getTime();
  }
  
  public String a(long paramLong, String paramString) {
    return (new SimpleDateFormat(paramString, Locale.ENGLISH)).format(new Date(paramLong));
  }
  
  public String a(String paramString) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(paramString);
    try {
      Date date = new Date();
      return simpleDateFormat.format(date);
    } catch (Exception exception) {
      return null;
    } 
  }
  
  public String a(String paramString1, String paramString2) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(paramString2);
    try {
      long l1 = simpleDateFormat.parse(paramString1).getTime();
      long l2 = TimeZone.getDefault().getOffset(l1);
      Date date = new Date();
      date.setTime(l1 + l2);
      String str = simpleDateFormat.format(date);
      paramString1 = str;
    } catch (Exception exception) {}
    return paramString1;
  }
  
  public String a(String paramString1, String paramString2, String paramString3) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(paramString2);
    try {
      long l1 = simpleDateFormat.parse(paramString1).getTime();
      long l2 = TimeZone.getDefault().getOffset(l1);
      Date date = new Date();
      date.setTime(l1 + l2);
      simpleDateFormat = new SimpleDateFormat(paramString3);
      String str = simpleDateFormat.format(date);
      paramString1 = str;
    } catch (Exception exception) {}
    return paramString1;
  }
  
  public long b(String paramString1, String paramString2) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(paramString2, Locale.ENGLISH);
    try {
      return simpleDateFormat.parse(paramString1).getTime();
    } catch (ParseException parseException) {
      return 0L;
    } 
  }
  
  public String b(String paramString) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(paramString);
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    try {
      Date date = new Date();
      return simpleDateFormat.format(date);
    } catch (Exception exception) {
      return null;
    } 
  }
}
