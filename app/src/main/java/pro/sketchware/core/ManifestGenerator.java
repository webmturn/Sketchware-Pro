package pro.sketchware.core;

import static android.text.TextUtils.isEmpty;
import static com.besome.sketch.Config.VAR_DEFAULT_TARGET_SDK_VERSION;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.util.Log;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Pair;

import com.besome.sketch.beans.ProjectFileBean;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import mod.agus.jcoderz.editor.manifest.EditorManifest;
import mod.hey.studios.build.BuildSettings;
import mod.hey.studios.project.ProjectSettings;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.android_manifest.AndroidManifestInjector;
import mod.jbk.build.BuiltInLibraries;
import pro.sketchware.util.library.BuiltInLibraryManager;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileResConfig;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.xml.XmlBuilder;

public class ManifestGenerator {
    private final BuiltInLibraryManager builtInLibraryManager;
    public XmlBuilder manifestXml = new XmlBuilder("manifest");
    public ArrayList<ProjectFileBean> projectFiles;
    public BuildSettings buildSettings;
    public BuildConfig buildConfig;
    public FilePathUtil filePathUtil = new FilePathUtil();
    public FileResConfig fileResConfig;
    public ProjectSettings settings;
    private boolean targetsSdkVersion31OrHigher = false;
    private String packageName;
    private final Set<String> addedPermissions = new HashSet<>();

    public ManifestGenerator(BuildConfig buildConfig, ArrayList<ProjectFileBean> projectFileBeans, BuiltInLibraryManager builtInLibraryManager) {
        this.buildConfig = buildConfig;
        projectFiles = projectFileBeans;
        this.builtInLibraryManager = builtInLibraryManager;
        buildSettings = new BuildSettings(buildConfig.sc_id);
        fileResConfig = new FileResConfig(this.buildConfig.sc_id);
        manifestXml.addAttribute("xmlns", "android", "http://schemas.android.com/apk/res/android");
    }

    /**
     * Adds FileProvider metadata to AndroidManifest.
     *
     * @param applicationTag AndroidManifest {@link XmlBuilder} object
     */
    private void writeFileProvider(XmlBuilder applicationTag) {
        XmlBuilder providerTag = new XmlBuilder("provider");
        providerTag.addAttribute("android", "authorities", buildConfig.packageName + ".provider");
        providerTag.addAttribute("android", "name", "androidx.core.content.FileProvider");
        providerTag.addAttribute("android", "exported", "false");
        providerTag.addAttribute("android", "grantUriPermissions", "true");
        XmlBuilder metadataTag = new XmlBuilder("meta-data");
        metadataTag.addAttribute("android", "name", "android.support.FILE_PROVIDER_PATHS");
        metadataTag.addAttribute("android", "resource", "@xml/provider_paths");
        providerTag.addChildNode(metadataTag);
        applicationTag.addChildNode(providerTag);
    }

    /**
     * Adds a permission to AndroidManifest.
     *
     * @param manifestTag    AndroidManifest {@link XmlBuilder} object
     * @param permissionName The {@code uses-permission} {@link XmlBuilder} tag
     */
    private void writePermission(XmlBuilder manifestTag, String permissionName) {
        if (addedPermissions.contains(permissionName)) {
            return;
        }
        XmlBuilder usesPermissionTag = new XmlBuilder("uses-permission");
        usesPermissionTag.addAttribute("android", "name", permissionName);
        manifestTag.addChildNode(usesPermissionTag);
        addedPermissions.add(permissionName);
    }

    /**
     * Adds Firebase metadata to AndroidManifest.
     *
     * @param applicationTag AndroidManifest {@link XmlBuilder} object
     */
    private void writeFirebaseMetaData(XmlBuilder applicationTag) {
        XmlBuilder providerTag = new XmlBuilder("provider");
        providerTag.addAttribute("android", "name", "com.google.firebase.provider.FirebaseInitProvider");
        providerTag.addAttribute("android", "authorities", buildConfig.packageName + ".firebaseinitprovider");
        providerTag.addAttribute("android", "exported", "false");
        providerTag.addAttribute("android", "initOrder", "100");
        applicationTag.addChildNode(providerTag);
        XmlBuilder serviceTag = new XmlBuilder("service");
        serviceTag.addAttribute("android", "name", "com.google.firebase.components.ComponentDiscoveryService");
        serviceTag.addAttribute("android", "exported", "false");
        if (buildConfig.isFirebaseAuthUsed) {
            XmlBuilder metadataTag = new XmlBuilder("meta-data");
            metadataTag.addAttribute("android", "name", "com.google.firebase.components:com.google.firebase.auth.FirebaseAuthRegistrar");
            metadataTag.addAttribute("android", "value", "com.google.firebase.components.ComponentRegistrar");
            serviceTag.addChildNode(metadataTag);
        }
        if (buildConfig.isFirebaseDatabaseUsed) {
            XmlBuilder metadataTag = new XmlBuilder("meta-data");
            metadataTag.addAttribute("android", "name", "com.google.firebase.components:com.google.firebase.database.DatabaseRegistrar");
            metadataTag.addAttribute("android", "value", "com.google.firebase.components.ComponentRegistrar");
            serviceTag.addChildNode(metadataTag);
        }
        if (buildConfig.isFirebaseStorageUsed) {
            XmlBuilder metadataTag = new XmlBuilder("meta-data");
            metadataTag.addAttribute("android", "name", "com.google.firebase.components:com.google.firebase.storage.StorageRegistrar");
            metadataTag.addAttribute("android", "value", "com.google.firebase.components.ComponentRegistrar");
            serviceTag.addChildNode(metadataTag);
        }
        if (buildConfig.constVarComponent.isFCMUsed) {
            XmlBuilder metadataTag = new XmlBuilder("meta-data");
            metadataTag.addAttribute("android", "name", "com.google.firebase.components:com.google.firebase.iid.Registrar");
            metadataTag.addAttribute("android", "value", "com.google.firebase.components.ComponentRegistrar");
            serviceTag.addChildNode(metadataTag);
        }
        applicationTag.addChildNode(serviceTag);
    }

