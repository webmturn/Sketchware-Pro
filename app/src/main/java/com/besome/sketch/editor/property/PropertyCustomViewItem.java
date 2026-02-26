package com.besome.sketch.editor.property;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.besome.sketch.beans.ProjectFileBean;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

import pro.sketchware.core.PropertyChangedCallback;
import pro.sketchware.core.UIHelper;
import pro.sketchware.core.ViewUtil;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.PropertyPopupSelectorSingleBinding;

@SuppressLint("ViewConstructor")
public class PropertyCustomViewItem extends RelativeLayout implements View.OnClickListener {

    private String key = "";
    private String value = "";
    private TextView tvName;
    private TextView tvValue;
    private ImageView imgLeftIcon;
    private int iconResId;
    private View propertyItem;
    private View propertyMenuItem;
    private PropertyChangedCallback propertyValueChangeListener;
    private ArrayList<ProjectFileBean> customViews;

    public PropertyCustomViewItem(Context context, boolean idk) {
        super(context);
        initializeView(idk);
    }

    private RadioButton createRadioButton(String fileName) {
        RadioButton radioButton = new RadioButton(getContext());
        radioButton.setText(fileName);
        radioButton.setTag(fileName);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, (int) (ViewUtil.dpToPx(getContext(), 1.0F) * 40.0F));
        radioButton.setGravity(19);
        radioButton.setLayoutParams(layoutParams);
        return radioButton;
    }

    private void showSelectionDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(getContext());
        dialog.setTitle(Helper.getText(tvName));
        dialog.setIcon(iconResId);
        PropertyPopupSelectorSingleBinding propertyBinding = PropertyPopupSelectorSingleBinding.inflate(((Activity) getContext()).getLayoutInflater());
        RadioGroup rgContent = propertyBinding.rgContent;
        rgContent.addView(createRadioButton("none"));

        for (ProjectFileBean projectFileBean : customViews) {
            RadioButton radioButton = createRadioButton(projectFileBean.fileName);
            propertyBinding.rgContent.addView(radioButton);
        }

        ((RadioButton) rgContent.getChildAt(0)).setChecked(true);

        for (int i = 0, childCount = rgContent.getChildCount(); i < childCount; i++) {
            RadioButton radioButton = (RadioButton) rgContent.getChildAt(i);
            if (radioButton.getTag().toString().equals(value)) {
                radioButton.setChecked(true);
            }
        }

        dialog.setView(propertyBinding.getRoot());
        dialog.setPositiveButton(R.string.common_word_select, (v, which) -> {
            for (int i = 0, childCount = rgContent.getChildCount(); i < childCount; i++) {
                RadioButton radioButton = (RadioButton) rgContent.getChildAt(i);

                if (radioButton.isChecked()) {
                    setValue(radioButton.getTag().toString());
                }
            }
            if (propertyValueChangeListener != null) {
                propertyValueChangeListener.onPropertyChanged(key, value);
            }
            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    private void initializeView(boolean showMenu) {
        ViewUtil.inflateLayoutInto(getContext(), this, R.layout.property_selector_item);
        tvName = findViewById(R.id.tv_name);
        tvValue = findViewById(R.id.tv_value);
        propertyItem = findViewById(R.id.property_item);
        propertyMenuItem = findViewById(R.id.property_menu_item);
        imgLeftIcon = findViewById(R.id.img_left_icon);
//        if (var2) {
//            propertyMenuItem.setOnClickListener(this);
//            propertyMenuItem.setSoundEffectsEnabled(true);
//        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
        int stringResId = getResources().getIdentifier(key, "string", getContext().getPackageName());
        if (stringResId > 0) {
            tvName.setText(stringResId);
            iconResId = R.drawable.ic_mtrl_interface;
            if (propertyMenuItem.getVisibility() == View.VISIBLE) {
                ImageView iconView = findViewById(R.id.img_icon);
                TextView titleView = findViewById(R.id.tv_title);
                iconView.setImageResource(iconResId);
                titleView.setText(stringResId);
            } else {
                imgLeftIcon.setImageResource(iconResId);
            }
        }

    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (TextUtils.isEmpty(value)) {
            value = "none";
        }
        this.value = value;
        tvValue.setText(value);
    }

    @Override
    public void onClick(View view) {
        if (!UIHelper.isClickThrottled()) {
            if ("property_custom_view_listview".equals(key)) {
                showSelectionDialog();
            }
        }
    }

    public void setCustomView(ArrayList<ProjectFileBean> customView) {
        customViews = customView;
    }

    public void setOnPropertyValueChangeListener(PropertyChangedCallback listener) {
        propertyValueChangeListener = listener;
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
}
