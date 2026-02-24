package pro.sketchware.core;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

public class UIHelper {
  public static long lastClickTime;
  
  public static SpannableStringBuilder createSpannable(Context context, String text) {
    return new SpannableStringBuilder(text);
  }
  
  public static void hideKeyboard(Context context, EditText editText) {
    ((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
  }
  
  public static void disableTemporarily(View view) {
    view.setEnabled(false);
    (new Handler(Looper.getMainLooper())).postDelayed(new ViewEnableRunnable(view), 100L);
  }
  
  public static void expandView(View view, int duration, Animation.AnimationListener listener) {
    view.measure(-1, -2);
    int targetHeight = view.getMeasuredHeight();
    (view.getLayoutParams()).height = 1;
    view.setVisibility(View.VISIBLE);
    Animation expandAnim = new Animation() {
      @Override
      protected void applyTransformation(float interpolatedTime, Transformation t) {
        view.getLayoutParams().height = interpolatedTime == 1
            ? ViewGroup.LayoutParams.WRAP_CONTENT
            : (int)(targetHeight * interpolatedTime);
        view.requestLayout();
      }
      @Override
      public boolean willChangeBounds() { return true; }
    };
    if (listener != null)
      expandAnim.setAnimationListener(listener);
    expandAnim.setDuration((long)((int)(targetHeight / (view.getContext().getResources().getDisplayMetrics()).density)) * duration);
    view.startAnimation(expandAnim);
  }
  
  public static void collapseView(View view, Animation.AnimationListener listener) {
    int initialHeight = view.getMeasuredHeight();
    Animation collapseAnim = new Animation() {
      @Override
      protected void applyTransformation(float interpolatedTime, Transformation t) {
        if (interpolatedTime == 1) {
          view.setVisibility(View.GONE);
        } else {
          view.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
          view.requestLayout();
        }
      }
      @Override
      public boolean willChangeBounds() { return true; }
    };
    if (listener != null)
      collapseAnim.setAnimationListener(listener);
    collapseAnim.setDuration((int)(initialHeight / (view.getContext().getResources().getDisplayMetrics()).density));
    view.startAnimation(collapseAnim);
  }
  
  public static void setSaturation(ImageView imageView, int saturation) {
    ColorMatrix colorMatrix = new ColorMatrix();
    colorMatrix.setSaturation(saturation);
    imageView.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
  }
  
  public static boolean isClickThrottled() {
    if (SystemClock.elapsedRealtime() - lastClickTime < 100L)
      return true; 
    lastClickTime = SystemClock.elapsedRealtime();
    return false;
  }
  
  public static SpannableStringBuilder createSpannableAlt(Context context, String text) {
    return new SpannableStringBuilder(text);
  }
  
  public static void expandViewDefault(View view, Animation.AnimationListener listener) {
    expandView(view, 1, listener);
  }
}