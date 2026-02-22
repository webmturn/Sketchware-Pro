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


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\Sp.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */