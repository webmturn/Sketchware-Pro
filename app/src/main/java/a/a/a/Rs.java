package a.a.a;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.widget.TextView;
import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.editor.logic.BlockPane;
import java.util.ArrayList;
import java.util.Iterator;

public class Rs extends Ts {
  public String T;
  
  public String U;
  
  public ArrayList<View> V;
  
  public int W = 30;
  
  public int aa = 50;
  
  public int ba = 90;
  
  public int ca = 90;
  
  public int da = 4;
  
  public boolean ea = false;
  
  public boolean fa = false;
  
  public boolean ga = false;
  
  public int ha = -1;
  
  public int ia = -1;
  
  public int ja = -1;
  
  public ArrayList<View> ka = new ArrayList<View>();
  
  public ArrayList<String> la = new ArrayList<String>();
  
  public TextView ma = null;
  
  public int na = 0;
  
  public int oa = 0;
  
  public BlockPane pa = null;
  
  public String qa;
  
  public String ra;
  
  private String spec2 = "";
  
  public Rs(Context paramContext, int paramInt, String paramString1, String paramString2, String paramString3) {
    super(paramContext, paramString2, false);
    setTag(Integer.valueOf(paramInt));
    this.T = paramString1;
    this.U = paramString3;
    l();
  }
  
  public Rs(Context paramContext, int paramInt, String paramString1, String paramString2, String paramString3, String paramString4) {
    super(paramContext, paramString2, paramString3, false);
    setTag(Integer.valueOf(paramInt));
    this.T = paramString1;
    this.U = paramString4;
    l();
  }
  
  public final int a(TextView paramTextView) {
    Rect rect = new Rect();
    paramTextView.getPaint().getTextBounds(paramTextView.getText().toString(), 0, paramTextView.getText().length(), rect);
    return rect.width();
  }
  
  public final TextView a(String paramString) {
    TextView textView = new TextView(this.a);
    String text = paramString;
    if (this.U.equals("getVar") || this.U.equals("getArg")) {
      String prefix = this.c;
      if (prefix != null && prefix.length() > 0) {
        text = this.c + " : " + paramString;
      }
    }
    textView.setText(text);
    textView.setTextSize(10.0f);
    textView.setPadding(0, 0, 0, 0);
    textView.setGravity(16);
    textView.setTextColor(-1);
    textView.setTypeface(null, 1);
    android.widget.RelativeLayout.LayoutParams lp = new android.widget.RelativeLayout.LayoutParams(-2, this.G);
    lp.setMargins(0, 0, 0, 0);
    textView.setLayoutParams(lp);
    return textView;
  }
  
  public final void a(Rs paramRs) {
    if (paramRs == this) return;
    if (b() && -1 == this.ia) {
      e(paramRs);
    } else {
      Rs rs = h();
      if (rs != paramRs) {
        rs.ha = ((Integer)paramRs.getTag()).intValue();
        paramRs.E = rs;
      }
    } 
  }
  
  public void a(Ts paramTs, Rs paramRs) {
    int index = this.ka.indexOf(paramTs);
    if (index < 0) return;
    boolean isRs = paramTs instanceof Rs;
    if (isRs) {
      Rs oldRs = (Rs) paramTs;
      paramRs.qa = oldRs.qa;
      paramRs.ra = oldRs.ra;
    } else if (paramTs instanceof Ss) {
      paramRs.qa = paramTs.b;
      paramRs.ra = paramTs.c;
    }
    if (!isRs) {
      removeView(paramTs);
    }
    this.ka.set(index, paramRs);
    paramRs.E = this;
    i();
    o();
    if (paramTs != paramRs && isRs) {
      ((Rs) paramTs).E = null;
      paramTs.setX(getX() + getWidthSum() + 10.0f);
      paramTs.setY(getY() + 5.0f);
      ((Rs) paramTs).k();
    }
  }
  
  public final void a(String paramString, int paramInt) {
    ArrayList<String> arrayList = FB.c(paramString);
    this.ka = new ArrayList<View>();
    this.la = new ArrayList<String>();
    for (int b = 0; b < arrayList.size(); b++) {
      View view = b(arrayList.get(b), paramInt);
      if (view instanceof Ts)
        ((Ts)view).E = this; 
      this.ka.add(view);
      if (view instanceof Ss) {
        paramString = arrayList.get(b);
      } else {
        paramString = "icon";
      } 
      if (view instanceof TextView)
        paramString = "label"; 
      this.la.add(paramString);
    } 
  }
  
