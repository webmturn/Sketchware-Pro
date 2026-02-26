package com.besome.sketch.editor.view.item;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.view.EditorListItem;
import com.besome.sketch.editor.view.ItemView;

import java.util.ArrayList;
import java.util.List;

import pro.sketchware.core.ViewUtil;
import pro.sketchware.utility.PropertiesUtil;

public class ItemRecyclerView extends RecyclerView implements ItemView, EditorListItem {

    private final Paint paint;
    private final Rect rect;
    private final float dip;
    private boolean hasSelection;
    private boolean hasFixed;
    private ViewBean viewBean;

    private SimpleAdapter adapter;

    public ItemRecyclerView(Context context) {
        super(context);
        setMinimumWidth((int) ViewUtil.dpToPx(context, 32.0F));
        setMinimumHeight((int) ViewUtil.dpToPx(context, 32.0F));
        dip = ViewUtil.dpToPx(context, 1.0f);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(ViewUtil.dpToPx(getContext(), 2.0f));
        rect = new Rect();
        setDrawingCacheEnabled(true);
        setLayoutManager(new LinearLayoutManager(context));
        setListItem(android.R.layout.simple_list_item_1);
    }

    @Override
    public void setListItem(int layout) {
        adapter = new SimpleAdapter(layout);
        setAdapter(adapter);
        setItemCount(3);
    }

    @Override
    public void setItemCount(int count) {
        adapter.setItemList(PropertiesUtil.generateItems("Recycler item", count));
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
        return hasFixed;
    }

    @Override
    public void setFixed(boolean fixed) {
        hasFixed = fixed;
    }

    public boolean getSelection() {
        return hasSelection;
    }

    @Override
    public void setSelection(boolean selected) {
        hasSelection = selected;
        invalidate();
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        if (hasSelection) {
            paint.setColor(0x9599d5d0);
            rect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
            canvas.drawRect(rect, paint);
        } else {
            paint.setColor(0x60000000);
            int measuredWidth = getMeasuredWidth();
            int measuredHeight = getMeasuredHeight();
            float widthF = (float) measuredWidth;
            canvas.drawLine(0.0f, 0.0f, widthF, 0.0f, paint);
            float heightF = (float) measuredHeight;
            canvas.drawLine(0.0f, 0.0f, 0.0f, heightF, paint);
            canvas.drawLine(widthF, 0.0f, widthF, heightF, paint);
            canvas.drawLine(0.0f, heightF, widthF, heightF, paint);
        }
        super.onDraw(canvas);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding((int) (left * dip), (int) (top * dip), (int) (right * dip), (int) (bottom * dip));
    }

    private static class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.ViewHolder> {

        private final int layout;
        private List<String> dataList;

        public SimpleAdapter(int layout) {
            dataList = new ArrayList<>();
            this.layout = layout;
        }

        public void setItemList(List<String> newList) {
            dataList = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bind(dataList.get(position));
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView textView;

            ViewHolder(View view) {
                super(view);
                textView = itemView.findViewById(android.R.id.text1);
            }

            void bind(String itemText) {
                if (textView != null) {
                    textView.setText(itemText);
                }
            }
        }
    }
}
