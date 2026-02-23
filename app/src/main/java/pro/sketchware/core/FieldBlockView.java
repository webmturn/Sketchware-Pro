package pro.sketchware.core;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextPaint;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FieldBlockView extends BaseBlockView {
  public Context T;
  
  public Object U = "";
  
  public TextView V;
  
  public TextView W;
  
  public int aa = 20;
  
  public int ba = 4;
  
  public int ca = 2;
  
  public int da = 0;
  
  public int ea = 0;
  
  public FieldBlockView(Context paramContext, String paramString1, String paramString2) {
    super(paramContext, paramString1, paramString2, true);
    this.T = paramContext;
    initSs(paramContext);
  }
  
  private void initSs(Context paramContext) {
    String type = this.blockType;
    switch (type) {
      case "b":
        this.blockColor = 1342177280;
        this.aa = 25;
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
    this.aa = (int) (this.aa * scale);
    this.ba = (int) (this.ba * scale);
    this.ca = (int) (this.ca * scale);
    this.da = this.ca;
    if (this.blockType.equals("m") && a(this.componentType).length() >= 0) {
      this.W = b(this.componentType);
      addView(this.W);
      this.da = getDropdownTypeWidth();
    }
    if (this.blockType.equals("m") || this.blockType.equals("d") || this.blockType.equals("n") || this.blockType.equals("s")) {
      this.V = c("");
      addView(this.V);
    }
    a((float) (this.aa + this.da), (float) this.textHeight, false);
  }
  
  private int getDropdownTypeWidth() {
    Rect rect = new Rect();
    TextPaint textPaint = this.W.getPaint();
    String str = a(this.componentType);
    textPaint.getTextBounds(str, 0, str.length(), rect);
    return rect.width() + this.ca * 2;
  }
  
  private int getLabelWidth() {
    Rect rect = new Rect();
    this.V.getPaint().getTextBounds(this.V.getText().toString(), 0, this.V.getText().length(), rect);
    return rect.width() + this.ba;
  }
  
  public final String a(String paramString) {
    String str1 = "";
    String str2 = BlockColorMapper.b(paramString);
    paramString = str2;
    if (str2.length() > 0) {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(str2);
      stringBuilder.append(" : ");
      str1 = stringBuilder.toString();
    } 
    return str1;
  }
  
  public final TextView b(String paramString) {
    TextView textView = new TextView(this.T);
    textView.setText(a(paramString));
    textView.setTextSize(8.0F);
    textView.setTypeface(null, 1);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-2, this.textHeight);
    int i = this.ca;
    layoutParams.setMargins(i, 0, i, 0);
    textView.setPadding(0, 0, 0, 0);
    textView.setLayoutParams((ViewGroup.LayoutParams)layoutParams);
    textView.setBackgroundColor(0);
    textView.setSingleLine();
    textView.setGravity(17);
    textView.setTextColor(-1);
    return textView;
  }
  
  public final TextView c(String paramString) {
    TextView textView = new TextView(this.T);
    textView.setText(paramString);
    textView.setTextSize(9.0F);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(this.aa, this.textHeight);
    layoutParams.setMargins(this.da, 0, this.ea, 0);
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
    return (this.blockType.equals("d") || this.blockType.equals("m") || this.blockType.equals("s")) ? this.V.getText() : this.U;
  }
  
  public String getMenuName() {
    return this.componentType;
  }
  
  public void setArgValue(Object paramObject) {
    this.U = paramObject;
    if (this.blockType.equals("d") || this.blockType.equals("m") || this.blockType.equals("s")) {
      this.V.setText(paramObject.toString());
      int i = Math.max(this.aa, getLabelWidth());
      (this.V.getLayoutParams()).width = i;
      a((i + this.da), this.textHeight, true);
    } 
  }
}