  public final View b(String paramString, int paramInt) {
    if (paramString.length() >= 2 && paramString.charAt(0) == '%') {
      int type = paramString.charAt(1);
      if (type == 98) {
        return new Ss(this.a, "b", "");
      } else if (type == 100) {
        return new Ss(this.a, "d", "");
      } else if (type == 109) {
        return new Ss(this.a, "m", paramString.substring(3));
      } else if (type == 115) {
        String str = "";
        if (paramString.length() > 2)
          str = paramString.substring(3);
        return new Ss(this.a, "s", str);
      }
      return a(FB.d(paramString));
    }
    return a(FB.d(paramString));
  }
  
  public void b(Rs paramRs) {
    if (paramRs == this) return;
    View view = this.pa.findViewWithTag(Integer.valueOf(this.ha));
    if (view != null)
      ((Rs)view).E = null; 
    paramRs.E = this;
    this.ha = ((Integer)paramRs.getTag()).intValue();
    if (view != null && view != paramRs)
      paramRs.a((Rs)view); 
  }
  
  public void c(Rs paramRs) {
    paramRs.setX(getX());
    paramRs.setY(getY() - paramRs.getHeightSum() + this.h);
    paramRs.h().b(this);
  }
  
  public void d(Rs paramRs) {
    if (paramRs == this) return;
    paramRs.setX(getX() - this.j);
    paramRs.setY(getY() - f());
    this.E = paramRs;
    paramRs.ia = ((Integer)getTag()).intValue();
  }
  
  public void e(Rs paramRs) {
    if (paramRs == this) return;
    View view = this.pa.findViewWithTag(Integer.valueOf(this.ia));
    if (view != null)
      ((Rs)view).E = null; 
    paramRs.E = this;
    this.ia = ((Integer)paramRs.getTag()).intValue();
    if (view != null && view != paramRs)
      paramRs.a((Rs)view); 
  }
  
  public void f(Rs paramRs) {
    if (paramRs == this) return;
    View view = this.pa.findViewWithTag(Integer.valueOf(this.ja));
    if (view != null)
      ((Rs)view).E = null; 
    paramRs.E = this;
    this.ja = ((Integer)paramRs.getTag()).intValue();
    if (view != null && view != paramRs)
      paramRs.a((Rs)view); 
  }
  
  public void g(Rs paramRs) {
    if (this.ha == ((Integer)paramRs.getTag()).intValue())
      this.ha = -1; 
    if (this.ia == ((Integer)paramRs.getTag()).intValue())
      this.ia = -1; 
    if (this.ja == ((Integer)paramRs.getTag()).intValue())
      this.ja = -1; 
    if (paramRs.fa) {
      int i = this.ka.indexOf(paramRs);
      if (i < 0)
        return; 
      paramRs.qa = "";
      paramRs.ra = "";
      View view = b(this.la.get(i), this.e);
      if (view instanceof Ts)
        ((Ts)view).E = this; 
      this.ka.set(i, view);
      addView(view);
      i();
      o();
    } 
    p().k();
  }
  
  public ArrayList<Rs> getAllChildren() {
    ArrayList<Rs> arrayList = new ArrayList<>();
    Rs rs = this;
    int limit = 200;
    while (true) {
      arrayList.add(rs);
      for (View view : rs.ka) {
        if (view instanceof Rs)
          arrayList.addAll(((Rs)view).getAllChildren()); 
      } 
      if (rs.b()) {
        int j = rs.ia;
        if (j != -1) {
          Rs sub = (Rs)this.pa.findViewWithTag(Integer.valueOf(j));
          if (sub != null && sub != this)
            arrayList.addAll(sub.getAllChildren()); 
        }
      } 
      if (rs.c()) {
        int j = rs.ja;
        if (j != -1) {
          Rs sub = (Rs)this.pa.findViewWithTag(Integer.valueOf(j));
          if (sub != null && sub != this)
            arrayList.addAll(sub.getAllChildren()); 
        }
      } 
      int i = rs.ha;
      if (i != -1 && --limit > 0) {
        Rs next = (Rs)this.pa.findViewWithTag(Integer.valueOf(i));
        if (next == null || next == this) return arrayList;
        rs = next;
        continue;
      } 
      return arrayList;
    } 
  }
  
