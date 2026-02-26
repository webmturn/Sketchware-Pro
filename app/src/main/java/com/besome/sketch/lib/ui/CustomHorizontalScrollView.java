package com.besome.sketch.lib.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public class CustomHorizontalScrollView extends HorizontalScrollView {
  public OnScrollChangedListener scrollChangedListener;
  
  public boolean scrollEnabled = true;
  
  public boolean useScroll = true;
  
  public CustomHorizontalScrollView(Context context) {
    super(context);
  }
  
  public CustomHorizontalScrollView(Context context, AttributeSet attrs) {
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
  
  public void onScrollChanged(int x, int y, int width, int height) {
    super.onScrollChanged(x, y, width, height);
    OnScrollChangedListener scrollListener = this.scrollChangedListener;
    if (scrollListener != null)
      scrollListener.onScrollChanged(x, y, width, height); 
  }
  
  public boolean onTouchEvent(MotionEvent event) {
    return (this.useScroll && this.scrollEnabled) ? super.onTouchEvent(event) : false;
  }
  
  public void setOnScrollChangedListener(OnScrollChangedListener listener) {
    this.scrollChangedListener = listener;
  }
  
  public void setUseScroll(boolean flag) {
    this.useScroll = flag;
  }
  
  public static interface OnScrollChangedListener {
    void onScrollChanged(int scrollX, int scrollY, int oldScrollX, int oldScrollY);
  }
}
