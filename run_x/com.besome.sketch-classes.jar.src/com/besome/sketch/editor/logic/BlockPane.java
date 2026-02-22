package com.besome.sketch.editor.logic;

import a.a.a.Gx;
import a.a.a.Rs;
import a.a.a.Ts;
import a.a.a.mq;
import a.a.a.wB;
import android.content.Context;
import android.graphics.Point;
import android.view.View;
import android.widget.RelativeLayout;
import com.besome.sketch.beans.BlockBean;
import java.util.ArrayList;
import java.util.Iterator;

public class BlockPane extends RelativeLayout {
  public Context a;
  
  public int[] b = new int[2];
  
  public Ts c;
  
  public Rs d;
  
  public ArrayList<Object[]> e = new ArrayList();
  
  public Object[] f = null;
  
  public int g = 10;
  
  public float h = wB.a(getContext(), 1.0F);
  
  public BlockPane(Context paramContext) {
    super(paramContext);
    a(paramContext);
  }
  
  public Rs a(int paramInt) {
    return (Rs)findViewWithTag(Integer.valueOf(paramInt));
  }
  
  public Rs a(Rs paramRs, int paramInt1, int paramInt2) {
    getLocationOnScreen(this.b);
    Rs rs = paramRs;
    if (paramRs.getBlockType() == 1) {
      Context context = getContext();
      int i = this.g;
      this.g = i + 1;
      rs = new Rs(context, i, paramRs.T, ((Ts)paramRs).b, ((Ts)paramRs).c, paramRs.U);
    } 
    rs.pa = this;
    addView((View)rs);
    rs.setX((paramInt1 - this.b[0] - getPaddingLeft()));
    rs.setY((paramInt2 - this.b[1] - getPaddingTop()));
    return rs;
  }
  
  public Rs a(Rs paramRs, int paramInt1, int paramInt2, boolean paramBoolean) {
    Rs rs;
    if (!paramBoolean) {
      rs = a(paramRs, paramInt1, paramInt2);
    } else {
      paramRs.setX((paramInt1 - this.b[0] - getPaddingLeft()));
      paramRs.setY((paramInt2 - this.b[1] - getPaddingTop()));
      rs = paramRs;
    } 
    Object[] arrayOfObject = this.f;
    if (arrayOfObject == null) {
      rs.p().k();
      b();
      return rs;
    } 
    if (paramRs.fa) {
      ((Ts)arrayOfObject[1]).E.a((Ts)arrayOfObject[1], rs);
    } else {
      paramRs = (Rs)arrayOfObject[1];
      paramInt1 = ((Integer)arrayOfObject[2]).intValue();
      if (paramInt1 != 0) {
        if (paramInt1 != 1) {
          if (paramInt1 != 2) {
            if (paramInt1 != 3) {
              if (paramInt1 == 4)
                paramRs.d(rs); 
            } else {
              paramRs.f(rs);
            } 
          } else {
            paramRs.e(rs);
          } 
        } else {
          paramRs.c(rs);
        } 
      } else {
        paramRs.b(rs);
      } 
    } 
    rs.p().k();
    b();
    return rs;
  }
  
  public Rs a(String paramString) {
    return (Rs)findViewWithTag(Integer.valueOf(paramString));
  }
  
  public final void a() {
    if (this.c == null)
      this.c = new Ts(this.a, " ", true); 
    this.c.a(10.0F, 10.0F, false);
    addView((View)this.c);
    d();
  }
  
  public void a(Rs paramRs) {
    boolean bool;
    boolean bool1 = (paramRs.h()).ga;
    if (paramRs.b() && -1 == paramRs.ia) {
      bool = true;
    } else {
      bool = false;
    } 
    boolean bool2 = paramRs.fa;
    a(paramRs.getTag().toString(), bool1, bool, bool2, paramRs.getHeight(), paramRs.f());
    this.f = null;
  }
  
