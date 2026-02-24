package com.besome.sketch.editor.manage;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.MoreBlockCollectionBean;
import com.besome.sketch.editor.logic.BlockPane;
import com.besome.sketch.lib.base.BaseAppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;

import pro.sketchware.core.DeviceUtil;
import pro.sketchware.core.UniqueNameValidator;
import pro.sketchware.core.MoreBlockCollectionManager;
import pro.sketchware.core.BlockView;
import pro.sketchware.core.FieldBlockView;
import pro.sketchware.core.BaseBlockView;
import pro.sketchware.core.SketchToast;
import mod.hey.studios.util.Helper;
import mod.jbk.util.BlockUtil;
import pro.sketchware.R;
import pro.sketchware.databinding.ManageCollectionShowBlockBinding;
import pro.sketchware.tools.ImageFactory;
import pro.sketchware.utility.SketchwareUtil;

public class ShowMoreBlockCollectionActivity extends BaseAppCompatActivity implements View.OnClickListener {

    private String moreBlockName;
    private BlockPane pane;
    private EditText moreBlockNameEditorText;
    private UniqueNameValidator moreBlockNameValidator;
    private ManageCollectionShowBlockBinding binding;

    private void addBlocks(ArrayList<BlockBean> blockBeans) {
        HashMap<Integer, BlockView> blockViewMap = new HashMap<>();

        boolean isFirstBlock = true;
        for (BlockBean blockBean : blockBeans) {
            BlockView block = getBlock(blockBean);
            int blockId = (Integer) block.getTag();
            blockViewMap.put(blockId, block);

            pane.nextBlockId = Math.max(pane.nextBlockId, blockId + 1);
            pane.addBlock(block, 0, 0);

            if (isFirstBlock) {
                pane.getRoot().setNextBlock(block);
                isFirstBlock = false;
            }
        }

        for (BlockBean blockBean : blockBeans) {
            BlockView block = blockViewMap.get(Integer.valueOf(blockBean.id));

            if (block != null) {
                int subStack1Id = blockBean.subStack1;
                BlockView subStack1;
                if (subStack1Id >= 0 && (subStack1 = blockViewMap.get(subStack1Id)) != null) {
                    block.setSubstack1Block(subStack1);
                }

                int subStack2Id = blockBean.subStack2;
                BlockView subStack2;
                if (subStack2Id >= 0 && (subStack2 = blockViewMap.get(subStack2Id)) != null) {
                    block.setSubstack2Block(subStack2);
                }

                int nextBlockId = blockBean.nextBlock;
                BlockView nextBlock;
                if (nextBlockId >= 0 && (nextBlock = blockViewMap.get(nextBlockId)) != null) {
                    block.setNextBlock(nextBlock);
                }

                ArrayList<String> parameters = blockBean.parameters;
                for (int i = 0; i < parameters.size(); i++) {
                    String parameter = parameters.get(i);

                    if (parameter != null && !parameter.isEmpty()) {
                        if (parameter.charAt(0) == '@') {
                            BlockView parameterBlock = blockViewMap.get(Integer.valueOf(parameter.substring(1)));
                            if (parameterBlock != null) {
                                block.replaceParameter((BaseBlockView) block.childViews.get(i), parameterBlock);
                            }
                        } else {
                            ((FieldBlockView) block.childViews.get(i)).setArgValue(parameter);
                            block.recalculateToRoot();
                        }
                    }
                }
            }
        }
        pane.getRoot().layoutChain();
        pane.updatePaneSize();
    }

    private void addHeaderBlock(String spec) {
        pane.createHeaderBlock(spec, "moreBlock");
        var header = pane.getRoot();
        BlockUtil.loadPreviewBlockVariables(pane, header, spec);
        header.layoutChain();
    }

    private void resizeBottomViews() {
        int height = getResources().getDisplayMetrics().heightPixels;
        binding.layoutButton.measure(0, 0);
        binding.editor.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ((height - DeviceUtil.getToolbarHeight((Context) this)) - DeviceUtil.getStatusBarHeight(this)) - binding.layoutButton.getMeasuredHeight()));
        binding.editor.requestLayout();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.save_button && moreBlockNameValidator.isValid()) {
            MoreBlockCollectionManager.getInstance().renameMoreBlock(moreBlockName, Helper.getText(moreBlockNameEditorText), true);
            SketchToast.toast(getApplicationContext(), Helper.getResString(R.string.design_manager_message_edit_complete), SketchToast.TOAST_NORMAL).show();
            finish();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resizeBottomViews();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ManageCollectionShowBlockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(Helper.getResString(R.string.design_manager_block_detail_actionbar_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        binding.toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));

        moreBlockName = getIntent().getStringExtra("block_name");
        binding.editor.setScrollEnabled(true);
        pane = binding.editor.getBlockPane();

        moreBlockNameEditorText = binding.edInput.getEditText();
        moreBlockNameEditorText.setPrivateImeOptions("defaultInputmode=english;");
        moreBlockNameEditorText.setText(moreBlockName);
        binding.edInput.setHint(Helper.getResString(R.string.design_manager_block_hint_enter_block_name));

        binding.saveButton.setText(Helper.getResString(R.string.common_word_save));
        binding.saveButton.setOnClickListener(this);
        moreBlockNameValidator = new UniqueNameValidator(this, binding.edInput.getTextInputLayout(), MoreBlockCollectionManager.getInstance().getMoreBlockNames());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        MoreBlockCollectionBean moreBlock = MoreBlockCollectionManager.getInstance().getMoreBlockByName(moreBlockName);
        if (moreBlock != null) {
            addHeaderBlock(moreBlock.spec);
            addBlocks(moreBlock.blocks);
            resizeBottomViews();
        } else {
            SketchwareUtil.toastError(Helper.getResString(R.string.error_corrupt_more_block));
            finish();
        }
    }

    private BlockView getBlock(BlockBean blockBean) {
        return new BlockView(this, Integer.parseInt(blockBean.id), blockBean.spec, blockBean.type, blockBean.typeName, blockBean.opCode);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem saveImageItem = menu.add(0, 12, 0, "Save image");
        saveImageItem.setIcon(R.drawable.full_image_48);
        saveImageItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 12) {
            if (ImageFactory.saveBitmap(binding.editor.getChildAt(0), moreBlockName).exists()) {
                SketchwareUtil.toast(String.format(Helper.getResString(R.string.toast_block_image_saved), moreBlockName));
            } else {
                SketchwareUtil.toastError(Helper.getResString(R.string.error_save_image_failed));
            }
        }

        return super.onOptionsItemSelected(item);
    }
}