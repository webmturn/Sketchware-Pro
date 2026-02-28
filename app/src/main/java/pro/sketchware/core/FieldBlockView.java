package pro.sketchware.core;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextPaint;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FieldBlockView extends BaseBlockView {
  public Context fieldContext;
  
  public Object argValue = "";
  
  public TextView labelView;
  
  public TextView dropdownLabel;
  
  public int minSimpleWidth = 20;
  
  public int minHatWidth = 4;
  
  public int minCWidth = 2;
  
  public int spacing = 0;
  
  public int isDefinitionBlock = 0;
  
  public FieldBlockView(Context context, String key, String value) {
    super(context, key, value, true);
    fieldContext = context;
    initSs(context);
  }
  
  private void initSs(Context context) {
    String type = blockType;
    switch (type) {
      case "b":
        blockColor = 1342177280;
        minSimpleWidth = 25;
        break;
      case "d":
        blockColor = -657931;
        break;
      case "n":
        blockColor = -3155748;
        break;
      case "s":
        blockColor = -1;
        break;
      case "m":
        blockColor = 805306368;
        break;
    }
    float scale = density;
    minSimpleWidth = (int) (minSimpleWidth * scale);
    minHatWidth = (int) (minHatWidth * scale);
    minCWidth = (int) (minCWidth * scale);
    spacing = minCWidth;
    if (blockType.equals("m") && getComponentLabel(componentType).length() >= 0) {
      dropdownLabel = createLabelTextView(componentType);
      addView(dropdownLabel);
      spacing = getDropdownTypeWidth();
    }
    if (blockType.equals("m") || blockType.equals("d") || blockType.equals("n") || blockType.equals("s")) {
      labelView = createValueTextView("");
      addView(labelView);
    }
    setBlockSize((float) (minSimpleWidth + spacing), (float) textHeight, false);
  }
  
  private int getDropdownTypeWidth() {
    Rect rect = new Rect();
    TextPaint textPaint = dropdownLabel.getPaint();
    String labelText = getComponentLabel(componentType);
    textPaint.getTextBounds(labelText, 0, labelText.length(), rect);
    return rect.width() + minCWidth * 2;
  }
  
  private int getLabelWidth() {
    Rect rect = new Rect();
    labelView.getPaint().getTextBounds(labelView.getText().toString(), 0, labelView.getText().length(), rect);
    return rect.width() + minHatWidth;
  }
  
  public final String getComponentLabel(String componentId) {
    String displayName = BlockColorMapper.getComponentDisplayName(componentId);
    if (displayName.length() > 0) {
      return displayName + " : ";
    } 
    return "";
  }
  
  public final TextView createLabelTextView(String componentId) {
    TextView textView = new TextView(fieldContext);
    textView.setText(getComponentLabel(componentId));
    textView.setTextSize(8.0F);
    textView.setTypeface(null, 1);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-2, textHeight);
    int margin = minCWidth;
    layoutParams.setMargins(margin, 0, margin, 0);
    textView.setPadding(0, 0, 0, 0);
    textView.setLayoutParams(layoutParams);
    textView.setBackgroundColor(0);
    textView.setSingleLine();
    textView.setGravity(17);
    textView.setTextColor(-1);
    return textView;
  }
  
  public final TextView createValueTextView(String text) {
    TextView textView = new TextView(fieldContext);
    textView.setText(text);
    textView.setTextSize(9.0F);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(minSimpleWidth, textHeight);
    layoutParams.setMargins(spacing, 0, isDefinitionBlock, 0);
    textView.setPadding(0, 0, 0, 0);
    textView.setLayoutParams(layoutParams);
    textView.setBackgroundColor(0);
    textView.setSingleLine();
    textView.setGravity(17);
    if (!blockType.equals("m")) {
      textView.setTextColor(-268435456);
    } else {
      textView.setTextColor(-251658241);
    } 
    return textView;
  }
  
  public Object getArgValue() {
    return (blockType.equals("d") || blockType.equals("m") || blockType.equals("s")) ? labelView.getText() : argValue;
  }
  
  public String getMenuName() {
    return componentType;
  }
  
  public void setArgValue(Object value) {
    argValue = value;
    if (blockType.equals("d") || blockType.equals("m") || blockType.equals("s")) {
      labelView.setText(value.toString());
      int labelWidth = Math.max(minSimpleWidth, getLabelWidth());
      (labelView.getLayoutParams()).width = labelWidth;
      setBlockSize((labelWidth + spacing), textHeight, true);
    } 
  }
}
