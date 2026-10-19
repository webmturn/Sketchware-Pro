package pro.sketchware.activities.design;

import pro.sketchware.util.Helper;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import pro.sketchware.activities.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.HashMap;

import pro.sketchware.core.project.ProguardHandler;
import pro.sketchware.core.library.ManageLocalLibrary;
import pro.sketchware.activities.editor.code.SrcCodeEditor;
import pro.sketchware.R;
import pro.sketchware.databinding.ManageProguardBinding;

public class ManageProguardActivity extends BaseAppCompatActivity
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private ProguardHandler proguardHandler;

    private ManageProguardBinding binding;

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == binding.lnPgRules.getId()) {
            Intent intent = new Intent(this, SrcCodeEditor.class);
            intent.putExtra("title", "proguard-rules.pro");
            intent.putExtra("content", proguardHandler.getCustomProguardRules());
            startActivity(intent);
        } else if (id == binding.lnPgFm.getId()) {
            fmDialog();
        }
    }

    private void showShrinkerHelp() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.proguard_about)
                .setMessage(getString(R.string.code_shrinker_obfuscate_def) + "\n\n"
                        + getString(R.string.code_shrinker_warning))
                .setPositiveButton(R.string.common_word_ok, null)
                .show();
    }

    private void fmDialog() {
        ManageLocalLibrary mll = new ManageLocalLibrary(getIntent().getStringExtra("sc_id"));

        if (mll.list.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.proguard_select_local_libs)
                    .setMessage(R.string.proguard_no_libraries)
                    .setPositiveButton(R.string.common_word_ok, null)
                    .show();
            return;
        }

        String[] libraries = new String[mll.list.size()];
        boolean[] enabledLibraries = new boolean[mll.list.size()];

        for (int i = 0; i < mll.list.size(); i++) {
            HashMap<String, Object> current = mll.list.get(i);

            Object name = current.get("name");
            if (name instanceof String) {
                libraries[i] = (String) name;
                enabledLibraries[i] = proguardHandler.libIsProguardFMEnabled(libraries[i]);
            } else {
                libraries[i] = Helper.getResString(R.string.library_broken_configuration);
                enabledLibraries[i] = false;
            }
        }

        MaterialAlertDialogBuilder bld = new MaterialAlertDialogBuilder(this);
        bld.setTitle(R.string.proguard_select_local_libs);
        bld.setMultiChoiceItems(
                libraries,
                enabledLibraries,
                (dialog, which, isChecked) -> enabledLibraries[which] = isChecked);
        bld.setPositiveButton(
                R.string.common_word_save,
                (dialog, which) -> {
                    ArrayList<String> finalList = new ArrayList<>();

                    for (int i = 0; i < libraries.length; i++) {
                        if (enabledLibraries[i]) {
                            finalList.add(libraries[i]);
                        }
                    }

                    proguardHandler.setProguardFMLibs(finalList);
                });
        bld.setNegativeButton(R.string.common_word_cancel, null);
        bld.create().show();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == binding.swPgEnabled.getId()) {
            proguardHandler.setProguardEnabled(isChecked);
        } else if (id == binding.swPgDebug.getId()) {
            proguardHandler.setDebugEnabled(isChecked);
        }
        updateControls();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ManageProguardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeLogic();
        initialize();
    }

    private void initialize() {
        binding.swPgEnabled.setOnCheckedChangeListener(this);
        binding.shrinkerMasterRow.setOnClickListener(view -> binding.swPgEnabled.performClick());
        binding.lnPgRules.setOnClickListener(this);
        binding.shrinkerChoice.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                proguardHandler.setR8Enabled(checkedId == R.id.engine_r8);
                updateControls();
            }
        });
        binding.swPgDebug.setOnCheckedChangeListener(this);
        binding.debugRow.setOnClickListener(view -> {
            if (binding.swPgDebug.isEnabled()) binding.swPgDebug.performClick();
        });
        binding.lnPgFm.setOnClickListener(this);
    }

    private void initializeLogic() {
        _initToolbar();
        proguardHandler = new ProguardHandler(getIntent().getStringExtra("sc_id"));
        binding.swPgEnabled.setChecked(proguardHandler.isShrinkingEnabled());
        binding.swPgDebug.setChecked(proguardHandler.isDebugFilesEnabled());
        binding.shrinkerChoice.check(proguardHandler.isR8Enabled() ? R.id.engine_r8 : R.id.engine_proguard);
        updateControls();
    }

    private void updateControls() {
        boolean enabled = binding.swPgEnabled.isChecked();
        boolean r8 = binding.shrinkerChoice.getCheckedButtonId() == R.id.engine_r8;
        binding.shrinkerMasterCard.setCardBackgroundColor(MaterialColors.getColor(binding.getRoot(),
                enabled ? R.attr.colorPrimaryContainer : R.attr.colorSurfaceContainerHigh));
        int titleColor = MaterialColors.getColor(binding.getRoot(),
                enabled ? R.attr.colorOnPrimaryContainer : R.attr.colorOnSurface);
        binding.shrinkerTitle.setTextColor(titleColor);
        binding.shrinkerStatus.setTextColor(enabled ? titleColor
                : MaterialColors.getColor(binding.getRoot(), R.attr.colorOnSurfaceVariant));
        binding.shrinkerStatus.setText(enabled
                ? getString(R.string.proguard_enabled_summary,
                        getString(r8 ? R.string.proguard_engine_r8 : R.string.proguard_engine_proguard))
                : getString(R.string.proguard_disabled_summary));
        binding.engineR8.setEnabled(enabled);
        binding.engineProguard.setEnabled(enabled);
        binding.swPgDebug.setEnabled(enabled && !r8);
        binding.debugRow.setEnabled(enabled && !r8);
        binding.debugTitle.setAlpha(enabled && !r8 ? 1f : 0.6f);
        binding.debugSummary.setText(r8 ? R.string.proguard_debug_r8_summary : R.string.proguard_debug_summary);
        binding.lnPgFm.setEnabled(enabled);
        binding.fullModeTitle.setAlpha(enabled ? 1f : 0.6f);
        binding.fullModeIcon.setAlpha(enabled ? 1f : 0.45f);
        binding.fullModeArrow.setAlpha(enabled ? 1f : 0.45f);
        binding.fullModeSummary.setText(enabled
                ? R.string.proguard_libraries_summary : R.string.proguard_libraries_disabled);
    }

    private void _initToolbar() {
        binding.toolbar.setTitle(R.string.proguard_page_title);
        binding.toolbar.inflateMenu(R.menu.menu_proguard_manager);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_shrinker_help) {
                showShrinkerHelp();
                return true;
            }
            return false;
        });
        binding.toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
    }
}
