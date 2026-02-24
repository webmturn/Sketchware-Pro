package com.besome.sketch.editor.manage.view;

import static pro.sketchware.utility.SketchwareUtil.dpToPx;

import android.content.Intent;
import android.util.Log;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.besome.sketch.beans.EventBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import pro.sketchware.core.SketchwareException;
import pro.sketchware.core.ViewFilesFragment;
import pro.sketchware.core.BaseAsyncTask;
import pro.sketchware.core.SketchToast;
import pro.sketchware.core.ProjectDataStore;
import pro.sketchware.core.ProjectDataManager;
import pro.sketchware.core.UIHelper;
import pro.sketchware.core.SketchwarePaths;
import pro.sketchware.core.ViewFilesAdapter;
import pro.sketchware.R;

public class ManageViewActivity extends BaseAppCompatActivity implements OnClickListener, ViewPager.OnPageChangeListener {
    private static final int TAB_COUNT = 2;
    private static final int REQUEST_CODE_ADD_ACTIVITY = 264;
    private static final int REQUEST_CODE_ADD_CUSTOM_VIEW = 266;
    private boolean launchedForActivity = false;

    private ActivityResultLauncher<Intent> addViewLauncher;

    private final int[] x = new int[19];
    // signature mustn't be changed: used in La/a/a/Bw;->a(Landroidx/recyclerview/widget/RecyclerView;II)V, La/a/a/tw;->a(Landroidx/recyclerview/widget/RecyclerView;II)V
    public FloatingActionButton s;
    private MaterialCardView actionButtonsContainer;
    private boolean selecting = false;
    private String isAppCompatEnabled = "N";
    private ViewFilesFragment activitiesFragment;
    private ViewFilesAdapter customViewsFragment;
    private ViewPager viewPager;
    private String sc_id;

    public final String a(int var1, String var2) {
        String var3 = SketchwarePaths.getWidgetTypeName(var1);
        StringBuilder var4 = new StringBuilder();
        var4.append(var3);
        int[] var5 = x;
        int var6 = var5[var1] + 1;
        var5[var1] = var6;
        var4.append(var6);
        String var9 = var4.toString();
        ArrayList<ViewBean> var12 = ProjectDataManager.getProjectDataManager(sc_id).getViews(var2);
        var2 = var9;

        while (true) {
            boolean var7 = false;
            Iterator<ViewBean> var10 = var12.iterator();

            boolean var13;
            while (true) {
                var13 = var7;
                if (!var10.hasNext()) {
                    break;
                }

                if (var2.equals(var10.next().id)) {
                    var13 = true;
                    break;
                }
            }

            if (!var13) {
                return var2;
            }

            StringBuilder var8 = new StringBuilder();
            var8.append(var3);
            int[] var11 = x;
            var6 = var11[var1] + 1;
            var11[var1] = var6;
            var8.append(var6);
            var2 = var8.toString();
        }
    }

    @Override
    public void onPageScrollStateChanged(int var1) {
    }

    @Override
    public void onPageScrolled(int var1, float var2, int var3) {
        a(false);
    }

    public final void a(ProjectFileBean var1, ArrayList<ViewBean> var2) {
        ProjectDataManager.getProjectDataManager(sc_id);
        for (ViewBean viewBean : ProjectDataStore.getSortedRootViews(var2)) {
            viewBean.id = a(viewBean.type, var1.getXmlName());
            ProjectDataManager.getProjectDataManager(sc_id).addView(var1.getXmlName(), viewBean);
            if (viewBean.type == ViewBean.VIEW_TYPE_WIDGET_BUTTON && var1.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY) {
                ProjectDataManager.getProjectDataManager(sc_id).addEvent(var1.getJavaName(), EventBean.EVENT_TYPE_VIEW, viewBean.type, viewBean.id, "onClick");
            }
        }
    }

