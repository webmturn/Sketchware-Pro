package pro.sketchware.core;

import android.util.Log;
import android.util.Pair;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.EventBean;
import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.beans.ViewBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProjectDataStore {
  public String projectId;
  public EncryptedFileUtil fileUtil;
  public HashMap<String, ArrayList<ViewBean>> viewMap;
  public HashMap<String, HashMap<String, ArrayList<BlockBean>>> blockMap;
  public HashMap<String, ArrayList<Pair<Integer, String>>> variableMap;
  public HashMap<String, ArrayList<Pair<Integer, String>>> listMap;
  public HashMap<String, ArrayList<Pair<String, String>>> moreBlockMap;
  public HashMap<String, ArrayList<ComponentBean>> componentMap;
  public HashMap<String, ArrayList<EventBean>> eventMap;
  public HashMap<String, ViewBean> fabMap;
  public Gson gson;
  public BuildConfig buildConfig;
  
  public ProjectDataStore(String projectId) {
    clearAllData();
    this.projectId = projectId;
    fileUtil = new EncryptedFileUtil();
    gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    buildConfig = new BuildConfig();
  }
  
  public static ArrayList<ViewBean> getSortedRootViews(ArrayList<ViewBean> list) {
    ArrayList<ViewBean> sortedViews = new ArrayList<>();
    for (ViewBean viewBean : list) {
      if (viewBean.parent.equals("root"))
        sortedViews.add(viewBean); 
    } 
    int listSize = sortedViews.size();
    int outerIdx;
    int nextIdx = 0;
    for (outerIdx = 0; outerIdx < listSize - 1; outerIdx++) {
      for (int innerIdx = 0; innerIdx < listSize - outerIdx - 1; innerIdx = nextIdx) {
        int currentIndex = sortedViews.get(innerIdx).index;
        nextIdx = innerIdx + 1;
        if (currentIndex > sortedViews.get(nextIdx).index) {
          ViewBean viewBean = sortedViews.get(innerIdx);
          sortedViews.set(innerIdx, sortedViews.get(nextIdx));
          sortedViews.set(nextIdx, viewBean);
        } 
      } 
    } 
    for (ViewBean viewBean : list) {
      int viewType = viewBean.type;
      if ((viewType == 2 || viewType == 1 || viewType == 36 || viewType == 37 || viewType == 38 || viewType == 39 || viewType == 40 || viewType == 0 || viewType == 12) && viewBean.parent.equals("root"))
        sortedViews.addAll(getChildViews(list, viewBean)); 
    } 
    return sortedViews;
  }
  
  public static ArrayList<ViewBean> getChildViews(ArrayList<ViewBean> list, ViewBean parentBean) {
    ArrayList<ViewBean> childViews = new ArrayList<>();
    for (ViewBean viewBean : list) {
      if (viewBean.parent.equals(parentBean.id))
        childViews.add(viewBean); 
    } 
    int listSize = childViews.size();
    int outerIdx;
    int nextIdx = 0;
    for (outerIdx = 0; outerIdx < listSize - 1; outerIdx++) {
      for (int innerIdx = 0; innerIdx < listSize - outerIdx - 1; innerIdx = nextIdx) {
        int currentIndex = childViews.get(innerIdx).index;
        nextIdx = innerIdx + 1;
        if (currentIndex > childViews.get(nextIdx).index) {
          ViewBean tempBean = childViews.get(innerIdx);
          childViews.set(innerIdx, childViews.get(nextIdx));
          childViews.set(nextIdx, tempBean);
        } 
      } 
    } 
    for (ViewBean viewBean : list) {
      if (viewBean.parent.equals(parentBean.id)) {
        int viewType = viewBean.type;
        if (viewType == 0 || viewType == 2 || viewType == 1 || viewType == 36 || viewType == 37 || viewType == 38 || viewType == 39 || viewType == 40 || viewType == 12)
          childViews.addAll(getChildViews(list, viewBean)); 
      } 
    } 
    return childViews;
  }
  
  public ComponentBean getComponent(String fileName, int index) {
    return !componentMap.containsKey(fileName) ? null : componentMap.get(fileName).get(index);
  }
  
  public ArrayList<String> getAllIdentifiers(ProjectFileBean fileBean) {
    String xmlName = fileBean.getXmlName();
    String javaName = fileBean.getJavaName();
    ArrayList<String> identifiers = new ArrayList<>();
    for (Pair<Integer, String> var : getVariables(javaName))
      identifiers.add(var.second);
    for (Pair<Integer, String> listVar : getListVariables(javaName))
      identifiers.add(listVar.second);
    for (Pair<String, String> moreBlock : getMoreBlocks(javaName))
      identifiers.add(moreBlock.first);
    for (ViewBean view : getViews(xmlName))
      identifiers.add(view.id);
    for (ComponentBean comp : getComponents(javaName))
      identifiers.add(comp.componentId);
    return identifiers;
  }
  
  public ArrayList<EventBean> getComponentEvents(String fileName, ComponentBean componentBean) {
    if (!eventMap.containsKey(fileName))
      return new ArrayList<>(); 
    ArrayList<EventBean> filteredEvents = new ArrayList<>();
    for (EventBean eventBean : eventMap.get(fileName)) {
      if (eventBean.targetType == componentBean.type && eventBean.targetId.equals(componentBean.componentId))
        filteredEvents.add(eventBean); 
    } 
    return filteredEvents;
  }
  
  public ArrayList<BlockBean> getBlocks(String fileName, String data) {
    if (!blockMap.containsKey(fileName))
      return new ArrayList<>(); 
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap == null || !blockEntryMap.containsKey(data))
      return new ArrayList<>();
    return blockEntryMap.get(data);
  }
  
  public void deleteBackupFiles() {
    String backupDir = SketchwarePaths.getBackupPath(projectId);
    fileUtil.deleteFileByPath(backupDir + File.separator + "view");
    fileUtil.deleteFileByPath(backupDir + File.separator + "logic");
  }
  
  public void syncWithFileManager(ProjectFileManager fileManager) {
    for (ProjectFileBean projectFileBean : fileManager.getActivities()) {
      if (!projectFileBean.hasActivityOption(8))
        removeFab(projectFileBean); 
      if (!projectFileBean.hasActivityOption(4))
        removeViewTypeEvents(projectFileBean.getJavaName()); 
    } 
    ArrayList<String> viewKeysToRemove = new ArrayList<>();
    for (Map.Entry<String, ArrayList<ViewBean>> entry : viewMap.entrySet()) {
      String key = entry.getKey();
      if (!fileManager.hasXmlName(key)) {
        viewKeysToRemove.add(key);
        continue;
      } 
      for (ViewBean viewBean : entry.getValue()) {
        if (viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) {
          String customViewName = viewBean.customView;
          if (customViewName != null && customViewName.length() > 0 && !customViewName.equals("none")) {
            boolean found = false;
            for (ProjectFileBean customView : fileManager.getCustomViews()) {
              if (customView.fileName.equals(viewBean.customView)) {
                found = true;
                break;
              }
            }
            if (!found)
              viewBean.customView = ""; 
          } 
        } 
      } 
    } 
    for (String key : viewKeysToRemove)
      viewMap.remove(key); 
    variableMap.entrySet().removeIf(entry -> !fileManager.hasJavaName(entry.getKey()));
    listMap.entrySet().removeIf(entry -> !fileManager.hasJavaName(entry.getKey()));
    moreBlockMap.entrySet().removeIf(entry -> !fileManager.hasJavaName(entry.getKey()));
    componentMap.entrySet().removeIf(entry -> !fileManager.hasJavaName(entry.getKey()));
    eventMap.entrySet().removeIf(entry -> !fileManager.hasJavaName(entry.getKey()));
    ArrayList<String> blockKeysToRemove = new ArrayList<>();
    for (Map.Entry<String, HashMap<String, ArrayList<BlockBean>>> entry : blockMap.entrySet()) {
      String key = entry.getKey();
      if (!fileManager.hasJavaName(key)) {
        blockKeysToRemove.add(key);
        continue;
      } 
      for (Map.Entry<String, ArrayList<BlockBean>> blockEntry : entry.getValue().entrySet()) {
        for (BlockBean blockBean : blockEntry.getValue()) {
          if (blockBean.opCode.equals("intentSetScreen")) {
            boolean activityExists = false;
            for (ProjectFileBean activity : fileManager.getActivities()) {
              if (activity.getActivityName().equals(blockBean.parameters.get(1))) {
                activityExists = true;
                break;
              }
            }
            if (!activityExists)
              blockBean.parameters.set(1, "");
          } 
        } 
      } 
    } 
    for (String key : blockKeysToRemove)
      blockMap.remove(key); 
  }
  
  public void syncFonts(ResourceManager resourceManager) {
    ArrayList<String> fontNames = resourceManager.getFontNames();
    for (HashMap<String, ArrayList<BlockBean>> fileBlocks : blockMap.values()) {
      for (ArrayList<BlockBean> blocks : fileBlocks.values()) {
        for (BlockBean blockBean : blocks) {
          if ("setTypeface".equals(blockBean.opCode) && !fontNames.contains(blockBean.parameters.get(1)))
            blockBean.parameters.set(1, "default_font"); 
        } 
      } 
    } 
  }
  
  public void removeView(ProjectFileBean fileBean, ViewBean targetBean) {
    if (!viewMap.containsKey(fileBean.getXmlName()))
      return; 
    ArrayList<ViewBean> views = viewMap.get(fileBean.getXmlName());
    for (int i = views.size() - 1; i >= 0; i--) {
      if (views.get(i).id.equals(targetBean.id)) {
        views.remove(i);
        break;
      }
    }
    int fileType = fileBean.fileType;
    if (fileType == 0) {
      removeEventsByTarget(fileBean.getJavaName(), targetBean.id);
      removeBlockReferences(fileBean.getJavaName(), targetBean.getClassInfo(), targetBean.id, true);
    } else if (fileType == 1) {
      ArrayList<Pair<String, String>> customViewPairs = new ArrayList<>();
      for (Map.Entry<String, ArrayList<ViewBean>> entry : viewMap.entrySet()) {
        for (ViewBean viewBean : entry.getValue()) {
          if ((viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) && viewBean.customView.equals(fileBean.fileName)) {
            String eventName = viewBean.id + "_onBindCustomView";
            String xmlKey = entry.getKey();
            customViewPairs.add(new Pair<>(ProjectFileBean.getJavaName(xmlKey.substring(0, xmlKey.lastIndexOf(".xml"))), eventName));
          } 
        } 
      } 
      for (Pair<String, String> viewPair : customViewPairs) {
        if (!blockMap.containsKey(viewPair.first))
          continue;
        HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(viewPair.first);
        if (!blockEntryMap.containsKey(viewPair.second))
          continue;
        ArrayList<BlockBean> blockBeans = blockEntryMap.get(viewPair.second);
        if (blockBeans == null || blockBeans.isEmpty())
          continue;
        for (int i = blockBeans.size() - 1; i >= 0; i--) {
          BlockBean blockBean = blockBeans.get(i);
          ClassInfo blockClassInfo = blockBean.getClassInfo();
          if (blockClassInfo != null && blockClassInfo.isExactType(targetBean.getClassInfo().getClassName()) && blockBean.spec.equals(targetBean.id)) {
            blockBeans.remove(i);
            continue;
          }
          ArrayList<ClassInfo> paramClassInfos = blockBean.getParamClassInfo();
          if (paramClassInfos != null && !paramClassInfos.isEmpty()) {
            for (int b = 0; b < paramClassInfos.size(); b++) {
              ClassInfo paramClassInfo = paramClassInfos.get(b);
              if (paramClassInfo != null && targetBean.getClassInfo().isAssignableFrom(paramClassInfo) && blockBean.parameters.get(b).equals(targetBean.id))
                blockBean.parameters.set(b, "");
            }
          }
        }
      } 
    } else if (fileType == 2) {
      removeViewEventsByTarget(fileBean.getDrawersJavaName(), targetBean.id);
    } 
  }
  
  public void removeAdmobComponents(ProjectLibraryBean libraryBean) {
    if (libraryBean.useYn.equals("Y"))
      return; 
    for (String key : new ArrayList<>(componentMap.keySet())) {
      removeComponentsByType(key, 6);
      removeComponentsByType(key, 12);
      removeComponentsByType(key, 14);
    } 
  }
  
  public void removeFirebaseViews(ProjectLibraryBean libraryBean, ProjectFileManager fileManager) {
    if (libraryBean.useYn.equals("Y"))
      return; 
    for (ProjectFileBean projectFileBean : fileManager.getActivities()) {
      ArrayList<ViewBean> toRemove = new ArrayList<>();
      for (ViewBean viewBean : getViews(projectFileBean.getXmlName())) {
        if (viewBean.type == 17)
          toRemove.add(viewBean); 
      } 
      for (ViewBean viewBean : toRemove)
        removeView(projectFileBean, viewBean); 
    } 
    for (ProjectFileBean projectFileBean : fileManager.getCustomViews()) {
      ArrayList<ViewBean> toRemove = new ArrayList<>();
      for (ViewBean viewBean : getViews(projectFileBean.getXmlName())) {
        if (viewBean.type == 17)
          toRemove.add(viewBean); 
      } 
      for (ViewBean viewBean : toRemove)
        removeView(projectFileBean, viewBean); 
    } 
    for (String key : new ArrayList<>(componentMap.keySet()))
      removeComponentsByType(key, 13); 
  }
  
  public void readLogicData(BufferedReader reader) throws IOException {
    if (variableMap != null) variableMap.clear();
    if (listMap != null) listMap.clear();
    if (moreBlockMap != null) moreBlockMap.clear();
    if (componentMap != null) componentMap.clear();
    if (eventMap != null) eventMap.clear();
    if (blockMap != null) blockMap.clear();
    StringBuilder contentBuffer = new StringBuilder();
    String sectionName = "";
    while (true) {
      String line = reader.readLine();
      if (line != null) {
        if (line.length() <= 0)
          continue; 
        if (line.charAt(0) == '@') {
          StringBuilder tempBuffer = contentBuffer;
          if (sectionName.length() > 0) {
            parseLogicSection(sectionName, contentBuffer.toString());
            tempBuffer = new StringBuilder();
          } 
          sectionName = line.substring(1);
          contentBuffer = tempBuffer;
          continue;
        } 
        contentBuffer.append(line);
        contentBuffer.append("\n");
        continue;
      } 
      if (sectionName.length() > 0 && contentBuffer.length() > 0)
        parseLogicSection(sectionName, contentBuffer.toString()); 
      return;
    } 
  }
  
  public void initFab(String fileName) {
    if (fabMap.containsKey(fileName))
      return; 
    ViewBean viewBean = new ViewBean("_fab", 16);
    LayoutBean layoutBean = viewBean.layout;
    layoutBean.marginLeft = 16;
    layoutBean.marginTop = 16;
    layoutBean.marginRight = 16;
    layoutBean.marginBottom = 16;
    layoutBean.layoutGravity = 85;
    fabMap.put(fileName, viewBean);
  }
  
  public void addEvent(String fileName, int x, int y, String data, String extra) {
    if (!eventMap.containsKey(fileName))
      eventMap.put(fileName, new ArrayList<>()); 
    eventMap.get(fileName).add(new EventBean(x, y, data, extra));
  }
  
  public void addComponent(String fileName, int index, String data) {
    if (!componentMap.containsKey(fileName))
      componentMap.put(fileName, new ArrayList<>()); 
    componentMap.get(fileName).add(new ComponentBean(index, data));
  }
  
  public void addComponentWithParam(String fileName, int index, String data, String extra) {
    if (!componentMap.containsKey(fileName))
      componentMap.put(fileName, new ArrayList<>()); 
    componentMap.get(fileName).add(new ComponentBean(index, data, extra));
  }
  
  public void removeBlockReferences(String fileName, ClassInfo classInfo, String data, boolean skipCustomView) {
    if (!blockMap.containsKey(fileName))
      return; 
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap == null)
      return; 
    for (Map.Entry<String, ArrayList<BlockBean>> entry : blockEntryMap.entrySet()) {
      if (skipCustomView && entry.getKey().substring(entry.getKey().lastIndexOf("_") + 1).equals("onBindCustomView"))
        continue; 
      ArrayList<BlockBean> blockBeans = entry.getValue();
      if (blockBeans == null || blockBeans.isEmpty())
        continue; 
      for (int i = blockBeans.size() - 1; i >= 0; i--) {
        BlockBean blockBean = blockBeans.get(i);
        ClassInfo blockClassInfo = blockBean.getClassInfo();
        if (blockClassInfo != null && blockClassInfo.isExactType(classInfo.getClassName()) && blockBean.spec.equals(data)) {
          blockBeans.remove(i);
          continue;
        }
        ArrayList<ClassInfo> paramClassInfos = blockBean.getParamClassInfo();
        if (paramClassInfos != null && !paramClassInfos.isEmpty()) {
          for (int b = 0; b < paramClassInfos.size(); b++) {
            ClassInfo paramClassInfo = paramClassInfos.get(b);
            if (paramClassInfo != null && classInfo.isAssignableFrom(paramClassInfo) && blockBean.parameters.get(b).equals(data))
              blockBean.parameters.set(b, ""); 
          }
        }
      }
    } 
  }
  
  public void addEventBean(String fileName, EventBean eventBean) {
    if (!eventMap.containsKey(fileName))
      eventMap.put(fileName, new ArrayList<>()); 
    eventMap.get(fileName).add(eventBean);
  }
  
  public void addView(String fileName, ViewBean viewBean) {
    if (!viewMap.containsKey(fileName))
      viewMap.put(fileName, new ArrayList<>()); 
    viewMap.get(fileName).add(viewBean);
  }
  
  public void addMoreBlock(String fileName, String data, String extra) {
    if (!moreBlockMap.containsKey(fileName))
      moreBlockMap.put(fileName, new ArrayList<>()); 
    moreBlockMap.get(fileName).add(new Pair<>(data, extra));
  }
  
  public void putBlocks(String fileName, String data, ArrayList<BlockBean> list) {
    if (!blockMap.containsKey(fileName))
      blockMap.put(fileName, new HashMap<>()); 
    blockMap.get(fileName).put(data, list);
  }
  
  public final void serializeLogicData(StringBuilder buffer) {
    if (variableMap != null && !variableMap.isEmpty()) {
      for (Map.Entry<String, ArrayList<Pair<Integer, String>>> entry : variableMap.entrySet()) {
        ArrayList<Pair<Integer, String>> variables = entry.getValue();
        if (variables == null || variables.isEmpty())
          continue;
        StringBuilder contentBuilder = new StringBuilder();
        for (Pair<Integer, String> varEntry : variables) {
          contentBuilder.append(varEntry.first).append(":").append(varEntry.second).append("\n");
        }
        buffer.append("@").append(entry.getKey()).append("_var").append("\n");
        buffer.append(contentBuilder).append("\n");
      }
    }
    if (listMap != null && !listMap.isEmpty()) {
      for (Map.Entry<String, ArrayList<Pair<Integer, String>>> entry : listMap.entrySet()) {
        ArrayList<Pair<Integer, String>> listEntries = entry.getValue();
        if (listEntries == null || listEntries.isEmpty())
          continue;
        StringBuilder contentBuilder = new StringBuilder();
        for (Pair<Integer, String> listEntry : listEntries) {
          contentBuilder.append(listEntry.first).append(":").append(listEntry.second).append("\n");
        }
        buffer.append("@").append(entry.getKey()).append("_list").append("\n");
        buffer.append(contentBuilder).append("\n");
      }
    }
    if (moreBlockMap != null && !moreBlockMap.isEmpty()) {
      for (Map.Entry<String, ArrayList<Pair<String, String>>> entry : moreBlockMap.entrySet()) {
        ArrayList<Pair<String, String>> moreBlocks = entry.getValue();
        if (moreBlocks == null || moreBlocks.isEmpty())
          continue;
        StringBuilder contentBuilder = new StringBuilder();
        for (Pair<String, String> moreBlockEntry : moreBlocks) {
          contentBuilder.append(moreBlockEntry.first).append(":").append(moreBlockEntry.second).append("\n");
        }
        buffer.append("@").append(entry.getKey()).append("_func").append("\n");
        buffer.append(contentBuilder).append("\n");
      }
    }
    if (componentMap != null && !componentMap.isEmpty()) {
      for (Map.Entry<String, ArrayList<ComponentBean>> entry : componentMap.entrySet()) {
        ArrayList<ComponentBean> components = entry.getValue();
        if (components == null || components.isEmpty())
          continue;
        StringBuilder contentBuilder = new StringBuilder();
        for (ComponentBean componentBean : components) {
          componentBean.clearClassInfo();
          contentBuilder.append(gson.toJson(componentBean)).append("\n");
        }
        buffer.append("@").append(entry.getKey()).append("_components").append("\n");
        buffer.append(contentBuilder).append("\n");
      }
    }
    if (eventMap != null && !eventMap.isEmpty()) {
      for (Map.Entry<String, ArrayList<EventBean>> entry : eventMap.entrySet()) {
        ArrayList<EventBean> events = entry.getValue();
        if (events == null || events.isEmpty())
          continue;
        StringBuilder contentBuilder = new StringBuilder();
        for (EventBean eventBean : events) {
          contentBuilder.append(gson.toJson(eventBean)).append("\n");
        }
        buffer.append("@").append(entry.getKey()).append("_events").append("\n");
        buffer.append(contentBuilder).append("\n");
      }
    }
    if (blockMap != null && !blockMap.isEmpty()) {
      for (Map.Entry<String, HashMap<String, ArrayList<BlockBean>>> entry : blockMap.entrySet()) {
        String key = entry.getKey();
        HashMap<String, ArrayList<BlockBean>> blockEntryMap = entry.getValue();
        if (blockEntryMap == null || blockEntryMap.isEmpty())
          continue;
        for (Map.Entry<String, ArrayList<BlockBean>> blockEntry : blockEntryMap.entrySet()) {
          ArrayList<BlockBean> blocks = blockEntry.getValue();
          if (blocks == null || blocks.isEmpty())
            continue;
          StringBuilder contentBuilder = new StringBuilder();
          for (BlockBean blockBean : blocks) {
            contentBuilder.append(gson.toJson(blockBean)).append("\n");
          }
          buffer.append("@").append(key).append("_").append(blockEntry.getKey()).append("\n");
          buffer.append(contentBuilder).append("\n");
        }
      }
    }
  }
  
  public String getMoreBlockSpec(String fileName, String data) {
    if (!moreBlockMap.containsKey(fileName))
      return ""; 
    ArrayList<Pair<String, String>> moreBlocks = moreBlockMap.get(fileName);
    if (moreBlocks == null)
      return ""; 
    for (Pair<String, String> moreBlockEntry : moreBlocks) {
      if (moreBlockEntry.first.equals(data))
        return moreBlockEntry.second; 
    } 
    return "";
  }
  
  public ArrayList<String> getComponentIdsByType(String fileName, int index) {
    ArrayList<String> componentIds = new ArrayList<>();
    if (!componentMap.containsKey(fileName))
      return componentIds; 
    ArrayList<ComponentBean> components = componentMap.get(fileName);
    if (components == null)
      return componentIds; 
    for (ComponentBean componentBean : components) {
      if (componentBean.type == index)
        componentIds.add(componentBean.componentId); 
    } 
    return componentIds;
  }
  
  public ArrayList<ViewBean> getViewWithChildren(String fileName, ViewBean viewBean) {
    ArrayList<ViewBean> result = new ArrayList<>();
    result.add(viewBean);
    result.addAll(getChildViews(viewMap.get(fileName), viewBean));
    return result;
  }
  
  public HashMap<String, ArrayList<BlockBean>> getBlockMap(String fileName) {
    return blockMap.containsKey(fileName) ? blockMap.get(fileName) : new HashMap<>();
  }
  
  public void clearAllData() {
    if (viewMap != null) viewMap.clear();
    if (blockMap != null) blockMap.clear();
    if (variableMap != null) variableMap.clear();
    if (listMap != null) listMap.clear();
    if (moreBlockMap != null) moreBlockMap.clear();
    if (componentMap != null) componentMap.clear();
    if (eventMap != null) eventMap.clear();
    viewMap = new HashMap<>();
    blockMap = new HashMap<>();
    variableMap = new HashMap<>();
    listMap = new HashMap<>();
    moreBlockMap = new HashMap<>();
    componentMap = new HashMap<>();
    eventMap = new HashMap<>();
    fabMap = new HashMap<>();
  }
  
  public void syncImages(ResourceManager resourceManager) {
    ArrayList<String> imageNames = resourceManager.getImageNames();
    for (ArrayList<ViewBean> views : viewMap.values()) {
      for (ViewBean viewBean : views) {
        if (!imageNames.contains(viewBean.layout.backgroundResource))
          viewBean.layout.backgroundResource = null; 
        if (!imageNames.contains(viewBean.image.resName))
          viewBean.image.resName = "default_image"; 
      } 
    } 
    for (ViewBean viewBean : fabMap.values()) {
      if (!imageNames.contains(viewBean.image.resName))
        viewBean.image.resName = "NONE"; 
    } 
    for (HashMap<String, ArrayList<BlockBean>> fileBlocks : blockMap.values()) {
      for (ArrayList<BlockBean> blocks : fileBlocks.values()) {
        for (BlockBean blockBean : blocks) {
          if ("setImage".equals(blockBean.opCode)) {
            if (!imageNames.contains(blockBean.parameters.get(1)))
              blockBean.parameters.set(1, "default_image"); 
            continue;
          } 
          if ("setBgResource".equals(blockBean.opCode) && !imageNames.contains(blockBean.parameters.get(1)))
            blockBean.parameters.set(1, "NONE"); 
        } 
      } 
    } 
  }
  
  public void removeFab(ProjectFileBean fileBean) {
    if (fabMap.containsKey(fileBean.getXmlName()))
      fabMap.remove(fileBean.getXmlName()); 
    removeEventsByTarget(fileBean.getJavaName(), "_fab");
  }
  
  public void removeMapViews(ProjectLibraryBean libraryBean, ProjectFileManager fileManager) {
    if (libraryBean.useYn.equals("Y"))
      return; 
    for (ProjectFileBean projectFileBean : fileManager.getActivities()) {
      for (ViewBean viewBean : getViews(projectFileBean.getXmlName())) {
        if (viewBean.type == 18)
          removeView(projectFileBean, viewBean); 
      } 
    } 
  }
  
  public void readViewData(BufferedReader reader) throws IOException {
    if (viewMap != null) viewMap.clear();
    if (fabMap != null) fabMap.clear();
    StringBuilder contentBuffer = new StringBuilder();
    String sectionName = "";
    while (true) {
      String line = reader.readLine();
      if (line != null) {
        if (line.length() <= 0)
          continue; 
        if (line.charAt(0) == '@') {
          StringBuilder tempBuffer = contentBuffer;
          if (sectionName.length() > 0) {
            parseViewSection(sectionName, contentBuffer.toString());
            tempBuffer = new StringBuilder();
          } 
          sectionName = line.substring(1);
          contentBuffer = tempBuffer;
          continue;
        } 
        contentBuffer.append(line);
        contentBuffer.append("\n");
        continue;
      } 
      if (sectionName.length() > 0 && contentBuffer.length() > 0)
        parseViewSection(sectionName, contentBuffer.toString()); 
      return;
    } 
  }
  
  public void addListVariable(String fileName, int index, String data) {
    if (!listMap.containsKey(fileName))
      listMap.put(fileName, new ArrayList<>()); 
    listMap.get(fileName).add(new Pair<>(index, data));
  }
  
  public void removeComponent(String fileName, ComponentBean componentBean) {
    if (!componentMap.containsKey(fileName))
      return; 
    ArrayList<ComponentBean> components = componentMap.get(fileName);
    if (!components.contains(componentBean))
      return; 
    components.remove(componentBean);
    removeEventsByTarget(fileName, componentBean.componentId);
    removeBlockReferences(fileName, componentBean.getClassInfo(), componentBean.componentId, false);
    buildConfig.constVarComponent.handleDeleteComponent(componentBean.componentId);
  }
  
  public final void serializeViewData(StringBuilder buffer) {
    if (viewMap != null && !viewMap.isEmpty()) {
      for (Map.Entry<String, ArrayList<ViewBean>> entry : viewMap.entrySet()) {
        ArrayList<ViewBean> viewBeans = entry.getValue();
        if (viewBeans == null || viewBeans.isEmpty())
          continue;
        ArrayList<ViewBean> sortedViews = getSortedRootViews(viewBeans);
        StringBuilder contentBuilder = new StringBuilder();
        if (sortedViews != null && !sortedViews.isEmpty()) {
          for (ViewBean viewBean : sortedViews) {
            viewBean.clearClassInfo();
            contentBuilder.append(gson.toJson(viewBean)).append("\n");
          }
        }
        buffer.append("@").append(entry.getKey()).append("\n");
        buffer.append(contentBuilder).append("\n");
      }
    }
    if (fabMap != null && !fabMap.isEmpty()) {
      for (Map.Entry<String, ViewBean> entry : fabMap.entrySet()) {
        ViewBean viewBean = entry.getValue();
        if (viewBean == null)
          continue;
        buffer.append("@").append(entry.getKey()).append("_fab").append("\n");
        buffer.append(gson.toJson(viewBean)).append("\n");
        buffer.append("\n");
      }
    }
  }
  
  public boolean isListUsedInBlocks(String fileName, String data, String extra) {
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap == null)
      return false; 
    for (Map.Entry<String, ArrayList<BlockBean>> entry : blockEntryMap.entrySet()) {
      if (entry.getKey().equals(extra))
        continue; 
      for (BlockBean blockBean : entry.getValue()) {
        ClassInfo blockClassInfo = blockBean.getClassInfo();
        if (blockClassInfo != null && blockClassInfo.isList() && blockBean.spec.equals(data))
          return true; 
        ArrayList<ClassInfo> paramClassInfos = blockBean.getParamClassInfo();
        if (paramClassInfos != null && !paramClassInfos.isEmpty())
          for (int b = 0; b < paramClassInfos.size(); b++) {
            ClassInfo paramClassInfo = paramClassInfos.get(b);
            if (paramClassInfo != null && paramClassInfo.isList() && blockBean.parameters.get(b).equals(data))
              return true; 
          }  
      } 
    } 
    return false;
  }
  
  public ViewBean getViewBean(String fileName, String data) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views == null)
      return null; 
    for (int b = 0; b < views.size(); b++) {
      ViewBean viewBean = views.get(b);
      if (data.equals(viewBean.id))
        return viewBean; 
    } 
    return null;
  }
  
  public ArrayList<String> getListNames(String fileName) {
    ArrayList<String> listNames = new ArrayList<>();
    if (!listMap.containsKey(fileName))
      return listNames; 
    ArrayList<Pair<Integer, String>> listVars = listMap.get(fileName);
    if (listVars == null)
      return listNames; 
    for (Pair<Integer, String> listVar : listVars)
      listNames.add(listVar.second); 
    return listNames;
  }
  
  public ArrayList<ComponentBean> getComponentsByType(String fileName, int index) {
    ArrayList<ComponentBean> filteredComponents = new ArrayList<>();
    if (!componentMap.containsKey(fileName))
      return filteredComponents; 
    ArrayList<ComponentBean> components = componentMap.get(fileName);
    if (components == null)
      return filteredComponents; 
    for (ComponentBean componentBean : components) {
      if (componentBean.type == index)
        filteredComponents.add(componentBean); 
    } 
    return filteredComponents;
  }
  
  public void syncSounds(ResourceManager resourceManager) {
    ArrayList<String> soundNames = resourceManager.getSoundNames();
    for (HashMap<String, ArrayList<BlockBean>> fileBlocks : blockMap.values()) {
      for (ArrayList<BlockBean> blocks : fileBlocks.values()) {
        for (BlockBean blockBean : blocks) {
          if (blockBean.opCode.equals("mediaplayerCreate") && !soundNames.contains(blockBean.parameters.get(1)))
            blockBean.parameters.set(1, ""); 
          if (blockBean.opCode.equals("soundpoolLoad") && !soundNames.contains(blockBean.parameters.get(1)))
            blockBean.parameters.set(1, ""); 
        } 
      } 
    } 
  }
  
  public void addVariable(String fileName, int index, String data) {
    if (!variableMap.containsKey(fileName))
      variableMap.put(fileName, new ArrayList<>()); 
    variableMap.get(fileName).add(new Pair<>(index, data));
  }
  
  public boolean hasLogicBackup() {
    return fileUtil.exists(SketchwarePaths.getBackupPath(projectId) + File.separator + "logic");
  }
  
  public boolean isVariableUsedInBlocks(String fileName, String data, String extra) {
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap == null)
      return false; 
    for (Map.Entry<String, ArrayList<BlockBean>> entry : blockEntryMap.entrySet()) {
      if (entry.getKey().equals(extra))
        continue; 
      for (BlockBean blockBean : entry.getValue()) {
        ClassInfo blockClassInfo = blockBean.getClassInfo();
        if (blockClassInfo != null && blockClassInfo.isVariable() && blockBean.spec.equals(data))
          return true; 
        ArrayList<ClassInfo> paramClassInfos = blockBean.getParamClassInfo();
        if (paramClassInfos != null && !paramClassInfos.isEmpty())
          for (int b = 0; b < paramClassInfos.size(); b++) {
            ClassInfo paramClassInfo = paramClassInfos.get(b);
            if (paramClassInfo != null && paramClassInfo.isVariable() && blockBean.parameters.get(b).equals(data))
              return true; 
          }  
      } 
    } 
    return false;
  }
  
  public ArrayList<ViewBean> getViews(String fileName) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    return views != null ? views : new ArrayList<>();
  }
  
  public ArrayList<String> getListNamesByType(String fileName, int index) {
    ArrayList<String> filteredNames = new ArrayList<>();
    if (!listMap.containsKey(fileName))
      return filteredNames; 
    ArrayList<Pair<Integer, String>> listVars = listMap.get(fileName);
    if (listVars == null)
      return filteredNames; 
    for (Pair<Integer, String> listEntry : listVars) {
      if (listEntry.first == index)
        filteredNames.add(listEntry.second); 
    } 
    return filteredNames;
  }
  
  public ArrayList<Pair<Integer, String>> getViewsByType(String fileName, String data) {
    ArrayList<Pair<Integer, String>> filteredViews = new ArrayList<>();
    ArrayList<ViewBean> viewBeans = viewMap.get(fileName);
    if (viewBeans == null)
      return filteredViews; 
    for (ViewBean viewBean : viewBeans) {
      Pair<Integer, String> pair;
      if (data.equals("CheckBox")) {
        if (viewBean.getClassInfo().matchesType("CompoundButton")) {
          pair = new Pair<>(viewBean.type, viewBean.id);
        } else {
          continue;
        } 
      } else if (viewBean.getClassInfo().matchesType(data)) {
        pair = new Pair<>(viewBean.type, viewBean.id);
      } else {
        continue;
      } 
      filteredViews.add(pair);
    } 
    return filteredViews;
  }
  
  public void removeEvent(String fileName, String data, String extra) {
    if (!eventMap.containsKey(fileName))
      return; 
    ArrayList<EventBean> events = eventMap.get(fileName);
    if (events == null)
      return; 
    for (int i = events.size() - 1; i >= 0; i--) {
      EventBean eventBean = events.get(i);
      if (eventBean.targetId.equals(data) && extra.equals(eventBean.eventName)) {
        events.remove(i);
        HashMap<String, ArrayList<BlockBean>> fileBlocks = blockMap != null ? blockMap.get(fileName) : null;
        if (fileBlocks != null) {
          String eventKey = eventBean.targetId + "_" + eventBean.eventName;
          fileBlocks.remove(eventKey);
        }
      }
    }
  }
  
  public boolean hasViewBackup() {
    return fileUtil.exists(SketchwarePaths.getBackupPath(projectId) + File.separator + "view");
  }
  
  public boolean hasComponent(String fileName, int index, String data) {
    ArrayList<ComponentBean> components = componentMap.get(fileName);
    if (components == null)
      return false; 
    for (ComponentBean componentBean : components) {
      if (componentBean.type == index && componentBean.componentId.equals(data))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<ComponentBean> getComponents(String fileName) {
    return !componentMap.containsKey(fileName) ? new ArrayList<>() : componentMap.get(fileName);
  }
  
  public ArrayList<String> getVariableNamesByType(String fileName, int index) {
    ArrayList<String> varNames = new ArrayList<>();
    if (!variableMap.containsKey(fileName))
      return varNames; 
    ArrayList<Pair<Integer, String>> varEntries = variableMap.get(fileName);
    if (varEntries == null)
      return varNames; 
    for (Pair<Integer, String> varEntry : varEntries) {
      if (varEntry.first == index)
        varNames.add(varEntry.second); 
    } 
    return varNames;
  }
  
  public void loadLogicFromData() {
    String logicPath = SketchwarePaths.getDataPath(projectId) + File.separator + "logic";
    if (!fileUtil.exists(logicPath))
      return;
    try (BufferedReader reader = new BufferedReader(new StringReader(fileUtil.decryptToString(fileUtil.readFileBytes(logicPath))))) {
      readLogicData(reader);
    } catch (Exception e) {
      Log.e("ProjectDataStore", "Failed to load logic data", e);
    }
  }
  
  public boolean hasListVariable(String fileName, int index, String data) {
    ArrayList<Pair<Integer, String>> listVars = listMap.get(fileName);
    if (listVars == null)
      return false; 
    for (Pair<Integer, String> listEntry : listVars) {
      if (listEntry.first == index && listEntry.second.equals(data))
        return true; 
    } 
    return false;
  }
  
  public boolean hasCompoundButtonView(String fileName, String data) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views == null)
      return false; 
    for (ViewBean viewBean : views) {
      if (viewBean.getClassInfo().matchesType("CompoundButton") && viewBean.id.equals(data))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<ViewBean> getCustomViewBeans(String input) {
    ArrayList<ViewBean> customViews = new ArrayList<>();
    ArrayList<ViewBean> viewBeans = viewMap.get(input);
    if (viewBeans == null)
      return customViews; 
    for (ViewBean viewBean : viewBeans) {
      if (viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) {
        String customViewName = viewBean.customView;
        if (customViewName != null && customViewName.length() > 0 && !viewBean.customView.equals("none"))
          customViews.add(viewBean); 
      } 
    } 
    return customViews;
  }
  
  public void loadLogicFromBackup() {
    String logicPath = SketchwarePaths.getBackupPath(projectId) + File.separator + "logic";
    try (BufferedReader reader = new BufferedReader(new StringReader(fileUtil.decryptToString(fileUtil.readFileBytes(logicPath))))) {
      readLogicData(reader);
    } catch (Exception e) {
      Log.e("ProjectDataStore", "Failed to load logic backup", e);
    }
  }
  
  public boolean hasComponentOfType(String fileName, int index) {
    ArrayList<ComponentBean> components = componentMap.get(fileName);
    if (components == null)
      return false; 
    for (ComponentBean component : components) {
      if (component.type == index)
        return true; 
    } 
    return false;
  }
  
  public boolean hasVariable(String fileName, int index, String data) {
    ArrayList<Pair<Integer, String>> varEntries = variableMap.get(fileName);
    if (varEntries == null)
      return false; 
    for (Pair<Integer, String> varEntry : varEntries) {
      if (varEntry.first == index && varEntry.second.equals(data))
        return true; 
    } 
    return false;
  }
  
  public boolean isMoreBlockUsed(String fileName, String data) {
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap == null)
      return false; 
    String moreBlockKey = data + "_moreBlock";
    for (Map.Entry<String, ArrayList<BlockBean>> entry : blockEntryMap.entrySet()) {
      if (entry.getKey().equals(moreBlockKey))
        continue; 
      for (BlockBean blockBean : entry.getValue()) {
        if (blockBean.opCode.equals("definedFunc")) {
          int spaceIdx = blockBean.spec.indexOf(" ");
          String specStr = blockBean.spec;
          String funcName = specStr;
          if (spaceIdx > 0)
            funcName = specStr.substring(0, spaceIdx); 
          if (funcName.equals(data))
            return true; 
        } 
      } 
    } 
    return false;
  }
  
  public ArrayList<EventBean> getEvents(String fileName) {
    return !eventMap.containsKey(fileName) ? new ArrayList<>() : eventMap.get(fileName);
  }
  
  public void loadViewFromData() {
    String viewPath = SketchwarePaths.getDataPath(projectId) + File.separator + "view";
    if (!fileUtil.exists(viewPath))
      return;
    try (BufferedReader reader = new BufferedReader(new StringReader(fileUtil.decryptToString(fileUtil.readFileBytes(viewPath))))) {
      readViewData(reader);
    } catch (Exception e) {
      Log.e("ProjectDataStore", "Failed to load view data", e);
    }
  }
  
  public void removeComponentsByType(String fileName, int index) {
    if (!componentMap.containsKey(fileName))
      return; 
    ArrayList<ComponentBean> components = getComponentsByType(fileName, index);
    if (components != null && !components.isEmpty()) {
      for (ComponentBean component : components)
        removeComponent(fileName, component); 
    } 
  }
  
  public boolean hasViewOfType(String fileName, int index, String data) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views == null)
      return false; 
    for (ViewBean viewBean : views) {
      if (viewBean.type == index && viewBean.id.equals(data))
        return true; 
    } 
    return false;
  }
  
  public boolean hasTextView(String fileName, String data) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views == null)
      return false; 
    for (ViewBean viewBean : views) {
      if (viewBean.getClassInfo().matchesType("TextView") && viewBean.id.equals(data))
        return true; 
    } 
    return false;
  }
  
  public ViewBean getFabView(String fileName) {
    if (!fabMap.containsKey(fileName))
      initFab(fileName); 
    return fabMap.get(fileName);
  }
  
  public void loadViewFromBackup() {
    String viewPath = SketchwarePaths.getBackupPath(projectId) + File.separator + "view";
    try (BufferedReader reader = new BufferedReader(new StringReader(fileUtil.decryptToString(fileUtil.readFileBytes(viewPath))))) {
      readViewData(reader);
    } catch (Exception e) {
      Log.e("ProjectDataStore", "Failed to load view backup", e);
    }
  }
  
  public boolean hasView(String fileName, String data) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views == null)
      return false; 
    for (ViewBean viewBean : views) {
      if (viewBean.id.equals(data))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<Pair<String, String>> getMoreBlocks(String fileName) {
    return moreBlockMap.containsKey(fileName) ? moreBlockMap.get(fileName) : new ArrayList<>();
  }
  
  public void resetProject() {
    projectId = "";
    clearAllData();
  }
  
  @SuppressWarnings("unchecked")
  public void parseLogicSection(String fileName, String data) {
    if (data.length() <= 0)
      return; 
    try {
      ProjectDataParser parser = new ProjectDataParser(fileName);
      String parsedFileName = parser.getFileName();
      ProjectDataParser.DataType dataType = parser.getDataType();
      switch (dataType) {
        case VARIABLE:
          variableMap.put(parsedFileName, (ArrayList<Pair<Integer, String>>)parser.parseData(data));
          break;
        case LIST:
          listMap.put(parsedFileName, (ArrayList<Pair<Integer, String>>)parser.parseData(data));
          break;
        case COMPONENT:
          componentMap.put(parsedFileName, (ArrayList<ComponentBean>)parser.parseData(data));
          break;
        case EVENT:
          eventMap.put(parsedFileName, (ArrayList<EventBean>)parser.parseData(data));
          break;
        case MORE_BLOCK:
          moreBlockMap.put(parsedFileName, (ArrayList<Pair<String, String>>)parser.parseData(data));
          break;
        case EVENT_BLOCK:
          if (!blockMap.containsKey(parsedFileName)) {
            blockMap.put(parsedFileName, new HashMap<String, ArrayList<BlockBean>>());
          }
          blockMap.get(parsedFileName).put(parser.getEventKey(), parser.parseData(data));
          break;
        default:
          return;
      }
    } catch (Exception e) {
      Log.e("ProjectDataStore", "Failed to parse logic section", e);
    } 
  }
  
  public ArrayList<Pair<Integer, String>> getListVariables(String fileName) {
    return listMap.containsKey(fileName) ? listMap.get(fileName) : new ArrayList<>();
  }
  
  public void saveAllData() {
    String basePath = SketchwarePaths.getDataPath(projectId);
    saveViewFile(basePath + File.separator + "view");
    saveLogicFile(basePath + File.separator + "logic");
    deleteBackupFiles();
  }
  
  @SuppressWarnings("unchecked")
  public void parseViewSection(String fileName, String data) {
    try {
      ProjectDataParser parser = new ProjectDataParser(fileName);
      String parsedFileName = parser.getFileName();
      ProjectDataParser.DataType dataType = parser.getDataType();
      if (dataType == ProjectDataParser.DataType.VIEW) {
        viewMap.put(parsedFileName, parser.parseData(data));
      } else if (dataType == ProjectDataParser.DataType.FAB) {
        fabMap.put(parsedFileName, parser.parseData(data));
      }
    } catch (Exception e) {
      Log.e("ProjectDataStore", "Failed to parse view section", e);
    } 
  }
  
  public ArrayList<Pair<Integer, String>> getVariables(String fileName) {
    return variableMap.containsKey(fileName) ? variableMap.get(fileName) : new ArrayList<>();
  }
  
  public void saveAllBackup() {
    String basePath = SketchwarePaths.getBackupPath(projectId);
    saveViewFile(basePath + File.separator + "view");
    saveLogicFile(basePath + File.separator + "logic");
  }
  
  public void removeBlockEntry(String fileName, String data) {
    if (!blockMap.containsKey(fileName))
      return; 
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap == null)
      return; 
    blockEntryMap.remove(data);
  }
  
  public void removeViewTypeEvents(String fileName) {
    if (!eventMap.containsKey(fileName))
      return; 
    ArrayList<EventBean> events = eventMap.get(fileName);
    events.removeIf(event -> event.eventType == 4);
  }
  
  public void removeViewEventsByTarget(String fileName, String data) {
    if (!eventMap.containsKey(fileName))
      return; 
    ArrayList<EventBean> events = eventMap.get(fileName);
    events.removeIf(event -> event.eventType == 4 && event.targetId.equals(data));
  }
  
  public final void saveLogicFile(String filePath) {
    StringBuilder contentBuffer = new StringBuilder();
    serializeLogicData(contentBuffer);
    try {
      fileUtil.writeBytes(filePath, fileUtil.encryptString(contentBuffer.toString()));
    } catch (Exception e) {
      Log.e("ProjectDataStore", "Failed to save logic file", e);
    } 
  }
  
  public void removeEventsByTarget(String fileName, String data) {
    if (!eventMap.containsKey(fileName))
      return; 
    ArrayList<EventBean> events = eventMap.get(fileName);
    if (events == null)
      return; 
    HashMap<String, ArrayList<BlockBean>> fileBlocks = blockMap != null ? blockMap.get(fileName) : null;
    for (int i = events.size() - 1; i >= 0; i--) {
      EventBean eventBean = events.get(i);
      if (eventBean.targetId.equals(data)) {
        events.remove(i);
        if (fileBlocks != null) {
          fileBlocks.remove(eventBean.targetId + "_" + eventBean.eventName);
        }
      }
    }
  }
  
  public final void saveViewFile(String filePath) {
    StringBuilder contentBuffer = new StringBuilder();
    serializeViewData(contentBuffer);
    try {
      fileUtil.writeBytes(filePath, fileUtil.encryptString(contentBuffer.toString()));
    } catch (Exception e) {
      Log.e("ProjectDataStore", "Failed to save view file", e);
    } 
  }
  
  public void removeMoreBlock(String fileName, String data) {
    if (!moreBlockMap.containsKey(fileName))
      return; 
    ArrayList<Pair<String, String>> moreBlocks = moreBlockMap.get(fileName);
    if (moreBlocks == null)
      return; 
    for (Pair<String, String> moreBlockEntry : moreBlocks) {
      if (moreBlockEntry.first.equals(data)) {
        moreBlocks.remove(moreBlockEntry);
        break;
      } 
    } 
    String blockKey = data + "_moreBlock";
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap != null) {
      blockEntryMap.remove(blockKey);
    } 
  }
  
  public void removeListVariable(String fileName, String data) {
    if (!listMap.containsKey(fileName))
      return; 
    ArrayList<Pair<Integer, String>> listVars = listMap.get(fileName);
    if (listVars == null)
      return; 
    for (Pair<Integer, String> listEntry : listVars) {
      if (listEntry.second.equals(data)) {
        listVars.remove(listEntry);
        break;
      } 
    } 
  }
  
  public void removeVariable(String fileName, String data) {
    if (!variableMap.containsKey(fileName))
      return; 
    ArrayList<Pair<Integer, String>> varEntries = variableMap.get(fileName);
    if (varEntries == null)
      return; 
    for (Pair<Integer, String> varEntry : varEntries) {
      if (varEntry.second.equals(data)) {
        varEntries.remove(varEntry);
        break;
      } 
    } 
  }
  
  public boolean hasViewType(String fileName, int index) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views == null)
      return false;
    for (ViewBean viewBean : views) {
      if (viewBean.type == index)
        return true; 
    } 
    return false;
  }
  
  public boolean hasViewMatchingType(String fileName, String data) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views == null)
      return false;
    for (ViewBean viewBean : views) {
      if (viewBean.getClassInfo().matchesType(data))
        return true; 
    } 
    return false;
  }
  
  public final String getSimpleClassName(String input) {
    String simpleName = input;
    if (simpleName.contains(".")) {
      String[] parts = simpleName.split("\\.");
      simpleName = parts[parts.length - 1];
    } 
    return simpleName;
  }
}
