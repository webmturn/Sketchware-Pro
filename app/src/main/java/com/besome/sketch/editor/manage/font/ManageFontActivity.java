package com.besome.sketch.editor.manage.font;

import androidx.activity.OnBackPressedCallback;
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

import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.project.FontCollectionManager;
import pro.sketchware.core.util.SketchToast;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.core.util.UIHelper;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.ManageFontBinding;

public class ManageFontActivity extends BaseAppCompatActivity {

    public ImportFontFragment projectFontsFragment;
    public FontManagerFragment collectionFontsFragment;
    public ManageFontBinding binding;
    private String sc_id;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (projectFontsFragment.isSelecting) {
                    projectFontsFragment.setSelectingMode(false);
                } else if (collectionFontsFragment.isSelecting()) {
                    collectionFontsFragment.resetSelection();
                } else {
                    showLoadingDialog();
                    try {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> new SaveAsyncTask(ManageFontActivity.this).execute(), 500L);
                    } catch (Exception e) {
                        dismissLoadingDialog();
                    }
                }
            }
        });

        binding = ManageFontBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!isStoragePermissionGranted()) {
            finish();
        }

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

        binding.viewPager.setAdapter(new TabLayoutAdapter(getSupportFragmentManager()));
        binding.viewPager.setOffscreenPageLimit(2);
        binding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                binding.layoutBtnGroup.setVisibility(View.GONE);
                binding.layoutBtnImport.setVisibility(View.GONE);
                collectionFontsFragment.resetSelection();
                projectFontsFragment.setSelectingMode(false);
                changeFabState(position == 0);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        binding.tabLayout.setupWithViewPager(binding.viewPager);
    }

    public void changeFabState(boolean state) {
        if (state) {
            binding.fab.animate().translationY(0F).setDuration(200L).start();
            binding.fab.show();
        } else {
            binding.fab.animate().translationY(400F).setDuration(200L).start();
            binding.fab.hide();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isStoragePermissionGranted()) {
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        super.onSaveInstanceState(outState);
    }

    private static class SaveAsyncTask {
        private final WeakReference<ManageFontActivity> activityWeakReference;

        public SaveAsyncTask(ManageFontActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        public void execute() {
            var activity = activityWeakReference.get();
            if (activity == null) return;
            BackgroundTasks.runSerial(TaskHost.of(activity), "ManageFontActivity$SaveAsyncTask",
                    this::doWork, this::onSuccess, this::onError);
        }

        private void onSuccess() {
            var activity = activityWeakReference.get();
            if (activity == null) return;
            activity.dismissLoadingDialog();
            activity.setResult(RESULT_OK);
            activity.finish();
            FontCollectionManager.getInstance().clearCollections();
        }

        private void doWork() {
            var activity = activityWeakReference.get();
            if (activity == null) return;
            activity.projectFontsFragment.processResources();
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

    private class TabLayoutAdapter extends FragmentPagerAdapter {
        private final String[] labels = new String[2];

        public TabLayoutAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
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
                projectFontsFragment = (ImportFontFragment) fragment;
            } else {
                collectionFontsFragment = (FontManagerFragment) fragment;
            }
            return fragment;
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            return position == 0 ? new ImportFontFragment() : new FontManagerFragment();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return labels[position];
        }
    }
}