    // signature mustn't be changed: used in La/a/a/Dw;->onLongClick(Landroid/view/View;)Z, La/a/a/vw;->onLongClick(Landroid/view/View;)Z
    public void a(boolean var1) {
        selecting = var1;
        invalidateOptionsMenu();

        if (selecting) {
            actionButtonsContainer.setVisibility(View.VISIBLE);
            actionButtonsContainer.post(() -> {
                float offset = dpToPx(actionButtonsContainer.getHeight());
                s.animate().translationY(offset).setDuration(200L).start();
            });
        } else {
            actionButtonsContainer.setVisibility(View.GONE);
            s.animate().translationY(0.0F).setDuration(200L).start();
        }

        activitiesFragment.setSelectionMode(selecting);
        customViewsFragment.setSelectionMode(selecting);
    }

    @Override
    public void onPageSelected(int var1) {
        s.show();
    }

    // signature mustn't be changed: used in La/a/a/ViewFilesFragment;->b(Lcom/besome/sketch/beans/ProjectFileBean;)V
    public void b(String var1) {
        customViewsFragment.addCustomView(var1);
        customViewsFragment.updateEmptyState();
    }

    // signature mustn't be changed: used in La/a/a/ViewFilesFragment;->b(Lcom/besome/sketch/beans/ProjectFileBean;)V, La/a/a/ViewFilesFragment;->f()V
    public void c(String var1) {
        customViewsFragment.removeCustomView(var1);
        customViewsFragment.updateEmptyState();
    }

    public ArrayList<String> getProjectLayoutFiles() {
        ArrayList<String> projectLayoutFiles = new ArrayList<>();
        projectLayoutFiles.add("debug");
        ArrayList<ProjectFileBean> activitiesFiles = activitiesFragment.getActivitiesFiles();
        ArrayList<ProjectFileBean> customViewsFiles = customViewsFragment.getProjectFiles();

        for (ProjectFileBean projectFileBean : activitiesFiles) {
            projectLayoutFiles.add(projectFileBean.fileName);
        }

        for (ProjectFileBean projectFileBean : customViewsFiles) {
            projectLayoutFiles.add(projectFileBean.fileName);
        }

        return projectLayoutFiles;
    }

    public final void saveAndSyncFiles() {
        ProjectDataManager.getFileManager(sc_id).setActivities(activitiesFragment.getActivitiesFiles());
        ProjectDataManager.getFileManager(sc_id).setCustomViews(customViewsFragment.getProjectFiles());
        ProjectDataManager.getFileManager(sc_id).saveToBackup();
        ProjectDataManager.getFileManager(sc_id).refreshNameLists();
        ProjectDataManager.getProjectDataManager(sc_id).syncWithFileManager(ProjectDataManager.getFileManager(sc_id));
    }


