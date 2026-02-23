package a.a.a;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.widget.RelativeLayout;

public class BaseBlockView extends RelativeLayout {
  public int A;
  
  public int B;
  
  public int C;
  
  public float D;
  
  public BlockView E;
  
  public int F;
  
  public int G;
  
  public int H;
  
  public int I;
  
  public int J;
  
  public boolean K;
  
  public boolean L;
  
  public Paint M;
  
  public Paint N;
  
  public Paint O;
  
  public int P;
  
  public int Q;
  
  public int R;
  
  public Gx S;
  
  public Context a;
  
  public String b;
  
  public String c;
  
  public int d;
  
  public int e;
  
  public Paint f;
  
  public boolean g;
  
  public int h;
  
  public int i;
  
  public int j;
  
  public int k;
  
  public int l;
  
  public int m;
  
  public int n;
  
  public int o;
  
  public int p;
  
  public int q;
  
  public int r;
  
  public int s;
  
  public int t;
  
  public int u;
  
  public int v;
  
  public int w;
  
  public int x;
  
  public int y;
  
  public int z;
  
  public BaseBlockView(Context paramContext, String paramString1, String paramString2, boolean paramBoolean) {
    super(paramContext);
    this.h = 3;
    this.i = 12;
    this.j = 15;
    this.k = 3;
    this.l = 2;
    this.m = 15;
    this.n = 15;
    this.o = 15;
    this.p = this.o + this.h;
    this.q = this.p + 10;
    this.r = this.q + this.h;
    this.s = 6;
    this.t = 60;
    this.u = 2;
    this.v = 2;
    this.w = 3;
    this.x = 0;
    this.y = 2;
    this.B = this.i;
    this.C = this.i;
    this.E = null;
    this.F = 100;
    this.G = 14;
    this.H = 15;
    this.I = 6;
    this.J = 4;
    this.K = false;
    this.L = false;
    this.P = 1;
    this.Q = 1;
    this.R = 805306368;
    this.S = null;
    this.a = paramContext;
    this.b = paramString1;
    if (paramString2 != null && paramString2.indexOf(".") > 0) {
      this.c = paramString2.substring(0, paramString2.indexOf("."));
    } else {
      this.c = paramString2;
    }
    a();
    String type = this.b;
    switch (type) {
      case " ": this.u = 4; this.d = 4; break;
      case "b": this.w = 8; this.x = 5; this.d = 2; break;
      case "d": this.d = 3; this.w = 4; break;
      case "n": this.d = 3; break;
      case "c": this.u = 4; this.d = 10; break;
      case "e": this.u = 4; this.d = 12; break;
      case "f": this.u = 4; this.d = 5; break;
      case "h": this.u = 8; this.d = 7; break;
      case "m": this.d = 9; break;
      case "s":
      case "v":
      case "p":
      case "l":
      case "a": this.d = 1; break;
    }
    this.e = this.R;
    this.g = paramBoolean;
    setWillNotDraw(false);
    a(paramContext);
  }
  
  public BaseBlockView(Context paramContext, String paramString, boolean paramBoolean) {
    this(paramContext, paramString, "", paramBoolean);
  }
  
  private float[] getBooleanReflections() {
    int i = this.A / 2;
    int j = this.P;
    float f1 = (j / 2 + 0);
    float f2 = i;
    return new float[] { f1, f2, f2, (j / 2 + 0), f2, (j / 2 + 0), (this.z - i), (j / 2 + 0) };
  }
  
  private float[] getBooleanShadows() {
    int i = this.A;
    int j = i / 2;
    int k = this.z;
    int m = this.P;
    float f1 = (k - m / 2);
    float f2 = j;
    return new float[] { f1, f2, (k - j), (i - m / 2), (k - j), (i - m / 2), f2, (i - m / 2) };
  }
  
  private float[] getNumberBottomShadows() {
    int i = this.A;
    int j = i / 2;
    float f = (this.z - j);
    int k = this.P;
    return new float[] { f, (i - k / 2), j, (i - k / 2) };
  }
  
