package com.besome.sketch.editor.logic;

import static pro.sketchware.utility.ThemeUtils.getColor;
import static pro.sketchware.utility.ThemeUtils.isDarkThemeEnabled;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

import pro.sketchware.R;
import pro.sketchware.core.BaseBlockView;
import pro.sketchware.core.BlockView;
import pro.sketchware.core.ViewUtil;
import pro.sketchware.databinding.PaletteBlockBinding;

public class PaletteBlock extends LinearLayout {

    public float density = 0.0F;
    private PaletteBlockBinding binding;
    private Context context;

    public PaletteBlock(Context context) {
        super(context);
        initialize(context);
    }

    public PaletteBlock(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    private void initialize(Context context) {
        this.context = context;
        binding = PaletteBlockBinding.inflate(LayoutInflater.from(context), this, true);
        density = ViewUtil.dpToPx(context, 1.0F);
    }

    public BaseBlockView addBlock(String blockType, String spec, String opCode) {
        View view = new View(context);
        view.setLayoutParams(getLayoutParams(8.0F));
        binding.blockBuilder.addView(view);
        BlockView blockView = new BlockView(context, -1, blockType, spec, opCode);
        blockView.setContentDescription(generateContentDescription(opCode));
        blockView.setBlockType(1);
        binding.blockBuilder.addView(blockView);
        return blockView;
    }

    public BaseBlockView addBlock(String blockType, String spec, String opCode, String componentType) {
        View view = new View(context);
        view.setLayoutParams(getLayoutParams(8.0F));
        binding.blockBuilder.addView(view);
        BlockView blockView = new BlockView(context, -1, blockType, spec, opCode, componentType);
        blockView.setContentDescription(generateContentDescription(componentType));
        blockView.setBlockType(1);
        binding.blockBuilder.addView(blockView);
        return blockView;
    }

    public TextView addActionLabel(String title) {
        var textView = new TextView(context);
        textView.setText(title);
        textView.setTextSize(10.0F);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding((int) (density * 8.0F), 0, (int) (density * 8.0F), 0);

        var cardView = new MaterialCardView(context);
        var params = getLayoutParams(30.0F);
        params.setMargins(0, 0, (int) (density * 4), (int) (density * 6));
        cardView.setLayoutParams(params);
        cardView.setCardBackgroundColor(getColor(context, isDarkThemeEnabled(context) ? R.attr.colorSurfaceContainerHigh : R.attr.colorSurfaceContainerHighest));
        cardView.addView(textView);

        binding.actionsContainer.addView(cardView);
        return textView;
    }

    public void clearAll() {
        binding.blockBuilder.removeAllViews();
        binding.actionsContainer.removeAllViews();
    }

    public void clearActions() {
        binding.actionsContainer.removeAllViews();
    }

    public void runBulkUpdate(Runnable action) {
        boolean canSuppressLayout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
        if (canSuppressLayout) {
            suppressLayout(true);
        }
        try {
            action.run();
        } finally {
            if (canSuppressLayout) {
                suppressLayout(false);
            }
            requestLayout();
            invalidate();
        }
    }

    public void addCategoryHeader(String title, int color) {
        var cardView = new MaterialCardView(context);
        var params = getLayoutParams(18.0F);
        params.topMargin = (int) (density * 16.0F);
        cardView.setLayoutParams(params);
        cardView.setCardBackgroundColor(color);
        cardView.setRadius(density * 8f);

        TextView textView = new TextView(context);
        textView.setText(title);
        textView.setTextColor(getColor(context, isDarkThemeEnabled(context) ? R.attr.colorOnSurface : R.attr.colorOnSurfaceInverse));
        textView.setTextSize(10.0F);
        textView.setGravity(Gravity.CENTER | Gravity.LEFT);
        textView.setPadding((int) (density * 12.0F), 0, (int) (density * 12.0F), 0);
        cardView.addView(textView);

        binding.blockBuilder.addView(cardView);
    }

    public void addDeprecatedBlock(String message, String type, String opCode) {
        if (message != null && !message.isEmpty()) {
            addCategoryHeader(message, getColor(context, isDarkThemeEnabled(context) ? R.attr.colorSurfaceContainerHigh : R.attr.colorSurfaceInverse));
        }
        BaseBlockView blockView = addBlock("", type, opCode);
        blockView.blockColor = 0xFFBDBDBD;
        blockView.setTag(opCode);
    }

    public static String generateContentDescription(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(name.charAt(0));
        for (int i = 1; i < name.length(); i++) {
            char currentChar = name.charAt(i);
            if (Character.isUpperCase(currentChar)) {
                if (i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1)) || Character.isLowerCase(name.charAt(i - 1))) {
                    result.append(' ');
                }
            }
            result.append(currentChar);
        }
        return result.toString();
    }

    private LinearLayout.LayoutParams getLayoutParams(float heightMultiplier) {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (density * heightMultiplier));
    }

    public void setDragEnabled(boolean dragEnabled) {
        if (dragEnabled) {
            binding.scroll.enableScroll();
            binding.scrollHorizontal.enableScroll();
        } else {
            binding.scroll.disableScroll();
            binding.scrollHorizontal.disableScroll();
        }
    }

    public void setMinWidth(int minWidth) {
        binding.scroll.setMinimumWidth(minWidth - (int) (density * 5.0F));
        binding.scrollHorizontal.setMinimumWidth(minWidth - (int) (density * 5.0F));
        getLayoutParams().width = minWidth;
    }

    public void setUseScroll(boolean useScroll) {
        binding.scroll.setUseScroll(useScroll);
        binding.scrollHorizontal.setUseScroll(useScroll);
    }

    public int filterBlocks(String query) {
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        final int[] visibleCount = {0};
        runBulkUpdate(() -> {
            int childCount = binding.blockBuilder.getChildCount();
            boolean lastHeaderVisible = false;
            View lastHeader = null;

            for (int i = 0; i < childCount; i++) {
                View child = binding.blockBuilder.getChildAt(i);

                if (child instanceof MaterialCardView) {
                    child.setVisibility(View.GONE);
                    lastHeader = child;
                    lastHeaderVisible = false;
                    continue;
                }

                if (child instanceof BlockView blockView) {
                    boolean matches = blockView.matchesPaletteSearchQuery(lowerQuery);
                    child.setVisibility(matches ? View.VISIBLE : View.GONE);
                    if (matches) {
                        visibleCount[0]++;
                        if (lastHeader != null && !lastHeaderVisible) {
                            lastHeader.setVisibility(View.VISIBLE);
                            lastHeaderVisible = true;
                        }
                    }

                    if (i > 0) {
                        View spacer = binding.blockBuilder.getChildAt(i - 1);
                        if (!(spacer instanceof BlockView) && !(spacer instanceof MaterialCardView)) {
                            spacer.setVisibility(matches ? View.VISIBLE : View.GONE);
                        }
                    }
                }
            }
        });
        return visibleCount[0];
    }
}
