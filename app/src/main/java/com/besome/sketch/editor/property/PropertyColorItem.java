package com.besome.sketch.editor.property;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.besome.sketch.lib.ui.ColorPickerDialog;

import java.util.Objects;

import pro.sketchware.core.PropertyChangedCallback;
import pro.sketchware.core.UIHelper;
import pro.sketchware.core.ViewUtil;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;

@SuppressLint("ViewConstructor")
public class PropertyColorItem extends RelativeLayout implements View.OnClickListener {

    private Context context;
    private String key;
    private String sc_id;
    private String resValue;
    private int value;
    private TextView tvName;
    private TextView tvValue;
    private View viewColor;
    private ImageView imgLeftIcon;
    private View propertyItem;
    private View propertyMenuItem;
    private PropertyChangedCallback valueChangeListener;

    public PropertyColorItem(Context context, boolean z) {
        super(context);
        initialize(context, z);
    }

    public PropertyColorItem(Context context, boolean z, String scId) {
        super(context);
        sc_id = scId;
        initialize(context, z);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
        int identifier = getResources().getIdentifier(key, "string", getContext().getPackageName());
        if (identifier > 0) {
            tvName.setText(Helper.getResString(identifier));
            if (propertyMenuItem.getVisibility() == VISIBLE) {
                ((ImageView) findViewById(R.id.img_icon)).setImageResource(R.drawable.ic_mtrl_palette);
                ((TextView) findViewById(R.id.tv_title)).setText(Helper.getResString(identifier));
                return;
            }
            imgLeftIcon.setImageResource(R.drawable.ic_mtrl_palette);
        }
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
        resValue = null;
        if (value == 0) {
            tvValue.setText(getContext().getString(R.string.color_transparent));
            viewColor.setBackgroundColor(value);
        } else if (value == 0xffffff) {
            tvValue.setText(getContext().getString(R.string.color_none));
            viewColor.setBackgroundColor(value);
        } else {
            tvValue.setText(String.format("#%08X", value));
            viewColor.setBackgroundColor(value);
        }
    }

    public String getResValue() {
        return resValue;
    }

    public void setValue(int value, String resValue) {
        this.value = value;
        this.resValue = resValue;
        if (value == 0) {
            tvValue.setText(getContext().getString(R.string.color_transparent));
        } else if (value == 0xffffff) {
            tvValue.setText(getContext().getString(R.string.color_none));
        } else {
            tvValue.setText(resValue);
        }
        viewColor.setBackgroundColor(value);
    }

    @Override
    public void onClick(View v) {
        if (!UIHelper.isClickThrottled()) {
            showColorPicker(v);
        }
    }

    public void setOnPropertyValueChangeListener(PropertyChangedCallback onPropertyValueChangeListener) {
        valueChangeListener = onPropertyValueChangeListener;
    }

    public void setOrientationItem(int orientationItem) {
        if (orientationItem == 0) {
            propertyItem.setVisibility(GONE);
            propertyMenuItem.setVisibility(VISIBLE);
            propertyItem.setOnClickListener(null);
            propertyMenuItem.setOnClickListener(this);
        } else {
            propertyItem.setVisibility(VISIBLE);
            propertyMenuItem.setVisibility(GONE);
            propertyItem.setOnClickListener(this);
            propertyMenuItem.setOnClickListener(null);
        }
    }

    private void initialize(Context context, boolean z) {
        this.context = context;
        ViewUtil.inflateLayoutInto(context, this, R.layout.property_color_item);
        tvName = findViewById(R.id.tv_name);
        tvValue = findViewById(R.id.tv_value);
        viewColor = findViewById(R.id.view_color);
        imgLeftIcon = findViewById(R.id.img_left_icon);
        propertyItem = findViewById(R.id.property_item);
        propertyMenuItem = findViewById(R.id.property_menu_item);
//        if (z) {
//            propertyMenuItem.setOnClickListener(this);
//            propertyMenuItem.setSoundEffectsEnabled(true);
//        }
    }

    private void showColorPicker(View anchorView) {
        String tvValueStr = tvValue.getText().toString();
        String color;
        if (tvValueStr.equals("NONE") || tvValueStr.equals("TRANSPARENT")) {
            color = tvValueStr;
        } else
            color = Objects.requireNonNullElseGet(resValue, () -> String.format("#%06X", value));

        ColorPickerDialog colorPicker = new ColorPickerDialog((Activity) context, color, key.equals("property_background_color"), true, sc_id);
        colorPicker.a(new ColorPickerDialog.b() {
            @Override
            public void a(int var1) {
                setValue(var1);
                if (valueChangeListener != null) {
                    valueChangeListener.onPropertyChanged(key, value);
                }
            }

            @Override
            public void a(String var1, int var2) {
                setValue(var2, "@color/" + var1);
                if (valueChangeListener != null) {
                    valueChangeListener.onPropertyChanged(key, value);
                }
            }
        });
        colorPicker.materialColorAttr((attr, attrColor) -> {
            setValue(attrColor, "?" + attr);
            if (valueChangeListener != null) {
                valueChangeListener.onPropertyChanged(key, value);
            }
        });
        colorPicker.showAtLocation(anchorView, Gravity.CENTER, 0, 0);
    }
}
