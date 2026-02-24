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
  public final String TAG = "dataParser";
  
  public String fileName;
  
  public String eventKey;
  
  public DataType dataType;
  
  public Gson gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
  
  public ProjectDataParser(String key) throws Exception {
    try {
      parseKey(key);
      return;
    } catch (Exception exception) {
      throw exception;
    } 
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
    return this.dataType;
  }
  
  public <T> T parseData(String data) {
    switch (KeyboardSettingConstants.VALUES[this.dataType.ordinal()]) {
      default:
        return null;
      case 8:
        return (T)parseBlockBeans(this.gson, data);
      case 7:
        return (T)parseMoreBlockFunctions(data);
      case 6:
        return (T)parseEventBeans(data);
      case 5:
        return (T)parseComponentBeans(data);
      case 4:
        return (T)parseListVariables(data);
      case 3:
        return (T)parseVariables(data);
      case 2:
        return (T)parseFabViewBean(data);
      case 1:
        break;
    } 
    return (T)parseViewBeans(this.gson, data);
  }
  
  public String getFileName() {
    return this.fileName;
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
        ComponentBean bean = this.gson.fromJson(line, ComponentBean.class);
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
    return this.eventKey;
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
        EventBean bean = this.gson.fromJson(line, EventBean.class);
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
    return (data.trim().length() <= 0 || data.trim().charAt(0) != '{') ? new ViewBean("_fab", 16) : (ViewBean)this.gson.fromJson(data, ViewBean.class);
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
      int i = keyPart.indexOf(".xml") + 4;
      this.fileName = keyPart.substring(0, i);
      if (keyPart.length() == i) {
        this.dataType = ProjectDataParser.DataType.VIEW;
      } else {
        keyPart = keyPart.substring(i);
        if (keyPart.charAt(0) == '_') {
          if (keyPart.substring(1).equals("fab")) {
            this.dataType = ProjectDataParser.DataType.FAB;
          } else {
            throw new Exception("invalid key : Unknown type string");
          } 
        } else {
          throw new Exception("invalid key : No separator");
        } 
      } 
    } else {
      if (keyPart.contains(".java")) {
        int i = keyPart.indexOf(".java") + 5;
        this.fileName = keyPart.substring(0, i);
        if (keyPart.length() != i) {
          keyPart = keyPart.substring(i);
          if (keyPart.charAt(0) == '_') {
            keyPart = keyPart.substring(1);
            i = -1;
            switch (keyPart.hashCode()) {
              case 3322014:
                if (keyPart.equals("list"))
                  i = 1; 
                break;
              case 3154628:
                if (keyPart.equals("func"))
                  i = 4; 
                break;
              case 116519:
                if (keyPart.equals("var"))
                  i = 0; 
                break;
              case -447446250:
                if (keyPart.equals("components"))
                  i = 2; 
                break;
              case -1291329255:
                if (keyPart.equals("events"))
                  i = 3; 
                break;
            } 
            if (i != 0) {
              if (i != 1) {
                if (i != 2) {
                  if (i != 3) {
                    if (i != 4) {
                      this.dataType = ProjectDataParser.DataType.EVENT_BLOCK;
                      this.eventKey = keyPart;
                    } else {
                      this.dataType = ProjectDataParser.DataType.MORE_BLOCK;
                    } 
                  } else {
                    this.dataType = ProjectDataParser.DataType.EVENT;
                  } 
                } else {
                  this.dataType = ProjectDataParser.DataType.COMPONENT;
                } 
              } else {
                this.dataType = ProjectDataParser.DataType.LIST;
              } 
            } else {
              this.dataType = ProjectDataParser.DataType.VARIABLE;
            } 
            return;
          } 
          throw new Exception("invalid key : No separator");
        } 
        throw new Exception("invalid key : No data type");
      } 
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
    VIEW, FAB, VARIABLE, LIST, COMPONENT, EVENT, MORE_BLOCK, EVENT_BLOCK;
    
    public static final DataType[] VALUES = new DataType[] { VIEW, FAB, VARIABLE, LIST, COMPONENT, EVENT, MORE_BLOCK, EVENT_BLOCK };
  }
}