  private float[] getNumberTopReflections() {
    int i = this.A / 2;
    float f = i;
    int j = this.P;
    return new float[] { f, (j / 2 + 0), (this.z - i), (j / 2 + 0) };
  }
  
  private float[] getRectReflections() {
    int i = this.P;
    return new float[] { 0.0F, (i / 2 + 0), (this.z - i / 2), (i / 2 + 0), (i / 2 + 0), 0.0F, (i / 2 + 0), (this.A - i / 2) };
  }
  
  private float[] getRectShadows() {
    int i = this.z;
    int j = this.P;
    float f1 = (i - j / 2);
    float f2 = (i - j / 2);
    int k = this.A;
    return new float[] { f1, 0.0F, f2, (k - j / 2), (i - j / 2), (k - j / 2), 0.0F, (k - j / 2) };
  }
  
  public void a() {
    this.S = mq.a(this.b, this.c);
  }
  
  public void a(float paramFloat1, float paramFloat2, boolean paramBoolean) {
    if (this.d == 9) {
      this.z = (int)paramFloat1 + this.H;
    } else {
      this.z = (int)paramFloat1;
    } 
    this.A = (int)paramFloat2;
    if (paramBoolean)
      e(); 
  }
  
  public void a(BaseBlockView paramTs, boolean paramBoolean1, boolean paramBoolean2, int paramInt) {
    this.e = -16777216;
    this.d = paramTs.d;
    this.z = paramTs.z;
    this.A = paramTs.A;
    this.B = paramTs.B;
    this.C = paramTs.C;
    if (!paramBoolean1)
      if (paramBoolean2) {
        this.d = 4;
        this.A = (int)(this.D * 6.0F);
      } else if (paramInt > 0) {
        this.B = paramInt - this.h;
      }  
    e();
  }
  
  public final void a(Context paramContext) {
    this.D = ViewUtil.a(paramContext, 1.0F);
    float f1 = this.h;
    float f2 = this.D;
    this.h = (int)(f1 * f2);
    this.i = (int)(this.i * f2);
    this.j = (int)(this.j * f2);
    this.m = (int)(this.m * f2);
    this.n = (int)(this.n * f2);
    this.k = (int)(this.k * f2);
    this.l = (int)(this.l * f2);
    this.o = (int)(this.o * f2);
    this.p = (int)(this.p * f2);
    this.q = (int)(this.q * f2);
    this.r = (int)(this.r * f2);
    this.s = (int)(this.s * f2);
    this.t = (int)(this.t * f2);
    this.B = (int)(this.B * f2);
    this.C = (int)(this.C * f2);
    this.w = (int)(this.w * f2);
    this.u = (int)(this.u * f2);
    this.x = (int)(this.x * f2);
    this.v = (int)(this.v * f2);
    this.y = (int)(this.y * f2);
    this.F = (int)(this.F * f2);
    this.G = (int)(this.G * f2);
    this.I = (int)(this.I * f2);
    this.J = (int)(this.J * f2);
    this.H = (int)(this.H * f2);
    this.P = (int)(this.P * f2);
    this.Q = (int)(this.Q * f2);
    if (this.P < 2)
      this.P = 2; 
    if (this.Q < 2)
      this.Q = 2; 
    this.f = new Paint();
    if (!this.g) {
      this.K = true;
      this.L = true;
    } 
    this.M = new Paint();
    this.M.setColor(-536870912);
    this.M.setStrokeWidth(this.P);
    this.N = new Paint();
    this.N.setColor(-1610612736);
    this.N.setStyle(Paint.Style.STROKE);
    this.N.setStrokeWidth(this.P);
    this.O = new Paint();
    this.O.setColor(-1593835521);
    this.O.setStyle(Paint.Style.STROKE);
    this.O.setStrokeWidth(this.Q);
    setLayerType(1, null);
    a(this.F, (this.G + this.u + this.v), false);
  }
  
