package a.a.a;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextPaint;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Ss extends Ts {
  public Context T;
  
  public Object U = "";
  
  public TextView V;
  
  public TextView W;
  
  public int aa = 20;
  
  public int ba = 4;
  
  public int ca = 2;
  
  public int da = 0;
  
  public int ea = 0;
  
  public Ss(Context paramContext, String paramString1, String paramString2) {
    super(paramContext, paramString1, paramString2, true);
    this.T = paramContext;
    initSs(paramContext);
  }
  
  private void initSs(Context paramContext) {
    String type = this.b;
    switch (type) {
      case "b":
        this.e = 1342177280;
        this.aa = 25;
        break;
      case "d":
        this.e = -657931;
        break;
      case "n":
        this.e = -3155748;
        break;
      case "s":
        this.e = -1;
        break;
      case "m":
        this.e = 805306368;
        break;
    }
    float scale = this.D;
    this.aa = (int) (this.aa * scale);
    this.ba = (int) (this.ba * scale);
    this.ca = (int) (this.ca * scale);
    this.da = this.ca;
    if (this.b.equals("m") && a(this.c).length() >= 0) {
      this.W = b(this.c);
      addView(this.W);
      this.da = getDropdownTypeWidth();
    }
    if (this.b.equals("m") || this.b.equals("d") || this.b.equals("n") || this.b.equals("s")) {
      this.V = c("");
      addView(this.V);
    }
    a((float) (this.aa + this.da), (float) this.G, false);
  }
  
  private int getDropdownTypeWidth() {
    Rect rect = new Rect();
    TextPaint textPaint = this.W.getPaint();
    String str = a(this.c);
    textPaint.getTextBounds(str, 0, str.length(), rect);
    return rect.width() + this.ca * 2;
  }
  
  private int getLabelWidth() {
    Rect rect = new Rect();
    this.V.getPaint().getTextBounds(this.V.getText().toString(), 0, this.V.getText().length(), rect);
    return rect.width() + this.ba;
  }
  
  public final String a(String paramString) {
    String str1;
    String str2 = kq.b(paramString);
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
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-2, this.G);
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
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(this.aa, this.G);
    layoutParams.setMargins(this.da, 0, this.ea, 0);
    textView.setPadding(0, 0, 0, 0);
    textView.setLayoutParams((ViewGroup.LayoutParams)layoutParams);
    textView.setBackgroundColor(0);
    textView.setSingleLine();
    textView.setGravity(17);
    if (!this.b.equals("m")) {
      textView.setTextColor(-268435456);
    } else {
      textView.setTextColor(-251658241);
    } 
    return textView;
  }
  
  public Object getArgValue() {
    return (this.b.equals("d") || this.b.equals("m") || this.b.equals("s")) ? this.V.getText() : this.U;
  }
  
  public String getMenuName() {
    return this.c;
  }
  
  public void setArgValue(Object paramObject) {
    this.U = paramObject;
    if (this.b.equals("d") || this.b.equals("m") || this.b.equals("s")) {
      this.V.setText(paramObject.toString());
      int i = Math.max(this.aa, getLabelWidth());
      (this.V.getLayoutParams()).width = i;
      a((i + this.da), this.G, true);
    } 
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\Ss.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */