package a.a.a;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.besome.sketch.beans.BlockBean;
import java.util.ArrayList;

public class DefinitionBlockView extends BlockView {
  public ArrayList<BlockBean> sa;
  
  public TextView ta;
  
  public DefinitionBlockView(Context paramContext, String paramString1, String paramString2, String paramString3, String paramString4, ArrayList<BlockBean> paramArrayList) {
    super(paramContext, -1, paramString4, paramString1, paramString2, paramString3);
    this.sa = paramArrayList;
    this.oa = 2;
  }
  
  private TextView createLabel(String paramString) {
    TextView textView = new TextView(((BaseBlockView)this).a);
    String str1 = ((BaseBlockView)this).c;
    String str2 = paramString;
    if (str1 != null) {
      str2 = paramString;
      if (str1.length() > 0) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(((BaseBlockView)this).c);
        stringBuilder.append(" : ");
        stringBuilder.append(paramString);
        str2 = stringBuilder.toString();
      } 
    } 
    textView.setText(str2);
    textView.setTextSize(10.0F);
    textView.setPadding(0, 0, 0, 0);
    textView.setGravity(16);
    textView.setTextColor(-1);
    textView.setTypeface(null, 1);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-2, ((BaseBlockView)this).G);
    layoutParams.setMargins(0, 0, 0, 0);
    textView.setLayoutParams((ViewGroup.LayoutParams)layoutParams);
    return textView;
  }
  
  public final int[] b(TextView paramTextView) {
    Rect rect = new Rect();
    paramTextView.getPaint().getTextBounds(paramTextView.getText().toString(), 0, paramTextView.getText().length(), rect);
    return new int[] { rect.width(), rect.height() };
  }
  
  public ArrayList<BlockBean> getData() {
    return this.sa;
  }
  
  public void k() {
    this.ta.setX((float) this.w);
    this.ta.setY((float) this.u);
    int[] textSize = b(this.ta);
    int textWidth = textSize[0];
    int textHeight = textSize[1];
    int width = this.w + textWidth + this.x;
    int yPos = this.u;
    int height = this.G;
    int vPad = this.v;
    String spec = this.c;
    if (spec != null && spec.length() > 0) {
      width = (int) (width + this.D * 8.0f);
    }
    String type = this.b;
    if (type.equals("b") || type.equals("d") || type.equals("s") || type.equals("a")) {
      width = Math.max(width, this.W);
    }
    if (type.equals(" ") || type.equals("") || type.equals("o")) {
      width = Math.max(width, this.aa);
    }
    if (type.equals("c") || type.equals("e")) {
      width = Math.max(width, this.ca);
    }
    int totalHeight = Math.max(yPos + height + vPad, this.u + textHeight + this.v);
    a((float) width, (float) totalHeight, true);
  }
  
  public void l() {
    byte b = 0;
    setDrawingCacheEnabled(false);
    float f1 = this.W;
    float f2 = ((BaseBlockView)this).D;
    this.W = (int)(f1 * f2);
    this.aa = (int)(this.aa * f2);
    this.ba = (int)(this.ba * f2);
    this.ca = (int)(this.ca * f2);
    this.da = (int)(this.da * f2);
    String str = ((BaseBlockView)this).b;
    int i = str.hashCode();
    if (i != 32) {
      if (i != 104) {
        if (i != 108) {
          if (i != 112) {
            if (i != 115) {
              if (i != 118) {
                switch (i) {
                  default:
                    b = -1;
                    break;
                  case 102:
                    if (str.equals("f")) {
                      b = 10;
                      break;
                    } 
                  case 101:
                    if (str.equals("e")) {
                      b = 9;
                      break;
                    } 
                  case 100:
                    if (str.equals("d")) {
                      b = 3;
                      break;
                    } 
                  case 99:
                    if (str.equals("c")) {
                      b = 8;
                      break;
                    } 
                  case 98:
                    if (str.equals("b")) {
                      b = 1;
                      break;
                    } 
                  case 97:
                    if (str.equals("a")) {
                      b = 7;
                      break;
                    } 
                } 
              } else if (str.equals("v")) {
                b = 4;
              } else {
              
              } 
            } else if (str.equals("s")) {
              b = 2;
            } else {
            
            } 
          } else if (str.equals("p")) {
            b = 5;
          } else {
          
          } 
        } else if (str.equals("l")) {
          b = 6;
        } else {
        
        } 
      } else if (str.equals("h")) {
        b = 11;
      } else {
      
      } 
    } else if (str.equals(" ")) {
      switch (b) {
        case 11:
          this.ea = true;
          break;
        case 10:
          this.ga = true;
          break;
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
        case 7:
          this.fa = true;
          break;
      } 
      this.ta = createLabel(this.T);
      addView((View)this.ta);
      ((BaseBlockView)this).e = getResources().getColor(pro.sketchware.R.color.scolor_red_02);
      k();
      return;
    } 
    switch (b) {
      case 11:
        this.ea = true;
        break;
      case 10:
        this.ga = true;
        break;
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
        this.fa = true;
        break;
    } 
    this.ta = createLabel(this.T);
    addView((View)this.ta);
    ((BaseBlockView)this).e = getResources().getColor(pro.sketchware.R.color.scolor_red_02);
    k();
  }
}
