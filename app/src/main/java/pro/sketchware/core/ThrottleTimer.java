package pro.sketchware.core;

public class ThrottleTimer {
  public static boolean isThrottled;
  
  public static long[] timestamps = new long[10];
  
  public static long getElapsedTime(int value) {
    if (timestamps == null)
      timestamps = new long[10]; 
    return System.currentTimeMillis() - timestamps[value];
  }
  
  public static boolean isExpired(int value) {
    if (timestamps == null)
      timestamps = new long[10]; 
    long l = getElapsedTime(value);
    return (timestamps[value] == 0L || l > 30000L);
  }
}