  public void a(Rs paramRs, int paramInt) {
    while (paramRs != null) {
      paramRs.setVisibility(paramInt);
      for (View view : paramRs.V) {
        if (view instanceof Rs)
          a((Rs)view, paramInt); 
      } 
      if (paramRs.b()) {
        int j = paramRs.ia;
        if (j != -1)
          a((Rs)findViewWithTag(Integer.valueOf(j)), paramInt); 
      } 
      if (paramRs.c()) {
        int j = paramRs.ja;
        if (j != -1)
          a((Rs)findViewWithTag(Integer.valueOf(j)), paramInt); 
      } 
      int i = paramRs.ha;
      if (i != -1)
        paramRs = (Rs)findViewWithTag(Integer.valueOf(i)); 
    } 
  }
  
  public final void a(Rs paramRs, String paramString) {
    while (paramRs != null) {
      if (!paramRs.ea)
        for (byte b = 0; b < paramRs.V.size(); b++) {
          View view = paramRs.V.get(b);
          boolean bool = view instanceof Rs;
          if ((bool || view instanceof a.a.a.Ss) && (!bool || !view.getTag().toString().equals(paramString))) {
            int[] arrayOfInt = new int[2];
            view.getLocationOnScreen(arrayOfInt);
            a(arrayOfInt, view, 0);
            if (bool)
              a((Rs)view, paramString); 
          } 
        }  
      int i = paramRs.ia;
      if (i != -1)
        a((Rs)findViewWithTag(Integer.valueOf(i)), paramString); 
      i = paramRs.ja;
      if (i != -1)
        a((Rs)findViewWithTag(Integer.valueOf(i)), paramString); 
      i = paramRs.ha;
      if (i != -1)
        paramRs = (Rs)findViewWithTag(Integer.valueOf(i)); 
    } 
  }
  
  public void a(Rs paramRs, ArrayList<BlockBean> paramArrayList) {
    if (paramArrayList.size() <= 0) {
      a(paramRs);
      return;
    }
    BlockBean first = paramArrayList.get(0);
    BlockBean last = paramArrayList.get(paramArrayList.size() - 1);
    String firstType = first.type;
    boolean isArgType = "b".equals(firstType) || "s".equals(firstType) || "d".equals(firstType)
        || "v".equals(firstType) || "p".equals(firstType) || "l".equals(firstType) || "a".equals(firstType);
    boolean endsWithF = "f".equals(last.type);
    boolean hasEmptySubStack = ("c".equals(first.type) || "e".equals(first.type)) && first.subStack1 <= 0;
    a(first.id, endsWithF, hasEmptySubStack, isArgType, paramRs.getHeight(), paramRs.f());
    this.f = null;
  }
  
  public final void a(Rs paramRs, boolean paramBoolean) {
    while (paramRs.getVisibility() != 8) {
      if (!paramRs.ga && (!paramBoolean || -1 == paramRs.ha)) {
        int[] arrayOfInt = new int[2];
        paramRs.getLocationOnScreen(arrayOfInt);
        arrayOfInt[1] = arrayOfInt[1] + paramRs.d();
        a(arrayOfInt, (View)paramRs, 0);
      } 
      if (paramRs.b() && (!paramBoolean || paramRs.ia == -1)) {
        int[] arrayOfInt = new int[2];
        paramRs.getLocationOnScreen(arrayOfInt);
        arrayOfInt[0] = arrayOfInt[0] + ((Ts)paramRs).j;
        arrayOfInt[1] = arrayOfInt[1] + paramRs.f();
        a(arrayOfInt, (View)paramRs, 2);
      } 
      if (paramRs.c() && (!paramBoolean || paramRs.ja == -1)) {
        int[] arrayOfInt = new int[2];
        paramRs.getLocationOnScreen(arrayOfInt);
        arrayOfInt[0] = arrayOfInt[0] + ((Ts)paramRs).j;
        arrayOfInt[1] = arrayOfInt[1] + paramRs.g();
        a(arrayOfInt, (View)paramRs, 3);
      } 
      int i = paramRs.ia;
      if (i != -1)
        a((Rs)findViewWithTag(Integer.valueOf(i)), paramBoolean); 
      i = paramRs.ja;
      if (i != -1)
        a((Rs)findViewWithTag(Integer.valueOf(i)), paramBoolean); 
      i = paramRs.ha;
      if (i != -1) {
        paramRs = (Rs)findViewWithTag(Integer.valueOf(i));
        continue;
      } 
      break;
    } 
  }
  
