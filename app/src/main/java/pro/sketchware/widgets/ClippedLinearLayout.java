package pro.sketchware.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Keeps content at its natural height while an animated container clips its visible area. */
public class ClippedLinearLayout extends LinearLayout {
    public ClippedLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Stable child specs keep the event list and action buttons from being remeasured and
        // compressed as the reveal height changes. The parent still sees the animated height.
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        setMeasuredDimension(getMeasuredWidthAndState(), resolveSizeAndState(getMeasuredHeight(),
                heightMeasureSpec, getMeasuredState() << MEASURED_HEIGHT_STATE_SHIFT));
    }
}
