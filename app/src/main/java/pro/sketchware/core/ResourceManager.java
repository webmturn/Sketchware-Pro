package pro.sketchware.core;

import com.besome.sketch.beans.ProjectResourceBean;
import com.bumptech.glide.signature.StringSignature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;

public class ResourceManager {
  public static StringSignature cacheSignature;
  
  public ArrayList<ProjectResourceBean> images;
  
  public ArrayList<ProjectResourceBean> sounds;
  
  public ArrayList<ProjectResourceBean> fonts;
  
  public String imageDirPath = "";
  
  public String soundDirPath = "";
  
  public String fontDirPath = "";
  
  public EncryptedFileUtil fileUtil;
  
  public String projectId;
  
  public Gson gson;
  
  public ResourceManager(String paramString) {
    this(paramString,
        SketchwarePaths.g() + File.separator + paramString,
        SketchwarePaths.t() + File.separator + paramString,
        SketchwarePaths.d() + File.separator + paramString);
  }
  
  public ResourceManager(String paramString1, String paramString2, String paramString3, String paramString4) {
    this.imageDirPath = paramString2;
    this.soundDirPath = paramString3;
    this.fontDirPath = paramString4;
    this.projectId = paramString1;
    z();
    this.fileUtil = new EncryptedFileUtil(false);
    this.images = new ArrayList<ProjectResourceBean>();
    this.sounds = new ArrayList<ProjectResourceBean>();
    this.fonts = new ArrayList<ProjectResourceBean>();
    this.gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
  }
  
  public static StringSignature n() {
    if (cacheSignature == null)
      z(); 
    return cacheSignature;
  }
  