  public final void a(Context paramContext) {
    this.a = paramContext;
    a();
  }
  
  public void a(BlockBean paramBlockBean, boolean paramBoolean) {
    String str = paramBlockBean.id;
    if (str != null && !str.equals("") && !paramBlockBean.id.equals("0")) {
      Rs rs1 = (Rs)findViewWithTag(Integer.valueOf(paramBlockBean.id));
      if (rs1 == null)
        return; 
      Rs rs2 = ((Ts)rs1).E;
      if (rs1 != rs2) {
        c(rs1);
        removeView((View)rs1);
      } else {
        removeView((View)rs1);
      } 
      if (paramBoolean && rs2 != null)
        rs2.p().k(); 
    } 
  }
  
  public void a(String paramString1, String paramString2) {
    this.d = new Rs(getContext(), 0, paramString1, "h", paramString2);
    Rs rs = this.d;
    rs.pa = this;
    addView((View)rs);
    float f = wB.a(getContext(), 1.0F);
    rs = this.d;
    f *= 8.0F;
    rs.setX(f);
    this.d.setY(f);
  }
  
  public void a(String paramString, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3, int paramInt1, int paramInt2) {
    this.e = new ArrayList();
    int i = (int)(this.h * 3.0F);
    for (byte b = 0; b < getChildCount(); b++) {
      View view = getChildAt(b);
      if (view instanceof Rs) {
        Rs rs = (Rs)view;
        if (rs.getVisibility() != 8 && ((Ts)rs).E == null)
          if (paramBoolean3) {
            a(rs, paramString);
          } else if (!rs.fa) {
            boolean bool = true;
            if (!paramBoolean1 && !rs.ea) {
              int[] arrayOfInt = new int[2];
              rs.getLocationOnScreen(arrayOfInt);
              arrayOfInt[1] = arrayOfInt[1] - paramInt1 - i;
              a(arrayOfInt, (View)rs, 1);
            } 
            if (paramBoolean2 && !rs.ea) {
              int[] arrayOfInt = new int[2];
              rs.getLocationOnScreen(arrayOfInt);
              arrayOfInt[0] = arrayOfInt[0] - ((Ts)rs).j;
              arrayOfInt[1] = arrayOfInt[1] - paramInt2 - i;
              a(arrayOfInt, (View)rs, 4);
            } 
            if (!paramBoolean1 || paramBoolean2)
              bool = false; 
            a(rs, bool);
          }  
      } 
    } 
  }
  
  public final void a(int[] paramArrayOfint, View paramView, int paramInt) {
    this.e.add(new Object[] { paramArrayOfint, paramView, Integer.valueOf(paramInt) });
  }
  
  public final boolean a(Rs paramRs, View paramView) {
    if (!paramRs.fa)
      return true; 
    if (paramView instanceof Ts) {
      if (((Ts)paramView).c.equals("!"))
        return true; 
      Gx gx1 = paramRs.getClassInfo();
      if (gx1 == null)
        return false; 
      Gx gx2 = ((Ts)paramView).getClassInfo();
      if (gx2 == null)
        return false; 
      if (gx1.a(gx2))
        return true; 
      if (paramView instanceof Rs && gx1.a(mq.b(((Rs)paramView).ra)))
        return true; 
    } 
    return false;
  }
  
  public void b() {
    int i = getChildCount();
    int j = (getLayoutParams()).width;
    int k = (getLayoutParams()).width;
    byte b = 0;
    while (b < i) {
      View view = getChildAt(b);
      int m = j;
      int n = k;
      if (view instanceof Rs) {
        float f = view.getX();
        Rs rs = (Rs)view;
        m = Math.max((int)(f + rs.getWidthSum()) + 150, j);
        n = Math.max((int)(view.getY() + rs.getHeightSum()) + 150, k);
      } 
      b++;
      j = m;
      k = n;
    } 
    (getLayoutParams()).width = j;
    (getLayoutParams()).height = k;
  }
  
