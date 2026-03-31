package pro.sketchware.core;

import android.util.Log;
import android.util.Pair;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.EventBean;
import com.besome.sketch.beans.ViewBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public class ProjectDataParser {
  private String fileName;
  
  private String blockKey;
  
  private DataType dataType;
  
  private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
  
  public ProjectDataParser(String sectionKey) {
    parseKey(sectionKey);
  }
  
  public static ArrayList<BlockBean> parseBlockBeans(Gson gson, String sectionContent) {
    ArrayList<BlockBean> result = new ArrayList<>();
    try (JsonReader reader = new JsonReader(new StringReader(sectionContent))) {
      reader.setLenient(true);
      while (reader.peek() != JsonToken.END_DOCUMENT) {
        result.add(gson.fromJson(reader, BlockBean.class));
      }
    } catch (IOException | RuntimeException e) {
      Log.w("ProjectDataParser", "Failed to parse block beans", e);
    }
    return result;
  }
  
  public static ArrayList<ViewBean> parseViewBeans(Gson gson, String sectionContent) {
    ArrayList<ViewBean> result = new ArrayList<>();
    try (JsonReader reader = new JsonReader(new StringReader(sectionContent))) {
      reader.setLenient(true);
      while (reader.peek() != JsonToken.END_DOCUMENT) {
        result.add(gson.fromJson(reader, ViewBean.class));
      }
    } catch (IOException | RuntimeException e) {
      Log.w("ProjectDataParser", "Failed to parse view beans", e);
    }
    return result;
  }
  
  public DataType getDataType() {
    return dataType;
  }
  
  @SuppressWarnings("unchecked")
  public <T> T parseData(String sectionContent) {
    switch (dataType) {
      case VIEW:        return (T) parseViewBeans(gson, sectionContent);
      case FAB:         return (T) parseFabViewBean(sectionContent);
      case VARIABLE:    return (T) parseVariables(sectionContent);
      case LIST:        return (T) parseListVariables(sectionContent);
      case COMPONENT:   return (T) parseComponentBeans(sectionContent);
      case EVENT:       return (T) parseEventBeans(sectionContent);
      case MORE_BLOCK:  return (T) parseMoreBlockFunctions(sectionContent);
      case EVENT_BLOCK: return (T) parseBlockBeans(gson, sectionContent);
      default:          return null;
    }
  }
  
  public String getFileName() {
    return fileName;
  }
  
  public ArrayList<ComponentBean> parseComponentBeans(String sectionContent) {
    ArrayList<ComponentBean> result = new ArrayList<>();
    try (JsonReader reader = new JsonReader(new StringReader(sectionContent))) {
      reader.setLenient(true);
      while (reader.peek() != JsonToken.END_DOCUMENT) {
        ComponentBean parsedComponent = gson.fromJson(reader, ComponentBean.class);
        parsedComponent.initValue();
        result.add(parsedComponent);
      }
    } catch (IOException | RuntimeException e) {
      Log.w("ProjectDataParser", "Failed to parse component beans", e);
    }
    return result;
  }
  
  public String getBlockKey() {
    return blockKey;
  }
  
  public ArrayList<EventBean> parseEventBeans(String sectionContent) {
    ArrayList<EventBean> result = new ArrayList<>();
    try (JsonReader reader = new JsonReader(new StringReader(sectionContent))) {
      reader.setLenient(true);
      while (reader.peek() != JsonToken.END_DOCUMENT) {
        EventBean parsedEvent = gson.fromJson(reader, EventBean.class);
        parsedEvent.initValue();
        result.add(parsedEvent);
      }
    } catch (IOException | RuntimeException e) {
      Log.w("ProjectDataParser", "Failed to parse event beans", e);
    }
    return result;
  }
  
  public ViewBean parseFabViewBean(String sectionContent) {
    return (sectionContent.trim().length() <= 0 || sectionContent.trim().charAt(0) != '{') ? new ViewBean("_fab", 16) : gson.fromJson(sectionContent, ViewBean.class);
  }
  
  public ArrayList<Pair<String, String>> parseMoreBlockFunctions(String sectionContent) {
    ArrayList<Pair<String, String>> result = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new StringReader(sectionContent))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().length() <= 0) continue;
        if (!line.contains(":")) continue;
        String moreBlockName = line.substring(0, line.indexOf(":"));
        String moreBlockSpec = line.substring(line.indexOf(":") + 1);
        result.add(new Pair<>(moreBlockName, moreBlockSpec));
      }
    } catch (IOException | RuntimeException e) {
      Log.w("ProjectDataParser", "Failed to parse more block functions", e);
    }
    return result;
  }
  
  public final void parseKey(String rawSectionKey) {
    String sectionKey = rawSectionKey.trim();
    if (sectionKey.contains(".xml")) {
      int extEndIdx = sectionKey.indexOf(".xml") + 4;
      fileName = sectionKey.substring(0, extEndIdx);
      if (sectionKey.length() == extEndIdx) {
        dataType = DataType.VIEW;
      } else {
        sectionKey = sectionKey.substring(extEndIdx);
        if (sectionKey.isEmpty()) {
          throw new IllegalArgumentException("invalid key : No separator");
        }
        if (sectionKey.charAt(0) == '_' && sectionKey.substring(1).equals("fab")) {
          dataType = DataType.FAB;
        } else if (sectionKey.charAt(0) != '_') {
          throw new IllegalArgumentException("invalid key : No separator");
        } else {
          throw new IllegalArgumentException("invalid key : Unknown type string");
        }
      }
    } else if (sectionKey.contains(".java")) {
      int extEndIdx = sectionKey.indexOf(".java") + 5;
      fileName = sectionKey.substring(0, extEndIdx);
      if (sectionKey.length() == extEndIdx) {
        throw new IllegalArgumentException("invalid key : No data type");
      }
      sectionKey = sectionKey.substring(extEndIdx);
      if (sectionKey.isEmpty()) {
        throw new IllegalArgumentException("invalid key : No separator");
      }
      if (sectionKey.charAt(0) != '_') {
        throw new IllegalArgumentException("invalid key : No separator");
      }
      sectionKey = sectionKey.substring(1);
      switch (sectionKey) {
        case "var":        dataType = DataType.VARIABLE;  break;
        case "list":       dataType = DataType.LIST;      break;
        case "components": dataType = DataType.COMPONENT; break;
        case "events":     dataType = DataType.EVENT;     break;
        case "func":       dataType = DataType.MORE_BLOCK; break;
        default:
          dataType = DataType.EVENT_BLOCK;
          blockKey = sectionKey;
          break;
      }
    } else {
      throw new IllegalArgumentException("invalid key : No filename");
    }
  }
  
  public ArrayList<Pair<Integer, String>> parseListVariables(String sectionContent) {
    ArrayList<Pair<Integer, String>> result = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new StringReader(sectionContent))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().length() <= 0) continue;
        if (!line.contains(":")) continue;
        String listType = line.substring(0, line.indexOf(":"));
        String listName = line.substring(line.indexOf(":") + 1);
        result.add(new Pair<>(Integer.valueOf(listType), listName));
      }
    } catch (IOException | RuntimeException e) {
      Log.w("ProjectDataParser", "Failed to parse list variables", e);
    }
    return result;
  }
  
  public ArrayList<Pair<Integer, String>> parseVariables(String sectionContent) {
    ArrayList<Pair<Integer, String>> result = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new StringReader(sectionContent))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().length() <= 0) continue;
        if (!line.contains(":")) continue;
        String variableType = line.substring(0, line.indexOf(":"));
        String variableName = line.substring(line.indexOf(":") + 1);
        result.add(new Pair<>(Integer.valueOf(variableType), variableName));
      }
    } catch (IOException | RuntimeException e) {
      Log.w("ProjectDataParser", "Failed to parse variables", e);
    }
    return result;
  }
  
  public enum DataType {
    VIEW, FAB, VARIABLE, LIST, COMPONENT, EVENT, MORE_BLOCK, EVENT_BLOCK
  }
}
