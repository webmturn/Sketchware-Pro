package com.besome.sketch.editor.view.item;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.view.ItemView;
import com.besome.sketch.editor.view.ScrollContainer;

import pro.sketchware.core.ViewUtil;

public class ItemVerticalScrollView extends FrameLayout implements ItemView, ScrollContainer {

    private final Rect tempRect = new Rect();
    private final Rect rect = new Rect();
    private ViewBean viewBean;
    private boolean isSelected = false;
    private boolean isFixed = false;
    private Paint paint;
    private float lastMotionY = -1.0F;
    private boolean scrollEnabled = true;

    public ItemVerticalScrollView(Context context) {
        super(context);
        initialize(context);
    }

    private int computeScrollDelta(Rect rect) {
        int childCount = getChildCount();
        byte defaultDelta = 0;
        if (childCount == 0) {
            return 0;
        } else {
            int viewHeight = getHeight();
            int scrollY = getScrollY();
            int bottomEdge = scrollY + viewHeight;
            int fadingEdge = getVerticalFadingEdgeLength();
            int topEdge = scrollY;
            if (rect.top > 0) {
                topEdge = scrollY + fadingEdge;
            }

            int adjustedBottom = bottomEdge;
            if (rect.bottom < getChildAt(0).getHeight()) {
                adjustedBottom = bottomEdge - fadingEdge;
            }

            int delta;
            if (rect.bottom > adjustedBottom && rect.top > topEdge) {
                if (rect.height() > viewHeight) {
                    delta = rect.top - topEdge;
                } else {
                    delta = rect.bottom - adjustedBottom;
                }

                delta = Math.min(delta, getChildAt(0).getBottom() - adjustedBottom);
            } else {
                delta = defaultDelta;
                if (rect.top < topEdge) {
                    if (rect.bottom < adjustedBottom) {
                        if (rect.height() > viewHeight) {
                            delta = -(adjustedBottom - rect.bottom);
                        } else {
                            delta = -(topEdge - rect.top);
                        }

                        delta = Math.max(delta, -getScrollY());
                    }
                }
            }

            return delta;
        }
    }

    @Override
    public void reindexChildren() {
        int childIdx = 0;

        int nextIndex;
        for (int itemIndex = 0; childIdx < getChildCount(); itemIndex = nextIndex) {
            View child = getChildAt(childIdx);
            nextIndex = itemIndex;
            if (child instanceof ItemView) {
                ((ItemView) child).getBean().index = itemIndex;
                nextIndex = itemIndex + 1;
            }

            ++childIdx;
        }

    }

    private void scrollByIfNeeded(int position) {
        if (position != 0) {
            scrollBy(0, position);
        }
    }

    private void initialize(Context context) {
        setDrawingCacheEnabled(true);
        setMinimumWidth((int) ViewUtil.dpToPx(context, 32.0F));
        setMinimumHeight((int) ViewUtil.dpToPx(context, 32.0F));
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(ViewUtil.dpToPx(getContext(), 2.0F));
    }

    private boolean isViewVisible(View view, int offset, int viewportHeight) {
        view.getDrawingRect(tempRect);
        offsetDescendantRectToMyCoords(view, tempRect);
        return tempRect.bottom + offset >= getScrollY() && tempRect.top - offset <= getScrollY() + viewportHeight;
    }

