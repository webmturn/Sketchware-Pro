package pro.sketchware.activities.editor.view.item;

import android.content.Context;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import pro.sketchware.R;
import pro.sketchware.activities.editor.view.ItemView;
import pro.sketchware.beans.ViewBean;

public class ItemFloatingActionButton extends FloatingActionButton implements ItemView {

    private ColorStateList mainColor;
    private ViewBean viewBean;
    private boolean isSelected;
    private boolean isFixed;

    public ItemFloatingActionButton(Context context) {
        super(context);
        mainColor = getBackgroundTintList();
        setCompatElevation(0.0F);
        setDrawingCacheEnabled(true);
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
    public void setSelection(boolean isSelected) {
        this.isSelected = isSelected;
        setBackgroundTintList(isSelected ? ColorStateList.valueOf(0x9599d5d0) : mainColor);
    }

    public void setMainColor(int color) {
        mainColor =
                ColorStateList.valueOf(
                        color != 0
                                ? color
                                : ContextCompat.getColor(getContext(), R.color.color_accent));
        if (!isSelected) {
            setBackgroundTintList(mainColor);
        }
    }
}
