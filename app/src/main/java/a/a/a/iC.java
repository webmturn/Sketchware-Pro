package a.a.a;

import com.besome.sketch.beans.ProjectLibraryBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;

public class iC {
  public String a;
  
  public oB b;
  
  public ProjectLibraryBean c;
  
  public ProjectLibraryBean d;
  
  public ProjectLibraryBean e;
  
  public ProjectLibraryBean f;
  
  public Gson g;
  
  public iC(String paramString) {
    this.a = paramString;
    this.b = new oB();
    this.g = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
    f();
  }
  
  public void a() {
    String str = wq.a(this.a);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("library");
    str = stringBuilder.toString();
    this.b.c(str);
  }
  
  public void a(ProjectLibraryBean paramProjectLibraryBean) {
    this.e = paramProjectLibraryBean;
  }
  
  public void a(BufferedReader paramBufferedReader) throws java.io.IOException {
    try {
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
        if (this.c == null)
          this.c = new ProjectLibraryBean(0); 
        if (this.d == null)
          this.d = new ProjectLibraryBean(1); 
        if (this.e == null)
          this.e = new ProjectLibraryBean(2); 
        if (this.f == null)
          this.f = new ProjectLibraryBean(3); 
        return;
      } 
    } catch (Exception exception) {
      throw exception;
    } 
  }
  
  public final void a(String paramString) {
    StringBuffer stringBuffer = new StringBuffer();
    a(stringBuffer);
    try {
      byte[] arrayOfByte = this.b.d(stringBuffer.toString());
      this.b.a(paramString, arrayOfByte);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void a(String paramString1, String paramString2) {
    if (paramString2.length() <= 0)
      return; 
    BufferedReader bufferedReader = null;
    try {
      bufferedReader = new BufferedReader(new StringReader(paramString2));
      ProjectLibraryBean bean = this.g.fromJson(paramString2, ProjectLibraryBean.class);
      if (paramString1.equals("firebaseDB")) {
        this.c = bean;
      } else if (paramString1.equals("compat")) {
        this.d = bean;
      } else if (paramString1.equals("admob")) {
        this.e = bean;
      } else if (paramString1.equals("googleMap")) {
        this.f = bean;
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public final void a(StringBuffer paramStringBuffer) {
    if (this.c != null) {
      paramStringBuffer.append("@");
      paramStringBuffer.append("firebaseDB");
      paramStringBuffer.append("\n");
      paramStringBuffer.append(this.g.toJson(this.c, ProjectLibraryBean.class));
      paramStringBuffer.append("\n");
    } 
    if (this.d != null) {
      paramStringBuffer.append("@");
      paramStringBuffer.append("compat");
      paramStringBuffer.append("\n");
      paramStringBuffer.append(this.g.toJson(this.d, ProjectLibraryBean.class));
      paramStringBuffer.append("\n");
    } 
    if (this.e != null) {
      paramStringBuffer.append("@");
      paramStringBuffer.append("admob");
      paramStringBuffer.append("\n");
      paramStringBuffer.append(this.g.toJson(this.e, ProjectLibraryBean.class));
      paramStringBuffer.append("\n");
    } 
    if (this.f != null) {
      paramStringBuffer.append("@");
      paramStringBuffer.append("googleMap");
      paramStringBuffer.append("\n");
      paramStringBuffer.append(this.g.toJson(this.f, ProjectLibraryBean.class));
      paramStringBuffer.append("\n");
    } 
  }
  
  public ProjectLibraryBean b() {
    return this.e;
  }
  
  public void b(ProjectLibraryBean paramProjectLibraryBean) {
    this.d = paramProjectLibraryBean;
  }
  
  public ProjectLibraryBean c() {
    return this.d;
  }
  
  public void c(ProjectLibraryBean paramProjectLibraryBean) {
    this.c = paramProjectLibraryBean;
  }
  
  public ProjectLibraryBean d() {
    return this.c;
  }
  
  public void d(ProjectLibraryBean paramProjectLibraryBean) {
    this.f = paramProjectLibraryBean;
  }
  
  public ProjectLibraryBean e() {
    return this.f;
  }
  
  public final void f() {
    this.c = new ProjectLibraryBean(0);
    this.d = new ProjectLibraryBean(1);
    this.e = new ProjectLibraryBean(2);
    this.f = new ProjectLibraryBean(3);
  }
  
  public boolean g() {
    String str1 = wq.a(this.a);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str1);
    stringBuilder.append(File.separator);
    stringBuilder.append("library");
    String str2 = stringBuilder.toString();
    return this.b.e(str2);
  }
  
  public void h() {
    f();
    String str1 = wq.a(this.a);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str1);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("library");
    str1 = stringBuilder1.toString();
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.b.h(str1);
      String str = this.b.a(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      a(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void i() {
    f();
    String str1 = wq.b(this.a);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str1);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("library");
    str1 = stringBuilder1.toString();
    if (!this.b.e(str1))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.b.h(str1);
      String str = this.b.a(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      a(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void j() {
    this.a = "";
    f();
  }
  
  public void k() {
    String str = wq.a(this.a);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("library");
    a(stringBuilder.toString());
  }
  
  public void l() {
    String str = wq.b(this.a);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("library");
    a(stringBuilder.toString());
    a();
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\iC.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */