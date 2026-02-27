package com.besome.sketch.editor.manage.library;


import android.util.Log;
import static android.text.TextUtils.isEmpty;

import android.app.Activity;
import android.content.Intent;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.editor.manage.library.admob.AdmobActivity;
import com.besome.sketch.editor.manage.library.admob.ManageAdmobActivity;
import com.besome.sketch.editor.manage.library.compat.ManageCompatActivity;
import com.besome.sketch.editor.manage.library.firebase.ManageFirebaseActivity;
import com.besome.sketch.editor.manage.library.googlemap.ManageGoogleMapActivity;
import com.besome.sketch.editor.manage.library.material3.Material3LibraryActivity;
import com.besome.sketch.editor.manage.library.material3.Material3LibraryItemView;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import pro.sketchware.core.BaseAsyncTask;
import pro.sketchware.core.ProjectDataManager;
import pro.sketchware.core.UIHelper;
import dev.aldi.sayuti.editor.manage.ManageLocalLibraryActivity;
import mod.hey.studios.activity.managers.nativelib.ManageNativelibsActivity;
import mod.hey.studios.util.Helper;
import mod.jbk.editor.manage.library.ExcludeBuiltInLibrariesActivity;
import mod.jbk.editor.manage.library.ExcludeBuiltInLibrariesLibraryItemView;
import pro.sketchware.R;
import pro.sketchware.utility.UI;

public class ManageLibraryActivity extends BaseAppCompatActivity implements View.OnClickListener {

    private ActivityResultLauncher<Intent> compatLauncher;
    private ActivityResultLauncher<Intent> firebaseLauncher;
    private ActivityResultLauncher<Intent> admobLauncher;
    private ActivityResultLauncher<Intent> googleMapLauncher;
    private ActivityResultLauncher<Intent> material3Launcher;
    private ActivityResultLauncher<Intent> customLibLauncher;

    private String sc_id;
    private LinearLayout libraryItemLayout;

    private ProjectLibraryBean firebaseLibraryBean;
    private ProjectLibraryBean compatLibraryBean;
    private ProjectLibraryBean admobLibraryBean;
    private ProjectLibraryBean googleMapLibraryBean;

    private String originalFirebaseUseYn = "N";
    private String originalCompatUseYn = "N";
    private String originalAdmobUseYn = "N";
    private String originalGoogleMapUseYn = "N";

    private final List<LibraryItemView> libraryItems = new ArrayList<>();

    private LibraryCategoryView addCategoryItem(String text) {
        LibraryCategoryView libraryCategoryView = new LibraryCategoryView(this);
        libraryCategoryView.setTitle(text);
        libraryItemLayout.addView(libraryCategoryView);
        return libraryCategoryView;
    }

    private void addLibraryItem(@Nullable ProjectLibraryBean libraryBean, LibraryCategoryView parent) {
        addLibraryItem(libraryBean, parent, true);
    }

    private void addLibraryItem(@Nullable ProjectLibraryBean libraryBean, LibraryCategoryView parent, boolean addDivider) {
        LibraryItemView libraryItemView;
        libraryItemView = new LibraryItemView(this);
        libraryItemView.setTag(libraryBean != null ? libraryBean.libType : null);
        //noinspection ConstantConditions since the variant if it's nullable handles nulls correctly
        libraryItemView.setData(libraryBean);
        libraryItemView.setOnClickListener(this);

        if (libraryBean.libType == ProjectLibraryBean.PROJECT_LIB_TYPE_LOCAL_LIB || libraryBean.libType == ProjectLibraryBean.PROJECT_LIB_TYPE_NATIVE_LIB) {
            libraryItemView.setHideEnabled();
        }
        parent.addLibraryItem(libraryItemView, addDivider);
        libraryItems.add(libraryItemView);
    }

    private void addCustomLibraryItem(int type, LibraryCategoryView parent) {
        addCustomLibraryItem(type, parent, true);
    }

    private void addCustomLibraryItem(int type, LibraryCategoryView parent, boolean addDivider) {
        LibraryItemView libraryItemView;
        if (type == ProjectLibraryBean.PROJECT_LIB_TYPE_EXCLUDE_BUILTIN_LIBRARIES) {
            libraryItemView = new ExcludeBuiltInLibrariesLibraryItemView(this, sc_id);
            libraryItemView.setData(null);
        } else {
            libraryItemView = new Material3LibraryItemView(this);
            libraryItemView.setData(compatLibraryBean);
        }
        libraryItemView.setTag(type);
        //noinspection ConstantConditions since the variant if it's nullable handles nulls correctly
        libraryItemView.setOnClickListener(this);
        parent.addLibraryItem(libraryItemView, addDivider);
        libraryItems.add(libraryItemView);
    }