    /**
     * Adds the Google Maps SDK API key metadata to AndroidManifest.
     *
     * @param applicationTag AndroidManifest {@link XmlBuilder} object
     */
    private void writeGoogleMapMetaData(XmlBuilder applicationTag) {
        XmlBuilder metadataTag = new XmlBuilder("meta-data");
        metadataTag.addAttribute("android", "name", "com.google.android.geo.API_KEY");
        metadataTag.addAttribute("android", "value", "@string/google_maps_key");
        applicationTag.addChildNode(metadataTag);
    }

    /**
     * Specifies in AndroidManifest that the app uses Apache HTTP legacy library.
     *
     * @param applicationTag AndroidManifest {@link XmlBuilder} object
     */
    private void writeLegacyLibrary(XmlBuilder applicationTag) {
        XmlBuilder usesLibraryTag = new XmlBuilder("uses-library");
        usesLibraryTag.addAttribute("android", "name", "org.apache.http.legacy");
        usesLibraryTag.addAttribute("android", "required", "false");
        applicationTag.addChildNode(usesLibraryTag);
    }

    /**
     * Adds metadata about the GMS library version (setNodeValue resource integer).
     *
     * @param applicationTag {@link XmlBuilder} object to add the {@code meta-data} tag to
     */
    private void writeGMSVersion(XmlBuilder applicationTag) {
        XmlBuilder metadataTag = new XmlBuilder("meta-data");
        metadataTag.addAttribute("android", "name", "com.google.android.gms.version");
        metadataTag.addAttribute("android", "value", "@integer/google_play_services_version");
        applicationTag.addChildNode(metadataTag);
    }

    /**
     * Registers a {@link BroadcastReceiver} in AndroidManifest.
     *
     * @param applicationTag AndroidManifest {@link XmlBuilder} object
     * @param receiverName   The component name of the broadcast
     * @see ComponentName
     */
    private void writeBroadcast(XmlBuilder applicationTag, String receiverName) {
        XmlBuilder receiverTag = new XmlBuilder("receiver");
        receiverTag.addAttribute("android", "name", receiverName);
        XmlBuilder intentFilterTag = new XmlBuilder("intent-filter");
        XmlBuilder actionTag = new XmlBuilder("action");
        actionTag.addAttribute("android", "name", receiverName);
        intentFilterTag.addChildNode(actionTag);
        if (targetsSdkVersion31OrHigher) {
            receiverTag.addAttribute("android", "exported", "true");
        }
        receiverTag.addChildNode(intentFilterTag);
        applicationTag.addChildNode(receiverTag);
    }

    private void writeAdmobAppId(XmlBuilder applicationTag) {
        XmlBuilder metadataTag = new XmlBuilder("meta-data");
        metadataTag.addAttribute("android", "name", "com.google.android.gms.ads.APPLICATION_ID");
        metadataTag.addAttribute("android", "value", buildConfig.appId);
        applicationTag.addChildNode(metadataTag);
    }

    /**
     * Registers a {@link Service} in AndroidManifest.
     *
     * @param applicationTag AndroidManifest {@link XmlBuilder} object
     * @param serviceName    The component name of the service
     */
    private void writeService(XmlBuilder applicationTag, String serviceName) {
        XmlBuilder serviceTag = new XmlBuilder("service");
        serviceTag.addAttribute("android", "name", serviceName);
        serviceTag.addAttribute("android", "enabled", "true");
        applicationTag.addChildNode(serviceTag);
    }

