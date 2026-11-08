package pro.sketchware.activities.editor.view.item;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import pro.sketchware.activities.editor.view.ItemView;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.util.ViewUtil;

public class ItemOTPView extends LinearLayout implements ItemView {
    // Bundled OTPView 0.1.0 creates six fixed-width EditTexts with 5dp margins.
    private static final int FIELD_COUNT = 6;
    private static final int FIELD_WIDTH_DP = 36;
    private static final int FIELD_MARGIN_DP = 5;

    private final Paint paint;
    private final float paddingFactor;
    private ViewBean viewBean;
    private boolean hasSelection;
    private boolean hasFixed;

    public ItemOTPView(Context context) {
        super(context);
        paddingFactor = ViewUtil.dpToPx(context, 1.0f);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x9599d5d0);
        setOrientation(HORIZONTAL);
        setWillNotDraw(false);
        int width = (int) (FIELD_WIDTH_DP * paddingFactor);
        int margin = (int) (FIELD_MARGIN_DP * paddingFactor);
        int padding = (int) (10 * paddingFactor);
        for (int index = 0; index < FIELD_COUNT; index++) {
            EditText field = new EditText(context);
            field.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            field.setTextSize(16);
            field.setTextColor(0xff323542);
            field.setInputType(InputType.TYPE_CLASS_NUMBER);
            field.setEnabled(false);
            field.setFocusable(false);
            field.setClickable(false);
            field.setLongClickable(false);
            field.setCursorVisible(false);
            field.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            LayerDrawable background =
                    new LayerDrawable(
                            new Drawable[] {
                                new ColorDrawable(0xff7c8ba2), new ColorDrawable(0xfffafafa)
                            });
            background.setLayerInset(1, 0, 0, 0, Math.round(2 * paddingFactor));
            field.setBackground(background);
            field.setBackgroundTintList(null);
            field.setPadding(padding, padding, padding, padding);
            LayoutParams params = new LayoutParams(width, LayoutParams.WRAP_CONTENT);
            params.setMargins(margin, margin, margin, margin);
            addView(field, params);
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (hasSelection) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
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
