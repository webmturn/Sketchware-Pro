package pro.sketchware.core;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextPaint;
import android.view.ViewGroup;
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
    this.fieldContext = context;
    initSs(context);
  }
  
  private void initSs(Context context) {
    String type = this.blockType;
    switch (type) {
      case "b":
        this.blockColor = 1342177280;
        this.minSimpleWidth = 25;
        break;
      case "d":
        this.blockColor = -657931;
        break;
      case "n":
        this.blockColor = -3155748;
        break;
      case "s":
        this.blockColor = -1;
        break;
      case "m":
        this.blockColor = 805306368;
        break;
    }
    float scale = this.density;
    this.minSimpleWidth = (int) (this.minSimpleWidth * scale);
    this.minHatWidth = (int) (this.minHatWidth * scale);
    this.minCWidth = (int) (this.minCWidth * scale);
    this.spacing = this.minCWidth;
    if (this.blockType.equals("m") && getComponentLabel(this.componentType).length() >= 0) {
      this.dropdownLabel = createLabelTextView(this.componentType);
      addView(this.dropdownLabel);
      this.spacing = getDropdownTypeWidth();
    }
    if (this.blockType.equals("m") || this.blockType.equals("d") || this.blockType.equals("n") || this.blockType.equals("s")) {
      this.labelView = createValueTextView("");
      addView(this.labelView);
    }
    setBlockSize((float) (this.minSimpleWidth + this.spacing), (float) this.textHeight, false);
  }
  
  private int getDropdownTypeWidth() {
    Rect rect = new Rect();
    TextPaint textPaint = this.dropdownLabel.getPaint();
    String labelText = getComponentLabel(this.componentType);
    textPaint.getTextBounds(labelText, 0, labelText.length(), rect);
    return rect.width() + this.minCWidth * 2;
  }
  
  private int getLabelWidth() {
    Rect rect = new Rect();
    this.labelView.getPaint().getTextBounds(this.labelView.getText().toString(), 0, this.labelView.getText().length(), rect);
    return rect.width() + this.minHatWidth;
  }
  
  public final String getComponentLabel(String componentId) {
    String prefix = "";
    String displayName = BlockColorMapper.getComponentDisplayName(componentId);
    componentId = displayName;
    if (displayName.length() > 0) {
      StringBuilder prefixBuilder = new StringBuilder();
      prefixBuilder.append(displayName);
      prefixBuilder.append(" : ");
      prefix = prefixBuilder.toString();
    } 
    return prefix;
  }
  
  public final TextView createLabelTextView(String componentId) {
    TextView textView = new TextView(this.fieldContext);
    textView.setText(getComponentLabel(componentId));
    textView.setTextSize(8.0F);
    textView.setTypeface(null, 1);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-2, this.textHeight);
    int i = this.minCWidth;
    layoutParams.setMargins(i, 0, i, 0);
    textView.setPadding(0, 0, 0, 0);
    textView.setLayoutParams((ViewGroup.LayoutParams)layoutParams);
    textView.setBackgroundColor(0);
    textView.setSingleLine();
    textView.setGravity(17);
    textView.setTextColor(-1);
    return textView;
  }
  
  public final TextView createValueTextView(String text) {
    TextView textView = new TextView(this.fieldContext);
    textView.setText(text);
    textView.setTextSize(9.0F);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(this.minSimpleWidth, this.textHeight);
    layoutParams.setMargins(this.spacing, 0, this.isDefinitionBlock, 0);
    textView.setPadding(0, 0, 0, 0);
    textView.setLayoutParams((ViewGroup.LayoutParams)layoutParams);
    textView.setBackgroundColor(0);
    textView.setSingleLine();
    textView.setGravity(17);
    if (!this.blockType.equals("m")) {
      textView.setTextColor(-268435456);
    } else {
      textView.setTextColor(-251658241);
    } 
    return textView;
  }
  
  public Object getArgValue() {
    return (this.blockType.equals("d") || this.blockType.equals("m") || this.blockType.equals("s")) ? this.labelView.getText() : this.argValue;
  }
  
  public String getMenuName() {
    return this.componentType;
  }
  
  public void setArgValue(Object obj) {
    this.argValue = obj;
    if (this.blockType.equals("d") || this.blockType.equals("m") || this.blockType.equals("s")) {
      this.labelView.setText(obj.toString());
      int i = Math.max(this.minSimpleWidth, getLabelWidth());
      (this.labelView.getLayoutParams()).width = i;
      setBlockSize((i + this.spacing), this.textHeight, true);
    } 
  }
}
