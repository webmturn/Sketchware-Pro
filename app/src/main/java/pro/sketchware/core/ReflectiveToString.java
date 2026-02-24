package pro.sketchware.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectiveToString {
  public String toString(ReflectiveToString reflectiveToString) {
    StringBuffer resultBuffer = new StringBuffer();
    resultBuffer.append("[");
    for (Field field : reflectiveToString.getClass().getFields()) {
      if (!Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()))
        try {
          resultBuffer.append(field.getName());
          resultBuffer.append("=");
          resultBuffer.append(field.get(reflectiveToString));
          resultBuffer.append(",");
        } catch (Exception exception) {
          exception.printStackTrace();
        }  
    } 
    resultBuffer.append("]");
    return resultBuffer.toString();
  }
}
