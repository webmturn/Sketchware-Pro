package com.besome.sketch.editor.manage.image;


import mod.hey.studios.util.Helper;
import androidx.activity.OnBackPressedCallback;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.besome.sketch.lib.base.BaseAppCompatActivity;

import java.lang.ref.WeakReference;

import pro.sketchware.core.BackgroundTasks;
import pro.sketchware.core.project.ImageCollectionManager;
import pro.sketchware.core.fragments.ImageCollectionFragment;
import pro.sketchware.core.util.SketchToast;
import pro.sketchware.core.TaskHost;
import pro.sketchware.core.util.UIHelper;
import pro.sketchware.core.fragments.ImageListFragment;
import pro.sketchware.R;
import pro.sketchware.databinding.ManageImageBinding;

public class ManageImageActivity extends BaseAppCompatActivity implements ViewPager.OnPageChangeListener {
    private String sc_id;
    private ImageListFragment projectImagesFragment;
    private ImageCollectionFragment collectionImagesFragment;
    private ManageImageBinding binding;

    public static int getImageGridColumnCount(Context context) {
        var displayMetrics = context.getResources().getDisplayMetrics();
        return (int) (displayMetrics.widthPixels / displayMetrics.density) / 100;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void setCurrentPage(int i) {
        binding.viewPager.setCurrentItem(i);
    }

    public ImageCollectionFragment getCollectionFragment() {
        return collectionImagesFragment;
    }

    public ImageListFragment getProjectImagesFragment() {
        return projectImagesFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (projectImagesFragment.isSelecting) {
                    projectImagesFragment.setSelectionMode(false);
                } else if (collectionImagesFragment.isSelecting()) {
                    collectionImagesFragment.unselectAll();
                    binding.layoutBtnImport.setVisibility(View.GONE);
                } else {
                    showLoadingDialog();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> new SaveImagesAsyncTask(ManageImageActivity.this).execute(), 500L);
                }
            }
        });
        binding = ManageImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!super.isStoragePermissionGranted()) {
            finish();
        }

        setSupportActionBar(binding.topAppBar);
        binding.topAppBar.setTitle(R.string.design_actionbar_title_manager_image);
        binding.topAppBar.setNavigationOnClickListener(v -> {
            if (!UIHelper.isClickThrottled()) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
        if (savedInstanceState == null) {
            sc_id = getIntent().getStringExtra("sc_id");
        } else {
            sc_id = savedInstanceState.getString("sc_id");
        }

        if (sc_id == null || sc_id.isEmpty()) {
            finish();
            return;
        }

        binding.viewPager.setAdapter(new PagerAdapter(getSupportFragmentManager()));
        binding.viewPager.setOffscreenPageLimit(2);
        binding.viewPager.addOnPageChangeListener(this);
        binding.tabLayout.setupWithViewPager(binding.viewPager);
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
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPageSelected(int position) {
        binding.layoutBtnGroup.setVisibility(View.GONE);
        binding.layoutBtnImport.setVisibility(View.GONE);

        if (position == 0) {
            binding.fab.animate().translationY(0F).setDuration(200L).start();
            binding.fab.show();
            collectionImagesFragment.unselectAll();
        } else {
            binding.fab.animate().translationY(400F).setDuration(200L).start();
            binding.fab.hide();
            projectImagesFragment.setSelectionMode(false);
        }
    }

    private static class SaveImagesAsyncTask {
        private final WeakReference<ManageImageActivity> activity;

        public SaveImagesAsyncTask(ManageImageActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        public void execute() {
            var activity = this.activity.get();
            if (activity == null) return;
            BackgroundTasks.runSerial(TaskHost.of(activity), "ManageImageActivity$SaveImagesAsyncTask",
                    this::doWork, this::onSuccess, this::onError);
        }

        private void onSuccess() {
            var activity = this.activity.get();
            if (activity == null) return;
            activity.dismissLoadingDialog();
            activity.setResult(Activity.RESULT_OK);
            activity.finish();
            ImageCollectionManager.getInstance().clearCollections();
        }

        private void doWork() {
            var activity = this.activity.get();
            if (activity == null) return;
            activity.projectImagesFragment.saveImages();
        }

        private void onError(Throwable error) {
            var activity = this.activity.get();
            if (activity == null) return;
            activity.dismissLoadingDialog();
            SketchToast.warning(activity, buildErrorMessage(error), 1).show();
        }

        private String buildErrorMessage(Throwable error) {
            String errorMessage = error != null ? error.getMessage() : null;
            if (errorMessage == null || errorMessage.isEmpty()) {
                return Helper.getResString(R.string.common_error_an_error_occurred);
            }
            return Helper.getResString(R.string.common_error_an_error_occurred) + "[" + errorMessage + "]";
        }
    }

    private class PagerAdapter extends FragmentPagerAdapter {
        private final String[] labels;

        public PagerAdapter(FragmentManager manager) {
            super(manager);
            labels = new String[2];
            labels[0] = Helper.getResString(R.string.design_manager_tab_title_this_project);
            labels[1] = Helper.getResString(R.string.design_manager_tab_title_my_collection);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        @NonNull
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            if (position == 0) {
                projectImagesFragment = (ImageListFragment) fragment;
            } else {
                collectionImagesFragment = (ImageCollectionFragment) fragment;
            }
            return fragment;
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            if (position != 0) {
                return new ImageCollectionFragment();
            }
            return new ImageListFragment();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return labels[position];
        }
    }
}
