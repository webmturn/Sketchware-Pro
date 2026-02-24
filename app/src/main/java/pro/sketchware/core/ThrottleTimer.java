package pro.sketchware.core;

public class ThrottleTimer {
  public static boolean isThrottled;
  
  public static long[] timestamps = new long[10];
  
  public static long getElapsedTime(int paramInt) {
    if (timestamps == null)
      timestamps = new long[10]; 
    return System.currentTimeMillis() - timestamps[paramInt];
  }
  
  public static boolean isExpired(int paramInt) {
    if (timestamps == null)
      timestamps = new long[10]; 
    long l = getElapsedTime(paramInt);
    return (timestamps[paramInt] == 0L || l > 30000L);
  }
}
