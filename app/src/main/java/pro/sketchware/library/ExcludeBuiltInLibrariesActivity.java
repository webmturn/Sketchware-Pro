package pro.sketchware.library;

import androidx.activity.OnBackPressedCallback;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import pro.sketchware.activities.base.BaseAppCompatActivity;
import pro.sketchware.util.library.ExcludeBuiltInLibrariesConfig;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.MaterialFadeThrough;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.util.Helper;
import pro.sketchware.util.library.BuiltInLibraries;
import pro.sketchware.util.LogUtil;
import pro.sketchware.R;
import pro.sketchware.databinding.DialogSelectLibrariesBinding;
import pro.sketchware.databinding.ManageLibraryExcludeBuiltinLibrariesBinding;
import pro.sketchware.util.SketchwareUtil;

public class ExcludeBuiltInLibrariesActivity extends BaseAppCompatActivity {
    private static final String TAG = "ExcludeBuiltInLibraries";

    private ManageLibraryExcludeBuiltinLibrariesBinding binding;
    private String sc_id;
    private boolean isExcludingEnabled;
    private List<BuiltInLibraries.BuiltInLibrary> excludedLibraries;
    private Pair<Boolean, List<BuiltInLibraries.BuiltInLibrary>> config;

    @DrawableRes
    public static int getItemIcon() {
        return R.drawable.ic_mtrl_tune;
    }

    public static String getItemTitle() {
        return Helper.getResString(R.string.library_title_exclude);
    }

    public static String getDefaultItemDescription() {
        return Helper.getResString(R.string.library_default_description);
    }

