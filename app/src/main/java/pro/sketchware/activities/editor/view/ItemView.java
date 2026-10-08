package pro.sketchware.activities.editor.view;

import pro.sketchware.beans.ViewBean;

// 'sy' is used in ViewPane items, example ItemLinearLayout
public interface ItemView {
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
