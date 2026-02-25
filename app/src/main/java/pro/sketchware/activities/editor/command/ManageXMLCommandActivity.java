package pro.sketchware.activities.editor.command;

import static pro.sketchware.utility.GsonUtils.getGson;
import static pro.sketchware.utility.SketchwareUtil.getDip;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;

import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pro.sketchware.core.ActivityCodeGenerator;
import pro.sketchware.core.ProjectFileManager;
import pro.sketchware.core.ProjectDataManager;
import pro.sketchware.core.UIHelper;
import pro.sketchware.core.SketchwarePaths;
import pro.sketchware.core.ProjectFilePaths;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.component.Magnifier;
import mod.hey.studios.project.ProjectSettings;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.blocks.CommandBlock;
import mod.jbk.code.CodeEditorColorSchemes;
import mod.jbk.code.CodeEditorLanguages;
import pro.sketchware.R;
import pro.sketchware.activities.editor.command.adapters.XMLCommandAdapter;
import pro.sketchware.databinding.ManageXmlCommandAddBinding;
import pro.sketchware.databinding.ManageXmlCommandBinding;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.ThemeUtils;
import pro.sketchware.utility.UI;

public class ManageXMLCommandActivity extends BaseAppCompatActivity {

    private static final String[] COMMANDS_ACTION = {
            "insert", "add", "replace", "find-replace", "find-replace-first", "find-replace-all"
    };
    private String sc_id;
    private String commandPath;
    private XMLCommandAdapter adapter;
    private ProjectSettings settings;
    private ArrayList<HashMap<String, Object>> commands = new ArrayList<>();
    private ArrayList<String> xmlFiles;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void fetchXMLCommand(Context context, String sc_id) {
        var path = SketchwarePaths.getDataPath(sc_id) + "/command";
        if (FileUtil.isExistFile(path)) {
            return;
        }
        var projectFilePaths = new ProjectFilePaths(context, sc_id);
        var projectLibraryManager = ProjectDataManager.getLibraryManager(sc_id);
        var projectFileManager = ProjectDataManager.getFileManager(sc_id);
        var projectDataManager = ProjectDataManager.getProjectDataManager(sc_id);
        projectFilePaths.initializeMetadata(projectLibraryManager, projectFileManager, projectDataManager);
        CommandBlock.clearTempCommands();
        ArrayList<ProjectFileBean> files = new ArrayList<>(projectFileManager.getActivities());
        files.addAll(new ArrayList<>(projectFileManager.getCustomViews()));
        for (ProjectFileBean file : files) {
            CommandBlock.CBForXml(new ActivityCodeGenerator(projectFilePaths.buildConfig, file, projectDataManager).generateCode(false, sc_id));
        }
        String commandPath = FileUtil.getExternalStorageDir().concat("/.sketchware/temp/commands");
        if (FileUtil.isExistFile(commandPath)) {
            FileUtil.copyFile(commandPath, path);
            CommandBlock.clearTempCommands();
        } else {
            FileUtil.writeFile(path, "[]");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ManageXmlCommandBinding binding = ManageXmlCommandBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(R.string.xml_command_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        UI.addSystemWindowInsetToPadding(binding.list, false, false, false, true);
        binding.toolbar.setNavigationOnClickListener(
                v -> {
                    if (!UIHelper.isClickThrottled()) {
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                });
        binding.fab.setOnClickListener(v -> dialog(false, 0));
        if (savedInstanceState == null) {
            sc_id = getIntent().getStringExtra("sc_id");
        } else {
            sc_id = savedInstanceState.getString("sc_id");
        }
        commandPath = SketchwarePaths.getDataPath(sc_id) + "/command";
        ProjectFileManager projectFile = ProjectDataManager.getFileManager(sc_id);
        xmlFiles = new ArrayList<>(projectFile.getXmlNames());
        xmlFiles.addAll(
                Arrays.asList("strings.xml", "colors.xml", "styles.xml", "AndroidManifest.xml"));
        settings = new ProjectSettings(sc_id);
        adapter = new XMLCommandAdapter();
        adapter.setOnItemClickListener(
                item -> {
                    int position = item.second;
                    PopupMenu popupMenu = new PopupMenu(this, item.first);
                    var menu = popupMenu.getMenu();
                    menu.add(Menu.NONE, 0, Menu.NONE, Helper.getResString(R.string.common_word_edit));
                    menu.add(Menu.NONE, 1, Menu.NONE, Helper.getResString(R.string.common_word_delete));
                    if (position != 0) menu.add(Menu.NONE, 2, Menu.NONE, Helper.getResString(R.string.common_word_move_up));
                    if (position != adapter.getItemCount() - 1)
                        menu.add(Menu.NONE, 3, Menu.NONE, Helper.getResString(R.string.common_word_move_down));

                    popupMenu.setOnMenuItemClickListener(
                            itemMenu -> {
                                if (itemMenu.getItemId() == 0) {
                                    dialog(true, position);
                                } else if (itemMenu.getItemId() == 2) {
                                    if (position > 0) {
                                        Collections.swap(commands, position, position - 1);
                                        save();
                                    }
                                } else if (itemMenu.getItemId() == 3) {
                                    if (position < adapter.getItemCount() - 1) {
                                        Collections.swap(commands, position, position + 1);
                                        save();
                                    }
                                } else {
                                    MaterialAlertDialogBuilder dialog =
                                            new MaterialAlertDialogBuilder(this);
                                    dialog.setTitle(R.string.common_word_delete);
                                    dialog.setMessage(R.string.common_word_delete_confirm);
                                    dialog.setPositiveButton(
                                            R.string.common_word_yes,
                                            (d, w) -> {
                                                if (position != -1) {
                                                    commands.remove(position);
                                                    FileUtil.writeFile(
                                                            commandPath,
                                                            getGson().toJson(commands));
                                                    adapter.notifyDataSetChanged();
                                                }
                                            });
                                    dialog.setNegativeButton(R.string.common_word_no, null);
                                    dialog.show();
                                }
                                return true;
                            });

                    popupMenu.show();
                });
        binding.list.setAdapter(adapter);
        fetchCommand();
        generateCommand();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, Menu.NONE, R.string.design_actionbar_title_show_source_code)
                .setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_mtrl_code))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case 0 -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.xml_command_select_xml)
                        .setAdapter(
                                new ArrayAdapter<>(
                                        this, android.R.layout.simple_list_item_1, xmlFiles),
                                (d, w) -> showSourceCode(xmlFiles.get(w)))
                        .show();
                return true;
            }
            default -> {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    private void save() {
        FileUtil.writeFile(commandPath, getGson().toJson(commands));
        adapter.submitList(null);
        adapter.submitList(commands);
    }

    private void dialog(boolean edit, int position) {
        var dialog = new BottomSheetDialog(this);
        ManageXmlCommandAddBinding binding =
                ManageXmlCommandAddBinding.inflate(getLayoutInflater());
        dialog.setContentView(binding.getRoot());
        dialog.setOnShowListener(
                d -> {
                    BottomSheetDialog bsd = (BottomSheetDialog) d;
                    View parent =
                            bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                    if (parent != null) {
                        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        behavior.setSkipCollapsed(true);

                        ViewGroup.LayoutParams layoutParams = parent.getLayoutParams();
                        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                        parent.setLayoutParams(layoutParams);
                    }
                });
        dialog.show();
        binding.title.setText(!edit ? Helper.getResString(R.string.xml_cmd_add_new_command) : Helper.getResString(R.string.xml_cmd_edit_command));

        if (edit) {
            var command = commands.get(position);
            binding.xmlName.setText(
                    CommandBlock.getInputName(
                            command.get("input") != null ? command.get("input").toString() : ""));
            binding.reference.setText(command.get("reference").toString());
            binding.command.setText(command.get("command").toString());
            binding.distance.setText(getIntValue(command.get("distance").toString()));
            binding.front.setText(getIntValue(command.get("after").toString()));
            binding.backend.setText(getIntValue(command.get("before").toString()));
            binding.changes.setText(
                    CommandBlock.getExceptFirstLine(
                            command.get("input") != null ? command.get("input").toString() : ""));
        }

        binding.xmlName.setAdapter(
                new ArrayAdapter<>(
                        this, android.R.layout.simple_dropdown_item_1line, xmlFiles));

        binding.command.setAdapter(
                new ArrayAdapter<>(
                        this, android.R.layout.simple_dropdown_item_1line, COMMANDS_ACTION));

        binding.positive.setText(R.string.common_word_save);
        binding.positive.setOnClickListener(
                v -> {
                    var xmlName = Helper.getText(binding.xmlName);
                    var reference = Helper.getText(binding.reference);
                    var command = Helper.getText(binding.command);
                    if (TextUtils.isEmpty(xmlName)) {
                        SketchwareUtil.toastError(Helper.getResString(R.string.error_xml_name_required));
                        return;
                    }
                    if (TextUtils.isEmpty(reference)) {
                        SketchwareUtil.toastError(Helper.getResString(R.string.error_reference_required));
                        return;
                    }
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("reference", reference);
                    map.put("distance", Integer.parseInt(Helper.getText(binding.distance)));
                    map.put("after", Integer.parseInt(Helper.getText(binding.front)));
                    map.put("before", Integer.parseInt(Helper.getText(binding.backend)));
                    map.put("command", Helper.getText(binding.command));
                    String inputBuilder = ">" + xmlName + "\n" +
                            Helper.getText(binding.changes);
                    map.put("input", inputBuilder);
                    if (edit) {
                        if (position != -1) {
                            commands.set(position, map);
                        }
                    } else {
                        commands.add(map);
                    }
                    save();
                    dialog.dismiss();
                });
        binding.negative.setOnClickListener(
                v -> {
                    dialog.dismiss();
                });
        binding.xmlName.requestFocus();
    }

    private String getIntValue(String value) {
        try {
            return String.valueOf((int) Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return "0";
        }
    }

    private void showConfirmationDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.xml_command_confirmation_title);
        dialog.setMessage(R.string.xml_command_enable_new_message);
        dialog.setPositiveButton(
                R.string.common_word_yes,
                (d, w) -> {
                    settings.setValue(
                            ProjectSettings.SETTING_NEW_XML_COMMAND,
                            ProjectSettings.SETTING_GENERIC_VALUE_TRUE);
                    generateCommand();
                });
        dialog.setNegativeButton(R.string.common_word_exit, (d, w) -> finish());
        dialog.setCancelable(false);
        dialog.show();
    }

    private void showSourceCode(String filename) {
        showLoadingDialog();
        executorService
                .execute(
                        () -> {
                            String source =
                                    new ProjectFilePaths(getApplicationContext(), sc_id)
                                            .getFileSrc(
                                                    filename,
                                                    ProjectDataManager.getFileManager(sc_id),
                                                    ProjectDataManager.getProjectDataManager(sc_id),
                                                    ProjectDataManager.getLibraryManager(sc_id));

                            var dialogBuilder =
                                    new MaterialAlertDialogBuilder(this)
                                            .setTitle(filename)
                                            .setCancelable(false)
                                            .setPositiveButton(R.string.common_word_dismiss, null);

                            runOnUiThread(
                                    () -> {
                                        if (isFinishing()) return;
                                        dismissLoadingDialog();

                                        CodeEditor editor = new CodeEditor(this);
                                        editor.setTypefaceText(Typeface.MONOSPACE);
                                        editor.setEditable(false);
                                        editor.setTextSize(14);
                                        editor.setText(
                                                !source.isEmpty()
                                                        ? source
                                                        : Helper.getResString(R.string.xml_cmd_generate_failed));
                                        editor.getComponent(Magnifier.class)
                                                .setWithinEditorForcibly(true);

                                        editor.setEditorLanguage(
                                                CodeEditorLanguages.loadTextMateLanguage(
                                                        CodeEditorLanguages.SCOPE_NAME_XML));
                                        if (ThemeUtils.isDarkThemeEnabled(
                                                getApplicationContext())) {
                                            editor.setColorScheme(
                                                    CodeEditorColorSchemes.loadTextMateColorScheme(
                                                            CodeEditorColorSchemes.THEME_DRACULA));
                                        } else {
                                            editor.setColorScheme(
                                                    CodeEditorColorSchemes.loadTextMateColorScheme(
                                                            CodeEditorColorSchemes.THEME_GITHUB));
                                        }

                                        AlertDialog dialog = dialogBuilder.create();
                                        dialog.setView(
                                                editor,
                                                (int) getDip(24),
                                                (int) getDip(20),
                                                (int) getDip(24),
                                                (int) getDip(0));
                                        dialog.show();
                                    });
                        });
    }

    private void fetchCommand() {
        if (FileUtil.isExistFile(commandPath)) {
            try {
                commands =
                        getGson().fromJson(FileUtil.readFile(commandPath), Helper.TYPE_MAP_LIST);
                adapter.submitList(commands);
            } catch (Exception e) {
                Log.e("ManageXMLCommand", "Failed to load XML commands from: " + commandPath, e);
                adapter.submitList(new ArrayList<>()); // Submit empty list on error
            }
        }
    }

    private void generateCommand() {
        var newXMLCommand =
                Boolean.parseBoolean(
                        settings.getValue(
                                ProjectSettings.SETTING_NEW_XML_COMMAND,
                                ProjectSettings.SETTING_GENERIC_VALUE_FALSE));
        if (newXMLCommand) {
            if (!FileUtil.isExistFile(commandPath)) {
                showLoadingDialog();
                executorService
                        .execute(
                                () -> {
                                    fetchXMLCommand(this, sc_id);
                                    runOnUiThread(
                                            () -> {
                                                dismissLoadingDialog();
                                                fetchCommand();
                                            });
                                });
            }
        } else {
            showConfirmationDialog();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}
