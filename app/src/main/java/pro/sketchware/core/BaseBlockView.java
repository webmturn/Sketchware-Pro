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
    borderWidth = 3;
    minHeight = 12;
    cornerRadius = 15;
    notchWidth = 3;
    notchDepth = 2;
    topPadding = 15;
    bottomPadding = 15;
    connectorOffset = 15;
    connectorStart = connectorOffset + borderWidth;
    connectorEnd = connectorStart + 10;
    connectorEndOffset = connectorEnd + borderWidth;
    connectorWidth = 6;
    defaultMinWidth = 60;
    topSpacing = 2;
    bottomSpacing = 2;
    leftIndent = 3;
    rightIndent = 0;
    shadowOffset = 2;
    contentHeight = minHeight;
    innerHeight = minHeight;
    parentBlock = null;
    baseWidth = 100;
    textHeight = 14;
    spacing = 15;
    padding = 6;
    margin = 4;
    drawShadow = false;
    drawReflection = false;
    outlineStrokeWidth = 1;
    reflectionStrokeWidth = 1;
    defaultBlockColor = 805306368;
    classInfo = null;
    this.context = context;
    blockType = key;
    if (value != null && value.indexOf(".") > 0) {
      componentType = value.substring(0, value.indexOf("."));
    } else {
      componentType = value;
    }
    initClassInfo();
    String type = blockType;
    switch (type) {
      case " ": topSpacing = 4; shapeType = 4; break;
      case "b": leftIndent = 8; rightIndent = 5; shapeType = 2; break;
      case "d": shapeType = 3; leftIndent = 4; break;
      case "n": shapeType = 3; break;
      case "c": topSpacing = 4; shapeType = 10; break;
      case "e": topSpacing = 4; shapeType = 12; break;
      case "f": topSpacing = 4; shapeType = 5; break;
      case "h": topSpacing = 8; shapeType = 7; break;
      case "m": shapeType = 9; break;
      case "s":
      case "v":
      case "p":
      case "l":
      case "a": shapeType = 1; break;
    }
    blockColor = defaultBlockColor;
    isEditable = flag;
    setWillNotDraw(false);
    initDensityAndPaints(context);
  }
  
  public BaseBlockView(Context context, String blockType, boolean flag) {
    this(context, blockType, "", flag);
  }
  
  private float[] getBooleanReflections() {
    int i = blockHeight / 2;
    int j = outlineStrokeWidth;
    float f1 = (j / 2 + 0);
    float f2 = i;
    return new float[] { f1, f2, f2, (j / 2 + 0), f2, (j / 2 + 0), (blockWidth - i), (j / 2 + 0) };
  }
  
  private float[] getBooleanShadows() {
    int i = blockHeight;
    int j = i / 2;
    int k = blockWidth;
    int m = outlineStrokeWidth;
    float f1 = (k - m / 2);
    float f2 = j;
    return new float[] { f1, f2, (k - j), (i - m / 2), (k - j), (i - m / 2), f2, (i - m / 2) };
  }
  
  private float[] getNumberBottomShadows() {
    int i = blockHeight;
    int j = i / 2;
    float f = (blockWidth - j);
    int k = outlineStrokeWidth;
    return new float[] { f, (i - k / 2), j, (i - k / 2) };
  }
  
  private float[] getNumberTopReflections() {
    int i = blockHeight / 2;
    float f = i;
    int j = outlineStrokeWidth;
    return new float[] { f, (j / 2 + 0), (blockWidth - i), (j / 2 + 0) };
  }
  
  private float[] getRectReflections() {
    int i = outlineStrokeWidth;
    return new float[] { 0.0F, (i / 2 + 0), (blockWidth - i / 2), (i / 2 + 0), (i / 2 + 0), 0.0F, (i / 2 + 0), (blockHeight - i / 2) };
  }
  
  private float[] getRectShadows() {
    int i = blockWidth;
    int j = outlineStrokeWidth;
    float f1 = (i - j / 2);
    float f2 = (i - j / 2);
    int k = blockHeight;
    return new float[] { f1, 0.0F, f2, (k - j / 2), (i - j / 2), (k - j / 2), 0.0F, (k - j / 2) };
  }
  
  public void initClassInfo() {
    classInfo = ComponentTypeMapper.getClassInfo(blockType, componentType);
  }
  
  public void setBlockSize(float x, float y, boolean flag) {
    if (shapeType == 9) {
      blockWidth = (int)x + spacing;
    } else {
      blockWidth = (int)x;
    } 
    blockHeight = (int)y;
    if (flag)
      refreshLayout(); 
  }
  
  public void copyBlockDimensions(BaseBlockView targetBlock, boolean isParameter, boolean showNotch, int index) {
    blockColor = -16777216;
    shapeType = targetBlock.shapeType;
    blockWidth = targetBlock.blockWidth;
    blockHeight = targetBlock.blockHeight;
    contentHeight = targetBlock.contentHeight;
    innerHeight = targetBlock.innerHeight;
    if (!isParameter)
      if (showNotch) {
        shapeType = 4;
        blockHeight = (int)(density * 6.0F);
      } else if (index > 0) {
        contentHeight = index - borderWidth;
      }  
    refreshLayout();
  }
  
  public final void initDensityAndPaints(Context context) {
    this.density = ViewUtil.dpToPx(context, 1.0F);
    float borderWidthF = borderWidth;
    float density = this.density;
    borderWidth = (int)(borderWidthF * density);
    minHeight = (int)(minHeight * density);
    cornerRadius = (int)(cornerRadius * density);
    topPadding = (int)(topPadding * density);
    bottomPadding = (int)(bottomPadding * density);
    notchWidth = (int)(notchWidth * density);
    notchDepth = (int)(notchDepth * density);
    connectorOffset = (int)(connectorOffset * density);
    connectorStart = (int)(connectorStart * density);
    connectorEnd = (int)(connectorEnd * density);
    connectorEndOffset = (int)(connectorEndOffset * density);
    connectorWidth = (int)(connectorWidth * density);
    defaultMinWidth = (int)(defaultMinWidth * density);
    contentHeight = (int)(contentHeight * density);
    innerHeight = (int)(innerHeight * density);
    leftIndent = (int)(leftIndent * density);
    topSpacing = (int)(topSpacing * density);
    rightIndent = (int)(rightIndent * density);
    bottomSpacing = (int)(bottomSpacing * density);
    shadowOffset = (int)(shadowOffset * density);
    baseWidth = (int)(baseWidth * density);
    textHeight = (int)(textHeight * density);
    padding = (int)(padding * density);
    margin = (int)(margin * density);
    spacing = (int)(spacing * density);
    outlineStrokeWidth = (int)(outlineStrokeWidth * density);
    reflectionStrokeWidth = (int)(reflectionStrokeWidth * density);
    if (outlineStrokeWidth < 2)
      outlineStrokeWidth = 2; 
    if (reflectionStrokeWidth < 2)
      reflectionStrokeWidth = 2; 
    fillPaint = new Paint();
    if (!isEditable) {
      drawShadow = true;
      drawReflection = true;
    } 
    shadowPaint = new Paint();
    shadowPaint.setColor(-536870912);
    shadowPaint.setStrokeWidth(outlineStrokeWidth);
    outlinePaint = new Paint();
    outlinePaint.setColor(-1610612736);
    outlinePaint.setStyle(Paint.Style.STROKE);
    outlinePaint.setStrokeWidth(outlineStrokeWidth);
    reflectionPaint = new Paint();
    reflectionPaint.setColor(-1593835521);
    reflectionPaint.setStyle(Paint.Style.STROKE);
    reflectionPaint.setStrokeWidth(reflectionStrokeWidth);
    setLayerType(1, null);
    setBlockSize(baseWidth, (textHeight + topSpacing + bottomSpacing), false);
  }
  
  public final void drawBooleanShape(Canvas canvas) {
    Path path = new Path();
    int i = blockHeight;
    int j = i / 2;
    float f = j;
    path.moveTo(f, i);
    path.lineTo(0.0F, f);
    path.lineTo(f, 0.0F);
    path.lineTo((blockWidth - j), 0.0F);
    path.lineTo(blockWidth, f);
    path.lineTo((blockWidth - j), blockHeight);
    canvas.drawPath(path, fillPaint);
    if (drawShadow)
      canvas.drawLines(getBooleanShadows(), outlinePaint); 
    if (drawReflection)
      canvas.drawLines(getBooleanReflections(), reflectionPaint); 
  }
  
  public final void drawTopPath(Path path) {
    path.moveTo(0.0F, notchWidth);
    path.lineTo(notchWidth, 0.0F);
    path.lineTo(connectorOffset, 0.0F);
    path.lineTo(connectorStart, borderWidth);
    path.lineTo(connectorEnd, borderWidth);
    path.lineTo(connectorEndOffset, 0.0F);
    path.lineTo((blockWidth - notchWidth), 0.0F);
    path.lineTo(blockWidth, notchWidth);
  }
  
  public final void drawSubstackBottomPath(Path path, int index) {
    path.lineTo(cornerRadius, (index - notchDepth));
    float f1 = (cornerRadius + notchDepth);
    float f2 = index;
    path.lineTo(f1, f2);
    path.lineTo((blockWidth - notchWidth), f2);
    path.lineTo(blockWidth, (index + notchWidth));
  }
  
  public final void drawBottomPath(Path path, int start, boolean flag, int end) {
    path.lineTo(blockWidth, (start - notchWidth));
    float f1 = (blockWidth - notchWidth);
    float f2 = start;
    path.lineTo(f1, f2);
    if (flag) {
      path.lineTo((connectorEndOffset + end), f2);
      path.lineTo((connectorEnd + end), (borderWidth + start));
      path.lineTo((connectorStart + end), (borderWidth + start));
      path.lineTo((connectorOffset + end), f2);
    } 
    if (end > 0) {
      path.lineTo((notchDepth + end), f2);
      path.lineTo(end, (start + notchDepth));
    } else {
      path.lineTo((end + notchWidth), f2);
      path.lineTo(0.0F, (start - notchWidth));
    } 
  }
  
  public final float[] getTopReflectionLines(int index) {
    int i = outlineStrokeWidth;
    float f1 = (i / 2 + 0);
    int j = notchWidth;
    float f2 = (index - j);
    float f3 = (i / 2 + 0);
    float f4 = j;
    float f5 = (i / 2 + 0);
    float f6 = j;
    float f7 = j;
    float f8 = (i / 2 + 0);
    float f9 = j;
    float f10 = (i / 2 + 0);
    float f11 = connectorOffset;
    float f12 = (i / 2 + 0);
    float f13 = connectorStart;
    index = borderWidth;
    float f14 = (i / 2 + index);
    int k = connectorEnd;
    float f15 = k;
    float f16 = (i / 2 + index);
    float f17 = k;
    float f18 = (index + i / 2);
    index = connectorEndOffset;
    return new float[] { 
        f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, 
        f11, f12, f13, f14, f15, f16, f17, f18, index, (i / 2 + 0), 
        index, (i / 2 + 0), (blockWidth - j), (i / 2 + 0) };
  }
  
  public final float[] getLeftSideShadowLines(int start, int end) {
    int i = cornerRadius;
    int j = notchDepth;
    float f = (i + j);
    int k = outlineStrokeWidth;
    return new float[] { f, (start - k / 2), (i - k / 2), (start + j), (i - k / 2), (start + j), (i - k / 2), (end - j) };
  }
  
  public final float[] getBottomShadowLines(int start, boolean flag, int end) {
    float[] floatValues;
    if (flag) {
      floatValues = new float[24];
    } else {
      floatValues = new float[8];
    } 
    int i = blockWidth;
    floatValues[0] = i;
    int j = notchWidth;
    int k = outlineStrokeWidth;
    floatValues[1] = (start - j - k / 2);
    floatValues[2] = (i - j);
    floatValues[3] = (start - k / 2);
    if (flag) {
      floatValues[4] = (i - j);
      floatValues[5] = (start - k / 2);
      i = connectorEndOffset;
      floatValues[6] = (end + i);
      floatValues[7] = (start - k / 2);
      floatValues[8] = (i + end);
      floatValues[9] = (start - k / 2);
      int m = connectorEnd;
      floatValues[10] = (end + m);
      i = borderWidth;
      floatValues[11] = (start + i - k / 2);
      floatValues[12] = (m + end);
      floatValues[13] = (start + i - k / 2);
      m = connectorStart;
      floatValues[14] = (end + m);
      floatValues[15] = (start + i - k / 2);
      floatValues[16] = (m + end);
      floatValues[17] = (i + start - k / 2);
      i = connectorOffset;
      floatValues[18] = (end + i);
      floatValues[19] = (start - k / 2);
      if (end > 0) {
        floatValues[20] = (i + end);
        floatValues[21] = (start - k / 2);
        floatValues[22] = (end + notchDepth);
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
      floatValues[6] = (end + notchDepth);
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
    Path path = new Path();
    drawTopPath(path);
    int i = blockHeight;
    boolean hasBottomNotch = (shapeType != 5);
    drawBottomPath(path, i, hasBottomNotch, 0);
    canvas.drawPath(path, fillPaint);
    if (drawShadow) {
      canvas.drawLines(getRightSideShadowLines(0, blockHeight), outlinePaint);
      canvas.drawLines(getBottomShadowLines(blockHeight, (shapeType != 5), 0), outlinePaint);
    } 
    if (drawReflection)
      canvas.drawLines(getTopReflectionLines(blockHeight), reflectionPaint); 
  }
  
  public boolean hasSubstack() {
    return shapeType >= 10;
  }
  
  public final float[] getRightSideShadowLines(int start, int end) {
    int i = blockWidth;
    int j = outlineStrokeWidth;
    float f = (i - j / 2);
    int k = notchWidth;
    return new float[] { f, (start + k), (i - j / 2), (end - k) };
  }
  
  public final void drawParameterShape(Canvas canvas) {
    canvas.drawRect(new Rect(0, 0, blockWidth, blockHeight), fillPaint);
    Path path = new Path();
    int i = blockWidth;
    int j = margin;
    path.moveTo((i - j), j);
    j = blockWidth;
    i = margin;
    int k = padding;
    path.lineTo((j - i - k / 2), (i + k));
    j = blockWidth;
    i = margin;
    path.lineTo((j - i - padding), i);
    canvas.drawPath(path, shadowPaint);
  }
  
  public boolean hasDoubleSubstack() {
    return shapeType == 12;
  }
  
  public final float[] getSubstackReflectionLines(int start, int end) {
    float f = (end + notchDepth);
    end = outlineStrokeWidth;
    return new float[] { f, (end / 2 + start), (blockWidth - notchWidth), (start + end / 2) };
  }
  
  public int getContentBottom() {
    return getTotalHeight() - borderWidth;
  }
  
  public final void drawHatShape(Canvas canvas) {
    Path path = new Path();
    path.moveTo(0.0F, connectorWidth);
    path.arcTo(new RectF(0.0F, 0.0F, defaultMinWidth, (connectorWidth * 2)), 180.0F, 180.0F);
    path.lineTo((blockWidth - notchWidth), connectorWidth);
    path.lineTo(blockWidth, (connectorWidth + notchWidth));
    drawBottomPath(path, blockHeight, true, 0);
    canvas.drawPath(path, fillPaint);
    if (drawShadow) {
      canvas.drawLines(getRightSideShadowLines(connectorWidth, blockHeight), outlinePaint);
      canvas.drawLines(getBottomShadowLines(blockHeight, true, 0), outlinePaint);
    } 
  }
  
  public void refreshLayout() {
    requestLayout();
  }
  
  public final void drawDoubleSubstackShape(Canvas canvas) {
    Path path = new Path();
    int i = blockHeight;
    int j = contentHeight;
    int k = borderWidth;
    i = i + j - k;
    k = bottomPadding + i + innerHeight - k;
    drawTopPath(path);
    drawBottomPath(path, blockHeight, true, cornerRadius);
    drawSubstackBottomPath(path, i);
    drawBottomPath(path, bottomPadding + i, true, cornerRadius);
    drawSubstackBottomPath(path, k);
    drawBottomPath(path, topPadding + k, true, 0);
    canvas.drawPath(path, fillPaint);
    if (drawShadow) {
      canvas.drawLines(getRightSideShadowLines(0, blockHeight), outlinePaint);
      canvas.drawLines(getBottomShadowLines(blockHeight, true, cornerRadius), outlinePaint);
      canvas.drawLines(getLeftSideShadowLines(blockHeight, i), outlinePaint);
      canvas.drawLines(getRightSideShadowLines(i, bottomPadding + i), outlinePaint);
      canvas.drawLines(getBottomShadowLines(bottomPadding + i, true, cornerRadius), outlinePaint);
      canvas.drawLines(getLeftSideShadowLines(bottomPadding + i, k), outlinePaint);
      canvas.drawLines(getRightSideShadowLines(k, topPadding + k), outlinePaint);
      canvas.drawLines(getBottomShadowLines(topPadding + k, true, 0), outlinePaint);
    } 
    if (drawReflection) {
      canvas.drawLines(getTopReflectionLines(topPadding + k), reflectionPaint);
      canvas.drawLines(getSubstackReflectionLines(i, cornerRadius), reflectionPaint);
      canvas.drawLines(getSubstackReflectionLines(k, cornerRadius), reflectionPaint);
    } 
  }
  
  public int getBlockHeight() {
    return blockHeight;
  }
  
  public final void drawSingleSubstackShape(Canvas canvas) {
    Path path = new Path();
    int i = blockHeight + contentHeight - borderWidth;
    drawTopPath(path);
    drawBottomPath(path, blockHeight, true, cornerRadius);
    drawSubstackBottomPath(path, i);
    boolean isEndCap = (shapeType == 10);
    drawBottomPath(path, topPadding + i, isEndCap, 0);
    canvas.drawPath(path, fillPaint);
    if (drawShadow) {
      canvas.drawLines(getRightSideShadowLines(0, blockHeight), outlinePaint);
      canvas.drawLines(getBottomShadowLines(blockHeight, true, cornerRadius), outlinePaint);
      canvas.drawLines(getLeftSideShadowLines(blockHeight, i), outlinePaint);
      canvas.drawLines(getRightSideShadowLines(i, topPadding + i), outlinePaint);
      canvas.drawLines(getBottomShadowLines(topPadding + i, (shapeType == 10), 0), outlinePaint);
    } 
    if (drawReflection) {
      canvas.drawLines(getTopReflectionLines(topPadding + i), reflectionPaint);
      canvas.drawLines(getSubstackReflectionLines(i, cornerRadius), reflectionPaint);
    } 
  }
  
  public int getSubstackBottom() {
    return blockHeight + contentHeight + bottomPadding - borderWidth;
  }
  
  public final void drawNumberShape(Canvas canvas) {
    Path path = new Path();
    int i = blockHeight;
    int j = i / 2;
    path.moveTo(j, i);
    i = blockHeight;
    path.arcTo(new RectF(0.0F, 0.0F, i, i), 90.0F, 180.0F);
    path.lineTo((blockWidth - j), 0.0F);
    j = blockWidth;
    i = blockHeight;
    path.arcTo(new RectF((j - i), 0.0F, j, i), 270.0F, 180.0F);
    canvas.drawPath(path, fillPaint);
    if (drawShadow) {
      i = blockWidth;
      j = blockHeight;
      float f = (i - j);
      int k = outlineStrokeWidth;
      canvas.drawArc(new RectF(f, 0.0F, (i - k / 2), (j - k / 2)), 330.0F, 120.0F, false, outlinePaint);
      canvas.drawLines(getNumberBottomShadows(), outlinePaint);
      i = outlineStrokeWidth;
      f = (i / 2 + 0);
      j = blockHeight;
      canvas.drawArc(new RectF(f, 0.0F, j, (j - i / 2)), 90.0F, 30.0F, false, outlinePaint);
    } 
    if (drawReflection) {
      j = outlineStrokeWidth;
      float f1 = (j / 2 + 0);
      float f2 = (j / 2 + 0);
      j = blockHeight;
      canvas.drawArc(new RectF(f1, f2, j, j), 150.0F, 120.0F, false, reflectionPaint);
      canvas.drawLines(getNumberTopReflections(), reflectionPaint);
      i = blockWidth;
      j = blockHeight;
      f1 = (i - j);
      int k = outlineStrokeWidth;
      canvas.drawArc(new RectF(f1, (k / 2 + 0), (i - k / 2), j), 270.0F, 30.0F, false, reflectionPaint);
    } 
  }
  
  public ClassInfo getClassInfo() {
    if (classInfo == null)
      initClassInfo(); 
    return classInfo;
  }
  
  public int getTopH() {
    return blockHeight;
  }
  
  public int getTotalHeight() {
    int i = blockHeight;
    int k = i;
    if (hasSubstack())
      k = i + bottomPadding + contentHeight - borderWidth; 
    i = k;
    if (hasDoubleSubstack())
      i = k + topPadding + innerHeight - borderWidth; 
    int j = shapeType;
    if (j != 4 && j != 7 && j != 10) {
      k = i;
      return (j == 12) ? (i + borderWidth) : k;
    } 
    return i + borderWidth;
  }
  
  public int getTotalWidth() {
    return blockWidth;
  }
  
  public int getW() {
    return blockWidth;
  }
  
  public final void drawRectShape(Canvas canvas) {
    canvas.drawRect(new Rect(0, 0, blockWidth, blockHeight), fillPaint);
    if (drawShadow)
      canvas.drawLines(getRectShadows(), outlinePaint); 
    if (drawReflection)
      canvas.drawLines(getRectReflections(), reflectionPaint); 
  }
  
  public void onDraw(Canvas canvas) {
    fillPaint.setColor(blockColor);
    switch (shapeType) {
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
    index = Math.max(index, minHeight);
    if (index != contentHeight)
      contentHeight = index; 
  }
  
  public void setSubstack2Height(int index) {
    index = Math.max(index, minHeight);
    if (index != innerHeight)
      innerHeight = index; 
  }
}