  public final void a(Canvas paramCanvas) {
    Path path = new Path();
    int i = this.A;
    int j = i / 2;
    float f = j;
    path.moveTo(f, i);
    path.lineTo(0.0F, f);
    path.lineTo(f, 0.0F);
    path.lineTo((this.z - j), 0.0F);
    path.lineTo(this.z, f);
    path.lineTo((this.z - j), this.A);
    paramCanvas.drawPath(path, this.f);
    if (this.K)
      paramCanvas.drawLines(getBooleanShadows(), this.N); 
    if (this.L)
      paramCanvas.drawLines(getBooleanReflections(), this.O); 
  }
  
  public final void a(Path paramPath) {
    paramPath.moveTo(0.0F, this.k);
    paramPath.lineTo(this.k, 0.0F);
    paramPath.lineTo(this.o, 0.0F);
    paramPath.lineTo(this.p, this.h);
    paramPath.lineTo(this.q, this.h);
    paramPath.lineTo(this.r, 0.0F);
    paramPath.lineTo((this.z - this.k), 0.0F);
    paramPath.lineTo(this.z, this.k);
  }
  
  public final void a(Path paramPath, int paramInt) {
    paramPath.lineTo(this.j, (paramInt - this.l));
    float f1 = (this.j + this.l);
    float f2 = paramInt;
    paramPath.lineTo(f1, f2);
    paramPath.lineTo((this.z - this.k), f2);
    paramPath.lineTo(this.z, (paramInt + this.k));
  }
  
  public final void a(Path paramPath, int paramInt1, boolean paramBoolean, int paramInt2) {
    paramPath.lineTo(this.z, (paramInt1 - this.k));
    float f1 = (this.z - this.k);
    float f2 = paramInt1;
    paramPath.lineTo(f1, f2);
    if (paramBoolean) {
      paramPath.lineTo((this.r + paramInt2), f2);
      paramPath.lineTo((this.q + paramInt2), (this.h + paramInt1));
      paramPath.lineTo((this.p + paramInt2), (this.h + paramInt1));
      paramPath.lineTo((this.o + paramInt2), f2);
    } 
    if (paramInt2 > 0) {
      paramPath.lineTo((this.l + paramInt2), f2);
      paramPath.lineTo(paramInt2, (paramInt1 + this.l));
    } else {
      paramPath.lineTo((paramInt2 + this.k), f2);
      paramPath.lineTo(0.0F, (paramInt1 - this.k));
    } 
  }
  
  public final float[] a(int paramInt) {
    int i = this.P;
    float f1 = (i / 2 + 0);
    int j = this.k;
    float f2 = (paramInt - j);
    float f3 = (i / 2 + 0);
    float f4 = j;
    float f5 = (i / 2 + 0);
    float f6 = j;
    float f7 = j;
    float f8 = (i / 2 + 0);
    float f9 = j;
    float f10 = (i / 2 + 0);
    float f11 = this.o;
    float f12 = (i / 2 + 0);
    float f13 = this.p;
    paramInt = this.h;
    float f14 = (i / 2 + paramInt);
    int k = this.q;
    float f15 = k;
    float f16 = (i / 2 + paramInt);
    float f17 = k;
    float f18 = (paramInt + i / 2);
    paramInt = this.r;
    return new float[] { 
        f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, 
        f11, f12, f13, f14, f15, f16, f17, f18, paramInt, (i / 2 + 0), 
        paramInt, (i / 2 + 0), (this.z - j), (i / 2 + 0) };
  }
  
  public final float[] a(int paramInt1, int paramInt2) {
    int i = this.j;
    int j = this.l;
    float f = (i + j);
    int k = this.P;
    return new float[] { f, (paramInt1 - k / 2), (i - k / 2), (paramInt1 + j), (i - k / 2), (paramInt1 + j), (i - k / 2), (paramInt2 - j) };
  }
  
