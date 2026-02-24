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
  
  public int spacing;
  
  public int padding;
  
  public int margin;
  
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
  
  public BaseBlockView(Context context, String key, String value, boolean flag) {
    super(context);
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
    this.spacing = 15;
    this.padding = 6;
    this.margin = 4;
    this.drawShadow = false;
    this.drawReflection = false;
    this.outlineStrokeWidth = 1;
    this.reflectionStrokeWidth = 1;
    this.defaultBlockColor = 805306368;
    this.classInfo = null;
    this.context = context;
    this.blockType = key;
    if (value != null && value.indexOf(".") > 0) {
      this.componentType = value.substring(0, value.indexOf("."));
    } else {
      this.componentType = value;
    }
    initClassInfo();
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
    this.isEditable = flag;
    setWillNotDraw(false);
    initDensityAndPaints(context);
  }
  
  public BaseBlockView(Context context, String str, boolean flag) {
    this(context, str, "", flag);
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
  
  public void initClassInfo() {
    this.classInfo = ComponentTypeMapper.getClassInfo(this.blockType, this.componentType);
  }
  
  public void setBlockSize(float x, float y, boolean flag) {
    if (this.shapeType == 9) {
      this.blockWidth = (int)x + this.spacing;
    } else {
      this.blockWidth = (int)x;
    } 
    this.blockHeight = (int)y;
    if (flag)
      refreshLayout(); 
  }
  
  public void copyBlockDimensions(BaseBlockView targetBlock, boolean flag1, boolean flag2, int index) {
    this.blockColor = -16777216;
    this.shapeType = targetBlock.shapeType;
    this.blockWidth = targetBlock.blockWidth;
    this.blockHeight = targetBlock.blockHeight;
    this.contentHeight = targetBlock.contentHeight;
    this.innerHeight = targetBlock.innerHeight;
    if (!flag1)
      if (flag2) {
        this.shapeType = 4;
        this.blockHeight = (int)(this.density * 6.0F);
      } else if (index > 0) {
        this.contentHeight = index - this.borderWidth;
      }  
    refreshLayout();
  }
  
  public final void initDensityAndPaints(Context context) {
    this.density = ViewUtil.dpToPx(context, 1.0F);
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
    this.padding = (int)(this.padding * f2);
    this.margin = (int)(this.margin * f2);
    this.spacing = (int)(this.spacing * f2);
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
    setBlockSize(this.baseWidth, (this.textHeight + this.topSpacing + this.bottomSpacing), false);
  }
  
  public final void drawBooleanShape(Canvas canvas) {
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
    canvas.drawPath(path, this.fillPaint);
    if (this.drawShadow)
      canvas.drawLines(getBooleanShadows(), this.outlinePaint); 
    if (this.drawReflection)
      canvas.drawLines(getBooleanReflections(), this.reflectionPaint); 
  }
  
  public final void drawTopPath(Path path) {
    path.moveTo(0.0F, this.notchWidth);
    path.lineTo(this.notchWidth, 0.0F);
    path.lineTo(this.connectorOffset, 0.0F);
    path.lineTo(this.connectorStart, this.borderWidth);
    path.lineTo(this.connectorEnd, this.borderWidth);
    path.lineTo(this.connectorEndOffset, 0.0F);
    path.lineTo((this.blockWidth - this.notchWidth), 0.0F);
    path.lineTo(this.blockWidth, this.notchWidth);
  }
  
  public final void drawSubstackBottomPath(Path path, int index) {
    path.lineTo(this.cornerRadius, (index - this.notchDepth));
    float f1 = (this.cornerRadius + this.notchDepth);
    float f2 = index;
    path.lineTo(f1, f2);
    path.lineTo((this.blockWidth - this.notchWidth), f2);
    path.lineTo(this.blockWidth, (index + this.notchWidth));
  }
  
  public final void drawBottomPath(Path path, int start, boolean flag, int end) {
    path.lineTo(this.blockWidth, (start - this.notchWidth));
    float f1 = (this.blockWidth - this.notchWidth);
    float f2 = start;
    path.lineTo(f1, f2);
    if (flag) {
      path.lineTo((this.connectorEndOffset + end), f2);
      path.lineTo((this.connectorEnd + end), (this.borderWidth + start));
      path.lineTo((this.connectorStart + end), (this.borderWidth + start));
      path.lineTo((this.connectorOffset + end), f2);
    } 
    if (end > 0) {
      path.lineTo((this.notchDepth + end), f2);
      path.lineTo(end, (start + this.notchDepth));
    } else {
      path.lineTo((end + this.notchWidth), f2);
      path.lineTo(0.0F, (start - this.notchWidth));
    } 
  }
  
  public final float[] getTopReflectionLines(int index) {
    int i = this.outlineStrokeWidth;
    float f1 = (i / 2 + 0);
    int j = this.notchWidth;
    float f2 = (index - j);
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
    index = this.borderWidth;
    float f14 = (i / 2 + index);
    int k = this.connectorEnd;
    float f15 = k;
    float f16 = (i / 2 + index);
    float f17 = k;
    float f18 = (index + i / 2);
    index = this.connectorEndOffset;
    return new float[] { 
        f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, 
        f11, f12, f13, f14, f15, f16, f17, f18, index, (i / 2 + 0), 
        index, (i / 2 + 0), (this.blockWidth - j), (i / 2 + 0) };
  }
  
  public final float[] getLeftSideShadowLines(int start, int end) {
    int i = this.cornerRadius;
    int j = this.notchDepth;
    float f = (i + j);
    int k = this.outlineStrokeWidth;
    return new float[] { f, (start - k / 2), (i - k / 2), (start + j), (i - k / 2), (start + j), (i - k / 2), (end - j) };
  }
  
  public final float[] getBottomShadowLines(int start, boolean flag, int end) {
    float[] floatValues;
    if (flag) {
      floatValues = new float[24];
    } else {
      floatValues = new float[8];
    } 
    int i = this.blockWidth;
    floatValues[0] = i;
    int j = this.notchWidth;
    int k = this.outlineStrokeWidth;
    floatValues[1] = (start - j - k / 2);
    floatValues[2] = (i - j);
    floatValues[3] = (start - k / 2);
    if (flag) {
      floatValues[4] = (i - j);
      floatValues[5] = (start - k / 2);
      i = this.connectorEndOffset;
      floatValues[6] = (end + i);
      floatValues[7] = (start - k / 2);
      floatValues[8] = (i + end);
      floatValues[9] = (start - k / 2);
      int m = this.connectorEnd;
      floatValues[10] = (end + m);
      i = this.borderWidth;
      floatValues[11] = (start + i - k / 2);
      floatValues[12] = (m + end);
      floatValues[13] = (start + i - k / 2);
      m = this.connectorStart;
      floatValues[14] = (end + m);
      floatValues[15] = (start + i - k / 2);
      floatValues[16] = (m + end);
      floatValues[17] = (i + start - k / 2);
      i = this.connectorOffset;
      floatValues[18] = (end + i);
      floatValues[19] = (start - k / 2);
      if (end > 0) {
        floatValues[20] = (i + end);
        floatValues[21] = (start - k / 2);
        floatValues[22] = (end + this.notchDepth);
        floatValues[23] = (start - k / 2);
      } else {
        floatValues[20] = (i + end);
        floatValues[21] = (start - k / 2);
        floatValues[22] = (end + j);
        floatValues[23] = (start - k / 2);
      } 
    } else if (end > 0) {
      floatValues[4] = (i - j);
      floatValues[5] = (start - k / 2);
      floatValues[6] = (end + this.notchDepth);
      floatValues[7] = (start - k / 2);
    } else {
      floatValues[4] = (i - j);
      floatValues[5] = (start - k / 2);
      floatValues[6] = (end + j);
      floatValues[7] = (start - k / 2);
    } 
    return floatValues;
  }
  
  public final void drawStatementShape(Canvas canvas) {
    boolean hasBottomNotch;
    Path path = new Path();
    drawTopPath(path);
    int i = this.blockHeight;
    int j = this.shapeType;
    if (j != 5) {
      hasBottomNotch = true;
    } else {
      hasBottomNotch = false;
    } 
    drawBottomPath(path, i, hasBottomNotch, 0);
    canvas.drawPath(path, this.fillPaint);
    if (this.drawShadow) {
      canvas.drawLines(getRightSideShadowLines(0, this.blockHeight), this.outlinePaint);
      i = this.blockHeight;
      hasBottomNotch = (this.shapeType != 5);
      canvas.drawLines(getBottomShadowLines(i, hasBottomNotch, 0), this.outlinePaint);
    } 
    if (this.drawReflection)
      canvas.drawLines(getTopReflectionLines(this.blockHeight), this.reflectionPaint); 
  }
  
  public boolean hasSubstack() {
    boolean bool;
    if (this.shapeType >= 10) {
      bool = true;
    } else {
      bool = false;
    } 
    return bool;
  }
  
  public final float[] getRightSideShadowLines(int start, int end) {
    int i = this.blockWidth;
    int j = this.outlineStrokeWidth;
    float f = (i - j / 2);
    int k = this.notchWidth;
    return new float[] { f, (start + k), (i - j / 2), (end - k) };
  }
  
  public final void drawParameterShape(Canvas canvas) {
    canvas.drawRect(new Rect(0, 0, this.blockWidth, this.blockHeight), this.fillPaint);
    Path path = new Path();
    int i = this.blockWidth;
    int j = this.margin;
    path.moveTo((i - j), j);
    j = this.blockWidth;
    i = this.margin;
    int k = this.padding;
    path.lineTo((j - i - k / 2), (i + k));
    j = this.blockWidth;
    i = this.margin;
    path.lineTo((j - i - this.padding), i);
    canvas.drawPath(path, this.shadowPaint);
  }
  
  public boolean hasDoubleSubstack() {
    boolean bool;
    if (this.shapeType == 12) {
      bool = true;
    } else {
      bool = false;
    } 
    return bool;
  }
  
  public final float[] getSubstackReflectionLines(int start, int end) {
    float f = (end + this.notchDepth);
    end = this.outlineStrokeWidth;
    return new float[] { f, (end / 2 + start), (this.blockWidth - this.notchWidth), (start + end / 2) };
  }
  
  public int getContentBottom() {
    return getTotalHeight() - this.borderWidth;
  }
  
  public final void drawHatShape(Canvas canvas) {
    Path path = new Path();
    path.moveTo(0.0F, this.connectorWidth);
    path.arcTo(new RectF(0.0F, 0.0F, this.defaultMinWidth, (this.connectorWidth * 2)), 180.0F, 180.0F);
    path.lineTo((this.blockWidth - this.notchWidth), this.connectorWidth);
    path.lineTo(this.blockWidth, (this.connectorWidth + this.notchWidth));
    drawBottomPath(path, this.blockHeight, true, 0);
    canvas.drawPath(path, this.fillPaint);
    if (this.drawShadow) {
      canvas.drawLines(getRightSideShadowLines(this.connectorWidth, this.blockHeight), this.outlinePaint);
      canvas.drawLines(getBottomShadowLines(this.blockHeight, true, 0), this.outlinePaint);
    } 
  }
  
  public void refreshLayout() {
    requestLayout();
  }
  
  public final void drawDoubleSubstackShape(Canvas canvas) {
    Path path = new Path();
    int i = this.blockHeight;
    int j = this.contentHeight;
    int k = this.borderWidth;
    i = i + j - k;
    k = this.bottomPadding + i + this.innerHeight - k;
    drawTopPath(path);
    drawBottomPath(path, this.blockHeight, true, this.cornerRadius);
    drawSubstackBottomPath(path, i);
    drawBottomPath(path, this.bottomPadding + i, true, this.cornerRadius);
    drawSubstackBottomPath(path, k);
    drawBottomPath(path, this.topPadding + k, true, 0);
    canvas.drawPath(path, this.fillPaint);
    if (this.drawShadow) {
      canvas.drawLines(getRightSideShadowLines(0, this.blockHeight), this.outlinePaint);
      canvas.drawLines(getBottomShadowLines(this.blockHeight, true, this.cornerRadius), this.outlinePaint);
      canvas.drawLines(getLeftSideShadowLines(this.blockHeight, i), this.outlinePaint);
      canvas.drawLines(getRightSideShadowLines(i, this.bottomPadding + i), this.outlinePaint);
      canvas.drawLines(getBottomShadowLines(this.bottomPadding + i, true, this.cornerRadius), this.outlinePaint);
      canvas.drawLines(getLeftSideShadowLines(this.bottomPadding + i, k), this.outlinePaint);
      canvas.drawLines(getRightSideShadowLines(k, this.topPadding + k), this.outlinePaint);
      canvas.drawLines(getBottomShadowLines(this.topPadding + k, true, 0), this.outlinePaint);
    } 
    if (this.drawReflection) {
      canvas.drawLines(getTopReflectionLines(this.topPadding + k), this.reflectionPaint);
      canvas.drawLines(getSubstackReflectionLines(i, this.cornerRadius), this.reflectionPaint);
      canvas.drawLines(getSubstackReflectionLines(k, this.cornerRadius), this.reflectionPaint);
    } 
  }
  
  public int getBlockHeight() {
    return this.blockHeight;
  }
  
  public final void drawSingleSubstackShape(Canvas canvas) {
    boolean isEndCap;
    Path path = new Path();
    int i = this.blockHeight + this.contentHeight - this.borderWidth;
    drawTopPath(path);
    int j = this.blockHeight;
    int k = this.cornerRadius;
    drawBottomPath(path, j, true, k);
    drawSubstackBottomPath(path, i);
    j = this.topPadding;
    if (this.shapeType == 10) {
      isEndCap = true;
    } else {
      isEndCap = false;
    } 
    drawBottomPath(path, j + i, isEndCap, 0);
    canvas.drawPath(path, this.fillPaint);
    if (this.drawShadow) {
      canvas.drawLines(getRightSideShadowLines(0, this.blockHeight), this.outlinePaint);
      canvas.drawLines(getBottomShadowLines(this.blockHeight, true, this.cornerRadius), this.outlinePaint);
      canvas.drawLines(getLeftSideShadowLines(this.blockHeight, i), this.outlinePaint);
      canvas.drawLines(getRightSideShadowLines(i, this.topPadding + i), this.outlinePaint);
      j = this.topPadding;
      isEndCap = (this.shapeType == 10);
      canvas.drawLines(getBottomShadowLines(j + i, isEndCap, 0), this.outlinePaint);
    } 
    if (this.drawReflection) {
      canvas.drawLines(getTopReflectionLines(this.topPadding + i), this.reflectionPaint);
      canvas.drawLines(getSubstackReflectionLines(i, this.cornerRadius), this.reflectionPaint);
    } 
  }
  
  public int getSubstackBottom() {
    return this.blockHeight + this.contentHeight + this.bottomPadding - this.borderWidth;
  }
  
  public final void drawNumberShape(Canvas canvas) {
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
    canvas.drawPath(path, this.fillPaint);
    if (this.drawShadow) {
      i = this.blockWidth;
      j = this.blockHeight;
      float f = (i - j);
      int k = this.outlineStrokeWidth;
      canvas.drawArc(new RectF(f, 0.0F, (i - k / 2), (j - k / 2)), 330.0F, 120.0F, false, this.outlinePaint);
      canvas.drawLines(getNumberBottomShadows(), this.outlinePaint);
      i = this.outlineStrokeWidth;
      f = (i / 2 + 0);
      j = this.blockHeight;
      canvas.drawArc(new RectF(f, 0.0F, j, (j - i / 2)), 90.0F, 30.0F, false, this.outlinePaint);
    } 
    if (this.drawReflection) {
      j = this.outlineStrokeWidth;
      float f1 = (j / 2 + 0);
      float f2 = (j / 2 + 0);
      j = this.blockHeight;
      canvas.drawArc(new RectF(f1, f2, j, j), 150.0F, 120.0F, false, this.reflectionPaint);
      canvas.drawLines(getNumberTopReflections(), this.reflectionPaint);
      i = this.blockWidth;
      j = this.blockHeight;
      f1 = (i - j);
      int k = this.outlineStrokeWidth;
      canvas.drawArc(new RectF(f1, (k / 2 + 0), (i - k / 2), j), 270.0F, 30.0F, false, this.reflectionPaint);
    } 
  }
  
  public ClassInfo getClassInfo() {
    if (this.classInfo == null)
      initClassInfo(); 
    return this.classInfo;
  }
  
  public int getTopH() {
    return this.blockHeight;
  }
  
  public int getTotalHeight() {
    int i = this.blockHeight;
    int k = i;
    if (hasSubstack())
      k = i + this.bottomPadding + this.contentHeight - this.borderWidth; 
    i = k;
    if (hasDoubleSubstack())
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
  
  public final void drawRectShape(Canvas canvas) {
    canvas.drawRect(new Rect(0, 0, this.blockWidth, this.blockHeight), this.fillPaint);
    if (this.drawShadow)
      canvas.drawLines(getRectShadows(), this.outlinePaint); 
    if (this.drawReflection)
      canvas.drawLines(getRectReflections(), this.reflectionPaint); 
  }
  
  public void onDraw(Canvas canvas) {
    this.fillPaint.setColor(this.blockColor);
    switch (this.shapeType) {
      case 12:
        drawDoubleSubstackShape(canvas);
        break;
      case 10:
      case 11:
        drawSingleSubstackShape(canvas);
        break;
      case 9:
        drawParameterShape(canvas);
        break;
      case 7:
        drawHatShape(canvas);
        break;
      case 4:
      case 5:
        drawStatementShape(canvas);
        break;
      case 3:
        drawNumberShape(canvas);
        break;
      case 2:
        drawBooleanShape(canvas);
        break;
      case 1:
        drawRectShape(canvas);
        break;
    } 
    super.onDraw(canvas);
  }
  
  public void onMeasure(int start, int end) {
    super.onMeasure(View.MeasureSpec.makeMeasureSpec(getTotalWidth(), 1073741824), View.MeasureSpec.makeMeasureSpec(getTotalHeight(), 1073741824));
  }
  
  public void setSubstack1Height(int index) {
    index = Math.max(index, this.minHeight);
    if (index != this.contentHeight)
      this.contentHeight = index; 
  }
  
  public void setSubstack2Height(int index) {
    index = Math.max(index, this.minHeight);
    if (index != this.innerHeight)
      this.innerHeight = index; 
  }
}
