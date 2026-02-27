package pro.sketchware.core;

import android.util.Pair;
import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.EventBean;
import com.besome.sketch.beans.ViewBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;

public class ProjectDataParser {
  private String fileName;
  
  private String eventKey;
  
  private DataType dataType;
  
  private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
  
  public ProjectDataParser(String key) throws Exception {
    parseKey(key);
  }
  
  public static ArrayList<BlockBean> parseBlockBeans(Gson gson, String data) {
    ArrayList<BlockBean> result = new ArrayList<>();
    java.io.BufferedReader reader = null;
    try {
      reader = new java.io.BufferedReader(new java.io.StringReader(data));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().length() <= 0) continue;
        if (line.trim().charAt(0) != '{') continue;
        result.add(gson.fromJson(line, BlockBean.class));
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (reader != null) try { reader.close(); } catch (Exception e) { e.printStackTrace(); }
    }
    return result;
  }
  
  public static ArrayList<ViewBean> parseViewBeans(Gson gson, String data) {
    ArrayList<ViewBean> result = new ArrayList<>();
    java.io.BufferedReader reader = null;
    try {
      reader = new java.io.BufferedReader(new java.io.StringReader(data));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().length() <= 0) continue;
        if (line.trim().charAt(0) != '{') continue;
        result.add(gson.fromJson(line, ViewBean.class));
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (reader != null) try { reader.close(); } catch (Exception e) { e.printStackTrace(); }
    }
    return result;
  }
  
  public DataType getDataType() {
    return dataType;
  }
  
  @SuppressWarnings("unchecked")
  public <T> T parseData(String data) {
    switch (dataType) {
      case VIEW:        return (T) parseViewBeans(gson, data);
      case FAB:         return (T) parseFabViewBean(data);
      case VARIABLE:    return (T) parseVariables(data);
      case LIST:        return (T) parseListVariables(data);
      case COMPONENT:   return (T) parseComponentBeans(data);
      case EVENT:       return (T) parseEventBeans(data);
      case MORE_BLOCK:  return (T) parseMoreBlockFunctions(data);
      case EVENT_BLOCK: return (T) parseBlockBeans(gson, data);
      default:          return null;
    }
  }
  
  public String getFileName() {
    return fileName;
  }
  
  public ArrayList<ComponentBean> parseComponentBeans(String data) {
    ArrayList<ComponentBean> result = new ArrayList<>();
    java.io.BufferedReader reader = null;
    try {
      reader = new java.io.BufferedReader(new java.io.StringReader(data));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().length() <= 0) continue;
        if (line.trim().charAt(0) != '{') continue;
        ComponentBean bean = gson.fromJson(line, ComponentBean.class);
        bean.initValue();
        result.add(bean);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (reader != null) try { reader.close(); } catch (Exception e) { e.printStackTrace(); }
    }
    return result;
  }
  
  public String getEventKey() {
    return eventKey;
  }
  
  public ArrayList<EventBean> parseEventBeans(String data) {
    ArrayList<EventBean> result = new ArrayList<>();
    java.io.BufferedReader reader = null;
    try {
      reader = new java.io.BufferedReader(new java.io.StringReader(data));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().length() <= 0) continue;
        if (line.charAt(0) != '{') continue;
        EventBean bean = gson.fromJson(line, EventBean.class);
        bean.initValue();
        result.add(bean);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (reader != null) try { reader.close(); } catch (Exception e) { e.printStackTrace(); }
    }
    return result;
  }
  
  public ViewBean parseFabViewBean(String data) {
    return (data.trim().length() <= 0 || data.trim().charAt(0) != '{') ? new ViewBean("_fab", 16) : gson.fromJson(data, ViewBean.class);
  }
  
  public ArrayList<Pair<String, String>> parseMoreBlockFunctions(String data) {
    ArrayList<Pair<String, String>> result = new ArrayList<>();
    java.io.BufferedReader reader = null;
    try {
      reader = new java.io.BufferedReader(new java.io.StringReader(data));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().length() <= 0) continue;
        if (!line.contains(":")) continue;
        String key = line.substring(0, line.indexOf(":"));
        String value = line.substring(line.indexOf(":") + 1);
        result.add(new Pair<>(key, value));
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (reader != null) try { reader.close(); } catch (Exception e) { e.printStackTrace(); }
    }
    return result;
  }
  
  public final void parseKey(String rawKey) throws Exception {
    String keyPart = rawKey.trim();
    if (keyPart.contains(".xml")) {
      int extEndIdx = keyPart.indexOf(".xml") + 4;
      fileName = keyPart.substring(0, extEndIdx);
      if (keyPart.length() == extEndIdx) {
        dataType = DataType.VIEW;
      } else {
        keyPart = keyPart.substring(extEndIdx);
        if (keyPart.charAt(0) == '_' && keyPart.substring(1).equals("fab")) {
          dataType = DataType.FAB;
        } else if (keyPart.charAt(0) != '_') {
          throw new Exception("invalid key : No separator");
        } else {
          throw new Exception("invalid key : Unknown type string");
        }
      }
    } else if (keyPart.contains(".java")) {
      int extEndIdx = keyPart.indexOf(".java") + 5;
      fileName = keyPart.substring(0, extEndIdx);
      if (keyPart.length() == extEndIdx) {
        throw new Exception("invalid key : No data type");
      }
      keyPart = keyPart.substring(extEndIdx);
      if (keyPart.charAt(0) != '_') {
        throw new Exception("invalid key : No separator");
      }
      keyPart = keyPart.substring(1);
      switch (keyPart) {
        case "var":        dataType = DataType.VARIABLE;  break;
        case "list":       dataType = DataType.LIST;      break;
        case "components": dataType = DataType.COMPONENT; break;
        case "events":     dataType = DataType.EVENT;     break;
        case "func":       dataType = DataType.MORE_BLOCK; break;
        default:
          dataType = DataType.EVENT_BLOCK;
          eventKey = keyPart;
          break;
      }
    } else {
      throw new Exception("invalid key : No filename");
    }
  }
  
  public ArrayList<Pair<Integer, String>> parseListVariables(String data) {
    ArrayList<Pair<Integer, String>> result = new ArrayList<>();
    java.io.BufferedReader reader = null;
    try {
      reader = new java.io.BufferedReader(new java.io.StringReader(data));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().length() <= 0) continue;
        if (!line.contains(":")) continue;
        String key = line.substring(0, line.indexOf(":"));
        String value = line.substring(line.indexOf(":") + 1);
        result.add(new Pair<>(Integer.valueOf(key), value));
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (reader != null) try { reader.close(); } catch (Exception e) { e.printStackTrace(); }
    }
    return result;
  }
  
  public ArrayList<Pair<Integer, String>> parseVariables(String data) {
    ArrayList<Pair<Integer, String>> result = new ArrayList<>();
    java.io.BufferedReader reader = null;
    try {
      reader = new java.io.BufferedReader(new java.io.StringReader(data));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().length() <= 0) continue;
        if (!line.contains(":")) continue;
        String key = line.substring(0, line.indexOf(":"));
        String value = line.substring(line.indexOf(":") + 1);
        result.add(new Pair<>(Integer.valueOf(key), value));
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (reader != null) try { reader.close(); } catch (Exception e) { e.printStackTrace(); }
    }
    return result;
  }
  
  public enum DataType {
    VIEW, FAB, VARIABLE, LIST, COMPONENT, EVENT, MORE_BLOCK, EVENT_BLOCK
  }
}
