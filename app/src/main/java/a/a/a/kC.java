package a.a.a;

import com.besome.sketch.beans.ProjectResourceBean;
import com.bumptech.glide.signature.StringSignature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;

public class kC {
  public static StringSignature a;
  
  public ArrayList<ProjectResourceBean> b;
  
  public ArrayList<ProjectResourceBean> c;
  
  public ArrayList<ProjectResourceBean> d;
  
  public String e = "";
  
  public String f = "";
  
  public String g = "";
  
  public oB h;
  
  public String i;
  
  public Gson j;
  
  public kC(String paramString) {
    this(paramString,
        wq.g() + File.separator + paramString,
        wq.t() + File.separator + paramString,
        wq.d() + File.separator + paramString);
  }
  
  public kC(String paramString1, String paramString2, String paramString3, String paramString4) {
    this.e = paramString2;
    this.f = paramString3;
    this.g = paramString4;
    this.i = paramString1;
    z();
    this.h = new oB(false);
    this.b = new ArrayList<ProjectResourceBean>();
    this.c = new ArrayList<ProjectResourceBean>();
    this.d = new ArrayList<ProjectResourceBean>();
    this.j = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
  }
  
  public static StringSignature n() {
    if (a == null)
      z(); 
    return a;
  }
  
  public static void z() {
    a = new StringSignature(String.valueOf(System.currentTimeMillis()));
  }
  
  public void a() {
    c();
    d();
    b();
  }
  
