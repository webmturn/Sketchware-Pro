package pro.sketchware.core;

import java.util.ArrayList;

public class CompileException extends Exception {
  public ArrayList<String> errorDetails;
  
  public CompileException(String message) {
    super(message);
  }
  
  public ArrayList<String> getErrorDetails() {
    return errorDetails;
  }
  
  public void setErrorDetails(ArrayList<String> errorDetails) {
    this.errorDetails = errorDetails;
  }
}