  public void b(Rs paramRs) {
    c(paramRs);
    Iterator<Rs> iterator = paramRs.getAllChildren().iterator();
    while (iterator.hasNext())
      removeView((View)iterator.next()); 
  }
  
  public boolean b(String paramString) {
    int i = getChildCount();
    for (byte b = 0; b < i; b++) {
      View view = getChildAt(b);
      if (view instanceof Rs) {
        byte b1;
        BlockBean blockBean = ((Rs)view).getBean();
        String str = blockBean.opCode;
        switch (str.hashCode()) {
          default:
            b1 = -1;
            break;
          case 2090189010:
            if (str.equals("addListStr")) {
              b1 = 8;
              break;
            } 
          case 2090182653:
            if (str.equals("addListMap")) {
              b1 = 20;
              break;
            } 
          case 2090179216:
            if (str.equals("addListInt")) {
              b1 = 7;
              break;
            } 
          case 1764351209:
            if (str.equals("deleteList")) {
              b1 = 13;
              break;
            } 
          case 1252547704:
            if (str.equals("listMapToStr")) {
              b1 = 6;
              break;
            } 
          case 1160674468:
            if (str.equals("lengthList")) {
              b1 = 1;
              break;
            } 
          case 762292097:
            if (str.equals("indexListStr")) {
              b1 = 12;
              break;
            } 
          case 762282303:
            if (str.equals("indexListInt")) {
              b1 = 11;
              break;
            } 
          case 389111867:
            if (str.equals("spnSetData")) {
              b1 = 14;
              break;
            } 
          case 134874756:
            if (str.equals("listSetCustomViewData")) {
              b1 = 16;
              break;
            } 
          case -96303809:
            if (str.equals("containListStr")) {
              b1 = 3;
              break;
            } 
          case -96310166:
            if (str.equals("containListMap")) {
              b1 = 4;
              break;
            } 
          case -96313603:
            if (str.equals("containListInt")) {
              b1 = 2;
              break;
            } 
          case -329552966:
            if (str.equals("insertListStr")) {
              b1 = 19;
              break;
            } 
          case -329559323:
            if (str.equals("insertListMap")) {
              b1 = 22;
              break;
            } 
          case -329562760:
            if (str.equals("insertListInt")) {
              b1 = 18;
              break;
            } 
          case -733318734:
            if (str.equals("strToListMap")) {
              b1 = 17;
              break;
            } 
          case -1139353316:
            if (str.equals("setListMap")) {
              b1 = 23;
              break;
            } 
          case -1249347599:
            if (str.equals("getVar")) {
              b1 = 0;
              break;
            } 
          case -1271141237:
            if (str.equals("clearList")) {
              b1 = 5;
              break;
            } 
          case -1384851894:
            if (str.equals("getAtListStr")) {
              b1 = 10;
              break;
            } 
          case -1384858251:
            if (str.equals("getAtListMap")) {
              b1 = 21;
              break;
            } 
          case -1384861688:
            if (str.equals("getAtListInt")) {
              b1 = 9;
              break;
            } 
          case -1998407506:
            if (str.equals("listSetData")) {
              b1 = 15;
              break;
            } 
        } 
        switch (b1) {
          case 22:
          case 23:
            if (((String)blockBean.parameters.get(3)).equals(paramString))
              return true; 
            break;
          case 18:
          case 19:
          case 20:
          case 21:
            if (((String)blockBean.parameters.get(2)).equals(paramString))
              return true; 
            break;
          case 7:
          case 8:
          case 9:
          case 10:
          case 11:
          case 12:
          case 13:
          case 14:
          case 15:
          case 16:
          case 17:
            if (((String)blockBean.parameters.get(1)).equals(paramString))
              return true; 
            break;
          case 1:
          case 2:
          case 3:
          case 4:
          case 5:
          case 6:
            if (((String)blockBean.parameters.get(0)).equals(paramString))
              return true; 
            break;
          case 0:
            if (blockBean.spec.equals(paramString))
              return true; 
            break;
        } 
      } 
    } 
    return false;
  }
  