  public void a(BufferedReader paramBufferedReader) throws java.io.IOException {
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
            a(str, stringBuffer.toString());
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
      if (str.length() > 0)
        a(str, stringBuffer.toString()); 
      return;
    } 
  }
  
  public void a(String paramString) {
    ArrayList<ProjectResourceBean> arrayList = this.d;
    if (arrayList != null && arrayList.size() > 0) {
      File file = new File(paramString);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : this.d) {
        StringBuilder stringBuilder1 = new StringBuilder();
        stringBuilder1.append(this.g);
        stringBuilder1.append(File.separator);
        stringBuilder1.append(projectResourceBean.resFullName.toLowerCase());
        String str2 = stringBuilder1.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(paramString);
        stringBuilder2.append(File.separator);
        stringBuilder2.append(projectResourceBean.resFullName.toLowerCase());
        String str1 = stringBuilder2.toString();
        try {
          this.h.a(str2, str1);
        } catch (Exception exception) {
          exception.printStackTrace();
        } 
      } 
    } 
  }
  
  public void a(String paramString1, String paramString2) {
    if (paramString2.trim().length() <= 0) return;
    java.io.BufferedReader reader = null;
    try {
      reader = new java.io.BufferedReader(new java.io.StringReader(paramString2));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().length() <= 0 || line.trim().charAt(0) != '{') continue;
        ProjectResourceBean bean = this.j.fromJson(line, ProjectResourceBean.class);
        if (paramString1.equals("images")) {
          this.b.add(bean);
        } else if (paramString1.equals("sounds")) {
          this.c.add(bean);
        } else if (paramString1.equals("fonts")) {
          this.d.add(bean);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (reader != null) try { reader.close(); } catch (Exception e) { e.printStackTrace(); }
    }
  }
  
  public final void a(StringBuffer paramStringBuffer) {
    paramStringBuffer.append("@");
    paramStringBuffer.append("images");
    paramStringBuffer.append("\n");
    for (ProjectResourceBean projectResourceBean : this.b) {
      paramStringBuffer.append(this.j.toJson(projectResourceBean, ProjectResourceBean.class));
      paramStringBuffer.append("\n");
    } 
    paramStringBuffer.append("@");
    paramStringBuffer.append("sounds");
    paramStringBuffer.append("\n");
    for (ProjectResourceBean projectResourceBean : this.c) {
      paramStringBuffer.append(this.j.toJson(projectResourceBean, ProjectResourceBean.class));
      paramStringBuffer.append("\n");
    } 
    paramStringBuffer.append("@");
    paramStringBuffer.append("fonts");
    paramStringBuffer.append("\n");
    for (ProjectResourceBean projectResourceBean : this.d) {
      paramStringBuffer.append(this.j.toJson(projectResourceBean, ProjectResourceBean.class));
      paramStringBuffer.append("\n");
    } 
  }
  
  public void a(ArrayList<ProjectResourceBean> paramArrayList) {
    this.d = paramArrayList;
  }
  
  public void b() {
    File[] files = new File(this.g).listFiles();
    if (files == null || files.length <= 0) return;
    for (File file : files) {
      file.isDirectory();
      if (file.isFile()) {
        boolean found = false;
        for (ProjectResourceBean bean : this.d) {
          if (bean.resFullName.equals(file.getName())) {
            found = true;
            break;
          }
        }
        if (!found) file.delete();
      }
    }
  }
  
  public void b(String paramString) {
    ArrayList<ProjectResourceBean> arrayList = this.b;
    if (arrayList != null && arrayList.size() > 0) {
      File file = new File(paramString);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : this.b) {
        StringBuilder stringBuilder1 = new StringBuilder();
        stringBuilder1.append(this.e);
        stringBuilder1.append(File.separator);
        stringBuilder1.append(projectResourceBean.resFullName.toLowerCase());
        String str2 = stringBuilder1.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(paramString);
        stringBuilder2.append(File.separator);
        stringBuilder2.append(projectResourceBean.resFullName.toLowerCase());
        String str1 = stringBuilder2.toString();
        try {
          this.h.a(str2, str1);
        } catch (Exception exception) {
          exception.printStackTrace();
        } 
      } 
    } 
  }
  
  public void b(ArrayList<ProjectResourceBean> paramArrayList) {
    this.b = paramArrayList;
  }
  
  public void c() {
    File[] files = new File(this.e).listFiles();
    if (files == null || files.length <= 0) return;
    for (File file : files) {
      file.isDirectory();
      if (file.isFile()) {
        boolean found = false;
        for (ProjectResourceBean bean : this.b) {
          if (bean.resFullName.equals(file.getName())) {
            found = true;
            break;
          }
        }
        if (!found) file.delete();
      }
    }
  }
  
  public void c(String paramString) {
    ArrayList<ProjectResourceBean> arrayList = this.c;
    if (arrayList != null && arrayList.size() > 0) {
      File file = new File(paramString);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : this.c) {
        StringBuilder stringBuilder1 = new StringBuilder();
        stringBuilder1.append(this.f);
        stringBuilder1.append(File.separator);
        stringBuilder1.append(projectResourceBean.resFullName.toLowerCase());
        String str2 = stringBuilder1.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(paramString);
        stringBuilder2.append(File.separator);
        stringBuilder2.append(projectResourceBean.resFullName.toLowerCase());
        String str1 = stringBuilder2.toString();
        try {
          this.h.a(str2, str1);
        } catch (Exception exception) {
          exception.printStackTrace();
        } 
      } 
    } 
  }
  
  public void c(ArrayList<ProjectResourceBean> paramArrayList) {
    this.c = paramArrayList;
  }
  
  public String d(String paramString) {
    ArrayList<ProjectResourceBean> arrayList = this.d;
    if (arrayList != null && arrayList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.d) {
        if (projectResourceBean.resName.equals(paramString)) {
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.g);
          stringBuilder.append(File.separator);
          stringBuilder.append(projectResourceBean.resFullName);
          return stringBuilder.toString();
        } 
      }  
    return "";
  }
  
  public void d() {
    File[] files = new File(this.f).listFiles();
    if (files == null || files.length <= 0) return;
    for (File file : files) {
      file.isDirectory();
      if (file.isFile()) {
        boolean found = false;
        for (ProjectResourceBean bean : this.c) {
          if (bean.resFullName.equals(file.getName())) {
            found = true;
            break;
          }
        }
        if (!found) file.delete();
      }
    }
  }
  
  public ProjectResourceBean e(String paramString) {
    for (ProjectResourceBean projectResourceBean : this.d) {
      if (projectResourceBean.resName.equals(paramString))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public void e() {
    String str = wq.u();
    try {
      this.h.b(str);
      oB oB1 = this.h;
      File file1 = new File(this.g);
      File file2 = new File(str);
      oB1.a(file1, file2);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public String f(String paramString) {
    ArrayList<ProjectResourceBean> arrayList = this.b;
    if (arrayList != null && arrayList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.b) {
        if (projectResourceBean.resName.equals(paramString)) {
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.e);
          stringBuilder.append(File.separator);
          stringBuilder.append(projectResourceBean.resFullName);
          return stringBuilder.toString();
        } 
      }  
    return "";
  }
  
  public void f() {
    String str = wq.v();
    try {
      this.h.b(str);
      oB oB1 = this.h;
      File file1 = new File(this.e);
      File file2 = new File(str);
      oB1.a(file1, file2);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public ProjectResourceBean g(String paramString) {
    for (ProjectResourceBean projectResourceBean : this.b) {
      if (projectResourceBean.resName.equals(paramString))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public void g() {
    String str = wq.w();
    try {
      this.h.b(str);
      oB oB1 = this.h;
      File file1 = new File(this.f);
      File file2 = new File(str);
      oB1.a(file1, file2);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public int h(String paramString) {
    ArrayList<ProjectResourceBean> arrayList = this.b;
    if (arrayList != null && arrayList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.b) {
        if (projectResourceBean.resName.equals(paramString))
          return projectResourceBean.resType; 
      }  
    return -1;
  }
  
  public void h() {
    String str1 = wq.v();
    String str2 = wq.w();
    String str3 = wq.u();
    try {
      this.h.b(str1);
      this.h.b(str2);
      this.h.b(str3);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public String i(String paramString) {
    ArrayList<ProjectResourceBean> arrayList = this.c;
    if (arrayList != null && arrayList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.c) {
        if (projectResourceBean.resName.equals(paramString)) {
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.f);
          stringBuilder.append(File.separator);
          stringBuilder.append(projectResourceBean.resFullName);
          return stringBuilder.toString();
        } 
      }  
    return "";
  }
  
  public void i() {
    String str = wq.a(this.i);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
    str = stringBuilder.toString();
    this.h.c(str);
  }
  
  public ProjectResourceBean j(String paramString) {
    for (ProjectResourceBean projectResourceBean : this.c) {
      if (projectResourceBean.resName.equals(paramString))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public String j() {
    return this.g;
  }
  
  public ArrayList<String> k() {
    ArrayList<String> arrayList = new ArrayList<>();
    Iterator<ProjectResourceBean> iterator = this.d.iterator();
    while (iterator.hasNext())
      arrayList.add(((ProjectResourceBean)iterator.next()).resName); 
    return arrayList;
  }
  
  public boolean k(String paramString) {
    Iterator<ProjectResourceBean> iterator = this.d.iterator();
    while (iterator.hasNext()) {
      if (((ProjectResourceBean)iterator.next()).resName.equals(paramString))
        return true; 
    } 
    return false;
  }
  
  public String l() {
    return this.e;
  }
  
  public boolean l(String paramString) {
    Iterator<ProjectResourceBean> iterator = this.b.iterator();
    while (iterator.hasNext()) {
      if (((ProjectResourceBean)iterator.next()).resName.equals(paramString))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<String> m() {
    ArrayList<String> arrayList = new ArrayList<>();
    Iterator<ProjectResourceBean> iterator = this.b.iterator();
    while (iterator.hasNext())
      arrayList.add(((ProjectResourceBean)iterator.next()).resName); 
    return arrayList;
  }
  
  public boolean m(String paramString) {
    Iterator<ProjectResourceBean> iterator = this.c.iterator();
    while (iterator.hasNext()) {
      if (((ProjectResourceBean)iterator.next()).resName.equals(paramString))
        return true; 
    } 
    return false;
  }
  
  public String o() {
    return this.f;
  }
  
  public ArrayList<String> p() {
    ArrayList<String> arrayList = new ArrayList<>();
    Iterator<ProjectResourceBean> iterator = this.c.iterator();
    while (iterator.hasNext())
      arrayList.add(((ProjectResourceBean)iterator.next()).resName); 
    return arrayList;
  }
  
  public boolean q() {
    String str = wq.a(this.i);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
    str = stringBuilder.toString();
    return this.h.e(str);
  }
  
  public void r() {
    this.b = new ArrayList<ProjectResourceBean>();
    this.c = new ArrayList<ProjectResourceBean>();
    this.d = new ArrayList<ProjectResourceBean>();
    String str1 = wq.a(this.i);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str1);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
    String str2 = stringBuilder.toString();
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.h.h(str2);
      String str = this.h.a(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      a(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void s() {
    this.b = new ArrayList<ProjectResourceBean>();
    this.c = new ArrayList<ProjectResourceBean>();
    this.d = new ArrayList<ProjectResourceBean>();
    String str1 = wq.b(this.i);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str1);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("resource");
    str1 = stringBuilder1.toString();
    if (!this.h.e(str1))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.h.h(str1);
      String str = this.h.a(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      a(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void t() {
    this.i = "";
    this.e = "";
    this.f = "";
    this.g = "";
    this.b = new ArrayList<ProjectResourceBean>();
    this.c = new ArrayList<ProjectResourceBean>();
    this.d = new ArrayList<ProjectResourceBean>();
  }
  
  public void u() {
    String str = wq.u();
    try {
      oB oB1 = this.h;
      File file2 = new File(this.g);
      oB1.a(file2);
      oB1 = this.h;
      file2 = new File(str);
      File file1 = new File(this.g);
      oB1.a(file2, file1);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void v() {
    String str = wq.v();
    try {
      oB oB1 = this.h;
      File file2 = new File(this.e);
      oB1.a(file2);
      oB1 = this.h;
      file2 = new File(str);
      File file1 = new File(this.e);
      oB1.a(file2, file1);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void w() {
    String str = wq.w();
    try {
      oB oB1 = this.h;
      File file2 = new File(this.f);
      oB1.a(file2);
      oB1 = this.h;
      file2 = new File(str);
      File file1 = new File(this.f);
      oB1.a(file2, file1);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void x() {
    String str1 = wq.b(this.i);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str1);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
    String str2 = stringBuilder.toString();
    StringBuffer stringBuffer = new StringBuffer();
    a(stringBuffer);
    try {
      byte[] arrayOfByte = this.h.d(stringBuffer.toString());
      this.h.a(str2, arrayOfByte);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
    i();
  }
  
  public void y() {
    String str = wq.a(this.i);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
    str = stringBuilder.toString();
    StringBuffer stringBuffer = new StringBuffer();
    a(stringBuffer);
    try {
      byte[] arrayOfByte = this.h.d(stringBuffer.toString());
      this.h.a(str, arrayOfByte);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
}
