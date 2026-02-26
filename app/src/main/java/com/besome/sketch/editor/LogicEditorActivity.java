package com.besome.sketch.editor;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.BlockCollectionBean;
import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.HistoryBlockBean;
import com.besome.sketch.beans.MoreBlockCollectionBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.design.DesignActivity;
import com.besome.sketch.editor.component.AddComponentBottomSheet;
import com.besome.sketch.editor.logic.BlockPane;
import com.besome.sketch.editor.logic.LogicTopMenu;
import com.besome.sketch.editor.logic.PaletteBlock;
import com.besome.sketch.editor.logic.PaletteSelector;
import com.besome.sketch.editor.makeblock.MakeBlockActivity;
import com.besome.sketch.editor.manage.ShowBlockCollectionActivity;
import com.besome.sketch.editor.view.ViewDummy;
import com.besome.sketch.editor.view.ViewLogicEditor;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.besome.sketch.lib.ui.ColorPickerDialog;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.core.SharedPrefsHelper;
import pro.sketchware.core.FormatUtil;
import pro.sketchware.core.BlockInterpreter;
import pro.sketchware.core.DeviceUtil;
import pro.sketchware.core.BaseAsyncTask;
import pro.sketchware.core.BlockCollectionManager;
import pro.sketchware.core.UniqueNameValidator;
import pro.sketchware.core.LayoutGenerator;
import pro.sketchware.core.MoreBlockCollectionManager;
import pro.sketchware.core.BlockView;
import pro.sketchware.core.FieldBlockView;
import pro.sketchware.core.BaseBlockView;
import pro.sketchware.core.DefinitionBlockView;
import pro.sketchware.core.BlockSizeListener;
import pro.sketchware.core.IdentifierValidator;
import pro.sketchware.core.BlockHistoryManager;
import pro.sketchware.core.ProjectDataStore;
import pro.sketchware.core.ProjectDataManager;
import pro.sketchware.core.BuildConfig;
import pro.sketchware.core.ResourceManager;
import pro.sketchware.core.UIHelper;
import pro.sketchware.core.SketchwareConstants;
import pro.sketchware.core.BlockConstants;
import pro.sketchware.core.ViewUtil;
import pro.sketchware.core.StringResource;
import pro.sketchware.core.ProjectFilePaths;
import dev.aldi.sayuti.block.ExtraPaletteBlock;
import mod.bobur.VectorDrawableLoader;
import mod.hey.studios.editor.view.IdGenerator;
import mod.hey.studios.moreblock.ReturnMoreblockManager;
import mod.hey.studios.moreblock.importer.MoreblockImporterDialog;
import mod.hey.studios.project.ProjectSettings;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.asd.AsdDialog;
import mod.jbk.editor.manage.MoreblockImporter;
import mod.jbk.util.BlockUtil;
import mod.jbk.util.LogUtil;
import mod.pranav.viewbinding.ViewBindingBuilder;
import pro.sketchware.R;
import pro.sketchware.activities.editor.view.CodeViewerActivity;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.databinding.ImagePickerItemBinding;
import pro.sketchware.databinding.SearchWithRecyclerViewBinding;
import pro.sketchware.menu.ExtraMenuBean;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.SvgUtils;

@SuppressLint({"ClickableViewAccessibility", "RtlHardcoded", "SetTextI18n", "DefaultLocale"})
public class LogicEditorActivity extends BaseAppCompatActivity implements View.OnClickListener, BlockSizeListener, View.OnTouchListener, MoreblockImporterDialog.CallBack {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int[] locationBuffer = new int[2];
    private final FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
    public ProjectFileBean projectFile;
    public PaletteBlock paletteBlock;
    public BlockPane blockPane;
    public String scId = "";
    public String id = "";
    public String eventName = "";
    private Vibrator vibrator;
    private LinearLayout paletteLayout, paletteArea;
    private FloatingActionButton openBlocksMenuButton;
    private LogicTopMenu logicTopMenu;
    private LogicEditorDrawer editorDrawer;
    private ObjectAnimator paletteShowAnimator, paletteHideAnimator, topMenuShowAnimator, topMenuHideAnimator, drawerShowAnimator, drawerHideAnimator;
    private ExtraPaletteBlock extraPaletteBlock;
    private ViewLogicEditor viewLogicEditor;
    private ViewDummy dummy;
    private PaletteSelector paletteSelector;
    private ActivityResultLauncher<Intent> openResourcesEditor;
    private ActivityResultLauncher<Intent> makeBlockLauncher;
    private BlockView dragSourceParent;
    private float posInitY, posInitX, touchOriginX, touchOriginY;
    private int minDist, dummyOffsetX, dragConnectionType, dragParameterIndex;
    private int dummyOffsetY = -30;
    private View currentTouchedView;
    private boolean isVibrationEnabled, isDragged, paletteAnimatorsInitialized, isPaletteVisible, topMenuAnimatorsInitialized, isTopMenuVisible, drawerAnimatorsInitialized, isDrawerVisible;
    private ArrayList<BlockBean> savedBlockBean = new ArrayList<>();
    private final Runnable longPressed = this::startDragMode;
    private Boolean isViewBindingEnabled;

    private SvgUtils svgUtils;
    private final FilePathUtil fpu = new FilePathUtil();

    public static ArrayList<String> getAllJavaFileNames(String projectScId) {
        ArrayList<String> javaFileNames = new ArrayList<>();
        for (ProjectFileBean projectFile : ProjectDataManager.getFileManager(projectScId).getActivities()) {
            javaFileNames.add(projectFile.getJavaName());
        }
        return javaFileNames;
    }

    public static ArrayList<String> getAllXmlFileNames(String projectScId) {
        ArrayList<String> xmlFileNames = new ArrayList<>();
        for (ProjectFileBean projectFile : ProjectDataManager.getFileManager(projectScId).getActivities()) {
            String xmlName = projectFile.getXmlName();
            if (xmlName != null && !xmlName.isEmpty()) {
                xmlFileNames.add(xmlName);
            }
        }
        return xmlFileNames;
    }

    private void loadEventBlocks() {
        crashlytics.log("Loading event blocks");
        ArrayList<BlockBean> eventBlocks = ProjectDataManager.getProjectDataManager(scId).getBlocks(projectFile.getJavaName(), id + "_" + eventName);
        if (eventBlocks != null) {
            if (eventBlocks.isEmpty()) {
                runOnUiThread(() -> togglePaletteVisibility(isPaletteVisible));
            }

            ArrayList<BlockView> createdBlocks = new ArrayList<>();
            HashMap<Integer, BlockView> blockIdsAndBlocks = new HashMap<>();
            for (BlockBean next : eventBlocks) {
                if (eventName.equals("onTextChanged") && next.opCode.equals("getArg") && next.spec.equals("text")) {
                    next.spec = "charSeq";
                }
                BlockView blockView = createBlockView(next);
                createdBlocks.add(blockView);
                blockIdsAndBlocks.put((Integer) blockView.getTag(), blockView);
                blockPane.nextBlockId = Math.max(blockPane.nextBlockId, (Integer) blockView.getTag() + 1);
            }

            runOnUiThread(() -> {
                android.util.Log.d("BlockLoad", "UI batch START blocks=" + createdBlocks.size());
                long t0 = System.currentTimeMillis();
                for (int idx = 0; idx < createdBlocks.size(); idx++) {
                    BlockView blockView = createdBlocks.get(idx);
                    blockPane.addBlock(blockView, 0, 0);
                    blockView.setOnTouchListener(this);
                    if (idx == 0) {
                        blockPane.getRoot().setNextBlock(blockView);
                    }
                }
                long t1 = System.currentTimeMillis();
                android.util.Log.d("BlockLoad", "UI addView done: " + (t1 - t0) + "ms");

                for (BlockBean next2 : eventBlocks) {
                    BlockView block = blockIdsAndBlocks.get(Integer.valueOf(next2.id));
                    if (block != null) {
                        BlockView subStack1RootBlock;
                        if (next2.subStack1 >= 0 && (subStack1RootBlock = blockIdsAndBlocks.get(next2.subStack1)) != null) {
                            block.setSubstack1Block(subStack1RootBlock);
                        }
                        BlockView subStack2RootBlock;
                        if (next2.subStack2 >= 0 && (subStack2RootBlock = blockIdsAndBlocks.get(next2.subStack2)) != null) {
                            block.setSubstack2Block(subStack2RootBlock);
                        }
                        BlockView nextBlock;
                        if (next2.nextBlock >= 0 && (nextBlock = blockIdsAndBlocks.get(next2.nextBlock)) != null) {
                            block.setNextBlock(nextBlock);
                        }
                        for (int i = 0; i < next2.parameters.size() && i < block.childViews.size(); i++) {
                            String parameter = next2.parameters.get(i);
                            if (parameter != null && !parameter.isEmpty()) {
                                if (parameter.charAt(0) == '@') {
                                    BlockView parameterBlock = blockIdsAndBlocks.get(Integer.valueOf(parameter.substring(1)));
                                    if (parameterBlock != null && block.childViews.get(i) instanceof BaseBlockView) {
                                        block.replaceParameter((BaseBlockView) block.childViews.get(i), parameterBlock);
                                    }
                                } else if (block.childViews.get(i) instanceof FieldBlockView fieldBlock) {
                                    fieldBlock.setArgValue(parameter);
                                }
                            }
                        }
                    }
                }
                long t2 = System.currentTimeMillis();
                android.util.Log.d("BlockLoad", "UI connect done: " + (t2 - t1) + "ms");

                blockPane.getRoot().layoutChain();
                long t3 = System.currentTimeMillis();
                android.util.Log.d("BlockLoad", "UI k() done: " + (t3 - t2) + "ms");
                blockPane.updatePaneSize();
                long t4 = System.currentTimeMillis();
                android.util.Log.d("BlockLoad", "UI b() done: " + (t4 - t3) + "ms total=" + (t4 - t0) + "ms");
            });
        }
    }

    private void redo() {
        if (!isDragged) {
            HistoryBlockBean historyBlockBean = BlockHistoryManager.getInstance(scId).redo(buildHistoryKey());
            if (historyBlockBean != null) {
                int actionType = historyBlockBean.getActionType();
                if (actionType == HistoryBlockBean.ACTION_TYPE_ADD) {
                    int[] locationOnScreen = new int[2];
                    blockPane.getLocationOnScreen(locationOnScreen);
                    addBlockBeans(historyBlockBean.getAddedData(), historyBlockBean.getCurrentX() + locationOnScreen[0], historyBlockBean.getCurrentY() + locationOnScreen[1], true);
                    if (historyBlockBean.getCurrentParentData() != null) {
                        connectBlock(historyBlockBean.getCurrentParentData(), true);
                    }
                } else if (actionType == HistoryBlockBean.ACTION_TYPE_UPDATE) {
                    connectBlock(historyBlockBean.getCurrentUpdateData(), true);
                } else if (actionType == HistoryBlockBean.ACTION_TYPE_REMOVE) {
                    ArrayList<BlockBean> removedData = historyBlockBean.getRemovedData();

                    for (int i = removedData.size() - 1; i >= 0; i--) {
                        blockPane.removeBlock(removedData.get(i), false);
                    }
                    if (historyBlockBean.getCurrentParentData() != null) {
                        connectBlock(historyBlockBean.getCurrentParentData(), true);
                    }
                } else if (actionType == HistoryBlockBean.ACTION_TYPE_MOVE) {
                    for (BlockBean afterMoveData : historyBlockBean.getAfterMoveData()) {
                        blockPane.removeBlock(afterMoveData, true);
                    }

                    int[] locationOnScreen = new int[2];
                    blockPane.getLocationOnScreen(locationOnScreen);
                    addBlockBeans(historyBlockBean.getAfterMoveData(), historyBlockBean.getCurrentX() + locationOnScreen[0], historyBlockBean.getCurrentY() + locationOnScreen[1], true);
                    if (historyBlockBean.getCurrentParentData() != null) {
                        connectBlock(historyBlockBean.getCurrentParentData(), true);
                    }

                    if (historyBlockBean.getCurrentOriginalParent() != null) {
                        connectBlock(historyBlockBean.getCurrentOriginalParent(), true);
                    }
                }
            }
            invalidateOptionsMenu();
        }
    }

    public void refreshOptionsMenu() {
        invalidateOptionsMenu();
    }

    public void saveBlocks() {
        ProjectDataStore projectDataStore = ProjectDataManager.getProjectDataManager(scId);
        String javaName = projectFile.getJavaName();
        projectDataStore.putBlocks(javaName, id + "_" + eventName, blockPane.getBlocks());
    }

