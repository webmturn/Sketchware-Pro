package com.besome.sketch.lib.base;

import androidx.activity.OnBackPressedCallback;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import pro.sketchware.core.UIHelper;
import pro.sketchware.core.ViewUtil;
import pro.sketchware.R;

public class BaseBottomSheetDialogActivity extends BaseAppCompatActivity {

    public BottomSheetBehavior<View> bottomSheetBehavior;
    public CoordinatorLayout dialogContainerView;
    public ViewGroup dialogContentView;
    public ConstraintLayout dialogButtonsView;
    public LinearLayout dialogTitleView;
    public TextView dialogTitle;
    public ImageView dialogIcon;
    public TextView dialogDefaultButton;
    public TextView dialogOkButton;
    public TextView dialogCancelButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
        super.setContentView(R.layout.common_bottom_sheet_dialog_layout);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#66000000"));

        overridePendingTransition(R.anim.design_bottom_sheet_slide_in, 0);

        dialogIcon = findViewById(R.id.common_dialog_icon);
        dialogContainerView = findViewById(R.id.common_dialog_container);
        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        dialogContentView = findViewById(R.id.common_dialog_content);
        dialogButtonsView = findViewById(R.id.common_dialog_button_layout);
        dialogTitle = findViewById(R.id.common_dialog_tv_title);
        dialogTitleView = findViewById(R.id.common_dialog_title_layout);
        dialogDefaultButton = findViewById(R.id.common_dialog_default_button);
        dialogOkButton = findViewById(R.id.common_dialog_ok_button);
        dialogCancelButton = findViewById(R.id.common_dialog_cancel_button);

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setHideable(true);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    BaseBottomSheetDialogActivity.super.finish();
                    overridePendingTransition(0, R.anim.design_bottom_sheet_slide_out);
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {
            }
        });

        dialogContainerView.setOnClickListener(v -> {
            if (!UIHelper.a()) {
                finish();
            }
        });
        dialogIcon.setVisibility(View.GONE);
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewUtil.a(this, dialogContentView, layoutResID);
    }

    public void a(boolean var1) {
        if (var1) {
            dialogContainerView.setOnClickListener(v -> {
                if (!UIHelper.a()) {
                    finish();
                }
            });
            return;
        }
        dialogContainerView.setOnClickListener(null);
    }

    public void b(String cancelText) {
        dialogCancelButton.setText(cancelText);
    }

    public void c(String defaultText) {
        dialogDefaultButton.setText(defaultText);
    }

    public void d(String okText) {
        dialogOkButton.setText(okText);
    }

    public void e(String title) {
        dialogTitle.setText(title);
    }

    public void f(int iconResId) {
        dialogIcon.setImageResource(iconResId);
        dialogIcon.setVisibility(View.VISIBLE);
    }

    public void l() {
        dialogButtonsView.setVisibility(View.GONE);
    }

    public void m() {
        dialogTitleView.setVisibility(View.GONE);
    }

    @Override
    public void finish() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void setContentView(View view) {
        dialogContentView.addView(view);
    }

}