  public BlockBean getBean() {
    BlockBean blockBean = new BlockBean(getTag().toString(), this.T, this.b, this.c, this.U);
    blockBean.color = this.e;
    for (View view : this.V) {
      if (view instanceof Ss) {
        blockBean.parameters.add(((Ss) view).getArgValue().toString());
      } else if (view instanceof Rs) {
        blockBean.parameters.add("@" + view.getTag().toString());
      }
    } 
    blockBean.subStack1 = this.ia;
    blockBean.subStack2 = this.ja;
    blockBean.nextBlock = this.ha;
    return blockBean;
  }
  
  public int getBlockType() {
    return this.oa;
  }
  
  public int getDepth() {
    int b = 0;
    Rs rs = this;
    int limit = 500;
    while (true) {
      rs = rs.E;
      if (rs != null && --limit > 0) {
        b++;
        continue;
      } 
      return b;
    } 
  }
  public int getHeightSum() {
    int i = 0;
    Rs rs = this;
    int limit = 200;
    while (true) {
      int j = i;
      if (i != 0)
        j = i - this.h; 
      i = j + rs.getTotalHeight();
      j = rs.ha;
      if (j != -1 && --limit > 0) {
        Rs next = (Rs)this.pa.findViewWithTag(Integer.valueOf(j));
        if (next == null || next == this) break;
        rs = next;
        continue;
      } 
      return i;
    }
    return i;
  }
  
  public int getWidthSum() {
    int i = 0;
    Rs rs = this;
    int limit = 200;
    while (true) {
      i = Math.max(i, rs.getW());
      int j = i;
      if (rs.b()) {
        int k = rs.ia;
        j = i;
        if (k != -1) {
          Rs sub1 = (Rs)this.pa.findViewWithTag(Integer.valueOf(k));
          if (sub1 != null) {
            j = this.j;
            j = Math.max(i, sub1.getWidthSum() + j);
          }
        } 
      } 
      i = j;
      if (rs.c()) {
        int k = rs.ja;
        i = j;
        if (k != -1) {
          Rs sub2 = (Rs)this.pa.findViewWithTag(Integer.valueOf(k));
          if (sub2 != null) {
            i = this.j;
            i = Math.max(j, sub2.getWidthSum() + i);
          }
        } 
      } 
      j = rs.ha;
      if (j != -1 && --limit > 0) {
        Rs next = (Rs)this.pa.findViewWithTag(Integer.valueOf(j));
        if (next == null || next == this) break;
        rs = next;
        continue;
      } 
      return i;
    }
    return i;
  }
  
  public Rs h() {
    Rs rs = this;
    int limit = 200;
    while (true) {
      int i = rs.ha;
      if (i != -1 && --limit > 0) {
        Rs next = (Rs)this.pa.findViewWithTag(Integer.valueOf(i));
        if (next == null || next == this) return rs;
        rs = next;
        continue;
      } 
      return rs;
    } 
  }
  
  public final void i() {
    this.V = new ArrayList<View>();
    for (int b = 0; b < this.ka.size(); b++) {
      View view = this.ka.get(b);
      if (view instanceof Rs || view instanceof Ss)
        this.V.add(view); 
    } 
  }
  
  public final void j() {
    TextView textView = this.ma;
    if (textView != null) {
      textView.bringToFront();
      this.ma.setX(this.w);
      this.ma.setY((g() - this.n));
    } 
  }
  
  public void k() {
    Rs current = this;
    int limit = 1000;
    while (current != null && --limit > 0) {
      current.kSingle();
      int next = current.ha;
      if (next > -1) {
        Rs nextRs = (Rs) current.pa.findViewWithTag(Integer.valueOf(next));
        if (nextRs == null || nextRs == this) break;
        nextRs.setX(current.getX());
        nextRs.setY(current.getY() + (float) current.d());
        nextRs.bringToFront();
        current = nextRs;
      } else {
        current = null;
      }
    }
  }

