package pro.sketchware.core.block.view;

import pro.sketchware.core.project.ClassInfo;
import pro.sketchware.core.codegen.ComponentTypeMapper;
import pro.sketchware.util.ViewUtil;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.widget.RelativeLayout;

public class BaseBlockView extends RelativeLayout {
  private static final int SHAPE_RECTANGULAR = 1;
  private static final int SHAPE_BOOLEAN = 2;
  private static final int SHAPE_NUMBER = 3;
  private static final int SHAPE_STATEMENT = 4;
  private static final int SHAPE_END_CAP = 5;
  private static final int SHAPE_HAT = 7;
  private static final int SHAPE_PARAMETER = 9;
  private static final int SHAPE_SINGLE_SUBSTACK = 10;
  // ID 11 is rendered as a single substack but is not assigned by constructors.
  private static final int SHAPE_SINGLE_SUBSTACK_ALTERNATE = 11;
  private static final int SHAPE_DOUBLE_SUBSTACK = 12;

  private static Paint disabledOverlayPaint;
  private static Paint disabledStripePaint;

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
  
  public BaseBlockView(Context context, String blockType, String typeName, boolean isEditable) {
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
    this.blockType = blockType;
    if (typeName != null && typeName.indexOf(".") > 0) {
      this.componentType = typeName.substring(0, typeName.indexOf("."));
    } else {
      this.componentType = typeName;
    }
    initClassInfo();
    String type = this.blockType;
    switch (type) {
      case " ": topSpacing = 4; shapeType = SHAPE_STATEMENT; break;
      case "b": leftIndent = 8; rightIndent = 5; shapeType = SHAPE_BOOLEAN; break;
      case "d": shapeType = SHAPE_NUMBER; leftIndent = 4; break;
      case "n": shapeType = SHAPE_NUMBER; break;
      case "c": topSpacing = 4; shapeType = SHAPE_SINGLE_SUBSTACK; break;
      case "e": topSpacing = 4; shapeType = SHAPE_DOUBLE_SUBSTACK; break;
      case "f": topSpacing = 4; shapeType = SHAPE_END_CAP; break;
      case "h": topSpacing = 8; shapeType = SHAPE_HAT; break;
      case "m": shapeType = SHAPE_PARAMETER; break;
      case "s":
      case "v":
      case "p":
      case "l":
      case "a": shapeType = SHAPE_RECTANGULAR; break;
    }
    blockColor = defaultBlockColor;
    this.isEditable = isEditable;
    setWillNotDraw(false);
    initDensityAndPaints(context);
  }
  
  public BaseBlockView(Context context, String blockType, boolean isEditable) {
    this(context, blockType, "", isEditable);
  }
  
  private float[] getBooleanReflections() {
    int halfHeight = blockHeight / 2;
    int halfStroke = outlineStrokeWidth / 2;
    return new float[] {
        halfStroke, halfHeight, halfHeight, halfStroke,
        halfHeight, halfStroke, blockWidth - halfHeight, halfStroke };
  }
  
  private float[] getBooleanShadows() {
    int halfHeight = blockHeight / 2;
    int halfStroke = outlineStrokeWidth / 2;
    int right = blockWidth - halfStroke;
    int bottom = blockHeight - halfStroke;
    int rightCorner = blockWidth - halfHeight;
    return new float[] {
        right, halfHeight, rightCorner, bottom,
        rightCorner, bottom, halfHeight, bottom };
  }
  
  private float[] getNumberBottomShadows() {
    int halfHeight = blockHeight / 2;
    int bottom = blockHeight - outlineStrokeWidth / 2;
    return new float[] { blockWidth - halfHeight, bottom, halfHeight, bottom };
  }
  
  private float[] getNumberTopReflections() {
    int halfHeight = blockHeight / 2;
    int halfStroke = outlineStrokeWidth / 2;
    return new float[] { halfHeight, halfStroke, blockWidth - halfHeight, halfStroke };
  }
  
  private float[] getRectReflections() {
    int halfStroke = outlineStrokeWidth / 2;
    return new float[] {
        0.0F, halfStroke, blockWidth - halfStroke, halfStroke,
        halfStroke, 0.0F, halfStroke, blockHeight - halfStroke };
  }
  
  private float[] getRectShadows() {
    int halfStroke = outlineStrokeWidth / 2;
    int right = blockWidth - halfStroke;
    int bottom = blockHeight - halfStroke;
    return new float[] { right, 0.0F, right, bottom, right, bottom, 0.0F, bottom };
  }
  
  public void initClassInfo() {
    classInfo = ComponentTypeMapper.getClassInfo(blockType, componentType);
  }
  