    private void writeAndroidxRoomService(XmlBuilder application) {
        XmlBuilder invalidationService = new XmlBuilder("service");
        invalidationService.addAttribute("android", "name", "androidx.room.MultiInstanceInvalidationService");
        invalidationService.addAttribute("android", "directBootAware", "true");
        invalidationService.addAttribute("android", "exported", "false");
        application.addChildNode(invalidationService);
    }

    private void writeAndroidxStartupInitializationProvider(XmlBuilder application) {
        var initializers = Set.of(
                new Pair<>(builtInLibraryManager.containsLibrary(BuiltInLibraries.ANDROIDX_EMOJI2), "androidx.emoji2.text.EmojiCompatInitializer"),
                new Pair<>(builtInLibraryManager.containsLibrary(BuiltInLibraries.ANDROIDX_LIFECYCLE_PROCESS), "androidx.lifecycle.ProcessLifecycleInitializer"),
                new Pair<>(builtInLibraryManager.containsLibrary(BuiltInLibraries.ANDROIDX_WORK_RUNTIME), "androidx.work.WorkManagerInitializer")
        );

        if (initializers.stream().anyMatch(initializer -> initializer.first)) {
            XmlBuilder initializationProvider = new XmlBuilder("provider");
            initializationProvider.addAttribute("android", "name", "androidx.startup.InitializationProvider");
            initializationProvider.addAttribute("android", "authorities", buildConfig.packageName + ".androidx-startup");
            initializationProvider.addAttribute("android", "exported", "false");
            for (var pair : initializers) {
                if (pair.first) {
                    XmlBuilder metadata = new XmlBuilder("meta-data");
                    metadata.addAttribute("android", "name", pair.second);
                    metadata.addAttribute("android", "value", "androidx.startup");
                    initializationProvider.addChildNode(metadata);
                }
            }
            application.addChildNode(initializationProvider);
        }
    }

