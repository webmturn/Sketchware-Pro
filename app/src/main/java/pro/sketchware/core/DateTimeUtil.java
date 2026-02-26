package pro.sketchware.core;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimeUtil {
  public long currentTimeMillis() {
    return (new Date()).getTime();
  }
  
  public String formatTimestamp(long timeout, String pattern) {
    return (new SimpleDateFormat(pattern, Locale.ENGLISH)).format(new Date(timeout));
  }
  
  public String formatCurrentTime(String pattern) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
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
      long timeMillis = simpleDateFormat.parse(key).getTime();
      long timezoneOffset = TimeZone.getDefault().getOffset(timeMillis);
      Date date = new Date();
      date.setTime(timeMillis + timezoneOffset);
      String formatted = simpleDateFormat.format(date);
      key = formatted;
    } catch (Exception exception) {
      Log.w("DateTimeUtil", "Failed to convert timezone", exception);
    }
    return key;
  }
  
  public String reformatDate(String key, String value, String extra) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(value);
    try {
      long timeMillis = simpleDateFormat.parse(key).getTime();
      long timezoneOffset = TimeZone.getDefault().getOffset(timeMillis);
      Date date = new Date();
      date.setTime(timeMillis + timezoneOffset);
      simpleDateFormat = new SimpleDateFormat(extra);
      String formatted = simpleDateFormat.format(date);
      key = formatted;
    } catch (Exception exception) {
      Log.w("DateTimeUtil", "Failed to reformat date", exception);
    }
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
  
  public String formatCurrentTimeGmt(String pattern) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    try {
      Date date = new Date();
      return simpleDateFormat.format(date);
    } catch (Exception exception) {
      return null;
    } 
  }
}