  public final float[] a(int paramInt1, boolean paramBoolean, int paramInt2) {
    float[] arrayOfFloat;
    if (paramBoolean) {
      arrayOfFloat = new float[24];
    } else {
      arrayOfFloat = new float[8];
    } 
    int i = this.z;
    arrayOfFloat[0] = i;
    int j = this.k;
    int k = this.P;
    arrayOfFloat[1] = (paramInt1 - j - k / 2);
    arrayOfFloat[2] = (i - j);
    arrayOfFloat[3] = (paramInt1 - k / 2);
    if (paramBoolean) {
      arrayOfFloat[4] = (i - j);
      arrayOfFloat[5] = (paramInt1 - k / 2);
      i = this.r;
      arrayOfFloat[6] = (paramInt2 + i);
      arrayOfFloat[7] = (paramInt1 - k / 2);
      arrayOfFloat[8] = (i + paramInt2);
      arrayOfFloat[9] = (paramInt1 - k / 2);
      int m = this.q;
      arrayOfFloat[10] = (paramInt2 + m);
      i = this.h;
      arrayOfFloat[11] = (paramInt1 + i - k / 2);
      arrayOfFloat[12] = (m + paramInt2);
      arrayOfFloat[13] = (paramInt1 + i - k / 2);
      m = this.p;
      arrayOfFloat[14] = (paramInt2 + m);
      arrayOfFloat[15] = (paramInt1 + i - k / 2);
      arrayOfFloat[16] = (m + paramInt2);
      arrayOfFloat[17] = (i + paramInt1 - k / 2);
      i = this.o;
      arrayOfFloat[18] = (paramInt2 + i);
      arrayOfFloat[19] = (paramInt1 - k / 2);
      if (paramInt2 > 0) {
        arrayOfFloat[20] = (i + paramInt2);
        arrayOfFloat[21] = (paramInt1 - k / 2);
        arrayOfFloat[22] = (paramInt2 + this.l);
        arrayOfFloat[23] = (paramInt1 - k / 2);
      } else {
        arrayOfFloat[20] = (i + paramInt2);
        arrayOfFloat[21] = (paramInt1 - k / 2);
        arrayOfFloat[22] = (paramInt2 + j);
        arrayOfFloat[23] = (paramInt1 - k / 2);
      } 
    } else if (paramInt2 > 0) {
      arrayOfFloat[4] = (i - j);
      arrayOfFloat[5] = (paramInt1 - k / 2);
      arrayOfFloat[6] = (paramInt2 + this.l);
      arrayOfFloat[7] = (paramInt1 - k / 2);
    } else {
      arrayOfFloat[4] = (i - j);
      arrayOfFloat[5] = (paramInt1 - k / 2);
      arrayOfFloat[6] = (paramInt2 + j);
      arrayOfFloat[7] = (paramInt1 - k / 2);
    } 
    return arrayOfFloat;
  }
  
  public final void b(Canvas paramCanvas) {
    boolean bool2;
    Path path = new Path();
    a(path);
    int i = this.A;
    int j = this.d;
    boolean bool1 = true;
    if (j != 5) {
      bool2 = true;
    } else {
      bool2 = false;
    } 
    a(path, i, bool2, 0);
    paramCanvas.drawPath(path, this.f);
    if (this.K) {
      paramCanvas.drawLines(b(0, this.A), this.N);
      i = this.A;
      if (this.d != 5) {
        bool2 = bool1;
      } else {
        bool2 = false;
      } 
      paramCanvas.drawLines(a(i, bool2, 0), this.N);
    } 
    if (this.L)
      paramCanvas.drawLines(a(this.A), this.O); 
  }
  
  public boolean b() {
    boolean bool;
    if (this.d >= 10) {
      bool = true;
    } else {
      bool = false;
    } 
    return bool;
  }
  
  public final float[] b(int paramInt1, int paramInt2) {
    int i = this.z;
    int j = this.P;
    float f = (i - j / 2);
    int k = this.k;
    return new float[] { f, (paramInt1 + k), (i - j / 2), (paramInt2 - k) };
  }
  
