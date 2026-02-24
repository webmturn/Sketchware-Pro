package pro.sketchware.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.widget.RelativeLayout;

public class BaseBlockView extends RelativeLayout {
  public int blockHeight;
  
  public int contentHeight;
  
  public int innerHeight;
  
  public float density;
  
  public BlockView parentBlock;
  
  public int baseWidth;
  
  public int textHeight;
  
  public int paramSpacing;
  
  public int paramPadding;
  
  public int paramMargin;
  
  public boolean drawShadow;
  
  public boolean drawReflection;
  
  public Paint shadowPaint;
  
  public Paint outlinePaint;
  
  public Paint reflectionPaint;
  
  public int outlineStrokeWidth;
  
  public int reflectionStrokeWidth;
  
  public int defaultBlockColor;
  
  public ClassInfo classInfo;
  
  public Context context;
  
  public String blockType;
  
  public String componentType;
  
  public int shapeType;
  
  public int blockColor;
  
  public Paint fillPaint;
  
  public boolean isEditable;
  
  public int borderWidth;
  
  public int minHeight;
  
  public int cornerRadius;
  
  public int notchWidth;
  
  public int notchDepth;
  
  public int topPadding;
  
  public int bottomPadding;
  
  public int connectorOffset;
  
  public int connectorStart;
  
  public int connectorEnd;
  
  public int connectorEndOffset;
  
  public int connectorWidth;
  
  public int defaultMinWidth;
  
  public int topSpacing;
  
  public int bottomSpacing;
  
  public int leftIndent;
  
  public int rightIndent;
  
  public int shadowOffset;
  
  public int blockWidth;
  
  public BaseBlockView(Context paramContext, String paramString1, String paramString2, boolean paramBoolean) {
    super(paramContext);
    this.borderWidth = 3;
    this.minHeight = 12;
    this.cornerRadius = 15;
    this.notchWidth = 3;
    this.notchDepth = 2;
    this.topPadding = 15;
    this.bottomPadding = 15;
    this.connectorOffset = 15;
    this.connectorStart = this.connectorOffset + this.borderWidth;
    this.connectorEnd = this.connectorStart + 10;
    this.connectorEndOffset = this.connectorEnd + this.borderWidth;
    this.connectorWidth = 6;
    this.defaultMinWidth = 60;
    this.topSpacing = 2;
    this.bottomSpacing = 2;
    this.leftIndent = 3;
    this.rightIndent = 0;
    this.shadowOffset = 2;
    this.contentHeight = this.minHeight;
    this.innerHeight = this.minHeight;
    this.parentBlock = null;
    this.baseWidth = 100;
    this.textHeight = 14;
    this.paramSpacing = 15;
    this.paramPadding = 6;
    this.paramMargin = 4;
    this.drawShadow = false;
    this.drawReflection = false;
    this.outlineStrokeWidth = 1;
    this.reflectionStrokeWidth = 1;
    this.defaultBlockColor = 805306368;
    this.classInfo = null;
    this.context = paramContext;
    this.blockType = paramString1;
    if (paramString2 != null && paramString2.indexOf(".") > 0) {
      this.componentType = paramString2.substring(0, paramString2.indexOf("."));
    } else {
      this.componentType = paramString2;
    }
    a();
    String type = this.blockType;
    switch (type) {
      case " ": this.topSpacing = 4; this.shapeType = 4; break;
      case "b": this.leftIndent = 8; this.rightIndent = 5; this.shapeType = 2; break;
      case "d": this.shapeType = 3; this.leftIndent = 4; break;
      case "n": this.shapeType = 3; break;
      case "c": this.topSpacing = 4; this.shapeType = 10; break;
      case "e": this.topSpacing = 4; this.shapeType = 12; break;
      case "f": this.topSpacing = 4; this.shapeType = 5; break;
      case "h": this.topSpacing = 8; this.shapeType = 7; break;
      case "m": this.shapeType = 9; break;
      case "s":
      case "v":
      case "p":
      case "l":
      case "a": this.shapeType = 1; break;
    }
    this.blockColor = this.defaultBlockColor;
    this.isEditable = paramBoolean;
    setWillNotDraw(false);
    a(paramContext);
  }
  
