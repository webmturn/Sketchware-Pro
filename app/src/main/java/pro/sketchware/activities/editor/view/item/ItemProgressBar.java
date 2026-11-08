package pro.sketchware.activities.editor.view.item;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import java.util.HashMap;
import java.util.Map;
import pro.sketchware.activities.editor.view.ItemView;
import pro.sketchware.activities.editor.view.PreviewValues;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.layout.ProgressBarAttributes;
import pro.sketchware.util.ViewUtil;
import pro.sketchware.util.format.FormatUtil;

public class ItemProgressBar extends LinearLayout implements ItemView {

    private ViewBean viewBean;
    private boolean isSelected;
    private boolean isFixed;
    private Paint paint;
    private float dip;
    private ProgressBar progressBar;
    private boolean horizontal;
    private int styleAttr;
    private int styleRes;
    private ColorStateList progressTint;
    private ColorStateList backgroundTint;
    private ColorStateList secondaryTint;
    private ColorStateList indeterminateTint;

    public ItemProgressBar(Context context) {
        super(context);
        initialize(context);
    }

    private void initialize(Context context) {
        dip = ViewUtil.dpToPx(context, 1.0F);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x9599d5d0);
        setWillNotDraw(false);
        setGravity(Gravity.CENTER);
        updateStyle(false, android.R.attr.progressBarStyle, 0);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        pro.sketchware.activities.editor.view.PreviewPadding.onLayoutDirectionChanged(
                this, layoutDirection);
    }

    @Override
    public ViewBean getBean() {
        return viewBean;
    }

    @Override
    public void setBean(ViewBean viewBean) {
        this.viewBean = viewBean;
    }

    @Override
    public boolean getFixed() {
        return isFixed;
    }

    @Override
    public void setFixed(boolean isFixed) {
        this.isFixed = isFixed;
    }

    public boolean getSelection() {
        return isSelected;
    }

    @Override
    public void setSelection(boolean selection) {
        isSelected = selection;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (isSelected) {
            canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), paint);
        }
        super.onDraw(canvas);
    }

    @Override
    public void setPaddingPixels(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(
                (int) (left * dip), (int) (top * dip), (int) (right * dip), (int) (bottom * dip));
    }

    public void applyPreviewAttributes(
            ViewBean bean, Map<String, String> injected, PreviewValues values) {
        Map<String, String> xmlAttributes = ProgressBarAttributes.generated(bean, injected);
        xmlAttributes.putAll(injected);
        String styleName = xmlAttributes.get("style");
        PreviewValues.Style style = values.resolveStyle(styleName);
        applyStyle(style.base(), styleName == null ? null : bean.progressStyle);

        Map<String, String> effectiveAttributes = new HashMap<>(style.attributes());
        effectiveAttributes.putAll(xmlAttributes);
        Map<String, String> resolvedAttributes = values.resolveAttributes(effectiveAttributes);
        applyProgress(resolvedAttributes, bean.max);
        applyTints(resolvedAttributes, values);
    }

    private void applyProgress(Map<String, String> attributes, int configuredMax) {
        int min = FormatUtil.safeParseInt(attributes.get("android:min"), 0);
        String maxValue = attributes.get("android:max");
        int max =
                maxValue == null
                        ? ViewBean.DEFAULT_MAX
                        : FormatUtil.safeParseInt(maxValue, configuredMax);
        max = Math.max(min, max);
        // Each setter clamps against the current opposite bound.
        if (min > progressBar.getMax()) {
            progressBar.setMax(max);
            progressBar.setMin(min);
        } else {
            progressBar.setMin(min);
            progressBar.setMax(max);
        }
        boolean indeterminate =
                "true".equals(attributes.get("android:indeterminateOnly"))
                        || "true".equals(attributes.get("android:indeterminate"));
        progressBar.setIndeterminate(indeterminate);
        progressBar.setProgress(
                FormatUtil.safeParseInt(
                        attributes.get("android:progress"), ViewBean.DEFAULT_PROGRESS));
        progressBar.setSecondaryProgress(
                FormatUtil.safeParseInt(attributes.get("android:secondaryProgress"), min));
    }

    private void applyTints(Map<String, String> attributes, PreviewValues values) {
        progressBar.setProgressTintList(
                tint(attributes.get("android:progressTint"), progressTint, values));
        progressBar.setProgressBackgroundTintList(
                tint(attributes.get("android:progressBackgroundTint"), backgroundTint, values));
        progressBar.setSecondaryProgressTintList(
                tint(attributes.get("android:secondaryProgressTint"), secondaryTint, values));
        progressBar.setIndeterminateTintList(
                tint(attributes.get("android:indeterminateTint"), indeterminateTint, values));
    }

    private static ColorStateList tint(
            String value, ColorStateList fallback, PreviewValues values) {
        if (value == null) return fallback;
        if (value.equals("@null")) return null;
        Integer color = values.color(value);
        return color == null ? fallback : ColorStateList.valueOf(color);
    }

    private void applyStyle(String style, String fallback) {
        int attr = 0;
        int res = 0;
        if (style.startsWith("?android:")) {
            String name = style.substring("?android:".length()).replaceFirst("^attr/", "");
            if (name.startsWith("progressBarStyle")) {
                attr = getResources().getIdentifier(name, "attr", "android");
            }
        } else if (style.startsWith("@android:style/")) {
            res = getResources().getIdentifier(style.substring(1), null, null);
        }
        if (attr == 0 && res == 0) {
            boolean horizontal = ViewBean.PROGRESSBAR_STYLE_HORIZONTAL.equals(fallback);
            updateStyle(
                    horizontal,
                    horizontal
                            ? android.R.attr.progressBarStyleHorizontal
                            : android.R.attr.progressBarStyle,
                    0);
        } else {
            updateStyle(
                    attr == android.R.attr.progressBarStyleHorizontal
                            || style.endsWith(".Horizontal"),
                    attr,
                    res);
        }
    }

    private void updateStyle(boolean horizontal, int styleAttr, int styleRes) {
        if (progressBar != null) {
            if (this.horizontal == horizontal
                    && this.styleAttr == styleAttr
                    && this.styleRes == styleRes) {
                return;
            }
            removeView(progressBar);
        }
        this.horizontal = horizontal;
        this.styleAttr = styleAttr;
        this.styleRes = styleRes;
        progressBar = new ProgressBar(getContext(), null, styleAttr, styleRes);
        progressTint = progressBar.getProgressTintList();
        backgroundTint = progressBar.getProgressBackgroundTintList();
        secondaryTint = progressBar.getSecondaryProgressTintList();
        indeterminateTint = progressBar.getIndeterminateTintList();
        addView(
                progressBar,
                new LayoutParams(
                        horizontal
                                ? ViewGroup.LayoutParams.MATCH_PARENT
                                : ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}
