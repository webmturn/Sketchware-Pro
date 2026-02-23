package pro.sketchware.core;

import java.util.ArrayList;

public class CompileException extends Exception {
  public ArrayList<String> a;
  
  public CompileException(String paramString) {
    super(paramString);
  }
  
  public ArrayList<String> a() {
    return this.a;
  }
  
  public void a(ArrayList<String> paramArrayList) {
    this.a = paramArrayList;
  }
}