  public BaseBlockView(Context paramContext, String paramString, boolean paramBoolean) {
    this(paramContext, paramString, "", paramBoolean);
  }
  
  private float[] getBooleanReflections() {
    int i = this.blockHeight / 2;
    int j = this.outlineStrokeWidth;
    float f1 = (j / 2 + 0);
    float f2 = i;
    return new float[] { f1, f2, f2, (j / 2 + 0), f2, (j / 2 + 0), (this.blockWidth - i), (j / 2 + 0) };
  }
  
  private float[] getBooleanShadows() {
    int i = this.blockHeight;
    int j = i / 2;
    int k = this.blockWidth;
    int m = this.outlineStrokeWidth;
    float f1 = (k - m / 2);
    float f2 = j;
    return new float[] { f1, f2, (k - j), (i - m / 2), (k - j), (i - m / 2), f2, (i - m / 2) };
  }
  
  private float[] getNumberBottomShadows() {
    int i = this.blockHeight;
    int j = i / 2;
    float f = (this.blockWidth - j);
    int k = this.outlineStrokeWidth;
    return new float[] { f, (i - k / 2), j, (i - k / 2) };
  }
  
  private float[] getNumberTopReflections() {
    int i = this.blockHeight / 2;
    float f = i;
    int j = this.outlineStrokeWidth;
    return new float[] { f, (j / 2 + 0), (this.blockWidth - i), (j / 2 + 0) };
  }
  
  private float[] getRectReflections() {
    int i = this.outlineStrokeWidth;
    return new float[] { 0.0F, (i / 2 + 0), (this.blockWidth - i / 2), (i / 2 + 0), (i / 2 + 0), 0.0F, (i / 2 + 0), (this.blockHeight - i / 2) };
  }
  
  private float[] getRectShadows() {
    int i = this.blockWidth;
    int j = this.outlineStrokeWidth;
    float f1 = (i - j / 2);
    float f2 = (i - j / 2);
    int k = this.blockHeight;
    return new float[] { f1, 0.0F, f2, (k - j / 2), (i - j / 2), (k - j / 2), 0.0F, (k - j / 2) };
  }
  
  public void a() {
    this.classInfo = ComponentTypeMapper.getClassInfo(this.blockType, this.componentType);
  }
  
  public void a(float paramFloat1, float paramFloat2, boolean paramBoolean) {
    if (this.shapeType == 9) {
      this.blockWidth = (int)paramFloat1 + this.paramSpacing;
    } else {
      this.blockWidth = (int)paramFloat1;
    } 
    this.blockHeight = (int)paramFloat2;
    if (paramBoolean)
      e(); 
  }
  
  public void a(BaseBlockView paramTs, boolean paramBoolean1, boolean paramBoolean2, int paramInt) {
    this.blockColor = -16777216;
    this.shapeType = paramTs.shapeType;
    this.blockWidth = paramTs.blockWidth;
    this.blockHeight = paramTs.blockHeight;
    this.contentHeight = paramTs.contentHeight;
    this.innerHeight = paramTs.innerHeight;
    if (!paramBoolean1)
      if (paramBoolean2) {
        this.shapeType = 4;
        this.blockHeight = (int)(this.density * 6.0F);
      } else if (paramInt > 0) {
        this.contentHeight = paramInt - this.borderWidth;
      }  
    e();
  }
  
