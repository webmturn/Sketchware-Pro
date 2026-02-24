package pro.sketchware.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimeUtil {
  public long currentTimeMillis() {
    return (new Date()).getTime();
  }
  
  public String formatTimestamp(long timeout, String str) {
    return (new SimpleDateFormat(str, Locale.ENGLISH)).format(new Date(timeout));
  }
  
  public String formatCurrentTime(String str) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(str);
    try {
      Date date = new Date();
      return simpleDateFormat.format(date);
    } catch (Exception exception) {
      return null;
    } 
  }
  
  public String convertTimezone(String key, String value) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(value);
    try {
      long l1 = simpleDateFormat.parse(key).getTime();
      long l2 = TimeZone.getDefault().getOffset(l1);
      Date date = new Date();
      date.setTime(l1 + l2);
      String str = simpleDateFormat.format(date);
      key = str;
    } catch (Exception exception) {}
    return key;
  }
  
  public String reformatDate(String key, String value, String extra) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(value);
    try {
      long l1 = simpleDateFormat.parse(key).getTime();
      long l2 = TimeZone.getDefault().getOffset(l1);
      Date date = new Date();
      date.setTime(l1 + l2);
      simpleDateFormat = new SimpleDateFormat(extra);
      String str = simpleDateFormat.format(date);
      key = str;
    } catch (Exception exception) {}
    return key;
  }
  
  public long parseToMillis(String key, String value) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(value, Locale.ENGLISH);
    try {
      return simpleDateFormat.parse(key).getTime();
    } catch (ParseException parseException) {
      return 0L;
    } 
  }
  
  public String formatCurrentTimeGmt(String str) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(str);
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    try {
      Date date = new Date();
      return simpleDateFormat.format(date);
    } catch (Exception exception) {
      return null;
    } 
  }
}