    private void writeAndroidxWorkRuntimeTags(XmlBuilder application) {
        XmlBuilder alarmService = new XmlBuilder("service");
        alarmService.addAttribute("android", "name", "androidx.work.impl.background.systemalarm.SystemAlarmService");
        alarmService.addAttribute("android", "directBootAware", "false");
        alarmService.addAttribute("android", "enabled", "@bool/enable_system_alarm_service_default");
        alarmService.addAttribute("android", "exported", "false");
        application.addChildNode(alarmService);

        XmlBuilder jobService = new XmlBuilder("service");
        jobService.addAttribute("android", "name", "androidx.work.impl.background.systemjob.SystemJobService");
        jobService.addAttribute("android", "directBootAware", "false");
        jobService.addAttribute("android", "enabled", "@bool/enable_system_job_service_default");
        jobService.addAttribute("android", "exported", "true");
        jobService.addAttribute("android", "permission", "android.permission.BIND_JOB_SERVICE");
        application.addChildNode(jobService);

        XmlBuilder foregroundService = new XmlBuilder("service");
        foregroundService.addAttribute("android", "name", "androidx.work.impl.foreground.SystemForegroundService");
        foregroundService.addAttribute("android", "directBootAware", "false");
        foregroundService.addAttribute("android", "enabled", "@bool/enable_system_foreground_service_default");
        foregroundService.addAttribute("android", "exported", "false");
        application.addChildNode(foregroundService);

        XmlBuilder forceStopRunnableReceiver = new XmlBuilder("receiver");
        forceStopRunnableReceiver.addAttribute("android", "name", "androidx.work.impl.utils.ForceStopRunnable$BroadcastReceiver");
        forceStopRunnableReceiver.addAttribute("android", "directBootAware", "false");
        forceStopRunnableReceiver.addAttribute("android", "enabled", "true");
        forceStopRunnableReceiver.addAttribute("android", "exported", "false");
        application.addChildNode(forceStopRunnableReceiver);

        XmlBuilder batteryChargingReceiver = new XmlBuilder("receiver");
        batteryChargingReceiver.addAttribute("android", "name", "androidx.work.impl.background.systemalarm.ConstraintProxy$BatteryChargingProxy");
        batteryChargingReceiver.addAttribute("android", "directBootAware", "false");
        batteryChargingReceiver.addAttribute("android", "enabled", "false");
        batteryChargingReceiver.addAttribute("android", "exported", "false");
        {
            XmlBuilder intentFilter = new XmlBuilder("intent-filter");
            XmlBuilder connectedAction = new XmlBuilder("action");
            connectedAction.addAttribute("android", "name", "android.intent.action.ACTION_POWER_CONNECTED");
            intentFilter.addChildNode(connectedAction);
            XmlBuilder disconnectedAction = new XmlBuilder("action");
            disconnectedAction.addAttribute("android", "name", "android.intent.action.ACTION_POWER_DISCONNECTED");
            intentFilter.addChildNode(disconnectedAction);
            batteryChargingReceiver.addChildNode(intentFilter);
        }
        application.addChildNode(batteryChargingReceiver);

        XmlBuilder batteryNotLowReceiver = new XmlBuilder("receiver");
        batteryNotLowReceiver.addAttribute("android", "name", "androidx.work.impl.background.systemalarm.ConstraintProxy$BatteryNotLowProxy");
        batteryNotLowReceiver.addAttribute("android", "directBootAware", "false");
        batteryNotLowReceiver.addAttribute("android", "enabled", "false");
        batteryNotLowReceiver.addAttribute("android", "exported", "false");
        {
            XmlBuilder intentFilter = new XmlBuilder("intent-filter");
            XmlBuilder okayAction = new XmlBuilder("action");
            okayAction.addAttribute("android", "name", "android.intent.action.BATTERY_OKAY");
            intentFilter.addChildNode(okayAction);
            XmlBuilder lowAction = new XmlBuilder("action");
            lowAction.addAttribute("android", "name", "android.intent.action.BATTERY_LOW");
            intentFilter.addChildNode(lowAction);
            batteryNotLowReceiver.addChildNode(intentFilter);
        }
        application.addChildNode(batteryNotLowReceiver);

        XmlBuilder storageNotLowReceiver = new XmlBuilder("receiver");
        storageNotLowReceiver.addAttribute("android", "name", "androidx.work.impl.background.systemalarm.ConstraintProxy$StorageNotLowProxy");
        storageNotLowReceiver.addAttribute("android", "directBootAware", "false");
        storageNotLowReceiver.addAttribute("android", "enabled", "false");
        storageNotLowReceiver.addAttribute("android", "exported", "false");
        {
            XmlBuilder intentFilter = new XmlBuilder("intent-filter");
            XmlBuilder lowAction = new XmlBuilder("action");
            lowAction.addAttribute("android", "name", "android.intent.action.DEVICE_STORAGE_LOW");
            intentFilter.addChildNode(lowAction);
            XmlBuilder okAction = new XmlBuilder("action");
            okAction.addAttribute("android", "name", "android.intent.action.DEVICE_STORAGE_OK");
            intentFilter.addChildNode(okAction);
            storageNotLowReceiver.addChildNode(intentFilter);
        }
        application.addChildNode(storageNotLowReceiver);

        XmlBuilder networkStateReceiver = new XmlBuilder("receiver");
        networkStateReceiver.addAttribute("android", "name", "androidx.work.impl.background.systemalarm.ConstraintProxy$NetworkStateProxy");
        networkStateReceiver.addAttribute("android", "directBootAware", "false");
        networkStateReceiver.addAttribute("android", "enabled", "false");
        networkStateReceiver.addAttribute("android", "exported", "false");
        {
            XmlBuilder intentFilter = new XmlBuilder("intent-filter");
            XmlBuilder action = new XmlBuilder("action");
            action.addAttribute("android", "name", "android.net.conn.CONNECTIVITY_CHANGE");
            intentFilter.addChildNode(action);
            networkStateReceiver.addChildNode(intentFilter);
        }
        application.addChildNode(networkStateReceiver);

        XmlBuilder rescheduleReceiver = new XmlBuilder("receiver");
        rescheduleReceiver.addAttribute("android", "name", "androidx.work.impl.background.systemalarm.RescheduleReceiver");
        rescheduleReceiver.addAttribute("android", "directBootAware", "false");
        rescheduleReceiver.addAttribute("android", "enabled", "false");
        rescheduleReceiver.addAttribute("android", "exported", "false");
        {
            XmlBuilder intentFilter = new XmlBuilder("intent-filter");
            XmlBuilder bootCompletedAction = new XmlBuilder("action");
            bootCompletedAction.addAttribute("android", "name", "android.intent.action.BOOT_COMPLETED");
            intentFilter.addChildNode(bootCompletedAction);
            XmlBuilder timeSetAction = new XmlBuilder("action");
            timeSetAction.addAttribute("android", "name", "android.intent.action.TIME_SET");
            intentFilter.addChildNode(timeSetAction);
            XmlBuilder timezoneChangedAction = new XmlBuilder("action");
            timezoneChangedAction.addAttribute("android", "name", "android.intent.action.TIMEZONE_CHANGED");
            intentFilter.addChildNode(timezoneChangedAction);
            rescheduleReceiver.addChildNode(intentFilter);
        }
        application.addChildNode(rescheduleReceiver);

        XmlBuilder proxyUpdateReceiver = new XmlBuilder("receiver");
        proxyUpdateReceiver.addAttribute("android", "name", "androidx.work.impl.background.systemalarm.ConstraintProxyUpdateReceiver");
        proxyUpdateReceiver.addAttribute("android", "directBootAware", "false");
        proxyUpdateReceiver.addAttribute("android", "enabled", "@bool/enable_system_alarm_service_default");
        proxyUpdateReceiver.addAttribute("android", "exported", "false");
        {
            XmlBuilder intentFilter = new XmlBuilder("intent-filter");
            XmlBuilder action = new XmlBuilder("action");
            action.addAttribute("android", "name", "androidx.work.impl.background.systemalarm.UpdateProxies");
            intentFilter.addChildNode(action);
            proxyUpdateReceiver.addChildNode(intentFilter);
        }
        application.addChildNode(proxyUpdateReceiver);

        XmlBuilder diagnosticsReceiver = new XmlBuilder("receiver");
        diagnosticsReceiver.addAttribute("android", "name", "androidx.work.impl.diagnostics.DiagnosticsReceiver");
        diagnosticsReceiver.addAttribute("android", "directBootAware", "false");
        diagnosticsReceiver.addAttribute("android", "enabled", "true");
        diagnosticsReceiver.addAttribute("android", "exported", "true");
        diagnosticsReceiver.addAttribute("android", "permission", "android.permission.DUMP");
        {
            XmlBuilder intentFilter = new XmlBuilder("intent-filter");
            XmlBuilder action = new XmlBuilder("action");
            action.addAttribute("android", "name", "androidx.work.diagnostics.REQUEST_DIAGNOSTICS");
            intentFilter.addChildNode(action);
            diagnosticsReceiver.addChildNode(intentFilter);
        }
        application.addChildNode(diagnosticsReceiver);
    }