    @Override
    public void addView(View view, int index) {
        int childCount = getChildCount();
        if (index > childCount) {
            super.addView(view);
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
                super.addView(view, index + 1);
            } else {
                super.addView(view, index);
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

    public boolean getSelection() {
        return isSelected;
    }

    @Override
    public void setSelection(boolean hasSelection) {
        isSelected = hasSelection;
        invalidate();
    }

    @Override
    public void measureChild(View view, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        parentWidthMeasureSpec = FrameLayout.getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft() + getPaddingRight(), layoutParams.width);
        view.measure(parentWidthMeasureSpec, MeasureSpec.makeMeasureSpec(Math.max(0, MeasureSpec.getSize(parentHeightMeasureSpec) - (getPaddingTop() + getPaddingBottom())), MeasureSpec.UNSPECIFIED));
    }

    @Override
    public void measureChildWithMargins(View view, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        int childMeasureSpec = FrameLayout.getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft() + getPaddingRight() + layoutParams.leftMargin + layoutParams.rightMargin + widthUsed, layoutParams.width);
        parentWidthMeasureSpec = getPaddingTop();
        widthUsed = getPaddingBottom();
        view.measure(childMeasureSpec, MeasureSpec.makeMeasureSpec(Math.max(0, MeasureSpec.getSize(parentHeightMeasureSpec) - (parentWidthMeasureSpec + widthUsed + layoutParams.topMargin + layoutParams.bottomMargin + heightUsed)), MeasureSpec.UNSPECIFIED));
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        if (!isFixed) {
            int scrollX = getScrollX();
            int measuredWidthX = getScrollX() + getMeasuredWidth();
            int scrollY = getScrollY();
            int measuredHeightY = getScrollY() + getMeasuredHeight();
            if (isSelected) {
                paint.setColor(0x9599d5d0);
                rect.set(scrollX, scrollY, measuredWidthX, measuredHeightY);
                canvas.drawRect(rect, paint);
            }
            paint.setColor(0xaad50000);
            canvas.drawLine((float) scrollX, (float) scrollY, (float) measuredWidthX, (float) scrollY, paint);
            canvas.drawLine((float) scrollX, (float) scrollY, (float) scrollX, (float) measuredHeightY, paint);
            canvas.drawLine((float) measuredWidthX, (float) scrollY, (float) measuredWidthX, (float) measuredHeightY, paint);
            canvas.drawLine((float) scrollX, (float) measuredHeightY, (float) measuredWidthX, (float) measuredHeightY, paint);
        }
        super.onDraw(canvas);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (!scrollEnabled) {
            return false;
        } else if (getChildCount() <= 0) {
            return false;
        } else {
            View firstChild = getChildAt(0);
            int action = motionEvent.getAction();
            float motionEventY = motionEvent.getY();
            if (action != 0) {
                if (action != 1) {
                    if (action == 2) {
                        if (lastMotionY < 0.0F) {
                            lastMotionY = motionEventY;
                        }

                        int scrollDelta = (int) (lastMotionY - motionEventY);
                        lastMotionY = motionEventY;
                        if (scrollDelta <= 0) {
                            if (getScrollY() <= 0) {
                                scrollDelta = 0;
                            }

                            scrollDelta = Math.max(-getScrollY(), scrollDelta);
                        } else {
                            int maxScroll = firstChild.getBottom() - getScrollY() - getHeight() + getPaddingRight();
                            if (maxScroll > 0) {
                                scrollDelta = Math.min(maxScroll, scrollDelta);
                            } else {
                                scrollDelta = 0;
                            }
                        }

                        if (scrollDelta != 0) {
                            scrollBy(0, scrollDelta);
                        }
                    }
                } else {
                    lastMotionY = -1.0F;
                }
            } else {
                lastMotionY = motionEventY;
            }

            return false;
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            if (getChildCount() > 0) {
                View child = getChildAt(0);
                ViewGroup.LayoutParams childParams = child.getLayoutParams();
                heightMeasureSpec = getPaddingLeft();
                int measuringSize = getMeasuredHeight() - (getPaddingTop() + getPaddingBottom());
                if (child.getMeasuredHeight() < measuringSize) {
                    child.measure(FrameLayout.getChildMeasureSpec(widthMeasureSpec, heightMeasureSpec + getPaddingRight(), childParams.width), MeasureSpec.makeMeasureSpec(measuringSize, MeasureSpec.EXACTLY));
                }
            }
        }
    }

    @Override
    public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        View focusedView = findFocus();
        if (focusedView != null && this != focusedView && isViewVisible(focusedView, 0, oldHeight)) {
            focusedView.getDrawingRect(tempRect);
            offsetDescendantRectToMyCoords(focusedView, tempRect);
            scrollByIfNeeded(computeScrollDelta(tempRect));
        }
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        setScrollY(0);
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

    public void setScrollEnabled(boolean isScrollEnabled) {
        scrollEnabled = isScrollEnabled;
    }
}
