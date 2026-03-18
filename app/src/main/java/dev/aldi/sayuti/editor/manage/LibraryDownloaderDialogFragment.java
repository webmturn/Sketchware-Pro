package dev.aldi.sayuti.editor.manage;

import static android.net.ConnectivityManager.NetworkCallback;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.createLibraryMap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import org.cosmic.ide.dependency.resolver.api.Artifact;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mod.hey.studios.build.BuildSettings;
import mod.hey.studios.util.Helper;
import mod.jbk.build.BuiltInLibraries;
import mod.pranav.dependency.resolver.DependencyResolver;
import pro.sketchware.R;
import pro.sketchware.databinding.LibraryDownloaderDialogBinding;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class LibraryDownloaderDialogFragment extends BottomSheetDialogFragment {
    private LibraryDownloaderDialogBinding binding;

    private DependencyDownloadAdapter dependencyAdapter;
    private final List<DependencyDownloadItem> downloadItems = new ArrayList<>();
    private ExecutorService downloadExecutor;

    private final Gson gson = new Gson();
    private BuildSettings buildSettings;

    private boolean notAssociatedWithProject;
    private String dependencyName;
    private String localLibFile;
    private OnLibraryDownloadedTask onLibraryDownloadedTask;

    private ConnectivityManager connectivityManager;
    private NetworkCallback networkCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = LibraryDownloaderDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (downloadExecutor != null && !downloadExecutor.isShutdown()) {
            downloadExecutor.shutdownNow();
        }
        unregisterNetworkCallback();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) {
            dismissAllowingStateLoss();
            return;
        }

        dependencyAdapter = new DependencyDownloadAdapter();
        binding.dependenciesRecyclerView.setAdapter(dependencyAdapter);
        binding.dependenciesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        downloadExecutor = Executors.newSingleThreadExecutor();

        notAssociatedWithProject = args.getBoolean("notAssociatedWithProject", false);
        buildSettings = (BuildSettings) args.getSerializable("buildSettings");
        localLibFile = args.getString("localLibFile");
        if (buildSettings == null || (!notAssociatedWithProject && localLibFile == null)) {
            dismissAllowingStateLoss();
            return;
        }

        binding.btnDownload.setOnClickListener(v -> initDownloadFlow());

        connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        registerNetworkCallback();
    }

    private void registerNetworkCallback() {
        networkCallback = new NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                var activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (binding != null) binding.btnDownload.setEnabled(true);
                    });
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                var activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (binding != null) binding.btnDownload.setEnabled(false);
                    });
                }
            }
        };
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
        // Initial check
        binding.btnDownload.setEnabled(isNetworkAvailable());
    }

    private void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    public void setOnLibraryDownloadedTask(OnLibraryDownloadedTask onLibraryDownloadedTask) {
        this.onLibraryDownloadedTask = onLibraryDownloadedTask;
    }

    private void initDownloadFlow() {
        dependencyName = Helper.getText(binding.dependencyInput).trim();
        if (dependencyName.isEmpty()) {
            binding.dependencyInputLayout.setError(Helper.getResString(R.string.error_enter_dependency));
            binding.dependencyInputLayout.setErrorEnabled(true);
            return;
        }

        var parts = dependencyName.split(":");
        if (parts.length != 3) {
            binding.dependencyInputLayout.setError(Helper.getResString(R.string.error_invalid_dependency_format));
            binding.dependencyInputLayout.setErrorEnabled(true);
            return;
        }

        showDownloadConfirmationDialog(parts[0], parts[1], parts[2]);
    }

    private void showDownloadConfirmationDialog(String group, String artifact, String version) {
        boolean skipSubdependencies = binding.cbSkipSubdependencies.isChecked();

        String message;
        if (skipSubdependencies) {
            message = "Are you sure you want to download " + dependencyName;
        } else {
            message = "Are you sure you want to download " + dependencyName + " and its sub-dependencies?";
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.library_download_confirm_title)
                .setMessage(message)
                .setPositiveButton(R.string.common_word_download, (dialog, which) -> startDownloadProcess(group, artifact, version))
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    private void startDownloadProcess(String group, String artifact, String version) {
        if (binding == null) return;
        binding.dependencyInputLayout.setErrorEnabled(false);

        binding.dependencyInfo.setVisibility(View.GONE);
        binding.overallProgress.setVisibility(View.VISIBLE);
        binding.dependenciesRecyclerView.setVisibility(View.VISIBLE);

        setDownloadState(true);

        boolean skipSubDeps = binding.cbSkipSubdependencies.isChecked();
        var resolver = new DependencyResolver(group, artifact, version,
                skipSubDeps, buildSettings);
        var handler = new Handler(Looper.getMainLooper());

        downloadExecutor.execute(() -> {
            BuiltInLibraries.maybeExtractAndroidJar((message, progress) ->
                    handler.post(() -> {
                        if (binding != null) binding.overallProgress.setIndeterminate(true);
                    }));
            BuiltInLibraries.maybeExtractCoreLambdaStubsJar();

            try {
                resolver.resolveDependency(new DependencyResolver.DependencyResolverCallback() {
                    @Override
                    public void onResolving(@NonNull Artifact artifact, @NonNull Artifact dependency) {
                        handler.post(() -> {
                            DependencyDownloadItem item = findOrCreateDependencyItem(dependency);
                            item.setState(DependencyDownloadItem.DownloadState.RESOLVING);
                            dependencyAdapter.updateDependency(item);
                        });
                    }

                    @Override
                    public void onResolutionComplete(@NonNull Artifact dep) {
                        handler.post(() -> updateDependencyState(dep, DependencyDownloadItem.DownloadState.COMPLETED));
                    }

                    @Override
                    public void onArtifactNotFound(@NonNull Artifact dep) {
                        handler.post(() -> {
                            setDownloadState(false);
                            var activity = getActivity();
                            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) SketchwareUtil.showAnErrorOccurredDialog(activity, "Dependency '" + dep + "' not found");
                        });
                    }····························································································································································································································································································································································································································································································································································

                    @Override··························································································································································································
                    public void onSkippingResolution(@NonNull Artifact dep) {
                        handler.post(() -> {
                            DependencyDownloadItem item = findOrCreateDependencyItem(dep);
                            item.setState(DependencyDownloadItem.DownloadState.COMPLETED);
                            dependencyAdapter.updateDependency(item);
                        });
                    }

                    @Override
                    public void onVersionNotFound(@NonNull Artifact dep) {
                        handler.post(() -> {
                            DependencyDownloadItem item = findOrCreateDependencyItem(dep);
                            item.setError(Helper.getResString(R.string.error_version_not_available));
                            dependencyAdapter.updateDependency(item);
                        });
                    }

                    @Override
                    public void onDependenciesNotFound(@NonNull Artifact dep) {
                        handler.post(() -> {
                            DependencyDownloadItem item = findOrCreateDependencyItem(dep);
                            item.setError(Helper.getResString(R.string.error_dependencies_not_found));
                            dependencyAdapter.updateDependency(item);
                        });
                    }

                    @Override
                    public void onInvalidScope(@NonNull Artifact dep, @NonNull String scope) {
                        handler.post(() -> {
                            DependencyDownloadItem item = findOrCreateDependencyItem(dep);
                            item.setError(Helper.getResString(R.string.error_invalid_scope_format, scope));
                            dependencyAdapter.updateDependency(item);
                        });
                    }

                    @Override
                    public void invalidPackaging(@NonNull Artifact dep) {
                        handler.post(() -> {
                            DependencyDownloadItem item = findOrCreateDependencyItem(dep);
                            item.setError(Helper.getResString(R.string.error_invalid_packaging));
                            dependencyAdapter.updateDependency(item);
                        });
                    }

                    @Override
                    public void onDownloadStart(@NonNull Artifact dep) {
                        handler.post(() -> {
                            setDownloadState(true);
                            DependencyDownloadItem item = findOrCreateDependencyItem(dep);
                            item.setState(DependencyDownloadItem.DownloadState.DOWNLOADING);
                            dependencyAdapter.updateDependency(item);
                            updateOverallProgress();
                        });
                    }

                    @Override
                    public void onDownloadEnd(@NonNull Artifact dep) {
                        handler.post(() -> {
                            updateDependencyState(dep, DependencyDownloadItem.DownloadState.COMPLETED);
                            updateOverallProgress();
                        });
                    }

                    @Override
                    public void onDownloadError(@NonNull Artifact dep, @NonNull Throwable e) {
                        handler.post(() -> {
                            DependencyDownloadItem item = findOrCreateDependencyItem(dep);
                            item.setError(e.getMessage());
                            dependencyAdapter.updateDependency(item);
                            setDownloadState(false);
                            if (isStoragePermissionError(e)) {
                                showStorageAccessError();
                            } else {
                                var activity = getActivity();
                                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) SketchwareUtil.showAnErrorOccurredDialog(activity,
                                        "Downloading dependency '" + dep + "' failed: " + Log.getStackTraceString(e));
                            }
                        });
                    }

                    @Override
                    public void unzipping(@NonNull Artifact artifact) {
                        handler.post(() -> updateDependencyState(artifact, DependencyDownloadItem.DownloadState.UNZIPPING));
                    }

                    @Override
                    public void dexing(@NonNull Artifact dep) {
                        handler.post(() -> updateDependencyState(dep, DependencyDownloadItem.DownloadState.DEXING));
                    }

                    @Override
                    public void onResolutionTimeout(@NonNull Artifact dep) {
                        handler.post(() -> SketchwareUtil.toast(
                                "Dependency tree resolution timed out for " + dep.getArtifactId()
                                        + ". Only the main library was downloaded."));
                    }

                    @Override
                    public void dexingFailed(@NonNull Artifact dependency, @NonNull Exception e) {
                        handler.post(() -> {
                            DependencyDownloadItem item = findOrCreateDependencyItem(dependency);
                            item.setError(Helper.getResString(R.string.error_dexing_failed_format, e.getMessage()));
                            dependencyAdapter.updateDependency(item);
                            setDownloadState(false);
                            var activity = getActivity();
                            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) SketchwareUtil.showAnErrorOccurredDialog(activity,
                                    "Dexing dependency '" + dependency + "' failed: " + Log.getStackTraceString(e));
                        });
                    }

                    @Override
                    public void onTaskCompleted(@NonNull List<String> dependencies) {
                        handler.post(() -> {
                            Map<String, String> dependencyCoordinates = buildDependencyCoordinates(dependencies);
                            if (downloadExecutor != null && !downloadExecutor.isShutdown()) {
                                downloadExecutor.execute(() -> finalizeDownloadedLibraries(
                                        dependencies, dependencyCoordinates, handler));
                            } else {
                                new Thread(() -> finalizeDownloadedLibraries(
                                        dependencies, dependencyCoordinates, handler),
                                        "LibraryDownloadFinalizer").start();
                            }
                        });
                    }
                });
            } catch (Throwable e) {
                handler.post(() -> {
                    setDownloadState(false);
                    var activity = getActivity();
                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) SketchwareUtil.showAnErrorOccurredDialog(activity,
                            "Failed to resolve dependency '" + dependencyName + "': " + e.getMessage());
                });
            }
        });
    }

    private DependencyDownloadItem findOrCreateDependencyItem(Artifact artifact) {
        for (DependencyDownloadItem item : downloadItems) {
            if (item.getArtifact().equals(artifact)) {
                return item;
            }
        }
        DependencyDownloadItem newItem = new DependencyDownloadItem(artifact);
        downloadItems.add(newItem);
        dependencyAdapter.addDependency(newItem);
        return newItem;
    }

    private Map<String, String> buildDependencyCoordinates(List<String> dependencyNames) {
        Map<String, String> dependencyCoordinates = new HashMap<>();
        java.util.Set<String> includedLibraries = new java.util.HashSet<>(dependencyNames);

        if (dependencyName != null) {
            String[] parts = dependencyName.split(":");
            if (parts.length == 3) {
                String libraryName = parts[1] + "-v" + parts[2];
                if (includedLibraries.contains(libraryName)) {
                    dependencyCoordinates.put(libraryName, dependencyName);
                }
            }
        }

        for (DependencyDownloadItem item : downloadItems) {
            Artifact artifact = item.getArtifact();
            String libraryName = artifact.getArtifactId() + "-v" + artifact.getVersion();
            if (includedLibraries.contains(libraryName)) {
                dependencyCoordinates.put(
                        libraryName,
                        artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion());
            }
        }

        return dependencyCoordinates;
    }

    private void persistDependencyCoordinates(Map<String, String> dependencyCoordinates) {
        for (Map.Entry<String, String> entry : dependencyCoordinates.entrySet()) {
            File libraryDirectory = resolveLibraryDirectory(entry.getKey());
            if (!libraryDirectory.exists() && !libraryDirectory.mkdirs()) {
                continue;
            }
            FileUtil.writeFile(new File(libraryDirectory, "maven-coordinate").getAbsolutePath(), entry.getValue());
        }
    }

    private File resolveLibraryDirectory(String libraryName) {
        File primaryDirectory = new File(FilePathUtil.getLocalLibsDir(), libraryName);
        if (primaryDirectory.exists()) {
            return primaryDirectory;
        }

        File fallbackDirectory = new File(FilePathUtil.getLocalLibsFallbackDir(), libraryName);
        if (fallbackDirectory.exists()) {
            return fallbackDirectory;
        }

        return primaryDirectory;
    }

    private void finalizeDownloadedLibraries(List<String> dependencies,
                                             Map<String, String> dependencyCoordinates,
                                             Handler handler) {
        try {
            persistDependencyCoordinates(dependencyCoordinates);
            for (String name : dependencies) {
                LocalLibraryImportPackageIndex.rebuildPackages(
                        LocalLibrariesUtil.getLocalLibraryDirectory(name));
            }
            if (!notAssociatedWithProject) {
                var fileContent = FileUtil.readFile(localLibFile);
                ArrayList<HashMap<String, Object>> enabledLibs;
                try {
                    enabledLibs = gson.fromJson(fileContent, Helper.TYPE_MAP_LIST);
                    if (enabledLibs == null) enabledLibs = new ArrayList<>();
                } catch (com.google.gson.JsonSyntaxException e) {
                    enabledLibs = new ArrayList<>();
                }

                // Remove existing entries for these libraries to avoid duplicates on re-download.
                java.util.Set<String> newNames = new java.util.HashSet<>(dependencies);
                enabledLibs.removeIf(m -> newNames.contains(String.valueOf(m.get("name"))));
                for (String name : dependencies) {
                    enabledLibs.add(createLibraryMap(name, dependencyCoordinates.get(name)));
                }
                FileUtil.writeFile(localLibFile, gson.toJson(enabledLibs));
            }

            handler.post(() -> {
                SketchwareUtil.toast(Helper.getResString(R.string.toast_library_downloaded));
                if (getActivity() == null) return;
                dismiss();
                if (onLibraryDownloadedTask != null) onLibraryDownloadedTask.invoke();
            });
        } catch (Throwable e) {
            handler.post(() -> {
                setDownloadState(false);
                String errorMessage = "Failed to finalize downloaded library '" + dependencyName
                        + "': " + e.getMessage();
                var activity = getActivity();
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    SketchwareUtil.showAnErrorOccurredDialog(activity, errorMessage);
                } else {
                    SketchwareUtil.toastError(errorMessage);
                }
            });
        }
    }

    private void updateDependencyState(Artifact artifact, DependencyDownloadItem.DownloadState state) {
        for (DependencyDownloadItem item : downloadItems) {
            if (item.getArtifact().equals(artifact)) {
                item.setState(state);
                dependencyAdapter.updateDependency(item);
                break;
            }
        }
    }

    private void updateOverallProgress() {
        if (binding == null) return;
        int completed = 0;
        for (DependencyDownloadItem item : downloadItems) {
            if (item.isCompleted()) completed++;
        }

        if (!downloadItems.isEmpty()) {
            binding.overallProgress.setIndeterminate(false);
            binding.overallProgress.setProgress((completed * 100) / downloadItems.size());
        }
    }

    private void setDownloadState(boolean downloading) {
        if (binding == null) return;
        if (downloading) {
            binding.btnDownload.setVisibility(View.GONE);
        } else {
            binding.btnDownload.setVisibility(View.VISIBLE);
            binding.btnDownload.setEnabled(true);
        }

        binding.dependencyInput.setEnabled(!downloading);
        binding.cbSkipSubdependencies.setEnabled(!downloading);
        setCancelable(!downloading);

        if (!downloading) {
            binding.dependencyInfo.setVisibility(View.VISIBLE);
            binding.overallProgress.setVisibility(View.GONE);
            binding.dependenciesRecyclerView.setVisibility(View.GONE);
            binding.dependencyInfo.setText(R.string.local_library_manager_dependency_info);

            downloadItems.clear();
            dependencyAdapter.setDependencies(new ArrayList<>());
        }
    }

    private boolean isStoragePermissionError(Throwable e) {
        // Walk the cause chain looking for EPERM / "Operation not permitted"
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && (msg.contains("Operation not permitted") || msg.contains("EPERM"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void showStorageAccessError() {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.common_message_permission_title_storage)
                .setMessage(Helper.getResString(R.string.common_message_permission_storage)
                        + "\n\nPath: " + FilePathUtil.getLocalLibsDir().getAbsolutePath())
                .setPositiveButton(R.string.common_word_settings, (dialog, which) -> {
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                android.net.Uri.parse("package:" + requireContext().getPackageName()));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (Exception e) {
                        // Fallback: open general app settings
                        try {
                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:" + requireContext().getPackageName()));
                            startActivity(intent);
                        } catch (Exception ignored) {
                            Log.e("LibraryDownloader", "Failed to open app settings screen", ignored);
                        }
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    private boolean isNetworkAvailable() {
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    public interface OnLibraryDownloadedTask {
        void invoke();
    }
}