    public void setProjectFilePaths(ProjectFilePaths projectFilePaths) {
        settings = new ProjectSettings(projectFilePaths.sc_id);
        targetsSdkVersion31OrHigher = Integer.parseInt(settings.getValue(ProjectSettings.SETTING_TARGET_SDK_VERSION, String.valueOf(VAR_DEFAULT_TARGET_SDK_VERSION))) >= 31;
        packageName = projectFilePaths.packageName;
    }

    /**
     * Builds an AndroidManifest.
     *
     * @return The AndroidManifest as {@link String}
     */
    public String generateManifest() {
        int targetSdkVersion;
        try {
            targetSdkVersion = Integer.parseInt(settings.getValue(ProjectSettings.SETTING_TARGET_SDK_VERSION, String.valueOf(VAR_DEFAULT_TARGET_SDK_VERSION)));
        } catch (NumberFormatException ignored) {
            targetSdkVersion = VAR_DEFAULT_TARGET_SDK_VERSION;
        }
        boolean addRequestLegacyExternalStorage = targetSdkVersion >= 28;

        manifestXml.addAttribute("", "package", buildConfig.packageName);

        if (!buildConfig.hasPermissions()) {
            if (buildConfig.hasPermission(BuildConfig.PERMISSION_CALL_PHONE)) {
                writePermission(manifestXml, Manifest.permission.CALL_PHONE);
            }
            if (buildConfig.hasPermission(BuildConfig.PERMISSION_INTERNET)) {
                writePermission(manifestXml, Manifest.permission.INTERNET);
            }
            if (buildConfig.hasPermission(BuildConfig.PERMISSION_VIBRATE)) {
                writePermission(manifestXml, Manifest.permission.VIBRATE);
            }
            if (buildConfig.hasPermission(BuildConfig.PERMISSION_ACCESS_NETWORK_STATE)) {
                writePermission(manifestXml, Manifest.permission.ACCESS_NETWORK_STATE);
            }
            if (buildConfig.hasPermission(BuildConfig.PERMISSION_CAMERA)) {
                writePermission(manifestXml, Manifest.permission.CAMERA);
            }
            if (buildConfig.hasPermission(BuildConfig.PERMISSION_READ_EXTERNAL_STORAGE)) {
                writePermission(manifestXml, Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (buildConfig.hasPermission(BuildConfig.PERMISSION_WRITE_EXTERNAL_STORAGE)) {
                writePermission(manifestXml, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (buildConfig.hasPermission(BuildConfig.PERMISSION_RECORD_AUDIO)) {
                writePermission(manifestXml, Manifest.permission.RECORD_AUDIO);
            }
            if (buildConfig.hasPermission(BuildConfig.PERMISSION_BLUETOOTH)) {
                writePermission(manifestXml, Manifest.permission.BLUETOOTH);
            }
            if (buildConfig.hasPermission(BuildConfig.PERMISSION_BLUETOOTH_ADMIN)) {
                writePermission(manifestXml, Manifest.permission.BLUETOOTH_ADMIN);
            }
            if (buildConfig.hasPermission(BuildConfig.PERMISSION_ACCESS_FINE_LOCATION)) {
                writePermission(manifestXml, Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        if (FileUtil.isExistFile(filePathUtil.getPathPermission(buildConfig.sc_id))) {
            for (String s : fileResConfig.getPermissionList()) {
                writePermission(manifestXml, s);
            }
        }
        if (buildConfig.isAdMobEnabled) {
            writePermission(manifestXml, "com.google.android.gms.permission.AD_ID");
        }
        if (builtInLibraryManager.containsLibrary(BuiltInLibraries.ANDROIDX_WORK_RUNTIME)) {
            writePermission(manifestXml, "android.permission.WAKE_LOCK");
            writePermission(manifestXml, "android.permission.ACCESS_NETWORK_STATE");
            writePermission(manifestXml, "android.permission.RECEIVE_BOOT_COMPLETED");
            writePermission(manifestXml, "android.permission.FOREGROUND_SERVICE");
        }
        if (buildConfig.constVarComponent.isFCMUsed) {
            writePermission(manifestXml, Manifest.permission.WAKE_LOCK);
            writePermission(manifestXml, "com.google.android.c2dm.permission.RECEIVE");
        }
        AndroidManifestInjector.getP(manifestXml, buildConfig.sc_id);

        if (buildConfig.isAdMobEnabled || buildConfig.isTextToSpeechUsed || buildConfig.isSpeechToTextUsed) {
            XmlBuilder queries = new XmlBuilder("queries");
            if (buildConfig.isAdMobEnabled) {
                XmlBuilder forBrowserContent = new XmlBuilder("intent");
                {
                    XmlBuilder action = new XmlBuilder("action");
                    action.addAttribute("android", "name", "android.intent.action.VIEW");
                    forBrowserContent.addChildNode(action);
                    XmlBuilder category = new XmlBuilder("category");
                    category.addAttribute("android", "name", "android.intent.category.BROWSABLE");
                    forBrowserContent.addChildNode(category);
                    XmlBuilder data = new XmlBuilder("data");
                    data.addAttribute("android", "scheme", "https");
                    forBrowserContent.addChildNode(data);
                }
                queries.addChildNode(forBrowserContent);
                XmlBuilder forCustomTabsService = new XmlBuilder("intent");
                {
                    XmlBuilder action = new XmlBuilder("action");
                    action.addAttribute("android", "name", "android.support.customtabs.action.CustomTabsService");
                    forCustomTabsService.addChildNode(action);
                }
                queries.addChildNode(forCustomTabsService);
            }
            if (buildConfig.isTextToSpeechUsed && targetSdkVersion >= 30) {
                XmlBuilder intent = new XmlBuilder("intent");
                XmlBuilder action = new XmlBuilder("action");
                action.addAttribute("android", "name", "android.intent.action.TTS_SERVICE");
                intent.addChildNode(action);
                queries.addChildNode(intent);
            }
            if (buildConfig.isSpeechToTextUsed && targetSdkVersion >= 30) {
                XmlBuilder intent = new XmlBuilder("intent");
                XmlBuilder action = new XmlBuilder("action");
                action.addAttribute("android", "name", "android.speech.RecognitionService");
                intent.addChildNode(action);
                queries.addChildNode(intent);
            }
            manifestXml.addChildNode(queries);
        }

        XmlBuilder applicationTag = new XmlBuilder("application");
        applicationTag.addAttribute("android", "allowBackup", "true");
        applicationTag.addAttribute("android", "icon", "@mipmap/ic_launcher");
        applicationTag.addAttribute("android", "label", "@string/app_name");

        String applicationClassName = settings.getValue(ProjectSettings.SETTING_APPLICATION_CLASS, ".SketchApplication");
        applicationTag.addAttribute("android", "name", applicationClassName);
        if (addRequestLegacyExternalStorage) {
            applicationTag.addAttribute("android", "requestLegacyExternalStorage", "true");
        }
        if (!buildSettings.getValue(BuildSettings.SETTING_NO_HTTP_LEGACY, BuildSettings.SETTING_GENERIC_VALUE_FALSE)
                .equals(BuildSettings.SETTING_GENERIC_VALUE_TRUE)) {
            applicationTag.addAttribute("android", "usesCleartextTraffic", "true");
        }
        AndroidManifestInjector.getAppAttrs(applicationTag, buildConfig.sc_id);

        boolean hasDebugActivity = false;
        for (ProjectFileBean projectFileBean : projectFiles) {
            if (!projectFileBean.fileName.contains("_fragment")) {
                XmlBuilder activityTag = new XmlBuilder("activity");

                String javaName = projectFileBean.getJavaName();
                activityTag.addAttribute("android", "name", "." + javaName.substring(0, javaName.indexOf(".java")));

                if (!AndroidManifestInjector.getActivityAttrs(activityTag, buildConfig.sc_id, projectFileBean.getJavaName())) {
                    activityTag.addAttribute("android", "configChanges", "orientation|screenSize|keyboardHidden|smallestScreenSize|screenLayout");
                    activityTag.addAttribute("android", "hardwareAccelerated", "true");
                    activityTag.addAttribute("android", "supportsPictureInPicture", "true");
                }
                if (!AndroidManifestInjector.isActivityThemeUsed(activityTag, buildConfig.sc_id, projectFileBean.getJavaName())) {
                    if (buildConfig.isAppCompatEnabled) {
                        if (projectFileBean.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FULLSCREEN)) {
                            activityTag.addAttribute("android", "theme", "@style/AppTheme.FullScreen");
                        }
                    } else if (projectFileBean.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FULLSCREEN)) {
                        if (projectFileBean.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR)) {
                            activityTag.addAttribute("android", "theme", "@style/NoStatusBar");
                        } else {
                            activityTag.addAttribute("android", "theme", "@style/FullScreen");
                        }
                    } else if (!projectFileBean.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR)) {
                        activityTag.addAttribute("android", "theme", "@style/NoActionBar");
                    }
                }
                if (!AndroidManifestInjector.isActivityOrientationUsed(activityTag, buildConfig.sc_id, projectFileBean.getJavaName())) {
                    int orientation = projectFileBean.orientation;
                    if (orientation == ProjectFileBean.ORIENTATION_PORTRAIT) {
                        activityTag.addAttribute("android", "screenOrientation", "portrait");
                    } else if (orientation == ProjectFileBean.ORIENTATION_LANDSCAPE) {
                        activityTag.addAttribute("android", "screenOrientation", "landscape");
                    }
                }
                if (!AndroidManifestInjector.isActivityKeyboardUsed(activityTag, buildConfig.sc_id, projectFileBean.getJavaName())) {
                    String keyboardSetting = ActivityConfigConstants.getKeyboardSettingName(projectFileBean.keyboardSetting);
                    if (!keyboardSetting.isEmpty()) {
                        activityTag.addAttribute("android", "windowSoftInputMode", keyboardSetting);
                    }
                }
                if (projectFileBean.fileName.equals(AndroidManifestInjector.getLauncherActivity(buildConfig.sc_id))) {
                    XmlBuilder intentFilterTag = new XmlBuilder("intent-filter");
                    XmlBuilder actionTag = new XmlBuilder("action");
                    actionTag.addAttribute("android", "name", Intent.ACTION_MAIN);
                    intentFilterTag.addChildNode(actionTag);
                    XmlBuilder categoryTag = new XmlBuilder("category");
                    categoryTag.addAttribute("android", "name", Intent.CATEGORY_LAUNCHER);
                    intentFilterTag.addChildNode(categoryTag);
                    if (targetsSdkVersion31OrHigher && !AndroidManifestInjector.isActivityExportedUsed(buildConfig.sc_id, javaName)) {
                        activityTag.addAttribute("android", "exported", "true");
                    }
                    activityTag.addChildNode(intentFilterTag);
                }
                applicationTag.addChildNode(activityTag);
            }
            if (projectFileBean.fileName.equals("debug")) {
                hasDebugActivity = true;
            }
        }

        if (!hasDebugActivity) {
            XmlBuilder activityTag = new XmlBuilder("activity");
            activityTag.addAttribute("android", "name", ".DebugActivity");
            activityTag.addAttribute("android", "screenOrientation", "portrait");
            activityTag.addAttribute("android", "theme", "@style/AppTheme.DebugActivity");
            applicationTag.addChildNode(activityTag);
        }
        if (buildConfig.isAdMobEnabled) {
            XmlBuilder activityTag = new XmlBuilder("activity");
            activityTag.addAttribute("android", "name", "com.google.android.gms.ads.AdActivity");
            activityTag.addAttribute("android", "configChanges", "keyboard|keyboardHidden|orientation|screenLayout|uiMode|screenSize|smallestScreenSize");
            activityTag.addAttribute("android", "exported", "false");
            activityTag.addAttribute("android", "theme", "@android:style/Theme.Translucent");
            applicationTag.addChildNode(activityTag);

            XmlBuilder initProvider = new XmlBuilder("provider");
            initProvider.addAttribute("android", "name", "com.google.android.gms.ads.MobileAdsInitProvider");
            initProvider.addAttribute("android", "authorities", buildConfig.packageName + ".mobileadsinitprovider");
            initProvider.addAttribute("android", "exported", "false");
            initProvider.addAttribute("android", "initOrder", "100");
            applicationTag.addChildNode(initProvider);

            XmlBuilder adService = new XmlBuilder("service");
            adService.addAttribute("android", "name", "com.google.android.gms.ads.AdService");
            adService.addAttribute("android", "enabled", "true");
            adService.addAttribute("android", "exported", "false");
            applicationTag.addChildNode(adService);

            XmlBuilder testingActivity = new XmlBuilder("activity");
            testingActivity.addAttribute("android", "name", "com.google.android.gms.ads.OutOfContextTestingActivity");
            testingActivity.addAttribute("android", "configChanges", "keyboard|keyboardHidden|orientation|screenLayout|uiMode|screenSize|smallestScreenSize");
            testingActivity.addAttribute("android", "exported", "false");
            applicationTag.addChildNode(testingActivity);
        }
        if (builtInLibraryManager.containsLibrary(BuiltInLibraries.ANDROIDX_ROOM_RUNTIME)) {
            writeAndroidxRoomService(applicationTag);
        }
        writeAndroidxStartupInitializationProvider(applicationTag);
        if (builtInLibraryManager.containsLibrary(BuiltInLibraries.ANDROIDX_WORK_RUNTIME)) {
            writeAndroidxWorkRuntimeTags(applicationTag);
        }
        if (buildConfig.isFirebaseEnabled || buildConfig.isAdMobEnabled || buildConfig.isMapUsed) {
            writeGMSVersion(applicationTag);
        }
        if (buildConfig.isFirebaseEnabled) {
            writeFirebaseMetaData(applicationTag);
        }
        if (buildConfig.isFileProviderUsed) {
            writeFileProvider(applicationTag);
        }
        if (buildConfig.isAdMobEnabled && !isEmpty(buildConfig.appId)) {
            writeAdmobAppId(applicationTag);
        }
        if (buildConfig.isMapUsed) {
            writeGoogleMapMetaData(applicationTag);
        }
        if (buildConfig.constVarComponent.isFCMUsed) {
            EditorManifest.writeDefFCM(applicationTag);
        }
        if (buildConfig.constVarComponent.isFBGoogleUsed) {
            EditorManifest.manifestFBGoogleLogin(applicationTag);
        }
        if (FileUtil.isExistFile(filePathUtil.getManifestJava(buildConfig.sc_id))) {
            ArrayList<HashMap<String, Object>> activityAttrs = getActivityAttrs();
            for (String activityName : fileResConfig.getJavaManifestList()) {
                writeJava(applicationTag, activityName, activityAttrs);
            }
        }
        if (buildSettings.getValue(BuildSettings.SETTING_NO_HTTP_LEGACY, BuildSettings.SETTING_GENERIC_VALUE_FALSE)
                .equals(BuildSettings.SETTING_GENERIC_VALUE_FALSE)) {
            writeLegacyLibrary(applicationTag);
        }
        if (FileUtil.isExistFile(filePathUtil.getManifestService(buildConfig.sc_id))) {
            for (String serviceName : fileResConfig.getServiceManifestList()) {
                writeService(applicationTag, serviceName);
            }
        }
        if (FileUtil.isExistFile(filePathUtil.getManifestBroadcast(buildConfig.sc_id))) {
            for (String receiverName : fileResConfig.getBroadcastManifestList()) {
                writeBroadcast(applicationTag, receiverName);
            }
        }
        manifestXml.addChildNode(applicationTag);
        // Needed, as crashing on my SM-A526B with Android 12 / One UI 4.1 / firmware build A526BFXXS1CVD1 otherwise
        //noinspection RegExpRedundantEscape
        return AndroidManifestInjector.mHolder(manifestXml.toCode(), buildConfig.sc_id).replaceAll("\\$\\{applicationId\\}", packageName);
    }

    private void writeJava(XmlBuilder applicationTag, String activityName, ArrayList<HashMap<String, Object>> activityAttrs) {
        XmlBuilder activityTag = new XmlBuilder("activity");
        boolean specifiedActivityName = false;
        boolean specifiedConfigChanges = false;
        for (HashMap<String, Object> attrMap : activityAttrs) {
            if (attrMap.containsKey("name") && attrMap.containsKey("value")) {
                Object nameObject = attrMap.get("name");
                Object valueObject = attrMap.get("value");
                if (nameObject instanceof String && valueObject instanceof String) {
                    String name = nameObject.toString();
                    String value = valueObject.toString();
                    if (name.equals(activityName)) {
                        activityTag.addAttributeValue(value);
                        if (value.contains("android:name=")) {
                            specifiedActivityName = true;
                        } else if (value.contains("android:configChanges=")) {
                            specifiedConfigChanges = true;
                        }
                    }
                }
            }
        }
        if (!specifiedActivityName) {
            activityTag.addAttribute("android", "name", activityName);
        }
        if (!specifiedConfigChanges) {
            activityTag.addAttribute("android", "configChanges", "orientation|screenSize");
        }
        applicationTag.addChildNode(activityTag);
    }

    private ArrayList<HashMap<String, Object>> getActivityAttrs() {
        String activityAttributesPath = FileUtil.getExternalStorageDir().concat("/.sketchware/data/").concat(buildConfig.sc_id).concat("/Injection/androidmanifest/attributes.json");
        if (FileUtil.isExistFile(activityAttributesPath)) {
            try {
                return new Gson().fromJson(FileUtil.readFile(activityAttributesPath), Helper.TYPE_MAP_LIST);
            } catch (JsonSyntaxException e) {
                Log.w("ManifestGenerator", "Failed to parse activity attributes from: " + activityAttributesPath, e);
            }
        }
        return new ArrayList<>();
    }
}
