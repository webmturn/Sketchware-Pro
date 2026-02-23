package a.a.a;

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
  public static long a;
  
  public static SpannableStringBuilder a(Context paramContext, String paramString) {
    return new SpannableStringBuilder(paramString);
  }
  
  public static void a(Context paramContext, EditText paramEditText) {
    ((InputMethodManager)paramContext.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(paramEditText.getWindowToken(), 0);
  }
  
  public static void a(View paramView) {
    paramView.setEnabled(false);
    (new Handler(Looper.getMainLooper())).postDelayed(new ViewEnableRunnable(paramView), 100L);
  }
  
  public static void a(View paramView, int paramInt, Animation.AnimationListener paramAnimationListener) {
    paramView.measure(-1, -2);
    int targetHeight = paramView.getMeasuredHeight();
    (paramView.getLayoutParams()).height = 1;
    paramView.setVisibility(View.VISIBLE);
    Animation expandAnim = new Animation() {
      @Override
      protected void applyTransformation(float interpolatedTime, Transformation t) {
        paramView.getLayoutParams().height = interpolatedTime == 1
            ? ViewGroup.LayoutParams.WRAP_CONTENT
            : (int)(targetHeight * interpolatedTime);
        paramView.requestLayout();
      }
      @Override
      public boolean willChangeBounds() { return true; }
    };
    if (paramAnimationListener != null)
      expandAnim.setAnimationListener(paramAnimationListener);
    expandAnim.setDuration((long)((int)(targetHeight / (paramView.getContext().getResources().getDisplayMetrics()).density)) * paramInt);
    paramView.startAnimation(expandAnim);
  }
  
  public static void a(View paramView, Animation.AnimationListener paramAnimationListener) {
    int initialHeight = paramView.getMeasuredHeight();
    Animation collapseAnim = new Animation() {
      @Override
      protected void applyTransformation(float interpolatedTime, Transformation t) {
        if (interpolatedTime == 1) {
          paramView.setVisibility(View.GONE);
        } else {
          paramView.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
          paramView.requestLayout();
        }
      }
      @Override
      public boolean willChangeBounds() { return true; }
    };
    if (paramAnimationListener != null)
      collapseAnim.setAnimationListener(paramAnimationListener);
    collapseAnim.setDuration((int)(initialHeight / (paramView.getContext().getResources().getDisplayMetrics()).density));
    paramView.startAnimation(collapseAnim);
  }
  
  public static void a(ImageView paramImageView, int paramInt) {
    ColorMatrix colorMatrix = new ColorMatrix();
    colorMatrix.setSaturation(paramInt);
    paramImageView.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
  }
  
  public static boolean a() {
    if (SystemClock.elapsedRealtime() - a < 100L)
      return true; 
    a = SystemClock.elapsedRealtime();
    return false;
  }
  
  public static SpannableStringBuilder b(Context paramContext, String paramString) {
    return new SpannableStringBuilder(paramString);
  }
  
  public static void b(View paramView, Animation.AnimationListener paramAnimationListener) {
    a(paramView, 1, paramAnimationListener);
  }
}