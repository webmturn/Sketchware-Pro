package a.a.a;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.CollectionBean;
import com.besome.sketch.beans.MoreBlockCollectionBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class Pp extends Lp {
  public static Pp f;
  
  public Gson g = null;
  
  public Pp() {
    i();
  }
  
  public static Pp h() {
    if (f == null) {
      synchronized (Pp.class) {
        if (f == null) {
          f = new Pp();
        }
      }
    }
    return f;
  }
  
  public MoreBlockCollectionBean a(String paramString) {
    for (CollectionBean collectionBean : this.e) {
      if (collectionBean.name.equals(paramString))
        return new MoreBlockCollectionBean(collectionBean.name, collectionBean.reserved1, gC.a(this.g, collectionBean.data)); 
    } 
    return null;
  }
  
  public void a(String paramString1, String paramString2, ArrayList<BlockBean> paramArrayList, boolean paramBoolean) {
    if (this.e == null)
      a(); 
    if (this.g == null)
      i(); 
    Iterator<CollectionBean> iterator = this.e.iterator();
    while (iterator.hasNext()) {
      if (!((CollectionBean)iterator.next()).name.equals(paramString1))
        continue; 
      throw new yy("duplicate_name");
    } 
    iterator = (Iterator)paramArrayList.iterator();
    String str;
    for (str = ""; iterator.hasNext(); str = stringBuilder.toString()) {
      BlockBean blockBean = (BlockBean)iterator.next();
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(str);
      stringBuilder.append(this.g.toJson(blockBean));
      stringBuilder.append("\n");
    } 
    this.e.add(new CollectionBean(paramString1, str, paramString2));
    if (paramBoolean)
      e(); 
  }
  
  public void a(String paramString1, String paramString2, boolean paramBoolean) {
    for (CollectionBean collectionBean : this.e) {
      if (collectionBean.name.equals(paramString1)) {
        collectionBean.name = paramString2;
        break;
      } 
    } 
    if (paramBoolean)
      e(); 
  }
  
  public void a(String paramString, boolean paramBoolean) {
    int i = this.e.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        i = j;
        if (((CollectionBean)this.e.get(j)).name.equals(paramString)) {
          this.e.remove(j);
          break;
        } 
        continue;
      } 
      break;
    } 
    if (paramBoolean)
      e(); 
  }
  
  public void b() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(wq.a());
    stringBuilder.append(File.separator);
    stringBuilder.append("more_block");
    stringBuilder.append(File.separator);
    stringBuilder.append("list");
    this.a = stringBuilder.toString();
  }
  
  public ArrayList<MoreBlockCollectionBean> f() {
    if (this.e == null)
      a(); 
    if (this.g == null)
      i(); 
    ArrayList<MoreBlockCollectionBean> arrayList = new ArrayList();
    for (CollectionBean collectionBean : this.e)
      arrayList.add(new MoreBlockCollectionBean(collectionBean.name, collectionBean.reserved1, gC.a(this.g, collectionBean.data))); 
    return arrayList;
  }
  
  public ArrayList<String> g() {
    if (this.e == null)
      a(); 
    ArrayList<String> arrayList = new ArrayList();
    Iterator<CollectionBean> iterator = this.e.iterator();
    while (iterator.hasNext())
      arrayList.add(((CollectionBean)iterator.next()).name); 
    return arrayList;
  }
  
  public final void i() {
    this.g = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\Pp.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */