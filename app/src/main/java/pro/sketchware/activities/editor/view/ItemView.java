package pro.sketchware.activities.editor.view;

import android.view.View;
import android.view.ViewGroup;

import pro.sketchware.beans.ViewBean;

// 'sy' is used in ViewPane items, example ItemLinearLayout
public interface ItemView {
    /** Clamp append requests before applying the existing two-argument overload's GONE offset. */
    static int safeInsertionIndex(ViewGroup parent, int index, boolean offsetGoneChild) {
        int count = parent.getChildCount();
        if (index < 0 || index >= count) return -1;
        if (offsetGoneChild) {
            for (int i = 0; i <= index; i++) {
                if (parent.getChildAt(i).getVisibility() == View.GONE) return index + 1;
            }
        }
        return index;
    }

    /** Sets physical padding without the editor widgets' legacy dp conversion. */
    default void setPaddingPixels(int left, int top, int right, int bottom) {
        ((android.view.View) this).setPadding(left, top, right, bottom);
    }

    ViewBean getBean();

    void setBean(ViewBean viewBean);

    boolean getFixed();

    void setFixed(boolean fixed);

    void setSelection(boolean selection);
}