  public final void c(Canvas paramCanvas) {
    paramCanvas.drawRect(new Rect(0, 0, this.z, this.A), this.f);
    Path path = new Path();
    int i = this.z;
    int j = this.J;
    path.moveTo((i - j), j);
    j = this.z;
    i = this.J;
    int k = this.I;
    path.lineTo((j - i - k / 2), (i + k));
    j = this.z;
    i = this.J;
    path.lineTo((j - i - this.I), i);
    paramCanvas.drawPath(path, this.M);
  }
  
  public boolean c() {
    boolean bool;
    if (this.d == 12) {
      bool = true;
    } else {
      bool = false;
    } 
    return bool;
  }
  
  public final float[] c(int paramInt1, int paramInt2) {
    float f = (paramInt2 + this.l);
    paramInt2 = this.P;
    return new float[] { f, (paramInt2 / 2 + paramInt1), (this.z - this.k), (paramInt1 + paramInt2 / 2) };
  }
  
  public int d() {
    return getTotalHeight() - this.h;
  }
  
  public final void d(Canvas paramCanvas) {
    Path path = new Path();
    path.moveTo(0.0F, this.s);
    path.arcTo(new RectF(0.0F, 0.0F, this.t, (this.s * 2)), 180.0F, 180.0F);
    path.lineTo((this.z - this.k), this.s);
    path.lineTo(this.z, (this.s + this.k));
    a(path, this.A, true, 0);
    paramCanvas.drawPath(path, this.f);
    if (this.K) {
      paramCanvas.drawLines(b(this.s, this.A), this.N);
      paramCanvas.drawLines(a(this.A, true, 0), this.N);
    } 
  }
  
  public void e() {
    requestLayout();
  }
  
  public final void e(Canvas paramCanvas) {
    Path path = new Path();
    int i = this.A;
    int j = this.B;
    int k = this.h;
    i = i + j - k;
    k = this.n + i + this.C - k;
    a(path);
    a(path, this.A, true, this.j);
    a(path, i);
    a(path, this.n + i, true, this.j);
    a(path, k);
    a(path, this.m + k, true, 0);
    paramCanvas.drawPath(path, this.f);
    if (this.K) {
      paramCanvas.drawLines(b(0, this.A), this.N);
      paramCanvas.drawLines(a(this.A, true, this.j), this.N);
      paramCanvas.drawLines(a(this.A, i), this.N);
      paramCanvas.drawLines(b(i, this.n + i), this.N);
      paramCanvas.drawLines(a(this.n + i, true, this.j), this.N);
      paramCanvas.drawLines(a(this.n + i, k), this.N);
      paramCanvas.drawLines(b(k, this.m + k), this.N);
      paramCanvas.drawLines(a(this.m + k, true, 0), this.N);
    } 
    if (this.L) {
      paramCanvas.drawLines(a(this.m + k), this.O);
      paramCanvas.drawLines(c(i, this.j), this.O);
      paramCanvas.drawLines(c(k, this.j), this.O);
    } 
  }
  
  public int f() {
    return this.A;
  }
  
  public final void f(Canvas paramCanvas) {
    boolean bool2;
    Path path = new Path();
    int i = this.A + this.B - this.h;
    a(path);
    int j = this.A;
    int k = this.j;
    boolean bool1 = true;
    a(path, j, true, k);
    a(path, i);
    j = this.m;
    if (this.d == 10) {
      bool2 = true;
    } else {
      bool2 = false;
    } 
    a(path, j + i, bool2, 0);
    paramCanvas.drawPath(path, this.f);
    if (this.K) {
      paramCanvas.drawLines(b(0, this.A), this.N);
      paramCanvas.drawLines(a(this.A, true, this.j), this.N);
      paramCanvas.drawLines(a(this.A, i), this.N);
      paramCanvas.drawLines(b(i, this.m + i), this.N);
      j = this.m;
      if (this.d == 10) {
        bool2 = bool1;
      } else {
        bool2 = false;
      } 
      paramCanvas.drawLines(a(j + i, bool2, 0), this.N);
    } 
    if (this.L) {
      paramCanvas.drawLines(a(this.m + i), this.O);
      paramCanvas.drawLines(c(i, this.j), this.O);
    } 
  }
  
