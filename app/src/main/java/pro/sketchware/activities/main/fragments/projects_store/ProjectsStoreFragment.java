package pro.sketchware.activities.main.fragments.projects_store;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.material.transition.MaterialFadeThrough;

import pro.sketchware.BuildConfig;
import pro.sketchware.activities.main.fragments.projects_store.adapters.StorePagerProjectsAdapter;
import pro.sketchware.activities.main.fragments.projects_store.adapters.StoreProjectsAdapter;
import pro.sketchware.activities.main.fragments.projects_store.api.SketchubAPI;
import pro.sketchware.activities.main.fragments.projects_store.classes.CenterZoomListener;
import pro.sketchware.databinding.FragmentProjectsStoreBinding;
import pro.sketchware.utility.UI;

import java.util.concurrent.atomic.AtomicInteger;

public class ProjectsStoreFragment extends Fragment {
    private FragmentProjectsStoreBinding binding;
    private SketchubAPI sketchubAPI;
    private final AtomicInteger pendingRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialFadeThrough());
        setReturnTransition(new MaterialFadeThrough());
        setExitTransition(new MaterialFadeThrough());
        setReenterTransition(new MaterialFadeThrough());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProjectsStoreBinding.inflate(inflater, container, false);
        sketchubAPI = new SketchubAPI(BuildConfig.SKETCHUB_API_KEY);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView(binding.editorsChoiceProjectsRecyclerView);
        binding.storeRetryButton.setOnClickListener(v -> fetchData());
        fetchData();

        UI.addSystemWindowInsetToPadding(binding.storeLoadingContainer, true, false, true, true);
        UI.addSystemWindowInsetToPadding(binding.storeErrorContainer, true, false, true, true);
        UI.addSystemWindowInsetToPadding(binding.textEditorsChoice, true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.editorsChoiceProjectsRecyclerView, true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.textRecent, true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.recentProjectsRecyclerView, true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.textMostDownloaded, true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.mostDownloadedProjectsRecyclerView, true, false, true, true);
        UI.addSystemWindowInsetToMargin(binding.cardWarning, true, false, true, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // avoid memory leaks
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        recyclerView.addOnScrollListener(new CenterZoomListener());

        recyclerView.setClipToPadding(false);
        recyclerView.setClipChildren(false);

        ViewParent parent = recyclerView.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).setClipChildren(false);
            ((ViewGroup) parent).setClipToPadding(false);
        }
    }

    private void fetchData() {
        if (binding == null) return;
        binding.storeLoadingContainer.setVisibility(View.VISIBLE);
        binding.storeErrorContainer.setVisibility(View.GONE);
        binding.storeContent.setVisibility(View.GONE);

        pendingRequests.set(3);
        failedRequests.set(0);
        var activity = getActivity();

        sketchubAPI.getEditorsChoicerProjects(1, projectModel -> {
            if (projectModel != null && activity != null) {
                activity.runOnUiThread(() -> {
                    if (binding != null) {
                        binding.editorsChoiceProjectsRecyclerView.setAdapter(new StorePagerProjectsAdapter(projectModel.getProjects(), activity));
                    }
                });
            } else {
                failedRequests.incrementAndGet();
            }
            if (pendingRequests.decrementAndGet() == 0 && activity != null && !activity.isFinishing()) {
                activity.runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.storeLoadingContainer.setVisibility(View.GONE);
                    if (failedRequests.get() >= 3) {
                        binding.storeErrorContainer.setVisibility(View.VISIBLE);
                    } else {
                        binding.storeContent.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
        sketchubAPI.getMostDownloadedProjects(1, projectModel -> {
            if (projectModel != null && activity != null) {
                activity.runOnUiThread(() -> {
                    if (binding != null) {
                        binding.mostDownloadedProjectsRecyclerView.setAdapter(new StoreProjectsAdapter(projectModel.getProjects(), activity));
                    }
                });
            } else {
                failedRequests.incrementAndGet();
            }
            if (pendingRequests.decrementAndGet() == 0 && activity != null && !activity.isFinishing()) {
                activity.runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.storeLoadingContainer.setVisibility(View.GONE);
                    if (failedRequests.get() >= 3) {
                        binding.storeErrorContainer.setVisibility(View.VISIBLE);
                    } else {
                        binding.storeContent.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
        sketchubAPI.getRecentProjects(1, projectModel -> {
            if (projectModel != null && activity != null) {
                activity.runOnUiThread(() -> {
                    if (binding != null) {
                        binding.recentProjectsRecyclerView.setAdapter(new StoreProjectsAdapter(projectModel.getProjects(), activity));
                    }
                });
            } else {
                failedRequests.incrementAndGet();
            }
            if (pendingRequests.decrementAndGet() == 0 && activity != null && !activity.isFinishing()) {
                activity.runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.storeLoadingContainer.setVisibility(View.GONE);
                    if (failedRequests.get() >= 3) {
                        binding.storeErrorContainer.setVisibility(View.VISIBLE);
                    } else {
                        binding.storeContent.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }
}
