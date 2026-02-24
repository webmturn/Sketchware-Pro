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
  
  public ProjectDataStore(String paramString) {
    clearAllData();
    this.projectId = paramString;
    this.fileUtil = new EncryptedFileUtil();
    this.gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
    this.buildConfig = new BuildConfig();
  }
  
  public static ArrayList<ViewBean> getSortedRootViews(ArrayList<ViewBean> paramArrayList) {
    ArrayList<ViewBean> arrayList = new ArrayList<>();
    for (ViewBean viewBean : paramArrayList) {
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
    for (ViewBean viewBean : paramArrayList) {
      i = viewBean.type;
      if ((i == 2 || i == 1 || i == 36 || i == 37 || i == 38 || i == 39 || i == 40 || i == 0 || i == 12) && viewBean.parent.equals("root"))
        arrayList.addAll(getChildViews(paramArrayList, viewBean)); 
    } 
    return arrayList;
  }
  
  public static ArrayList<ViewBean> getChildViews(ArrayList<ViewBean> paramArrayList, ViewBean paramViewBean) {
    ArrayList<ViewBean> arrayList = new ArrayList<>();
    for (ViewBean viewBean : paramArrayList) {
      if (viewBean.parent.equals(paramViewBean.id))
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
    for (ViewBean viewBean : paramArrayList) {
      if (viewBean.parent.equals(paramViewBean.id)) {
        i = viewBean.type;
        if (i == 0 || i == 2 || i == 1 || i == 36 || i == 37 || i == 38 || i == 39 || i == 40 || i == 12)
          arrayList.addAll(getChildViews(paramArrayList, viewBean)); 
      } 
    } 
    return arrayList;
  }
  
  public ComponentBean getComponent(String paramString, int paramInt) {
    return !this.componentMap.containsKey(paramString) ? null : ((ArrayList<ComponentBean>)this.componentMap.get(paramString)).get(paramInt);
  }
  
  public ArrayList<String> getAllIdentifiers(ProjectFileBean paramProjectFileBean) {
    String str1 = paramProjectFileBean.getXmlName();
    String str2 = paramProjectFileBean.getJavaName();
    ArrayList<Object> arrayList = new ArrayList<>();
    Iterator<Pair<Integer, String>> iterator1 = getVariables(str2).iterator();
    while (iterator1.hasNext())
      arrayList.add(((Pair)iterator1.next()).second); 
    iterator1 = getListVariables(str2).iterator();
    while (iterator1.hasNext())
      arrayList.add(((Pair)iterator1.next()).second); 
    iterator1 = (Iterator)getMoreBlocks(str2).iterator();
    while (iterator1.hasNext())
      arrayList.add(((Pair)iterator1.next()).first); 
    Iterator<ViewBean> iterator = getViews(str1).iterator();
    while (iterator.hasNext())
      arrayList.add(((ViewBean)iterator.next()).id); 
    Iterator iteratorComp = getComponents(str2).iterator();
    while (iteratorComp.hasNext())
      arrayList.add(((ComponentBean)iteratorComp.next()).componentId); 
    return (ArrayList)arrayList;
  }
  
  public ArrayList<EventBean> getComponentEvents(String paramString, ComponentBean paramComponentBean) {
    if (!this.eventMap.containsKey(paramString))
      return new ArrayList<EventBean>(); 
    ArrayList<EventBean> arrayList = new ArrayList<>();
    for (EventBean eventBean : this.eventMap.get(paramString)) {
      if (eventBean.targetType == paramComponentBean.type && eventBean.targetId.equals(paramComponentBean.componentId))
        arrayList.add(eventBean); 
    } 
    return arrayList;
  }
  
  public ArrayList<BlockBean> getBlocks(String paramString1, String paramString2) {
    if (!this.blockMap.containsKey(paramString1))
      return new ArrayList<BlockBean>(); 
    Map map = this.blockMap.get(paramString1);
    return (map == null) ? new ArrayList<BlockBean>() : (!map.containsKey(paramString2) ? new ArrayList<BlockBean>() : (ArrayList<BlockBean>)map.get(paramString2));
  }
  
  public void deleteBackupFiles() {
    String str1 = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder2 = new StringBuilder();
    stringBuilder2.append(str1);
    stringBuilder2.append(File.separator);
    stringBuilder2.append("view");
    String str2 = stringBuilder2.toString();
    this.fileUtil.deleteFileByPath(str2);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str1);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("logic");
    str1 = stringBuilder1.toString();
    this.fileUtil.deleteFileByPath(str1);
  }
  
  public void syncWithFileManager(ProjectFileManager paramhC) {
    for (ProjectFileBean projectFileBean : paramhC.getActivities()) {
      if (!projectFileBean.hasActivityOption(8))
        removeFab(projectFileBean); 
      if (!projectFileBean.hasActivityOption(4))
        removeViewTypeEvents(projectFileBean.getJavaName()); 
    } 
    ArrayList<String> arrayList2 = new ArrayList<>();
    for (Map.Entry<String, ArrayList<ViewBean>> entry : this.viewMap.entrySet()) {
      String str = (String)entry.getKey();
      if (!paramhC.hasXmlName(str)) {
        arrayList2.add(str);
        continue;
      } 
      for (ViewBean viewBean : entry.getValue()) {
        if (viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) {
          String str1 = viewBean.customView;
          if (str1 != null && str1.length() > 0 && !viewBean.customView.equals("none")) {
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
    for (String str : arrayList2)
      this.viewMap.remove(str); 
    ArrayList<String> arrayList6 = new ArrayList<>();
    Iterator<Map.Entry<String, ArrayList<Pair<Integer, String>>>> iterator4 = this.variableMap.entrySet().iterator();
    while (iterator4.hasNext()) {
      String str = (String)((Map.Entry)iterator4.next()).getKey();
      if (!paramhC.hasJavaName(str))
        arrayList6.add(str); 
    } 
    for (String str : arrayList6)
      this.variableMap.remove(str); 
    ArrayList<String> arrayList4 = new ArrayList<>();
    Iterator iterator3 = this.listMap.entrySet().iterator();
    while (iterator3.hasNext()) {
      String str = (String)((Map.Entry)iterator3.next()).getKey();
      if (!paramhC.hasJavaName(str))
        arrayList4.add(str); 
    } 
    for (String str : arrayList4)
      this.listMap.remove(str); 
    arrayList4 = new ArrayList<String>();
    Iterator iterator1 = this.moreBlockMap.entrySet().iterator();
    while (iterator1.hasNext()) {
      String str = (String)((Map.Entry)iterator1.next()).getKey();
      if (!paramhC.hasJavaName(str))
        arrayList4.add(str); 
    } 
    for (String str : arrayList4)
      this.moreBlockMap.remove(str); 
    ArrayList<String> arrayList1 = new ArrayList<>();
    Iterator<Map.Entry<String, ArrayList<ComponentBean>>> iterator5 = this.componentMap.entrySet().iterator();
    while (iterator5.hasNext()) {
      String str = (String)((Map.Entry)iterator5.next()).getKey();
      if (!paramhC.hasJavaName(str))
        arrayList1.add(str); 
    } 
    for (String str : arrayList1)
      this.componentMap.remove(str); 
    ArrayList<String> arrayList5 = new ArrayList<>();
    Iterator iterator2 = this.eventMap.entrySet().iterator();
    while (iterator2.hasNext()) {
      String str = (String)((Map.Entry)iterator2.next()).getKey();
      if (!paramhC.hasJavaName(str))
        arrayList5.add(str); 
    } 
    for (String str : arrayList5)
      this.eventMap.remove(str); 
    ArrayList<String> arrayList3 = new ArrayList<>();
    for (Map.Entry<String, HashMap<String, ArrayList<BlockBean>>> entry : this.blockMap.entrySet()) {
      String str = (String)entry.getKey();
      if (!paramhC.hasJavaName(str)) {
        arrayList3.add(str);
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
    for (String str : arrayList3)
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
  
  public void removeView(ProjectFileBean paramProjectFileBean, ViewBean paramViewBean) {
    if (!this.viewMap.containsKey(paramProjectFileBean.getXmlName()))
      return; 
    ArrayList arrayList = this.viewMap.get(paramProjectFileBean.getXmlName());
    int i = arrayList.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        i = j;
        if (((ViewBean)arrayList.get(j)).id.equals(paramViewBean.id)) {
          arrayList.remove(j);
          break;
        } 
        continue;
      } 
      break;
    } 
    i = paramProjectFileBean.fileType;
    if (i == 0) {
      removeEventsByTarget(paramProjectFileBean.getJavaName(), paramViewBean.id);
      removeBlockReferences(paramProjectFileBean.getJavaName(), paramViewBean.getClassInfo(), paramViewBean.id, true);
    } else if (i == 1) {
      ArrayList<Pair> arrayList1 = new ArrayList<>();
      for (Map.Entry<String, ArrayList<ViewBean>> entry : this.viewMap.entrySet()) {
        for (ViewBean viewBean : entry.getValue()) {
          if ((viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) && viewBean.customView.equals(paramProjectFileBean.fileName)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(viewBean.id);
            stringBuilder.append("_");
            stringBuilder.append("onBindCustomView");
            String str2 = stringBuilder.toString();
            String str1 = (String)entry.getKey();
            arrayList1.add(new Pair(ProjectFileBean.getJavaName(str1.substring(0, str1.lastIndexOf(".xml"))), str2));
          } 
        } 
      } 
      label70: for (Pair pair : arrayList1) {
        if (this.blockMap.containsKey(pair.first)) {
          Map map = this.blockMap.get(pair.first);
          if (map.containsKey(pair.second)) {
            ArrayList<BlockBean> arrayList2 = (ArrayList)map.get(pair.second);
            if (arrayList2 == null || arrayList2.size() <= 0)
              continue; 
            i = arrayList2.size();
            label68: while (true) {
              int j = i - 1;
              if (j >= 0) {
                BlockBean blockBean = arrayList2.get(j);
                ClassInfo gx = blockBean.getClassInfo();
                if (gx != null && gx.isExactType(paramViewBean.getClassInfo().getClassName()) && blockBean.spec.equals(paramViewBean.id)) {
                  arrayList2.remove(j);
                  i = j;
                  continue;
                } 
                ArrayList<ClassInfo> arrayList3 = blockBean.getParamClassInfo();
                i = j;
                if (arrayList3 != null) {
                  i = j;
                  if (arrayList3.size() > 0) {
                    int b = 0;
                    while (true) {
                      i = j;
                      if (b < arrayList3.size()) {
                        ClassInfo gx1 = arrayList3.get(b);
                        if (gx1 != null && paramViewBean.getClassInfo().isAssignableFrom(gx1) && ((String)blockBean.parameters.get(b)).equals(paramViewBean.id))
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
      removeViewEventsByTarget(paramProjectFileBean.getDrawersJavaName(), paramViewBean.id);
    } 
  }
  
  public void removeAdmobComponents(ProjectLibraryBean paramProjectLibraryBean) {
    if (paramProjectLibraryBean.useYn.equals("Y"))
      return; 
    Iterator iterator = this.componentMap.entrySet().iterator();
    while (iterator.hasNext()) {
      String str = (String)((Map.Entry)iterator.next()).getKey();
      removeComponentsByType(str, 6);
      removeComponentsByType(str, 12);
      removeComponentsByType(str, 14);
    } 
  }
  
  public void removeFirebaseViews(ProjectLibraryBean paramProjectLibraryBean, ProjectFileManager paramhC) {
    if (paramProjectLibraryBean.useYn.equals("Y"))
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
  
  public void readLogicData(BufferedReader paramBufferedReader) throws java.io.IOException {
    HashMap<String, ArrayList<Pair<Integer, String>>> hashMap4 = this.variableMap;
    if (hashMap4 != null)
      hashMap4.clear(); 
    hashMap4 = this.listMap;
    if (hashMap4 != null)
      hashMap4.clear(); 
    HashMap<String, ArrayList<Pair<String, String>>> hashMap3 = this.moreBlockMap;
    if (hashMap3 != null)
      hashMap3.clear(); 
    HashMap<String, ArrayList<ComponentBean>> hashMap2 = this.componentMap;
    if (hashMap2 != null)
      hashMap2.clear(); 
    HashMap<String, ArrayList<EventBean>> hashMap1 = this.eventMap;
    if (hashMap1 != null)
      hashMap1.clear(); 
    HashMap<String, HashMap<String, ArrayList<BlockBean>>> hashMap = this.blockMap;
    if (hashMap != null)
      hashMap.clear(); 
    StringBuffer stringBuffer = new StringBuffer();
    String str = "";
    while (true) {
      String str1 = paramBufferedReader.readLine();
      if (str1 != null) {
        if (str1.length() <= 0)
          continue; 
        if (str1.charAt(0) == '@') {
          StringBuffer stringBuffer1 = stringBuffer;
          if (str.length() > 0) {
            parseLogicSection(str, stringBuffer.toString());
            stringBuffer1 = new StringBuffer();
          } 
          str = str1.substring(1);
          stringBuffer = stringBuffer1;
          continue;
        } 
        stringBuffer.append(str1);
        stringBuffer.append("\n");
        continue;
      } 
      if (str.length() > 0 && stringBuffer.length() > 0)
        parseLogicSection(str, stringBuffer.toString()); 
      return;
    } 
  }
  
  public void initFab(String paramString) {
    if (this.fabMap.containsKey(paramString))
      return; 
    ViewBean viewBean = new ViewBean("_fab", 16);
    LayoutBean layoutBean = viewBean.layout;
    layoutBean.marginLeft = 16;
    layoutBean.marginTop = 16;
    layoutBean.marginRight = 16;
    layoutBean.marginBottom = 16;
    layoutBean.layoutGravity = 85;
    this.fabMap.put(paramString, viewBean);
  }
  
  public void addEvent(String paramString1, int paramInt1, int paramInt2, String paramString2, String paramString3) {
    if (!this.eventMap.containsKey(paramString1))
      this.eventMap.put(paramString1, new ArrayList<EventBean>()); 
    ((ArrayList<EventBean>)this.eventMap.get(paramString1)).add(new EventBean(paramInt1, paramInt2, paramString2, paramString3));
  }
  
  public void addComponent(String paramString1, int paramInt, String paramString2) {
    if (!this.componentMap.containsKey(paramString1))
      this.componentMap.put(paramString1, new ArrayList<ComponentBean>()); 
    ((ArrayList<ComponentBean>)this.componentMap.get(paramString1)).add(new ComponentBean(paramInt, paramString2));
  }
  
  public void addComponentWithParam(String paramString1, int paramInt, String paramString2, String paramString3) {
    if (!this.componentMap.containsKey(paramString1))
      this.componentMap.put(paramString1, new ArrayList<ComponentBean>()); 
    ((ArrayList<ComponentBean>)this.componentMap.get(paramString1)).add(new ComponentBean(paramInt, paramString2, paramString3));
  }
  
  public void removeBlockReferences(String paramString1, ClassInfo paramGx, String paramString2, boolean paramBoolean) {
    if (!this.blockMap.containsKey(paramString1))
      return; 
    Map map = this.blockMap.get(paramString1);
    if (map == null)
      return; 
    label44: for (Map.Entry<String, ArrayList<BlockBean>> entry : ((Map<String, ArrayList<BlockBean>>)map).entrySet()) {
      if (paramBoolean && ((String)entry.getKey()).substring(((String)entry.getKey()).lastIndexOf("_") + 1).equals("onBindCustomView"))
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
          if (gx != null && gx.isExactType(paramGx.getClassName()) && blockBean.spec.equals(paramString2)) {
            arrayList.remove(j);
            i = j;
            continue;
          } 
          ArrayList<ClassInfo> arrayList1 = blockBean.getParamClassInfo();
          i = j;
          if (arrayList1 != null) {
            i = j;
            if (arrayList1.size() > 0) {
              int b = 0;
              while (true) {
                i = j;
                if (b < arrayList1.size()) {
                  ClassInfo gx1 = arrayList1.get(b);
                  if (gx1 != null && paramGx.isAssignableFrom(gx1) && ((String)blockBean.parameters.get(b)).equals(paramString2))
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
  
  public void addEventBean(String paramString, EventBean paramEventBean) {
    if (!this.eventMap.containsKey(paramString))
      this.eventMap.put(paramString, new ArrayList<EventBean>()); 
    ((ArrayList<EventBean>)this.eventMap.get(paramString)).add(paramEventBean);
  }
  
  public void addView(String paramString, ViewBean paramViewBean) {
    if (!this.viewMap.containsKey(paramString))
      this.viewMap.put(paramString, new ArrayList<ViewBean>()); 
    ((ArrayList<ViewBean>)this.viewMap.get(paramString)).add(paramViewBean);
  }
  
  public void addMoreBlock(String paramString1, String paramString2, String paramString3) {
    Pair pair = new Pair(paramString2, paramString3);
    if (!this.moreBlockMap.containsKey(paramString1))
      this.moreBlockMap.put(paramString1, new ArrayList<Pair<String, String>>()); 
    ((ArrayList)this.moreBlockMap.get(paramString1)).add(pair);
  }
  
  public void putBlocks(String paramString1, String paramString2, ArrayList<BlockBean> paramArrayList) {
    if (!this.blockMap.containsKey(paramString1))
      this.blockMap.put(paramString1, new HashMap<String, ArrayList<BlockBean>>()); 
    ((Map<String, ArrayList<BlockBean>>)this.blockMap.get(paramString1)).put(paramString2, paramArrayList);
  }
  
  public final void serializeLogicData(StringBuffer paramStringBuffer) {
    HashMap<String, ArrayList<Pair<Integer, String>>> hashMap4 = this.variableMap;
    if (hashMap4 != null && hashMap4.size() > 0)
      for (Map.Entry<String, ArrayList<Pair<Integer, String>>> entry : this.variableMap.entrySet()) {
        ArrayList arrayList = (ArrayList)entry.getValue();
        if (arrayList == null || arrayList.size() <= 0)
          continue; 
        StringBuilder stringBuilder1 = new StringBuilder();
        for (int vi = 0; vi < arrayList.size(); vi++) {
          Pair pair = (Pair) arrayList.get(vi);
          stringBuilder1.append(pair.first);
          stringBuilder1.append(":");
          stringBuilder1.append((String)pair.second);
          stringBuilder1.append("\n");
        }
        String str = stringBuilder1.toString();
        paramStringBuffer.append("@");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_");
        stringBuilder.append("var");
        paramStringBuffer.append(stringBuilder.toString());
        paramStringBuffer.append("\n");
        paramStringBuffer.append(str);
        paramStringBuffer.append("\n");
      }  
    hashMap4 = this.listMap;
    if (hashMap4 != null && hashMap4.size() > 0)
      for (Map.Entry<String, ArrayList<Pair<Integer, String>>> entry : this.listMap.entrySet()) {
        ArrayList arrayList = (ArrayList)entry.getValue();
        if (arrayList == null || arrayList.size() <= 0)
          continue; 
        StringBuilder stringBuilder1 = new StringBuilder();
        for (int li = 0; li < arrayList.size(); li++) {
          Pair pair = (Pair) arrayList.get(li);
          stringBuilder1.append(pair.first);
          stringBuilder1.append(":");
          stringBuilder1.append((String)pair.second);
          stringBuilder1.append("\n");
        }
        String str = stringBuilder1.toString();
        paramStringBuffer.append("@");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_");
        stringBuilder.append("list");
        paramStringBuffer.append(stringBuilder.toString());
        paramStringBuffer.append("\n");
        paramStringBuffer.append(str);
        paramStringBuffer.append("\n");
      }  
    HashMap<String, ArrayList<Pair<String, String>>> hashMap3 = this.moreBlockMap;
    if (hashMap3 != null && hashMap3.size() > 0)
      for (Map.Entry<String, ArrayList<Pair<String, String>>> entry : this.moreBlockMap.entrySet()) {
        ArrayList arrayList = (ArrayList)entry.getValue();
        if (arrayList == null || arrayList.size() <= 0)
          continue; 
        StringBuilder stringBuilder1 = new StringBuilder();
        for (int fi = 0; fi < arrayList.size(); fi++) {
          Pair pair = (Pair) arrayList.get(fi);
          stringBuilder1.append((String)pair.first);
          stringBuilder1.append(":");
          stringBuilder1.append((String)pair.second);
          stringBuilder1.append("\n");
        }
        String str = stringBuilder1.toString();
        paramStringBuffer.append("@");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_");
        stringBuilder.append("func");
        paramStringBuffer.append(stringBuilder.toString());
        paramStringBuffer.append("\n");
        paramStringBuffer.append(str);
        paramStringBuffer.append("\n");
      }  
    HashMap<String, ArrayList<ComponentBean>> hashMap2 = this.componentMap;
    if (hashMap2 != null && hashMap2.size() > 0)
      for (Map.Entry<String, ArrayList<ComponentBean>> entry : this.componentMap.entrySet()) {
        ArrayList arrayList = (ArrayList)entry.getValue();
        if (arrayList == null || arrayList.size() <= 0)
          continue; 
        StringBuilder stringBuilder1 = new StringBuilder();
        for (int ci = 0; ci < arrayList.size(); ci++) {
          ComponentBean componentBean = (ComponentBean) arrayList.get(ci);
          componentBean.clearClassInfo();
          stringBuilder1.append(this.gson.toJson(componentBean));
          stringBuilder1.append("\n");
        }
        String str = stringBuilder1.toString();
        paramStringBuffer.append("@");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_");
        stringBuilder.append("components");
        paramStringBuffer.append(stringBuilder.toString());
        paramStringBuffer.append("\n");
        paramStringBuffer.append(str);
        paramStringBuffer.append("\n");
      }  
    HashMap<String, ArrayList<EventBean>> hashMap1 = this.eventMap;
    if (hashMap1 != null && hashMap1.size() > 0)
      for (Map.Entry<String, ArrayList<EventBean>> entry : this.eventMap.entrySet()) {
        ArrayList arrayList = (ArrayList)entry.getValue();
        if (arrayList == null || arrayList.size() <= 0)
          continue; 
        StringBuilder stringBuilder1 = new StringBuilder();
        for (int ei = 0; ei < arrayList.size(); ei++) {
          EventBean eventBean = (EventBean) arrayList.get(ei);
          stringBuilder1.append(this.gson.toJson(eventBean));
          stringBuilder1.append("\n");
        }
        String str = stringBuilder1.toString();
        paramStringBuffer.append("@");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_");
        stringBuilder.append("events");
        paramStringBuffer.append(stringBuilder.toString());
        paramStringBuffer.append("\n");
        paramStringBuffer.append(stringBuilder1.toString());
        paramStringBuffer.append("\n");
      }  
    HashMap<String, HashMap<String, ArrayList<BlockBean>>> hashMap = this.blockMap;
    if (hashMap != null && hashMap.size() > 0)
      for (Map.Entry<String, HashMap<String, ArrayList<BlockBean>>> entry : this.blockMap.entrySet()) {
        String str = (String)entry.getKey();
        HashMap hashMap5 = (HashMap)entry.getValue();
        if (hashMap5 == null || hashMap5.size() <= 0)
          continue; 
        for (Map.Entry<String, ArrayList<BlockBean>> entry1 : ((HashMap<String, ArrayList<BlockBean>>)hashMap5).entrySet()) {
          ArrayList arrayList = (ArrayList)entry1.getValue();
          if (arrayList == null || arrayList.size() <= 0)
            continue; 
          StringBuilder stringBuilder1 = new StringBuilder();
          for (int bi = 0; bi < arrayList.size(); bi++) {
            BlockBean blockBean = (BlockBean) arrayList.get(bi);
            stringBuilder1.append(this.gson.toJson(blockBean));
            stringBuilder1.append("\n");
          }
          String str1 = stringBuilder1.toString();
          paramStringBuffer.append("@");
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(str);
          stringBuilder.append("_");
          stringBuilder.append((String)entry1.getKey());
          paramStringBuffer.append(stringBuilder.toString());
          paramStringBuffer.append("\n");
          paramStringBuffer.append(str1);
          paramStringBuffer.append("\n");
        } 
      }  
  }
  
  public String getMoreBlockSpec(String paramString1, String paramString2) {
    if (!this.moreBlockMap.containsKey(paramString1))
      return ""; 
    ArrayList arrayList = this.moreBlockMap.get(paramString1);
    if (arrayList == null)
      return ""; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((String)pair.first).equals(paramString2))
        return (String)pair.second; 
    } 
    return "";
  }
  
  public ArrayList<String> getComponentIdsByType(String paramString, int paramInt) {
    ArrayList<String> arrayList1 = new ArrayList<>();
    if (!this.componentMap.containsKey(paramString))
      return arrayList1; 
    ArrayList arrayList = this.componentMap.get(paramString);
    if (arrayList == null)
      return arrayList1; 
    for (ComponentBean componentBean : (ArrayList<ComponentBean>)arrayList) {
      if (componentBean.type == paramInt)
        arrayList1.add(componentBean.componentId); 
    } 
    return arrayList1;
  }
  
  public ArrayList<ViewBean> getViewWithChildren(String paramString, ViewBean paramViewBean) {
    ArrayList<ViewBean> arrayList = new ArrayList<>();
    arrayList.add(paramViewBean);
    arrayList.addAll(getChildViews(this.viewMap.get(paramString), paramViewBean));
    return arrayList;
  }
  
  public HashMap<String, ArrayList<BlockBean>> getBlockMap(String paramString) {
    return !this.blockMap.containsKey(paramString) ? new HashMap<String, ArrayList<BlockBean>>() : this.blockMap.get(paramString);
  }
  
  public void clearAllData() {
    HashMap<String, ArrayList<ViewBean>> hashMap4 = this.viewMap;
    if (hashMap4 != null)
      hashMap4.clear(); 
    HashMap<String, HashMap<String, ArrayList<BlockBean>>> hashMap3 = this.blockMap;
    if (hashMap3 != null)
      hashMap3.clear(); 
    HashMap<String, ArrayList<Pair<Integer, String>>> hashMap2 = this.variableMap;
    if (hashMap2 != null)
      hashMap2.clear(); 
    hashMap2 = this.listMap;
    if (hashMap2 != null)
      hashMap2.clear(); 
    HashMap<String, ArrayList<ComponentBean>> hashMap1 = this.componentMap;
    if (hashMap1 != null)
      hashMap1.clear(); 
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
  
  public void removeFab(ProjectFileBean paramProjectFileBean) {
    if (this.fabMap.containsKey(paramProjectFileBean.getXmlName()))
      this.fabMap.remove(paramProjectFileBean.getXmlName()); 
    removeEventsByTarget(paramProjectFileBean.getJavaName(), "_fab");
  }
  
  public void removeMapViews(ProjectLibraryBean paramProjectLibraryBean, ProjectFileManager paramhC) {
    if (paramProjectLibraryBean.useYn.equals("Y"))
      return; 
    for (ProjectFileBean projectFileBean : paramhC.getActivities()) {
      for (ViewBean viewBean : getViews(projectFileBean.getXmlName())) {
        if (viewBean.type == 18)
          removeView(projectFileBean, viewBean); 
      } 
    } 
  }
  
  public void readViewData(BufferedReader paramBufferedReader) throws java.io.IOException {
    HashMap<String, ArrayList<ViewBean>> hashMap1 = this.viewMap;
    if (hashMap1 != null)
      hashMap1.clear(); 
    HashMap<String, ViewBean> hashMap = this.fabMap;
    if (hashMap != null)
      hashMap.clear(); 
    StringBuffer stringBuffer = new StringBuffer();
    String str = "";
    while (true) {
      String str1 = paramBufferedReader.readLine();
      if (str1 != null) {
        if (str1.length() <= 0)
          continue; 
        if (str1.charAt(0) == '@') {
          StringBuffer stringBuffer1 = stringBuffer;
          if (str.length() > 0) {
            parseViewSection(str, stringBuffer.toString());
            stringBuffer1 = new StringBuffer();
          } 
          str = str1.substring(1);
          stringBuffer = stringBuffer1;
          continue;
        } 
        stringBuffer.append(str1);
        stringBuffer.append("\n");
        continue;
      } 
      if (str.length() > 0 && stringBuffer.length() > 0)
        parseViewSection(str, stringBuffer.toString()); 
      return;
    } 
  }
  
  public void addListVariable(String paramString1, int paramInt, String paramString2) {
    Pair pair = new Pair(Integer.valueOf(paramInt), paramString2);
    if (!this.listMap.containsKey(paramString1))
      this.listMap.put(paramString1, new ArrayList<Pair<Integer, String>>()); 
    ((ArrayList)this.listMap.get(paramString1)).add(pair);
  }
  
  public void removeComponent(String paramString, ComponentBean paramComponentBean) {
    if (!this.componentMap.containsKey(paramString))
      return; 
    ArrayList arrayList = this.componentMap.get(paramString);
    if (arrayList.indexOf(paramComponentBean) < 0)
      return; 
    arrayList.remove(paramComponentBean);
    removeEventsByTarget(paramString, paramComponentBean.componentId);
    removeBlockReferences(paramString, paramComponentBean.getClassInfo(), paramComponentBean.componentId, false);
    this.buildConfig.constVarComponent.handleDeleteComponent(paramComponentBean.componentId);
  }
  
  public final void serializeViewData(StringBuffer paramStringBuffer) {
    HashMap<String, ArrayList<ViewBean>> hashMap1 = this.viewMap;
    if (hashMap1 != null && hashMap1.size() > 0)
      for (Map.Entry<String, ArrayList<ViewBean>> entry : this.viewMap.entrySet()) {
        String str;
        ArrayList arrayList = (ArrayList)entry.getValue();
        if (arrayList == null || arrayList.size() <= 0)
          continue; 
        ArrayList<ViewBean> arrayList1 = getSortedRootViews((ArrayList<ViewBean>)entry.getValue());
        if (arrayList1 != null && arrayList1.size() > 0) {
          int b = 0;
          String str1 = "";
          while (true) {
            str = str1;
            if (b < arrayList1.size()) {
              ViewBean viewBean = arrayList1.get(b);
              viewBean.clearClassInfo();
              StringBuilder stringBuilder = new StringBuilder();
              stringBuilder.append(str1);
              stringBuilder.append(this.gson.toJson(viewBean));
              stringBuilder.append("\n");
              str1 = stringBuilder.toString();
              b++;
              continue;
            } 
            break;
          } 
        } else {
          str = "";
        } 
        paramStringBuffer.append("@");
        paramStringBuffer.append((String)entry.getKey());
        paramStringBuffer.append("\n");
        paramStringBuffer.append(str);
        paramStringBuffer.append("\n");
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
        paramStringBuffer.append("@");
        stringBuilder = new StringBuilder();
        stringBuilder.append((String)entry.getKey());
        stringBuilder.append("_fab");
        paramStringBuffer.append(stringBuilder.toString());
        paramStringBuffer.append("\n");
        paramStringBuffer.append(str);
        paramStringBuffer.append("\n");
      }  
  }
  
  public boolean isListUsedInBlocks(String paramString1, String paramString2, String paramString3) {
    Map map = this.blockMap.get(paramString1);
    if (map == null)
      return false; 
    for (Map.Entry<String, ArrayList<BlockBean>> entry : ((Map<String, ArrayList<BlockBean>>)map).entrySet()) {
      if (((String)entry.getKey()).equals(paramString3))
        continue; 
      for (BlockBean blockBean : (ArrayList<BlockBean>)entry.getValue()) {
        ClassInfo gx = blockBean.getClassInfo();
        if (gx != null && gx.isList() && blockBean.spec.equals(paramString2))
          return true; 
        ArrayList<ClassInfo> arrayList = blockBean.getParamClassInfo();
        if (arrayList != null && arrayList.size() > 0)
          for (int b = 0; b < arrayList.size(); b++) {
            ClassInfo gx1 = arrayList.get(b);
            if (gx1 != null && gx1.isList() && ((String)blockBean.parameters.get(b)).equals(paramString2))
              return true; 
          }  
      } 
    } 
    return false;
  }
  
  public ViewBean getViewBean(String paramString1, String paramString2) {
    ArrayList<ViewBean> arrayList = this.viewMap.get(paramString1);
    if (arrayList == null)
      return null; 
    for (int b = 0; b < arrayList.size(); b++) {
      ViewBean viewBean = arrayList.get(b);
      if (paramString2.equals(viewBean.id))
        return viewBean; 
    } 
    return null;
  }
  
  public ArrayList<String> getListNames(String paramString) {
    ArrayList<String> arrayList1 = new ArrayList<>();
    if (!this.listMap.containsKey(paramString))
      return arrayList1; 
    ArrayList arrayList = this.listMap.get(paramString);
    if (arrayList == null)
      return arrayList1; 
    Iterator iterator = arrayList.iterator();
    while (iterator.hasNext())
      arrayList1.add((String)((Pair)iterator.next()).second); 
    return arrayList1;
  }
  
  public ArrayList<ComponentBean> getComponentsByType(String paramString, int paramInt) {
    ArrayList<ComponentBean> arrayList1 = new ArrayList<>();
    if (!this.componentMap.containsKey(paramString))
      return arrayList1; 
    ArrayList arrayList = this.componentMap.get(paramString);
    if (arrayList == null)
      return arrayList1; 
    for (ComponentBean componentBean : (ArrayList<ComponentBean>)arrayList) {
      if (componentBean.type == paramInt)
        arrayList1.add(componentBean); 
    } 
    return arrayList1;
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
  
  public void addVariable(String paramString1, int paramInt, String paramString2) {
    Pair pair = new Pair(Integer.valueOf(paramInt), paramString2);
    if (!this.variableMap.containsKey(paramString1))
      this.variableMap.put(paramString1, new ArrayList<Pair<Integer, String>>()); 
    ((ArrayList)this.variableMap.get(paramString1)).add(pair);
  }
  
  public boolean hasLogicBackup() {
    String str2 = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str2);
    stringBuilder.append(File.separator);
    stringBuilder.append("logic");
    String str1 = stringBuilder.toString();
    return this.fileUtil.exists(str1);
  }
  
  public boolean isVariableUsedInBlocks(String paramString1, String paramString2, String paramString3) {
    Map map = this.blockMap.get(paramString1);
    if (map == null)
      return false; 
    for (Map.Entry<String, ArrayList<BlockBean>> entry : ((Map<String, ArrayList<BlockBean>>)map).entrySet()) {
      if (((String)entry.getKey()).equals(paramString3))
        continue; 
      for (BlockBean blockBean : (ArrayList<BlockBean>)entry.getValue()) {
        ClassInfo gx = blockBean.getClassInfo();
        if (gx != null && gx.isVariable() && blockBean.spec.equals(paramString2))
          return true; 
        ArrayList<ClassInfo> arrayList = blockBean.getParamClassInfo();
        if (arrayList != null && arrayList.size() > 0)
          for (int b = 0; b < arrayList.size(); b++) {
            ClassInfo gx1 = arrayList.get(b);
            if (gx1 != null && gx1.isVariable() && ((String)blockBean.parameters.get(b)).equals(paramString2))
              return true; 
          }  
      } 
    } 
    return false;
  }
  
  public ArrayList<ViewBean> getViews(String paramString) {
    ArrayList<ViewBean> arrayList2 = this.viewMap.get(paramString);
    ArrayList<ViewBean> arrayList1 = arrayList2;
    if (arrayList2 == null)
      arrayList1 = new ArrayList<>(); 
    return arrayList1;
  }
  
  public ArrayList<String> getListNamesByType(String paramString, int paramInt) {
    ArrayList<String> arrayList1 = new ArrayList<>();
    if (!this.listMap.containsKey(paramString))
      return arrayList1; 
    ArrayList arrayList = this.listMap.get(paramString);
    if (arrayList == null)
      return arrayList1; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((Integer)pair.first).intValue() == paramInt)
        arrayList1.add((String)pair.second); 
    } 
    return arrayList1;
  }
  
  public ArrayList<Pair<Integer, String>> getViewsByType(String paramString1, String paramString2) {
    ArrayList<Pair<Integer, String>> arrayList1 = new ArrayList<>();
    ArrayList arrayList = this.viewMap.get(paramString1);
    if (arrayList == null)
      return arrayList1; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)arrayList) {
      Pair<Integer, String> pair;
      if (paramString2.equals("CheckBox")) {
        if (viewBean.getClassInfo().matchesType("CompoundButton")) {
          pair = new Pair(Integer.valueOf(viewBean.type), viewBean.id);
        } else {
          continue;
        } 
      } else if (viewBean.getClassInfo().matchesType(paramString2)) {
        pair = new Pair(Integer.valueOf(viewBean.type), viewBean.id);
      } else {
        continue;
      } 
      arrayList1.add(pair);
    } 
    return arrayList1;
  }
  
  public void removeEvent(String paramString1, String paramString2, String paramString3) {
    if (!this.eventMap.containsKey(paramString1))
      return; 
    ArrayList<EventBean> arrayList = this.eventMap.get(paramString1);
    if (arrayList == null)
      return; 
    int i = arrayList.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        EventBean eventBean = arrayList.get(j);
        i = j;
        if (eventBean.targetId.equals(paramString2)) {
          i = j;
          if (paramString3.equals(eventBean.eventName)) {
            arrayList.remove(eventBean);
            HashMap<String, HashMap<String, ArrayList<BlockBean>>> hashMap = this.blockMap;
            i = j;
            if (hashMap != null) {
              i = j;
              if (hashMap.get(paramString1) != null) {
                HashMap hashMap1 = this.blockMap.get(paramString1);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(eventBean.targetId);
                stringBuilder.append("_");
                stringBuilder.append(eventBean.eventName);
                i = j;
                if (hashMap1.containsKey(stringBuilder.toString())) {
                  hashMap1 = this.blockMap.get(paramString1);
                  stringBuilder = new StringBuilder();
                  stringBuilder.append(eventBean.targetId);
                  stringBuilder.append("_");
                  stringBuilder.append(eventBean.eventName);
                  hashMap1.remove(stringBuilder.toString());
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
    String str2 = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str2);
    stringBuilder.append(File.separator);
    stringBuilder.append("view");
    String str1 = stringBuilder.toString();
    return this.fileUtil.exists(str1);
  }
  
  public boolean hasComponent(String paramString1, int paramInt, String paramString2) {
    ArrayList arrayList = this.componentMap.get(paramString1);
    if (arrayList == null)
      return false; 
    for (ComponentBean componentBean : (ArrayList<ComponentBean>)arrayList) {
      if (componentBean.type == paramInt && componentBean.componentId.equals(paramString2))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<ComponentBean> getComponents(String paramString) {
    return !this.componentMap.containsKey(paramString) ? new ArrayList<ComponentBean>() : this.componentMap.get(paramString);
  }
  
  public ArrayList<String> getVariableNamesByType(String paramString, int paramInt) {
    ArrayList<String> arrayList1 = new ArrayList<>();
    if (!this.variableMap.containsKey(paramString))
      return arrayList1; 
    ArrayList arrayList = this.variableMap.get(paramString);
    if (arrayList == null)
      return arrayList1; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((Integer)pair.first).intValue() == paramInt)
        arrayList1.add((String)pair.second); 
    } 
    return arrayList1;
  }
  
  public void loadLogicFromData() {
    String str2 = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str2);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("logic");
    str2 = stringBuilder1.toString();
    if (!this.fileUtil.exists(str2))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.fileUtil.readFileBytes(str2);
      String str = this.fileUtil.decryptToString(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      readLogicData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public boolean hasListVariable(String paramString1, int paramInt, String paramString2) {
    ArrayList arrayList = this.listMap.get(paramString1);
    if (arrayList == null)
      return false; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((Integer)pair.first).intValue() == paramInt && ((String)pair.second).equals(paramString2))
        return true; 
    } 
    return false;
  }
  
  public boolean hasCompoundButtonView(String paramString1, String paramString2) {
    ArrayList arrayList = this.viewMap.get(paramString1);
    if (arrayList == null)
      return false; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)arrayList) {
      if (viewBean.getClassInfo().matchesType("CompoundButton") && viewBean.id.equals(paramString2))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<ViewBean> getCustomViewBeans(String paramString) {
    ArrayList<ViewBean> arrayList1 = new ArrayList<>();
    ArrayList arrayList = this.viewMap.get(paramString);
    if (arrayList == null)
      return arrayList1; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)arrayList) {
      if (viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) {
        String str = viewBean.customView;
        if (str != null && str.length() > 0 && !viewBean.customView.equals("none"))
          arrayList1.add(viewBean); 
      } 
    } 
    return arrayList1;
  }
  
  public void loadLogicFromBackup() {
    String str1 = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str1);
    stringBuilder.append(File.separator);
    stringBuilder.append("logic");
    String str2 = stringBuilder.toString();
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.fileUtil.readFileBytes(str2);
      String str = this.fileUtil.decryptToString(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      readLogicData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public boolean hasComponentOfType(String paramString, int paramInt) {
    ArrayList arrayList = this.componentMap.get(paramString);
    if (arrayList == null)
      return false; 
    Iterator iterator = arrayList.iterator();
    while (iterator.hasNext()) {
      if (((ComponentBean)iterator.next()).type == paramInt)
        return true; 
    } 
    return false;
  }
  
  public boolean hasVariable(String paramString1, int paramInt, String paramString2) {
    ArrayList arrayList = this.variableMap.get(paramString1);
    if (arrayList == null)
      return false; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((Integer)pair.first).intValue() == paramInt && ((String)pair.second).equals(paramString2))
        return true; 
    } 
    return false;
  }
  
  public boolean isMoreBlockUsed(String paramString1, String paramString2) {
    Map map = this.blockMap.get(paramString1);
    if (map == null)
      return false; 
    for (Map.Entry<String, ArrayList<BlockBean>> entry : ((Map<String, ArrayList<BlockBean>>)map).entrySet()) {
      String str = (String)entry.getKey();
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(paramString2);
      stringBuilder.append("_");
      stringBuilder.append("moreBlock");
      if (str.equals(stringBuilder.toString()))
        continue; 
      for (BlockBean blockBean : (ArrayList<BlockBean>)entry.getValue()) {
        if (blockBean.opCode.equals("definedFunc")) {
          int i = blockBean.spec.indexOf(" ");
          str = blockBean.spec;
          String str1 = str;
          if (i > 0)
            str1 = str.substring(0, i); 
          if (str1.equals(paramString2))
            return true; 
        } 
      } 
    } 
    return false;
  }
  
  public ArrayList<EventBean> getEvents(String paramString) {
    return !this.eventMap.containsKey(paramString) ? new ArrayList<EventBean>() : this.eventMap.get(paramString);
  }
  
  public void loadViewFromData() {
    String str2 = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str2);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("view");
    str2 = stringBuilder1.toString();
    if (!this.fileUtil.exists(str2))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.fileUtil.readFileBytes(str2);
      String str = this.fileUtil.decryptToString(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      readViewData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void removeComponentsByType(String paramString, int paramInt) {
    if (!this.componentMap.containsKey(paramString))
      return; 
    ArrayList<ComponentBean> arrayList = getComponentsByType(paramString, paramInt);
    if (arrayList != null && arrayList.size() > 0) {
      Iterator<ComponentBean> iterator = arrayList.iterator();
      while (iterator.hasNext())
        removeComponent(paramString, iterator.next()); 
    } 
  }
  
  public boolean hasViewOfType(String paramString1, int paramInt, String paramString2) {
    ArrayList arrayList = this.viewMap.get(paramString1);
    if (arrayList == null)
      return false; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)arrayList) {
      if (viewBean.type == paramInt && viewBean.id.equals(paramString2))
        return true; 
    } 
    return false;
  }
  
  public boolean hasTextView(String paramString1, String paramString2) {
    ArrayList arrayList = this.viewMap.get(paramString1);
    if (arrayList == null)
      return false; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)arrayList) {
      if (viewBean.getClassInfo().matchesType("TextView") && viewBean.id.equals(paramString2))
        return true; 
    } 
    return false;
  }
  
  public ViewBean getFabView(String paramString) {
    if (!this.fabMap.containsKey(paramString))
      initFab(paramString); 
    return this.fabMap.get(paramString);
  }
  
  public void loadViewFromBackup() {
    String str2 = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str2);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("view");
    str2 = stringBuilder1.toString();
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.fileUtil.readFileBytes(str2);
      String str = this.fileUtil.decryptToString(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      readViewData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public boolean hasView(String paramString1, String paramString2) {
    ArrayList arrayList = this.viewMap.get(paramString1);
    if (arrayList == null)
      return false; 
    Iterator iterator = arrayList.iterator();
    while (iterator.hasNext()) {
      if (((ViewBean)iterator.next()).id.equals(paramString2))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<Pair<String, String>> getMoreBlocks(String paramString) {
    return this.moreBlockMap.containsKey(paramString) ? this.moreBlockMap.get(paramString) : new ArrayList<Pair<String, String>>();
  }
  
  public void resetProject() {
    this.projectId = "";
    clearAllData();
  }
  
  public void parseLogicSection(String paramString1, String paramString2) {
    if (paramString2.length() <= 0)
      return; 
    try {
      ProjectDataParser ProjectDataParser = new ProjectDataParser(paramString1);
      String str = ProjectDataParser.getFileName();
      ProjectDataParser.DataType dataType = ProjectDataParser.getDataType();
      switch (ScreenOrientationConstants.a[dataType.ordinal()]) {
        default:
          return;
        case 8:
          if (!this.blockMap.containsKey(str)) {
            this.blockMap.put(str, new HashMap<String, ArrayList<BlockBean>>());
          } 
          this.blockMap.get(str).put(ProjectDataParser.getEventKey(), (ArrayList<BlockBean>)ProjectDataParser.parseData(paramString2));
          break;
        case 7:
          this.moreBlockMap.put(str, (ArrayList<Pair<String, String>>)ProjectDataParser.parseData(paramString2));
          break;
        case 6:
          this.eventMap.put(str, (ArrayList<EventBean>)ProjectDataParser.parseData(paramString2));
          break;
        case 5:
          this.componentMap.put(str, (ArrayList<ComponentBean>)ProjectDataParser.parseData(paramString2));
          break;
        case 4:
          this.listMap.put(str, (ArrayList<Pair<Integer, String>>)ProjectDataParser.parseData(paramString2));
          break;
        case 3:
          this.variableMap.put(str, (ArrayList<Pair<Integer, String>>)ProjectDataParser.parseData(paramString2));
          break;
      } 
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public ArrayList<Pair<Integer, String>> getListVariables(String paramString) {
    return this.listMap.containsKey(paramString) ? this.listMap.get(paramString) : new ArrayList<Pair<Integer, String>>();
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
  
  public void parseViewSection(String paramString1, String paramString2) {
    try {
      ProjectDataParser ProjectDataParser = new ProjectDataParser(paramString1);
      String str = ProjectDataParser.getFileName();
      ProjectDataParser.DataType dataType = ProjectDataParser.getDataType();
      int i = ScreenOrientationConstants.a[dataType.ordinal()];
      if (i == 1) {
        this.viewMap.put(str, (ArrayList<ViewBean>)ProjectDataParser.parseData(paramString2));
      } else if (i == 2) {
        this.fabMap.put(str, (ViewBean)ProjectDataParser.parseData(paramString2));
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public ArrayList<Pair<Integer, String>> getVariables(String paramString) {
    return this.variableMap.containsKey(paramString) ? this.variableMap.get(paramString) : new ArrayList<Pair<Integer, String>>();
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
  
  public void removeBlockEntry(String paramString1, String paramString2) {
    if (!this.blockMap.containsKey(paramString1))
      return; 
    Map map = this.blockMap.get(paramString1);
    if (map == null)
      return; 
    if (map.containsKey(paramString2))
      map.remove(paramString2); 
  }
  
  public void removeViewTypeEvents(String paramString) {
    if (!this.eventMap.containsKey(paramString))
      return; 
    ArrayList arrayList = this.eventMap.get(paramString);
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
  
  public void removeViewEventsByTarget(String paramString1, String paramString2) {
    if (!this.eventMap.containsKey(paramString1))
      return; 
    ArrayList<EventBean> arrayList = this.eventMap.get(paramString1);
    int i = arrayList.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        EventBean eventBean = arrayList.get(j);
        i = j;
        if (eventBean.eventType == 4) {
          i = j;
          if (eventBean.targetId.equals(paramString2)) {
            arrayList.remove(j);
            i = j;
          } 
        } 
        continue;
      } 
      break;
    } 
  }
  
  public final void saveLogicFile(String paramString) {
    StringBuffer stringBuffer = new StringBuffer();
    serializeLogicData(stringBuffer);
    try {
      byte[] arrayOfByte = this.fileUtil.encryptString(stringBuffer.toString());
      this.fileUtil.writeBytes(paramString, arrayOfByte);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void removeEventsByTarget(String paramString1, String paramString2) {
    if (!this.eventMap.containsKey(paramString1))
      return; 
    ArrayList<EventBean> arrayList = this.eventMap.get(paramString1);
    if (arrayList == null)
      return; 
    int i = arrayList.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        EventBean eventBean = arrayList.get(j);
        i = j;
        if (eventBean.targetId.equals(paramString2)) {
          arrayList.remove(eventBean);
          HashMap<String, HashMap<String, ArrayList<BlockBean>>> hashMap = this.blockMap;
          i = j;
          if (hashMap != null) {
            i = j;
            if (hashMap.get(paramString1) != null) {
              HashMap hashMap1 = this.blockMap.get(paramString1);
              StringBuilder stringBuilder = new StringBuilder();
              stringBuilder.append(eventBean.targetId);
              stringBuilder.append("_");
              stringBuilder.append(eventBean.eventName);
              i = j;
              if (hashMap1.containsKey(stringBuilder.toString())) {
                hashMap1 = this.blockMap.get(paramString1);
                stringBuilder = new StringBuilder();
                stringBuilder.append(eventBean.targetId);
                stringBuilder.append("_");
                stringBuilder.append(eventBean.eventName);
                hashMap1.remove(stringBuilder.toString());
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
  
  public final void saveViewFile(String paramString) {
    StringBuffer stringBuffer = new StringBuffer();
    serializeViewData(stringBuffer);
    try {
      byte[] arrayOfByte = this.fileUtil.encryptString(stringBuffer.toString());
      this.fileUtil.writeBytes(paramString, arrayOfByte);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void removeMoreBlock(String paramString1, String paramString2) {
    if (!this.moreBlockMap.containsKey(paramString1))
      return; 
    ArrayList arrayList = this.moreBlockMap.get(paramString1);
    if (arrayList == null)
      return; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((String)pair.first).equals(paramString2)) {
        arrayList.remove(pair);
        break;
      } 
    } 
    HashMap hashMap = this.blockMap.get(paramString1);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(paramString2);
    stringBuilder.append("_");
    stringBuilder.append("moreBlock");
    if (hashMap.containsKey(stringBuilder.toString())) {
      hashMap = this.blockMap.get(paramString1);
      StringBuilder stringBuilder1 = new StringBuilder();
      stringBuilder1.append(paramString2);
      stringBuilder1.append("_");
      stringBuilder1.append("moreBlock");
      hashMap.remove(stringBuilder1.toString());
    } 
  }
  
  public void removeListVariable(String paramString1, String paramString2) {
    if (!this.listMap.containsKey(paramString1))
      return; 
    ArrayList arrayList = this.listMap.get(paramString1);
    if (arrayList == null)
      return; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((String)pair.second).equals(paramString2)) {
        arrayList.remove(pair);
        break;
      } 
    } 
  }
  
  public void removeVariable(String paramString1, String paramString2) {
    if (!this.variableMap.containsKey(paramString1))
      return; 
    ArrayList arrayList = this.variableMap.get(paramString1);
    if (arrayList == null)
      return; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((String)pair.second).equals(paramString2)) {
        arrayList.remove(pair);
        break;
      } 
    } 
  }
  
  public boolean hasViewType(String paramString, int paramInt) {
    ArrayList arrayList = this.viewMap.get(paramString);
    boolean bool = false;
    if (arrayList != null) {
      Iterator iterator = arrayList.iterator();
      while (iterator.hasNext()) {
        if (((ViewBean)iterator.next()).type == paramInt) {
          bool = true;
          break;
        } 
      } 
    } 
    return bool;
  }
  
  public boolean hasViewMatchingType(String paramString1, String paramString2) {
    ArrayList arrayList = this.viewMap.get(paramString1);
    boolean bool = false;
    if (arrayList != null) {
      Iterator<ViewBean> iterator = arrayList.iterator();
      while (iterator.hasNext()) {
        if (((ViewBean)iterator.next()).getClassInfo().matchesType(paramString2)) {
          bool = true;
          break;
        } 
      } 
    } 
    return bool;
  }
  
  public final String getSimpleClassName(String paramString) {
    String str = paramString;
    if (paramString.contains(".")) {
      String[] arrayOfString = paramString.split("\\.");
      str = arrayOfString[arrayOfString.length - 1];
    } 
    return str;
  }
}