    public void showAddListDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.logic_editor_title_add_new_list);
        View dialogView = ViewUtil.inflateLayout(this, R.layout.logic_popup_add_list);
        RadioGroup radioGroup = dialogView.findViewById(R.id.rg_type);
        TextInputEditText editText = dialogView.findViewById(R.id.ed_input);
        IdentifierValidator listNameValidator = new IdentifierValidator(getContext(), dialogView.findViewById(R.id.ti_input), BlockConstants.RESERVED_KEYWORDS, BlockConstants.COMPONENT_TYPES, ProjectDataManager.getProjectDataManager(scId).getAllIdentifiers(projectFile));
        dialog.setView(dialogView);
        dialog.setPositiveButton(R.string.common_word_add, (v, which) -> {
            if (listNameValidator.isValid()) {
                int listType = 1;
                int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                if (checkedRadioButtonId != R.id.rb_int) {
                    if (checkedRadioButtonId == R.id.rb_string) {
                        listType = 2;
                    } else if (checkedRadioButtonId == R.id.rb_map) {
                        listType = 3;
                    }
                }

                addListVariable(listType, Helper.getText(editText));
                v.dismiss();
            }
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    private void showAddNewVariableDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.logic_editor_title_add_new_variable);

        View customView = ViewUtil.inflateLayout(this, R.layout.logic_popup_add_variable);
        RadioGroup radioGroup = customView.findViewById(R.id.rg_type);
        TextInputEditText editText = customView.findViewById(R.id.ed_input);
        IdentifierValidator nameValidator = new IdentifierValidator(getContext(), customView.findViewById(R.id.ti_input), BlockConstants.RESERVED_KEYWORDS, BlockConstants.COMPONENT_TYPES, ProjectDataManager.getProjectDataManager(scId).getAllIdentifiers(projectFile));
        dialog.setView(customView);
        dialog.setPositiveButton(R.string.common_word_add, (v, which) -> {
            int variableType = 1;
            if (radioGroup.getCheckedRadioButtonId() == R.id.rb_boolean) {
                variableType = 0;
            } else if (radioGroup.getCheckedRadioButtonId() != R.id.rb_int) {
                if (radioGroup.getCheckedRadioButtonId() == R.id.rb_string) {
                    variableType = 2;
                } else if (radioGroup.getCheckedRadioButtonId() == R.id.rb_map) {
                    variableType = 3;
                }
            }

            if (nameValidator.isValid()) {
                addVariable(variableType, Helper.getText(editText));
                v.dismiss();
            }
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    public void openResourcesEditor() {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), ResourcesEditorActivity.class);
        intent.putExtra("sc_id", scId);
        openResourcesEditor.launch(intent);
    }

    public void showMoreBlockImporter() {
        ArrayList<MoreBlockCollectionBean> moreBlocks = MoreBlockCollectionManager.getInstance().getMoreBlocks();
        new MoreblockImporterDialog(this, moreBlocks, this).show();
    }

    public void showRemoveListDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.logic_editor_title_remove_list);
        View dialogView = ViewUtil.inflateLayout(this, R.layout.property_popup_selector_single);
        ViewGroup viewGroup = dialogView.findViewById(R.id.rg_content);
        for (Pair<Integer, String> list : ProjectDataManager.getProjectDataManager(scId).getListVariables(projectFile.getJavaName())) {
            viewGroup.addView(createRadioButton(list.second));
        }
        dialog.setView(dialogView);
        dialog.setPositiveButton(R.string.common_word_remove, (v, which) -> {
            int childCount = viewGroup.getChildCount();
            int radioIdx = 0;
            while (radioIdx < childCount) {
                RadioButton radioButton = (RadioButton) viewGroup.getChildAt(radioIdx);
                if (radioButton.isChecked()) {
                    if (!blockPane.hasListReference(Helper.getText(radioButton))) {
                        if (!ProjectDataManager.getProjectDataManager(scId).isListUsedInBlocks(projectFile.getJavaName(), Helper.getText(radioButton), id + "_" + eventName)) {
                            removeListVariable(Helper.getText(radioButton));
                        }
                    }
                    Toast.makeText(getContext(), R.string.logic_editor_message_currently_used_list, Toast.LENGTH_SHORT).show();
                    return;
                }
                radioIdx++;
            }
            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    public void showRemoveVariableDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.logic_editor_title_remove_variable);
        View dialogView = ViewUtil.inflateLayout(this, R.layout.property_popup_selector_single);
        ViewGroup viewGroup = dialogView.findViewById(R.id.rg_content);
        for (Pair<Integer, String> next : ProjectDataManager.getProjectDataManager(scId).getVariables(projectFile.getJavaName())) {
            RadioButton radioButton = createRadioButton(next.second);
            radioButton.setTag(next.first);
            viewGroup.addView(radioButton);
        }
        dialog.setView(dialogView);
        dialog.setPositiveButton(R.string.common_word_remove, (v, which) -> {
            int childCount = viewGroup.getChildCount();
            int radioIdx = 0;
            while (radioIdx < childCount) {
                RadioButton radioButton = (RadioButton) viewGroup.getChildAt(radioIdx);
                if (radioButton.isChecked()) {
                    if (!blockPane.hasMapReference(Helper.getText(radioButton))) {
                        if (!ProjectDataManager.getProjectDataManager(scId).isVariableUsedInBlocks(projectFile.getJavaName(), Helper.getText(radioButton), id + "_" + eventName)) {
                            removeVariable(Helper.getText(radioButton));
                        }
                    }
                    Toast.makeText(getContext(), R.string.logic_editor_message_currently_used_variable, Toast.LENGTH_SHORT).show();
                    return;
                }
                radioIdx++;
            }
            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    public void saveProject() {
        try {
            new Handler(Looper.getMainLooper()).postDelayed(() -> new ProjectSaver(this).execute(), 500L);
        } catch (Exception e) {
            crashlytics.recordException(e);
        }
    }

    private void undo() {
        if (!isDragged) {
            HistoryBlockBean history = BlockHistoryManager.getInstance(scId).undo(buildHistoryKey());
            if (history != null) {
                int actionType = history.getActionType();
                if (actionType == HistoryBlockBean.ACTION_TYPE_ADD) {
                    ArrayList<BlockBean> addedData = history.getAddedData();
                    for (int i = addedData.size() - 1; i >= 0; i--) {
                        blockPane.removeBlock(addedData.get(i), false);
                    }

                    if (history.getPrevParentData() != null) {
                        history.getPrevParentData().print();
                        connectBlock(history.getPrevParentData(), true);
                    }
                } else if (actionType == HistoryBlockBean.ACTION_TYPE_UPDATE) {
                    connectBlock(history.getPrevUpdateData(), true);
                } else if (actionType == HistoryBlockBean.ACTION_TYPE_REMOVE) {
                    int[] oLocationOnScreen = new int[2];
                    blockPane.getLocationOnScreen(oLocationOnScreen);
                    addBlockBeans(history.getRemovedData(), history.getCurrentX() + oLocationOnScreen[0], history.getCurrentY() + oLocationOnScreen[1], true);

                    if (history.getPrevParentData() != null) {
                        connectBlock(history.getPrevParentData(), true);
                    }
                } else if (actionType == HistoryBlockBean.ACTION_TYPE_MOVE) {
                    for (BlockBean beforeMoveBlock : history.getBeforeMoveData()) {
                        blockPane.removeBlock(beforeMoveBlock, true);
                    }

                    int[] oLocationOnScreen = new int[2];
                    blockPane.getLocationOnScreen(oLocationOnScreen);
                    addBlockBeans(history.getBeforeMoveData(), history.getPrevX() + oLocationOnScreen[0], history.getPrevY() + oLocationOnScreen[1], true);

                    if (history.getPrevParentData() != null) {
                        connectBlock(history.getPrevParentData(), true);
                    }
                    if (history.getPrevOriginalParent() != null) {
                        connectBlock(history.getPrevOriginalParent(), true);
                    }
                }
            }

            invalidateOptionsMenu();
        }
    }

    public BlockView dropBlockOnPane(BlockView rs, int x, int y, boolean isParameter) {
        BlockView droppedBlock = blockPane.dropBlock(rs, x, y, isParameter);
        if (!isParameter) {
            droppedBlock.setOnTouchListener(this);
        }
        return droppedBlock;
    }

    public void addDeprecatedBlock(String message, String type, String opCode) {
        paletteBlock.addDeprecatedBlock(message, type, opCode);
    }

    public View createPaletteBlock(String spec, String opCode) {
        BaseBlockView paletteBlockView = paletteBlock.addBlock("", spec, opCode);
        paletteBlockView.setTag(opCode);
        paletteBlockView.setClickable(true);
        paletteBlockView.setOnTouchListener(this);
        return paletteBlockView;
    }

    public final View createPaletteBlockWithSpec(String type, String spec, String opCode) {
        BaseBlockView paletteBlockView = paletteBlock.addBlock(type, spec, opCode);
        paletteBlockView.setTag(opCode);
        paletteBlockView.setClickable(true);
        paletteBlockView.setOnTouchListener(this);
        return paletteBlockView;
    }

    public final View createPaletteBlockWithComponent(String type, String spec, String opCode, String componentType) {
        BaseBlockView paletteBlockView = paletteBlock.addBlock(type, spec, opCode, componentType);
        paletteBlockView.setTag(componentType);
        paletteBlockView.setClickable(true);
        paletteBlockView.setOnTouchListener(this);
        return paletteBlockView;
    }

    private ImageView setImageViewContent(String name) {
        float dp = ViewUtil.dpToPx(this, 1.0f);
        int size = (int) (dp * 48);
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundResource(R.drawable.bg_outline);

        if ("NONE".equals(name)) {
            return imageView;
        }

        if ("default_image".equals(name)) {
            int resId = getResources().getIdentifier(name, "drawable", getPackageName());
            if (resId != 0) imageView.setImageResource(resId);
            return imageView;
        }

        File imageFile = new File(ProjectDataManager.getResourceManager(scId).getImagePath(name));
        if (imageFile.exists()) {
            String path = imageFile.getAbsolutePath();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);

            if (path.endsWith(".xml")) {
                svgUtils.loadImage(imageView, fpu.getSvgFullPath(scId, name));
            } else {
                Glide.with(this)
                        .load(uri)
                        .signature(ResourceManager.getCacheSignature())
                        .error(R.drawable.ic_remove_grey600_24dp)
                        .into(imageView);
            }
        } else {
            try {
                new VectorDrawableLoader().setImageVectorFromFile(imageView, new VectorDrawableLoader().getVectorFullPath(DesignActivity.sc_id, name));
            } catch (Exception e) {
                imageView.setImageResource(R.drawable.ic_remove_grey600_24dp);
            }
        }

        return imageView;
    }

    public final ArrayList<BlockBean> addBlockBeans(ArrayList<BlockBean> blockBeans, int x, int y, boolean layoutChain) {
        HashMap<Integer, Integer> idMapping = new HashMap<>();
        ArrayList<BlockBean> clonedBlocks = new ArrayList<>();
        for (BlockBean next : blockBeans) {
            if (next.id != null && !next.id.isEmpty()) {
                clonedBlocks.add(next.clone());
            }
        }
        for (BlockBean next2 : clonedBlocks) {
            if (Integer.parseInt(next2.id) >= 99000000) {
                idMapping.put(Integer.valueOf(next2.id), blockPane.nextBlockId);
                blockPane.nextBlockId = blockPane.nextBlockId + 1;
            } else {
                idMapping.put(Integer.valueOf(next2.id), Integer.valueOf(next2.id));
            }
        }
        int size = clonedBlocks.size();
        while (true) {
            size--;
            if (size < 0) {
                break;
            }
            BlockBean blockBean = clonedBlocks.get(size);
            if (!isBlockValid(blockBean)) {
                clonedBlocks.remove(size);
                idMapping.remove(Integer.valueOf(blockBean.id));
            }
        }
        for (BlockBean block : clonedBlocks) {
            if (idMapping.containsKey(Integer.valueOf(block.id))) {
                block.id = String.valueOf(idMapping.get(Integer.valueOf(block.id)));
            } else {
                block.id = "";
            }
            for (int j = 0; j < block.parameters.size(); j++) {
                String parameter = block.parameters.get(j);
                if (parameter != null && !parameter.isEmpty() && parameter.charAt(0) == '@') {
                    int parameterId = Integer.parseInt(parameter.substring(1));
                    int parameterAsBlockId = idMapping.containsKey(parameterId) ? idMapping.get(parameterId) : 0;
                    if (parameterAsBlockId >= 0) {
                        block.parameters.set(j, '@' + String.valueOf(parameterAsBlockId));
                    } else {
                        block.parameters.set(j, "");
                    }
                }
            }
            if (block.subStack1 >= 0 && idMapping.containsKey(block.subStack1)) {
                block.subStack1 = idMapping.get(block.subStack1);
            }
            if (block.subStack2 >= 0 && idMapping.containsKey(block.subStack2)) {
                block.subStack2 = idMapping.get(block.subStack2);
            }
            if (block.nextBlock >= 0 && idMapping.containsKey(block.nextBlock)) {
                block.nextBlock = idMapping.get(block.nextBlock);
            }
        }
        BlockView firstBlock = null;
        for (int j = 0; j < clonedBlocks.size(); j++) {
            BlockBean blockBean = clonedBlocks.get(j);
            if (blockBean.id != null && !blockBean.id.isEmpty()) {
                BlockView block = createBlockView(blockBean);
                if (j == 0) {
                    firstBlock = block;
                }
                blockPane.addBlock(block, x, y);
                block.setOnTouchListener(this);
            }
        }
        for (BlockBean block : clonedBlocks) {
            if (block.id != null && !block.id.isEmpty()) {
                connectBlock(block, false);
            }
        }
        if (firstBlock != null && layoutChain) {
            firstBlock.getRootBlock().layoutChain();
            blockPane.updatePaneSize();
        }
        return clonedBlocks;
    }

    @Override
    public void onBlockSizeChanged(int width, int height) {
        extraPaletteBlock.setBlock(width, height);
    }

    public void addListVariable(int i, String variableName) {
        ProjectDataManager.getProjectDataManager(scId).addListVariable(projectFile.getJavaName(), i, variableName);
        onBlockSizeChanged(1, 0xffcc5b22);
    }

    public void trackDragSource(BlockView rs) {
        dragSourceParent = null;
        dragParameterIndex = -1;
        dragConnectionType = 0;
        int[] iArr = new int[2];
        BlockView rs2 = rs.parentBlock;
        if (rs2 != null) {
            dragSourceParent = rs2;
            if (savedBlockBean.isEmpty()) {
                savedBlockBean = blockPane.getBlocks();
            }
        }
        BlockView rs3 = dragSourceParent;
        if (rs3 == null) {
            return;
        }
        if (rs3.nextBlock == (Integer) rs.getTag()) {
            dragConnectionType = 0;
        } else if (dragSourceParent.subStack1 == (Integer) rs.getTag()) {
            dragConnectionType = 2;
        } else if (dragSourceParent.subStack2 == (Integer) rs.getTag()) {
            dragConnectionType = 3;
        } else if (dragSourceParent.childViews.contains(rs)) {
            dragConnectionType = 5;
            dragParameterIndex = dragSourceParent.childViews.indexOf(rs);
        }
    }

    public void handleBlockFieldClick(BlockView rs, float touchX, float touchY) {
        for (View next : rs.childViews) {
            if ((next instanceof FieldBlockView) && next.getX() < touchX && next.getX() + next.getWidth() > touchX && next.getY() < touchY && next.getY() + next.getHeight() > touchY) {
                new ExtraMenuBean(this).defineMenuSelector((FieldBlockView) next);
                return;
            }
        }
    }

    public void setFieldValue(FieldBlockView ss, Object value) {
        BlockBean clone = ss.parentBlock.getBean().clone();
        ss.setArgValue(value);
        ss.parentBlock.recalculateToRoot();
        ss.parentBlock.getRootBlock().layoutChain();
        ss.parentBlock.blockPane.updatePaneSize();
        BlockHistoryManager.getInstance(scId).recordUpdate(buildHistoryKey(), clone, ss.parentBlock.getBean().clone());
        refreshOptionsMenu();
    }

    public void pickImage(FieldBlockView ss, String propertyKey) {
        boolean selectingBackgroundImage = "property_background_resource".equals(propertyKey);
        boolean selectingImage = !selectingBackgroundImage && "property_image".equals(propertyKey);
        AtomicReference<String> selectedImage = new AtomicReference<>("");

        SearchWithRecyclerViewBinding binding = SearchWithRecyclerViewBinding.inflate(getLayoutInflater());

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        if (selectingImage) {
            dialog.setTitle(R.string.logic_editor_title_select_image);
        } else if (selectingBackgroundImage) {
            dialog.setTitle(R.string.logic_editor_title_select_image_background);
        }

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<String> images = ProjectDataManager.getResourceManager(scId).getImageNames();
        images.addAll(new VectorDrawableLoader().getVectorDrawables(DesignActivity.sc_id));
        if (selectingImage) {
            images.add(0, "default_image");
        } else if (selectingBackgroundImage) {
            images.add(0, "NONE");
        }

        ImagePickerAdapter adapter = new ImagePickerAdapter(images, (String) ss.getArgValue(), selectedImage::set);
        binding.recyclerView.setAdapter(adapter);


        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().toLowerCase();
                adapter.filter(query);
            }
        });

        dialog.setPositiveButton(R.string.common_word_save, (v, which) -> {
            String selectedImg = selectedImage.get();
            if (!selectedImg.isEmpty()) {
                setFieldValue(ss, selectedImage.get());
            }
        });

        dialog.setView(binding.getRoot());
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    public void showNumberOrStringInput(FieldBlockView ss, boolean isNumber) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(isNumber ? R.string.logic_editor_title_enter_number_value : R.string.logic_editor_title_enter_string_value);
        View dialogView = ViewUtil.inflateLayout(this, R.layout.property_popup_input_text);
        EditText editText = dialogView.findViewById(R.id.ed_input);
        if (isNumber) {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
            editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editText.setMaxLines(1);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            editText.setImeOptions(EditorInfo.IME_ACTION_NONE);
        }
        editText.setText(ss.getArgValue().toString());
        dialog.setView(dialogView);
        dialog.setPositiveButton(R.string.common_word_save, (v, which) -> {
            String text = Helper.getText(editText);
            emptyStringSetter:
            {
                if (isNumber) {
                    try {
                        double d = Double.parseDouble(text);
                        if (!Double.isNaN(d) && !Double.isInfinite(d)) {
                            break emptyStringSetter;
                        }
                    } catch (NumberFormatException e) {
                        LogUtil.e("LogicEditor", "", e);
                    }
                } else if (!text.isEmpty()) {
                    if (text.charAt(0) == '@') {
                        text = " " + text;
                        break emptyStringSetter;
                    }
                }

                text = "";
            }

            setFieldValue(ss, text);
            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    public void connectBlock(BlockBean blockBean, boolean doLayout) {
        BlockView block = blockPane.findBlockByString(blockBean.id);
        if (block != null) {
            block.subStack1 = -1;
            block.subStack2 = -1;
            block.nextBlock = -1;

            for (int i = 0; i < blockBean.parameters.size() && i < block.childViews.size(); i++) {
                String parameter = blockBean.parameters.get(i);
                if (parameter != null) {
                    if (!parameter.isEmpty() && parameter.charAt(0) == '@') {
                        int blockId = Integer.parseInt(parameter.substring(1));
                        if (blockId > 0) {
                            BlockView parameterBlock = blockPane.findBlockById(blockId);
                            if (parameterBlock != null && block.childViews.get(i) instanceof BaseBlockView) {
                                block.replaceParameter((BaseBlockView) block.childViews.get(i), parameterBlock);
                            }
                        }
                    } else {
                        if (block.childViews.get(i) instanceof FieldBlockView ss) {
                            String javaName = projectFile.getJavaName();
                            String xmlName = projectFile.getXmlName();
                            if (eventName.equals("onBindCustomView")) {
                                var ProjectDataStore = ProjectDataManager.getProjectDataManager(scId);
                                var view = ProjectDataStore.getViewBean(xmlName, id);
                                if (view == null) {
                                    // Event is of a Drawer View
                                    view = ProjectDataStore.getViewBean("_drawer_" + xmlName, id);
                                }
                                String customView = view.customView;
                                if (customView != null) {
                                    xmlName = ProjectFileBean.getXmlName(customView);
                                }
                            }

                            if (!parameter.isEmpty()) {
                                if (ss.blockType.equals("m")) {
                                    ProjectDataStore ProjectDataStore = ProjectDataManager.getProjectDataManager(scId);

                                    switch (ss.componentType) {
                                        case "varInt":
                                            ProjectDataStore.hasVariable(javaName, ExtraMenuBean.VARIABLE_TYPE_NUMBER, parameter);
                                            break;

                                        case "varBool":
                                            ProjectDataStore.hasVariable(javaName, ExtraMenuBean.VARIABLE_TYPE_BOOLEAN, parameter);
                                            break;

                                        case "varStr":
                                            ProjectDataStore.hasVariable(javaName, ExtraMenuBean.VARIABLE_TYPE_STRING, parameter);
                                            break;

                                        case "listInt":
                                            ProjectDataStore.hasListVariable(javaName, ExtraMenuBean.LIST_TYPE_NUMBER, parameter);
                                            break;

                                        case "listStr":
                                            ProjectDataStore.hasListVariable(javaName, ExtraMenuBean.LIST_TYPE_STRING, parameter);
                                            break;

                                        case "listMap":
                                            ProjectDataStore.hasListVariable(javaName, ExtraMenuBean.LIST_TYPE_MAP, parameter);
                                            break;

                                        case "list":
                                            boolean hasListVar = ProjectDataStore.hasListVariable(javaName, ExtraMenuBean.LIST_TYPE_NUMBER, parameter);
                                            if (!hasListVar) {
                                                hasListVar = ProjectDataStore.hasListVariable(javaName, ExtraMenuBean.LIST_TYPE_STRING, parameter);
                                            }

                                            if (!hasListVar) {
                                                ProjectDataStore.hasListVariable(javaName, ExtraMenuBean.LIST_TYPE_MAP, parameter);
                                            }
                                            break;

                                        case "view":
                                            ProjectDataStore.hasView(xmlName, parameter);
                                            break;

                                        case "textview":
                                            ProjectDataStore.hasTextView(xmlName, parameter);
                                            break;

                                        case "checkbox":
                                            ProjectDataStore.hasCompoundButtonView(xmlName, parameter);
                                            break;

                                        case "imageview":
                                            ProjectDataStore.hasViewOfType(xmlName, ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW, parameter);
                                            break;

                                        case "seekbar":
                                            ProjectDataStore.hasViewOfType(xmlName, ViewBean.VIEW_TYPE_WIDGET_SEEKBAR, parameter);
                                            break;

                                        case "calendarview":
                                            ProjectDataStore.hasViewOfType(xmlName, ViewBean.VIEW_TYPE_WIDGET_CALENDARVIEW, parameter);
                                            break;

                                        case "adview":
                                            ProjectDataStore.hasViewOfType(xmlName, ViewBean.VIEW_TYPE_WIDGET_ADVIEW, parameter);
                                            break;

                                        case "listview":
                                            ProjectDataStore.hasViewOfType(xmlName, ViewBean.VIEW_TYPE_WIDGET_LISTVIEW, parameter);
                                            break;

                                        case "spinner":
                                            ProjectDataStore.hasViewOfType(xmlName, ViewBean.VIEW_TYPE_WIDGET_SPINNER, parameter);
                                            break;

                                        case "webview":
                                            ProjectDataStore.hasViewOfType(xmlName, ViewBean.VIEW_TYPE_WIDGET_WEBVIEW, parameter);
                                            break;

                                        case "switch":
                                            ProjectDataStore.hasViewOfType(xmlName, ViewBean.VIEW_TYPE_WIDGET_SWITCH, parameter);
                                            break;

                                        case "progressbar":
                                            ProjectDataStore.hasViewOfType(xmlName, ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR, parameter);
                                            break;

                                        case "mapview":
                                            ProjectDataStore.hasViewOfType(xmlName, ViewBean.VIEW_TYPE_WIDGET_MAPVIEW, parameter);
                                            break;

                                        case "intent":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_INTENT, parameter);
                                            break;

                                        case "file":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_SHAREDPREF, parameter);
                                            break;

                                        case "calendar":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_CALENDAR, parameter);
                                            break;

                                        case "timer":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_TIMERTASK, parameter);
                                            break;

                                        case "vibrator":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_VIBRATOR, parameter);
                                            break;

                                        case "dialog":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_DIALOG, parameter);
                                            break;

                                        case "mediaplayer":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_MEDIAPLAYER, parameter);
                                            break;

                                        case "soundpool":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_SOUNDPOOL, parameter);
                                            break;

                                        case "objectanimator":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_OBJECTANIMATOR, parameter);
                                            break;

                                        case "firebase":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_FIREBASE, parameter);
                                            break;

                                        case "firebaseauth":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH, parameter);
                                            break;

                                        case "firebasestorage":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_FIREBASE_STORAGE, parameter);
                                            break;

                                        case "gyroscope":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_GYROSCOPE, parameter);
                                            break;

                                        case "interstitialad":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_INTERSTITIAL_AD, parameter);
                                            break;

                                        case "requestnetwork":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_REQUEST_NETWORK, parameter);
                                            break;

                                        case "texttospeech":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_TEXT_TO_SPEECH, parameter);
                                            break;

                                        case "speechtotext":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_SPEECH_TO_TEXT, parameter);
                                            break;

                                        case "bluetoothconnect":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_BLUETOOTH_CONNECT, parameter);
                                            break;

                                        case "locationmanager":
                                            ProjectDataStore.hasComponent(javaName, ComponentBean.COMPONENT_TYPE_LOCATION_MANAGER, parameter);
                                            break;

                                        case "resource_bg":
                                        case "resource":
                                            for (String imageName : ProjectDataManager.getResourceManager(scId).getImageNames()) {
                                                // Like this in vanilla Sketchware. Don't ask me why.
                                                //noinspection StatementWithEmptyBody
                                                if (parameter.equals(imageName)) {
                                                }
                                            }
                                            break;

                                        case "activity":
                                            for (String activityName : ProjectDataManager.getFileManager(scId).getJavaNames()) {
                                                // Like this in vanilla Sketchware. Don't ask me why.
                                                //noinspection StatementWithEmptyBody
                                                if (parameter.equals(activityName.substring(activityName.indexOf(".java")))) {
                                                }
                                            }
                                            break;

                                        case "sound":
                                            for (String soundName : ProjectDataManager.getResourceManager(scId).getSoundNames()) {
                                                // Like this in vanilla Sketchware. Don't ask me why.
                                                //noinspection StatementWithEmptyBody
                                                if (parameter.equals(soundName)) {
                                                }
                                            }
                                            break;

                                        case "videoad":
                                            ProjectDataStore.hasComponent(xmlName, ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD, parameter);
                                            break;

                                        case "progressdialog":
                                            ProjectDataStore.hasComponent(xmlName, ComponentBean.COMPONENT_TYPE_PROGRESS_DIALOG, parameter);
                                            break;

                                        case "datepickerdialog":
                                            ProjectDataStore.hasComponent(xmlName, ComponentBean.COMPONENT_TYPE_DATE_PICKER_DIALOG, parameter);
                                            break;

                                        case "timepickerdialog":
                                            ProjectDataStore.hasComponent(xmlName, ComponentBean.COMPONENT_TYPE_TIME_PICKER_DIALOG, parameter);
                                            break;

                                        case "notification":
                                            ProjectDataStore.hasComponent(xmlName, ComponentBean.COMPONENT_TYPE_NOTIFICATION, parameter);
                                            break;

                                        case "radiobutton":
                                            ProjectDataStore.hasViewOfType(xmlName, 19, parameter);
                                            break;

                                        case "ratingbar":
                                            ProjectDataStore.hasViewOfType(xmlName, 20, parameter);
                                            break;

                                        case "videoview":
                                            ProjectDataStore.hasViewOfType(xmlName, 21, parameter);
                                            break;

                                        case "searchview":
                                            ProjectDataStore.hasViewOfType(xmlName, 22, parameter);
                                            break;

                                        case "actv":
                                            ProjectDataStore.hasViewOfType(xmlName, 23, parameter);
                                            break;

                                        case "mactv":
                                            ProjectDataStore.hasViewOfType(xmlName, 24, parameter);
                                            break;

                                        case "gridview":
                                            ProjectDataStore.hasViewOfType(xmlName, 25, parameter);
                                            break;

                                        case "tablayout":
                                            ProjectDataStore.hasViewOfType(xmlName, 30, parameter);
                                            break;

                                        case "viewpager":
                                            ProjectDataStore.hasViewOfType(xmlName, 31, parameter);
                                            break;

                                        case "bottomnavigation":
                                            ProjectDataStore.hasViewOfType(xmlName, 32, parameter);
                                            break;

                                        case "badgeview":
                                            ProjectDataStore.hasViewOfType(xmlName, 33, parameter);
                                            break;

                                        case "patternview":
                                            ProjectDataStore.hasViewOfType(xmlName, 34, parameter);
                                            break;

                                        case "sidebar":
                                            ProjectDataStore.hasViewOfType(xmlName, 35, parameter);
                                            break;

                                        default:
                                            extraPaletteBlock.hasExtraComponent(ss.componentType, parameter);
                                    }
                                }
                            }

                            ss.setArgValue(parameter);
                            block.recalculateToRoot();
                        }
                    }
                }
            }

            int subStack1RootBlockId = blockBean.subStack1;
            if (subStack1RootBlockId >= 0) {
                BlockView subStack1RootBlock = blockPane.findBlockById(subStack1RootBlockId);
                if (subStack1RootBlock != null) {
                    block.setSubstack1Block(subStack1RootBlock);
                }
            }

            int subStack2RootBlockId = blockBean.subStack2;
            if (subStack2RootBlockId >= 0) {
                BlockView subStack2RootBlock = blockPane.findBlockById(subStack2RootBlockId);
                if (subStack2RootBlock != null) {
                    block.setSubstack2Block(subStack2RootBlock);
                }
            }

            int nextBlockId = blockBean.nextBlock;
            if (nextBlockId >= 0) {
                BlockView nextBlock = blockPane.findBlockById(nextBlockId);
                if (nextBlock != null) {
                    block.setNextBlock(nextBlock);
                    block.recalculateToRoot();
                }
            }

            block.recalculateToRoot();
            if (doLayout) {
                block.getRootBlock().layoutChain();
                blockPane.updatePaneSize();
            }
        }
    }

    public void addPaletteCategory(String categoryName, int i) {
        paletteBlock.addCategoryHeader(categoryName, i);
    }

    public void saveBlockToCollection(String collectionName, BlockView rs) {
        ArrayList<String> paramsList;
        ArrayList<BlockView> allChildren = rs.getAllChildren();
        ArrayList<BlockBean> collectionBlocks = new ArrayList<>();
        for (BlockView child : allChildren) {
            BlockBean blockBean = new BlockBean();
            BlockBean bean = child.getBean();
            blockBean.copy(bean);
            blockBean.id = String.format("99%06d", Integer.valueOf(bean.id));
            int subStack1Val = bean.subStack1;
            if (subStack1Val > 0) {
                blockBean.subStack1 = subStack1Val + 99000000;
            }
            int subStack2Val = bean.subStack2;
            if (subStack2Val > 0) {
                blockBean.subStack2 = subStack2Val + 99000000;
            }
            int nextBlockVal = bean.nextBlock;
            if (nextBlockVal > 0) {
                blockBean.nextBlock = nextBlockVal + 99000000;
            }
            blockBean.parameters.clear();
            for (String next : bean.parameters) {
                if (next.length() <= 1 || next.charAt(0) != '@') {
                    paramsList = blockBean.parameters;
                } else {
                    String format = String.format("99%06d", Integer.valueOf(next.substring(1)));
                    paramsList = blockBean.parameters;
                    next = '@' + format;
                }
                paramsList.add(next);
            }
            collectionBlocks.add(blockBean);
        }
        try {
            BlockCollectionManager.getInstance().addBlock(collectionName, collectionBlocks, true);
            editorDrawer.addBlockCollection(collectionName, collectionBlocks).setOnTouchListener(this);
        } catch (Exception e) {
            crashlytics.recordException(e);
        }
    }

    public void setCopyActive(boolean active) {
        logicTopMenu.setCopyActive(active);
    }

    public final boolean hitTestCopy(float x, float y) {
        return logicTopMenu.isInsideCopyArea(x, y);
    }

    public final boolean isBlockValid(BlockBean blockBean) {
        if (blockBean.opCode.equals("getArg")) {
            return true;
        }
        if (blockBean.opCode.equals("definedFunc")) {
            Iterator<Pair<String, String>> it = ProjectDataManager.getProjectDataManager(scId).getMoreBlocks(projectFile.getJavaName()).iterator();
            boolean found = false;
            while (it.hasNext()) {
                if (blockBean.spec.equals(ReturnMoreblockManager.getMbName(it.next().second))) {
                    found = true;
                }
            }
            return found;
        }
        return true;
    }

    public BlockView createBlockView(BlockBean blockBean) {
        return new BlockView(this, Integer.parseInt(blockBean.id), blockBean.spec, blockBean.type, blockBean.typeName, blockBean.opCode);
    }

    private RadioButton getFontRadioButton(String fontName) {
        RadioButton radioButton = new RadioButton(this);
        radioButton.setText("");
        radioButton.setTag(fontName);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (ViewUtil.dpToPx(this, 1.0f) * 60.0f));
        radioButton.setGravity(Gravity.CENTER | Gravity.LEFT);
        radioButton.setLayoutParams(layoutParams);
        return radioButton;
    }

    public void addVariable(int i, String variableName) {
        ProjectDataManager.getProjectDataManager(scId).addVariable(projectFile.getJavaName(), i, variableName);
        onBlockSizeChanged(0, 0xffee7d16);
    }

    public void deleteBlock(BlockView rs) {
        blockPane.removeBlockTree(rs);
    }

    public void showColorPicker(FieldBlockView ss) {
        ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, (ss.getArgValue() == null || ss.getArgValue().toString().isEmpty()) ? "Color.TRANSPARENT" : ss.getArgValue().toString().replace("0xFF", "#"), true, false, scId);
        colorPickerDialog.setColorPickerCallback(new ColorPickerDialog.OnColorPickedListener() {
            @Override
            public void onColorPicked(int color) {
                if (color == 0) {
                    LogicEditorActivity.this.setFieldValue(ss, "Color.TRANSPARENT");
                } else {
                    LogicEditorActivity.this.setFieldValue(ss, String.format("0x%08X", color & (Color.WHITE)));
                }
            }

            @Override
            public void onResourceColorPicked(String resourceName, int color) {
                LogicEditorActivity.this.setFieldValue(ss, "R.color." + resourceName);
            }
        });
        colorPickerDialog.materialColorAttr((attr, attrColor) -> setFieldValue(ss, "R.attr." + attr));
        colorPickerDialog.showAtLocation(ss, Gravity.CENTER, 0, 0);
    }

    public void addPaletteLabel(String label, String tag) {
        TextView textView = paletteBlock.addActionLabel(label);
        textView.setTag(tag);
        textView.setSoundEffectsEnabled(true);
        textView.setOnClickListener(this);
    }

    public void addPaletteLabelWithListener(String label, String tag, View.OnClickListener onClickListener) {
        TextView textView = paletteBlock.addActionLabel(label);
        textView.setTag(tag);
        textView.setSoundEffectsEnabled(true);
        textView.setOnClickListener(onClickListener);
    }

    public void activeIconDelete(boolean showDeleteIcon) {
        logicTopMenu.setDeleteActive(showDeleteIcon);
    }

    public final boolean hitTestIconDelete(float x, float y) {
        return logicTopMenu.isInsideDeleteArea(x, y);
    }

    public void showSaveToFavorites(BlockView rs) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.logic_block_favorites_save_title);
        View dialogView = ViewUtil.inflateLayout(this, R.layout.property_popup_save_to_favorite);
        ((TextView) dialogView.findViewById(R.id.tv_favorites_guide)).setText(R.string.logic_block_favorites_save_guide);
        EditText editText = dialogView.findViewById(R.id.ed_input);
        editText.setPrivateImeOptions("defaultInputmode=english;");
        editText.setLines(1);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        UniqueNameValidator blockNameValidator = new UniqueNameValidator(this, dialogView.findViewById(R.id.ti_input), BlockCollectionManager.getInstance().getBlockNames());
        dialog.setView(dialogView);
        dialog.setPositiveButton(R.string.common_word_save, (v, which) -> {
            if (blockNameValidator.isValid()) {
                saveBlockToCollection(Helper.getText(editText), rs);
                v.dismiss();
            }
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    public void showStringInput(FieldBlockView ss) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.logic_editor_title_enter_string_value);
        View dialogView = ViewUtil.inflateLayout(this, R.layout.property_popup_input_text);
        ((TextInputLayout) dialogView.findViewById(R.id.ti_input)).setHint(Helper.getResString(R.string.property_hint_enter_value));
        EditText editText = dialogView.findViewById(R.id.ed_input);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setText(ss.getArgValue().toString());
        dialog.setView(dialogView);
        dialog.setPositiveButton(R.string.common_word_save, (v, which) -> {
            setFieldValue(ss, Helper.getText(editText));
            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    public void addMoreBlock(String name, String spec) {
        ProjectDataManager.getProjectDataManager(scId).addMoreBlock(projectFile.getJavaName(), name, spec);
        onBlockSizeChanged(8, 0xff8a55d7);
    }

    public void setDetailActive(boolean active) {
        logicTopMenu.setDetailActive(active);
    }

    public final boolean hitTestDetail(float x, float y) {
        return logicTopMenu.isInsideDetailArea(x, y);
    }

    private LinearLayout getFontPreview(String fontName) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (ViewUtil.dpToPx(this, 1.0f) * 60.0f)));
        linearLayout.setGravity(Gravity.CENTER | Gravity.LEFT);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView name = new TextView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.weight = 1.0f;
        name.setLayoutParams(layoutParams);
        name.setText(fontName);
        linearLayout.addView(name);
        TextView preview = new TextView(this);
        preview.setLayoutParams(layoutParams);
        preview.setText(Helper.getResString(R.string.font_preview));

        Typeface typeface;
        if (fontName.equalsIgnoreCase("default_font")) {
            typeface = Typeface.DEFAULT;
        } else {
            try {
                typeface = Typeface.createFromFile(ProjectDataManager.getResourceManager(scId).getFontPath(fontName));
            } catch (RuntimeException e) {
                crashlytics.log("Loading font preview");
                crashlytics.recordException(e);
                typeface = Typeface.DEFAULT;
                preview.setText(Helper.getResString(R.string.font_load_failed));
            }
        }

        preview.setTypeface(typeface);
        linearLayout.addView(preview);
        return linearLayout;
    }

    public RadioButton createViewRadioButton(String type, String id) {
        if (isViewBindingEnabled) {
            id = ViewBindingBuilder.generateParameterFromId(id);
        }
        RadioButton radioButton = new RadioButton(this);
        radioButton.setText(type + " : " + id);
        radioButton.setTag(id);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (ViewUtil.dpToPx(this, 1.0f) * 40.0f));
        radioButton.setGravity(Gravity.CENTER | Gravity.LEFT);
        radioButton.setLayoutParams(layoutParams);
        return radioButton;
    }

    public void showFontPicker(FieldBlockView ss) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.logic_editor_title_select_font);

        View customView = ViewUtil.inflateLayout(this, R.layout.property_popup_selector_color);
        RadioGroup radioGroup = customView.findViewById(R.id.rg);
        LinearLayout linearLayout = customView.findViewById(R.id.content);
        ArrayList<String> fontNames = ProjectDataManager.getResourceManager(scId).getFontNames();
        fontNames.add(0, "default_font");
        for (String fontName : fontNames) {
            RadioButton font = getFontRadioButton(fontName);
            radioGroup.addView(font);
            if (fontName.equals(ss.getArgValue())) {
                font.setChecked(true);
            }
            LinearLayout fontPreview = getFontPreview(fontName);
            fontPreview.setOnClickListener(v -> font.setChecked(true));
            linearLayout.addView(fontPreview);
        }

        dialog.setView(customView);
        dialog.setPositiveButton(R.string.common_word_select, (v, which) -> {
            for (int i = 0; i < radioGroup.getChildCount(); i++) {
                RadioButton radioButton = (RadioButton) radioGroup.getChildAt(i);
                if (radioButton.isChecked()) {
                    setFieldValue(ss, radioButton.getTag());
                    break;
                }
            }
            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    public void setFavoriteActive(boolean active) {
        logicTopMenu.setFavoriteActive(active);
    }

    public final boolean hitTestFavorite(float x, float y) {
        return logicTopMenu.isInsideFavoriteArea(x, y);
    }

    public final RadioButton createRadioButton(String text) {
        RadioButton radioButton = new RadioButton(this);
        radioButton.setText(text);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = (int) ViewUtil.dpToPx(getContext(), 4.0f);
        layoutParams.bottomMargin = (int) ViewUtil.dpToPx(getContext(), 4.0f);
        radioButton.setGravity(Gravity.CENTER | Gravity.LEFT);
        radioButton.setLayoutParams(layoutParams);
        return radioButton;
    }

    public final CheckBox createCheckBox(String text) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(text);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = (int) ViewUtil.dpToPx(getContext(), 4.0f);
        layoutParams.bottomMargin = (int) ViewUtil.dpToPx(getContext(), 4.0f);
        checkBox.setGravity(Gravity.CENTER | Gravity.LEFT);
        checkBox.setLayoutParams(layoutParams);
        return checkBox;
    }

    public void showIntentDataInput(FieldBlockView ss) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.logic_editor_title_enter_data_value);
        View dialogView = ViewUtil.inflateLayout(this, R.layout.property_popup_input_intent_data);
        ((TextView) dialogView.findViewById(R.id.tv_desc_intent_usage)).setText(Helper.getResString(R.string.property_description_component_intent_usage));
        EditText editText = dialogView.findViewById(R.id.ed_input);
        ((TextInputLayout) dialogView.findViewById(R.id.ti_input)).setHint(Helper.getResString(R.string.property_hint_enter_value));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setText(ss.getArgValue().toString());
        dialog.setView(dialogView);
        dialog.setPositiveButton(R.string.common_word_save, (v, which) -> {
            setFieldValue(ss, Helper.getText(editText));
            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    public void togglePaletteVisibility(boolean visible) {
        ObjectAnimator objectAnimator;
        if (!paletteAnimatorsInitialized) {
            setupPaletteAnimators(getResources().getConfiguration().orientation);
        }
        if (isPaletteVisible == visible) {
            return;
        }
        isPaletteVisible = visible;
        cancelPaletteAnimations();
        if (visible) {
            toggleDrawerVisibility(false);
            objectAnimator = paletteShowAnimator;
        } else {
            objectAnimator = paletteHideAnimator;
        }
        objectAnimator.start();
        layoutEditorForOrientation(getResources().getConfiguration().orientation);
    }

    public void layoutEditorForOrientation(int i) {
        LinearLayout.LayoutParams layoutParams;
        int editorHeight;
        int editorWidth = ViewGroup.LayoutParams.MATCH_PARENT;
        if (isPaletteVisible) {
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            if (width <= height) {
                width = height;
            }
            if (2 == i) {
                editorWidth = width - ((int) ViewUtil.dpToPx(this, 320.0f));
                editorHeight = ViewGroup.LayoutParams.MATCH_PARENT;
            } else {
                editorHeight = viewLogicEditor.getHeight() - paletteArea.getHeight();
            }
            layoutParams = new LinearLayout.LayoutParams(editorWidth, editorHeight);
        } else {
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        viewLogicEditor.setLayoutParams(layoutParams);
        viewLogicEditor.requestLayout();
    }

    public void showViewSelector(FieldBlockView ss) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        View customView = ViewUtil.inflateLayout(this, R.layout.property_popup_selector_single);
        ViewGroup viewGroup = customView.findViewById(R.id.rg_content);
        String xmlName = projectFile.getXmlName();

        if (eventName.equals("onBindCustomView")) {
            var ProjectDataStore = ProjectDataManager.getProjectDataManager(scId);
            var view = ProjectDataStore.getViewBean(xmlName, id);
            if (view == null) {
                view = ProjectDataStore.getViewBean("_drawer_" + xmlName, id);
            }
            if (view != null && view.customView != null) {
                xmlName = ProjectFileBean.getXmlName(view.customView);
            }
        }

        dialog.setTitle(R.string.logic_editor_title_select_view);
        ArrayList<ViewBean> views = ProjectDataManager.getProjectDataManager(scId).getViews(xmlName);
        for (ViewBean viewBean : views) {
            String convert = viewBean.convert;
            String typeName = convert.isEmpty() ? ViewBean.getViewTypeName(viewBean.type) : IdGenerator.getLastPath(convert);
            if (!convert.equals("include")) {
                Set<String> toNotAdd = new LayoutGenerator(new BuildConfig(), projectFile).readAttributesToReplace(viewBean);
                if (!toNotAdd.contains("android:id")) {
                    String classInfo = ss.getClassInfo().getClassName();
                    if ((classInfo.equals("CheckBox") && viewBean.getClassInfo().matchesType("CompoundButton")) || viewBean.getClassInfo().matchesType(classInfo)) {
                        viewGroup.addView(createViewRadioButton(typeName, viewBean.id));
                    }
                }
                ExtraMenuBean.setupSearchView(customView, viewGroup);
            }
        }

        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            RadioButton radioButton = (RadioButton) viewGroup.getChildAt(i);
            String argValue = ss.getArgValue().toString();
            if (argValue.equals(radioButton.getTag().toString())) {
                radioButton.setChecked(true);
                break;
            }
        }

        dialog.setView(customView);
        dialog.setNeutralButton(R.string.common_word_code_editor, (v, which) -> {
            AsdDialog editor = new AsdDialog(this);
            editor.setContent(ss.getArgValue().toString());
            editor.show();
            editor.setOnSaveClickListener(this, false, ss, editor);
            editor.setOnCancelClickListener(editor);
            v.dismiss();
        });
        dialog.setPositiveButton(R.string.common_word_select, (v, which) -> {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                RadioButton radioButton = (RadioButton) viewGroup.getChildAt(i);
                if (radioButton.isChecked()) {
                    setFieldValue(ss, radioButton.getTag());
                    break;
                }
            }
            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    public void toggleLayoutVisibility(boolean visible) {
        logicTopMenu.toggleLayoutVisibility(visible);
    }

    @Override
    public void finish() {
        BlockHistoryManager.getInstance(scId).removeHistory(buildHistoryKey());
        super.finish();
    }

    private Context getContext() {
        return this;
    }

    public void handleOrientationChange(int i) {
        RelativeLayout.LayoutParams layoutParams;
        int orientation;
        if (2 == i) {
            paletteArea.setLayoutParams(new LinearLayout.LayoutParams((int) ViewUtil.dpToPx(this, 320.0f), ViewGroup.LayoutParams.MATCH_PARENT));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER | Gravity.BOTTOM;
            int dimension = (int) getResources().getDimension(R.dimen.action_button_margin);
            params.setMargins(dimension, dimension, dimension, dimension);
            openBlocksMenuButton.setLayoutParams(params);
            layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            layoutParams.topMargin = DeviceUtil.getToolbarHeight(getContext());
            orientation = LinearLayout.HORIZONTAL;
        } else {
            paletteArea.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) ViewUtil.dpToPx(this, 240.0f)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER | Gravity.RIGHT;
            int dimension2 = (int) getResources().getDimension(R.dimen.action_button_margin);
            params.setMargins(dimension2, dimension2, dimension2, dimension2);
            openBlocksMenuButton.setLayoutParams(params);
            layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            orientation = LinearLayout.VERTICAL;
        }
        paletteLayout.setOrientation(orientation);
        paletteLayout.setLayoutParams(layoutParams);
        setupPaletteAnimators(i);
        layoutEditorForOrientation(i);
    }

    public void toggleDrawerVisibility(boolean visible) {
        if (!drawerAnimatorsInitialized) {
            initDrawerAnimators();
        }
        if (isDrawerVisible != visible) {
            isDrawerVisible = visible;
            cancelDrawerAnimations();
            (visible ? drawerShowAnimator : drawerHideAnimator).start();
        }
    }

    public void setupPaletteAnimators(int i) {
        boolean wasPaletteVisible = isPaletteVisible;
        if (i == 2) {
            if (!wasPaletteVisible) {
                paletteLayout.setTranslationX(ViewUtil.dpToPx(this, 320.0F));
            } else {
                paletteLayout.setTranslationX(0.0F);
            }
            paletteLayout.setTranslationY(0.0F);
        } else {
            if (!wasPaletteVisible) {
                paletteLayout.setTranslationX(0.0F);
                paletteLayout.setTranslationY(ViewUtil.dpToPx(this, 240.0F));
            } else {
                paletteLayout.setTranslationX(0.0F);
                paletteLayout.setTranslationY(0.0F);
            }
        }

        if (i == 2) {
            paletteShowAnimator = ObjectAnimator.ofFloat(paletteLayout, View.TRANSLATION_X, 0.0F);
            paletteHideAnimator = ObjectAnimator.ofFloat(paletteLayout, View.TRANSLATION_X, ViewUtil.dpToPx(this, 320.0F));
        } else {
            paletteShowAnimator = ObjectAnimator.ofFloat(paletteLayout, View.TRANSLATION_Y, 0.0F);
            paletteHideAnimator = ObjectAnimator.ofFloat(paletteLayout, View.TRANSLATION_Y, ViewUtil.dpToPx(this, 240.0F));
        }

        paletteShowAnimator.setDuration(500L);
        paletteShowAnimator.setInterpolator(new DecelerateInterpolator());
        paletteHideAnimator.setDuration(300L);
        paletteHideAnimator.setInterpolator(new DecelerateInterpolator());
        paletteAnimatorsInitialized = true;
    }

    public void showSoundPicker(FieldBlockView ss) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.logic_editor_title_select_sound);

        View customView = ViewUtil.inflateLayout(this, R.layout.property_popup_selector_single);
        RadioGroup radioGroup = customView.findViewById(R.id.rg_content);
        SoundPool soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .build();
        soundPool.setOnLoadCompleteListener((soundPool1, sampleId, status) -> {
            if (soundPool1 != null) {
                soundPool1.play(sampleId, 1, 1, 1, 0, 1);
            }
        });

        for (String soundName : ProjectDataManager.getResourceManager(scId).getSoundNames()) {
            RadioButton sound = createRadioButton(soundName);
            radioGroup.addView(sound);
            if (soundName.equals(ss.getArgValue())) {
                sound.setChecked(true);
            }
            sound.setOnClickListener(v -> soundPool.load(ProjectDataManager.getResourceManager(scId).getSoundPath(Helper.getText(sound)), 1));
        }
        dialog.setView(customView);
        dialog.setPositiveButton(R.string.common_word_select, (v, which) -> {
            RadioButton checkedRadioButton = radioGroup.findViewById(radioGroup.getCheckedRadioButtonId());
            setFieldValue(ss, Helper.getText(checkedRadioButton));
            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    public void toggleTopMenuVisibility(boolean visible) {
        logicTopMenu.setDeleteActive(false);
        logicTopMenu.setCopyActive(false);
        logicTopMenu.setFavoriteActive(false);
        logicTopMenu.setDetailActive(false);
        if (!topMenuAnimatorsInitialized) {
            initTopMenuAnimators();
        }
        if (isTopMenuVisible == visible) {
            return;
        }
        isTopMenuVisible = visible;
        cancelTopMenuAnimations();
        (visible ? topMenuShowAnimator : topMenuHideAnimator).start();
    }

    public void showTypefaceSelector(FieldBlockView ss) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.logic_editor_title_select_typeface);
        View dialogView = ViewUtil.inflateLayout(this, R.layout.property_popup_selector_single);
        RadioGroup radioGroup = dialogView.findViewById(R.id.rg_content);
        for (Pair<Integer, String> pair : SketchwareConstants.getPropertyPairs("property_text_style")) {
            RadioButton radioButton = createRadioButton(pair.second);
            radioGroup.addView(radioButton);
            if (pair.second.equals(ss.getArgValue())) {
                radioButton.setChecked(true);
            }
        }
        dialog.setView(dialogView);
        dialog.setPositiveButton(R.string.common_word_save, (v, which) -> {
            int childCount = radioGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                RadioButton radioButton = (RadioButton) radioGroup.getChildAt(i);
                if (radioButton.isChecked()) {
                    setFieldValue(ss, Helper.getText(radioButton));
                    break;
                }
            }

            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    public void cancelDrawerAnimations() {
        if (drawerShowAnimator.isRunning()) {
            drawerShowAnimator.cancel();
        }
        if (drawerHideAnimator.isRunning()) {
            drawerHideAnimator.cancel();
        }
    }

    public void removeListVariable(String variableName) {
        ProjectDataManager.getProjectDataManager(scId).removeListVariable(projectFile.getJavaName(), variableName);
        onBlockSizeChanged(1, 0xffcc5b22);
    }

    public void cancelTopMenuAnimations() {
        if (topMenuShowAnimator.isRunning()) {
            topMenuShowAnimator.cancel();
        }
        if (topMenuHideAnimator.isRunning()) {
            topMenuHideAnimator.cancel();
        }
    }

    public void removeVariable(String variableName) {
        ProjectDataManager.getProjectDataManager(scId).removeVariable(projectFile.getJavaName(), variableName);
        onBlockSizeChanged(0, 0xffee7d16);
    }

    public void cancelPaletteAnimations() {
        if (paletteShowAnimator.isRunning()) {
            paletteShowAnimator.cancel();
        }
        if (paletteHideAnimator.isRunning()) {
            paletteHideAnimator.cancel();
        }
    }

    public void showDeleteFavoriteDialog(String blockName) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.logic_block_favorites_delete_title);
        dialog.setMessage(R.string.logic_block_favorites_delete_message);
        dialog.setPositiveButton(R.string.common_word_delete, (v, which) -> {
            BlockCollectionManager.getInstance().removeBlock(blockName, true);
            editorDrawer.removeBlockCollection(blockName);
            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    public void showBlockCollection(String blockName) {
        Intent intent = new Intent(getContext(), ShowBlockCollectionActivity.class);
        intent.putExtra("block_name", blockName);
        startActivity(intent);
    }

    public boolean isDrawerEnabled() {
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 224) {
                onBlockSizeChanged(7, 0xff2ca5e2);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (!UIHelper.isClickThrottled()) {
            Object tag = v.getTag();
            if (tag != null) {
                if (tag.equals("variableAdd")) {
                    showAddNewVariableDialog();
                } else if (tag.equals("variableRemove")) {
                    showRemoveVariableDialog();
                } else if (tag.equals("openResourcesEditor")) {
                    openResourcesEditor();
                } else if (tag.equals("listAdd")) {
                    showAddListDialog();
                } else if (tag.equals("listRemove")) {
                    showRemoveListDialog();
                } else if (tag.equals("blockAdd")) {
                    Intent intent = new Intent(this, MakeBlockActivity.class);
                    intent.putExtra("sc_id", scId);
                    intent.putExtra("project_file", projectFile);
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    makeBlockLauncher.launch(intent);
                } else if (tag.equals("componentAdd")) {
                    AddComponentBottomSheet addComponentBottomSheet = AddComponentBottomSheet.newInstance(scId, projectFile, () -> onBlockSizeChanged(7, 0xff2ca5e2));
                    addComponentBottomSheet.show(getSupportFragmentManager(), null);
                } else if (tag.equals("blockImport")) {
                    showMoreBlockImporter();
                }
            }
            int id = v.getId();
            if (id == R.id.btn_delete) {
                setResult(Activity.RESULT_OK, new Intent());
                finish();
            } else if (id == R.id.btn_cancel) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration configuration) {
        super.onConfigurationChanged(configuration);
        handleOrientationChange(configuration.orientation);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isDrawerVisible) {
                    toggleDrawerVisibility(false);
                    return;
                }
                if (isPaletteVisible) {
                    togglePaletteVisibility(false);
                    return;
                }
                showLoadingDialog();
                if (!isTopMenuEnabled()) {
                    return;
                }
                saveProject();
            }
        });
        openResourcesEditor = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        paletteSelector.performClickPalette(-1);
                    }
                });
        makeBlockLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        addMoreBlock(result.getData().getStringExtra("block_name"), result.getData().getStringExtra("block_spec"));
                    }
                });
        setContentView(R.layout.logic_editor);
        if (!super.isStoragePermissionGranted()) {
            finish();
        }
        Parcelable parcelable;
        if (savedInstanceState == null) {
            scId = getIntent().getStringExtra("sc_id");
            id = getIntent().getStringExtra("id");
            eventName = getIntent().getStringExtra("event");
            parcelable = getIntent().getParcelableExtra("project_file");
        } else {
            scId = savedInstanceState.getString("sc_id");
            id = savedInstanceState.getString("id");
            eventName = savedInstanceState.getString("event");
            parcelable = savedInstanceState.getParcelable("project_file");
        }
        if (parcelable == null) {
            finish();
            return;
        }
        isViewBindingEnabled = new ProjectSettings(scId).getValue(ProjectSettings.SETTING_ENABLE_VIEWBINDING, "false").equals("true");
        projectFile = (ProjectFileBean) parcelable;
        dummyOffsetY = (int) ViewUtil.dpToPx(getContext(), (float) dummyOffsetY);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (!UIHelper.isClickThrottled()) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
        isVibrationEnabled = new SharedPrefsHelper(getContext(), "P12").getBoolean("P12I0", true);
        minDist = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        String eventText = getIntent().getStringExtra("event_text");
        toolbar.setTitle(id.equals("_fab") ? "fab" : ReturnMoreblockManager.getMbName(id));
        toolbar.setSubtitle(eventText);
        paletteSelector = findViewById(R.id.palette_selector);
        paletteSelector.setOnBlockCategorySelectListener(this);
        paletteBlock = findViewById(R.id.palette_block);
        dummy = findViewById(R.id.dummy);
        viewLogicEditor = findViewById(R.id.editor);
        blockPane = viewLogicEditor.getBlockPane();
        paletteLayout = findViewById(R.id.layout_palette);
        paletteArea = findViewById(R.id.area_palette);
        openBlocksMenuButton = findViewById(R.id.fab_toggle_palette);
        openBlocksMenuButton.setOnClickListener(v -> togglePaletteVisibility(!isPaletteVisible));
        logicTopMenu = findViewById(R.id.top_menu);
        editorDrawer = findViewById(R.id.right_drawer);
        findViewById(R.id.search_header).setOnClickListener(v -> paletteSelector.showSearchDialog());
        extraPaletteBlock = new ExtraPaletteBlock(this, isViewBindingEnabled);

        svgUtils = new SvgUtils(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.logic_menu, menu);
        menu.findItem(R.id.menu_logic_redo).setEnabled(projectFile != null && BlockHistoryManager.getInstance(scId).canRedo(buildHistoryKey()));
        menu.findItem(R.id.menu_logic_undo).setEnabled(projectFile != null && BlockHistoryManager.getInstance(scId).canUndo(buildHistoryKey()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();

        if (itemId == R.id.menu_block_helper) {
            togglePaletteVisibility(false);
            toggleDrawerVisibility(!isDrawerVisible);
        } else if (itemId == R.id.menu_logic_redo) {
            redo();
        } else if (itemId == R.id.menu_logic_undo) {
            undo();
        } else if (itemId == R.id.menu_logic_showsource) {
            showSourceCode();
        }

        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);

        String title;
        if (eventName.equals("moreBlock")) {
            title = Helper.getResString(R.string.root_spec_common_define) + " " + ReturnMoreblockManager.getLogicEditorTitle(ProjectDataManager.getProjectDataManager(scId).getMoreBlockSpec(projectFile.getJavaName(), id));
        } else if (id.equals("_fab")) {
            title = StringResource.getInstance().getRootSpecTranslation(getContext(), "fab", eventName);
        } else {
            title = StringResource.getInstance().getRootSpecTranslation(getContext(), id, eventName);
        }
        String headerTitle = title;

        long pc0 = System.currentTimeMillis();
        blockPane.createHeaderBlock(headerTitle, eventName);
        long pc1 = System.currentTimeMillis();
        android.util.Log.d("BlockLoad", "PC rootBlock: " + (pc1 - pc0) + "ms");

        ArrayList<String> spec = FormatUtil.parseBlockSpec(headerTitle);
        int blockId = 0;
        for (int i = 0; i < spec.size(); i++) {
            String specBit = spec.get(i);
            if (specBit.charAt(0) == '%') {
                BlockView block = BlockUtil.getVariableBlock(getContext(), blockId + 1, specBit, "getArg");
                if (block != null) {
                    block.setBlockType(1);
                    blockPane.addView(block);
                    blockPane.getRoot().replaceParameter((BaseBlockView) blockPane.getRoot().childViews.get(blockId), block);
                    block.setOnTouchListener(this);
                    blockId++;
                }
            }
        }
        long pc2 = System.currentTimeMillis();
        android.util.Log.d("BlockLoad", "PC varBlocks: " + (pc2 - pc1) + "ms");

        blockPane.getRoot().layoutChain();
        long pc3 = System.currentTimeMillis();
        android.util.Log.d("BlockLoad", "PC rootK: " + (pc3 - pc2) + "ms");

        handleOrientationChange(getResources().getConfiguration().orientation);
        long pc4 = System.currentTimeMillis();
        android.util.Log.d("BlockLoad", "PC orient: " + (pc4 - pc3) + "ms");

        onBlockSizeChanged(0, 0xffee7d16);
        long pc5 = System.currentTimeMillis();
        android.util.Log.d("BlockLoad", "PC palette: " + (pc5 - pc4) + "ms");

        LoadEventBlocksTask loadEventBlocksTask = new LoadEventBlocksTask(this);
        loadEventBlocksTask.execute();
        long pc6 = System.currentTimeMillis();
        android.util.Log.d("BlockLoad", "PC taskExec: " + (pc6 - pc5) + "ms");

        loadBlockCollections();
        android.util.Log.d("BlockLoad", "PC z(): " + (System.currentTimeMillis() - pc6) + "ms total=" + (System.currentTimeMillis() - pc0) + "ms");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!super.isStoragePermissionGranted()) {
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString("sc_id", scId);
        bundle.putString("id", id);
        bundle.putString("event", eventName);
        bundle.putParcelable("project_file", projectFile);
        super.onSaveInstanceState(bundle);
        ArrayList<BlockBean> blocks = blockPane.getBlocks();
        ProjectDataStore projectDataStore = ProjectDataManager.getProjectDataManager(scId);
        String javaName = projectFile.getJavaName();
        projectDataStore.putBlocks(javaName, id + "_" + eventName, blocks);
        ProjectDataManager.getProjectDataManager(scId).saveAllBackup();
    }

    @Override
    public void onSelected(MoreBlockCollectionBean moreBlockCollectionBean) {
        new MoreblockImporter(this, scId, projectFile).importMoreblock(moreBlockCollectionBean, () -> onBlockSizeChanged(8, 0xff8a55d7));
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int actionMasked = event.getActionMasked();
        if (event.getPointerId(event.getActionIndex()) > 0) {
            return true;
        }
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            isDragged = false;
            handler.postDelayed(longPressed, ViewConfiguration.getLongPressTimeout() / 2);
            int[] locationOnScreen = new int[2];
            v.getLocationOnScreen(locationOnScreen);
            touchOriginX = locationOnScreen[0];
            touchOriginY = locationOnScreen[1];
            posInitX = event.getRawX();
            posInitY = event.getRawY();
            currentTouchedView = v;
            return true;
        }
        if (actionMasked == MotionEvent.ACTION_MOVE) {
            if (!isDragged) {
                if (Math.abs(posInitX - touchOriginX - event.getX()) >= minDist || Math.abs(posInitY - touchOriginY - event.getY()) >= minDist) {
                    currentTouchedView = null;
                    handler.removeCallbacks(longPressed);
                }
                return false;
            }
            handler.removeCallbacks(longPressed);
            float rawX = event.getRawX();
            float rawY = event.getRawY();
            dummy.updateDummyPosition(v, rawX - touchOriginX, rawY - touchOriginY, posInitX - touchOriginX, posInitY - touchOriginY, dummyOffsetX, dummyOffsetY);
            if (hitTestIconDelete(event.getRawX(), event.getRawY())) {
                dummy.setAllow(true);
                activeIconDelete(true);
                setCopyActive(false);
                setFavoriteActive(false);
                setDetailActive(false);
                return true;
            }
            activeIconDelete(false);
            if (hitTestCopy(event.getRawX(), event.getRawY())) {
                dummy.setAllow(true);
                setCopyActive(true);
                setFavoriteActive(false);
                setDetailActive(false);
                return true;
            }
            setCopyActive(false);
            if (hitTestFavorite(event.getRawX(), event.getRawY())) {
                dummy.setAllow(true);
                setFavoriteActive(true);
                setDetailActive(false);
                return true;
            }
            setFavoriteActive(false);
            if (hitTestDetail(event.getRawX(), event.getRawY())) {
                dummy.setAllow(true);
                setDetailActive(true);
                return true;
            }
            setDetailActive(false);
            dummy.getDummyLocation(this.locationBuffer);
            if (viewLogicEditor.hitTest(this.locationBuffer[0], this.locationBuffer[1])) {
                dummy.setAllow(true);
                blockPane.updateDragPreview((BlockView) currentTouchedView, this.locationBuffer[0], this.locationBuffer[1]);
            } else {
                dummy.setAllow(false);
                blockPane.hideActiveBlock();
            }
            return true;
        } else if (actionMasked == MotionEvent.ACTION_UP) {
            currentTouchedView = null;
            handler.removeCallbacks(longPressed);
            if (!isDragged) {
                if (v instanceof BlockView rs) {
                    if (rs.getBlockType() == 0) {
                        handleBlockFieldClick(rs, event.getX(), event.getY());
                    }
                }
                return false;
            }
            paletteBlock.setDragEnabled(true);
            viewLogicEditor.setScrollEnabled(true);
            editorDrawer.setDragEnabled(true);
            dummy.setDummyVisibility(View.GONE);
            if (!dummy.getAllow()) {
                BlockView rs2 = (BlockView) v;
                if (rs2.getBlockType() == 0) {
                    blockPane.setBlockTreeVisibility(rs2, 0);
                    if (dragSourceParent != null) {
                        if (dragConnectionType == 0) {
                            dragSourceParent.nextBlock = (Integer) v.getTag();
                        }
                        if (dragConnectionType == 2) {
                            dragSourceParent.subStack1 = (Integer) v.getTag();
                        }
                        if (dragConnectionType == 3) {
                            dragSourceParent.subStack2 = (Integer) v.getTag();
                        }
                        if (dragConnectionType == 5) {
                            dragSourceParent.replaceParameter((BaseBlockView) dragSourceParent.childViews.get(dragParameterIndex), rs2);
                        }
                        rs2.parentBlock = dragSourceParent;
                        dragSourceParent.getRootBlock().layoutChain();
                    } else {
                        rs2.getRootBlock().layoutChain();
                    }
                }
                onBlockDropped();
            } else if (logicTopMenu.isDeleteActive) {
                BlockView rs5 = (BlockView) v;
                if (rs5.getBlockType() == 2) {
                    toggleDrawerVisibility(true);
                    showDeleteFavoriteDialog(rs5.spec);
                } else {
                    activeIconDelete(false);
                    int id;
                    try {
                        id = Integer.parseInt(rs5.getBean().id);
                    } catch (NumberFormatException e) {
                        id = -1;
                    }
                    BlockBean blockBean2;
                    if (dragSourceParent != null && id != -1) {
                        BlockBean clone = dragSourceParent.getBean().clone();
                        if (dragConnectionType == 0) {
                            clone.nextBlock = id;
                        } else if (dragConnectionType == 2) {
                            clone.subStack1 = id;
                        } else if (dragConnectionType == 3) {
                            clone.subStack2 = id;
                        } else if (dragConnectionType == 5) {
                            clone.parameters.set(dragParameterIndex, "@" + id);
                        }
                        blockBean2 = clone;
                    } else {
                        blockBean2 = null;
                    }
                    ArrayList<BlockBean> removedBlocks = new ArrayList<>();
                    for (BlockView allChild : rs5.getAllChildren()) {
                        removedBlocks.add(allChild.getBean().clone());
                    }
                    deleteBlock(rs5);
                    BlockBean blockBean3 = null;
                    if (dragSourceParent != null) {
                        blockBean3 = dragSourceParent.getBean().clone();
                    }
                    int[] oLocationOnScreen = new int[2];
                    blockPane.getLocationOnScreen(oLocationOnScreen);
                    BlockHistoryManager.getInstance(scId).recordRemove(buildHistoryKey(), removedBlocks, ((int) touchOriginX) - oLocationOnScreen[0], ((int) touchOriginY) - oLocationOnScreen[1], blockBean2, blockBean3);
                    refreshOptionsMenu();
                }
            } else if (logicTopMenu.isFavoriteActive) {
                setFavoriteActive(false);
                BlockView rs7 = (BlockView) v;
                blockPane.setBlockTreeVisibility(rs7, 0);
                if (dragSourceParent != null) {
                    if (dragConnectionType == 0) {
                        dragSourceParent.nextBlock = (Integer) v.getTag();
                    }
                    if (dragConnectionType == 2) {
                        dragSourceParent.subStack1 = (Integer) v.getTag();
                    }
                    if (dragConnectionType == 3) {
                        dragSourceParent.subStack2 = (Integer) v.getTag();
                    }
                    if (dragConnectionType == 5) {
                        dragSourceParent.replaceParameter((BaseBlockView) dragSourceParent.childViews.get(dragParameterIndex), rs7);
                    }
                    rs7.parentBlock = dragSourceParent;
                    dragSourceParent.getRootBlock().layoutChain();
                } else {
                    rs7.getRootBlock().layoutChain();
                }
                showSaveToFavorites(rs7);
            } else if (logicTopMenu.isDetailActive) {
                setDetailActive(false);
                if (v instanceof DefinitionBlockView) {
                    showBlockCollection(((DefinitionBlockView) v).spec);
                }
            } else if (logicTopMenu.isCopyActive) {
                setCopyActive(false);
                BlockView rs10 = (BlockView) v;
                blockPane.setBlockTreeVisibility(rs10, 0);
                if (dragSourceParent != null) {
                    if (dragConnectionType == 0) {
                        dragSourceParent.nextBlock = (Integer) v.getTag();
                    }
                    if (dragConnectionType == 2) {
                        dragSourceParent.subStack1 = (Integer) v.getTag();
                    }
                    if (dragConnectionType == 3) {
                        dragSourceParent.subStack2 = (Integer) v.getTag();
                    }
                    if (dragConnectionType == 5) {
                        dragSourceParent.replaceParameter((BaseBlockView) dragSourceParent.childViews.get(dragParameterIndex), rs10);
                    }
                    rs10.parentBlock = dragSourceParent;
                    dragSourceParent.getRootBlock().layoutChain();
                } else {
                    // somehow the blocks is moving to the last position
                    // commenting it to fix it too
                    // rs10.getRootBlock().layoutChain();
                }
                ArrayList<BlockBean> clonedBeans = new ArrayList<>();
                for (BlockView rs : rs10.getAllChildren()) {
                    BlockBean clonedBean = rs.getBean().clone();
                    int id;
                    try {
                        id = Integer.parseInt(clonedBean.id);
                    } catch (NumberFormatException e) {
                        id = -1;
                    }
                    if (id != -1) {
                        clonedBean.id = String.valueOf(id + 99000000);
                        if (clonedBean.nextBlock > 0) {
                            clonedBean.nextBlock = clonedBean.nextBlock + 99000000;
                        }
                        if (clonedBean.subStack1 > 0) {
                            clonedBean.subStack1 = clonedBean.subStack1 + 99000000;
                        }
                        if (clonedBean.subStack2 > 0) {
                            clonedBean.subStack2 = clonedBean.subStack2 + 99000000;
                        }
                        for (int i = 0; i < clonedBean.parameters.size(); i++) {
                            String parameter = clonedBean.parameters.get(i);
                            if (parameter != null && !parameter.isEmpty() && parameter.charAt(0) == '@') {
                                clonedBean.parameters.set(i, "@" + (Integer.parseInt(parameter.substring(1)) + 99000000));
                            }
                        }
                        clonedBeans.add(clonedBean);
                    }
                }
                int[] nLocationOnScreen = new int[2];
                viewLogicEditor.getLocationOnScreen(nLocationOnScreen);
                int width = nLocationOnScreen[0] + (viewLogicEditor.getWidth() / 2);
                int dropY = nLocationOnScreen[1] + ((int) ViewUtil.dpToPx(getContext(), 4.0f));
                ArrayList<BlockBean> addedBlocks = addBlockBeans(clonedBeans, width, dropY, true);
                int[] oLocationOnScreen = new int[2];
                blockPane.getLocationOnScreen(oLocationOnScreen);
                BlockHistoryManager.getInstance(scId).recordAddMultiple(buildHistoryKey(), addedBlocks, width - oLocationOnScreen[0], dropY - oLocationOnScreen[1], null, null);
                refreshOptionsMenu();
            } else if (v instanceof BlockView rs13) {
                dummy.getDummyLocation(this.locationBuffer);
                if (rs13.getBlockType() == 1) {
                    int addTargetId = blockPane.getAddTargetId();
                    BlockBean prevAddTargetClone = addTargetId >= 0 ? blockPane.findBlockById(addTargetId).getBean().clone() : null;
                    BlockView droppedBlock = dropBlockOnPane(rs13, this.locationBuffer[0], this.locationBuffer[1], false);
                    BlockBean blockBean3 = null;
                    if (addTargetId >= 0) {
                        blockBean3 = blockPane.findBlockById(addTargetId).getBean().clone();
                    }
                    int[] locationOnScreen = new int[2];
                    blockPane.getLocationOnScreen(locationOnScreen);
                    BlockHistoryManager.getInstance(scId).recordAdd(buildHistoryKey(), droppedBlock.getBean().clone(), this.locationBuffer[0] - locationOnScreen[0], this.locationBuffer[1] - locationOnScreen[1], prevAddTargetClone, blockBean3);
                    if (prevAddTargetClone != null) {
                        prevAddTargetClone.print();
                    }
                    if (blockBean3 != null) {
                        blockBean3.print();
                    }
                } else if (rs13.getBlockType() == 2) {
                    int defAddTargetId = blockPane.getAddTargetId();
                    BlockBean prevDefTargetClone = defAddTargetId >= 0 ? blockPane.findBlockById(defAddTargetId).getBean().clone() : null;
                    ArrayList<BlockBean> data = ((DefinitionBlockView) v).getData();
                    ArrayList<BlockBean> addedBlocks = addBlockBeans(data, this.locationBuffer[0], this.locationBuffer[1], true);
                    if (!addedBlocks.isEmpty()) {
                        BlockView firstAddedBlock = blockPane.findBlockByString(addedBlocks.get(0).id);
                        dropBlockOnPane(firstAddedBlock, this.locationBuffer[0], this.locationBuffer[1], true);
                        BlockBean currentParentData = null;
                        if (defAddTargetId >= 0) {
                            currentParentData = blockPane.findBlockById(defAddTargetId).getBean().clone();
                        }
                        int[] locationOnScreen = new int[2];
                        blockPane.getLocationOnScreen(locationOnScreen);
                        BlockHistoryManager.getInstance(scId).recordAddMultiple(buildHistoryKey(), addedBlocks, this.locationBuffer[0] - locationOnScreen[0], this.locationBuffer[1] - locationOnScreen[1], prevDefTargetClone, currentParentData);
                    }
                    blockPane.clearSnapState();
                } else {
                    blockPane.setBlockTreeVisibility(rs13, 0);
                    int id = Integer.parseInt(rs13.getBean().id);
                    BlockBean blockBean;
                    if (dragSourceParent != null) {
                        blockBean = dragSourceParent.getBean().clone();
                        if (dragConnectionType == 0) {
                            blockBean.nextBlock = id;
                        } else if (dragConnectionType == 2) {
                            blockBean.subStack1 = id;
                        } else if (dragConnectionType == 3) {
                            blockBean.subStack2 = id;
                        } else if (dragConnectionType == 5) {
                            blockBean.parameters.set(dragParameterIndex, "@" + id);
                        }
                    } else {
                        blockBean = null;
                    }
                    BlockView targetBlock = blockPane.findBlockById(blockPane.getAddTargetId());
                    BlockBean prevMoveTargetClone = targetBlock != null ? targetBlock.getBean().clone() : null;
                    ArrayList<BlockView> allChildren3 = rs13.getAllChildren();
                    ArrayList<BlockBean> beansBeforeMove = new ArrayList<>();
                    for (BlockView rs : allChildren3) {
                        beansBeforeMove.add(rs.getBean().clone());
                    }
                    dropBlockOnPane(rs13, this.locationBuffer[0], this.locationBuffer[1], true);
                    ArrayList<BlockBean> beansAfterMove = new ArrayList<>();
                    for (BlockView rs : allChildren3) {
                        beansAfterMove.add(rs.getBean().clone());
                    }
                    BlockBean prevDragSourceClone = dragSourceParent != null ? dragSourceParent.getBean().clone() : null;
                    BlockBean blockBean3 = null;
                    if (targetBlock != null) {
                        blockBean3 = targetBlock.getBean().clone();
                    }
                    if (blockBean == null || prevDragSourceClone == null || !blockBean.isEqual(prevDragSourceClone)) {
                        int[] locationOnScreen = new int[2];
                        blockPane.getLocationOnScreen(locationOnScreen);
                        int x = locationOnScreen[0];
                        int y = locationOnScreen[1];
                        BlockHistoryManager.getInstance(scId).recordMove(buildHistoryKey(), beansBeforeMove, beansAfterMove, ((int) touchOriginX) - x, ((int) touchOriginY) - y, this.locationBuffer[0] - x, this.locationBuffer[1] - y, blockBean, prevDragSourceClone, prevMoveTargetClone, blockBean3);
                    }
                    blockPane.clearSnapState();
                }
                refreshOptionsMenu();
                blockPane.clearSnapState();
            }
            dummy.setAllow(false);
            toggleTopMenuVisibility(false);
            isDragged = false;
            return true;
        } else if (actionMasked == MotionEvent.ACTION_CANCEL) {
            handler.removeCallbacks(longPressed);
            isDragged = false;
            return false;
        } else if (actionMasked == MotionEvent.ACTION_SCROLL) {
            handler.removeCallbacks(longPressed);
            isDragged = false;
            return false;
        } else {
            return true;
        }
    }

    public boolean isTopMenuEnabled() {
        return true;
    }

    public void onBlockDropped() {
    }

    private void startDragMode() {
        if (currentTouchedView != null) {
            paletteBlock.setDragEnabled(false);
            viewLogicEditor.setScrollEnabled(false);
            editorDrawer.setDragEnabled(false);
            if (isDrawerVisible) {
                toggleDrawerVisibility(false);
            }

            if (isVibrationEnabled) {
                vibrator.vibrate(100L);
            }

            isDragged = true;
            if (((BlockView) currentTouchedView).getBlockType() == 0) {
                trackDragSource((BlockView) currentTouchedView);
                toggleLayoutVisibility(true);
                toggleTopMenuVisibility(true);
                dummy.setBlockTypeImage((BlockView) currentTouchedView);
                blockPane.setBlockTreeVisibility((BlockView) currentTouchedView, 8);
                blockPane.detachFromParent((BlockView) currentTouchedView);
                blockPane.computeSnapPoints((BlockView) currentTouchedView);
            } else if (((BlockView) currentTouchedView).getBlockType() == 2) {
                toggleLayoutVisibility(false);
                toggleTopMenuVisibility(true);
                dummy.setBlockTypeImage((BlockView) currentTouchedView);
                blockPane.computeSnapPointsForBlocks((BlockView) currentTouchedView, ((DefinitionBlockView) currentTouchedView).getData());
            } else {
                dummy.setBlockTypeImage((BlockView) currentTouchedView);
                blockPane.computeSnapPoints((BlockView) currentTouchedView);
            }

            float deltaX = posInitX - touchOriginX;
            float deltaY = posInitY - touchOriginY;
            dummy.updateDummyPosition(currentTouchedView, deltaX, deltaY, deltaX, deltaY, dummyOffsetX, dummyOffsetY);
            dummy.getDummyLocation(locationBuffer);
            if (viewLogicEditor.hitTest(locationBuffer[0], locationBuffer[1])) {
                dummy.setAllow(true);
                blockPane.updateDragPreview((BlockView) currentTouchedView, locationBuffer[0], locationBuffer[1]);
            } else {
                dummy.setAllow(false);
                blockPane.hideActiveBlock();
            }
        }
    }

    public final String buildHistoryKey() {
        return BlockHistoryManager.buildKey(projectFile.getJavaName(), id, eventName);
    }

    public void showSourceCode() {
        ProjectFilePaths projectFilePaths = new ProjectFilePaths(this, scId);
        projectFilePaths.initializeMetadata(ProjectDataManager.getLibraryManager(scId), ProjectDataManager.getFileManager(scId), ProjectDataManager.getProjectDataManager(scId));
        String code = new BlockInterpreter(projectFile.getActivityName(), projectFilePaths.buildConfig, blockPane.getBlocks(), isViewBindingEnabled).interpretBlocks();
        var intent = new Intent(this, CodeViewerActivity.class);
        intent.putExtra("code", code);
        intent.putExtra("sc_id", scId);
        intent.putExtra("scheme", CodeViewerActivity.SCHEME_JAVA);
        startActivity(intent);
    }

    public void initDrawerAnimators() {
        drawerShowAnimator = ObjectAnimator.ofFloat(editorDrawer, View.TRANSLATION_X, 0.0f);
        drawerShowAnimator.setDuration(500L);
        drawerShowAnimator.setInterpolator(new DecelerateInterpolator());
        drawerHideAnimator = ObjectAnimator.ofFloat(editorDrawer, View.TRANSLATION_X, editorDrawer.getHeight());
        drawerHideAnimator.setDuration(300L);
        drawerHideAnimator.setInterpolator(new DecelerateInterpolator());
        drawerAnimatorsInitialized = true;
    }

    public void initTopMenuAnimators() {
        topMenuShowAnimator = ObjectAnimator.ofFloat(logicTopMenu, View.TRANSLATION_Y, 0.0f);
        topMenuShowAnimator.setDuration(500L);
        topMenuShowAnimator.setInterpolator(new DecelerateInterpolator());
        topMenuHideAnimator = ObjectAnimator.ofFloat(logicTopMenu, View.TRANSLATION_Y, logicTopMenu.getHeight() * (-1));
        topMenuHideAnimator.setDuration(300L);
        topMenuHideAnimator.setInterpolator(new DecelerateInterpolator());
        topMenuAnimatorsInitialized = true;
    }

    public void loadBlockCollections() {
        editorDrawer.clearAllBlocks();
        for (BlockCollectionBean next : BlockCollectionManager.getInstance().getBlocks()) {
            editorDrawer.addBlockCollection(next.name, next.blocks).setOnTouchListener(this);
        }
    }

    private static class ProjectSaver extends BaseAsyncTask {
        private final WeakReference<LogicEditorActivity> activity;

        public ProjectSaver(LogicEditorActivity logicEditorActivity) {
            super(logicEditorActivity);
            activity = new WeakReference<>(logicEditorActivity);
            logicEditorActivity.addTask(this);
        }

        @Override
        public void onSuccess() {
            var act = activity.get();
            if (act == null) return;
            act.dismissLoadingDialog();
            act.finish();
        }

        @Override
        public void onError(String errorMessage) {
            Toast.makeText(getContext(), R.string.common_error_failed_to_save, Toast.LENGTH_SHORT).show();
            var act = activity.get();
            if (act == null) return;
            act.dismissLoadingDialog();
        }

        @Override
        public void doWork() {
            var act = activity.get();
            if (act == null) return;
            publishProgress("Now saving..");
            act.saveBlocks();
        }
    }

    public static class LoadEventBlocksTask {
        private final WeakReference<LogicEditorActivity> activityRef;

        public LoadEventBlocksTask(LogicEditorActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        public void execute() {
            getActivity().showLoadingDialog();
            new Thread(this::doInBackground).start();
        }

        private void doInBackground() {
            LogicEditorActivity activity = getActivity();
            if (activity != null) {
                activity.loadEventBlocks();
                activity.runOnUiThread(activity::dismissLoadingDialog);
            }
        }

        private LogicEditorActivity getActivity() {
            return activityRef.get();
        }
    }

    public class ImagePickerAdapter extends RecyclerView.Adapter<ImagePickerAdapter.ViewHolder> {

        private final ArrayList<String> images;
        private final OnImageSelectedListener listener;
        private final ArrayList<String> filteredImages;
        private final Map<String, View> imageCache = new HashMap<>();
        private String selectedImage;

        public ImagePickerAdapter(ArrayList<String> images, String selectedImage, OnImageSelectedListener listener) {
            this.images = images;
            this.selectedImage = selectedImage;
            this.listener = listener;
            filteredImages = new ArrayList<>(images);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImagePickerItemBinding binding = ImagePickerItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String image = filteredImages.get(position);

            holder.binding.textView.setText(image);

            View imageView = imageCache.get(image);
            if (imageView == null) {
                imageView = setImageViewContent(image);
                imageCache.put(image, imageView);
            }

            if (imageView.getParent() != null) {
                ((ViewGroup) imageView.getParent()).removeView(imageView);
            }

            holder.binding.layoutImg.removeAllViews();
            holder.binding.layoutImg.addView(imageView);

            holder.binding.radioButton.setChecked(image.equals(selectedImage));

            holder.binding.transparentOverlay.setOnClickListener(v -> {
                if (!image.equals(selectedImage)) {
                    selectedImage = image;
                    listener.onImageSelected(image);
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return filteredImages.size();
        }

        public void filter(String query) {
            filteredImages.clear();
            if (query.isEmpty()) {
                filteredImages.addAll(images);
            } else {
                for (String image : images) {
                    if (image.toLowerCase().contains(query)) {
                        filteredImages.add(image);
                    }
                }
            }
            notifyDataSetChanged();
        }

        public interface OnImageSelectedListener {
            void onImageSelected(String image);
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public final ImagePickerItemBinding binding;

            public ViewHolder(@NonNull ImagePickerItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