  private void kSingle() {
    bringToFront();
    int xOffset = this.w;
    for (int idx = 0; idx < this.ka.size(); idx++) {
      View child = (View) this.ka.get(idx);
      child.bringToFront();
      boolean isRs = child instanceof Rs;
      float childX;
      if (isRs) {
        childX = getX() + (float) xOffset;
      } else {
        childX = (float) xOffset;
      }
      child.setX(childX);
      int childWidth;
      if (((String) this.la.get(idx)).equals("label")) {
        childWidth = a((TextView) child);
      } else {
        childWidth = 0;
      }
      if (child instanceof Ss) {
        childWidth = ((Ss) child).getW();
      }
      if (isRs) {
        childWidth = ((Rs) child).getWidthSum();
      }
      xOffset += childWidth + this.da;
      if (isRs) {
        Rs childRs = (Rs) child;
        child.setY(getY() + (float) this.u + (float) ((this.na - childRs.na - 1) * this.y));
        childRs.k();
      } else {
        child.setY((float) (this.u + this.na * this.y));
      }
    }
    int w = xOffset;
    if (this.b.equals("b") || this.b.equals("d") || this.b.equals("s") || this.b.equals("a")) {
      w = Math.max(xOffset, this.W);
    }
    int w2 = w;
    if (this.b.equals(" ") || this.b.equals("") || this.b.equals("f")) {
      w2 = Math.max(w, this.aa);
    }
    int w3 = w2;
    if (this.b.equals("c") || this.b.equals("e")) {
      w3 = Math.max(w2, this.ca);
    }
    int w4 = w3;
    if (this.b.equals("h")) {
      w4 = Math.max(w3, this.ba);
    }
    a((float) (this.x + w4), (float) (this.u + this.G + this.na * this.y * 2 + this.v), true);
    if (b()) {
      int ss1Height = this.i;
      int sub1 = this.ia;
      if (sub1 > -1) {
        Rs sub1Rs = (Rs) this.pa.findViewWithTag(Integer.valueOf(sub1));
        if (sub1Rs != null) {
          sub1Rs.setX(getX() + (float) this.j);
          sub1Rs.setY(getY() + (float) f());
          sub1Rs.bringToFront();
          sub1Rs.k();
          ss1Height = sub1Rs.getHeightSum();
        }
      }
      setSubstack1Height(ss1Height);
      int ss2Height = this.i;
      int sub2 = this.ja;
      if (sub2 > -1) {
        Rs sub2Rs = (Rs) this.pa.findViewWithTag(Integer.valueOf(sub2));
        if (sub2Rs != null) {
          sub2Rs.setX(getX() + (float) this.j);
          sub2Rs.setY(getY() + (float) g());
          sub2Rs.bringToFront();
          sub2Rs.k();
          ss2Height = sub2Rs.getHeightSum();
          if (sub2Rs.h().ga) {
            ss2Height += this.h;
          }
        }
      }
      setSubstack2Height(ss2Height);
      j();
    }
  }
  
  public void l() {
    setDrawingCacheEnabled(false);
    float scale = this.D;
    this.W = (int) (this.W * scale);
    this.aa = (int) (this.aa * scale);
    this.ba = (int) (this.ba * scale);
    this.ca = (int) (this.ca * scale);
    this.da = (int) (this.da * scale);
    String type = this.b;
    byte typeNum = 0;
    if (type.equals("b")) { typeNum = 1; }
    else if (type.equals("s")) { typeNum = 2; }
    else if (type.equals("d")) { typeNum = 3; }
    else if (type.equals("v")) { typeNum = 4; }
    else if (type.equals("p")) { typeNum = 5; }
    else if (type.equals("l")) { typeNum = 6; }
    else if (type.equals("a")) { typeNum = 7; }
    else if (type.equals("c")) { typeNum = 8; }
    else if (type.equals("e")) { typeNum = 9; }
    else if (type.equals("f")) { typeNum = 10; }
    else if (type.equals("h")) { typeNum = 11; }
    else if (type.equals(" ")) { typeNum = 0; }
    else { typeNum = -1; }
    switch (typeNum) {
      case 1: case 2: case 3: case 4: case 5: case 6: case 7:
        this.fa = true;
        break;
      case 10:
        this.ga = true;
        break;
      case 11:
        this.ea = true;
        break;
      default:
        break;
    }
    int color = kq.a(this.U, this.b);
    if (!this.ea && !this.U.equals("definedFunc") && !this.U.equals("getVar")
        && !this.U.equals("getResStr") && !this.U.equals("getArg") && color != -7711273) {
      this.T = xB.b().a(getContext(), this.U);
    }
    if (color == -7711273) {
      mod.hey.studios.editor.manage.block.ExtraBlockInfo info = mod.hey.studios.editor.manage.block.v2.BlockLoader.getBlockInfo(this.U);
      mod.hey.studios.editor.manage.block.ExtraBlockInfo blockInfo = info;
      if (mod.hey.studios.project.ProjectTracker.SC_ID != null && info.isMissing
          && !mod.hey.studios.project.ProjectTracker.SC_ID.equals("")) {
        blockInfo = mod.hey.studios.editor.manage.block.v2.BlockLoader.getBlockFromProject(
            mod.hey.studios.project.ProjectTracker.SC_ID, this.U);
        android.util.Log.d("Rs", "Rs:returned block: " + new com.google.gson.Gson().toJson(blockInfo));
      }
      this.spec2 = blockInfo.getSpec2();
      if (this.T.equals("")) {
        this.T = blockInfo.getSpec();
      }
      if (this.T.equals("")) {
        this.T = this.U;
      }
      setSpec(this.T);
      int blockColor = blockInfo.getColor();
      int paletteColor = blockInfo.getPaletteColor();
      if (!this.U.equals("definedFunc") && !this.U.equals("getVar")
          && !this.U.equals("getResStr") && !this.U.equals("getArg")) {
        if (blockColor != 0) {
          this.e = blockColor;
          return;
        }
        if (paletteColor != 0) {
          this.e = paletteColor;
          return;
        }
      }
    } else {
      if (this.T.equals("")) {
        this.T = this.U;
      }
      setSpec(this.T);
    }
    this.e = color;
  }
  
