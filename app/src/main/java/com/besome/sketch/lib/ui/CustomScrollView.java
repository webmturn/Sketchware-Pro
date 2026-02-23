package com.besome.sketch.lib.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class CustomScrollView extends ScrollView {
  public boolean scrollEnabled = true;
  
  public boolean useScroll = true;
  
  public CustomScrollView(Context paramContext) {
    super(paramContext);
  }
  
  public CustomScrollView(Context paramContext, AttributeSet paramAttributeSet) {
    super(paramContext, paramAttributeSet);
  }
  
  public void a() {
    this.scrollEnabled = false;
  }
  
  public void b() {
    this.scrollEnabled = true;
  }
  
  public boolean onInterceptTouchEvent(MotionEvent paramMotionEvent) {
    return (this.useScroll && this.scrollEnabled) ? super.onInterceptTouchEvent(paramMotionEvent) : false;
  }
  
  public boolean onTouchEvent(MotionEvent paramMotionEvent) {
    return (this.useScroll && this.scrollEnabled) ? super.onTouchEvent(paramMotionEvent) : false;
  }
  
  public void setUseScroll(boolean paramBoolean) {
    this.useScroll = paramBoolean;
  }
}
