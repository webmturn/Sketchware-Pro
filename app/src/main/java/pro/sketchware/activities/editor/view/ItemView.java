package pro.sketchware.activities.editor.view;

import pro.sketchware.beans.ViewBean;

// 'sy' is used in ViewPane items, example ItemLinearLayout
public interface ItemView {
    ViewBean getBean();

    void setBean(ViewBean viewBean);

    boolean getFixed();

    void setFixed(boolean fixed);

    void setSelection(boolean selection);
}
