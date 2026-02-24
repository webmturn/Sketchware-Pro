package pro.sketchware.core;

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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
    this.fileUtil = new EncryptedFileUtil();
    this.gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
    this.buildConfig = new BuildConfig();
  }
  
  public static ArrayList<ViewBean> getSortedRootViews(ArrayList<ViewBean> list) {
    ArrayList<ViewBean> sortedViews = new ArrayList<>();
    for (ViewBean viewBean : list) {
      if (viewBean.parent.equals("root"))
        sortedViews.add(viewBean); 
    } 
    int j = sortedViews.size();
    int i;
    int m = 0;
    for (i = 0; i < j - 1; i++) {
      for (int k = 0; k < j - i - 1; k = m) {
        int n = ((ViewBean)sortedViews.get(k)).index;
        m = k + 1;
        if (n > ((ViewBean)sortedViews.get(m)).index) {
          ViewBean viewBean = sortedViews.get(k);
          sortedViews.set(k, sortedViews.get(m));
          sortedViews.set(m, viewBean);
        } 
      } 
    } 
    for (ViewBean viewBean : list) {
      i = viewBean.type;
      if ((i == 2 || i == 1 || i == 36 || i == 37 || i == 38 || i == 39 || i == 40 || i == 0 || i == 12) && viewBean.parent.equals("root"))
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
    int j = childViews.size();
    int i;
    int m = 0;
    for (i = 0; i < j - 1; i++) {
      for (int k = 0; k < j - i - 1; k = m) {
        int n = ((ViewBean)childViews.get(k)).index;
        m = k + 1;
        if (n > ((ViewBean)childViews.get(m)).index) {
          ViewBean tempBean = childViews.get(k);
          childViews.set(k, childViews.get(m));
          childViews.set(m, tempBean);
        } 
      } 
    } 
    for (ViewBean viewBean : list) {
      if (viewBean.parent.equals(parentBean.id)) {
        i = viewBean.type;
        if (i == 0 || i == 2 || i == 1 || i == 36 || i == 37 || i == 38 || i == 39 || i == 40 || i == 12)
          childViews.addAll(getChildViews(list, viewBean)); 
      } 
    } 
    return childViews;
  }
  
  public ComponentBean getComponent(String fileName, int index) {
    return !this.componentMap.containsKey(fileName) ? null : ((ArrayList<ComponentBean>)this.componentMap.get(fileName)).get(index);
  }
  
  public ArrayList<String> getAllIdentifiers(ProjectFileBean fileBean) {
    String xmlName = fileBean.getXmlName();
    String javaName = fileBean.getJavaName();
    ArrayList<Object> identifiers = new ArrayList<>();
    Iterator<Pair<Integer, String>> innerIterator = getVariables(javaName).iterator();
    while (innerIterator.hasNext())
      identifiers.add(((Pair)innerIterator.next()).second); 
    innerIterator = getListVariables(javaName).iterator();
    while (innerIterator.hasNext())
      identifiers.add(((Pair)innerIterator.next()).second); 
    innerIterator = (Iterator)getMoreBlocks(javaName).iterator();
    while (innerIterator.hasNext())
      identifiers.add(((Pair)innerIterator.next()).first); 
    Iterator<ViewBean> iterator = getViews(xmlName).iterator();
    while (iterator.hasNext())
      identifiers.add(((ViewBean)iterator.next()).id); 
    Iterator iteratorComp = getComponents(javaName).iterator();
    while (iteratorComp.hasNext())
      identifiers.add(((ComponentBean)iteratorComp.next()).componentId); 
    return (ArrayList)identifiers;
  }
  
  public ArrayList<EventBean> getComponentEvents(String fileName, ComponentBean componentBean) {
    if (!this.eventMap.containsKey(fileName))
      return new ArrayList<EventBean>(); 
    ArrayList<EventBean> filteredEvents = new ArrayList<>();
    for (EventBean eventBean : this.eventMap.get(fileName)) {
      if (eventBean.targetType == componentBean.type && eventBean.targetId.equals(componentBean.componentId))
        filteredEvents.add(eventBean); 
    } 
    return filteredEvents;
  }
  
  public ArrayList<BlockBean> getBlocks(String fileName, String data) {
    if (!this.blockMap.containsKey(fileName))
      return new ArrayList<BlockBean>(); 
    Map map = this.blockMap.get(fileName);
    return (map == null) ? new ArrayList<BlockBean>() : (!map.containsKey(data) ? new ArrayList<BlockBean>() : (ArrayList<BlockBean>)map.get(data));
  }
  
  public void deleteBackupFiles() {
    String backupDir = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder viewPathBuilder = new StringBuilder();
    viewPathBuilder.append(backupDir);
    viewPathBuilder.append(File.separator);
    viewPathBuilder.append("view");
    String viewPath = viewPathBuilder.toString();
    this.fileUtil.deleteFileByPath(viewPath);
    StringBuilder contentBuilder = new StringBuilder();
    contentBuilder.append(backupDir);
    contentBuilder.append(File.separator);
    contentBuilder.append("logic");
    String logicPath = contentBuilder.toString();
    this.fileUtil.deleteFileByPath(logicPath);
  }
  
  public void syncWithFileManager(ProjectFileManager fileManager) {
    for (ProjectFileBean projectFileBean : fileManager.getActivities()) {
      if (!projectFileBean.hasActivityOption(8))
        removeFab(projectFileBean); 
      if (!projectFileBean.hasActivityOption(4))
        removeViewTypeEvents(projectFileBean.getJavaName()); 
    } 
    ArrayList<String> viewKeysToRemove = new ArrayList<>();
    for (Map.Entry<String, ArrayList<ViewBean>> entry : this.viewMap.entrySet()) {
      String key = (String)entry.getKey();
      if (!fileManager.hasXmlName(key)) {
        viewKeysToRemove.add(key);
        continue;
      } 
      for (ViewBean viewBean : entry.getValue()) {
        if (viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) {
          String customViewName = viewBean.customView;
          if (customViewName != null && customViewName.length() > 0 && !viewBean.customView.equals("none")) {
            Iterator iterator = fileManager.getCustomViews().iterator();
            boolean found = false;
            while (iterator.hasNext()) {
              if (((ProjectFileBean)iterator.next()).fileName.equals(viewBean.customView))
                found = true; 
            } 
            if (!found)
              viewBean.customView = ""; 
          } 
        } 
      } 
    } 
    for (String key : viewKeysToRemove)
      this.viewMap.remove(key); 
    ArrayList<String> varKeysToRemove = new ArrayList<>();
    Iterator<Map.Entry<String, ArrayList<Pair<Integer, String>>>> varEntryIterator = this.variableMap.entrySet().iterator();
    while (varEntryIterator.hasNext()) {
      String key = (String)((Map.Entry)varEntryIterator.next()).getKey();
      if (!fileManager.hasJavaName(key))
        varKeysToRemove.add(key); 
    } 
    for (String key : varKeysToRemove)
      this.variableMap.remove(key); 
    ArrayList<String> keysToRemove = new ArrayList<>();
    Iterator listEntryIterator = this.listMap.entrySet().iterator();
    while (listEntryIterator.hasNext()) {
      String key = (String)((Map.Entry)listEntryIterator.next()).getKey();
      if (!fileManager.hasJavaName(key))
        keysToRemove.add(key); 
    } 
    for (String key : keysToRemove)
      this.listMap.remove(key); 
    keysToRemove = new ArrayList<String>();
    Iterator innerIterator = this.moreBlockMap.entrySet().iterator();
    while (innerIterator.hasNext()) {
      String key = (String)((Map.Entry)innerIterator.next()).getKey();
      if (!fileManager.hasJavaName(key))
        keysToRemove.add(key); 
    } 
    for (String key : keysToRemove)
      this.moreBlockMap.remove(key); 
    ArrayList<String> compKeysToRemove = new ArrayList<>();
    Iterator<Map.Entry<String, ArrayList<ComponentBean>>> compEntryIterator = this.componentMap.entrySet().iterator();
    while (compEntryIterator.hasNext()) {
      String key = (String)((Map.Entry)compEntryIterator.next()).getKey();
      if (!fileManager.hasJavaName(key))
        compKeysToRemove.add(key); 
    } 
    for (String key : compKeysToRemove)
      this.componentMap.remove(key); 
    ArrayList<String> eventKeysToRemove = new ArrayList<>();
    Iterator blockEntryIterator = this.eventMap.entrySet().iterator();
    while (blockEntryIterator.hasNext()) {
      String key = (String)((Map.Entry)blockEntryIterator.next()).getKey();
      if (!fileManager.hasJavaName(key))
        eventKeysToRemove.add(key); 
    } 
    for (String key : eventKeysToRemove)
      this.eventMap.remove(key); 
    ArrayList<String> blockKeysToRemove = new ArrayList<>();
    for (Map.Entry<String, HashMap<String, ArrayList<BlockBean>>> entry : this.blockMap.entrySet()) {
      String key = (String)entry.getKey();
      if (!fileManager.hasJavaName(key)) {
        blockKeysToRemove.add(key);
        continue;
      } 
      Iterator iterator = ((HashMap)entry.getValue()).entrySet().iterator();
      while (iterator.hasNext()) {
        label128: for (BlockBean blockBean : (ArrayList<BlockBean>)((Map.Entry)iterator.next()).getValue()) {
          if (blockBean.opCode.equals("intentSetScreen")) {
            Iterator<ProjectFileBean> activityIterator = fileManager.getActivities().iterator();
            while (activityIterator.hasNext()) {
              if (((ProjectFileBean)activityIterator.next()).getActivityName().equals(blockBean.parameters.get(1)))
                continue label128; 
            } 
            blockBean.parameters.set(1, "");
          } 
        } 
      } 
    } 
    for (String str : blockKeysToRemove)
      this.blockMap.remove(str); 
  }
  
  public void syncFonts(ResourceManager resourceManager) {
    ArrayList fontNames = resourceManager.getFontNames();
    Iterator iterator = this.blockMap.entrySet().iterator();
    while (iterator.hasNext()) {
      Iterator innerIterator = ((HashMap)((Map.Entry)iterator.next()).getValue()).entrySet().iterator();
      while (innerIterator.hasNext()) {
        for (BlockBean blockBean : (ArrayList<BlockBean>)((Map.Entry<String, ArrayList<BlockBean>>)innerIterator.next()).getValue()) {
          if ("setTypeface".equals(blockBean.opCode) && fontNames.indexOf(blockBean.parameters.get(1)) < 0)
            blockBean.parameters.set(1, "default_font"); 
        } 
      } 
    } 
  }
  
  public void removeView(ProjectFileBean fileBean, ViewBean targetBean) {
    if (!this.viewMap.containsKey(fileBean.getXmlName()))
      return; 
    ArrayList views = this.viewMap.get(fileBean.getXmlName());
    int i = views.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        i = j;
        if (((ViewBean)views.get(j)).id.equals(targetBean.id)) {
          views.remove(j);
          break;
        } 
        continue;
      } 
      break;
    } 
    i = fileBean.fileType;
    if (i == 0) {
      removeEventsByTarget(fileBean.getJavaName(), targetBean.id);
      removeBlockReferences(fileBean.getJavaName(), targetBean.getClassInfo(), targetBean.id, true);
    } else if (i == 1) {
      ArrayList<Pair> customViewPairs = new ArrayList<>();
      for (Map.Entry<String, ArrayList<ViewBean>> entry : this.viewMap.entrySet()) {
        for (ViewBean viewBean : entry.getValue()) {
          if ((viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) && viewBean.customView.equals(fileBean.fileName)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(viewBean.id);
            stringBuilder.append("_");
            stringBuilder.append("onBindCustomView");
            String eventName = stringBuilder.toString();
            String xmlKey = (String)entry.getKey();
            customViewPairs.add(new Pair(ProjectFileBean.getJavaName(xmlKey.substring(0, xmlKey.lastIndexOf(".xml"))), eventName));
          } 
        } 
      } 
      label70: for (Pair pair : customViewPairs) {
        if (this.blockMap.containsKey(pair.first)) {
          Map map = this.blockMap.get(pair.first);
          if (map.containsKey(pair.second)) {
            ArrayList<BlockBean> blockBeans = (ArrayList)map.get(pair.second);
            if (blockBeans == null || blockBeans.size() <= 0)
              continue; 
            i = blockBeans.size();
            label68: while (true) {
              int j = i - 1;
              if (j >= 0) {
                BlockBean blockBean = blockBeans.get(j);
                ClassInfo gx = blockBean.getClassInfo();
                if (gx != null && gx.isExactType(targetBean.getClassInfo().getClassName()) && blockBean.spec.equals(targetBean.id)) {
                  blockBeans.remove(j);
                  i = j;
                  continue;
                } 
                ArrayList<ClassInfo> paramClassInfos = blockBean.getParamClassInfo();
                i = j;
                if (paramClassInfos != null) {
                  i = j;
                  if (paramClassInfos.size() > 0) {
                    int b = 0;
                    while (true) {
                      i = j;
                      if (b < paramClassInfos.size()) {
                        ClassInfo gx1 = paramClassInfos.get(b);
                        if (gx1 != null && targetBean.getClassInfo().isAssignableFrom(gx1) && ((String)blockBean.parameters.get(b)).equals(targetBean.id))
                          blockBean.parameters.set(b, ""); 
                        b++;
                        continue;
                      } 
                      continue label68;
                    } 
                  } 
                } 
                continue;
              } 
              continue label70;
            } 
          } 
        } 
      } 
    } else if (i == 2) {
      removeViewEventsByTarget(fileBean.getDrawersJavaName(), targetBean.id);
    } 
  }
  
  public void removeAdmobComponents(ProjectLibraryBean libraryBean) {
    if (libraryBean.useYn.equals("Y"))
      return; 
    Iterator iterator = this.componentMap.entrySet().iterator();
    while (iterator.hasNext()) {
      String key = (String)((Map.Entry)iterator.next()).getKey();
      removeComponentsByType(key, 6);
      removeComponentsByType(key, 12);
      removeComponentsByType(key, 14);
    } 
  }
  
  public void removeFirebaseViews(ProjectLibraryBean libraryBean, ProjectFileManager fileManager) {
    if (libraryBean.useYn.equals("Y"))
      return; 
    for (ProjectFileBean projectFileBean : fileManager.getActivities()) {
      for (ViewBean viewBean : getViews(projectFileBean.getXmlName())) {
        if (viewBean.type == 17)
          removeView(projectFileBean, viewBean); 
      } 
    } 
    for (ProjectFileBean projectFileBean : fileManager.getCustomViews()) {
      for (ViewBean viewBean : getViews(projectFileBean.getXmlName())) {
        if (viewBean.type == 17)
          removeView(projectFileBean, viewBean); 
      } 
    } 
    Iterator iterator = this.componentMap.entrySet().iterator();
    while (iterator.hasNext())
      removeComponentsByType((String)((Map.Entry)iterator.next()).getKey(), 13); 
  }
  
  public void readLogicData(BufferedReader reader) throws java.io.IOException {
    HashMap<String, ArrayList<Pair<Integer, String>>> tempVarMap = this.variableMap;
    if (tempVarMap != null)
      tempVarMap.clear(); 
    tempVarMap = this.listMap;
    if (tempVarMap != null)
      tempVarMap.clear(); 
    HashMap<String, ArrayList<Pair<String, String>>> tempMoreBlockMap = this.moreBlockMap;
    if (tempMoreBlockMap != null)
      tempMoreBlockMap.clear(); 
    HashMap<String, ArrayList<ComponentBean>> tempCompMap = this.componentMap;
    if (tempCompMap != null)
      tempCompMap.clear(); 
    HashMap<String, ArrayList<EventBean>> tempEventMap = this.eventMap;
    if (tempEventMap != null)
      tempEventMap.clear(); 
    HashMap<String, HashMap<String, ArrayList<BlockBean>>> hashMap = this.blockMap;
    if (hashMap != null)
      hashMap.clear(); 
    StringBuffer stringBuffer = new StringBuffer();
    String sectionName = "";
    while (true) {
      String line = reader.readLine();
      if (line != null) {
        if (line.length() <= 0)
          continue; 
        if (line.charAt(0) == '@') {
          StringBuffer tempBuffer = stringBuffer;
          if (sectionName.length() > 0) {
            parseLogicSection(sectionName, stringBuffer.toString());
            tempBuffer = new StringBuffer();
          } 
          sectionName = line.substring(1);
          stringBuffer = tempBuffer;
          continue;
        } 
        stringBuffer.append(line);
        stringBuffer.append("\n");
        continue;
      } 
      if (sectionName.length() > 0 && stringBuffer.length() > 0)
        parseLogicSection(sectionName, stringBuffer.toString()); 
      return;
    } 
  }
  
  public void initFab(String fileName) {
    if (this.fabMap.containsKey(fileName))
      return; 
    ViewBean viewBean = new ViewBean("_fab", 16);
    LayoutBean layoutBean = viewBean.layout;
    layoutBean.marginLeft = 16;
    layoutBean.marginTop = 16;
    layoutBean.marginRight = 16;
    layoutBean.marginBottom = 16;
    layoutBean.layoutGravity = 85;
    this.fabMap.put(fileName, viewBean);
  }
  
  public void addEvent(String fileName, int x, int y, String data, String extra) {
    if (!this.eventMap.containsKey(fileName))
      this.eventMap.put(fileName, new ArrayList<EventBean>()); 
    ((ArrayList<EventBean>)this.eventMap.get(fileName)).add(new EventBean(x, y, data, extra));
  }
  
  public void addComponent(String fileName, int index, String data) {
    if (!this.componentMap.containsKey(fileName))
      this.componentMap.put(fileName, new ArrayList<ComponentBean>()); 
    ((ArrayList<ComponentBean>)this.componentMap.get(fileName)).add(new ComponentBean(index, data));
  }
  
  public void addComponentWithParam(String fileName, int index, String data, String extra) {
    if (!this.componentMap.containsKey(fileName))
      this.componentMap.put(fileName, new ArrayList<ComponentBean>()); 
    ((ArrayList<ComponentBean>)this.componentMap.get(fileName)).add(new ComponentBean(index, data, extra));
  }
  
  public void removeBlockReferences(String fileName, ClassInfo classInfo, String data, boolean flag) {
    if (!this.blockMap.containsKey(fileName))
      return; 
    Map map = this.blockMap.get(fileName);
    if (map == null)
      return; 
    label44: for (Map.Entry<String, ArrayList<BlockBean>> entry : ((Map<String, ArrayList<BlockBean>>)map).entrySet()) {
      if (flag && ((String)entry.getKey()).substring(((String)entry.getKey()).lastIndexOf("_") + 1).equals("onBindCustomView"))
        continue; 
      ArrayList<BlockBean> arrayList = (ArrayList)entry.getValue();
      if (arrayList == null || arrayList.size() <= 0)
        continue; 
      int i = arrayList.size();
      label42: while (true) {
        int j = i - 1;
        if (j >= 0) {
          BlockBean blockBean = arrayList.get(j);
          ClassInfo gx = blockBean.getClassInfo();
          if (gx != null && gx.isExactType(classInfo.getClassName()) && blockBean.spec.equals(data)) {
            arrayList.remove(j);
            i = j;
            continue;
          } 
          ArrayList<ClassInfo> paramClassInfos = blockBean.getParamClassInfo();
          i = j;
          if (paramClassInfos != null) {
            i = j;
            if (paramClassInfos.size() > 0) {
              int b = 0;
              while (true) {
                i = j;
                if (b < paramClassInfos.size()) {
                  ClassInfo gx1 = paramClassInfos.get(b);
                  if (gx1 != null && classInfo.isAssignableFrom(gx1) && ((String)blockBean.parameters.get(b)).equals(data))
                    blockBean.parameters.set(b, ""); 
                  b++;
                  continue;
                } 
                continue label42;
              } 
            } 
          } 
          continue;
        } 
        continue label44;
      } 
    } 
  }
  
  public void addEventBean(String fileName, EventBean eventBean) {
    if (!this.eventMap.containsKey(fileName))
      this.eventMap.put(fileName, new ArrayList<EventBean>()); 
    ((ArrayList<EventBean>)this.eventMap.get(fileName)).add(eventBean);
  }
  
  public void addView(String fileName, ViewBean viewBean) {
    if (!this.viewMap.containsKey(fileName))
      this.viewMap.put(fileName, new ArrayList<ViewBean>()); 
    ((ArrayList<ViewBean>)this.viewMap.get(fileName)).add(viewBean);
  }
  
  public void addMoreBlock(String fileName, String data, String extra) {
    Pair pair = new Pair(data, extra);
    if (!this.moreBlockMap.containsKey(fileName))
      this.moreBlockMap.put(fileName, new ArrayList<Pair<String, String>>()); 
    ((ArrayList)this.moreBlockMap.get(fileName)).add(pair);
  }
  
  public void putBlocks(String fileName, String data, ArrayList<BlockBean> list) {
    if (!this.blockMap.containsKey(fileName))
      this.blockMap.put(fileName, new HashMap<String, ArrayList<BlockBean>>()); 
    ((Map<String, ArrayList<BlockBean>>)this.blockMap.get(fileName)).put(data, list);
  }
  
  public final void serializeLogicData(StringBuffer buffer) {
    HashMap<String, ArrayList<Pair<Integer, String>>> tempVarMap = this.variableMap;
    if (tempVarMap != null && tempVarMap.size() > 0)
      for (Map.Entry<String, ArrayList<Pair<Integer, String>>> entry : this.variableMap.entrySet()) {
        ArrayList variables = (ArrayList)entry.getValue();
        if (variables == null || variables.size() <= 0)
          continue; 
        StringBuilder contentBuilder = new StringBuilder();
        for (int vi = 0; vi < variables.size(); vi++) {
          Pair pair = (Pair) variables.get(vi);
          contentBuilder.append(pair.first);
          contentBuilder.append(":");
          contentBuilder.append((String)pair.second);
          contentBuilder.append("\n");
        }
        String content = contentBuilder.toString();
        buffer.append("@");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_");
        stringBuilder.append("var");
        buffer.append(stringBuilder.toString());
        buffer.append("\n");
        buffer.append(content);
        buffer.append("\n");
      }  
    tempVarMap = this.listMap;
    if (tempVarMap != null && tempVarMap.size() > 0)
      for (Map.Entry<String, ArrayList<Pair<Integer, String>>> entry : this.listMap.entrySet()) {
        ArrayList listEntries = (ArrayList)entry.getValue();
        if (listEntries == null || listEntries.size() <= 0)
          continue; 
        StringBuilder contentBuilder = new StringBuilder();
        for (int li = 0; li < listEntries.size(); li++) {
          Pair pair = (Pair) listEntries.get(li);
          contentBuilder.append(pair.first);
          contentBuilder.append(":");
          contentBuilder.append((String)pair.second);
          contentBuilder.append("\n");
        }
        String content = contentBuilder.toString();
        buffer.append("@");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_");
        stringBuilder.append("list");
        buffer.append(stringBuilder.toString());
        buffer.append("\n");
        buffer.append(content);
        buffer.append("\n");
      }  
    HashMap<String, ArrayList<Pair<String, String>>> tempMoreBlockMap = this.moreBlockMap;
    if (tempMoreBlockMap != null && tempMoreBlockMap.size() > 0)
      for (Map.Entry<String, ArrayList<Pair<String, String>>> entry : this.moreBlockMap.entrySet()) {
        ArrayList moreBlocks = (ArrayList)entry.getValue();
        if (moreBlocks == null || moreBlocks.size() <= 0)
          continue; 
        StringBuilder contentBuilder = new StringBuilder();
        for (int fi = 0; fi < moreBlocks.size(); fi++) {
          Pair pair = (Pair) moreBlocks.get(fi);
          contentBuilder.append((String)pair.first);
          contentBuilder.append(":");
          contentBuilder.append((String)pair.second);
          contentBuilder.append("\n");
        }
        String content = contentBuilder.toString();
        buffer.append("@");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_");
        stringBuilder.append("func");
        buffer.append(stringBuilder.toString());
        buffer.append("\n");
        buffer.append(content);
        buffer.append("\n");
      }  
    HashMap<String, ArrayList<ComponentBean>> tempCompMap = this.componentMap;
    if (tempCompMap != null && tempCompMap.size() > 0)
      for (Map.Entry<String, ArrayList<ComponentBean>> entry : this.componentMap.entrySet()) {
        ArrayList components = (ArrayList)entry.getValue();
        if (components == null || components.size() <= 0)
          continue; 
        StringBuilder contentBuilder = new StringBuilder();
        for (int ci = 0; ci < components.size(); ci++) {
          ComponentBean componentBean = (ComponentBean) components.get(ci);
          componentBean.clearClassInfo();
          contentBuilder.append(this.gson.toJson(componentBean));
          contentBuilder.append("\n");
        }
        String content = contentBuilder.toString();
        buffer.append("@");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_");
        stringBuilder.append("components");
        buffer.append(stringBuilder.toString());
        buffer.append("\n");
        buffer.append(content);
        buffer.append("\n");
      }  
    HashMap<String, ArrayList<EventBean>> tempEventMap = this.eventMap;
    if (tempEventMap != null && tempEventMap.size() > 0)
      for (Map.Entry<String, ArrayList<EventBean>> entry : this.eventMap.entrySet()) {
        ArrayList events = (ArrayList)entry.getValue();
        if (events == null || events.size() <= 0)
          continue; 
        StringBuilder contentBuilder = new StringBuilder();
        for (int ei = 0; ei < events.size(); ei++) {
          EventBean eventBean = (EventBean) events.get(ei);
          contentBuilder.append(this.gson.toJson(eventBean));
          contentBuilder.append("\n");
        }
        String content = contentBuilder.toString();
        buffer.append("@");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_");
        stringBuilder.append("events");
        buffer.append(stringBuilder.toString());
        buffer.append("\n");
        buffer.append(contentBuilder.toString());
        buffer.append("\n");
      }  
    HashMap<String, HashMap<String, ArrayList<BlockBean>>> hashMap = this.blockMap;
    if (hashMap != null && hashMap.size() > 0)
      for (Map.Entry<String, HashMap<String, ArrayList<BlockBean>>> entry : this.blockMap.entrySet()) {
        String key = (String)entry.getKey();
        HashMap blockEntryMap = (HashMap)entry.getValue();
        if (blockEntryMap == null || blockEntryMap.size() <= 0)
          continue; 
        for (Map.Entry<String, ArrayList<BlockBean>> entry1 : ((HashMap<String, ArrayList<BlockBean>>)blockEntryMap).entrySet()) {
          ArrayList blocks = (ArrayList)entry1.getValue();
          if (blocks == null || blocks.size() <= 0)
            continue; 
          StringBuilder contentBuilder = new StringBuilder();
          for (int bi = 0; bi < blocks.size(); bi++) {
            BlockBean blockBean = (BlockBean) blocks.get(bi);
            contentBuilder.append(this.gson.toJson(blockBean));
            contentBuilder.append("\n");
          }
          String blockJson = contentBuilder.toString();
          buffer.append("@");
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(key);
          stringBuilder.append("_");
          stringBuilder.append((String)entry1.getKey());
          buffer.append(stringBuilder.toString());
          buffer.append("\n");
          buffer.append(blockJson);
          buffer.append("\n");
        } 
      }  
  }
  
  public String getMoreBlockSpec(String fileName, String data) {
    if (!this.moreBlockMap.containsKey(fileName))
      return ""; 
    ArrayList moreBlocks = this.moreBlockMap.get(fileName);
    if (moreBlocks == null)
      return ""; 
    for (Pair pair : (ArrayList<Pair>)moreBlocks) {
      if (((String)pair.first).equals(data))
        return (String)pair.second; 
    } 
    return "";
  }
  
  public ArrayList<String> getComponentIdsByType(String fileName, int index) {
    ArrayList<String> componentIds = new ArrayList<>();
    if (!this.componentMap.containsKey(fileName))
      return componentIds; 
    ArrayList components = this.componentMap.get(fileName);
    if (components == null)
      return componentIds; 
    for (ComponentBean componentBean : (ArrayList<ComponentBean>)components) {
      if (componentBean.type == index)
        componentIds.add(componentBean.componentId); 
    } 
    return componentIds;
  }
  
  public ArrayList<ViewBean> getViewWithChildren(String fileName, ViewBean viewBean) {
    ArrayList<ViewBean> result = new ArrayList<>();
    result.add(viewBean);
    result.addAll(getChildViews(this.viewMap.get(fileName), viewBean));
    return result;
  }
  
  public HashMap<String, ArrayList<BlockBean>> getBlockMap(String fileName) {
    return !this.blockMap.containsKey(fileName) ? new HashMap<String, ArrayList<BlockBean>>() : this.blockMap.get(fileName);
  }
  
  public void clearAllData() {
    HashMap<String, ArrayList<ViewBean>> tempVarMap = this.viewMap;
    if (tempVarMap != null)
      tempVarMap.clear(); 
    HashMap<String, HashMap<String, ArrayList<BlockBean>>> tempMoreBlockMap = this.blockMap;
    if (tempMoreBlockMap != null)
      tempMoreBlockMap.clear(); 
    HashMap<String, ArrayList<Pair<Integer, String>>> tempCompMap = this.variableMap;
    if (tempCompMap != null)
      tempCompMap.clear(); 
    tempCompMap = this.listMap;
    if (tempCompMap != null)
      tempCompMap.clear(); 
    HashMap<String, ArrayList<ComponentBean>> tempEventMap = this.componentMap;
    if (tempEventMap != null)
      tempEventMap.clear(); 
    HashMap<String, ArrayList<EventBean>> hashMap = this.eventMap;
    if (hashMap != null)
      hashMap.clear(); 
    this.viewMap = new HashMap<String, ArrayList<ViewBean>>();
    this.blockMap = new HashMap<String, HashMap<String, ArrayList<BlockBean>>>();
    this.variableMap = new HashMap<String, ArrayList<Pair<Integer, String>>>();
    this.listMap = new HashMap<String, ArrayList<Pair<Integer, String>>>();
    this.moreBlockMap = new HashMap<String, ArrayList<Pair<String, String>>>();
    this.componentMap = new HashMap<String, ArrayList<ComponentBean>>();
    this.eventMap = new HashMap<String, ArrayList<EventBean>>();
    this.fabMap = new HashMap<String, ViewBean>();
  }
  
  public void syncImages(ResourceManager resourceManager) {
    ArrayList imageNames = resourceManager.getImageNames();
    Iterator innerIterator = this.viewMap.entrySet().iterator();
    while (innerIterator.hasNext()) {
      for (ViewBean viewBean : (ArrayList<ViewBean>)((Map.Entry)innerIterator.next()).getValue()) {
        if (imageNames.indexOf(viewBean.layout.backgroundResource) < 0)
          viewBean.layout.backgroundResource = null; 
        if (imageNames.indexOf(viewBean.image.resName) < 0)
          viewBean.image.resName = "default_image"; 
      } 
    } 
    innerIterator = this.fabMap.entrySet().iterator();
    while (innerIterator.hasNext()) {
      ViewBean viewBean = (ViewBean)((Map.Entry)innerIterator.next()).getValue();
      if (imageNames.indexOf(viewBean.image.resName) < 0)
        viewBean.image.resName = "NONE"; 
    } 
    Iterator blockEntryIterator = this.blockMap.entrySet().iterator();
    while (blockEntryIterator.hasNext()) {
      Iterator iterator = ((HashMap)((Map.Entry)blockEntryIterator.next()).getValue()).entrySet().iterator();
      while (iterator.hasNext()) {
        for (BlockBean blockBean : (ArrayList<BlockBean>)((Map.Entry)iterator.next()).getValue()) {
          if ("setImage".equals(blockBean.opCode)) {
            if (imageNames.indexOf(blockBean.parameters.get(1)) < 0)
              blockBean.parameters.set(1, "default_image"); 
            continue;
          } 
          if ("setBgResource".equals(blockBean.opCode) && imageNames.indexOf(blockBean.parameters.get(1)) < 0)
            blockBean.parameters.set(1, "NONE"); 
        } 
      } 
    } 
  }
  
  public void removeFab(ProjectFileBean fileBean) {
    if (this.fabMap.containsKey(fileBean.getXmlName()))
      this.fabMap.remove(fileBean.getXmlName()); 
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
  
  public void readViewData(BufferedReader reader) throws java.io.IOException {
    HashMap<String, ArrayList<ViewBean>> tempEventMap = this.viewMap;
    if (tempEventMap != null)
      tempEventMap.clear(); 
    HashMap<String, ViewBean> hashMap = this.fabMap;
    if (hashMap != null)
      hashMap.clear(); 
    StringBuffer stringBuffer = new StringBuffer();
    String sectionName = "";
    while (true) {
      String line = reader.readLine();
      if (line != null) {
        if (line.length() <= 0)
          continue; 
        if (line.charAt(0) == '@') {
          StringBuffer tempBuffer = stringBuffer;
          if (sectionName.length() > 0) {
            parseViewSection(sectionName, stringBuffer.toString());
            tempBuffer = new StringBuffer();
          } 
          sectionName = line.substring(1);
          stringBuffer = tempBuffer;
          continue;
        } 
        stringBuffer.append(line);
        stringBuffer.append("\n");
        continue;
      } 
      if (sectionName.length() > 0 && stringBuffer.length() > 0)
        parseViewSection(sectionName, stringBuffer.toString()); 
      return;
    } 
  }
  
  public void addListVariable(String fileName, int index, String data) {
    Pair pair = new Pair(Integer.valueOf(index), data);
    if (!this.listMap.containsKey(fileName))
      this.listMap.put(fileName, new ArrayList<Pair<Integer, String>>()); 
    ((ArrayList)this.listMap.get(fileName)).add(pair);
  }
  
  public void removeComponent(String fileName, ComponentBean componentBean) {
    if (!this.componentMap.containsKey(fileName))
      return; 
    ArrayList components = this.componentMap.get(fileName);
    if (components.indexOf(componentBean) < 0)
      return; 
    components.remove(componentBean);
    removeEventsByTarget(fileName, componentBean.componentId);
    removeBlockReferences(fileName, componentBean.getClassInfo(), componentBean.componentId, false);
    this.buildConfig.constVarComponent.handleDeleteComponent(componentBean.componentId);
  }
  
  public final void serializeViewData(StringBuffer buffer) {
    HashMap<String, ArrayList<ViewBean>> tempEventMap = this.viewMap;
    if (tempEventMap != null && tempEventMap.size() > 0)
      for (Map.Entry<String, ArrayList<ViewBean>> entry : this.viewMap.entrySet()) {
        String viewContent;
        ArrayList viewBeans = (ArrayList)entry.getValue();
        if (viewBeans == null || viewBeans.size() <= 0)
          continue; 
        ArrayList<ViewBean> sortedViews = getSortedRootViews((ArrayList<ViewBean>)entry.getValue());
        if (sortedViews != null && sortedViews.size() > 0) {
          int b = 0;
          String viewJson = "";
          while (true) {
            viewContent = viewJson;
            if (b < sortedViews.size()) {
              ViewBean viewBean = sortedViews.get(b);
              viewBean.clearClassInfo();
              StringBuilder stringBuilder = new StringBuilder();
              stringBuilder.append(viewJson);
              stringBuilder.append(this.gson.toJson(viewBean));
              stringBuilder.append("\n");
              viewJson = stringBuilder.toString();
              b++;
              continue;
            } 
            break;
          } 
        } else {
          viewContent = "";
        } 
        buffer.append("@");
        buffer.append((String)entry.getKey());
        buffer.append("\n");
        buffer.append(viewContent);
        buffer.append("\n");
      }  
    HashMap<String, ViewBean> hashMap = this.fabMap;
    if (hashMap != null && hashMap.size() > 0)
      for (Map.Entry<String, ViewBean> entry : this.fabMap.entrySet()) {
        ViewBean viewBean = (ViewBean)entry.getValue();
        if (viewBean == null)
          continue; 
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(this.gson.toJson(viewBean));
        stringBuilder.append("\n");
        String content = stringBuilder.toString();
        buffer.append("@");
        stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_fab");
        buffer.append(stringBuilder.toString());
        buffer.append("\n");
        buffer.append(content);
        buffer.append("\n");
      }  
  }
  
  public boolean isListUsedInBlocks(String fileName, String data, String extra) {
    Map map = this.blockMap.get(fileName);
    if (map == null)
      return false; 
    for (Map.Entry<String, ArrayList<BlockBean>> entry : ((Map<String, ArrayList<BlockBean>>)map).entrySet()) {
      if (((String)entry.getKey()).equals(extra))
        continue; 
      for (BlockBean blockBean : (ArrayList<BlockBean>)entry.getValue()) {
        ClassInfo gx = blockBean.getClassInfo();
        if (gx != null && gx.isList() && blockBean.spec.equals(data))
          return true; 
        ArrayList<ClassInfo> paramClassInfos = blockBean.getParamClassInfo();
        if (paramClassInfos != null && paramClassInfos.size() > 0)
          for (int b = 0; b < paramClassInfos.size(); b++) {
            ClassInfo gx1 = paramClassInfos.get(b);
            if (gx1 != null && gx1.isList() && ((String)blockBean.parameters.get(b)).equals(data))
              return true; 
          }  
      } 
    } 
    return false;
  }
  
  public ViewBean getViewBean(String fileName, String data) {
    ArrayList<ViewBean> views = this.viewMap.get(fileName);
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
    if (!this.listMap.containsKey(fileName))
      return listNames; 
    ArrayList listVars = this.listMap.get(fileName);
    if (listVars == null)
      return listNames; 
    Iterator iterator = listVars.iterator();
    while (iterator.hasNext())
      listNames.add((String)((Pair)iterator.next()).second); 
    return listNames;
  }
  
  public ArrayList<ComponentBean> getComponentsByType(String fileName, int index) {
    ArrayList<ComponentBean> filteredComponents = new ArrayList<>();
    if (!this.componentMap.containsKey(fileName))
      return filteredComponents; 
    ArrayList components = this.componentMap.get(fileName);
    if (components == null)
      return filteredComponents; 
    for (ComponentBean componentBean : (ArrayList<ComponentBean>)components) {
      if (componentBean.type == index)
        filteredComponents.add(componentBean); 
    } 
    return filteredComponents;
  }
  
  public void syncSounds(ResourceManager resourceManager) {
    ArrayList soundNames = resourceManager.getSoundNames();
    Iterator iterator = this.blockMap.entrySet().iterator();
    while (iterator.hasNext()) {
      Iterator innerIterator = ((HashMap)((Map.Entry)iterator.next()).getValue()).entrySet().iterator();
      while (innerIterator.hasNext()) {
        for (BlockBean blockBean : (ArrayList<BlockBean>)((Map.Entry)innerIterator.next()).getValue()) {
          if (blockBean.opCode.equals("mediaplayerCreate") && soundNames.indexOf(blockBean.parameters.get(1)) < 0)
            blockBean.parameters.set(1, ""); 
          if (blockBean.opCode.equals("soundpoolLoad") && soundNames.indexOf(blockBean.parameters.get(1)) < 0)
            blockBean.parameters.set(1, ""); 
        } 
      } 
    } 
  }
  
  public void addVariable(String fileName, int index, String data) {
    Pair pair = new Pair(Integer.valueOf(index), data);
    if (!this.variableMap.containsKey(fileName))
      this.variableMap.put(fileName, new ArrayList<Pair<Integer, String>>()); 
    ((ArrayList)this.variableMap.get(fileName)).add(pair);
  }
  
  public boolean hasLogicBackup() {
    String backupDir = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(backupDir);
    stringBuilder.append(File.separator);
    stringBuilder.append("logic");
    String logicPath = stringBuilder.toString();
    return this.fileUtil.exists(logicPath);
  }
  
  public boolean isVariableUsedInBlocks(String fileName, String data, String extra) {
    Map map = this.blockMap.get(fileName);
    if (map == null)
      return false; 
    for (Map.Entry<String, ArrayList<BlockBean>> entry : ((Map<String, ArrayList<BlockBean>>)map).entrySet()) {
      if (((String)entry.getKey()).equals(extra))
        continue; 
      for (BlockBean blockBean : (ArrayList<BlockBean>)entry.getValue()) {
        ClassInfo gx = blockBean.getClassInfo();
        if (gx != null && gx.isVariable() && blockBean.spec.equals(data))
          return true; 
        ArrayList<ClassInfo> paramClassInfos = blockBean.getParamClassInfo();
        if (paramClassInfos != null && paramClassInfos.size() > 0)
          for (int b = 0; b < paramClassInfos.size(); b++) {
            ClassInfo gx1 = paramClassInfos.get(b);
            if (gx1 != null && gx1.isVariable() && ((String)blockBean.parameters.get(b)).equals(data))
              return true; 
          }  
      } 
    } 
    return false;
  }
  
  public ArrayList<ViewBean> getViews(String fileName) {
    ArrayList<ViewBean> views = this.viewMap.get(fileName);
    ArrayList<ViewBean> result = views;
    if (views == null)
      result = new ArrayList<>(); 
    return result;
  }
  
  public ArrayList<String> getListNamesByType(String fileName, int index) {
    ArrayList<String> filteredNames = new ArrayList<>();
    if (!this.listMap.containsKey(fileName))
      return filteredNames; 
    ArrayList listVars = this.listMap.get(fileName);
    if (listVars == null)
      return filteredNames; 
    for (Pair pair : (ArrayList<Pair>)listVars) {
      if (((Integer)pair.first).intValue() == index)
        filteredNames.add((String)pair.second); 
    } 
    return filteredNames;
  }
  
  public ArrayList<Pair<Integer, String>> getViewsByType(String fileName, String data) {
    ArrayList<Pair<Integer, String>> filteredViews = new ArrayList<>();
    ArrayList viewBeans = this.viewMap.get(fileName);
    if (viewBeans == null)
      return filteredViews; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)viewBeans) {
      Pair<Integer, String> pair;
      if (data.equals("CheckBox")) {
        if (viewBean.getClassInfo().matchesType("CompoundButton")) {
          pair = new Pair(Integer.valueOf(viewBean.type), viewBean.id);
        } else {
          continue;
        } 
      } else if (viewBean.getClassInfo().matchesType(data)) {
        pair = new Pair(Integer.valueOf(viewBean.type), viewBean.id);
      } else {
        continue;
      } 
      filteredViews.add(pair);
    } 
    return filteredViews;
  }
  
  public void removeEvent(String fileName, String data, String extra) {
    if (!this.eventMap.containsKey(fileName))
      return; 
    ArrayList<EventBean> events = this.eventMap.get(fileName);
    if (events == null)
      return; 
    int i = events.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        EventBean eventBean = events.get(j);
        i = j;
        if (eventBean.targetId.equals(data)) {
          i = j;
          if (extra.equals(eventBean.eventName)) {
            events.remove(eventBean);
            HashMap<String, HashMap<String, ArrayList<BlockBean>>> hashMap = this.blockMap;
            i = j;
            if (hashMap != null) {
              i = j;
              if (hashMap.get(fileName) != null) {
                HashMap tempEventMap = this.blockMap.get(fileName);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(eventBean.targetId);
                stringBuilder.append("_");
                stringBuilder.append(eventBean.eventName);
                i = j;
                if (tempEventMap.containsKey(stringBuilder.toString())) {
                  tempEventMap = this.blockMap.get(fileName);
                  stringBuilder = new StringBuilder();
                  stringBuilder.append(eventBean.targetId);
                  stringBuilder.append("_");
                  stringBuilder.append(eventBean.eventName);
                  tempEventMap.remove(stringBuilder.toString());
                  i = j;
                } 
              } 
            } 
          } 
        } 
        continue;
      } 
      break;
    } 
  }
  
  public boolean hasViewBackup() {
    String backupDir = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(backupDir);
    stringBuilder.append(File.separator);
    stringBuilder.append("view");
    String viewPath = stringBuilder.toString();
    return this.fileUtil.exists(viewPath);
  }
  
  public boolean hasComponent(String fileName, int index, String data) {
    ArrayList components = this.componentMap.get(fileName);
    if (components == null)
      return false; 
    for (ComponentBean componentBean : (ArrayList<ComponentBean>)components) {
      if (componentBean.type == index && componentBean.componentId.equals(data))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<ComponentBean> getComponents(String fileName) {
    return !this.componentMap.containsKey(fileName) ? new ArrayList<ComponentBean>() : this.componentMap.get(fileName);
  }
  
  public ArrayList<String> getVariableNamesByType(String fileName, int index) {
    ArrayList<String> varNames = new ArrayList<>();
    if (!this.variableMap.containsKey(fileName))
      return varNames; 
    ArrayList varEntries = this.variableMap.get(fileName);
    if (varEntries == null)
      return varNames; 
    for (Pair pair : (ArrayList<Pair>)varEntries) {
      if (((Integer)pair.first).intValue() == index)
        varNames.add((String)pair.second); 
    } 
    return varNames;
  }
  
  public void loadLogicFromData() {
    String dataPath = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder contentBuilder = new StringBuilder();
    contentBuilder.append(dataPath);
    contentBuilder.append(File.separator);
    contentBuilder.append("logic");
    dataPath = contentBuilder.toString();
    if (!this.fileUtil.exists(dataPath))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] bytes = this.fileUtil.readFileBytes(dataPath);
      String decryptedData = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(decryptedData));
      readLogicData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public boolean hasListVariable(String fileName, int index, String data) {
    ArrayList listVars = this.listMap.get(fileName);
    if (listVars == null)
      return false; 
    for (Pair pair : (ArrayList<Pair>)listVars) {
      if (((Integer)pair.first).intValue() == index && ((String)pair.second).equals(data))
        return true; 
    } 
    return false;
  }
  
  public boolean hasCompoundButtonView(String fileName, String data) {
    ArrayList views = this.viewMap.get(fileName);
    if (views == null)
      return false; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)views) {
      if (viewBean.getClassInfo().matchesType("CompoundButton") && viewBean.id.equals(data))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<ViewBean> getCustomViewBeans(String input) {
    ArrayList<ViewBean> customViews = new ArrayList<>();
    ArrayList viewBeans = this.viewMap.get(input);
    if (viewBeans == null)
      return customViews; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)viewBeans) {
      if (viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) {
        String customViewName = viewBean.customView;
        if (customViewName != null && customViewName.length() > 0 && !viewBean.customView.equals("none"))
          customViews.add(viewBean); 
      } 
    } 
    return customViews;
  }
  
  public void loadLogicFromBackup() {
    String backupDir = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(backupDir);
    stringBuilder.append(File.separator);
    stringBuilder.append("logic");
    String logicPath = stringBuilder.toString();
    BufferedReader bufferedReader = null;
    try {
      byte[] bytes = this.fileUtil.readFileBytes(logicPath);
      String decryptedData = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(decryptedData));
      readLogicData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public boolean hasComponentOfType(String fileName, int index) {
    ArrayList components = this.componentMap.get(fileName);
    if (components == null)
      return false; 
    Iterator iterator = components.iterator();
    while (iterator.hasNext()) {
      if (((ComponentBean)iterator.next()).type == index)
        return true; 
    } 
    return false;
  }
  
  public boolean hasVariable(String fileName, int index, String data) {
    ArrayList varEntries = this.variableMap.get(fileName);
    if (varEntries == null)
      return false; 
    for (Pair pair : (ArrayList<Pair>)varEntries) {
      if (((Integer)pair.first).intValue() == index && ((String)pair.second).equals(data))
        return true; 
    } 
    return false;
  }
  
  public boolean isMoreBlockUsed(String fileName, String data) {
    Map map = this.blockMap.get(fileName);
    if (map == null)
      return false; 
    for (Map.Entry<String, ArrayList<BlockBean>> entry : ((Map<String, ArrayList<BlockBean>>)map).entrySet()) {
      String entryKey = (String)entry.getKey();
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(data);
      stringBuilder.append("_");
      stringBuilder.append("moreBlock");
      if (entryKey.equals(stringBuilder.toString()))
        continue; 
      for (BlockBean blockBean : (ArrayList<BlockBean>)entry.getValue()) {
        if (blockBean.opCode.equals("definedFunc")) {
          int i = blockBean.spec.indexOf(" ");
          String specStr = blockBean.spec;
          String funcName = specStr;
          if (i > 0)
            funcName = specStr.substring(0, i); 
          if (funcName.equals(data))
            return true; 
        } 
      } 
    } 
    return false;
  }
  
  public ArrayList<EventBean> getEvents(String fileName) {
    return !this.eventMap.containsKey(fileName) ? new ArrayList<EventBean>() : this.eventMap.get(fileName);
  }
  
  public void loadViewFromData() {
    String dataPath = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder contentBuilder = new StringBuilder();
    contentBuilder.append(dataPath);
    contentBuilder.append(File.separator);
    contentBuilder.append("view");
    dataPath = contentBuilder.toString();
    if (!this.fileUtil.exists(dataPath))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] bytes = this.fileUtil.readFileBytes(dataPath);
      String decryptedData = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(decryptedData));
      readViewData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void removeComponentsByType(String fileName, int index) {
    if (!this.componentMap.containsKey(fileName))
      return; 
    ArrayList<ComponentBean> components = getComponentsByType(fileName, index);
    if (components != null && components.size() > 0) {
      Iterator<ComponentBean> iterator = components.iterator();
      while (iterator.hasNext())
        removeComponent(fileName, iterator.next()); 
    } 
  }
  
  public boolean hasViewOfType(String fileName, int index, String data) {
    ArrayList views = this.viewMap.get(fileName);
    if (views == null)
      return false; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)views) {
      if (viewBean.type == index && viewBean.id.equals(data))
        return true; 
    } 
    return false;
  }
  
  public boolean hasTextView(String fileName, String data) {
    ArrayList views = this.viewMap.get(fileName);
    if (views == null)
      return false; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)views) {
      if (viewBean.getClassInfo().matchesType("TextView") && viewBean.id.equals(data))
        return true; 
    } 
    return false;
  }
  
  public ViewBean getFabView(String fileName) {
    if (!this.fabMap.containsKey(fileName))
      initFab(fileName); 
    return this.fabMap.get(fileName);
  }
  
  public void loadViewFromBackup() {
    String viewPath = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder contentBuilder = new StringBuilder();
    contentBuilder.append(viewPath);
    contentBuilder.append(File.separator);
    contentBuilder.append("view");
    viewPath = contentBuilder.toString();
    BufferedReader bufferedReader = null;
    try {
      byte[] bytes = this.fileUtil.readFileBytes(viewPath);
      String decryptedData = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(decryptedData));
      readViewData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public boolean hasView(String fileName, String data) {
    ArrayList views = this.viewMap.get(fileName);
    if (views == null)
      return false; 
    Iterator iterator = views.iterator();
    while (iterator.hasNext()) {
      if (((ViewBean)iterator.next()).id.equals(data))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<Pair<String, String>> getMoreBlocks(String fileName) {
    return this.moreBlockMap.containsKey(fileName) ? this.moreBlockMap.get(fileName) : new ArrayList<Pair<String, String>>();
  }
  
  public void resetProject() {
    this.projectId = "";
    clearAllData();
  }
  
  public void parseLogicSection(String fileName, String data) {
    if (data.length() <= 0)
      return; 
    try {
      ProjectDataParser ProjectDataParser = new ProjectDataParser(fileName);
      String parsedFileName = ProjectDataParser.getFileName();
      ProjectDataParser.DataType dataType = ProjectDataParser.getDataType();
      switch (ScreenOrientationConstants.ORIENTATION_VALUES[dataType.ordinal()]) {
        default:
          return;
        case 8:
          if (!this.blockMap.containsKey(parsedFileName)) {
            this.blockMap.put(parsedFileName, new HashMap<String, ArrayList<BlockBean>>());
          } 
          this.blockMap.get(parsedFileName).put(ProjectDataParser.getEventKey(), (ArrayList<BlockBean>)ProjectDataParser.parseData(data));
          break;
        case 7:
          this.moreBlockMap.put(parsedFileName, (ArrayList<Pair<String, String>>)ProjectDataParser.parseData(data));
          break;
        case 6:
          this.eventMap.put(parsedFileName, (ArrayList<EventBean>)ProjectDataParser.parseData(data));
          break;
        case 5:
          this.componentMap.put(parsedFileName, (ArrayList<ComponentBean>)ProjectDataParser.parseData(data));
          break;
        case 4:
          this.listMap.put(parsedFileName, (ArrayList<Pair<Integer, String>>)ProjectDataParser.parseData(data));
          break;
        case 3:
          this.variableMap.put(parsedFileName, (ArrayList<Pair<Integer, String>>)ProjectDataParser.parseData(data));
          break;
      } 
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public ArrayList<Pair<Integer, String>> getListVariables(String fileName) {
    return this.listMap.containsKey(fileName) ? this.listMap.get(fileName) : new ArrayList<Pair<Integer, String>>();
  }
  
  public void saveAllData() {
    String basePath = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(basePath);
    stringBuilder.append(File.separator);
    stringBuilder.append("view");
    saveViewFile(stringBuilder.toString());
    stringBuilder = new StringBuilder();
    stringBuilder.append(basePath);
    stringBuilder.append(File.separator);
    stringBuilder.append("logic");
    saveLogicFile(stringBuilder.toString());
    deleteBackupFiles();
  }
  
  public void parseViewSection(String fileName, String data) {
    try {
      ProjectDataParser ProjectDataParser = new ProjectDataParser(fileName);
      String parsedFileName = ProjectDataParser.getFileName();
      ProjectDataParser.DataType dataType = ProjectDataParser.getDataType();
      int i = ScreenOrientationConstants.ORIENTATION_VALUES[dataType.ordinal()];
      if (i == 1) {
        this.viewMap.put(parsedFileName, (ArrayList<ViewBean>)ProjectDataParser.parseData(data));
      } else if (i == 2) {
        this.fabMap.put(parsedFileName, (ViewBean)ProjectDataParser.parseData(data));
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public ArrayList<Pair<Integer, String>> getVariables(String fileName) {
    return this.variableMap.containsKey(fileName) ? this.variableMap.get(fileName) : new ArrayList<Pair<Integer, String>>();
  }
  
  public void saveAllBackup() {
    String basePath = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(basePath);
    stringBuilder.append(File.separator);
    stringBuilder.append("view");
    saveViewFile(stringBuilder.toString());
    stringBuilder = new StringBuilder();
    stringBuilder.append(basePath);
    stringBuilder.append(File.separator);
    stringBuilder.append("logic");
    saveLogicFile(stringBuilder.toString());
  }
  
  public void removeBlockEntry(String fileName, String data) {
    if (!this.blockMap.containsKey(fileName))
      return; 
    Map map = this.blockMap.get(fileName);
    if (map == null)
      return; 
    if (map.containsKey(data))
      map.remove(data); 
  }
  
  public void removeViewTypeEvents(String fileName) {
    if (!this.eventMap.containsKey(fileName))
      return; 
    ArrayList events = this.eventMap.get(fileName);
    int i = events.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        i = j;
        if (((EventBean)events.get(j)).eventType == 4) {
          events.remove(j);
          i = j;
        } 
        continue;
      } 
      break;
    } 
  }
  
  public void removeViewEventsByTarget(String fileName, String data) {
    if (!this.eventMap.containsKey(fileName))
      return; 
    ArrayList<EventBean> events = this.eventMap.get(fileName);
    int i = events.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        EventBean eventBean = events.get(j);
        i = j;
        if (eventBean.eventType == 4) {
          i = j;
          if (eventBean.targetId.equals(data)) {
            events.remove(j);
            i = j;
          } 
        } 
        continue;
      } 
      break;
    } 
  }
  
  public final void saveLogicFile(String filePath) {
    StringBuffer stringBuffer = new StringBuffer();
    serializeLogicData(stringBuffer);
    try {
      byte[] bytes = this.fileUtil.encryptString(stringBuffer.toString());
      this.fileUtil.writeBytes(filePath, bytes);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void removeEventsByTarget(String fileName, String data) {
    if (!this.eventMap.containsKey(fileName))
      return; 
    ArrayList<EventBean> events = this.eventMap.get(fileName);
    if (events == null)
      return; 
    int i = events.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        EventBean eventBean = events.get(j);
        i = j;
        if (eventBean.targetId.equals(data)) {
          events.remove(eventBean);
          HashMap<String, HashMap<String, ArrayList<BlockBean>>> hashMap = this.blockMap;
          i = j;
          if (hashMap != null) {
            i = j;
            if (hashMap.get(fileName) != null) {
              HashMap tempEventMap = this.blockMap.get(fileName);
              StringBuilder stringBuilder = new StringBuilder();
              stringBuilder.append(eventBean.targetId);
              stringBuilder.append("_");
              stringBuilder.append(eventBean.eventName);
              i = j;
              if (tempEventMap.containsKey(stringBuilder.toString())) {
                tempEventMap = this.blockMap.get(fileName);
                stringBuilder = new StringBuilder();
                stringBuilder.append(eventBean.targetId);
                stringBuilder.append("_");
                stringBuilder.append(eventBean.eventName);
                tempEventMap.remove(stringBuilder.toString());
                i = j;
              } 
            } 
          } 
        } 
        continue;
      } 
      break;
    } 
  }
  
  public final void saveViewFile(String filePath) {
    StringBuffer stringBuffer = new StringBuffer();
    serializeViewData(stringBuffer);
    try {
      byte[] bytes = this.fileUtil.encryptString(stringBuffer.toString());
      this.fileUtil.writeBytes(filePath, bytes);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void removeMoreBlock(String fileName, String data) {
    if (!this.moreBlockMap.containsKey(fileName))
      return; 
    ArrayList moreBlocks = this.moreBlockMap.get(fileName);
    if (moreBlocks == null)
      return; 
    for (Pair pair : (ArrayList<Pair>)moreBlocks) {
      if (((String)pair.first).equals(data)) {
        moreBlocks.remove(pair);
        break;
      } 
    } 
    HashMap blockEntryMap = this.blockMap.get(fileName);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(data);
    stringBuilder.append("_");
    stringBuilder.append("moreBlock");
    if (blockEntryMap.containsKey(stringBuilder.toString())) {
      blockEntryMap = this.blockMap.get(fileName);
      StringBuilder contentBuilder = new StringBuilder();
      contentBuilder.append(data);
      contentBuilder.append("_");
      contentBuilder.append("moreBlock");
      blockEntryMap.remove(contentBuilder.toString());
    } 
  }
  
  public void removeListVariable(String fileName, String data) {
    if (!this.listMap.containsKey(fileName))
      return; 
    ArrayList listVars = this.listMap.get(fileName);
    if (listVars == null)
      return; 
    for (Pair pair : (ArrayList<Pair>)listVars) {
      if (((String)pair.second).equals(data)) {
        listVars.remove(pair);
        break;
      } 
    } 
  }
  
  public void removeVariable(String fileName, String data) {
    if (!this.variableMap.containsKey(fileName))
      return; 
    ArrayList varEntries = this.variableMap.get(fileName);
    if (varEntries == null)
      return; 
    for (Pair pair : (ArrayList<Pair>)varEntries) {
      if (((String)pair.second).equals(data)) {
        varEntries.remove(pair);
        break;
      } 
    } 
  }
  
  public boolean hasViewType(String fileName, int index) {
    ArrayList views = this.viewMap.get(fileName);
    boolean hasType = false;
    if (views != null) {
      Iterator iterator = views.iterator();
      while (iterator.hasNext()) {
        if (((ViewBean)iterator.next()).type == index) {
          hasType = true;
          break;
        } 
      } 
    } 
    return hasType;
  }
  
  public boolean hasViewMatchingType(String fileName, String data) {
    ArrayList views = this.viewMap.get(fileName);
    boolean hasMatch = false;
    if (views != null) {
      Iterator<ViewBean> iterator = views.iterator();
      while (iterator.hasNext()) {
        if (((ViewBean)iterator.next()).getClassInfo().matchesType(data)) {
          hasMatch = true;
          break;
        } 
      } 
    } 
    return hasMatch;
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
