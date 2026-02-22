package a.a.a;

import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ViewBean;
import java.util.ArrayList;
import pro.sketchware.R;

public class rq {
  public static int a(String paramString) {
    byte b;
    if (paramString.hashCode() == 1424216003 && paramString.equals("Basic List Item")) {
      b = 0;
    } else {
      b = -1;
    } 
    return (b != 0) ? -1 : R.drawable.activity_preset_1;
  }
  
  public static ViewBean a() {
    ViewBean viewBean = new ViewBean("textview1", 4);
    viewBean.parent = "root";
    viewBean.index = 0;
    viewBean.preParentType = 0;
    viewBean.name = "textview1";
    viewBean.text.text = "TextView";
    LayoutBean layoutBean = viewBean.layout;
    layoutBean.paddingTop = 8;
    layoutBean.paddingBottom = 8;
    layoutBean.paddingLeft = 8;
    layoutBean.paddingRight = 8;
    return viewBean;
  }
  
  public static ArrayList<ProjectFileBean> b() {
    ArrayList<ProjectFileBean> arrayList = new ArrayList<>();
    arrayList.add(i());
    return arrayList;
  }
  
  public static ArrayList<ViewBean> b(String paramString) {
    byte b;
    if (paramString.hashCode() == 1424216003 && paramString.equals("Basic List Item")) {
      b = 0;
    } else {
      b = -1;
    } 
    return (b != 0) ? new ArrayList<ViewBean>() : j();
  }
  
  public static int c(String paramString) {
    byte b;
    if (paramString.hashCode() == 920477027 && paramString.equals("Basic Drawer")) {
      b = 0;
    } else {
      b = -1;
    } 
    return (b != 0) ? -1 : R.drawable.activity_preset_1;
  }
  
  public static ArrayList<ProjectFileBean> c() {
    ArrayList<ProjectFileBean> arrayList = new ArrayList<>();
    arrayList.add(g());
    return arrayList;
  }
  
  public static ArrayList<ProjectFileBean> d() {
    ArrayList<ProjectFileBean> arrayList = new ArrayList<>();
    arrayList.add(k());
    arrayList.add(e());
    arrayList.add(m());
    return arrayList;
  }
  
  public static ArrayList<ViewBean> d(String paramString) {
    byte b;
    if (paramString.hashCode() == 920477027 && paramString.equals("Basic Drawer")) {
      b = 0;
    } else {
      b = -1;
    } 
    return (b != 0) ? new ArrayList<ViewBean>() : h();
  }
  
  public static int e(String paramString) {
    switch (paramString) {
      case "Empty Activity":
        return R.drawable.activity_preset_4;
      case "Basic Activity":
      case "Text Activity":
        return R.drawable.activity_preset_1;
      default:
        return -1;
    }
  }
  
  public static ProjectFileBean e() {
    return new ProjectFileBean(0, null, "Basic Activity", 0, 0, true, false, false, false);
  }
  
  public static ArrayList<ViewBean> f() {
    return new ArrayList<ViewBean>();
  }
  
  public static ArrayList<ViewBean> f(String paramString) {
    switch (paramString) {
      case "Empty Activity":
        return l();
      case "Basic Activity":
        return f();
      case "Text Activity":
        return n();
      default:
        return new ArrayList<ViewBean>();
    }
  }
  
  public static ProjectFileBean g() {
    return new ProjectFileBean(2, null, "Basic Drawer");
  }
  
  public static ArrayList<ViewBean> h() {
    ArrayList<ViewBean> arrayList = new ArrayList<>();
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    return arrayList;
  }
  
  public static ProjectFileBean i() {
    return new ProjectFileBean(1, null, "Basic List Item");
  }
  
  public static ArrayList<ViewBean> j() {
    ArrayList<ViewBean> arrayList = new ArrayList<>();
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    return arrayList;
  }
  
  public static ProjectFileBean k() {
    return new ProjectFileBean(0, null, "Empty Activity", 0, 0, false, true, false, false);
  }
  
  public static ArrayList<ViewBean> l() {
    return new ArrayList<ViewBean>();
  }
  
  public static ProjectFileBean m() {
    return new ProjectFileBean(0, null, "Text Activity", 0, 0, true, false, false, false);
  }
  
  public static ArrayList<ViewBean> n() {
    ArrayList<ViewBean> arrayList = new ArrayList<>();
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    arrayList.add(a());
    return arrayList;
  }
}