  public void m() {
    Rs rs2;
    Rs rs1 = this;
    int count = 0;
    do {
      rs1.n();
      rs2 = rs1.E;
      count++;
      if (count > 500) {
        android.util.Log.e("Rs", "m() infinite loop detected! count=" + count + " tag=" + rs1.getTag() + " E.tag=" + (rs2 != null ? rs2.getTag() : "null"));
        break;
      }
      rs1 = rs2;
    } while (rs2 != null);
  }
  
  public void n() {
    int xOffset = this.w;
    for (int idx = 0; idx < this.ka.size(); idx++) {
      View child = (View) this.ka.get(idx);
      int childWidth;
      if (((String) this.la.get(idx)).equals("label")) {
        childWidth = a((TextView) child);
      } else {
        childWidth = 0;
      }
      if (child instanceof Ss) {
        childWidth = ((Ss) child).getW();
      }
      if (child instanceof Rs) {
        childWidth = ((Rs) child).getWidthSum();
      }
      xOffset += childWidth + this.da;
    }
    int w = xOffset;
    if (this.b.equals("b") || this.b.equals("d") || this.b.equals("s") || this.b.equals("a")) {
      w = Math.max(xOffset, this.W);
    }
    int w2 = w;
    if (this.b.equals(" ") || this.b.equals("") || this.b.equals("o")) {
      w2 = Math.max(w, this.aa);
    }
    int w3 = w2;
    if (this.b.equals("c") || this.b.equals("e")) {
      w3 = Math.max(w2, this.ca);
    }
    int w4 = w3;
    if (this.b.equals("h")) {
      w4 = Math.max(w3, this.ba);
    }
    int finalW = w4;
    TextView label = this.ma;
    if (label != null) {
      finalW = Math.max(w4, label.getWidth() + this.w + 2);
    }
    a((float) (this.x + finalW), (float) (this.u + this.G + this.na * this.y * 2 + this.v), false);
  }
  
  public void o() {
    Rs rs = this;
    int limit = 200;
    while (rs != null && --limit > 0) {
      Iterator<View> iterator = rs.V.iterator();
      int i = 0;
      while (iterator.hasNext()) {
        View view = iterator.next();
        if (view instanceof Rs)
          i = Math.max(i, ((Rs)view).na + 1); 
      } 
      rs.na = i;
      rs.n();
      if (rs.fa) {
        rs = rs.E;
        continue;
      } 
      break;
    } 
  }
  
  public Rs p() {
    Rs rs = this;
    int limit = 200;
    while (true) {
      Rs rs1 = rs.E;
      if (rs1 != null && --limit > 0) {
        rs = rs1;
        continue;
      } 
      return rs;
    } 
  }
  
  public void setBlockType(int paramInt) {
    this.oa = paramInt;
  }
  
  public void setSpec(String paramString) {
    this.T = paramString;
    removeAllViews();
    a(this.T, this.e);
    Iterator<View> iterator = this.ka.iterator();
    while (true) {
      if (!iterator.hasNext()) {
        i();
        if (this.b.equals("e") && this.U.equals("ifElse")) {
          this.ma = a(xB.b().a(getContext(), "else"));
          addView((View)this.ma);
        } 
        if (this.b.equals("e") && !this.spec2.equals("")) {
          this.ma = a(this.spec2);
          addView((View)this.ma);
        } 
        k();
        return;
      } 
      addView(iterator.next());
    } 
  }
}
