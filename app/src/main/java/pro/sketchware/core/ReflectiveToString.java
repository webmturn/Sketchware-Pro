package pro.sketchware.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectiveToString {
  public String toString(ReflectiveToString paramReflectiveToString) {
    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append("[");
    for (Field field : paramReflectiveToString.getClass().getFields()) {
      if (!Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()))
        try {
          stringBuffer.append(field.getName());
          stringBuffer.append("=");
          stringBuffer.append(field.get(paramReflectiveToString));
          stringBuffer.append(",");
        } catch (Exception exception) {
          exception.printStackTrace();
        }  
    } 
    stringBuffer.append("]");
    return stringBuffer.toString();
  }
}
