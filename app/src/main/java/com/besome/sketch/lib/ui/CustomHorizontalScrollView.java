package com.besome.sketch.lib.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public class CustomHorizontalScrollView extends HorizontalScrollView {
  public a scrollChangedListener;
  
  public boolean scrollEnabled = true;
  
  public boolean useScroll = true;
  
  public CustomHorizontalScrollView(Context paramContext) {
    super(paramContext);
  }
  
  public CustomHorizontalScrollView(Context paramContext, AttributeSet paramAttributeSet) {
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
  
  public void onScrollChanged(int paramInt1, int paramInt2, int paramInt3, int paramInt4) {
    super.onScrollChanged(paramInt1, paramInt2, paramInt3, paramInt4);
    a a1 = this.scrollChangedListener;
    if (a1 != null)
      a1.a(paramInt1, paramInt2, paramInt3, paramInt4); 
  }
  
  public boolean onTouchEvent(MotionEvent paramMotionEvent) {
    return (this.useScroll && this.scrollEnabled) ? super.onTouchEvent(paramMotionEvent) : false;
  }
  
  public void setOnScrollChangedListener(a parama) {
    this.scrollChangedListener = parama;
  }
  
  public void setUseScroll(boolean paramBoolean) {
    this.useScroll = paramBoolean;
  }
  
  public static interface a {
    void a(int param1Int1, int param1Int2, int param1Int3, int param1Int4);
  }
}