  public final void a(Context paramContext) {
    this.density = ViewUtil.a(paramContext, 1.0F);
    float f1 = this.borderWidth;
    float f2 = this.density;
    this.borderWidth = (int)(f1 * f2);
    this.minHeight = (int)(this.minHeight * f2);
    this.cornerRadius = (int)(this.cornerRadius * f2);
    this.topPadding = (int)(this.topPadding * f2);
    this.bottomPadding = (int)(this.bottomPadding * f2);
    this.notchWidth = (int)(this.notchWidth * f2);
    this.notchDepth = (int)(this.notchDepth * f2);
    this.connectorOffset = (int)(this.connectorOffset * f2);
    this.connectorStart = (int)(this.connectorStart * f2);
    this.connectorEnd = (int)(this.connectorEnd * f2);
    this.connectorEndOffset = (int)(this.connectorEndOffset * f2);
    this.connectorWidth = (int)(this.connectorWidth * f2);
    this.defaultMinWidth = (int)(this.defaultMinWidth * f2);
    this.contentHeight = (int)(this.contentHeight * f2);
    this.innerHeight = (int)(this.innerHeight * f2);
    this.leftIndent = (int)(this.leftIndent * f2);
    this.topSpacing = (int)(this.topSpacing * f2);
    this.rightIndent = (int)(this.rightIndent * f2);
    this.bottomSpacing = (int)(this.bottomSpacing * f2);
    this.shadowOffset = (int)(this.shadowOffset * f2);
    this.baseWidth = (int)(this.baseWidth * f2);
    this.textHeight = (int)(this.textHeight * f2);
    this.paramPadding = (int)(this.paramPadding * f2);
    this.paramMargin = (int)(this.paramMargin * f2);
    this.paramSpacing = (int)(this.paramSpacing * f2);
    this.outlineStrokeWidth = (int)(this.outlineStrokeWidth * f2);
    this.reflectionStrokeWidth = (int)(this.reflectionStrokeWidth * f2);
    if (this.outlineStrokeWidth < 2)
      this.outlineStrokeWidth = 2; 
    if (this.reflectionStrokeWidth < 2)
      this.reflectionStrokeWidth = 2; 
    this.fillPaint = new Paint();
    if (!this.isEditable) {
      this.drawShadow = true;
      this.drawReflection = true;
    } 
    this.shadowPaint = new Paint();
    this.shadowPaint.setColor(-536870912);
    this.shadowPaint.setStrokeWidth(this.outlineStrokeWidth);
    this.outlinePaint = new Paint();
    this.outlinePaint.setColor(-1610612736);
    this.outlinePaint.setStyle(Paint.Style.STROKE);
    this.outlinePaint.setStrokeWidth(this.outlineStrokeWidth);
    this.reflectionPaint = new Paint();
    this.reflectionPaint.setColor(-1593835521);
    this.reflectionPaint.setStyle(Paint.Style.STROKE);
    this.reflectionPaint.setStrokeWidth(this.reflectionStrokeWidth);
    setLayerType(1, null);
    a(this.baseWidth, (this.textHeight + this.topSpacing + this.bottomSpacing), false);
  }
  
  public final void a(Canvas paramCanvas) {
    Path path = new Path();
    int i = this.blockHeight;
    int j = i / 2;
    float f = j;
    path.moveTo(f, i);
    path.lineTo(0.0F, f);
    path.lineTo(f, 0.0F);
    path.lineTo((this.blockWidth - j), 0.0F);
    path.lineTo(this.blockWidth, f);
    path.lineTo((this.blockWidth - j), this.blockHeight);
    paramCanvas.drawPath(path, this.fillPaint);
    if (this.drawShadow)
      paramCanvas.drawLines(getBooleanShadows(), this.outlinePaint); 
    if (this.drawReflection)
      paramCanvas.drawLines(getBooleanReflections(), this.reflectionPaint); 
  }
  
  public final void a(Path paramPath) {
    paramPath.moveTo(0.0F, this.notchWidth);
    paramPath.lineTo(this.notchWidth, 0.0F);
    paramPath.lineTo(this.connectorOffset, 0.0F);
    paramPath.lineTo(this.connectorStart, this.borderWidth);
    paramPath.lineTo(this.connectorEnd, this.borderWidth);
    paramPath.lineTo(this.connectorEndOffset, 0.0F);
    paramPath.lineTo((this.blockWidth - this.notchWidth), 0.0F);
    paramPath.lineTo(this.blockWidth, this.notchWidth);
  }
  
