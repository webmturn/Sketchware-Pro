package com.besome.sketch.lib.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class CustomScrollView extends ScrollView {
  public boolean scrollEnabled = true;
  
  public boolean useScroll = true;
  
  public CustomScrollView(Context context) {
    super(context);
  }
  
  public CustomScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  
  public void disableScroll() {
    this.scrollEnabled = false;
  }
  
  public void enableScroll() {
    this.scrollEnabled = true;
  }
  
  public boolean onInterceptTouchEvent(MotionEvent event) {
    return (this.useScroll && this.scrollEnabled) ? super.onInterceptTouchEvent(event) : false;
  }
  
  public boolean onTouchEvent(MotionEvent event) {
    return (this.useScroll && this.scrollEnabled) ? super.onTouchEvent(event) : false;
  }
  
  public void setUseScroll(boolean flag) {
    this.useScroll = flag;
  }
}
