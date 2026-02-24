package pro.sketchware.core;

import java.util.ArrayList;

public class CompileException extends Exception {
  public ArrayList<String> errorDetails;
  
  public CompileException(String paramString) {
    super(paramString);
  }
  
  public ArrayList<String> getErrorDetails() {
    return this.errorDetails;
  }
  
  public void setErrorDetails(ArrayList<String> paramArrayList) {
    this.errorDetails = paramArrayList;
  }
}
