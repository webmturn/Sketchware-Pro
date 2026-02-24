package pro.sketchware.core;

import java.util.ArrayList;

public class CompileException extends Exception {
  public ArrayList<String> errorDetails;
  
  public CompileException(String paramString) {
    super(paramString);
  }
  
  public ArrayList<String> a() {
    return this.errorDetails;
  }
  
  public void a(ArrayList<String> paramArrayList) {
    this.errorDetails = paramArrayList;
  }
}
