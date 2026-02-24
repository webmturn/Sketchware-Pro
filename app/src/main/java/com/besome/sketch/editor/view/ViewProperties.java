package com.besome.sketch.editor.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.besome.sketch.ctrls.ViewIdSpinnerItem;

import java.util.ArrayList;

import pro.sketchware.core.FileSelectedCallback;
import pro.sketchware.core.ViewUtil;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;

public class ViewProperties extends RelativeLayout implements AdapterView.OnItemSelectedListener {

    private final ArrayList<String> viewsIdList = new ArrayList<>();
    private SpinnerItemAdapter spinnerItemAdapter;
    private FileSelectedCallback propertyTargetChangeListener = null;

    public ViewProperties(Context context) {
        super(context);
        initialize(context);
    }

    public ViewProperties(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initialize(context);
    }

    public void setOnPropertyTargetChangeListener(FileSelectedCallback onPropertyTargetChangeListener) {
        propertyTargetChangeListener = onPropertyTargetChangeListener;
    }

    private void initialize(Context context) {
        ViewUtil.inflateLayoutInto(context, this, R.layout.view_properties);
        ((TextView) findViewById(R.id.btn_editproperties)).setText(Helper.getResString(R.string.design_button_properties));
        Spinner spinner = findViewById(R.id.spn_widget);
        spinnerItemAdapter = new SpinnerItemAdapter(context, viewsIdList);
        spinner.setAdapter(spinnerItemAdapter);
        spinner.setSelection(0);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        spinnerItemAdapter.setLayoutPosition(position);
        if (propertyTargetChangeListener != null) {
            propertyTargetChangeListener.onFileSelected(viewsIdList.get(position));
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private static class SpinnerItemAdapter extends BaseAdapter {

        private final Context context;
        private final ArrayList<String> data;
        private int layoutPosition;

        public SpinnerItemAdapter(Context context, ArrayList<String> items) {
            this.context = context;
            data = items;
        }

        public void setLayoutPosition(int position) {
            layoutPosition = position;
        }

        @Override
        public int getCount() {
            if (data == null) return 0;
            return data.size();
        }

        @Override
        public View getDropDownView(int position, View view, ViewGroup viewGroup) {
            return createSpinnerItemView(position, view, viewGroup, layoutPosition == position);
        }

        @Override
        public String getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return createSpinnerItemView(position, convertView, parent, false);
        }

        private ViewIdSpinnerItem createSpinnerItemView(int position, View convertView, ViewGroup parent, boolean isDropDown) {
            ViewIdSpinnerItem viewIdSpinnerItem;
            if (convertView != null) {
                viewIdSpinnerItem = (ViewIdSpinnerItem) convertView;
            } else {
                viewIdSpinnerItem = new ViewIdSpinnerItem(context);
                viewIdSpinnerItem.setTextSize(R.dimen.text_size_body_small);
            }
            viewIdSpinnerItem.setData(0, data.get(position), isDropDown);
            viewIdSpinnerItem.setTextStyle(false, 0xff404040, 0xff404040);
            return viewIdSpinnerItem;
        }
    }
}
