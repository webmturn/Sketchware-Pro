package pro.sketchware.activities.editor.block;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import pro.sketchware.widgets.LoadingDialog;
import pro.sketchware.core.async.BackgroundTasks;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowInsetsCompat;

import pro.sketchware.activities.base.BaseAppCompatActivity;
import pro.sketchware.widgets.ColorPickerDialog;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import pro.sketchware.core.block.BlockSpecParser;
import pro.sketchware.core.block.BlockTypeUtils;
import pro.sketchware.core.block.view.BlockView;
import pro.sketchware.core.codegen.CodeTemplateValidator;
import pro.sketchware.util.Helper;
import pro.sketchware.util.ColorTextViewListener;
import pro.sketchware.util.LogUtil;
import pro.sketchware.R;
import pro.sketchware.databinding.ActivityBlocksManagerCreatorBinding;
import pro.sketchware.util.BaseTextWatcher;
import pro.sketchware.lib.highlighter.SimpleHighlighter;
import pro.sketchware.util.PropertiesUtil;
import pro.sketchware.util.SketchwareUtil;
import pro.sketchware.util.UI;

public class BlocksManagerCreatorActivity extends BaseAppCompatActivity {

    private final ArrayList<String> id_detector = new ArrayList<>();
    private ActivityBlocksManagerCreatorBinding binding;
    private ArrayList<HashMap<String, Object>> blocksList = new ArrayList<>();
    /**
     * Current mode of this activity, "edit" if editing a block, "add" if creating a new block and "insert" if inserting a block above another
     */
    private String mode = "";
    /**
     * Position of current editing/adding/inserting block in palette
     */
    private int blockPosition = 0;
    private String palletColour = "";
    private String path = "";
    private String selectedBlockType = " ";
    private static final ExecutorService LOADER = BackgroundTasks.createSingleThreadExecutor("Sketchware-BlockEditorLoad");
    private Future<?> loadTask;
    private long loadGeneration;
    private boolean loaded;
    private boolean conflicted;
    private boolean restoring;
    private boolean formInitialized;
    private String ownerToken;
    private byte[] loadedDigest;
    private byte[] anchorDigest;
    private String anchorName;
    private LoadingDialog workDialog;
    private AlertDialog saveErrorDialog;
    private BlockEditorSaveState saveState;
    private String palettePath;
    private byte[] paletteAnchor;
    private byte[] loadedPaletteDigest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        saveState = new androidx.lifecycle.ViewModelProvider(this).get(BlockEditorSaveState.class);
        if (saveState.saved) { setResult(RESULT_OK); finish(); return; }
        conflicted = saveState.conflicted;
        restoring = savedInstanceState != null && savedInstanceState.getBoolean("block_editor_form_ready", true);
        anchorDigest = getIntent().getByteArrayExtra("block_editor_anchor");
        anchorName = getIntent().getStringExtra("block_editor_anchor_name");
        palettePath = getIntent().getStringExtra("block_editor_palette_path");
        paletteAnchor = getIntent().getByteArrayExtra("block_editor_palette_anchor");
        if (savedInstanceState != null) {
            selectedBlockType = savedInstanceState.getString("selected_block_type", " ");
            byte[] savedAnchor = savedInstanceState.getByteArray("block_editor_anchor");
            if (savedAnchor != null) anchorDigest = savedAnchor;
            anchorName = savedInstanceState.getString("block_editor_anchor_name");
        }
        ownerToken = getIntent().getStringExtra("block_editor_owner");
        if (ownerToken == null) { ownerToken = UUID.randomUUID().toString(); getIntent().putExtra("block_editor_owner", ownerToken); }
        binding = ActivityBlocksManagerCreatorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize();
        initializeLogic();
    }

    @Override
    public void onDestroy() {
        loadGeneration++;
        if (loadTask != null) loadTask.cancel(true);
        if (workDialog != null) { workDialog.cancelAnimation(); workDialog.dismiss(); }
        if (saveErrorDialog != null) saveErrorDialog.dismiss();
        super.onDestroy();
        binding = null;
    }

    @Override public void onSaveInstanceState(Bundle state) {
        state.putString("selected_block_type", selectedBlockType);
        state.putBoolean("block_editor_form_ready", formInitialized || restoring);
        state.putByteArray("block_editor_anchor", anchorDigest);
        state.putString("block_editor_anchor_name", anchorName);
        super.onSaveInstanceState(state);
    }

    @Override public void onResume() {
        super.onResume();
        if (BlocksManager.pendingOrderSave != null) observeSave();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (BlocksManager.pendingOrderSave != null) { observeSave(); return true; }
        if (!loaded && binding != null) {
            android.graphics.Rect toolbar = new android.graphics.Rect();
            binding.appBarLayout.getGlobalVisibleRect(toolbar);
            if (!toolbar.contains((int) event.getRawX(), (int) event.getRawY())) return true;
        }
        return super.dispatchTouchEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initialize() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());

        binding.name.addTextChangedListener(new BaseTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!loaded || conflicted || BlocksManager.pendingOrderSave != null) { binding.save.setEnabled(false); return; }
                String string = s.toString();
                boolean currentName = "edit".equals(mode) && blockPosition >= 0 && blockPosition < blocksList.size()
                        && blocksList.get(blockPosition) != null && string.equals(blocksList.get(blockPosition).get("name"));
                if (!id_detector.contains(string) || currentName) {
                    binding.nameLayout.setError(null);
                    binding.nameLayout.setErrorEnabled(false);
                    binding.save.setEnabled(true);
                } else {
                    binding.nameLayout.setError(Helper.getResString(R.string.error_block_name_in_use));
                    binding.nameLayout.setErrorEnabled(true);
                    binding.save.setEnabled(false);
                }
            }
        });

        binding.type.setOnClickListener(view -> binding.selectType.performClick());
        binding.selectType.setOnClickListener(v -> {
            List<String> types = BlockTypeUtils.getSelectableBlockTypes();
            List<String> choices = new ArrayList<>();
            for (String type : types) {
                choices.add(BlockTypeUtils.getDisplayBlockTypeLabel(this, type));
            }
            int selectedIndex = types.indexOf(selectedBlockType);
            AtomicInteger choice = new AtomicInteger(selectedIndex >= 0 ? selectedIndex : 0);
            new MaterialAlertDialogBuilder(this).setTitle(R.string.blocks_type_title)
                    .setSingleChoiceItems(choices.toArray(new String[0]),
                            selectedIndex >= 0 ? selectedIndex : 0, (dialog, which) -> choice.set(which))
                    .setPositiveButton(R.string.common_word_save, (dialog, which) -> setSelectedBlockType(types.get(choice.get())))
                    .setNegativeButton(R.string.common_word_cancel, null)
                    .create().show();
        });

        binding.spec.addTextChangedListener(new BaseTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateBlockSpec(selectedBlockType, Helper.getText(binding.colour));
                updateCodeTemplateWarning();
            }
        });

        binding.code.addTextChangedListener(new BaseTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCodeTemplateWarning();
            }
        });

        binding.openColorPalette.setOnClickListener(v -> {
            ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, 0, false, false);
            colorPickerDialog.setColorPickerCallback(new ColorTextViewListener(binding.colour));
            colorPickerDialog.showAtLocation(v, Gravity.CENTER, 0, 0);
        });

        binding.colour.addTextChangedListener(new BaseTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!PropertiesUtil.isHexColor(s.toString())) {
                    binding.colourLayout.setError(Helper.getResString(R.string.error_invalid_hex_color_short));
                    binding.colourLayout.setErrorEnabled(true);
                } else {
                    binding.colourLayout.setError(null);
                    binding.colourLayout.setErrorEnabled(false);
                }
                updateBlockSpec(selectedBlockType, s.toString());
            }
        });

        binding.cancel.setOnClickListener(Helper.getBackPressedClickListener(this));
        binding.save.setOnClickListener(v -> {
            if (!loaded || conflicted || waitForSave()) return;
            if (!PropertiesUtil.isHexColor(Helper.getText(binding.colour))) {
                SketchwareUtil.showMessage(getApplicationContext(), Helper.getResString(R.string.error_invalid_hex_color_toast));
                return;
            }
            if (BlockSpecParser.findInvalidMenuParameterEnd(Helper.getText(binding.spec)) >= 0) {
                SketchwareUtil.showMessage(getApplicationContext(), Helper.getResString(R.string.error_invalid_block_params));
                return;
            }
            if (mode.equals("add")) {
                addBlock();
            }
            if (mode.equals("insert")) {
                insertBlockAt(blockPosition);
            }
            if (mode.equals("edit")) {
                editBlock(blockPosition);
            }
        });

        binding.reset.setOnClickListener(v -> binding.colour.setText(palletColour));
        binding.spec.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;

                case MotionEvent.ACTION_UP:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            v.onTouchEvent(event);
            return true;
        });
    }

    private void initializeLogic() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (waitForSave()) return;
                setEnabled(false); getOnBackPressedDispatcher().onBackPressed();
            }
        });
        inputProperties();
        addParameters();
        receiveIntents();
        updateCodeTemplateWarning();
        new SimpleHighlighter(binding.code);

        UI.addWindowInsetToPadding(binding.content, WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.displayCutout(), true, false, true, true);
        UI.addSystemWindowInsetToPadding(binding.appBarLayout, true, true, true, true);
    }

    private void updateCodeTemplateWarning() {
        int available = BlockSpecParser.parseParameters(Helper.getText(binding.spec)).size() + 2;
        CodeTemplateValidator.Problem problem = CodeTemplateValidator.findProblem(Helper.getText(binding.code), available);
        String warning = null;
        if (problem != null) {
            warning = switch (problem.kind) {
                case INVALID_FORMAT -> getString(R.string.block_code_template_invalid_format,
                        problem.specifier, problem.position, problem.errorType);
                case STRING_CONVERSION -> getString(R.string.block_code_template_string_conversion, problem.specifier);
                case MISSING_PREVIOUS_ARGUMENT -> getString(R.string.block_code_template_missing_previous, problem.specifier);
                case NOT_ENOUGH_ARGUMENTS -> getString(R.string.block_code_template_missing_arguments,
                        problem.requiredArguments, problem.availableArguments);
            };
        }
        binding.codeLayout.setHelperText(warning);
    }

    private View addBlockMenu(String menu, String name) {
        TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        textView.setPadding(
                SketchwareUtil.dpToPx(8),
                0,
                SketchwareUtil.dpToPx(8),
                0
        );
        textView.setTextColor(MaterialColors.getColor(textView, R.attr.colorPrimary));
        textView.setText(name);
        textView.setTextSize(14.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder(Helper.getText(binding.spec));
            int selectionStart = binding.spec.getSelectionStart();
            sb.insert(selectionStart, menu);
            binding.spec.setText(sb);
            binding.spec.setSelection(selectionStart + menu.length());
        });
        return textView;
    }

    private void inputProperties() {
        binding.name.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        binding.name.setMaxLines(1);
        binding.type.setInputType(InputType.TYPE_NULL);
        binding.type.setMaxLines(1);
        binding.type.setKeyListener(null);
        binding.type.setCursorVisible(false);
        binding.type.setLongClickable(false);
        binding.typename.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        binding.typename.setMaxLines(1);
        binding.spec.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        binding.spec.setMaxLines(1);
        binding.colour.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        binding.colour.setMaxLines(1);
        binding.spec2.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        binding.spec2.setMaxLines(1);
        binding.spec2Layout.setVisibility(View.GONE);
        setSelectedBlockType(" ");
    }

    private void addParameters() {
        binding.parametersHolder.addView(addBlockMenu("%s.inputOnly ", Helper.getResString(R.string.block_parameter_inputonly)));
        binding.parametersHolder.addView(addBlockMenu("%s ", Helper.getResString(R.string.block_parameter_string)));
        binding.parametersHolder.addView(addBlockMenu("%b ", Helper.getResString(R.string.block_parameter_boolean)));
        binding.parametersHolder.addView(addBlockMenu("%d ", Helper.getResString(R.string.block_parameter_number)));
        binding.parametersHolder.addView(addBlockMenu("%m.varMap ", Helper.getResString(R.string.block_parameter_map)));
        binding.parametersHolder.addView(addBlockMenu("%m.view ", Helper.getResString(R.string.block_parameter_view)));
        binding.parametersHolder.addView(addBlockMenu("%m.textview ", Helper.getResString(R.string.block_parameter_textview)));
        binding.parametersHolder.addView(addBlockMenu("%m.edittext ", Helper.getResString(R.string.block_parameter_edittext)));
        binding.parametersHolder.addView(addBlockMenu("%m.imageview ", Helper.getResString(R.string.block_parameter_imageview)));
        binding.parametersHolder.addView(addBlockMenu("%m.listview ", Helper.getResString(R.string.block_parameter_listview)));
        binding.parametersHolder.addView(addBlockMenu("%m.list ", Helper.getResString(R.string.block_parameter_list)));
        binding.parametersHolder.addView(addBlockMenu("%m.listMap ", Helper.getResString(R.string.block_parameter_listmap)));
        binding.parametersHolder.addView(addBlockMenu("%m.listStr ", Helper.getResString(R.string.block_parameter_liststring)));
        binding.parametersHolder.addView(addBlockMenu("%m.listInt ", Helper.getResString(R.string.block_parameter_listnumber)));
        binding.parametersHolder.addView(addBlockMenu("%m.intent ", Helper.getResString(R.string.block_parameter_intent)));
        binding.parametersHolder.addView(addBlockMenu("%m.color ", Helper.getResString(R.string.block_parameter_color)));
        binding.parametersHolder.addView(addBlockMenu("%m.activity ", Helper.getResString(R.string.block_parameter_activity)));
        binding.parametersHolder.addView(addBlockMenu("%m.resource ", Helper.getResString(R.string.block_parameter_resource)));
        binding.parametersHolder.addView(addBlockMenu("%m.customViews ", Helper.getResString(R.string.block_parameter_custom_views)));
        binding.parametersHolder.addView(addBlockMenu("%m.layout ", Helper.getResString(R.string.block_parameter_layout)));
        binding.parametersHolder.addView(addBlockMenu("%m.anim ", Helper.getResString(R.string.block_parameter_anim)));
        binding.parametersHolder.addView(addBlockMenu("%m.drawable ", Helper.getResString(R.string.block_parameter_drawable)));
        binding.parametersHolder.addView(addBlockMenu("%m.ResString ", Helper.getResString(R.string.block_parameter_resstrings)));
    }

    private void receiveIntents() {
        mode = getIntent().getStringExtra("mode");
        path = getIntent().getStringExtra("path");
        palletColour = getIntent().getStringExtra("color");
        if (mode.equals("add")) {
            blockPosition = Integer.parseInt(getIntent().getStringExtra("pallet"));
            binding.colour.setText(palletColour);
            getSupportActionBar().setTitle(R.string.blocks_add_title);
            getBlockList();
            return;
        }
        blockPosition = Integer.parseInt(getIntent().getStringExtra("pos"));
        binding.colour.setText(palletColour);
        getSupportActionBar().setTitle(R.string.blocks_insert_title);
        if (mode.equals("edit")) {
            getSupportActionBar().setTitle(R.string.blocks_edit_title);
        }
        getBlockList();
    }

    private void fillUpInputs(int position) {
        HashMap<String, Object> block = blocksList.get(position);

        Object nameObject = block.get("name");
        if (nameObject instanceof String) {
            binding.name.setText((String) nameObject);
        } else {
            binding.nameLayout.setError(Helper.getResString(R.string.error_invalid_name_data));
            binding.nameLayout.setErrorEnabled(true);
        }

        Object typeObject = block.get("type");
        if (typeObject instanceof String typeString) {
            setSelectedBlockType(typeString);
        } else {
            binding.typeLayout.setError(Helper.getResString(R.string.error_invalid_type_data));
            binding.typeLayout.setErrorEnabled(true);
        }

        Object typeName = block.get("typeName");
        if (typeName != null) {
            if (typeName instanceof String) {
                binding.typename.setText((String) typeName);
            } else {
                binding.typenameLayout.setError(Helper.getResString(R.string.error_invalid_typename_data));
                binding.typenameLayout.setErrorEnabled(true);
            }
        }

        Object specObject = block.get("spec");
        if (specObject instanceof String) {
            binding.spec.setText((String) specObject);
        } else {
            binding.specLayout.setError(Helper.getResString(R.string.error_invalid_spec_data));
            binding.specLayout.setErrorEnabled(true);
        }

        Object spec2Object = block.get("spec2");
        if (spec2Object != null) {
            if (spec2Object instanceof String) {
                binding.spec2.setText((String) spec2Object);
            } else {
                binding.spec2Layout.setError(Helper.getResString(R.string.error_invalid_spec2_data));
                binding.spec2Layout.setErrorEnabled(true);
            }
        }

        Object importsObject = block.get("imports");
        if (importsObject != null) {
            if (importsObject instanceof String) {
                binding.customImport.setText((String) importsObject);
            } else {
                binding.customImportLayout.setError(Helper.getResString(R.string.error_invalid_imports_data));
                binding.customImportLayout.setErrorEnabled(true);
            }
        }

        Object colorObject = block.get("color");
        if (colorObject != null) {
            if (colorObject instanceof String) {
                binding.colour.setText((String) colorObject);
            } else {
                binding.colourLayout.setError(Helper.getResString(R.string.error_invalid_color_data));
                binding.colourLayout.setErrorEnabled(true);
            }
        } else {
            binding.colour.setText(palletColour);
        }

        Object codeObject = block.get("code");
        if (codeObject instanceof String) {
            binding.code.setText((String) codeObject);
        } else {
            binding.code.setHint(Helper.getResString(R.string.block_invalid_data));
        }
    }

    private void getBlockList() {
        if (waitForSave()) return;
        if (loadTask != null) loadTask.cancel(true);
        loaded = false;
        if (!formInitialized) binding.save.setEnabled(false);
        String filePath = path;
        String expectedName = anchorName;
        byte[] expectedAnchor = anchorDigest;
        int requestedPosition = blockPosition;
        boolean needsAnchor = !"add".equals(mode);
        boolean preserveForm = restoring;
        String paletteFile = palettePath;
        byte[] expectedPalette = paletteAnchor;
        long generation = ++loadGeneration;
        WeakReference<BlocksManagerCreatorActivity> reference = new WeakReference<>(this);
        loadTask = LOADER.submit(() -> {
            BlockEditorMutation.Loaded result = null, paletteResult = null; Exception failure = null;
            int target = requestedPosition, group = 0;
            byte[] recordIdentity = null;
            try {
                result = BlockEditorMutation.read(new File(filePath).toPath(), !needsAnchor);
                if (paletteFile != null) paletteResult = BlockEditorMutation.read(new File(paletteFile).toPath());
                if (needsAnchor && expectedAnchor != null) target = BlockEditorMutation.findAnchor(result, expectedAnchor, expectedName, requestedPosition);
                else if (needsAnchor && preserveForm) target = -1;
                if (paletteResult != null && expectedPalette != null) group = BlockEditorMutation.findAnchor(paletteResult, expectedPalette, null, requestedPosition - 9);
                if (needsAnchor && target >= 0 && target < result.blocks().size()) recordIdentity = BlockEditorMutation.recordDigest(result.blocks().get(target));
            }
            catch (Exception e) { failure = e; }
            if (Thread.currentThread().isInterrupted()) return;
            BlockEditorMutation.Loaded parsed = result; Exception error = failure;
            final int resolved = target;
            final boolean paletteConflict = group < 0;
            final int selectedGroup = paletteResult != null && expectedPalette != null && !needsAnchor ? group + 9 : resolved;
            final byte[] paletteDigest = paletteResult == null ? null : paletteResult.digest();
            byte[] identity = recordIdentity;
            BackgroundTasks.postMain(() -> {
                var owner = reference.get();
                if (owner == null || owner.isDestroyed() || owner.isFinishing() || owner.binding == null
                        || owner.loadGeneration != generation) return;
                owner.loadTask = null; owner.hideWork();
                if (error != null) {
                    LogUtil.w("BlocksManagerCreator", "Failed to load custom blocks", error);
                    if (error instanceof java.io.IOException) {
                        owner.saveErrorDialog = new MaterialAlertDialogBuilder(owner).setMessage(R.string.blocks_editor_load_failed)
                                .setPositiveButton(R.string.store_retry, (dialog, which) -> { owner.saveErrorDialog = null; owner.getBlockList(); })
                                .setNegativeButton(R.string.common_word_cancel, (dialog, which) -> owner.saveErrorDialog = null).show();
                    } else {
                        SketchwareUtil.showFailedToParseJsonDialog(owner, new File(filePath), Helper.getResString(R.string.blocks_custom_blocks), v -> owner.getBlockList());
                    }
                    return;
                }
                owner.blocksList = parsed.blocks();
                owner.loadedDigest = parsed.digest();
                owner.loadedPaletteDigest = paletteDigest;
                owner.id_detector.clear(); owner.id_detector.addAll(parsed.names());
                owner.loaded = true;
                if (paletteConflict || (needsAnchor && (resolved < 0 || resolved >= owner.blocksList.size() || owner.blocksList.get(resolved) == null))) {
                    owner.conflicted = true; owner.saveState.conflicted = true; owner.binding.save.setEnabled(false);
                    owner.saveErrorDialog = new MaterialAlertDialogBuilder(owner).setMessage(R.string.blocks_editor_conflict)
                            .setPositiveButton(R.string.common_word_ok, (dialog, which) -> owner.saveErrorDialog = null).show();
                    return;
                }
                owner.blockPosition = selectedGroup;
                owner.anchorDigest = identity;
                var record = needsAnchor ? owner.blocksList.get(resolved) : null;
                owner.anchorName = record != null && record.get("name") instanceof String name ? name : null;
                if (!owner.formInitialized) {
                    if (!owner.restoring && "edit".equals(owner.mode)) owner.fillUpInputs(owner.blockPosition);
                    else owner.setSelectedBlockType(owner.selectedBlockType);
                    owner.formInitialized = true;
                }
                owner.restoring = false;
                owner.binding.save.setEnabled(!owner.conflicted && !owner.binding.nameLayout.isErrorEnabled());
            });
        });
        binding.getRoot().postDelayed(() -> {
            var owner = reference.get();
            if (owner != null && !owner.isFinishing() && !owner.isDestroyed() && owner.loadGeneration == generation && owner.loadTask != null) owner.showWork();
        }, 100);
    }

    private void showWork() {
        if (isFinishing() || isDestroyed() || !getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return;
        if (workDialog == null) {
            workDialog = new LoadingDialog(this);
            ((TextView) workDialog.findViewById(R.id.tv_progress)).setText(R.string.common_word_please_wait);
        }
        workDialog.show(); workDialog.resumeAnimation();
        workDialog.setCancelable(BlocksManager.pendingOrderSave == null);
        workDialog.setOnCancelListener(dialog -> {
            if (BlocksManager.pendingOrderSave == null && loadTask != null) finish();
        });
    }

    private void hideWork() {
        if (workDialog != null) { workDialog.pauseAnimation(); workDialog.hide(); }
    }

    private boolean waitForSave() {
        if (BlocksManager.pendingOrderSave == null) return false;
        observeSave(); return true;
    }

    void observeSave() {
        var save = BlocksManager.pendingOrderSave;
        if (save == null || isFinishing() || isDestroyed()) return;
        boolean observed = false;
        for (var observer : save.observers) if (observer.get() == this) observed = true;
        if (!observed) save.observers.add(new WeakReference<>(this));
        if (!save.running) { onBlockSaveCompleted(save); return; }
        if (saveErrorDialog != null) { saveErrorDialog.dismiss(); saveErrorDialog = null; }
        if (!BlocksManager.delayDetailsIndicator(save, this)) showWork();
    }

    void onBlockSaveCompleted(BlocksManager.OrderSave save) {
        hideWork();
        if (save.failure == null && saveErrorDialog != null) { saveErrorDialog.dismiss(); saveErrorDialog = null; }
        boolean owned = save.editorMutation != null && ownerToken.equals(save.editorMutation.ownerToken);
        if (save.abandoned) {
            if (owned) { conflicted = true; loaded = true; binding.save.setEnabled(false); }
            else if (!loaded) getBlockList();
            return;
        }
        if (save.failure != null) {
            if (!getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED) || saveErrorDialog != null) return;
            if (save.failure instanceof BlockEditorMutation.Conflict) {
                saveErrorDialog = new MaterialAlertDialogBuilder(this).setMessage(R.string.blocks_editor_conflict)
                        .setPositiveButton(R.string.common_word_ok, (dialog, which) -> {
                            saveErrorDialog = null; BlocksManager.abandonEditorSave(save);
                        }).setCancelable(false).show();
            } else {
                saveErrorDialog = new MaterialAlertDialogBuilder(this).setMessage(R.string.common_message_save_failed)
                        .setPositiveButton(R.string.store_retry, (dialog, which) -> {
                            saveErrorDialog = null;
                            if (BlocksManager.pendingOrderSave == save && !save.running) BlocksManager.startOrderSave(save);
                            observeSave();
                        }).setCancelable(false).show();
            }
            return;
        }
        if (owned) {
            setResult(RESULT_OK); SketchwareUtil.toast(Helper.getResString(R.string.common_word_saved)); finish();
        } else if (!loaded) getBlockList();
    }

    private void saveMutation(String mutationMode, int position, HashMap<String, Object> values) {
        if (waitForSave() || !loaded || conflicted) return;
        var original = "add".equals(mutationMode) ? null : blocksList.get(position);
        var mutation = new BlockEditorMutation(ownerToken, mutationMode, position, original, values, blocksList, loadedDigest);
        mutation.receipt = saveState;
        if (palettePath != null && loadedPaletteDigest != null) mutation.guardPalette(new File(palettePath).toPath(), loadedPaletteDigest);
        binding.save.setEnabled(false);
        BlocksManager.saveBlockDetails(path, new ArrayList<>(), 0, mutation);
        observeSave();
    }

    private void setSelectedBlockType(String rawType) {
        selectedBlockType = BlockTypeUtils.normalizeStoredBlockType(this, rawType);
        binding.type.setText(BlockTypeUtils.getDisplayBlockTypeLabel(this, selectedBlockType));
        updateSpec2Visibility();
        updateBlockSpec(selectedBlockType, Helper.getText(binding.colour));
    }

    private void updateSpec2Visibility() {
        AutoTransition transition = new AutoTransition();
        transition.setDuration(300L);
        TransitionManager.beginDelayedTransition(binding.scrollView, transition);
        binding.spec2Layout.setVisibility("e".equals(selectedBlockType) ? View.VISIBLE : View.GONE);
    }

    private void updateBlockSpec(String specId, String color) {
        binding.blockArea.removeAllViews();
        var blockType = BlockTypeUtils.normalizeStoredBlockType(this, specId);
        try {
            var block = new BlockView(this, -1, Helper.getText(binding.spec), blockType, Helper.getText(binding.name));
            block.blockColor = PropertiesUtil.isHexColor(color) ? PropertiesUtil.parseColor(color)
                    : PropertiesUtil.isHexColor(palletColour) ? PropertiesUtil.parseColor(palletColour)
                    : 0xff8a55d7;
            binding.blockArea.addView(block);
        } catch (Exception e) {
            var block = new TextView(this);
            block.setTextColor(Color.RED);
            var input = Helper.getText(binding.spec);
            int position = BlockSpecParser.findInvalidMenuParameterEnd(input);
            if (position >= 0) {
                // Show a readable error for a malformed menu placeholder.
                block.setText(Helper.getResString(R.string.block_parameter_syntax_error, position));
            } else {
                block.setText(e.toString());
            }
            binding.blockArea.addView(block);
        }
    }

    private void addBlock() {
        HashMap<String, Object> tempMap = new HashMap<>();
        tempMap.put("name", Helper.getText(binding.name));
        tempMap.put("type", selectedBlockType);
        tempMap.put("typeName", Helper.getText(binding.typename));
        tempMap.put("spec", Helper.getText(binding.spec));
        tempMap.put("color", Helper.getText(binding.colour));
        if ("e".equals(selectedBlockType)) {
            tempMap.put("spec2", Helper.getText(binding.spec2));
        }
        if (!TextUtils.isEmpty(Helper.getText(binding.customImport))) {
            tempMap.put("imports", Helper.getText(binding.customImport));
        }
        tempMap.put("code", Helper.getText(binding.code));
        tempMap.put("palette", String.valueOf(blockPosition));
        saveMutation("add", blockPosition, tempMap);
    }

    private void insertBlockAt(int position) {
        HashMap<String, Object> tempMap = new HashMap<>();
        tempMap.put("name", Helper.getText(binding.name));
        tempMap.put("type", selectedBlockType);
        tempMap.put("typeName", Helper.getText(binding.typename));
        tempMap.put("spec", Helper.getText(binding.spec));
        tempMap.put("color", Helper.getText(binding.colour));
        if ("e".equals(selectedBlockType)) {
            tempMap.put("spec2", Helper.getText(binding.spec2));
        }
        if (!TextUtils.isEmpty(Helper.getText(binding.customImport))) {
            tempMap.put("imports", Helper.getText(binding.customImport));
        }
        tempMap.put("code", Helper.getText(binding.code));
        tempMap.put("palette", blocksList.get(position).get("palette"));
        saveMutation("insert", position, tempMap);
    }

    private void editBlock(int position) {
        HashMap<String, Object> tempMap = new HashMap<>(blocksList.get(position));
        tempMap.put("name", Helper.getText(binding.name));
        tempMap.put("type", selectedBlockType);
        tempMap.put("typeName", Helper.getText(binding.typename));
        tempMap.put("spec", Helper.getText(binding.spec));
        tempMap.put("color", Helper.getText(binding.colour));
        if ("e".equals(selectedBlockType)) {
            tempMap.put("spec2", Helper.getText(binding.spec2));
        }
        tempMap.put("imports", Helper.getText(binding.customImport));
        tempMap.put("code", Helper.getText(binding.code));
        saveMutation("edit", position, tempMap);
    }
}
