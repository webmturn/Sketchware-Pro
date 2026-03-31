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
  
  public String formatTimestamp(long timestampMillis, String pattern) {
    return (new SimpleDateFormat(pattern, Locale.ENGLISH)).format(new Date(timestampMillis));
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
  
  public String convertTimezone(String dateText, String pattern) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
    String convertedDateText = dateText;
    try {
      long timeMillis = simpleDateFormat.parse(dateText).getTime();
      long timezoneOffset = TimeZone.getDefault().getOffset(timeMillis);
      Date date = new Date();
      date.setTime(timeMillis + timezoneOffset);
      convertedDateText = simpleDateFormat.format(date);
    } catch (Exception exception) {
      Log.w("DateTimeUtil", "Failed to convert timezone", exception);
    }
    return convertedDateText;
  }
  
  public String reformatDate(String dateText, String inputPattern, String outputPattern) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(inputPattern);
    String reformattedDateText = dateText;
    try {
      long timeMillis = simpleDateFormat.parse(dateText).getTime();
      long timezoneOffset = TimeZone.getDefault().getOffset(timeMillis);
      Date date = new Date();
      date.setTime(timeMillis + timezoneOffset);
      simpleDateFormat = new SimpleDateFormat(outputPattern);
      reformattedDateText = simpleDateFormat.format(date);
    } catch (Exception exception) {
      Log.w("DateTimeUtil", "Failed to reformat date", exception);
    }
    return reformattedDateText;
  }
  
  public long parseToMillis(String dateText, String pattern) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.ENGLISH);
    try {
      return simpleDateFormat.parse(dateText).getTime();
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
