package a.a.a;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class nA {
  public String toString(nA paramnA) {
    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append("[");
    for (Field field : paramnA.getClass().getFields()) {
      if (!Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()))
        try {
          stringBuffer.append(field.getName());
          stringBuffer.append("=");
          stringBuffer.append(field.get(paramnA));
          stringBuffer.append(",");
        } catch (Exception exception) {
          exception.printStackTrace();
        }  
    } 
    stringBuffer.append("]");
    return stringBuffer.toString();
  }
}
