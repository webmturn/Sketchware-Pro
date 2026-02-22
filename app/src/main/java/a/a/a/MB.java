package a.a.a;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Handler;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import java.util.ArrayList;

public class mB {
  public static long a;
  
  public static SpannableStringBuilder a(Context paramContext, String paramString) {
    tB tB = new tB(paramContext);
    tB.a(paramString);
    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(paramString);
    while (true) {
      int i = tB.d();
      if (i != -1) {
        if (i != 2 && i != 3 && i != 4 && i != 5 && i != 6)
          continue; 
        spannableStringBuilder.setSpan(new ForegroundColorSpan(tB.a[i]), tB.b(), tB.b() + tB.a(), 33);
        continue;
      } 
      return spannableStringBuilder;
    } 
  }
  
  public static void a(Context paramContext, EditText paramEditText) {
    ((InputMethodManager)paramContext.getSystemService("input_method")).hideSoftInputFromWindow(paramEditText.getWindowToken(), 0);
  }
  
  public static void a(View paramView) {
    paramView.setEnabled(false);
    (new Handler()).postDelayed(new jB(paramView), 100L);
  }
  
  public static void a(View paramView, int paramInt, Animation.AnimationListener paramAnimationListener) {
    paramView.measure(-1, -2);
    int i = paramView.getMeasuredHeight();
    (paramView.getLayoutParams()).height = 1;
    paramView.setVisibility(0);
    kB kB = new kB(paramView, i);
    if (paramAnimationListener != null)
      kB.setAnimationListener(paramAnimationListener); 
    kB.setDuration(((int)(i / (paramView.getContext().getResources().getDisplayMetrics()).density) * paramInt));
    paramView.startAnimation((Animation)kB);
  }
  
  public static void a(View paramView, Animation.AnimationListener paramAnimationListener) {
    int i = paramView.getMeasuredHeight();
    lB lB = new lB(paramView, i);
    if (paramAnimationListener != null)
      lB.setAnimationListener(paramAnimationListener); 
    lB.setDuration((int)(i / (paramView.getContext().getResources().getDisplayMetrics()).density));
    paramView.startAnimation((Animation)lB);
  }
  
  public static void a(ImageView paramImageView, int paramInt) {
    ColorMatrix colorMatrix = new ColorMatrix();
    colorMatrix.setSaturation(paramInt);
    paramImageView.setColorFilter((ColorFilter)new ColorMatrixColorFilter(colorMatrix));
  }
  
  public static boolean a() {
    if (SystemClock.elapsedRealtime() - a < 100L)
      return true; 
    a = SystemClock.elapsedRealtime();
    return false;
  }
  
  public static SpannableStringBuilder b(Context paramContext, String paramString) {
    IB iB = new IB(paramContext);
    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(paramString);
    iB.b(paramString);
    while (true) {
      int i = iB.c();
      if (i != -1) {
        if (i != 2 && i != 4 && i != 5)
          continue; 
        spannableStringBuilder.setSpan(new ForegroundColorSpan(IB.a[i]), iB.b(), iB.b() + iB.a(), 33);
        continue;
      } 
      ArrayList<int[]> arrayList = iB.a(paramString);
      for (i = 0; i < arrayList.size(); i++) {
        int[] arrayOfInt = arrayList.get(i);
        spannableStringBuilder.setSpan(new ForegroundColorSpan(IB.a[3]), arrayOfInt[0], arrayOfInt[1], 33);
      } 
      return spannableStringBuilder;
    } 
  }
  
  public static void b(View paramView, Animation.AnimationListener paramAnimationListener) {
    a(paramView, 1, paramAnimationListener);
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\mB.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */