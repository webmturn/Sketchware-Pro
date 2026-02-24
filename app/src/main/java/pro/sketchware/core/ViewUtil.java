package pro.sketchware.core;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;

public class ViewUtil {

    public static float dpToPx(Context context, float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    public static View inflateLayout(Context context, @LayoutRes int layoutRes) {
        return ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutRes, null);
    }

    public static View inflateLayoutInto(Context context, ViewGroup parent, @LayoutRes int layoutRes) {
        return ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutRes, parent, true);
    }
}
