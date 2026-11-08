package pro.sketchware.activities.editor.view.item;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.TypedValue;
import android.view.View;
import java.util.Map;
import java.util.function.Function;
import pro.sketchware.R;
import pro.sketchware.activities.editor.view.ItemView;
import pro.sketchware.activities.editor.view.PreviewPadding;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.util.PropertiesUtil;
import pro.sketchware.util.ThemeUtils;
import pro.sketchware.util.ViewUtil;
import pro.sketchware.util.format.FormatUtil;

public class ItemPatternLockView extends View implements ItemView {
    private static final int DEFAULT_DOT_COUNT = 3;
    private static final int MAX_PREVIEW_DOT_COUNT = 32;
    private static final int DEFAULT_SIZE_DP = 240;

    private final Paint paint;
    private final Path pattern = new Path();
    private final float paddingFactor;
    private final int defaultDotColor;
    private final int defaultPatternColor;
    private ViewBean viewBean;
    private boolean hasSelection;
    private boolean hasFixed;
    private int dotCount = DEFAULT_DOT_COUNT;
    private float dotSize;
    private float selectedDotSize;
    private float pathWidth;
    private int dotColor;
    private int patternColor;
    private boolean aspectRatioEnabled;
    private String aspectRatio = "square";

    public ItemPatternLockView(Context context) {
        super(context);
        paddingFactor = ViewUtil.dpToPx(context, 1.0f);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        defaultDotColor = ThemeUtils.getColor(context, R.attr.colorOnSurface);
        defaultPatternColor = ThemeUtils.getColor(context, R.attr.colorPrimary);
        dotColor = defaultDotColor;
        patternColor = defaultPatternColor;
        dotSize = 10 * paddingFactor;
        selectedDotSize = 24 * paddingFactor;
        pathWidth = 3 * paddingFactor;
    }

    public void applyPreviewAttributes(
            Map<String, String> attributes, Function<String, Integer> resolveColor) {
        int count = FormatUtil.safeParseInt(attributes.get("app:dotCount"), DEFAULT_DOT_COUNT);
        dotCount = count > 0 ? Math.min(count, MAX_PREVIEW_DOT_COUNT) : DEFAULT_DOT_COUNT;
        dotSize = dimension(attributes.get("app:dotNormalSize"), 10);
        selectedDotSize = dimension(attributes.get("app:dotSelectedSize"), 24);
        pathWidth = dimension(attributes.get("app:pathWidth"), 3);
        String normalValue = attributes.get("app:normalStateColor");
        String correctValue = attributes.get("app:correctStateColor");
        Integer normal = normalValue != null ? resolveColor.apply(normalValue.trim()) : null;
        Integer correct = correctValue != null ? resolveColor.apply(correctValue.trim()) : null;
        dotColor = normal != null ? normal : defaultDotColor;
        patternColor = correct != null ? correct : defaultPatternColor;

        boolean enabled =
                "true".equals(attributes.getOrDefault("app:aspectRatioEnabled", "").trim());
        String ratio = attributes.getOrDefault("app:aspectRatio", "square").trim();
        if (enabled != aspectRatioEnabled || !ratio.equals(aspectRatio)) {
            aspectRatioEnabled = enabled;
            aspectRatio = ratio;
            requestLayout();
        }
        invalidate();
    }

    private float dimension(String value, float defaultDp) {
        float fallback = defaultDp * paddingFactor;
        if (value == null) return fallback;
        var match = PropertiesUtil.UNIT_PATTERN.matcher(value.trim().replaceFirst("dip$", "dp"));
        if (!match.matches()) return fallback;
        float size = FormatUtil.safeParseFloat(match.group(1), -1);
        if (!Float.isFinite(size) || size < 0) return fallback;
        int unit =
                switch (match.group(2)) {
                    case "dp" -> TypedValue.COMPLEX_UNIT_DIP;
                    case "sp" -> TypedValue.COMPLEX_UNIT_SP;
                    case "mm" -> TypedValue.COMPLEX_UNIT_MM;
                    case "pt" -> TypedValue.COMPLEX_UNIT_PT;
                    case "in" -> TypedValue.COMPLEX_UNIT_IN;
                    default -> TypedValue.COMPLEX_UNIT_PX;
                };
        return TypedValue.applyDimension(unit, size, getResources().getDisplayMetrics());
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        PreviewPadding.onLayoutDirectionChanged(this, layoutDirection);
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
        return hasFixed;
    }

    @Override
    public void setFixed(boolean fixed) {
        hasFixed = fixed;
    }

    public boolean getSelection() {
        return hasSelection;
    }

    @Override
    public void setSelection(boolean selected) {
        hasSelection = selected;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The bundled PatternLockView takes the parent size when constrained.
        // Keep a visible sample when the editor measures without a height limit.
        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int verticalPadding = getPaddingTop() + getPaddingBottom();
        int width =
                getDefaultSize(
                        Math.max(
                                getSuggestedMinimumWidth(),
                                Math.round(DEFAULT_SIZE_DP * paddingFactor) + horizontalPadding),
                        widthMeasureSpec);
        int height =
                getDefaultSize(
                        Math.max(
                                getSuggestedMinimumHeight(),
                                Math.max(0, width - horizontalPadding) + verticalPadding),
                        heightMeasureSpec);
        if (aspectRatioEnabled) {
            int shorter = Math.min(width, height);
            switch (aspectRatio) {
                case "width_bias", "1" -> height = shorter;
                case "height_bias", "2" -> width = shorter;
                default -> width = height = shorter;
            }
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (hasSelection) {
            paint.setColor(0x9599d5d0);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        }
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        if (width <= 0 || height <= 0) return;

        float cellWidth = (float) width / dotCount;
        float cellHeight = (float) height / dotCount;
        float left = getPaddingLeft() + cellWidth / 2;
        float top = getPaddingTop() + cellHeight / 2;
        float lastX = left + (dotCount - 1) * cellWidth;
        float lastY = top + (dotCount - 1) * cellHeight;
        float cellSize = Math.min(cellWidth, cellHeight);
        float radius = Math.min(dotSize, cellSize) / 2;
        float selectedRadius = Math.min(selectedDotSize, cellSize) / 2;

        if (pathWidth > 0 && dotCount > 1) {
            pattern.rewind();
            pattern.moveTo(left, top);
            pattern.lineTo(left, lastY);
            pattern.lineTo(lastX, lastY);
            paint.setColor(patternColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.min(pathWidth, cellSize));
            canvas.drawPath(pattern, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        for (int row = 0; row < dotCount; row++) {
            float y = top + row * cellHeight;
            for (int column = 0; column < dotCount; column++) {
                boolean selected = column == 0 || row == dotCount - 1;
                paint.setColor(selected ? patternColor : dotColor);
                canvas.drawCircle(
                        left + column * cellWidth, y, selected ? selectedRadius : radius, paint);
            }
        }
    }

    @Override
    public void setPaddingPixels(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(
                (int) (left * paddingFactor),
                (int) (top * paddingFactor),
                (int) (right * paddingFactor),
                (int) (bottom * paddingFactor));
    }
}
