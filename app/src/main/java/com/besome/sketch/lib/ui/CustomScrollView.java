package com.besome.sketch.lib.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class CustomScrollView extends ScrollView {
  public boolean a = true;
  
  public boolean b = true;
  
  public CustomScrollView(Context paramContext) {
    super(paramContext);
  }
  
  public CustomScrollView(Context paramContext, AttributeSet paramAttributeSet) {
    super(paramContext, paramAttributeSet);
  }
  
  public void a() {
    this.a = false;
  }
  
  public void b() {
    this.a = true;
  }
  
  public boolean onInterceptTouchEvent(MotionEvent paramMotionEvent) {
    return (this.b && this.a) ? super.onInterceptTouchEvent(paramMotionEvent) : false;
  }
  
  public boolean onTouchEvent(MotionEvent paramMotionEvent) {
    return (this.b && this.a) ? super.onTouchEvent(paramMotionEvent) : false;
  }
  
  public void setUseScroll(boolean paramBoolean) {
    this.b = paramBoolean;
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\com.besome.sketch-classes.jar!\com\besome\sketch\li\\ui\CustomScrollView.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */