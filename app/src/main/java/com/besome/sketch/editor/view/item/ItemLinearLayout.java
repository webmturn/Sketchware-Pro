package com.besome.sketch.editor.view.item;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.widget.LinearLayout;

import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.view.ItemView;
import com.besome.sketch.editor.view.ScrollContainer;

import pro.sketchware.core.ViewUtil;

public class ItemLinearLayout extends LinearLayout implements ItemView, ScrollContainer {

    private ViewBean viewBean = null;
    private boolean isSelected = false;
    private boolean isFixed = false;
    private Paint paint;

    private Rect rect;
    private int layoutGravity = 0;

    public ItemLinearLayout(Context context) {
        super(context);
        initialize(context);
    }

    @Override
    public void reindexChildren() {
        int childIdx = 0;

        int nextIndex;
        for (int i = 0; childIdx < getChildCount(); i = nextIndex) {
            View child = getChildAt(childIdx);
            nextIndex = i;
            if (child instanceof ItemView) {
                ((ItemView) child).getBean().index = i;
                nextIndex = i + 1;
            }

            ++childIdx;
        }

    }

    private void initialize(Context context) {
        setOrientation(LinearLayout.HORIZONTAL);
        setDrawingCacheEnabled(true);
        setMinimumWidth((int) ViewUtil.dpToPx(context, 32.0F));
        setMinimumHeight((int) ViewUtil.dpToPx(context, 32.0F));
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(ViewUtil.dpToPx(getContext(), 2.0F));
        rect = new Rect();
    }

    @Override
    public void addView(View child, int index) {
        int childCount = getChildCount();
        if (index > childCount) {
            super.addView(child);
        } else {
            byte defaultGoneIndex = -1;
            int searchIdx = 0;

            int goneChildIndex;
            while (true) {
                goneChildIndex = defaultGoneIndex;
                if (searchIdx >= childCount) {
                    break;
                }

                if (getChildAt(searchIdx).getVisibility() == View.GONE) {
                    goneChildIndex = searchIdx;
                    break;
                }

                ++searchIdx;
            }

            if (goneChildIndex >= 0 && index >= goneChildIndex) {
                super.addView(child, index + 1);
            } else {
                super.addView(child, index);
            }
        }
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

    public int getLayoutGravity() {
        return layoutGravity;
    }

    public void setLayoutGravity(int layoutGravity) {
        this.layoutGravity = layoutGravity;
        super.setGravity(layoutGravity);
    }

    public boolean getSelection() {
        return isSelected;
    }

    @Override
    public void setSelection(boolean selected) {
        isSelected = selected;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (!isFixed) {
            if (isSelected) {
                paint.setColor(0x9599d5d0);
                rect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                canvas.drawRect(rect, paint);
            }
            paint.setColor(0x60000000);

            int measuredWidth = getMeasuredWidth();
            int measuredHeight = getMeasuredHeight();

            canvas.drawLine(0.0F, 0.0F, (float) measuredWidth, 0.0F, paint);
            canvas.drawLine(0.0F, 0.0F, 0.0F, (float) measuredHeight, paint);
            canvas.drawLine((float) measuredWidth, 0.0F, (float) measuredWidth, (float) measuredHeight, paint);
            canvas.drawLine(0.0F, (float) measuredHeight, (float) measuredWidth, (float) measuredHeight, paint);
        }

        super.onDraw(canvas);
    }

    @Override
    public void setChildScrollEnabled(boolean scrollEnabled) {
        for (int i = 0; i < getChildCount(); ++i) {
            View child = getChildAt(i);
            if (child instanceof ScrollContainer) {
                ((ScrollContainer) child).setChildScrollEnabled(scrollEnabled);
            }

            if (child instanceof ItemHorizontalScrollView) {
                ((ItemHorizontalScrollView) child).setScrollEnabled(scrollEnabled);
            }

            if (child instanceof ItemVerticalScrollView) {
                ((ItemVerticalScrollView) child).setScrollEnabled(scrollEnabled);
            }
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding((int) ViewUtil.dpToPx(getContext(), (float) left), (int) ViewUtil.dpToPx(getContext(), (float) top), (int) ViewUtil.dpToPx(getContext(), (float) right), (int) ViewUtil.dpToPx(getContext(), (float) bottom));
    }
}