    @Override
    public void onClick(View v) {
        if (!UIHelper.isClickThrottled()) {
            int viewId = v.getId();
            if (viewId == R.id.btn_cancel) {
                if (selecting) {
                    a(false);
                }
            } else if (viewId == R.id.btn_delete) {
                if (selecting) {
                    activitiesFragment.removeSelectedFiles();
                    customViewsFragment.removeSelectedFiles();
                    a(false);
                    activitiesFragment.updateGuideVisibility();
                    customViewsFragment.updateEmptyState();
                    SketchToast.toast(getApplicationContext(), getString(R.string.common_message_complete_delete), SketchToast.TOAST_WARNING).show();
                    s.show();
                }
            } else if (viewId == R.id.fab) {
                a(false);

                boolean isActivitiesTab = viewPager.getCurrentItem() == 0;
                Intent intent = new Intent(this, isActivitiesTab ? AddViewActivity.class : AddCustomViewActivity.class);
                intent.putStringArrayListExtra("screen_names", getProjectLayoutFiles());
                if (isActivitiesTab) {
                    intent.putExtra("request_code", REQUEST_CODE_ADD_ACTIVITY);
                }
                launchedForActivity = isActivitiesTab;
                addViewLauncher.launch(intent);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (selecting) {
                    a(false);
                } else {
                    showLoadingDialog();
                    try {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> new SaveViewAsyncTask(ManageViewActivity.this).execute(), 500L);
                    } catch (Exception e) {
                        Log.e("ManageViewActivity", e.getMessage(), e);
                        dismissLoadingDialog();
                    }
                }
            }
        });
        addViewLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        ProjectFileBean projectFileBean;
                        if (launchedForActivity) {
                            projectFileBean = data.getParcelableExtra("project_file");
                            if (projectFileBean == null) return;
                            activitiesFragment.addProjectFile(projectFileBean);
                            if (projectFileBean.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)) {
                                b(projectFileBean.getDrawerName());
                            }
                            if (projectFileBean.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER) || projectFileBean.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB)) {
                                ProjectDataManager.getLibraryManager(sc_id).getCompat().useYn = "Y";
                            }
                            if (data.hasExtra("preset_views")) {
                                a(projectFileBean, data.getParcelableArrayListExtra("preset_views"));
                            }
                        } else {
                            projectFileBean = data.getParcelableExtra("project_file");
                            if (projectFileBean == null) return;
                            customViewsFragment.addProjectFile(projectFileBean);
                            customViewsFragment.updateEmptyState();
                            if (data.hasExtra("preset_views")) {
                                a(projectFileBean, data.getParcelableArrayListExtra("preset_views"));
                            }
                        }
                    }
                });
        if (!super.isStoragePermissionGranted()) {
            finish();
        }

        setContentView(R.layout.manage_view);
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        actionButtonsContainer = findViewById(R.id.layout_btn_group);
        Button delete = findViewById(R.id.btn_delete);
        Button cancel = findViewById(R.id.btn_cancel);
        delete.setText(R.string.common_word_delete);
        cancel.setText(R.string.common_word_cancel);
        delete.setOnClickListener(this);
        cancel.setOnClickListener(this);

        if (savedInstanceState == null) {
            sc_id = getIntent().getStringExtra("sc_id");
            isAppCompatEnabled = getIntent().getStringExtra("compatUseYn");
        } else {
            sc_id = savedInstanceState.getString("sc_id");
            isAppCompatEnabled = savedInstanceState.getString("compatUseYn");
        }

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new ManageViewActivity.b(getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(TAB_COUNT);
        viewPager.addOnPageChangeListener(this);
        tabLayout.setupWithViewPager(viewPager);
        s = findViewById(R.id.fab);
        s.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manage_screen_menu, menu);
        menu.findItem(R.id.menu_screen_delete).setVisible(!selecting);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_screen_delete) {
            a(!selecting);
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!super.isStoragePermissionGranted()) {
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle newState) {
        newState.putString("sc_id", sc_id);
        newState.putString("compatUseYn", isAppCompatEnabled);
        super.onSaveInstanceState(newState);
    }

    private static class SaveViewAsyncTask extends BaseAsyncTask {
        private final WeakReference<ManageViewActivity> activity;

        public SaveViewAsyncTask(ManageViewActivity activity) {
            super(activity.getApplicationContext());
            this.activity = new WeakReference<>(activity);
            activity.addTask(this);
        }

        @Override
        public void onSuccess() {
            var activity = this.activity.get();
            if (activity == null) return;
            activity.dismissLoadingDialog();
            activity.setResult(RESULT_OK);
            activity.finish();
        }

        @Override
        public void onError(String var1) {
            var activity = this.activity.get();
            if (activity == null) return;
            activity.dismissLoadingDialog();
        }

        @Override
        public void doWork() throws SketchwareException {
            var activity = this.activity.get();
            if (activity == null) return;
            try {
                publishProgress(activity.getString(R.string.common_message_progress));
                activity.saveAndSyncFiles();
            } catch (Exception e) {
                Log.e("ManageViewActivity", e.getMessage(), e);
                throw new SketchwareException(activity.getString(R.string.common_error_unknown));
            }
        }

    }

    public class b extends FragmentPagerAdapter {
        public String[] f;

        public b(FragmentManager fragmentManager) {
            super(fragmentManager);
            f = new String[]{getString(R.string.common_word_view), getString(R.string.common_word_custom_view)};
        }

        @Override
        public int getCount() {
            return TAB_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return f[position];
        }

        @Override
        @NonNull
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Fragment var3 = (Fragment) super.instantiateItem(container, position);
            if (position != 0) {
                customViewsFragment = (ViewFilesAdapter) var3;
            } else {
                activitiesFragment = (ViewFilesFragment) var3;
            }

            return var3;
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            return position != 0 ? new ViewFilesAdapter() : new ViewFilesFragment();
        }
    }
}
