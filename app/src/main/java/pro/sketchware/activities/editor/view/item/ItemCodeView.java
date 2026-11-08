package pro.sketchware.activities.editor.view.item;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import java.util.Map;
import pro.sketchware.R;
import pro.sketchware.activities.editor.view.ItemView;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.util.ThemeUtils;
import pro.sketchware.util.ViewUtil;

public class ItemCodeView extends LinearLayout implements ItemView {
    // Bundled CodeView 0.4.0 XML default.
    private static final int DEFAULT_FONT_SIZE = 14;
    private static final String SAMPLE_TYPE = "String";
    private static final String[] SAMPLE_LINES = {
        SAMPLE_TYPE + " name = \"World\";",
        SAMPLE_TYPE + " message = \"Hello, \" + name;",
        "log(message);"
    };

    private final Paint paint;
    private final float paddingFactor;
    private final TextView code;
    private final int gutterColor;
    private final int typeColor;
    private final int literalColor;
    private final int highlightColor;
    private ViewBean viewBean;
    private boolean hasSelection;
    private boolean hasFixed;

    public ItemCodeView(Context context) {
        super(context);
        paddingFactor = ViewUtil.dpToPx(context, 1.0f);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * paddingFactor);

        setOrientation(VERTICAL);
        int surface = ThemeUtils.getColor(context, R.attr.colorSurface);
        int foreground = ThemeUtils.getColor(context, R.attr.colorOnSurface);
        int primary = ThemeUtils.getColor(context, R.attr.colorPrimary);
        int secondary = ThemeUtils.getColor(context, R.attr.colorSecondary);
        paint.setColor(primary);
        int panelColor = ColorUtils.blendARGB(surface, foreground, 0.07f);
        gutterColor = ColorUtils.blendARGB(surface, foreground, 0.68f);
        typeColor = primary;
        literalColor = ColorUtils.blendARGB(secondary, foreground, 0.45f);
        highlightColor = ColorUtils.blendARGB(panelColor, primary, 0.22f);
        code = new TextView(context);
        code.setTypeface(Typeface.MONOSPACE);
        code.setTextColor(foreground);
        code.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        code.setTextDirection(TEXT_DIRECTION_LTR);
        code.setGravity(Gravity.TOP | Gravity.START);

        GradientDrawable panel = new GradientDrawable();
        panel.setColor(panelColor);
        panel.setCornerRadius(8 * paddingFactor);
        code.setBackground(panel);
        int horizontal = Math.round(12 * paddingFactor);
        int vertical = Math.round(8 * paddingFactor);
        code.setPadding(horizontal, vertical, horizontal, vertical);
        applyPreviewAttributes(Map.of());
        addView(
                code,
                new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void applyPreviewAttributes(Map<String, String> attributes) {
        int fontSize = integerAttribute(attributes, "app:cv_font_size", DEFAULT_FONT_SIZE);
        code.setTextSize(Math.max(8, Math.min(fontSize, 256)));
        code.setHorizontallyScrolling(!booleanAttribute(attributes, "app:cv_wrap_line", false));
        code.setText(
                sampleCode(
                        booleanAttribute(attributes, "app:cv_show_line_number", false),
                        integerAttribute(attributes, "app:cv_start_line_number", 1),
                        integerAttribute(attributes, "app:cv_highlight_line_number", -1)));
    }

    private CharSequence sampleCode(boolean showNumbers, int firstLine, int highlightLine) {
        SpannableStringBuilder sample = new SpannableStringBuilder();
        int flags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
        for (int i = 0; i < SAMPLE_LINES.length; i++) {
            if (i > 0) sample.append('\n');
            int rowStart = sample.length();
            long lineNumber = (long) firstLine + i;
            if (showNumbers) {
                sample.append(lineNumber + "  ", new ForegroundColorSpan(gutterColor), flags);
            }
            int start = sample.length();
            String line = SAMPLE_LINES[i];
            sample.append(line);
            if (line.startsWith(SAMPLE_TYPE + " ")) {
                sample.setSpan(
                        new ForegroundColorSpan(typeColor),
                        start,
                        start + SAMPLE_TYPE.length(),
                        flags);
            }
            int quote = line.indexOf('"');
            if (quote >= 0) {
                sample.setSpan(
                        new ForegroundColorSpan(literalColor),
                        start + quote,
                        start + line.lastIndexOf('"') + 1,
                        flags);
            }
            if (highlightLine >= 0 && lineNumber == highlightLine) {
                sample.setSpan(
                        new BackgroundColorSpan(highlightColor), rowStart, sample.length(), flags);
            }
        }
        return sample;
    }

    private static boolean booleanAttribute(
            Map<String, String> attributes, String name, boolean fallback) {
        String value = attributes.getOrDefault(name, "").trim();
        return switch (value) {
            case "true" -> true;
            case "false" -> false;
            default -> fallback;
        };
    }

    private static int integerAttribute(Map<String, String> attributes, String name, int fallback) {
        String value = attributes.get(name);
        if (value == null) return fallback;
        try {
            return Integer.decode(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
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
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (hasSelection) {
            float inset = paint.getStrokeWidth() / 2;
            canvas.drawRect(inset, inset, getWidth() - inset, getHeight() - inset, paint);
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
