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
  
  public void onScrollChanged(int x, int y, int paramInt3, int paramInt4) {
    super.onScrollChanged(x, y, paramInt3, paramInt4);
    OnScrollChangedListener a1 = this.scrollChangedListener;
    if (a1 != null)
      a1.onScrollChanged(x, y, paramInt3, paramInt4); 
  }
  
  public boolean onTouchEvent(MotionEvent event) {
    return (this.useScroll && this.scrollEnabled) ? super.onTouchEvent(event) : false;
  }
  
  public void setOnScrollChangedListener(OnScrollChangedListener parama) {
    this.scrollChangedListener = parama;
  }
  
  public void setUseScroll(boolean flag) {
    this.useScroll = flag;
  }
  
  public static interface OnScrollChangedListener {
    void onScrollChanged(int scrollX, int scrollY, int oldScrollX, int oldScrollY);
  }
}
