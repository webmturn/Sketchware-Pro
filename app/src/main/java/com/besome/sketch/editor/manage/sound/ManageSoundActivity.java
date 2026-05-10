package com.besome.sketch.editor.manage.sound;


import mod.hey.studios.util.Helper;
import androidx.activity.OnBackPressedCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.besome.sketch.lib.base.BaseAppCompatActivity;

import java.lang.ref.WeakReference;

import pro.sketchware.core.BackgroundTasks;
import pro.sketchware.core.util.SketchToast;
import pro.sketchware.core.project.SoundCollectionManager;
import pro.sketchware.core.fragments.SoundImportFragment;
import pro.sketchware.core.TaskHost;
import pro.sketchware.core.util.UIHelper;
import pro.sketchware.core.fragments.SoundListFragment;
import pro.sketchware.R;
import pro.sketchware.databinding.ManageSoundBinding;

public class ManageSoundActivity extends BaseAppCompatActivity implements ViewPager.OnPageChangeListener {

    private final int TAB_COUNT = 2;
    public ManageSoundBinding binding;
    public SoundListFragment projectSounds;
    public SoundImportFragment collectionSounds;
    private String sc_id;

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (projectSounds.isSelecting) {
                    projectSounds.setSelecting(false);
                } else if (collectionSounds.isSelecting()) {
                    collectionSounds.resetSelection();
                } else {
                    showLoadingDialog();
                    try {
                        projectSounds.stopPlayback();
                        collectionSounds.stopPlayback();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> new SaveAsyncTask(ManageSoundActivity.this).execute(), 500L);
                    } catch (Exception e) {
                        dismissLoadingDialog();
                    }
                }
            }
        });
        if (!super.isStoragePermissionGranted()) {
            finish();
        }

        binding = ManageSoundBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> {
            if (!UIHelper.isClickThrottled()) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
        sc_id = savedInstanceState == null ? getIntent().getStringExtra("sc_id") : savedInstanceState.getString("sc_id");

        if (sc_id == null || sc_id.isEmpty()) {
            finish();
            return;
        }

        binding.viewPager.setAdapter(new PagerAdapter(getSupportFragmentManager()));
        binding.viewPager.setOffscreenPageLimit(TAB_COUNT);
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
        projectSounds.setSelecting(false);
        collectionSounds.resetSelection();
        if (position == 0) {
            binding.fab.show();
            collectionSounds.stopPlayback();
        } else {
            binding.fab.hide();
            projectSounds.stopPlayback();
        }
    }

    private static class SaveAsyncTask {
        private final WeakReference<ManageSoundActivity> activityWeakReference;

        public SaveAsyncTask(ManageSoundActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        public void execute() {
            var activity = activityWeakReference.get();
            if (activity == null) return;
            BackgroundTasks.runSerial(TaskHost.of(activity), "ManageSoundActivity$SaveAsyncTask",
                    this::doWork, this::onSuccess, this::onError);
        }

        private void onSuccess() {
            var activity = activityWeakReference.get();
            if (activity == null) return;
            activity.dismissLoadingDialog();
            activity.setResult(RESULT_OK);
            activity.finish();
            SoundCollectionManager.getInstance().clearCollections();
        }

        private void doWork() {
            var activity = activityWeakReference.get();
            if (activity == null) return;
            activity.projectSounds.saveSounds();
        }

        private void onError(Throwable error) {
            var activity = activityWeakReference.get();
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
        private final String[] titles;

        public PagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            titles = new String[TAB_COUNT];
            titles[0] = Helper.getResString(R.string.design_manager_tab_title_this_project);
            titles[1] = Helper.getResString(R.string.design_manager_tab_title_my_collection);
        }

        @Override
        public int getCount() {
            return TAB_COUNT;
        }

        @Override
        @NonNull
        public Object instantiateItem(@NonNull ViewGroup viewGroup, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(viewGroup, position);
            if (position != 0) {
                collectionSounds = (SoundImportFragment) fragment;
            } else {
                projectSounds = (SoundListFragment) fragment;
            }
            return fragment;
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            return position == 0 ? new SoundListFragment() : new SoundImportFragment();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }
    }
}