    private void toCompatActivity(ProjectLibraryBean compatLibraryBean, ProjectLibraryBean firebaseLibraryBean) {
        Intent intent = new Intent(getApplicationContext(), ManageCompatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("compat", compatLibraryBean);
        intent.putExtra("firebase", firebaseLibraryBean);
        compatLauncher.launch(intent);
    }

    private void initializeLibrary(@Nullable ProjectLibraryBean libraryBean) {
        if (libraryBean != null) {
            switch (libraryBean.libType) {
                case ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE ->
                        firebaseLibraryBean = libraryBean;
                case ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT -> compatLibraryBean = libraryBean;
                case ProjectLibraryBean.PROJECT_LIB_TYPE_ADMOB -> admobLibraryBean = libraryBean;
                case ProjectLibraryBean.PROJECT_LIB_TYPE_GOOGLE_MAP ->
                        googleMapLibraryBean = libraryBean;
            }
        }

        for (LibraryItemView itemView : libraryItems) {
            Object tag = itemView.getTag();
            if (itemView instanceof ExcludeBuiltInLibrariesLibraryItemView) {
                itemView.setData(null);
            } else if (itemView instanceof Material3LibraryItemView) {
                itemView.setData(compatLibraryBean);
            } else if (tag instanceof Integer && libraryBean != null && ((Integer) tag) == libraryBean.libType) {
                itemView.setData(libraryBean);
            }
        }
    }

    private void toAdmobActivity(ProjectLibraryBean libraryBean) {
        Intent intent;
        if (!isEmpty(libraryBean.reserved1) && !isEmpty(libraryBean.appId)) {
            intent = new Intent(getApplicationContext(), ManageAdmobActivity.class);
        } else {
            intent = new Intent(getApplicationContext(), AdmobActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("admob", libraryBean);
        admobLauncher.launch(intent);
    }

    private void toFirebaseActivity(ProjectLibraryBean libraryBean) {
        Intent intent = new Intent(getApplicationContext(), ManageFirebaseActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("firebase", libraryBean);
        firebaseLauncher.launch(intent);
    }

    private void toGoogleMapActivity(ProjectLibraryBean libraryBean) {
        Intent intent = new Intent(getApplicationContext(), ManageGoogleMapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("google_map", libraryBean);
        googleMapLauncher.launch(intent);
    }

    private void launchCustomActivity(Class<? extends Activity> toLaunch) {
        Intent intent = new Intent(getApplicationContext(), toLaunch);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("app_compat", compatLibraryBean);
        customLibLauncher.launch(intent);
    }

    private void toMaterial3Activity() {
        Intent intent = new Intent(getApplicationContext(), Material3LibraryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("compat", compatLibraryBean);
        material3Launcher.launch(intent);
    }

    private void launchActivity(Class<? extends Activity> toLaunch) {
        Intent intent = new Intent(getApplicationContext(), toLaunch);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        startActivity(intent);
    }

    private void saveLibraryConfiguration() {
        ProjectDataManager.getLibraryManager(sc_id).setCompat(compatLibraryBean);
        ProjectDataManager.getLibraryManager(sc_id).setFirebaseDB(firebaseLibraryBean);
        ProjectDataManager.getLibraryManager(sc_id).setAdmob(admobLibraryBean);
        ProjectDataManager.getLibraryManager(sc_id).setGoogleMap(googleMapLibraryBean);
        ProjectDataManager.getLibraryManager(sc_id).saveToBackup();
        ProjectDataManager.getFileManager(sc_id).syncWithLibrary(ProjectDataManager.getLibraryManager(sc_id));
        ProjectDataManager.getProjectDataManager(sc_id).syncWithFileManager(ProjectDataManager.getFileManager(sc_id));
        ProjectDataManager.getProjectDataManager(sc_id).removeFirebaseViews(firebaseLibraryBean, ProjectDataManager.getFileManager(sc_id));
        ProjectDataManager.getProjectDataManager(sc_id).removeAdmobComponents(admobLibraryBean);
        ProjectDataManager.getProjectDataManager(sc_id).removeMapViews(googleMapLibraryBean, ProjectDataManager.getFileManager(sc_id));
    }

    @Override
    public void onClick(View v) {
        if (!UIHelper.isClickThrottled()) {
            Object tag = v.getTag();

            if (tag != null) {
                int vTag = (Integer) tag;
                switch (vTag) {
                    case ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE:
                        toFirebaseActivity(firebaseLibraryBean);
                        break;

                    case ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT:
                        toCompatActivity(compatLibraryBean, firebaseLibraryBean);
                        break;

                    case ProjectLibraryBean.PROJECT_LIB_TYPE_ADMOB:
                        toAdmobActivity(admobLibraryBean);
                        break;

                    case ProjectLibraryBean.PROJECT_LIB_TYPE_GOOGLE_MAP:
                        toGoogleMapActivity(googleMapLibraryBean);
                        break;

                    case ProjectLibraryBean.PROJECT_LIB_TYPE_LOCAL_LIB:
                        launchActivity(ManageLocalLibraryActivity.class);
                        break;

                    case ProjectLibraryBean.PROJECT_LIB_TYPE_NATIVE_LIB:
                        launchActivity(ManageNativelibsActivity.class);
                        break;

                    case ProjectLibraryBean.PROJECT_LIB_TYPE_EXCLUDE_BUILTIN_LIBRARIES:
                        launchCustomActivity(ExcludeBuiltInLibrariesActivity.class);
                        break;

                    case ProjectLibraryBean.PROJECT_LIB_TYPE_MATERIAL3:
                        toMaterial3Activity();
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showLoadingDialog();
                try {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> new SaveLibraryTask(ManageLibraryActivity.this).execute(), 500L);
                } catch (Exception e) {
                    Log.e("ManageLibraryActivity", e.getMessage(), e);
                    dismissLoadingDialog();
                }
            }
        });
        compatLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ProjectLibraryBean compat = result.getData().getParcelableExtra("compat");
                        if (compat != null) initializeLibrary(compat);
                    }
                });
        firebaseLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ProjectLibraryBean libraryBean = result.getData().getParcelableExtra("firebase");
                        if (libraryBean == null) return;
                        initializeLibrary(libraryBean);
                        if (libraryBean.useYn.equals("Y") && !compatLibraryBean.useYn.equals("Y")) {
                            libraryBean = compatLibraryBean;
                            libraryBean.useYn = "Y";
                            initializeLibrary(libraryBean);
                            showFirebaseNeedCompatDialog();
                        }
                    }
                });
        admobLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ProjectLibraryBean admob = result.getData().getParcelableExtra("admob");
                        if (admob != null) initializeLibrary(admob);
                    }
                });
        googleMapLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ProjectLibraryBean googleMap = result.getData().getParcelableExtra("google_map");
                        if (googleMap != null) initializeLibrary(googleMap);
                    }
                });
        material3Launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ProjectLibraryBean compat = result.getData().getParcelableExtra("compat");
                        if (compat != null) initializeLibrary(compat);
                    }
                });
        customLibLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        initializeLibrary(null);
                    }
                });
        if (!super.isStoragePermissionGranted()) {
            finish();
        }

        if (savedInstanceState == null) {
            sc_id = getIntent().getStringExtra("sc_id");
        } else {
            sc_id = savedInstanceState.getString("sc_id");
        }

        setContentView(R.layout.manage_library);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(Helper.getResString(R.string.design_actionbar_title_library));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        libraryItemLayout = findViewById(R.id.contents);

        UI.addSystemWindowInsetToPadding(libraryItemLayout, false, false, false, true);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (savedInstanceState == null) {
            compatLibraryBean = ProjectDataManager.getLibraryManager(sc_id).getCompat();
            if (compatLibraryBean == null) {
                compatLibraryBean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT);
            }
            originalCompatUseYn = compatLibraryBean.useYn;

            firebaseLibraryBean = ProjectDataManager.getLibraryManager(sc_id).getFirebaseDB();
            if (firebaseLibraryBean == null) {
                firebaseLibraryBean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE);
            }
            originalFirebaseUseYn = firebaseLibraryBean.useYn;

            admobLibraryBean = ProjectDataManager.getLibraryManager(sc_id).getAdmob();
            if (admobLibraryBean == null) {
                admobLibraryBean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_ADMOB);
            }
            originalAdmobUseYn = admobLibraryBean.useYn;

            googleMapLibraryBean = ProjectDataManager.getLibraryManager(sc_id).getGoogleMap();
            if (googleMapLibraryBean == null) {
                googleMapLibraryBean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_GOOGLE_MAP);
            }
            originalGoogleMapUseYn = googleMapLibraryBean.useYn;
        } else {
            firebaseLibraryBean = savedInstanceState.getParcelable("firebase");
            originalFirebaseUseYn = savedInstanceState.getString("originalFirebaseUseYn");
            compatLibraryBean = savedInstanceState.getParcelable("compat");
            originalCompatUseYn = savedInstanceState.getString("originalCompatUseYn");
            admobLibraryBean = savedInstanceState.getParcelable("admob");
            originalAdmobUseYn = savedInstanceState.getString("originalAdmobUseYn");
            googleMapLibraryBean = savedInstanceState.getParcelable("google_map");
            originalGoogleMapUseYn = savedInstanceState.getString("originalGoogleMapUseYn");
        }

        LibraryCategoryView basicCategory = addCategoryItem(null);
        addLibraryItem(compatLibraryBean, basicCategory);
        addCustomLibraryItem(ProjectLibraryBean.PROJECT_LIB_TYPE_MATERIAL3, basicCategory);
        addLibraryItem(firebaseLibraryBean, basicCategory);
        addLibraryItem(admobLibraryBean, basicCategory);
        addLibraryItem(googleMapLibraryBean, basicCategory, false);

        LibraryCategoryView externalCategory = addCategoryItem(getString(R.string.design_library_category_external));
        addLibraryItem(new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_LOCAL_LIB), externalCategory);
        addLibraryItem(new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_NATIVE_LIB), externalCategory, false);

        LibraryCategoryView advancedCategory = addCategoryItem(getString(R.string.design_library_category_advanced));
        addCustomLibraryItem(ProjectLibraryBean.PROJECT_LIB_TYPE_EXCLUDE_BUILTIN_LIBRARIES, advancedCategory, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!super.isStoragePermissionGranted()) {
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        outState.putParcelable("firebase", firebaseLibraryBean);
        outState.putParcelable("compat", compatLibraryBean);
        outState.putParcelable("admob", admobLibraryBean);
        outState.putParcelable("google_map", googleMapLibraryBean);
        outState.putString("originalFirebaseUseYn", originalFirebaseUseYn);
        outState.putString("originalCompatUseYn", originalCompatUseYn);
        outState.putString("originalAdmobUseYn", originalAdmobUseYn);
        outState.putString("originalGoogleMapUseYn", originalGoogleMapUseYn);
        super.onSaveInstanceState(outState);
    }

    private void showFirebaseNeedCompatDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setIcon(R.drawable.ic_mtrl_firebase);
        dialog.setTitle(Helper.getResString(R.string.common_word_warning));
        dialog.setMessage(Helper.getResString(R.string.design_library_firebase_message_need_compat));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), null);
        dialog.show();
    }

    private static class SaveLibraryTask extends BaseAsyncTask {

        private final WeakReference<ManageLibraryActivity> activity;

        public SaveLibraryTask(ManageLibraryActivity activity) {
            super(activity);
            this.activity = new WeakReference<>(activity);
            activity.addTask(this);
        }

        @Override
        public void onSuccess() {
            var act = activity.get();
            if (act == null) return;
            act.dismissLoadingDialog();
            Intent intent = new Intent();
            intent.putExtra("sc_id", act.sc_id);
            intent.putExtra("firebase", act.firebaseLibraryBean);
            intent.putExtra("compat", act.compatLibraryBean);
            intent.putExtra("admob", act.admobLibraryBean);
            intent.putExtra("google_map", act.googleMapLibraryBean);
            act.setResult(RESULT_OK, intent);
            act.finish();
        }

        @Override
        public void onError(String idk) {
            var act = activity.get();
            if (act == null) return;
            act.dismissLoadingDialog();
        }

        @Override
        public void doWork() {
            var act = activity.get();
            if (act == null) return;
            try {
                act.saveLibraryConfiguration();
            } catch (Exception e) {
                Log.e("ManageLibraryActivity", e.getMessage(), e);
            }
        }
    }
}
