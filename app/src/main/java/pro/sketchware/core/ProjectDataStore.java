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
    b();
    this.projectId = paramString;
    this.fileUtil = new EncryptedFileUtil();
    this.gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
    this.buildConfig = new BuildConfig();
  }
  
  public static ArrayList<ViewBean> a(ArrayList<ViewBean> paramArrayList) {
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
        arrayList.addAll(a(paramArrayList, viewBean)); 
    } 
    return arrayList;
  }
  
  public static ArrayList<ViewBean> a(ArrayList<ViewBean> paramArrayList, ViewBean paramViewBean) {
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
          arrayList.addAll(a(paramArrayList, viewBean)); 
      } 
    } 
    return arrayList;
  }
  
  public ComponentBean a(String paramString, int paramInt) {
    return !this.componentMap.containsKey(paramString) ? null : ((ArrayList<ComponentBean>)this.componentMap.get(paramString)).get(paramInt);
  }
  
  public ArrayList<String> a(ProjectFileBean paramProjectFileBean) {
    String str1 = paramProjectFileBean.getXmlName();
    String str2 = paramProjectFileBean.getJavaName();
    ArrayList<Object> arrayList = new ArrayList<>();
    Iterator<Pair<Integer, String>> iterator1 = k(str2).iterator();
    while (iterator1.hasNext())
      arrayList.add(((Pair)iterator1.next()).second); 
    iterator1 = j(str2).iterator();
    while (iterator1.hasNext())
      arrayList.add(((Pair)iterator1.next()).second); 
    iterator1 = (Iterator)i(str2).iterator();
    while (iterator1.hasNext())
      arrayList.add(((Pair)iterator1.next()).first); 
    Iterator<ViewBean> iterator = d(str1).iterator();
    while (iterator.hasNext())
      arrayList.add(((ViewBean)iterator.next()).id); 
    Iterator iteratorComp = e(str2).iterator();
    while (iteratorComp.hasNext())
      arrayList.add(((ComponentBean)iteratorComp.next()).componentId); 
    return (ArrayList)arrayList;
  }
  
  public ArrayList<EventBean> a(String paramString, ComponentBean paramComponentBean) {
    if (!this.eventMap.containsKey(paramString))
      return new ArrayList<EventBean>(); 
    ArrayList<EventBean> arrayList = new ArrayList<>();
    for (EventBean eventBean : this.eventMap.get(paramString)) {
      if (eventBean.targetType == paramComponentBean.type && eventBean.targetId.equals(paramComponentBean.componentId))
        arrayList.add(eventBean); 
    } 
    return arrayList;
  }
  
  public ArrayList<BlockBean> a(String paramString1, String paramString2) {
    if (!this.blockMap.containsKey(paramString1))
      return new ArrayList<BlockBean>(); 
    Map map = this.blockMap.get(paramString1);
    return (map == null) ? new ArrayList<BlockBean>() : (!map.containsKey(paramString2) ? new ArrayList<BlockBean>() : (ArrayList<BlockBean>)map.get(paramString2));
  }
  
  public void a() {
    String str1 = SketchwarePaths.a(this.projectId);
    StringBuilder stringBuilder2 = new StringBuilder();
    stringBuilder2.append(str1);
    stringBuilder2.append(File.separator);
    stringBuilder2.append("view");
    String str2 = stringBuilder2.toString();
    this.fileUtil.c(str2);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str1);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("logic");
    str1 = stringBuilder1.toString();
    this.fileUtil.c(str1);
  }
  
  public void a(ProjectFileManager paramhC) {
    for (ProjectFileBean projectFileBean : paramhC.b()) {
      if (!projectFileBean.hasActivityOption(8))
        b(projectFileBean); 
      if (!projectFileBean.hasActivityOption(4))
        l(projectFileBean.getJavaName()); 
    } 
    ArrayList<String> arrayList2 = new ArrayList<>();
    for (Map.Entry<String, ArrayList<ViewBean>> entry : this.viewMap.entrySet()) {
      String str = (String)entry.getKey();
      if (!paramhC.d(str)) {
        arrayList2.add(str);
        continue;
      } 
      for (ViewBean viewBean : entry.getValue()) {
        if (viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) {
          String str1 = viewBean.customView;
          if (str1 != null && str1.length() > 0 && !viewBean.customView.equals("none")) {
            Iterator iterator = paramhC.c().iterator();
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
      if (!paramhC.c(str))
        arrayList6.add(str); 
    } 
    for (String str : arrayList6)
      this.variableMap.remove(str); 
    ArrayList<String> arrayList4 = new ArrayList<>();
    Iterator iterator3 = this.listMap.entrySet().iterator();
    while (iterator3.hasNext()) {
      String str = (String)((Map.Entry)iterator3.next()).getKey();
      if (!paramhC.c(str))
        arrayList4.add(str); 
    } 
    for (String str : arrayList4)
      this.listMap.remove(str); 
    arrayList4 = new ArrayList<String>();
    Iterator iterator1 = this.moreBlockMap.entrySet().iterator();
    while (iterator1.hasNext()) {
      String str = (String)((Map.Entry)iterator1.next()).getKey();
      if (!paramhC.c(str))
        arrayList4.add(str); 
    } 
    for (String str : arrayList4)
      this.moreBlockMap.remove(str); 
    ArrayList<String> arrayList1 = new ArrayList<>();
    Iterator<Map.Entry<String, ArrayList<ComponentBean>>> iterator5 = this.componentMap.entrySet().iterator();
    while (iterator5.hasNext()) {
      String str = (String)((Map.Entry)iterator5.next()).getKey();
      if (!paramhC.c(str))
        arrayList1.add(str); 
    } 
    for (String str : arrayList1)
      this.componentMap.remove(str); 
    ArrayList<String> arrayList5 = new ArrayList<>();
    Iterator iterator2 = this.eventMap.entrySet().iterator();
    while (iterator2.hasNext()) {
      String str = (String)((Map.Entry)iterator2.next()).getKey();
      if (!paramhC.c(str))
        arrayList5.add(str); 
    } 
    for (String str : arrayList5)
      this.eventMap.remove(str); 
    ArrayList<String> arrayList3 = new ArrayList<>();
    for (Map.Entry<String, HashMap<String, ArrayList<BlockBean>>> entry : this.blockMap.entrySet()) {
      String str = (String)entry.getKey();
      if (!paramhC.c(str)) {
        arrayList3.add(str);
        continue;
      } 
      Iterator iterator = ((HashMap)entry.getValue()).entrySet().iterator();
      while (iterator.hasNext()) {
        label128: for (BlockBean blockBean : (ArrayList<BlockBean>)((Map.Entry)iterator.next()).getValue()) {
          if (blockBean.opCode.equals("intentSetScreen")) {
            Iterator<ProjectFileBean> iterator6 = paramhC.b().iterator();
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
  
  public void a(ResourceManager paramkC) {
    ArrayList arrayList = paramkC.k();
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
  
  public void a(ProjectFileBean paramProjectFileBean, ViewBean paramViewBean) {
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
      m(paramProjectFileBean.getJavaName(), paramViewBean.id);
      a(paramProjectFileBean.getJavaName(), paramViewBean.getClassInfo(), paramViewBean.id, true);
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
                if (gx != null && gx.b(paramViewBean.getClassInfo().a()) && blockBean.spec.equals(paramViewBean.id)) {
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
                        if (gx1 != null && paramViewBean.getClassInfo().a(gx1) && ((String)blockBean.parameters.get(b)).equals(paramViewBean.id))
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
      l(paramProjectFileBean.getDrawersJavaName(), paramViewBean.id);
    } 
  }
  
  public void a(ProjectLibraryBean paramProjectLibraryBean) {
    if (paramProjectLibraryBean.useYn.equals("Y"))
      return; 
    Iterator iterator = this.componentMap.entrySet().iterator();
    while (iterator.hasNext()) {
      String str = (String)((Map.Entry)iterator.next()).getKey();
      g(str, 6);
      g(str, 12);
      g(str, 14);
    } 
  }
  
  public void a(ProjectLibraryBean paramProjectLibraryBean, ProjectFileManager paramhC) {
    if (paramProjectLibraryBean.useYn.equals("Y"))
      return; 
    for (ProjectFileBean projectFileBean : paramhC.b()) {
      for (ViewBean viewBean : d(projectFileBean.getXmlName())) {
        if (viewBean.type == 17)
          a(projectFileBean, viewBean); 
      } 
    } 
    for (ProjectFileBean projectFileBean : paramhC.c()) {
      for (ViewBean viewBean : d(projectFileBean.getXmlName())) {
        if (viewBean.type == 17)
          a(projectFileBean, viewBean); 
      } 
    } 
    Iterator iterator = this.componentMap.entrySet().iterator();
    while (iterator.hasNext())
      g((String)((Map.Entry)iterator.next()).getKey(), 13); 
  }
  
  public void a(BufferedReader paramBufferedReader) throws java.io.IOException {
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
            i(str, stringBuffer.toString());
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
        i(str, stringBuffer.toString()); 
      return;
    } 
  }
  
  public void a(String paramString) {
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
  
  public void a(String paramString1, int paramInt1, int paramInt2, String paramString2, String paramString3) {
    if (!this.eventMap.containsKey(paramString1))
      this.eventMap.put(paramString1, new ArrayList<EventBean>()); 
    ((ArrayList<EventBean>)this.eventMap.get(paramString1)).add(new EventBean(paramInt1, paramInt2, paramString2, paramString3));
  }
  
  public void a(String paramString1, int paramInt, String paramString2) {
    if (!this.componentMap.containsKey(paramString1))
      this.componentMap.put(paramString1, new ArrayList<ComponentBean>()); 
    ((ArrayList<ComponentBean>)this.componentMap.get(paramString1)).add(new ComponentBean(paramInt, paramString2));
  }
  
  public void a(String paramString1, int paramInt, String paramString2, String paramString3) {
    if (!this.componentMap.containsKey(paramString1))
      this.componentMap.put(paramString1, new ArrayList<ComponentBean>()); 
    ((ArrayList<ComponentBean>)this.componentMap.get(paramString1)).add(new ComponentBean(paramInt, paramString2, paramString3));
  }
  
  public void a(String paramString1, ClassInfo paramGx, String paramString2, boolean paramBoolean) {
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
          if (gx != null && gx.b(paramGx.a()) && blockBean.spec.equals(paramString2)) {
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
                  if (gx1 != null && paramGx.a(gx1) && ((String)blockBean.parameters.get(b)).equals(paramString2))
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
  
  public void a(String paramString, EventBean paramEventBean) {
    if (!this.eventMap.containsKey(paramString))
      this.eventMap.put(paramString, new ArrayList<EventBean>()); 
    ((ArrayList<EventBean>)this.eventMap.get(paramString)).add(paramEventBean);
  }
  
  public void a(String paramString, ViewBean paramViewBean) {
    if (!this.viewMap.containsKey(paramString))
      this.viewMap.put(paramString, new ArrayList<ViewBean>()); 
    ((ArrayList<ViewBean>)this.viewMap.get(paramString)).add(paramViewBean);
  }
  
  public void a(String paramString1, String paramString2, String paramString3) {
    Pair pair = new Pair(paramString2, paramString3);
    if (!this.moreBlockMap.containsKey(paramString1))
      this.moreBlockMap.put(paramString1, new ArrayList<Pair<String, String>>()); 
    ((ArrayList)this.moreBlockMap.get(paramString1)).add(pair);
  }
  
  public void a(String paramString1, String paramString2, ArrayList<BlockBean> paramArrayList) {
    if (!this.blockMap.containsKey(paramString1))
      this.blockMap.put(paramString1, new HashMap<String, ArrayList<BlockBean>>()); 
    ((Map<String, ArrayList<BlockBean>>)this.blockMap.get(paramString1)).put(paramString2, paramArrayList);
  }
  
  public final void a(StringBuffer paramStringBuffer) {
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
  
  public String b(String paramString1, String paramString2) {
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
  
  public ArrayList<String> b(String paramString, int paramInt) {
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
  
  public ArrayList<ViewBean> b(String paramString, ViewBean paramViewBean) {
    ArrayList<ViewBean> arrayList = new ArrayList<>();
    arrayList.add(paramViewBean);
    arrayList.addAll(a(this.viewMap.get(paramString), paramViewBean));
    return arrayList;
  }
  
  public HashMap<String, ArrayList<BlockBean>> b(String paramString) {
    return !this.blockMap.containsKey(paramString) ? new HashMap<String, ArrayList<BlockBean>>() : this.blockMap.get(paramString);
  }
  
  public void b() {
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
  
  public void b(ResourceManager paramkC) {
    ArrayList arrayList = paramkC.m();
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
  
  public void b(ProjectFileBean paramProjectFileBean) {
    if (this.fabMap.containsKey(paramProjectFileBean.getXmlName()))
      this.fabMap.remove(paramProjectFileBean.getXmlName()); 
    m(paramProjectFileBean.getJavaName(), "_fab");
  }
  
  public void b(ProjectLibraryBean paramProjectLibraryBean, ProjectFileManager paramhC) {
    if (paramProjectLibraryBean.useYn.equals("Y"))
      return; 
    for (ProjectFileBean projectFileBean : paramhC.b()) {
      for (ViewBean viewBean : d(projectFileBean.getXmlName())) {
        if (viewBean.type == 18)
          a(projectFileBean, viewBean); 
      } 
    } 
  }
  
  public void b(BufferedReader paramBufferedReader) throws java.io.IOException {
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
            j(str, stringBuffer.toString());
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
        j(str, stringBuffer.toString()); 
      return;
    } 
  }
  
  public void b(String paramString1, int paramInt, String paramString2) {
    Pair pair = new Pair(Integer.valueOf(paramInt), paramString2);
    if (!this.listMap.containsKey(paramString1))
      this.listMap.put(paramString1, new ArrayList<Pair<Integer, String>>()); 
    ((ArrayList)this.listMap.get(paramString1)).add(pair);
  }
  
  public void b(String paramString, ComponentBean paramComponentBean) {
    if (!this.componentMap.containsKey(paramString))
      return; 
    ArrayList arrayList = this.componentMap.get(paramString);
    if (arrayList.indexOf(paramComponentBean) < 0)
      return; 
    arrayList.remove(paramComponentBean);
    m(paramString, paramComponentBean.componentId);
    a(paramString, paramComponentBean.getClassInfo(), paramComponentBean.componentId, false);
    this.buildConfig.constVarComponent.handleDeleteComponent(paramComponentBean.componentId);
  }
  
  public final void b(StringBuffer paramStringBuffer) {
    HashMap<String, ArrayList<ViewBean>> hashMap1 = this.viewMap;
    if (hashMap1 != null && hashMap1.size() > 0)
      for (Map.Entry<String, ArrayList<ViewBean>> entry : this.viewMap.entrySet()) {
        String str;
        ArrayList arrayList = (ArrayList)entry.getValue();
        if (arrayList == null || arrayList.size() <= 0)
          continue; 
        ArrayList<ViewBean> arrayList1 = a((ArrayList<ViewBean>)entry.getValue());
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
  
  public boolean b(String paramString1, String paramString2, String paramString3) {
    Map map = this.blockMap.get(paramString1);
    if (map == null)
      return false; 
    for (Map.Entry<String, ArrayList<BlockBean>> entry : ((Map<String, ArrayList<BlockBean>>)map).entrySet()) {
      if (((String)entry.getKey()).equals(paramString3))
        continue; 
      for (BlockBean blockBean : (ArrayList<BlockBean>)entry.getValue()) {
        ClassInfo gx = blockBean.getClassInfo();
        if (gx != null && gx.b() && blockBean.spec.equals(paramString2))
          return true; 
        ArrayList<ClassInfo> arrayList = blockBean.getParamClassInfo();
        if (arrayList != null && arrayList.size() > 0)
          for (int b = 0; b < arrayList.size(); b++) {
            ClassInfo gx1 = arrayList.get(b);
            if (gx1 != null && gx1.b() && ((String)blockBean.parameters.get(b)).equals(paramString2))
              return true; 
          }  
      } 
    } 
    return false;
  }
  
  public ViewBean c(String paramString1, String paramString2) {
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
  
  public ArrayList<String> c(String paramString) {
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
  
  public ArrayList<ComponentBean> c(String paramString, int paramInt) {
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
  
  public void c(ResourceManager paramkC) {
    ArrayList arrayList = paramkC.p();
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
  
  public void c(String paramString1, int paramInt, String paramString2) {
    Pair pair = new Pair(Integer.valueOf(paramInt), paramString2);
    if (!this.variableMap.containsKey(paramString1))
      this.variableMap.put(paramString1, new ArrayList<Pair<Integer, String>>()); 
    ((ArrayList)this.variableMap.get(paramString1)).add(pair);
  }
  
  public boolean c() {
    String str2 = SketchwarePaths.a(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str2);
    stringBuilder.append(File.separator);
    stringBuilder.append("logic");
    String str1 = stringBuilder.toString();
    return this.fileUtil.e(str1);
  }
  
  public boolean c(String paramString1, String paramString2, String paramString3) {
    Map map = this.blockMap.get(paramString1);
    if (map == null)
      return false; 
    for (Map.Entry<String, ArrayList<BlockBean>> entry : ((Map<String, ArrayList<BlockBean>>)map).entrySet()) {
      if (((String)entry.getKey()).equals(paramString3))
        continue; 
      for (BlockBean blockBean : (ArrayList<BlockBean>)entry.getValue()) {
        ClassInfo gx = blockBean.getClassInfo();
        if (gx != null && gx.c() && blockBean.spec.equals(paramString2))
          return true; 
        ArrayList<ClassInfo> arrayList = blockBean.getParamClassInfo();
        if (arrayList != null && arrayList.size() > 0)
          for (int b = 0; b < arrayList.size(); b++) {
            ClassInfo gx1 = arrayList.get(b);
            if (gx1 != null && gx1.c() && ((String)blockBean.parameters.get(b)).equals(paramString2))
              return true; 
          }  
      } 
    } 
    return false;
  }
  
  public ArrayList<ViewBean> d(String paramString) {
    ArrayList<ViewBean> arrayList2 = this.viewMap.get(paramString);
    ArrayList<ViewBean> arrayList1 = arrayList2;
    if (arrayList2 == null)
      arrayList1 = new ArrayList<>(); 
    return arrayList1;
  }
  
  public ArrayList<String> d(String paramString, int paramInt) {
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
  
  public ArrayList<Pair<Integer, String>> d(String paramString1, String paramString2) {
    ArrayList<Pair<Integer, String>> arrayList1 = new ArrayList<>();
    ArrayList arrayList = this.viewMap.get(paramString1);
    if (arrayList == null)
      return arrayList1; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)arrayList) {
      Pair<Integer, String> pair;
      if (paramString2.equals("CheckBox")) {
        if (viewBean.getClassInfo().a("CompoundButton")) {
          pair = new Pair(Integer.valueOf(viewBean.type), viewBean.id);
        } else {
          continue;
        } 
      } else if (viewBean.getClassInfo().a(paramString2)) {
        pair = new Pair(Integer.valueOf(viewBean.type), viewBean.id);
      } else {
        continue;
      } 
      arrayList1.add(pair);
    } 
    return arrayList1;
  }
  
  public void d(String paramString1, String paramString2, String paramString3) {
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
  
  public boolean d() {
    String str2 = SketchwarePaths.a(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str2);
    stringBuilder.append(File.separator);
    stringBuilder.append("view");
    String str1 = stringBuilder.toString();
    return this.fileUtil.e(str1);
  }
  
  public boolean d(String paramString1, int paramInt, String paramString2) {
    ArrayList arrayList = this.componentMap.get(paramString1);
    if (arrayList == null)
      return false; 
    for (ComponentBean componentBean : (ArrayList<ComponentBean>)arrayList) {
      if (componentBean.type == paramInt && componentBean.componentId.equals(paramString2))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<ComponentBean> e(String paramString) {
    return !this.componentMap.containsKey(paramString) ? new ArrayList<ComponentBean>() : this.componentMap.get(paramString);
  }
  
  public ArrayList<String> e(String paramString, int paramInt) {
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
  
  public void e() {
    String str2 = SketchwarePaths.b(this.projectId);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str2);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("logic");
    str2 = stringBuilder1.toString();
    if (!this.fileUtil.e(str2))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.fileUtil.h(str2);
      String str = this.fileUtil.a(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      a(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public boolean e(String paramString1, int paramInt, String paramString2) {
    ArrayList arrayList = this.listMap.get(paramString1);
    if (arrayList == null)
      return false; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((Integer)pair.first).intValue() == paramInt && ((String)pair.second).equals(paramString2))
        return true; 
    } 
    return false;
  }
  
  public boolean e(String paramString1, String paramString2) {
    ArrayList arrayList = this.viewMap.get(paramString1);
    if (arrayList == null)
      return false; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)arrayList) {
      if (viewBean.getClassInfo().a("CompoundButton") && viewBean.id.equals(paramString2))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<ViewBean> f(String paramString) {
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
  
  public void f() {
    String str1 = SketchwarePaths.a(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str1);
    stringBuilder.append(File.separator);
    stringBuilder.append("logic");
    String str2 = stringBuilder.toString();
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.fileUtil.h(str2);
      String str = this.fileUtil.a(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      a(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public boolean f(String paramString, int paramInt) {
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
  
  public boolean f(String paramString1, int paramInt, String paramString2) {
    ArrayList arrayList = this.variableMap.get(paramString1);
    if (arrayList == null)
      return false; 
    for (Pair pair : (ArrayList<Pair>)arrayList) {
      if (((Integer)pair.first).intValue() == paramInt && ((String)pair.second).equals(paramString2))
        return true; 
    } 
    return false;
  }
  
  public boolean f(String paramString1, String paramString2) {
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
  
  public ArrayList<EventBean> g(String paramString) {
    return !this.eventMap.containsKey(paramString) ? new ArrayList<EventBean>() : this.eventMap.get(paramString);
  }
  
  public void g() {
    String str2 = SketchwarePaths.b(this.projectId);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str2);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("view");
    str2 = stringBuilder1.toString();
    if (!this.fileUtil.e(str2))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.fileUtil.h(str2);
      String str = this.fileUtil.a(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      b(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void g(String paramString, int paramInt) {
    if (!this.componentMap.containsKey(paramString))
      return; 
    ArrayList<ComponentBean> arrayList = c(paramString, paramInt);
    if (arrayList != null && arrayList.size() > 0) {
      Iterator<ComponentBean> iterator = arrayList.iterator();
      while (iterator.hasNext())
        b(paramString, iterator.next()); 
    } 
  }
  
  public boolean g(String paramString1, int paramInt, String paramString2) {
    ArrayList arrayList = this.viewMap.get(paramString1);
    if (arrayList == null)
      return false; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)arrayList) {
      if (viewBean.type == paramInt && viewBean.id.equals(paramString2))
        return true; 
    } 
    return false;
  }
  
  public boolean g(String paramString1, String paramString2) {
    ArrayList arrayList = this.viewMap.get(paramString1);
    if (arrayList == null)
      return false; 
    for (ViewBean viewBean : (ArrayList<ViewBean>)arrayList) {
      if (viewBean.getClassInfo().a("TextView") && viewBean.id.equals(paramString2))
        return true; 
    } 
    return false;
  }
  
  public ViewBean h(String paramString) {
    if (!this.fabMap.containsKey(paramString))
      a(paramString); 
    return this.fabMap.get(paramString);
  }
  
  public void h() {
    String str2 = SketchwarePaths.a(this.projectId);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str2);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("view");
    str2 = stringBuilder1.toString();
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.fileUtil.h(str2);
      String str = this.fileUtil.a(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      b(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public boolean h(String paramString1, String paramString2) {
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
  
  public ArrayList<Pair<String, String>> i(String paramString) {
    return this.moreBlockMap.containsKey(paramString) ? this.moreBlockMap.get(paramString) : new ArrayList<Pair<String, String>>();
  }
  
  public void i() {
    this.projectId = "";
    b();
  }
  
  public void i(String paramString1, String paramString2) {
    if (paramString2.length() <= 0)
      return; 
    try {
      ProjectDataParser ProjectDataParser = new ProjectDataParser(paramString1);
      String str = ProjectDataParser.b();
      ProjectDataParser.a a = ProjectDataParser.a();
      switch (ScreenOrientationConstants.a[a.ordinal()]) {
        default:
          return;
        case 8:
          if (!this.blockMap.containsKey(str)) {
            this.blockMap.put(str, new HashMap<String, ArrayList<BlockBean>>());
          } 
          this.blockMap.get(str).put(ProjectDataParser.c(), (ArrayList<BlockBean>)ProjectDataParser.a(paramString2));
          break;
        case 7:
          this.moreBlockMap.put(str, (ArrayList<Pair<String, String>>)ProjectDataParser.a(paramString2));
          break;
        case 6:
          this.eventMap.put(str, (ArrayList<EventBean>)ProjectDataParser.a(paramString2));
          break;
        case 5:
          this.componentMap.put(str, (ArrayList<ComponentBean>)ProjectDataParser.a(paramString2));
          break;
        case 4:
          this.listMap.put(str, (ArrayList<Pair<Integer, String>>)ProjectDataParser.a(paramString2));
          break;
        case 3:
          this.variableMap.put(str, (ArrayList<Pair<Integer, String>>)ProjectDataParser.a(paramString2));
          break;
      } 
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public ArrayList<Pair<Integer, String>> j(String paramString) {
    return this.listMap.containsKey(paramString) ? this.listMap.get(paramString) : new ArrayList<Pair<Integer, String>>();
  }
  
  public void j() {
    String str = SketchwarePaths.b(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("view");
    n(stringBuilder.toString());
    stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("logic");
    m(stringBuilder.toString());
    a();
  }
  
  public void j(String paramString1, String paramString2) {
    try {
      ProjectDataParser ProjectDataParser = new ProjectDataParser(paramString1);
      String str = ProjectDataParser.b();
      ProjectDataParser.a a = ProjectDataParser.a();
      int i = ScreenOrientationConstants.a[a.ordinal()];
      if (i == 1) {
        this.viewMap.put(str, (ArrayList<ViewBean>)ProjectDataParser.a(paramString2));
      } else if (i == 2) {
        this.fabMap.put(str, (ViewBean)ProjectDataParser.a(paramString2));
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public ArrayList<Pair<Integer, String>> k(String paramString) {
    return this.variableMap.containsKey(paramString) ? this.variableMap.get(paramString) : new ArrayList<Pair<Integer, String>>();
  }
  
  public void k() {
    String str = SketchwarePaths.a(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("view");
    n(stringBuilder.toString());
    stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("logic");
    m(stringBuilder.toString());
  }
  
  public void k(String paramString1, String paramString2) {
    if (!this.blockMap.containsKey(paramString1))
      return; 
    Map map = this.blockMap.get(paramString1);
    if (map == null)
      return; 
    if (map.containsKey(paramString2))
      map.remove(paramString2); 
  }
  
  public void l(String paramString) {
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
  
  public void l(String paramString1, String paramString2) {
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
  
  public final void m(String paramString) {
    StringBuffer stringBuffer = new StringBuffer();
    a(stringBuffer);
    try {
      byte[] arrayOfByte = this.fileUtil.d(stringBuffer.toString());
      this.fileUtil.a(paramString, arrayOfByte);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void m(String paramString1, String paramString2) {
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
  
  public final void n(String paramString) {
    StringBuffer stringBuffer = new StringBuffer();
    b(stringBuffer);
    try {
      byte[] arrayOfByte = this.fileUtil.d(stringBuffer.toString());
      this.fileUtil.a(paramString, arrayOfByte);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void n(String paramString1, String paramString2) {
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
  
  public void o(String paramString1, String paramString2) {
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
  
  public void p(String paramString1, String paramString2) {
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
  
  public boolean x(String paramString, int paramInt) {
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
  
  public boolean y(String paramString1, String paramString2) {
    ArrayList arrayList = this.viewMap.get(paramString1);
    boolean bool = false;
    if (arrayList != null) {
      Iterator<ViewBean> iterator = arrayList.iterator();
      while (iterator.hasNext()) {
        if (((ViewBean)iterator.next()).getClassInfo().a(paramString2)) {
          bool = true;
          break;
        } 
      } 
    } 
    return bool;
  }
  
  public final String z(String paramString) {
    String str = paramString;
    if (paramString.contains(".")) {
      String[] arrayOfString = paramString.split("\\.");
      str = arrayOfString[arrayOfString.length - 1];
    } 
    return str;
  }
}
