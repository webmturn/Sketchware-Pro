package com.besome.sketch.ctrls;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import pro.sketchware.R;
import pro.sketchware.databinding.VarIdSpinnerItemBinding;

public class ViewIdSpinnerItem extends LinearLayout {

    private boolean isDropDown;

    private final VarIdSpinnerItemBinding binding;

    public ViewIdSpinnerItem(Context context) {
        super(context);
        binding = VarIdSpinnerItemBinding.inflate(LayoutInflater.from(context), this, true);
    }

    public void setData(int iconResId, String text, boolean isSelected) {
        if (isSelected) {
            binding.imgvSelected.setVisibility(View.VISIBLE);
        } else {
            binding.imgvSelected.setVisibility(View.GONE);
        }

        if (text.charAt(0) == '_') {
            binding.name.setText(text.substring(1));
            setTextStyle(false, 0xffff5555, 0xfff8f820);
        } else {
            binding.name.setText(text);
            setTextStyle(true, ContextCompat.getColor(getContext(), R.color.view_property_spinner_filter), ContextCompat.getColor(getContext(), R.color.view_property_spinner_filter));
        }

        binding.icon.setImageResource(iconResId);
    }

    public void setTextStyle(boolean notSelected, int color, int defaultColor) {
        if (notSelected) {
            if (!isDropDown) color = defaultColor;
            binding.name.setTextColor(color);
            binding.name.setTypeface(null, Typeface.NORMAL);
        } else {
            if (!isDropDown) color = defaultColor;
            binding.name.setTextColor(color);
            binding.name.setTypeface(null, Typeface.BOLD_ITALIC);
        }
    }

    public void setDropDown(boolean isDropDown) {
        this.isDropDown = isDropDown;
    }

    public void setTextSize(int textSize) {
    }
}