    public static String getSelectedLibrariesItemDescription() {
        return Helper.getResString(R.string.library_excluded_count);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (config != null && config.first.equals(isExcludingEnabled) && config.second.equals(excludedLibraries)) {
                    finish();
                } else {
                    showLoadingDialog();
                    try {
                        new Handler(Looper.myLooper()).postDelayed(() ->
                                new SaveConfigTask(ExcludeBuiltInLibrariesActivity.this).execute(), 500);
                    } catch (Exception e) {
                        onSaveError(e);
                    }
                }
            }
        });
        if (!isStoragePermissionGranted()) {
            finish();
            return;
        }

        binding = ManageLibraryExcludeBuiltinLibrariesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            sc_id = getIntent().getStringExtra("sc_id");
        } else {
            sc_id = savedInstanceState.getString("sc_id");
        }

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.library_title_exclude);
        binding.toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());

        binding.tvEnable.setText(Helper.getResString(R.string.design_library_settings_title_enabled));

        binding.excludeLibrary.setOnClickListener(v -> showSelectBuiltInLibrariesDialog());
        binding.layoutSwitchCard.setOnClickListener(v -> binding.libSwitch.setChecked(!binding.libSwitch.isChecked()));
        binding.libSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isExcludingEnabled = isChecked;
            refresh();
        });
        config = ExcludeBuiltInLibrariesConfig.readConfig(sc_id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.common_word_reset)
                .setIcon(AppCompatResources.getDrawable(this, R.drawable.history_24px))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        String title = menuItem.getTitle().toString();
        if (title.equals(Helper.getResString(R.string.common_word_reset))) {
            showResetDialog();
        } else {
            return false;
        }
        return false;
    }

    private void onSaveError(Throwable throwable) {
        String errorMessage = "Couldn't save configuration: " + throwable.getMessage();
        LogUtil.e(TAG, errorMessage, throwable);
        onSaveError(errorMessage);
    }

    private void onSaveError(String errorMessage) {
        SketchwareUtil.toastError(errorMessage);
        dismissLoadingDialog();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        outState.putBoolean("isExcludingEnabled", isExcludingEnabled);
        outState.putParcelableArrayList("excludedLibraryNames", new ArrayList<>(excludedLibraries));
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (savedInstanceState == null) {
            isExcludingEnabled = ExcludeBuiltInLibrariesConfig.isExcludingEnabled(sc_id);
            excludedLibraries = ExcludeBuiltInLibrariesConfig.getExcludedLibraries(sc_id);
        } else {
            isExcludingEnabled = savedInstanceState.getBoolean("isExcludingEnabled");
            excludedLibraries = savedInstanceState.getParcelableArrayList("excludedLibraryNames");
        }

        refresh();
    }

    private void refresh() {
        binding.libSwitch.setChecked(isExcludingEnabled);

        if (isExcludingEnabled) {
            binding.excludeLibrary.show();
        } else {
            binding.excludeLibrary.hide();
        }

        String libraries = excludedLibraries.stream()
                .map(BuiltInLibraries.BuiltInLibrary::getName)
                .collect(Collectors.joining(", "));

        libraries = isExcludingEnabled ? libraries : "";

        MaterialFadeThrough transition = new MaterialFadeThrough();
        TransitionManager.beginDelayedTransition(binding.content, transition);

        binding.actualContent.setVisibility(libraries.isEmpty() ? View.GONE : View.VISIBLE);
        binding.noContent.setVisibility(libraries.isEmpty() ? View.VISIBLE : View.GONE);
        binding.itemDesc.setText(libraries);
    }

    private void showResetDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setIcon(R.drawable.rollback_96);
        dialog.setTitle(Helper.getResString(R.string.common_word_reset));
        dialog.setMessage(R.string.library_msg_reset_confirm);
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_reset), (v, which) -> {
            ExcludeBuiltInLibrariesConfig.saveConfig(sc_id, false, Collections.emptyList());
            binding.libSwitch.setChecked(false);
            excludedLibraries = Collections.emptyList();
            refresh();
            v.dismiss();
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }

    private void showSelectBuiltInLibrariesDialog() {
        DialogSelectLibrariesBinding binding = DialogSelectLibrariesBinding.inflate(getLayoutInflater());

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.library_title_select);

        // magic to initialize scrollbars even without android:scrollbars defined in XML
        // https://stackoverflow.com/a/48698300/10929762
        TypedArray typedArray = obtainStyledAttributes(null, new int[0]);
        try {
            //noinspection JavaReflectionMemberAccess
            Method method = View.class.getDeclaredMethod("initializeScrollbars", TypedArray.class);
            method.setAccessible(true);
            method.invoke(binding.recyclerView, typedArray);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            LogUtil.e(TAG, "Couldn't add scrollbars to RecyclerView", e);
        }
        typedArray.recycle();
        binding.recyclerView.setVerticalScrollBarEnabled(true);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        BuiltInLibraryAdapter adapter = new BuiltInLibraryAdapter(excludedLibraries);
        adapter.setHasStableIds(true);
        binding.recyclerView.setAdapter(adapter);

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        dialog.setView(binding.getRoot());
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_save), (v, which) -> {
            excludedLibraries = adapter.getSelectedBuiltInLibraries();
            v.dismiss();
            refresh();
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);

        dialog.show();
    }

    private static class SaveConfigTask {
        private final WeakReference<ExcludeBuiltInLibrariesActivity> activity;

        public SaveConfigTask(ExcludeBuiltInLibrariesActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        public void execute() {
            var act = activity.get();
            if (act == null) return;
            BackgroundTasks.runSerial(TaskHost.of(act), "ExcludeBuiltInLibrariesActivity$SaveConfigTask",
                    this::doWork, this::onSuccess, this::onError);
        }

        private void onSuccess() {
            var act = activity.get();
            if (act == null) return;
            act.dismissLoadingDialog();
            act.setResult(RESULT_OK);
            act.finish();
        }

        private void onError(Throwable error) {
            var act = activity.get();
            if (act == null) return;
            act.onSaveError("Couldn't save configuration: " + buildErrorMessage(error));
        }

        private void doWork() {
            var act = activity.get();
            if (act == null) return;
            ExcludeBuiltInLibrariesConfig.saveConfig(act.sc_id, act.isExcludingEnabled, act.excludedLibraries);
        }

        private String buildErrorMessage(Throwable error) {
            String errorMessage = error != null ? error.getMessage() : null;
            if (errorMessage == null || errorMessage.isEmpty()) {
                return Helper.getResString(R.string.common_error_an_error_occurred);
            }
            return errorMessage;
        }

    }

    private static class BuiltInLibraryAdapter extends RecyclerView.Adapter<BuiltInLibraryAdapter.ViewHolder> {
        private final List<BuiltInLibraries.BuiltInLibrary> libraries;
        private final Map<Integer, Void> checkedIndices;
        private List<BuiltInLibraries.BuiltInLibrary> filteredLibraries;

        public BuiltInLibraryAdapter(List<BuiltInLibraries.BuiltInLibrary> excludedLibraries) {
            libraries = Arrays.asList(BuiltInLibraries.KNOWN_BUILT_IN_LIBRARIES);
            libraries.sort(Comparator.comparing(BuiltInLibraries.BuiltInLibrary::getName, String.CASE_INSENSITIVE_ORDER));
            filteredLibraries = new ArrayList<>(libraries);
            checkedIndices = new HashMap<>();

            for (BuiltInLibraries.BuiltInLibrary excludedLibrary : excludedLibraries) {
                int index = libraries.indexOf(excludedLibrary);
                if (index >= 0) {
                    checkedIndices.put(index, null);
                }
            }
        }

        @Override
        public int getItemCount() {
            return filteredLibraries.size();
        }

        @Override
        public long getItemId(int position) {
            return libraries.indexOf(filteredLibraries.get(position));
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_library_exclude_builtin_libraries_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            BuiltInLibraries.BuiltInLibrary library = filteredLibraries.get(position);
            int originalPosition = libraries.indexOf(library);
            holder.selected.setChecked(checkedIndices.containsKey(originalPosition));
            holder.name.setText(library.getName());
            Optional<String> packageName = library.getPackageName();
            if (packageName.isPresent()) {
                holder.packageName.setVisibility(View.VISIBLE);
                holder.packageName.setText(packageName.get());
            } else {
                holder.packageName.setVisibility(View.GONE);
            }

            View.OnClickListener selectingListener = v -> {
                CheckBox selected = holder.selected;
                if (v.getId() != R.id.chk_select) {
                    selected.setChecked(!selected.isChecked());
                }

                if (selected.isChecked()) {
                    checkedIndices.put(originalPosition, null);
                } else {
                    checkedIndices.remove(originalPosition);
                }
            };
            holder.selected.setOnClickListener(selectingListener);
            holder.selectableItem.setOnClickListener(selectingListener);
        }

        public List<BuiltInLibraries.BuiltInLibrary> getSelectedBuiltInLibraries() {
            Set<Integer> checkedIndicesKeySet = checkedIndices.keySet();
            List<BuiltInLibraries.BuiltInLibrary> selectedLibraries = new ArrayList<>(checkedIndicesKeySet.size());
            for (int i : checkedIndicesKeySet) {
                selectedLibraries.add(libraries.get(i));
            }
            return selectedLibraries;
        }

        public void filter(String query) {
            filteredLibraries = new ArrayList<>();
            for (BuiltInLibraries.BuiltInLibrary library : libraries) {
                if (library.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredLibraries.add(library);
                }
            }
            notifyDataSetChanged();
        }

        private static class ViewHolder extends RecyclerView.ViewHolder {
            public final LinearLayout selectableItem;
            public final CheckBox selected;
            public final TextView name;
            public final TextView packageName;

            public ViewHolder(View itemView) {
                super(itemView);
                selectableItem = itemView.findViewById(R.id.view_item);
                selected = itemView.findViewById(R.id.chk_select);
                name = itemView.findViewById(R.id.tv_screen_name);
                packageName = itemView.findViewById(R.id.tv_activity_name);
            }
        }
    }
}