  public Object[] b(Rs paramRs, int paramInt1, int paramInt2) {
    byte b;
    Object[] arrayOfObject;
    if (paramRs.fa) {
      b = 40;
    } else {
      b = 60;
    } 
    int i = 100000;
    Point point1 = null;
    Point point2 = new Point(paramInt1, paramInt2);
    paramInt1 = 0;
    paramInt2 = i;
    while (paramInt1 < this.e.size()) {
      Object[] arrayOfObject2;
      Object[] arrayOfObject1 = this.e.get(paramInt1);
      int[] arrayOfInt = (int[])arrayOfObject1[0];
      Point point = new Point(point2.x - arrayOfInt[0], point2.y - arrayOfInt[1]);
      int j = Math.abs(point.x / 2) + Math.abs(point.y);
      i = paramInt2;
      point = point1;
      if (j < paramInt2) {
        i = paramInt2;
        point = point1;
        if (j < b) {
          i = paramInt2;
          point = point1;
          if (a(paramRs, (View)arrayOfObject1[1])) {
            arrayOfObject2 = arrayOfObject1;
            i = j;
          } 
        } 
      } 
      paramInt1++;
      paramInt2 = i;
      arrayOfObject = arrayOfObject2;
    } 
    return arrayOfObject;
  }
  
  public void c() {
    d();
    this.e = new ArrayList();
    this.f = null;
  }
  
  public void c(Rs paramRs) {
    Rs rs = ((Ts)paramRs).E;
    if (rs == null)
      return; 
    if (rs != null) {
      rs.g(paramRs);
      ((Ts)paramRs).E = null;
    } 
  }
  
  public void c(Rs paramRs, int paramInt1, int paramInt2) {
    getLocationOnScreen(this.b);
    this.f = b(paramRs, paramInt1, paramInt2);
    boolean bool = paramRs.b();
    boolean bool1 = true;
    if (bool && -1 == paramRs.ia) {
      Object[] arrayOfObject1 = this.f;
      if (arrayOfObject1 != null) {
        Rs rs = (Rs)arrayOfObject1[1];
        paramInt1 = ((Integer)arrayOfObject1[2]).intValue();
        if (paramInt1 != 0) {
          if (paramInt1 != 2) {
            if (paramInt1 == 3)
              rs = (Rs)findViewWithTag(Integer.valueOf(rs.ja)); 
          } else {
            rs = (Rs)findViewWithTag(Integer.valueOf(rs.ia));
          } 
        } else {
          rs = (Rs)findViewWithTag(Integer.valueOf(rs.ha));
        } 
      } 
    } 
    Object[] arrayOfObject = this.f;
    if (arrayOfObject != null) {
      int[] arrayOfInt = (int[])arrayOfObject[0];
      View view = (View)arrayOfObject[1];
      this.c.setX((arrayOfInt[0] - this.b[0]));
      this.c.setY((arrayOfInt[1] - this.b[1]));
      this.c.bringToFront();
      this.c.setVisibility(0);
      if (paramRs.fa) {
        if (view instanceof Rs)
          this.c.a((Ts)view, true, false, 0); 
        if (view instanceof a.a.a.Ss)
          this.c.a((Ts)view, true, false, 0); 
      } else {
        paramInt2 = ((Integer)this.f[2]).intValue();
        if (paramInt2 == 4) {
          paramInt1 = ((Rs)view).getHeightSum();
        } else {
          paramInt1 = 0;
        } 
        if (paramInt2 == 1 || paramInt2 == 4)
          bool1 = false; 
        this.c.a((Ts)paramRs, false, bool1, paramInt1);
      } 
    } else {
      d();
    } 
  }
  