  public void setBlockSize(float x, float y, boolean flag) {
    if (shapeType == SHAPE_PARAMETER) {
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
        shapeType = SHAPE_STATEMENT;
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
    setLayerType(View.LAYER_TYPE_SOFTWARE, null);
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
  
  public final float[] getTopReflectionLines(int height) {
    // Keep integer division before conversion to float for pixel alignment.
    int halfStroke = outlineStrokeWidth / 2;
    int connectorY = halfStroke + borderWidth;
    return new float[] {
        halfStroke, height - notchWidth, halfStroke, notchWidth,
        halfStroke, notchWidth, notchWidth, halfStroke,
        notchWidth, halfStroke, connectorOffset, halfStroke,
        connectorStart, connectorY, connectorEnd, connectorY,
        connectorEnd, connectorY, connectorEndOffset, halfStroke,
        connectorEndOffset, halfStroke, blockWidth - notchWidth, halfStroke };
  }
  
  public final float[] getLeftSideShadowLines(int start, int end) {
    int halfStroke = outlineStrokeWidth / 2;
    int left = cornerRadius - halfStroke;
    int top = start + notchDepth;
    return new float[] {
        cornerRadius + notchDepth, start - halfStroke, left, top,
        left, top, left, end - notchDepth };
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
    boolean hasBottomNotch = (shapeType != SHAPE_END_CAP);
    drawBottomPath(path, i, hasBottomNotch, 0);
    canvas.drawPath(path, fillPaint);
    if (drawShadow) {
      canvas.drawLines(getRightSideShadowLines(0, blockHeight), outlinePaint);
      canvas.drawLines(getBottomShadowLines(blockHeight, (shapeType != SHAPE_END_CAP), 0), outlinePaint);
    } 
    if (drawReflection)
      canvas.drawLines(getTopReflectionLines(blockHeight), reflectionPaint); 
  }
  
  public boolean hasSubstack() {
    return shapeType >= SHAPE_SINGLE_SUBSTACK;
  }
  
  public final float[] getRightSideShadowLines(int start, int end) {
    int right = blockWidth - outlineStrokeWidth / 2;
    return new float[] { right, start + notchWidth, right, end - notchWidth };
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
    return shapeType == SHAPE_DOUBLE_SUBSTACK;
  }
  
  public final float[] getSubstackReflectionLines(int start, int end) {
    int halfStroke = outlineStrokeWidth / 2;
    return new float[] {
        end + notchDepth, halfStroke + start, blockWidth - notchWidth, start + halfStroke };
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
    boolean hasBottomNotch = (shapeType == SHAPE_SINGLE_SUBSTACK);
    drawBottomPath(path, topPadding + i, hasBottomNotch, 0);
    canvas.drawPath(path, fillPaint);
    if (drawShadow) {
      canvas.drawLines(getRightSideShadowLines(0, blockHeight), outlinePaint);
      canvas.drawLines(getBottomShadowLines(blockHeight, true, cornerRadius), outlinePaint);
      canvas.drawLines(getLeftSideShadowLines(blockHeight, i), outlinePaint);
      canvas.drawLines(getRightSideShadowLines(i, topPadding + i), outlinePaint);
      canvas.drawLines(getBottomShadowLines(topPadding + i, (shapeType == SHAPE_SINGLE_SUBSTACK), 0), outlinePaint);
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
    int height = blockHeight;
    if (hasSubstack())
      height = height + bottomPadding + contentHeight - borderWidth;
    if (hasDoubleSubstack())
      height = height + topPadding + innerHeight - borderWidth;
    if (shapeType == SHAPE_STATEMENT || shapeType == SHAPE_HAT
        || shapeType == SHAPE_SINGLE_SUBSTACK || shapeType == SHAPE_DOUBLE_SUBSTACK)
      return height + borderWidth;
    return height;
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
      case SHAPE_DOUBLE_SUBSTACK:
        drawDoubleSubstackShape(canvas);
        break;
      case SHAPE_SINGLE_SUBSTACK:
      case SHAPE_SINGLE_SUBSTACK_ALTERNATE:
        drawSingleSubstackShape(canvas);
        break;
      case SHAPE_PARAMETER:
        drawParameterShape(canvas);
        break;
      case SHAPE_HAT:
        drawHatShape(canvas);
        break;
      case SHAPE_STATEMENT:
      case SHAPE_END_CAP:
        drawStatementShape(canvas);
        break;
      case SHAPE_NUMBER:
        drawNumberShape(canvas);
        break;
      case SHAPE_BOOLEAN:
        drawBooleanShape(canvas);
        break;
      case SHAPE_RECTANGULAR:
        drawRectShape(canvas);
        break;
    }
    super.onDraw(canvas);
  }
  
  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    if (this instanceof BlockView && isEffectivelyDisabled((BlockView) this)) {
      if (disabledOverlayPaint == null) {
        disabledOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        disabledOverlayPaint.setColor(0x60000000);
      }
      canvas.drawRect(0, 0, getTotalWidth(), getTotalHeight(), disabledOverlayPaint);
      if (disabledStripePaint == null) {
        disabledStripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        disabledStripePaint.setColor(0x40FFFFFF);
        disabledStripePaint.setStrokeWidth(density * 2);
      }
      int step = (int) (density * 8);
      int w = getTotalWidth();
      int h = getTotalHeight();
      for (int i = -h; i < w; i += step) {
        canvas.drawLine(i, h, i + h, 0, disabledStripePaint);
      }
    }
  }
  
  private static boolean isEffectivelyDisabled(BlockView blockView) {
    if (blockView.disabled) return true;
    BlockView child = blockView;
    BlockView parent = blockView.parentBlock;
    int limit = 200;
    while (parent != null && --limit > 0) {
      int childTag = ((Integer) child.getTag()).intValue();
      boolean isSubStackChild = (parent.subStack1 == childTag || parent.subStack2 == childTag);
      boolean isNextBlockChild = (parent.nextBlock == childTag);
      if (!isSubStackChild && !isNextBlockChild) {
        // child is a parameter of parent — parent's disabled state applies
        if (parent.disabled) return true;
      } else if (isSubStackChild) {
        if (parent.disabled) return true;
      }
      // for nextBlock, skip disabled check and continue up
      child = parent;
      parent = parent.parentBlock;
    }
    return false;
  }
  
  public void onMeasure(int start, int end) {
    super.onMeasure(View.MeasureSpec.makeMeasureSpec(getTotalWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(getTotalHeight(), View.MeasureSpec.EXACTLY));
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
