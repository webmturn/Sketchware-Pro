package pro.sketchware.core;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectiveToString {
  public String toString(ReflectiveToString reflectiveToString) {
    StringBuilder resultBuffer = new StringBuilder();
    resultBuffer.append("[");
    for (Field field : reflectiveToString.getClass().getFields()) {
      if (!Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()))
        try {
          resultBuffer.append(field.getName());
          resultBuffer.append("=");
          resultBuffer.append(field.get(reflectiveToString));
          resultBuffer.append(",");
        } catch (Exception e) {
          Log.w("ReflectiveToString", "Failed to get field value", e);
        }  
    } 
    resultBuffer.append("]");
    return resultBuffer.toString();
  }
}