  public boolean c(String paramString) {
    int i = getChildCount();
    for (byte b = 0; b < i; b++) {
      View view = getChildAt(b);
      if (view instanceof Rs) {
        byte b1;
        BlockBean blockBean = ((Rs)view).getBean();
        String str = blockBean.opCode;
        switch (str.hashCode()) {
          default:
            b1 = -1;
            break;
          case 2090182653:
            if (str.equals("addListMap")) {
              b1 = 15;
              break;
            } 
          case 1775620400:
            if (str.equals("strToMap")) {
              b1 = 18;
              break;
            } 
          case 1431171391:
            if (str.equals("mapRemoveKey")) {
              b1 = 10;
              break;
            } 
          case 845089750:
            if (str.equals("setVarString")) {
              b1 = 3;
              break;
            } 
          case 836692861:
            if (str.equals("mapSize")) {
              b1 = 11;
              break;
            } 
          case 754442829:
            if (str.equals("increaseInt")) {
              b1 = 4;
              break;
            } 
          case 747168008:
            if (str.equals("mapCreateNew")) {
              b1 = 6;
              break;
            } 
          case 657721930:
            if (str.equals("setVarInt")) {
              b1 = 2;
              break;
            } 
          case 463560551:
            if (str.equals("mapContainKey")) {
              b1 = 9;
              break;
            } 
          case 442768763:
            if (str.equals("mapGetAllKeys")) {
              b1 = 14;
              break;
            } 
          case 168740282:
            if (str.equals("mapToStr")) {
              b1 = 17;
              break;
            } 
          case 152967761:
            if (str.equals("mapClear")) {
              b1 = 12;
              break;
            } 
          case -329559323:
            if (str.equals("insertListMap")) {
              b1 = 16;
              break;
            } 
          case -1081391085:
            if (str.equals("mapPut")) {
              b1 = 7;
              break;
            } 
          case -1081400230:
            if (str.equals("mapGet")) {
              b1 = 8;
              break;
            } 
          case -1249347599:
            if (str.equals("getVar")) {
              b1 = 0;
              break;
            } 
          case -1377080719:
            if (str.equals("decreaseInt")) {
              b1 = 5;
              break;
            } 
          case -1384858251:
            if (str.equals("getAtListMap")) {
              b1 = 19;
              break;
            } 
          case -1920517885:
            if (str.equals("setVarBoolean")) {
              b1 = 1;
              break;
            } 
          case -2120571577:
            if (str.equals("mapIsEmpty")) {
              b1 = 13;
              break;
            } 
        } 
        switch (b1) {
          case 19:
            if (((String)blockBean.parameters.get(2)).equals(paramString))
              return true; 
            break;
          case 18:
            if (((String)blockBean.parameters.get(1)).equals(paramString))
              return true; 
            break;
          case 1:
          case 2:
          case 3:
          case 4:
          case 5:
          case 6:
          case 7:
          case 8:
          case 9:
          case 10:
          case 11:
          case 12:
          case 13:
          case 14:
          case 15:
          case 16:
          case 17:
            if (((String)blockBean.parameters.get(0)).equals(paramString))
              return true; 
            break;
          case 0:
            if (blockBean.spec.equals(paramString))
              return true; 
            break;
        } 
      } 
    } 
    return false;
  }
  
  public void d() {
    this.c.setVisibility(8);
  }
  
  public int getAddTargetId() {
    Object[] target = getNearestTarget();
    int result = -1;
    if (target == null || target[2] == null) return result;
    int type = ((Integer) target[2]).intValue();
    if (type != 0 && type != 2 && type != 3 && type != 5) return result;
    if (target[1] == null) return result;
    View view = (View) target[1];
    if (view instanceof Rs) {
      Rs rs = (Rs) view;
      if (rs.fa) {
        result = ((Integer) rs.E.getTag()).intValue();
      } else {
        result = ((Integer) ((Rs) target[1]).getTag()).intValue();
      }
    }
    if (view instanceof a.a.a.Ss) {
      result = ((Integer) ((a.a.a.Ss) view).E.getTag()).intValue();
    }
    return result;
  }
  
  public ArrayList<BlockBean> getBlocks() {
    ArrayList<BlockBean> arrayList = new ArrayList();
    Rs rs = (Rs)findViewWithTag(Integer.valueOf(this.d.ha));
    if (rs != null) {
      Iterator<Rs> iterator = rs.getAllChildren().iterator();
      while (iterator.hasNext())
        arrayList.add(((Rs)iterator.next()).getBean()); 
    } 
    return arrayList;
  }
  
  public Object[] getNearestTarget() {
    return this.f;
  }
  
  public Rs getRoot() {
    return this.d;
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\com.besome.sketch-classes.jar!\com\besome\sketch\editor\logic\BlockPane.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */