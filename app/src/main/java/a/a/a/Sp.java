package a.a.a;

public class Sp {
  public static boolean a;
  
  public static long[] b = new long[10];
  
  public static long a(int paramInt) {
    if (b == null)
      b = new long[10]; 
    return System.currentTimeMillis() - b[paramInt];
  }
  
  public static boolean b(int paramInt) {
    if (b == null)
      b = new long[10]; 
    long l = a(paramInt);
    return (b[paramInt] == 0L || l > 30000L);
  }
}