  public int g() {
    return this.A + this.B + this.n - this.h;
  }
  
  public final void g(Canvas paramCanvas) {
    Path path = new Path();
    int i = this.A;
    int j = i / 2;
    path.moveTo(j, i);
    i = this.A;
    path.arcTo(new RectF(0.0F, 0.0F, i, i), 90.0F, 180.0F);
    path.lineTo((this.z - j), 0.0F);
    j = this.z;
    i = this.A;
    path.arcTo(new RectF((j - i), 0.0F, j, i), 270.0F, 180.0F);
    paramCanvas.drawPath(path, this.f);
    if (this.K) {
      i = this.z;
      j = this.A;
      float f = (i - j);
      int k = this.P;
      paramCanvas.drawArc(new RectF(f, 0.0F, (i - k / 2), (j - k / 2)), 330.0F, 120.0F, false, this.N);
      paramCanvas.drawLines(getNumberBottomShadows(), this.N);
      i = this.P;
      f = (i / 2 + 0);
      j = this.A;
      paramCanvas.drawArc(new RectF(f, 0.0F, j, (j - i / 2)), 90.0F, 30.0F, false, this.N);
    } 
    if (this.L) {
      j = this.P;
      float f1 = (j / 2 + 0);
      float f2 = (j / 2 + 0);
      j = this.A;
      paramCanvas.drawArc(new RectF(f1, f2, j, j), 150.0F, 120.0F, false, this.O);
      paramCanvas.drawLines(getNumberTopReflections(), this.O);
      i = this.z;
      j = this.A;
      f1 = (i - j);
      int k = this.P;
      paramCanvas.drawArc(new RectF(f1, (k / 2 + 0), (i - k / 2), j), 270.0F, 30.0F, false, this.O);
    } 
  }
  
  public Gx getClassInfo() {
    if (this.S == null)
      a(); 
    return this.S;
  }
  
  public int getTopH() {
    return this.A;
  }
  
  public int getTotalHeight() {
    int i = this.A;
    int k = i;
    if (b())
      k = i + this.n + this.B - this.h; 
    i = k;
    if (c())
      i = k + this.m + this.C - this.h; 
    int j = this.d;
    if (j != 4 && j != 7 && j != 10) {
      k = i;
      return (j == 12) ? (i + this.h) : k;
    } 
    return i + this.h;
  }
  
  public int getTotalWidth() {
    return this.z;
  }
  
  public int getW() {
    return this.z;
  }
  
  public final void h(Canvas paramCanvas) {
    paramCanvas.drawRect(new Rect(0, 0, this.z, this.A), this.f);
    if (this.K)
      paramCanvas.drawLines(getRectShadows(), this.N); 
    if (this.L)
      paramCanvas.drawLines(getRectReflections(), this.O); 
  }
  
  public void onDraw(Canvas paramCanvas) {
    this.f.setColor(this.e);
    switch (this.d) {
      case 12:
        e(paramCanvas);
        break;
      case 10:
      case 11:
        f(paramCanvas);
        break;
      case 9:
        c(paramCanvas);
        break;
      case 7:
        d(paramCanvas);
        break;
      case 4:
      case 5:
        b(paramCanvas);
        break;
      case 3:
        g(paramCanvas);
        break;
      case 2:
        a(paramCanvas);
        break;
      case 1:
        h(paramCanvas);
        break;
    } 
    super.onDraw(paramCanvas);
  }
  
  public void onMeasure(int paramInt1, int paramInt2) {
    super.onMeasure(View.MeasureSpec.makeMeasureSpec(getTotalWidth(), 1073741824), View.MeasureSpec.makeMeasureSpec(getTotalHeight(), 1073741824));
  }
  
  public void setSubstack1Height(int paramInt) {
    paramInt = Math.max(paramInt, this.i);
    if (paramInt != this.B)
      this.B = paramInt; 
  }
  
  public void setSubstack2Height(int paramInt) {
    paramInt = Math.max(paramInt, this.i);
    if (paramInt != this.C)
      this.C = paramInt; 
  }
}
