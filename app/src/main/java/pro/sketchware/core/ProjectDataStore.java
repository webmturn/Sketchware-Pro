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
  
  public ProjectDataStore(String str) {
    clearAllData();
    this.projectId = str;
    this.fileUtil = new EncryptedFileUtil();
    this.gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
    this.buildConfig = new BuildConfig();
  }
  
  public static ArrayList<ViewBean> getSortedRootViews(ArrayList<ViewBean> list) {
    ArrayList<ViewBean> arrayList = new ArrayList<>();
    for (ViewBean viewBean : list) {
      if (viewBean.parent.equals("root"))
        arrayList.add(viewBean); 
    } 
    int j = arrayList.size();
    int i;
    int m = 0;
    for (i = 0; i < j - 1; i++) {
      for (int k = 0; k < j - i - 1; k = m) {
        int n = ((ViewBean)arrayList.get(k)).index;
        m = k + 1;
        if (n > ((ViewBean)arrayList.get(m)).index) {
          ViewBean viewBean = arrayList.get(k);
          arrayList.set(k, arrayList.get(m));
          arrayList.set(m, viewBean);
        } 
      } 
    } 
    for (ViewBean viewBean : list) {
      i = viewBean.type;
      if ((i == 2 || i == 1 || i == 36 || i == 37 || i == 38 || i == 39 || i == 40 || i == 0 || i == 12) && viewBean.parent.equals("root"))
        arrayList.addAll(getChildViews(list, viewBean)); 
    } 
    return arrayList;
  }
  
  public static ArrayList<ViewBean> getChildViews(ArrayList<ViewBean> list, ViewBean parentBean) {
    ArrayList<ViewBean> arrayList = new ArrayList<>();
    for (ViewBean viewBean : list) {
      if (viewBean.parent.equals(parentBean.id))
        arrayList.add(viewBean); 
    } 
    int j = arrayList.size();
    int i;
    int m = 0;
    for (i = 0; i < j - 1; i++) {
      for (int k = 0; k < j - i - 1; k = m) {
        int n = ((ViewBean)arrayList.get(k)).index;
        m = k + 1;
        if (n > ((ViewBean)arrayList.get(m)).index) {
          ViewBean tempBean = arrayList.get(k);
          arrayList.set(k, arrayList.get(m));
          arrayList.set(m, tempBean);
        } 
      } 
    } 
    for (ViewBean viewBean : list) {
      if (viewBean.parent.equals(parentBean.id)) {
        i = viewBean.type;
        if (i == 0 || i == 2 || i == 1 || i == 36 || i == 37 || i == 38 || i == 39 || i == 40 || i == 12)
          arrayList.addAll(getChildViews(list, viewBean)); 
      } 
    } 
    return arrayList;
  }
  
  public ComponentBean getComponent(String str, int index) {
    return !this.componentMap.containsKey(str) ? null : ((ArrayList<ComponentBean>)this.componentMap.get(str)).get(index);
  }
  
  public ArrayList<String> getAllIdentifiers(ProjectFileBean fileBean) {
    String xmlName = fileBean.getXmlName();
    String javaName = fileBean.getJavaName();
    ArrayList<Object> arrayList = new ArrayList<>();
    Iterator<Pair<Integer, String>> iterator1 = getVariables(javaName).iterator();
    while (iterator1.hasNext())
      arrayList.add(((Pair)iterator1.next()).second); 
    iterator1 = getListVariables(javaName).iterator();
    while (iterator1.hasNext())
      arrayList.add(((Pair)iterator1.next()).second); 
    iterator1 = (Iterator)getMoreBlocks(javaName).iterator();
    while (iterator1.hasNext())
      arrayList.add(((Pair)iterator1.next()).first); 
    Iterator<ViewBean> iterator = getViews(xmlName).iterator();
    while (iterator.hasNext())
      arrayList.add(((ViewBean)iterator.next()).id); 
    Iterator iteratorComp = getComponents(javaName).iterator();
    while (iteratorComp.hasNext())
      arrayList.add(((ComponentBean)iteratorComp.next()).componentId); 
    return (ArrayList)arrayList;
  }
  
  public ArrayList<EventBean> getComponentEvents(String str, ComponentBean componentBean) {
    if (!this.eventMap.containsKey(str))
      return new ArrayList<EventBean>(); 
    ArrayList<EventBean> arrayList = new ArrayList<>();
    for (EventBean eventBean : this.eventMap.get(str)) {
      if (eventBean.targetType == componentBean.type && eventBean.targetId.equals(componentBean.componentId))
        arrayList.add(eventBean); 
    } 
    return arrayList;
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
  
  public void syncWithFileManager(ProjectFileManager paramhC) {
    for (ProjectFileBean projectFileBean : paramhC.getActivities()) {
      if (!projectFileBean.hasActivityOption(8))
        removeFab(projectFileBean); 
      if (!projectFileBean.hasActivityOption(4))
        removeViewTypeEvents(projectFileBean.getJavaName()); 
    } 
    ArrayList<String> viewKeysToRemove = new ArrayList<>();
    for (Map.Entry<String, ArrayList<ViewBean>> entry : this.viewMap.entrySet()) {
      String str = (String)entry.getKey();
      if (!paramhC.hasXmlName(str)) {
        viewKeysToRemove.add(str);
        continue;
      } 
      for (ViewBean viewBean : entry.getValue()) {
        if (viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) {
          String customViewName = viewBean.customView;
          if (customViewName != null && customViewName.length() > 0 && !viewBean.customView.equals("none")) {
            Iterator iterator = paramhC.getCustomViews().iterator();
            boolean bool = false;
            while (iterator.hasNext()) {
              if (((ProjectFileBean)iterator.next()).fileName.equals(viewBean.customView))
                bool = true; 
            } 
            if (!bool)
              viewBean.customView = ""; 
          } 
        } 
      } 
    } 
    for (String str : viewKeysToRemove)
      this.viewMap.remove(str); 
    ArrayList<String> varKeysToRemove = new ArrayList<>();
    Iterator<Map.Entry<String, ArrayList<Pair<Integer, String>>>> iterator4 = this.variableMap.entrySet().iterator();
    while (iterator4.hasNext()) {
      String str = (String)((Map.Entry)iterator4.next()).getKey();
      if (!paramhC.hasJavaName(str))
        varKeysToRemove.add(str); 
    } 
    for (String str : varKeysToRemove)
      this.variableMap.remove(str); 
    ArrayList<String> keysToRemove = new ArrayList<>();
    Iterator iterator3 = this.listMap.entrySet().iterator();
    while (iterator3.hasNext()) {
      String str = (String)((Map.Entry)iterator3.next()).getKey();
      if (!paramhC.hasJavaName(str))
        keysToRemove.add(str); 
    } 
    for (String str : keysToRemove)
      this.listMap.remove(str); 
    keysToRemove = new ArrayList<String>();
    Iterator iterator1 = this.moreBlockMap.entrySet().iterator();
    while (iterator1.hasNext()) {
      String str = (String)((Map.Entry)iterator1.next()).getKey();
      if (!paramhC.hasJavaName(str))
        keysToRemove.add(str); 
    } 
    for (String str : keysToRemove)
      this.moreBlockMap.remove(str); 
    ArrayList<String> compKeysToRemove = new ArrayList<>();
    Iterator<Map.Entry<String, ArrayList<ComponentBean>>> iterator5 = this.componentMap.entrySet().iterator();
    while (iterator5.hasNext()) {
      String str = (String)((Map.Entry)iterator5.next()).getKey();
      if (!paramhC.hasJavaName(str))
        compKeysToRemove.add(str); 
    } 
    for (String str : compKeysToRemove)
      this.componentMap.remove(str); 
    ArrayList<String> eventKeysToRemove = new ArrayList<>();
    Iterator iterator2 = this.eventMap.entrySet().iterator();
    while (iterator2.hasNext()) {
      String str = (String)((Map.Entry)iterator2.next()).getKey();
      if (!paramhC.hasJavaName(str))
        eventKeysToRemove.add(str); 
    } 
    for (String str : eventKeysToRemove)
      this.eventMap.remove(str); 
    ArrayList<String> blockKeysToRemove = new ArrayList<>();
    for (Map.Entry<String, HashMap<String, ArrayList<BlockBean>>> entry : this.blockMap.entrySet()) {
      String str = (String)entry.getKey();
      if (!paramhC.hasJavaName(str)) {
        blockKeysToRemove.add(str);
        continue;
      } 
      Iterator iterator = ((HashMap)entry.getValue()).entrySet().iterator();
      while (iterator.hasNext()) {
        label128: for (BlockBean blockBean : (ArrayList<BlockBean>)((Map.Entry)iterator.next()).getValue()) {
          if (blockBean.opCode.equals("intentSetScreen")) {
            Iterator<ProjectFileBean> iterator6 = paramhC.getActivities().iterator();
            while (iterator6.hasNext()) {
              if (((ProjectFileBean)iterator6.next()).getActivityName().equals(blockBean.parameters.get(1)))
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
  
  public void syncFonts(ResourceManager paramkC) {
    ArrayList arrayList = paramkC.getFontNames();
    Iterator iterator = this.blockMap.entrySet().iterator();
    while (iterator.hasNext()) {
      Iterator iterator1 = ((HashMap)((Map.Entry)iterator.next()).getValue()).entrySet().iterator();
      while (iterator1.hasNext()) {
        for (BlockBean blockBean : (ArrayList<BlockBean>)((Map.Entry<String, ArrayList<BlockBean>>)iterator1.next()).getValue()) {
          if ("setTypeface".equals(blockBean.opCode) && arrayList.indexOf(blockBean.parameters.get(1)) < 0)
            blockBean.parameters.set(1, "default_font"); 
        } 
      } 
    } 
  }
  
  public void removeView(ProjectFileBean fileBean, ViewBean targetBean) {
    if (!this.viewMap.containsKey(fileBean.getXmlName()))
      return; 
    ArrayList arrayList = this.viewMap.get(fileBean.getXmlName());
    int i = arrayList.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        i = j;
        if (((ViewBean)arrayList.get(j)).id.equals(targetBean.id)) {
          arrayList.remove(j);
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
      String str = (String)((Map.Entry)iterator.next()).getKey();
      removeComponentsByType(str, 6);
      removeComponentsByType(str, 12);
      removeComponentsByType(str, 14);
    } 
  }
  
  public void removeFirebaseViews(ProjectLibraryBean libraryBean, ProjectFileManager paramhC) {
    if (libraryBean.useYn.equals("Y"))
      return; 
    for (ProjectFileBean projectFileBean : paramhC.getActivities()) {
      for (ViewBean viewBean : getViews(projectFileBean.getXmlName())) {
        if (viewBean.type == 17)
          removeView(projectFileBean, viewBean); 
      } 
    } 
    for (ProjectFileBean projectFileBean : paramhC.getCustomViews()) {
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
    String str = "";
    while (true) {
      String line = reader.readLine();
      if (line != null) {
        if (line.length() <= 0)
          continue; 
        if (line.charAt(0) == '@') {
          StringBuffer tempBuffer = stringBuffer;
          if (str.length() > 0) {
            parseLogicSection(str, stringBuffer.toString());
            tempBuffer = new StringBuffer();
          } 
          str = line.substring(1);
          stringBuffer = tempBuffer;
          continue;
        } 
        stringBuffer.append(line);
        stringBuffer.append("\n");
        continue;
      } 
      if (str.length() > 0 && stringBuffer.length() > 0)
        parseLogicSection(str, stringBuffer.toString()); 
      return;
    } 
  }
  
  public void initFab(String str) {
    if (this.fabMap.containsKey(str))
      return; 
    ViewBean viewBean = new ViewBean("_fab", 16);
    LayoutBean layoutBean = viewBean.layout;
    layoutBean.marginLeft = 16;
    layoutBean.marginTop = 16;
    layoutBean.marginRight = 16;
    layoutBean.marginBottom = 16;
    layoutBean.layoutGravity = 85;
    this.fabMap.put(str, viewBean);
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
  
  public void addEventBean(String str, EventBean eventBean) {
    if (!this.eventMap.containsKey(str))
      this.eventMap.put(str, new ArrayList<EventBean>()); 
    ((ArrayList<EventBean>)this.eventMap.get(str)).add(eventBean);
  }
  
  public void addView(String str, ViewBean viewBean) {
    if (!this.viewMap.containsKey(str))
      this.viewMap.put(str, new ArrayList<ViewBean>()); 
    ((ArrayList<ViewBean>)this.viewMap.get(str)).add(viewBean);
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
        ArrayList arrayList = (ArrayList)entry.getValue();
        if (arrayList == null || arrayList.size() <= 0)
          continue; 
        StringBuilder contentBuilder = new StringBuilder();
        for (int vi = 0; vi < arrayList.size(); vi++) {
          Pair pair = (Pair) arrayList.get(vi);
          contentBuilder.append(pair.first);
          contentBuilder.append(":");
          contentBuilder.append((String)pair.second);
          contentBuilder.append("\n");
        }
        String str = contentBuilder.toString();
        buffer.append("@");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_");
        stringBuilder.append("var");
        buffer.append(stringBuilder.toString());
        buffer.append("\n");
        buffer.append(str);
        buffer.append("\n");
      }  
    tempVarMap = this.listMap;
    if (tempVarMap != null && tempVarMap.size() > 0)
      for (Map.Entry<String, ArrayList<Pair<Integer, String>>> entry : this.listMap.entrySet()) {
        ArrayList arrayList = (ArrayList)entry.getValue();
        if (arrayList == null || arrayList.size() <= 0)
          continue; 
        StringBuilder contentBuilder = new StringBuilder();
        for (int li = 0; li < arrayList.size(); li++) {
          Pair pair = (Pair) arrayList.get(li);
          contentBuilder.append(pair.first);
          contentBuilder.append(":");
          contentBuilder.append((String)pair.second);
          contentBuilder.append("\n");
        }
        String str = contentBuilder.toString();
        buffer.append("@");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_");
        stringBuilder.append("list");
        buffer.append(stringBuilder.toString());
        buffer.append("\n");
        buffer.append(str);
        buffer.append("\n");
      }  
    HashMap<String, ArrayList<Pair<String, String>>> tempMoreBlockMap = this.moreBlockMap;
    if (tempMoreBlockMap != null && tempMoreBlockMap.size() > 0)
      for (Map.Entry<String, ArrayList<Pair<String, String>>> entry : this.moreBlockMap.entrySet()) {
        ArrayList arrayList = (ArrayList)entry.getValue();
        if (arrayList == null || arrayList.size() <= 0)
          continue; 
        StringBuilder contentBuilder = new StringBuilder();
        for (int fi = 0; fi < arrayList.size(); fi++) {
          Pair pair = (Pair) arrayList.get(fi);
          contentBuilder.append((String)pair.first);
          contentBuilder.append(":");
          contentBuilder.append((String)pair.second);
          contentBuilder.append("\n");
        }
        String str = contentBuilder.toString();
        buffer.append("@");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_");
        stringBuilder.append("func");
        buffer.append(stringBuilder.toString());
        buffer.append("\n");
        buffer.append(str);
        buffer.append("\n");
      }  
    HashMap<String, ArrayList<ComponentBean>> tempCompMap = this.componentMap;
    if (tempCompMap != null && tempCompMap.size() > 0)
      for (Map.Entry<String, ArrayList<ComponentBean>> entry : this.componentMap.entrySet()) {
        ArrayList arrayList = (ArrayList)entry.getValue();
        if (arrayList == null || arrayList.size() <= 0)
          continue; 
        StringBuilder contentBuilder = new StringBuilder();
        for (int ci = 0; ci < arrayList.size(); ci++) {
          ComponentBean componentBean = (ComponentBean) arrayList.get(ci);
          componentBean.clearClassInfo();
          contentBuilder.append(this.gson.toJson(componentBean));
          contentBuilder.append("\n");
        }
        String str = contentBuilder.toString();
        buffer.append("@");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_");
        stringBuilder.append("components");
        buffer.append(stringBuilder.toString());
        buffer.append("\n");
        buffer.append(str);
        buffer.append("\n");
      }  
    HashMap<String, ArrayList<EventBean>> tempEventMap = this.eventMap;
    if (tempEventMap != null && tempEventMap.size() > 0)
      for (Map.Entry<String, ArrayList<EventBean>> entry : this.eventMap.entrySet()) {
        ArrayList arrayList = (ArrayList)entry.getValue();
        if (arrayList == null || arrayList.size() <= 0)
          continue; 
        StringBuilder contentBuilder = new StringBuilder();
        for (int ei = 0; ei < arrayList.size(); ei++) {
          EventBean eventBean = (EventBean) arrayList.get(ei);
          contentBuilder.append(this.gson.toJson(eventBean));
          contentBuilder.append("\n");
        }
        String str = contentBuilder.toString();
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
        String str = (String)entry.getKey();
        HashMap blockEntryMap = (HashMap)entry.getValue();
        if (blockEntryMap == null || blockEntryMap.size() <= 0)
          continue; 
        for (Map.Entry<String, ArrayList<BlockBean>> entry1 : ((HashMap<String, ArrayList<BlockBean>>)blockEntryMap).entrySet()) {
          ArrayList arrayList = (ArrayList)entry1.getValue();
          if (arrayList == null || arrayList.size() <= 0)
            continue; 
          StringBuilder contentBuilder = new StringBuilder();
          for (int bi = 0; bi < arrayList.size(); bi++) {
            BlockBean blockBean = (BlockBean) arrayList.get(bi);
            contentBuilder.append(this.gson.toJson(blockBean));
            contentBuilder.append("\n");
          }
          String blockJson = contentBuilder.toString();
          buffer.append("@");
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(str);
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
    ArrayList arrayList = this.moreBlockMap.get(fileName);
    if (arrayList == null)
      return ""; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((String)pair.first).equals(data))
        return (String)pair.second; 
    } 
    return "";
  }
  
  public ArrayList<String> getComponentIdsByType(String str, int index) {
    ArrayList<String> componentIds = new ArrayList<>();
    if (!this.componentMap.containsKey(str))
      return componentIds; 
    ArrayList arrayList = this.componentMap.get(str);
    if (arrayList == null)
      return componentIds; 
    for (ComponentBean componentBean : (ArrayList<ComponentBean>)arrayList) {
      if (componentBean.type == index)
        componentIds.add(componentBean.componentId); 
    } 
    return componentIds;
  }
  
  public ArrayList<ViewBean> getViewWithChildren(String str, ViewBean viewBean) {
    ArrayList<ViewBean> arrayList = new ArrayList<>();
    arrayList.add(viewBean);
    arrayList.addAll(getChildViews(this.viewMap.get(str), viewBean));
    return arrayList;
  }
  
  public HashMap<String, ArrayList<BlockBean>> getBlockMap(String str) {
    return !this.blockMap.containsKey(str) ? new HashMap<String, ArrayList<BlockBean>>() : this.blockMap.get(str);
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
  
  public void syncImages(ResourceManager paramkC) {
    ArrayList arrayList = paramkC.getImageNames();
    Iterator iterator1 = this.viewMap.entrySet().iterator();
    while (iterator1.hasNext()) {
      for (ViewBean viewBean : (ArrayList<ViewBean>)((Map.Entry)iterator1.next()).getValue()) {
        if (arrayList.indexOf(viewBean.layout.backgroundResource) < 0)
          viewBean.layout.backgroundResource = null; 
        if (arrayList.indexOf(viewBean.image.resName) < 0)
          viewBean.image.resName = "default_image"; 
      } 
    } 
    iterator1 = this.fabMap.entrySet().iterator();
    while (iterator1.hasNext()) {
      ViewBean viewBean = (ViewBean)((Map.Entry)iterator1.next()).getValue();
      if (arrayList.indexOf(viewBean.image.resName) < 0)
        viewBean.image.resName = "NONE"; 
    } 
    Iterator iterator2 = this.blockMap.entrySet().iterator();
    while (iterator2.hasNext()) {
      Iterator iterator = ((HashMap)((Map.Entry)iterator2.next()).getValue()).entrySet().iterator();
      while (iterator.hasNext()) {
        for (BlockBean blockBean : (ArrayList<BlockBean>)((Map.Entry)iterator.next()).getValue()) {
          if ("setImage".equals(blockBean.opCode)) {
            if (arrayList.indexOf(blockBean.parameters.get(1)) < 0)
              blockBean.parameters.set(1, "default_image"); 
            continue;
          } 
          if ("setBgResource".equals(blockBean.opCode) && arrayList.indexOf(blockBean.parameters.get(1)) < 0)
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
  
  public void removeMapViews(ProjectLibraryBean libraryBean, ProjectFileManager paramhC) {
    if (libraryBean.useYn.equals("Y"))
      return; 
    for (ProjectFileBean projectFileBean : paramhC.getActivities()) {
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
    String str = "";
    while (true) {
      String line = reader.readLine();
      if (line != null) {
        if (line.length() <= 0)
          continue; 
        if (line.charAt(0) == '@') {
          StringBuffer tempBuffer = stringBuffer;
          if (str.length() > 0) {
            parseViewSection(str, stringBuffer.toString());
            tempBuffer = new StringBuffer();
          } 
          str = line.substring(1);
          stringBuffer = tempBuffer;
          continue;
        } 
        stringBuffer.append(line);
        stringBuffer.append("\n");
        continue;
      } 
      if (str.length() > 0 && stringBuffer.length() > 0)
        parseViewSection(str, stringBuffer.toString()); 
      return;
    } 
  }
  
  public void addListVariable(String fileName, int index, String data) {
    Pair pair = new Pair(Integer.valueOf(index), data);
    if (!this.listMap.containsKey(fileName))
      this.listMap.put(fileName, new ArrayList<Pair<Integer, String>>()); 
    ((ArrayList)this.listMap.get(fileName)).add(pair);
  }
  
  public void removeComponent(String str, ComponentBean componentBean) {
    if (!this.componentMap.containsKey(str))
      return; 
    ArrayList arrayList = this.componentMap.get(str);
    if (arrayList.indexOf(componentBean) < 0)
      return; 
    arrayList.remove(componentBean);
    removeEventsByTarget(str, componentBean.componentId);
    removeBlockReferences(str, componentBean.getClassInfo(), componentBean.componentId, false);
    this.buildConfig.constVarComponent.handleDeleteComponent(componentBean.componentId);
  }
  
  public final void serializeViewData(StringBuffer buffer) {
    HashMap<String, ArrayList<ViewBean>> tempEventMap = this.viewMap;
    if (tempEventMap != null && tempEventMap.size() > 0)
      for (Map.Entry<String, ArrayList<ViewBean>> entry : this.viewMap.entrySet()) {
        String str;
        ArrayList arrayList = (ArrayList)entry.getValue();
        if (arrayList == null || arrayList.size() <= 0)
          continue; 
        ArrayList<ViewBean> sortedViews = getSortedRootViews((ArrayList<ViewBean>)entry.getValue());
        if (sortedViews != null && sortedViews.size() > 0) {
          int b = 0;
          String viewJson = "";
          while (true) {
            str = viewJson;
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
          str = "";
        } 
        buffer.append("@");
        buffer.append((String)entry.getKey());
        buffer.append("\n");
        buffer.append(str);
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
        String str = stringBuilder.toString();
        buffer.append("@");
        stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_fab");
        buffer.append(stringBuilder.toString());
        buffer.append("\n");
        buffer.append(str);
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
        ArrayList<ClassInfo> arrayList = blockBean.getParamClassInfo();
        if (arrayList != null && arrayList.size() > 0)
          for (int b = 0; b < arrayList.size(); b++) {
            ClassInfo gx1 = arrayList.get(b);
            if (gx1 != null && gx1.isList() && ((String)blockBean.parameters.get(b)).equals(data))
              return true; 
          }  
      } 
    } 
    return false;
  }
  
  public ViewBean getViewBean(String fileName, String data) {
    ArrayList<ViewBean> arrayList = this.viewMap.get(fileName);
    if (arrayList == null)
      return null; 
    for (int b = 0; b < arrayList.size(); b++) {
      ViewBean viewBean = arrayList.get(b);
      if (data.equals(viewBean.id))
        return viewBean; 
    } 
    return null;
  }
  
  public ArrayList<String> getListNames(String str) {
    ArrayList<String> listNames = new ArrayList<>();
    if (!this.listMap.containsKey(str))
      return listNames; 
    ArrayList arrayList = this.listMap.get(str);
    if (arrayList == null)
      return listNames; 
    Iterator iterator = arrayList.iterator();
    while (iterator.hasNext())
      listNames.add((String)((Pair)iterator.next()).second); 
    return listNames;
  }
  
  public ArrayList<ComponentBean> getComponentsByType(String str, int index) {
    ArrayList<ComponentBean> filteredComponents = new ArrayList<>();
    if (!this.componentMap.containsKey(str))
      return filteredComponents; 
    ArrayList arrayList = this.componentMap.get(str);
    if (arrayList == null)
      return filteredComponents; 
    for (ComponentBean componentBean : (ArrayList<ComponentBean>)arrayList) {
      if (componentBean.type == index)
        filteredComponents.add(componentBean); 
    } 
    return filteredComponents;
  }
  
  public void syncSounds(ResourceManager paramkC) {
    ArrayList arrayList = paramkC.getSoundNames();
    Iterator iterator = this.blockMap.entrySet().iterator();
    while (iterator.hasNext()) {
      Iterator iterator1 = ((HashMap)((Map.Entry)iterator.next()).getValue()).entrySet().iterator();
      while (iterator1.hasNext()) {
        for (BlockBean blockBean : (ArrayList<BlockBean>)((Map.Entry)iterator1.next()).getValue()) {
          if (blockBean.opCode.equals("mediaplayerCreate") && arrayList.indexOf(blockBean.parameters.get(1)) < 0)
            blockBean.parameters.set(1, ""); 
          if (blockBean.opCode.equals("soundpoolLoad") && arrayList.indexOf(blockBean.parameters.get(1)) < 0)
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
        ArrayList<ClassInfo> arrayList = blockBean.getParamClassInfo();
        if (arrayList != null && arrayList.size() > 0)
          for (int b = 0; b < arrayList.size(); b++) {
            ClassInfo gx1 = arrayList.get(b);
            if (gx1 != null && gx1.isVariable() && ((String)blockBean.parameters.get(b)).equals(data))
              return true; 
          }  
      } 
    } 
    return false;
  }
  
  public ArrayList<ViewBean> getViews(String str) {
    ArrayList<ViewBean> views = this.viewMap.get(str);
    ArrayList<ViewBean> result = views;
    if (views == null)
      result = new ArrayList<>(); 
    return result;
  }
  
  public ArrayList<String> getListNamesByType(String str, int index) {
    ArrayList<String> filteredNames = new ArrayList<>();
    if (!this.listMap.containsKey(str))
      return filteredNames; 
    ArrayList arrayList = this.listMap.get(str);
    if (arrayList == null)
      return filteredNames; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((Integer)pair.first).intValue() == index)
        filteredNames.add((String)pair.second); 
    } 
    return filteredNames;
  }
  
  public ArrayList<Pair<Integer, String>> getViewsByType(String fileName, String data) {
    ArrayList<Pair<Integer, String>> filteredViews = new ArrayList<>();
    ArrayList arrayList = this.viewMap.get(fileName);
    if (arrayList == null)
      return filteredViews; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)arrayList) {
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
    ArrayList<EventBean> arrayList = this.eventMap.get(fileName);
    if (arrayList == null)
      return; 
    int i = arrayList.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        EventBean eventBean = arrayList.get(j);
        i = j;
        if (eventBean.targetId.equals(data)) {
          i = j;
          if (extra.equals(eventBean.eventName)) {
            arrayList.remove(eventBean);
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
    ArrayList arrayList = this.componentMap.get(fileName);
    if (arrayList == null)
      return false; 
    for (ComponentBean componentBean : (ArrayList<ComponentBean>)arrayList) {
      if (componentBean.type == index && componentBean.componentId.equals(data))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<ComponentBean> getComponents(String str) {
    return !this.componentMap.containsKey(str) ? new ArrayList<ComponentBean>() : this.componentMap.get(str);
  }
  
  public ArrayList<String> getVariableNamesByType(String str, int index) {
    ArrayList<String> varNames = new ArrayList<>();
    if (!this.variableMap.containsKey(str))
      return varNames; 
    ArrayList arrayList = this.variableMap.get(str);
    if (arrayList == null)
      return varNames; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
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
      String str = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(str));
      readLogicData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public boolean hasListVariable(String fileName, int index, String data) {
    ArrayList arrayList = this.listMap.get(fileName);
    if (arrayList == null)
      return false; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((Integer)pair.first).intValue() == index && ((String)pair.second).equals(data))
        return true; 
    } 
    return false;
  }
  
  public boolean hasCompoundButtonView(String fileName, String data) {
    ArrayList arrayList = this.viewMap.get(fileName);
    if (arrayList == null)
      return false; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)arrayList) {
      if (viewBean.getClassInfo().matchesType("CompoundButton") && viewBean.id.equals(data))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<ViewBean> getCustomViewBeans(String input) {
    ArrayList<ViewBean> customViews = new ArrayList<>();
    ArrayList arrayList = this.viewMap.get(input);
    if (arrayList == null)
      return customViews; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)arrayList) {
      if (viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) {
        String str = viewBean.customView;
        if (str != null && str.length() > 0 && !viewBean.customView.equals("none"))
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
      String str = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(str));
      readLogicData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public boolean hasComponentOfType(String str, int index) {
    ArrayList arrayList = this.componentMap.get(str);
    if (arrayList == null)
      return false; 
    Iterator iterator = arrayList.iterator();
    while (iterator.hasNext()) {
      if (((ComponentBean)iterator.next()).type == index)
        return true; 
    } 
    return false;
  }
  
  public boolean hasVariable(String fileName, int index, String data) {
    ArrayList arrayList = this.variableMap.get(fileName);
    if (arrayList == null)
      return false; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
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
      String str = (String)entry.getKey();
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(data);
      stringBuilder.append("_");
      stringBuilder.append("moreBlock");
      if (str.equals(stringBuilder.toString()))
        continue; 
      for (BlockBean blockBean : (ArrayList<BlockBean>)entry.getValue()) {
        if (blockBean.opCode.equals("definedFunc")) {
          int i = blockBean.spec.indexOf(" ");
          str = blockBean.spec;
          String funcName = str;
          if (i > 0)
            funcName = str.substring(0, i); 
          if (funcName.equals(data))
            return true; 
        } 
      } 
    } 
    return false;
  }
  
  public ArrayList<EventBean> getEvents(String str) {
    return !this.eventMap.containsKey(str) ? new ArrayList<EventBean>() : this.eventMap.get(str);
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
      String str = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(str));
      readViewData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void removeComponentsByType(String str, int index) {
    if (!this.componentMap.containsKey(str))
      return; 
    ArrayList<ComponentBean> arrayList = getComponentsByType(str, index);
    if (arrayList != null && arrayList.size() > 0) {
      Iterator<ComponentBean> iterator = arrayList.iterator();
      while (iterator.hasNext())
        removeComponent(str, iterator.next()); 
    } 
  }
  
  public boolean hasViewOfType(String fileName, int index, String data) {
    ArrayList arrayList = this.viewMap.get(fileName);
    if (arrayList == null)
      return false; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)arrayList) {
      if (viewBean.type == index && viewBean.id.equals(data))
        return true; 
    } 
    return false;
  }
  
  public boolean hasTextView(String fileName, String data) {
    ArrayList arrayList = this.viewMap.get(fileName);
    if (arrayList == null)
      return false; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)arrayList) {
      if (viewBean.getClassInfo().matchesType("TextView") && viewBean.id.equals(data))
        return true; 
    } 
    return false;
  }
  
  public ViewBean getFabView(String str) {
    if (!this.fabMap.containsKey(str))
      initFab(str); 
    return this.fabMap.get(str);
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
      String str = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(str));
      readViewData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public boolean hasView(String fileName, String data) {
    ArrayList arrayList = this.viewMap.get(fileName);
    if (arrayList == null)
      return false; 
    Iterator iterator = arrayList.iterator();
    while (iterator.hasNext()) {
      if (((ViewBean)iterator.next()).id.equals(data))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<Pair<String, String>> getMoreBlocks(String str) {
    return this.moreBlockMap.containsKey(str) ? this.moreBlockMap.get(str) : new ArrayList<Pair<String, String>>();
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
      String str = ProjectDataParser.getFileName();
      ProjectDataParser.DataType dataType = ProjectDataParser.getDataType();
      switch (ScreenOrientationConstants.ORIENTATION_VALUES[dataType.ordinal()]) {
        default:
          return;
        case 8:
          if (!this.blockMap.containsKey(str)) {
            this.blockMap.put(str, new HashMap<String, ArrayList<BlockBean>>());
          } 
          this.blockMap.get(str).put(ProjectDataParser.getEventKey(), (ArrayList<BlockBean>)ProjectDataParser.parseData(data));
          break;
        case 7:
          this.moreBlockMap.put(str, (ArrayList<Pair<String, String>>)ProjectDataParser.parseData(data));
          break;
        case 6:
          this.eventMap.put(str, (ArrayList<EventBean>)ProjectDataParser.parseData(data));
          break;
        case 5:
          this.componentMap.put(str, (ArrayList<ComponentBean>)ProjectDataParser.parseData(data));
          break;
        case 4:
          this.listMap.put(str, (ArrayList<Pair<Integer, String>>)ProjectDataParser.parseData(data));
          break;
        case 3:
          this.variableMap.put(str, (ArrayList<Pair<Integer, String>>)ProjectDataParser.parseData(data));
          break;
      } 
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public ArrayList<Pair<Integer, String>> getListVariables(String str) {
    return this.listMap.containsKey(str) ? this.listMap.get(str) : new ArrayList<Pair<Integer, String>>();
  }
  
  public void saveAllData() {
    String str = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("view");
    saveViewFile(stringBuilder.toString());
    stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("logic");
    saveLogicFile(stringBuilder.toString());
    deleteBackupFiles();
  }
  
  public void parseViewSection(String fileName, String data) {
    try {
      ProjectDataParser ProjectDataParser = new ProjectDataParser(fileName);
      String str = ProjectDataParser.getFileName();
      ProjectDataParser.DataType dataType = ProjectDataParser.getDataType();
      int i = ScreenOrientationConstants.ORIENTATION_VALUES[dataType.ordinal()];
      if (i == 1) {
        this.viewMap.put(str, (ArrayList<ViewBean>)ProjectDataParser.parseData(data));
      } else if (i == 2) {
        this.fabMap.put(str, (ViewBean)ProjectDataParser.parseData(data));
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public ArrayList<Pair<Integer, String>> getVariables(String str) {
    return this.variableMap.containsKey(str) ? this.variableMap.get(str) : new ArrayList<Pair<Integer, String>>();
  }
  
  public void saveAllBackup() {
    String str = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("view");
    saveViewFile(stringBuilder.toString());
    stringBuilder = new StringBuilder();
    stringBuilder.append(str);
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
  
  public void removeViewTypeEvents(String str) {
    if (!this.eventMap.containsKey(str))
      return; 
    ArrayList arrayList = this.eventMap.get(str);
    int i = arrayList.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        i = j;
        if (((EventBean)arrayList.get(j)).eventType == 4) {
          arrayList.remove(j);
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
    ArrayList<EventBean> arrayList = this.eventMap.get(fileName);
    int i = arrayList.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        EventBean eventBean = arrayList.get(j);
        i = j;
        if (eventBean.eventType == 4) {
          i = j;
          if (eventBean.targetId.equals(data)) {
            arrayList.remove(j);
            i = j;
          } 
        } 
        continue;
      } 
      break;
    } 
  }
  
  public final void saveLogicFile(String str) {
    StringBuffer stringBuffer = new StringBuffer();
    serializeLogicData(stringBuffer);
    try {
      byte[] bytes = this.fileUtil.encryptString(stringBuffer.toString());
      this.fileUtil.writeBytes(str, bytes);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void removeEventsByTarget(String fileName, String data) {
    if (!this.eventMap.containsKey(fileName))
      return; 
    ArrayList<EventBean> arrayList = this.eventMap.get(fileName);
    if (arrayList == null)
      return; 
    int i = arrayList.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        EventBean eventBean = arrayList.get(j);
        i = j;
        if (eventBean.targetId.equals(data)) {
          arrayList.remove(eventBean);
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
  
  public final void saveViewFile(String str) {
    StringBuffer stringBuffer = new StringBuffer();
    serializeViewData(stringBuffer);
    try {
      byte[] bytes = this.fileUtil.encryptString(stringBuffer.toString());
      this.fileUtil.writeBytes(str, bytes);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void removeMoreBlock(String fileName, String data) {
    if (!this.moreBlockMap.containsKey(fileName))
      return; 
    ArrayList arrayList = this.moreBlockMap.get(fileName);
    if (arrayList == null)
      return; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((String)pair.first).equals(data)) {
        arrayList.remove(pair);
        break;
      } 
    } 
    HashMap hashMap = this.blockMap.get(fileName);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(data);
    stringBuilder.append("_");
    stringBuilder.append("moreBlock");
    if (hashMap.containsKey(stringBuilder.toString())) {
      hashMap = this.blockMap.get(fileName);
      StringBuilder contentBuilder = new StringBuilder();
      contentBuilder.append(data);
      contentBuilder.append("_");
      contentBuilder.append("moreBlock");
      hashMap.remove(contentBuilder.toString());
    } 
  }
  
  public void removeListVariable(String fileName, String data) {
    if (!this.listMap.containsKey(fileName))
      return; 
    ArrayList arrayList = this.listMap.get(fileName);
    if (arrayList == null)
      return; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((String)pair.second).equals(data)) {
        arrayList.remove(pair);
        break;
      } 
    } 
  }
  
  public void removeVariable(String fileName, String data) {
    if (!this.variableMap.containsKey(fileName))
      return; 
    ArrayList arrayList = this.variableMap.get(fileName);
    if (arrayList == null)
      return; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((String)pair.second).equals(data)) {
        arrayList.remove(pair);
        break;
      } 
    } 
  }
  
  public boolean hasViewType(String str, int index) {
    ArrayList arrayList = this.viewMap.get(str);
    boolean bool = false;
    if (arrayList != null) {
      Iterator iterator = arrayList.iterator();
      while (iterator.hasNext()) {
        if (((ViewBean)iterator.next()).type == index) {
          bool = true;
          break;
        } 
      } 
    } 
    return bool;
  }
  
  public boolean hasViewMatchingType(String fileName, String data) {
    ArrayList arrayList = this.viewMap.get(fileName);
    boolean bool = false;
    if (arrayList != null) {
      Iterator<ViewBean> iterator = arrayList.iterator();
      while (iterator.hasNext()) {
        if (((ViewBean)iterator.next()).getClassInfo().matchesType(data)) {
          bool = true;
          break;
        } 
      } 
    } 
    return bool;
  }
  
  public final String getSimpleClassName(String input) {
    String str = input;
    if (str.contains(".")) {
      String[] parts = str.split("\\.");
      str = parts[parts.length - 1];
    } 
    return str;
  }
}