  public final void a(Path paramPath, int paramInt) {
    paramPath.lineTo(this.cornerRadius, (paramInt - this.notchDepth));
    float f1 = (this.cornerRadius + this.notchDepth);
    float f2 = paramInt;
    paramPath.lineTo(f1, f2);
    paramPath.lineTo((this.blockWidth - this.notchWidth), f2);
    paramPath.lineTo(this.blockWidth, (paramInt + this.notchWidth));
  }
  
  public final void a(Path paramPath, int paramInt1, boolean paramBoolean, int paramInt2) {
    paramPath.lineTo(this.blockWidth, (paramInt1 - this.notchWidth));
    float f1 = (this.blockWidth - this.notchWidth);
    float f2 = paramInt1;
    paramPath.lineTo(f1, f2);
    if (paramBoolean) {
      paramPath.lineTo((this.connectorEndOffset + paramInt2), f2);
      paramPath.lineTo((this.connectorEnd + paramInt2), (this.borderWidth + paramInt1));
      paramPath.lineTo((this.connectorStart + paramInt2), (this.borderWidth + paramInt1));
      paramPath.lineTo((this.connectorOffset + paramInt2), f2);
    } 
    if (paramInt2 > 0) {
      paramPath.lineTo((this.notchDepth + paramInt2), f2);
      paramPath.lineTo(paramInt2, (paramInt1 + this.notchDepth));
    } else {
      paramPath.lineTo((paramInt2 + this.notchWidth), f2);
      paramPath.lineTo(0.0F, (paramInt1 - this.notchWidth));
    } 
  }
  
  public final float[] a(int paramInt) {
    int i = this.outlineStrokeWidth;
    float f1 = (i / 2 + 0);
    int j = this.notchWidth;
    float f2 = (paramInt - j);
    float f3 = (i / 2 + 0);
    float f4 = j;
    float f5 = (i / 2 + 0);
    float f6 = j;
    float f7 = j;
    float f8 = (i / 2 + 0);
    float f9 = j;
    float f10 = (i / 2 + 0);
    float f11 = this.connectorOffset;
    float f12 = (i / 2 + 0);
    float f13 = this.connectorStart;
    paramInt = this.borderWidth;
    float f14 = (i / 2 + paramInt);
    int k = this.connectorEnd;
    float f15 = k;
    float f16 = (i / 2 + paramInt);
    float f17 = k;
    float f18 = (paramInt + i / 2);
    paramInt = this.connectorEndOffset;
    return new float[] { 
        f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, 
        f11, f12, f13, f14, f15, f16, f17, f18, paramInt, (i / 2 + 0), 
        paramInt, (i / 2 + 0), (this.blockWidth - j), (i / 2 + 0) };
  }
  
  public final float[] a(int paramInt1, int paramInt2) {
    int i = this.cornerRadius;
    int j = this.notchDepth;
    float f = (i + j);
    int k = this.outlineStrokeWidth;
    return new float[] { f, (paramInt1 - k / 2), (i - k / 2), (paramInt1 + j), (i - k / 2), (paramInt1 + j), (i - k / 2), (paramInt2 - j) };
  }
  
