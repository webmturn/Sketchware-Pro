package pro.sketchware.core;

import android.content.Context;
import android.graphics.Rect;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.besome.sketch.beans.BlockBean;
import java.util.ArrayList;

public class DefinitionBlockView extends BlockView {
  public ArrayList<BlockBean> blockData;
  
  public TextView definitionLabel;
  
  public DefinitionBlockView(Context context, String key, String value, String extra, String extra2, ArrayList<BlockBean> list) {
    super(context, -1, extra2, key, value, extra);
    blockData = list;
    blockTypeInt = 2;
  }
  
  private TextView createDefinitionLabel(String input) {
    TextView textView = new TextView(context);
    String labelText = input;
    if (componentType != null && componentType.length() > 0) {
      labelText = componentType + " : " + input;
    } 
    textView.setText(labelText);
    textView.setTextSize(10.0F);
    textView.setPadding(0, 0, 0, 0);
    textView.setGravity(16);
    textView.setTextColor(-1);
    textView.setTypeface(null, 1);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-2, textHeight);
    layoutParams.setMargins(0, 0, 0, 0);
    textView.setLayoutParams(layoutParams);
    return textView;
  }
  
  public final int[] measureTextBounds(TextView textView) {
    Rect rect = new Rect();
    textView.getPaint().getTextBounds(textView.getText().toString(), 0, textView.getText().length(), rect);
    return new int[] { rect.width(), rect.height() };
  }
  
  public ArrayList<BlockBean> getData() {
    return blockData;
  }
  
  public void calculateBlockLayout() {
    definitionLabel.setX((float) leftIndent);
    definitionLabel.setY((float) topSpacing);
    int[] textSize = measureTextBounds(definitionLabel);
    int textWidth = textSize[0];
    int measuredTextHeight = textSize[1];
    int width = leftIndent + textWidth + rightIndent;
    int yPos = topSpacing;
    int height = textHeight;
    int vPad = bottomSpacing;
    String compType = componentType;
    if (compType != null && compType.length() > 0) {
      width = (int) (width + density * 8.0f);
    }
    String type = blockType;
    if (type.equals("b") || type.equals("d") || type.equals("s") || type.equals("a")) {
      width = Math.max(width, minBlockWidth);
    }
    if (type.equals(" ") || type.equals("") || type.equals("o")) {
      width = Math.max(width, minSimpleWidth);
    }
    if (type.equals("c") || type.equals("e")) {
      width = Math.max(width, minCWidth);
    }
    int totalHeight = Math.max(yPos + height + vPad, topSpacing + measuredTextHeight + bottomSpacing);
    setBlockSize((float) width, (float) totalHeight, true);
  }
  
  public void initializeBlockDimensions() {
    setDrawingCacheEnabled(false);
    float scale = density;
    minBlockWidth = (int)(minBlockWidth * scale);
    minSimpleWidth = (int)(minSimpleWidth * scale);
    minHatWidth = (int)(minHatWidth * scale);
    minCWidth = (int)(minCWidth * scale);
    spacing = (int)(spacing * scale);
    String type = blockType;
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
        isParameter = true;
        break;
      case 10:
        hasEndCap = true;
        break;
      case 11:
        isDefinitionBlock = true;
        break;
      default:
        break;
    }
    definitionLabel = createDefinitionLabel(spec);
    addView(definitionLabel);
    blockColor = getResources().getColor(pro.sketchware.R.color.scolor_red_02);
    layoutChain();
  }
}
