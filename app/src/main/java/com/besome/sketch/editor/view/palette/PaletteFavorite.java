package com.besome.sketch.editor.view.palette;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.lib.ui.CustomScrollView;

import java.util.ArrayList;

import pro.sketchware.core.WidgetPaletteIcon;
import pro.sketchware.core.ViewUtil;
import pro.sketchware.R;

public class PaletteFavorite extends LinearLayout {
    private LinearLayout collectionWidgets;
    private CustomScrollView scrollView;

    public PaletteFavorite(Context context) {
        super(context);
        initialize(context);
    }

    public PaletteFavorite(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initialize(context);
    }

    public View addWidgetCollection(String name, ArrayList<ViewBean> viewBeans) {
        WidgetPaletteIcon favoriteWidget = new WidgetPaletteIcon(getContext(), name, viewBeans);
        collectionWidgets.addView(favoriteWidget);
        return favoriteWidget;
    }

    public void removeAllWidgets() {
        collectionWidgets.removeAllViews();
    }

    private void initialize(Context context) {
        ViewUtil.inflateLayoutInto(context, this, R.layout.palette_favorite);
        collectionWidgets = findViewById(R.id.widget);
        scrollView = findViewById(R.id.scv);
    }

    public void setScrollEnabled(boolean scrollEnabled) {
        if (scrollEnabled) {
            scrollView.enableScroll();
        } else {
            scrollView.disableScroll();
        }
    }
}
