package com.besome.sketch.lib.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

public class CircleImageView extends AppCompatImageView {
  public CircleImageView(Context context) {
    super(context);
  }
  
  public CircleImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  
  public Bitmap createCircleBitmap(Bitmap sourceBitmap) {
    Bitmap bitmap = Bitmap.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    Paint paint = new Paint();
    Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
    paint.setAntiAlias(true);
    canvas.drawARGB(0, 0, 0, 0);
    paint.setColor(-16776961);
    canvas.drawCircle((bitmap.getWidth() / 2), (bitmap.getHeight() / 2), (bitmap.getHeight() / 2), paint);
    paint.setXfermode((Xfermode)new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    canvas.drawBitmap(bitmap, rect, rect, paint);
    return bitmap;
  }
  
  public void onDraw(Canvas canvas) {
    BitmapDrawable bitmapDrawable = (BitmapDrawable)getDrawable();
    if (bitmapDrawable == null)
      return; 
    if (getWidth() != 0 && getHeight() != 0) {
      Bitmap bitmap = bitmapDrawable.getBitmap();
      int measuredWidth = getMeasuredWidth();
      int measuredHeight = getMeasuredHeight();
      if (bitmap != null) {
        if (measuredWidth != bitmap.getWidth() || measuredHeight != bitmap.getHeight())
          bitmap = Bitmap.createScaledBitmap(bitmap, measuredWidth, measuredHeight, true); 
        canvas.drawBitmap(createCircleBitmap(bitmap), 0.0F, 0.0F, null);
      } 
    } 
  }
}
