package pro.sketchware.activities.editor.view.item;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.google.android.material.button.MaterialButton;
import pro.sketchware.activities.editor.view.ItemView;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.util.ViewUtil;

public class ItemMaterialButton extends MaterialButton implements ItemView {

    private final Paint paint;
    private final float paddingFactor;
    private final Rect rect;
    private final ColorStateList defaultBackgroundTint;
    private Drawable previewBackgroundImage;
    private ViewBean viewBean;
    private boolean hasSelection;
    private boolean hasFixed;

    public ItemMaterialButton(Context context) {
        super(context);
        paddingFactor = ViewUtil.dpToPx(context, 1.0f);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x9599d5d0);
        rect = new Rect();
        defaultBackgroundTint = getBackgroundTintList();

        setDrawingCacheEnabled(true);
        setFocusable(false);
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
    public void onDraw(Canvas canvas) {
        if (previewBackgroundImage != null) {
            previewBackgroundImage.setBounds(0, 0, getWidth(), getHeight());
            previewBackgroundImage.draw(canvas);
        }
        if (hasSelection) {
            rect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
            canvas.drawRect(rect, paint);
        }
        super.onDraw(canvas);
    }

    @Override
    public void setBackgroundColor(int color) {
        if (color == 0xffffff) {
            setBackgroundTintList(defaultBackgroundTint);
        } else {
            super.setBackgroundColor(color);
        }
    }

    @Override
    public void setBackgroundTintList(ColorStateList tint) {
        super.setBackgroundTintList(tint);
        if (previewBackgroundImage != null) {
            previewBackgroundImage.setTintList(tint);
            invalidate();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (previewBackgroundImage != null && previewBackgroundImage.setState(getDrawableState())) {
            invalidate();
        }
    }

    public void setPreviewBackgroundImage(Drawable image) {
        if (previewBackgroundImage != image) {
            previewBackgroundImage = image;
            if (image != null) {
                image.mutate();
                var tintMode = getBackgroundTintMode();
                if (tintMode != null) image.setTintMode(tintMode);
                image.setTintList(getBackgroundTintList());
                image.setState(getDrawableState());
            }
            // Replacing the Material background disables its shape and stroke setters.
            getBackground().setAlpha(image == null ? 255 : 0);
            invalidate();
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