  public static void z() {
    cacheSignature = new StringSignature(String.valueOf(System.currentTimeMillis()));
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
    ArrayList<ProjectResourceBean> arrayList = this.fonts;
    if (arrayList != null && arrayList.size() > 0) {
      File file = new File(paramString);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : this.fonts) {
        StringBuilder stringBuilder1 = new StringBuilder();
        stringBuilder1.append(this.fontDirPath);
        stringBuilder1.append(File.separator);
        stringBuilder1.append(projectResourceBean.resFullName.toLowerCase());
        String str2 = stringBuilder1.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(paramString);
        stringBuilder2.append(File.separator);
        stringBuilder2.append(projectResourceBean.resFullName.toLowerCase());
        String str1 = stringBuilder2.toString();
        try {
          this.fileUtil.a(str2, str1);
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
        ProjectResourceBean bean = this.gson.fromJson(line, ProjectResourceBean.class);
        if (paramString1.equals("images")) {
          this.images.add(bean);
        } else if (paramString1.equals("sounds")) {
          this.sounds.add(bean);
        } else if (paramString1.equals("fonts")) {
          this.fonts.add(bean);
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
    for (ProjectResourceBean projectResourceBean : this.images) {
      paramStringBuffer.append(this.gson.toJson(projectResourceBean, ProjectResourceBean.class));
      paramStringBuffer.append("\n");
    } 
    paramStringBuffer.append("@");
    paramStringBuffer.append("sounds");
    paramStringBuffer.append("\n");
    for (ProjectResourceBean projectResourceBean : this.sounds) {
      paramStringBuffer.append(this.gson.toJson(projectResourceBean, ProjectResourceBean.class));
      paramStringBuffer.append("\n");
    } 
    paramStringBuffer.append("@");
    paramStringBuffer.append("fonts");
    paramStringBuffer.append("\n");
    for (ProjectResourceBean projectResourceBean : this.fonts) {
      paramStringBuffer.append(this.gson.toJson(projectResourceBean, ProjectResourceBean.class));
      paramStringBuffer.append("\n");
    } 
  }
  
  public void a(ArrayList<ProjectResourceBean> paramArrayList) {
    this.fonts = paramArrayList;
  }
  
  public void b() {
    File[] files = new File(this.fontDirPath).listFiles();
    if (files == null || files.length <= 0) return;
    for (File file : files) {
      file.isDirectory();
      if (file.isFile()) {
        boolean found = false;
        for (ProjectResourceBean bean : this.fonts) {
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
    ArrayList<ProjectResourceBean> arrayList = this.images;
    if (arrayList != null && arrayList.size() > 0) {
      File file = new File(paramString);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : this.images) {
        StringBuilder stringBuilder1 = new StringBuilder();
        stringBuilder1.append(this.imageDirPath);
        stringBuilder1.append(File.separator);
        stringBuilder1.append(projectResourceBean.resFullName.toLowerCase());
        String str2 = stringBuilder1.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(paramString);
        stringBuilder2.append(File.separator);
        stringBuilder2.append(projectResourceBean.resFullName.toLowerCase());
        String str1 = stringBuilder2.toString();
        try {
          this.fileUtil.a(str2, str1);
        } catch (Exception exception) {
          exception.printStackTrace();
        } 
      } 
    } 
  }
  
  public void b(ArrayList<ProjectResourceBean> paramArrayList) {
    this.images = paramArrayList;
  }
  
  public void c() {
    File[] files = new File(this.imageDirPath).listFiles();
    if (files == null || files.length <= 0) return;
    for (File file : files) {
      file.isDirectory();
      if (file.isFile()) {
        boolean found = false;
        for (ProjectResourceBean bean : this.images) {
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
    ArrayList<ProjectResourceBean> arrayList = this.sounds;
    if (arrayList != null && arrayList.size() > 0) {
      File file = new File(paramString);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : this.sounds) {
        StringBuilder stringBuilder1 = new StringBuilder();
        stringBuilder1.append(this.soundDirPath);
        stringBuilder1.append(File.separator);
        stringBuilder1.append(projectResourceBean.resFullName.toLowerCase());
        String str2 = stringBuilder1.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(paramString);
        stringBuilder2.append(File.separator);
        stringBuilder2.append(projectResourceBean.resFullName.toLowerCase());
        String str1 = stringBuilder2.toString();
        try {
          this.fileUtil.a(str2, str1);
        } catch (Exception exception) {
          exception.printStackTrace();
        } 
      } 
    } 
  }
  
  public void c(ArrayList<ProjectResourceBean> paramArrayList) {
    this.sounds = paramArrayList;
  }
  
  public String d(String paramString) {
    ArrayList<ProjectResourceBean> arrayList = this.fonts;
    if (arrayList != null && arrayList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.fonts) {
        if (projectResourceBean.resName.equals(paramString)) {
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.fontDirPath);
          stringBuilder.append(File.separator);
          stringBuilder.append(projectResourceBean.resFullName);
          return stringBuilder.toString();
        } 
      }  
    return "";
  }
  
  public void d() {
    File[] files = new File(this.soundDirPath).listFiles();
    if (files == null || files.length <= 0) return;
    for (File file : files) {
      file.isDirectory();
      if (file.isFile()) {
        boolean found = false;
        for (ProjectResourceBean bean : this.sounds) {
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
    for (ProjectResourceBean projectResourceBean : this.fonts) {
      if (projectResourceBean.resName.equals(paramString))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public void e() {
    String str = SketchwarePaths.u();
    try {
      this.fileUtil.b(str);
      EncryptedFileUtil oB1 = this.fileUtil;
      File file1 = new File(this.fontDirPath);
      File file2 = new File(str);
      oB1.a(file1, file2);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public String f(String paramString) {
    ArrayList<ProjectResourceBean> arrayList = this.images;
    if (arrayList != null && arrayList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.images) {
        if (projectResourceBean.resName.equals(paramString)) {
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.imageDirPath);
          stringBuilder.append(File.separator);
          stringBuilder.append(projectResourceBean.resFullName);
          return stringBuilder.toString();
        } 
      }  
    return "";
  }
  
  public void f() {
    String str = SketchwarePaths.v();
    try {
      this.fileUtil.b(str);
      EncryptedFileUtil oB1 = this.fileUtil;
      File file1 = new File(this.imageDirPath);
      File file2 = new File(str);
      oB1.a(file1, file2);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public ProjectResourceBean g(String paramString) {
    for (ProjectResourceBean projectResourceBean : this.images) {
      if (projectResourceBean.resName.equals(paramString))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public void g() {
    String str = SketchwarePaths.w();
    try {
      this.fileUtil.b(str);
      EncryptedFileUtil oB1 = this.fileUtil;
      File file1 = new File(this.soundDirPath);
      File file2 = new File(str);
      oB1.a(file1, file2);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public int h(String paramString) {
    ArrayList<ProjectResourceBean> arrayList = this.images;
    if (arrayList != null && arrayList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.images) {
        if (projectResourceBean.resName.equals(paramString))
          return projectResourceBean.resType; 
      }  
    return -1;
  }
  
  public void h() {
    String str1 = SketchwarePaths.v();
    String str2 = SketchwarePaths.w();
    String str3 = SketchwarePaths.u();
    try {
      this.fileUtil.b(str1);
      this.fileUtil.b(str2);
      this.fileUtil.b(str3);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public String i(String paramString) {
    ArrayList<ProjectResourceBean> arrayList = this.sounds;
    if (arrayList != null && arrayList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.sounds) {
        if (projectResourceBean.resName.equals(paramString)) {
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.soundDirPath);
          stringBuilder.append(File.separator);
          stringBuilder.append(projectResourceBean.resFullName);
          return stringBuilder.toString();
        } 
      }  
    return "";
  }
  
  public void i() {
    String str = SketchwarePaths.a(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
    str = stringBuilder.toString();
    this.fileUtil.c(str);
  }
  
  public ProjectResourceBean j(String paramString) {
    for (ProjectResourceBean projectResourceBean : this.sounds) {
      if (projectResourceBean.resName.equals(paramString))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public String j() {
    return this.fontDirPath;
  }
  
  public ArrayList<String> k() {
    ArrayList<String> arrayList = new ArrayList<>();
    Iterator<ProjectResourceBean> iterator = this.fonts.iterator();
    while (iterator.hasNext())
      arrayList.add(((ProjectResourceBean)iterator.next()).resName); 
    return arrayList;
  }
  
  public boolean k(String paramString) {
    Iterator<ProjectResourceBean> iterator = this.fonts.iterator();
    while (iterator.hasNext()) {
      if (((ProjectResourceBean)iterator.next()).resName.equals(paramString))
        return true; 
    } 
    return false;
  }
  
  public String l() {
    return this.imageDirPath;
  }
  
  public boolean l(String paramString) {
    Iterator<ProjectResourceBean> iterator = this.images.iterator();
    while (iterator.hasNext()) {
      if (((ProjectResourceBean)iterator.next()).resName.equals(paramString))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<String> m() {
    ArrayList<String> arrayList = new ArrayList<>();
    Iterator<ProjectResourceBean> iterator = this.images.iterator();
    while (iterator.hasNext())
      arrayList.add(((ProjectResourceBean)iterator.next()).resName); 
    return arrayList;
  }
  
  public boolean m(String paramString) {
    Iterator<ProjectResourceBean> iterator = this.sounds.iterator();
    while (iterator.hasNext()) {
      if (((ProjectResourceBean)iterator.next()).resName.equals(paramString))
        return true; 
    } 
    return false;
  }
  
  public String o() {
    return this.soundDirPath;
  }
  
  public ArrayList<String> p() {
    ArrayList<String> arrayList = new ArrayList<>();
    Iterator<ProjectResourceBean> iterator = this.sounds.iterator();
    while (iterator.hasNext())
      arrayList.add(((ProjectResourceBean)iterator.next()).resName); 
    return arrayList;
  }
  
  public boolean q() {
    String str = SketchwarePaths.a(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
    str = stringBuilder.toString();
    return this.fileUtil.e(str);
  }
  
  public void r() {
    this.images = new ArrayList<ProjectResourceBean>();
    this.sounds = new ArrayList<ProjectResourceBean>();
    this.fonts = new ArrayList<ProjectResourceBean>();
    String str1 = SketchwarePaths.a(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str1);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
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
  
  public void s() {
    this.images = new ArrayList<ProjectResourceBean>();
    this.sounds = new ArrayList<ProjectResourceBean>();
    this.fonts = new ArrayList<ProjectResourceBean>();
    String str1 = SketchwarePaths.b(this.projectId);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str1);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("resource");
    str1 = stringBuilder1.toString();
    if (!this.fileUtil.e(str1))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.fileUtil.h(str1);
      String str = this.fileUtil.a(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      a(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void t() {
    this.projectId = "";
    this.imageDirPath = "";
    this.soundDirPath = "";
    this.fontDirPath = "";
    this.images = new ArrayList<ProjectResourceBean>();
    this.sounds = new ArrayList<ProjectResourceBean>();
    this.fonts = new ArrayList<ProjectResourceBean>();
  }
  
  public void u() {
    String str = SketchwarePaths.u();
    try {
      EncryptedFileUtil oB1 = this.fileUtil;
      File file2 = new File(this.fontDirPath);
      oB1.a(file2);
      oB1 = this.fileUtil;
      file2 = new File(str);
      File file1 = new File(this.fontDirPath);
      oB1.a(file2, file1);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void v() {
    String str = SketchwarePaths.v();
    try {
      EncryptedFileUtil oB1 = this.fileUtil;
      File file2 = new File(this.imageDirPath);
      oB1.a(file2);
      oB1 = this.fileUtil;
      file2 = new File(str);
      File file1 = new File(this.imageDirPath);
      oB1.a(file2, file1);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void w() {
    String str = SketchwarePaths.w();
    try {
      EncryptedFileUtil oB1 = this.fileUtil;
      File file2 = new File(this.soundDirPath);
      oB1.a(file2);
      oB1 = this.fileUtil;
      file2 = new File(str);
      File file1 = new File(this.soundDirPath);
      oB1.a(file2, file1);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void x() {
    String str1 = SketchwarePaths.b(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str1);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
    String str2 = stringBuilder.toString();
    StringBuffer stringBuffer = new StringBuffer();
    a(stringBuffer);
    try {
      byte[] arrayOfByte = this.fileUtil.d(stringBuffer.toString());
      this.fileUtil.a(str2, arrayOfByte);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
    i();
  }
  
  public void y() {
    String str = SketchwarePaths.a(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
    str = stringBuilder.toString();
    StringBuffer stringBuffer = new StringBuffer();
    a(stringBuffer);
    try {
      byte[] arrayOfByte = this.fileUtil.d(stringBuffer.toString());
      this.fileUtil.a(str, arrayOfByte);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
}
