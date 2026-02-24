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
  public CircleImageView(Context paramContext) {
    super(paramContext);
  }
  
  public CircleImageView(Context paramContext, AttributeSet paramAttributeSet) {
    super(paramContext, paramAttributeSet);
  }
  
  public Bitmap createCircleBitmap(Bitmap paramBitmap) {
    Bitmap bitmap = Bitmap.createBitmap(paramBitmap.getWidth(), paramBitmap.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    Paint paint = new Paint();
    Rect rect = new Rect(0, 0, paramBitmap.getWidth(), paramBitmap.getHeight());
    paint.setAntiAlias(true);
    canvas.drawARGB(0, 0, 0, 0);
    paint.setColor(-16776961);
    canvas.drawCircle((paramBitmap.getWidth() / 2), (paramBitmap.getHeight() / 2), (paramBitmap.getHeight() / 2), paint);
    paint.setXfermode((Xfermode)new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    canvas.drawBitmap(paramBitmap, rect, rect, paint);
    return bitmap;
  }
  
  public void onDraw(Canvas paramCanvas) {
    BitmapDrawable bitmapDrawable = (BitmapDrawable)getDrawable();
    if (bitmapDrawable == null)
      return; 
    if (getWidth() != 0 && getHeight() != 0) {
      Bitmap bitmap = bitmapDrawable.getBitmap();
      int i = getMeasuredWidth();
      int j = getMeasuredHeight();
      if (bitmap != null) {
        if (i != bitmap.getWidth() || j != bitmap.getHeight())
          bitmap = Bitmap.createScaledBitmap(bitmap, i, j, true); 
        paramCanvas.drawBitmap(createCircleBitmap(bitmap), 0.0F, 0.0F, null);
      } 
    } 
  }
}