  public final float[] a(int paramInt1, boolean paramBoolean, int paramInt2) {
    float[] arrayOfFloat;
    if (paramBoolean) {
      arrayOfFloat = new float[24];
    } else {
      arrayOfFloat = new float[8];
    } 
    int i = this.blockWidth;
    arrayOfFloat[0] = i;
    int j = this.notchWidth;
    int k = this.outlineStrokeWidth;
    arrayOfFloat[1] = (paramInt1 - j - k / 2);
    arrayOfFloat[2] = (i - j);
    arrayOfFloat[3] = (paramInt1 - k / 2);
    if (paramBoolean) {
      arrayOfFloat[4] = (i - j);
      arrayOfFloat[5] = (paramInt1 - k / 2);
      i = this.connectorEndOffset;
      arrayOfFloat[6] = (paramInt2 + i);
      arrayOfFloat[7] = (paramInt1 - k / 2);
      arrayOfFloat[8] = (i + paramInt2);
      arrayOfFloat[9] = (paramInt1 - k / 2);
      int m = this.connectorEnd;
      arrayOfFloat[10] = (paramInt2 + m);
      i = this.borderWidth;
      arrayOfFloat[11] = (paramInt1 + i - k / 2);
      arrayOfFloat[12] = (m + paramInt2);
      arrayOfFloat[13] = (paramInt1 + i - k / 2);
      m = this.connectorStart;
      arrayOfFloat[14] = (paramInt2 + m);
      arrayOfFloat[15] = (paramInt1 + i - k / 2);
      arrayOfFloat[16] = (m + paramInt2);
      arrayOfFloat[17] = (i + paramInt1 - k / 2);
      i = this.connectorOffset;
      arrayOfFloat[18] = (paramInt2 + i);
      arrayOfFloat[19] = (paramInt1 - k / 2);
      if (paramInt2 > 0) {
        arrayOfFloat[20] = (i + paramInt2);
        arrayOfFloat[21] = (paramInt1 - k / 2);
        arrayOfFloat[22] = (paramInt2 + this.notchDepth);
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
      arrayOfFloat[6] = (paramInt2 + this.notchDepth);
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
    int i = this.blockHeight;
    int j = this.shapeType;
    boolean bool1 = true;
    if (j != 5) {
      bool2 = true;
    } else {
      bool2 = false;
    } 
    a(path, i, bool2, 0);
    paramCanvas.drawPath(path, this.fillPaint);
    if (this.drawShadow) {
      paramCanvas.drawLines(b(0, this.blockHeight), this.outlinePaint);
      i = this.blockHeight;
      if (this.shapeType != 5) {
        bool2 = bool1;
      } else {
        bool2 = false;
      } 
      paramCanvas.drawLines(a(i, bool2, 0), this.outlinePaint);
    } 
    if (this.drawReflection)
      paramCanvas.drawLines(a(this.blockHeight), this.reflectionPaint); 
  }
  
  public boolean b() {
    boolean bool;
    if (this.shapeType >= 10) {
      bool = true;
    } else {
      bool = false;
    } 
    return bool;
  }
  
  public final float[] b(int paramInt1, int paramInt2) {
    int i = this.blockWidth;
    int j = this.outlineStrokeWidth;
    float f = (i - j / 2);
    int k = this.notchWidth;
    return new float[] { f, (paramInt1 + k), (i - j / 2), (paramInt2 - k) };
  }
  
  public final void c(Canvas paramCanvas) {
    paramCanvas.drawRect(new Rect(0, 0, this.blockWidth, this.blockHeight), this.fillPaint);
    Path path = new Path();
    int i = this.blockWidth;
    int j = this.paramMargin;
    path.moveTo((i - j), j);
    j = this.blockWidth;
    i = this.paramMargin;
    int k = this.paramPadding;
    path.lineTo((j - i - k / 2), (i + k));
    j = this.blockWidth;
    i = this.paramMargin;
    path.lineTo((j - i - this.paramPadding), i);
    paramCanvas.drawPath(path, this.shadowPaint);
  }
  
  public boolean c() {
    boolean bool;
    if (this.shapeType == 12) {
      bool = true;
    } else {
      bool = false;
    } 
    return bool;
  }
  
  public final float[] c(int paramInt1, int paramInt2) {
    float f = (paramInt2 + this.notchDepth);
    paramInt2 = this.outlineStrokeWidth;
    return new float[] { f, (paramInt2 / 2 + paramInt1), (this.blockWidth - this.notchWidth), (paramInt1 + paramInt2 / 2) };
  }
  
  public int d() {
    return getTotalHeight() - this.borderWidth;
  }
  
  public final void d(Canvas paramCanvas) {
    Path path = new Path();
    path.moveTo(0.0F, this.connectorWidth);
    path.arcTo(new RectF(0.0F, 0.0F, this.defaultMinWidth, (this.connectorWidth * 2)), 180.0F, 180.0F);
    path.lineTo((this.blockWidth - this.notchWidth), this.connectorWidth);
    path.lineTo(this.blockWidth, (this.connectorWidth + this.notchWidth));
    a(path, this.blockHeight, true, 0);
    paramCanvas.drawPath(path, this.fillPaint);
    if (this.drawShadow) {
      paramCanvas.drawLines(b(this.connectorWidth, this.blockHeight), this.outlinePaint);
      paramCanvas.drawLines(a(this.blockHeight, true, 0), this.outlinePaint);
    } 
  }
  
  public void e() {
    requestLayout();
  }
  
  public final void e(Canvas paramCanvas) {
    Path path = new Path();
    int i = this.blockHeight;
    int j = this.contentHeight;
    int k = this.borderWidth;
    i = i + j - k;
    k = this.bottomPadding + i + this.innerHeight - k;
    a(path);
    a(path, this.blockHeight, true, this.cornerRadius);
    a(path, i);
    a(path, this.bottomPadding + i, true, this.cornerRadius);
    a(path, k);
    a(path, this.topPadding + k, true, 0);
    paramCanvas.drawPath(path, this.fillPaint);
    if (this.drawShadow) {
      paramCanvas.drawLines(b(0, this.blockHeight), this.outlinePaint);
      paramCanvas.drawLines(a(this.blockHeight, true, this.cornerRadius), this.outlinePaint);
      paramCanvas.drawLines(a(this.blockHeight, i), this.outlinePaint);
      paramCanvas.drawLines(b(i, this.bottomPadding + i), this.outlinePaint);
      paramCanvas.drawLines(a(this.bottomPadding + i, true, this.cornerRadius), this.outlinePaint);
      paramCanvas.drawLines(a(this.bottomPadding + i, k), this.outlinePaint);
      paramCanvas.drawLines(b(k, this.topPadding + k), this.outlinePaint);
      paramCanvas.drawLines(a(this.topPadding + k, true, 0), this.outlinePaint);
    } 
    if (this.drawReflection) {
      paramCanvas.drawLines(a(this.topPadding + k), this.reflectionPaint);
      paramCanvas.drawLines(c(i, this.cornerRadius), this.reflectionPaint);
      paramCanvas.drawLines(c(k, this.cornerRadius), this.reflectionPaint);
    } 
  }
  
  public int f() {
    return this.blockHeight;
  }
  
  public final void f(Canvas paramCanvas) {
    boolean bool2;
    Path path = new Path();
    int i = this.blockHeight + this.contentHeight - this.borderWidth;
    a(path);
    int j = this.blockHeight;
    int k = this.cornerRadius;
    boolean bool1 = true;
    a(path, j, true, k);
    a(path, i);
    j = this.topPadding;
    if (this.shapeType == 10) {
      bool2 = true;
    } else {
      bool2 = false;
    } 
    a(path, j + i, bool2, 0);
    paramCanvas.drawPath(path, this.fillPaint);
    if (this.drawShadow) {
      paramCanvas.drawLines(b(0, this.blockHeight), this.outlinePaint);
      paramCanvas.drawLines(a(this.blockHeight, true, this.cornerRadius), this.outlinePaint);
      paramCanvas.drawLines(a(this.blockHeight, i), this.outlinePaint);
      paramCanvas.drawLines(b(i, this.topPadding + i), this.outlinePaint);
      j = this.topPadding;
      if (this.shapeType == 10) {
        bool2 = bool1;
      } else {
        bool2 = false;
      } 
      paramCanvas.drawLines(a(j + i, bool2, 0), this.outlinePaint);
    } 
    if (this.drawReflection) {
      paramCanvas.drawLines(a(this.topPadding + i), this.reflectionPaint);
      paramCanvas.drawLines(c(i, this.cornerRadius), this.reflectionPaint);
    } 
  }
  
  public int g() {
    return this.blockHeight + this.contentHeight + this.bottomPadding - this.borderWidth;
  }
  
  public final void g(Canvas paramCanvas) {
    Path path = new Path();
    int i = this.blockHeight;
    int j = i / 2;
    path.moveTo(j, i);
    i = this.blockHeight;
    path.arcTo(new RectF(0.0F, 0.0F, i, i), 90.0F, 180.0F);
    path.lineTo((this.blockWidth - j), 0.0F);
    j = this.blockWidth;
    i = this.blockHeight;
    path.arcTo(new RectF((j - i), 0.0F, j, i), 270.0F, 180.0F);
    paramCanvas.drawPath(path, this.fillPaint);
    if (this.drawShadow) {
      i = this.blockWidth;
      j = this.blockHeight;
      float f = (i - j);
      int k = this.outlineStrokeWidth;
      paramCanvas.drawArc(new RectF(f, 0.0F, (i - k / 2), (j - k / 2)), 330.0F, 120.0F, false, this.outlinePaint);
      paramCanvas.drawLines(getNumberBottomShadows(), this.outlinePaint);
      i = this.outlineStrokeWidth;
      f = (i / 2 + 0);
      j = this.blockHeight;
      paramCanvas.drawArc(new RectF(f, 0.0F, j, (j - i / 2)), 90.0F, 30.0F, false, this.outlinePaint);
    } 
    if (this.drawReflection) {
      j = this.outlineStrokeWidth;
      float f1 = (j / 2 + 0);
      float f2 = (j / 2 + 0);
      j = this.blockHeight;
      paramCanvas.drawArc(new RectF(f1, f2, j, j), 150.0F, 120.0F, false, this.reflectionPaint);
      paramCanvas.drawLines(getNumberTopReflections(), this.reflectionPaint);
      i = this.blockWidth;
      j = this.blockHeight;
      f1 = (i - j);
      int k = this.outlineStrokeWidth;
      paramCanvas.drawArc(new RectF(f1, (k / 2 + 0), (i - k / 2), j), 270.0F, 30.0F, false, this.reflectionPaint);
    } 
  }
  
  public ClassInfo getClassInfo() {
    if (this.classInfo == null)
      a(); 
    return this.classInfo;
  }
  
  public int getTopH() {
    return this.blockHeight;
  }
  
  public int getTotalHeight() {
    int i = this.blockHeight;
    int k = i;
    if (b())
      k = i + this.bottomPadding + this.contentHeight - this.borderWidth; 
    i = k;
    if (c())
      i = k + this.topPadding + this.innerHeight - this.borderWidth; 
    int j = this.shapeType;
    if (j != 4 && j != 7 && j != 10) {
      k = i;
      return (j == 12) ? (i + this.borderWidth) : k;
    } 
    return i + this.borderWidth;
  }
  
  public int getTotalWidth() {
    return this.blockWidth;
  }
  
  public int getW() {
    return this.blockWidth;
  }
  
  public final void h(Canvas paramCanvas) {
    paramCanvas.drawRect(new Rect(0, 0, this.blockWidth, this.blockHeight), this.fillPaint);
    if (this.drawShadow)
      paramCanvas.drawLines(getRectShadows(), this.outlinePaint); 
    if (this.drawReflection)
      paramCanvas.drawLines(getRectReflections(), this.reflectionPaint); 
  }
  
  public void onDraw(Canvas paramCanvas) {
    this.fillPaint.setColor(this.blockColor);
    switch (this.shapeType) {
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
    paramInt = Math.max(paramInt, this.minHeight);
    if (paramInt != this.contentHeight)
      this.contentHeight = paramInt; 
  }
  
  public void setSubstack2Height(int paramInt) {
    paramInt = Math.max(paramInt, this.minHeight);
    if (paramInt != this.innerHeight)
      this.innerHeight = paramInt; 
  }
}
