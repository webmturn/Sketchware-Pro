package com.besome.sketch.lib.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public class CustomHorizontalScrollView extends HorizontalScrollView {
  public a a;
  
  public boolean b = true;
  
  public boolean c = true;
  
  public CustomHorizontalScrollView(Context paramContext) {
    super(paramContext);
  }
  
  public CustomHorizontalScrollView(Context paramContext, AttributeSet paramAttributeSet) {
    super(paramContext, paramAttributeSet);
  }
  
  public void a() {
    this.b = false;
  }
  
  public void b() {
    this.b = true;
  }
  
  public boolean onInterceptTouchEvent(MotionEvent paramMotionEvent) {
    return (this.c && this.b) ? super.onInterceptTouchEvent(paramMotionEvent) : false;
  }
  
  public void onScrollChanged(int paramInt1, int paramInt2, int paramInt3, int paramInt4) {
    super.onScrollChanged(paramInt1, paramInt2, paramInt3, paramInt4);
    a a1 = this.a;
    if (a1 != null)
      a1.a(paramInt1, paramInt2, paramInt3, paramInt4); 
  }
  
  public boolean onTouchEvent(MotionEvent paramMotionEvent) {
    return (this.c && this.b) ? super.onTouchEvent(paramMotionEvent) : false;
  }
  
  public void setOnScrollChangedListener(a parama) {
    this.a = parama;
  }
  
  public void setUseScroll(boolean paramBoolean) {
    this.c = paramBoolean;
  }
  
  public static interface a {
    void a(int param1Int1, int param1Int2, int param1Int3, int param1Int4);
  }
}
