package pro.sketchware.core.library;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.ContextThemeWrapper;
import com.google.android.material.color.DynamicColors;
import pro.sketchware.R;
import pro.sketchware.beans.ProjectLibraryBean;
import pro.sketchware.core.project.ProjectDataManager;

public class Material3LibraryManager {

    private final boolean isEditingState;
    private final boolean isAppCompatEnabled;
    private final ProjectLibraryBean appCombatLibraryBean;

    public Material3LibraryManager(String sc_id) {
        isEditingState = false;
        appCombatLibraryBean = ProjectDataManager.getLibraryManager(sc_id).getCompat();
        isAppCompatEnabled = appCombatLibraryBean.isEnabled();
    }

    public Material3LibraryManager(ProjectLibraryBean projectLibraryBean) {
        isEditingState = true;
        appCombatLibraryBean = projectLibraryBean;
        isAppCompatEnabled = projectLibraryBean.isEnabled();
    }

    public boolean isAppCompatEnabled() {
        return isAppCompatEnabled;
    }

    public boolean isMaterial3Enabled() {
        return isAppCompatEnabled && safeGetBoolean("material3");
    }

    public boolean isDynamicColorsEnabled() {
        return (isMaterial3Enabled() || isEditingState) && safeGetBoolean("dynamic_colors");
    }

    public String getTheme() {
        return isMaterial3Enabled() || isEditingState ? safeGetString("theme") : "DayNight";
    }

    private String safeGetString(String key) {
        if (appCombatLibraryBean.configurations != null
                && appCombatLibraryBean.configurations.containsKey(key)
                && appCombatLibraryBean.configurations.get(key) instanceof String keyValue) {
            return keyValue;
        }
        return "";
    }

    private boolean safeGetBoolean(String key) {
        if (appCombatLibraryBean.configurations != null
                && appCombatLibraryBean.configurations.containsKey(key)
                && appCombatLibraryBean.configurations.get(key) instanceof Boolean keyValue) {
            return keyValue;
        }
        return false;
    }

    public Context createPreviewContext(Context base) {
        return createPreviewContext(base, usesNightResources());
    }

    public Context createPreviewContext(Context base, boolean night) {
        if (!isMaterial3Enabled()) {
            return new ContextThemeWrapper(base, R.style.ThemeOverlay_SketchwarePro_ViewEditor);
        }
        int style =
                switch (getTheme()) {
                    case "Light" ->
                            com.google.android.material.R.style.Theme_Material3_Light_NoActionBar;
                    case "Dark" ->
                            com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar;
                    default ->
                            com.google.android.material.R.style
                                    .Theme_Material3_DayNight_NoActionBar;
                };
        return createMaterial3Context(base, style, night);
    }

    public Context createColorSampleContext(Context base, boolean dark) {
        int style =
                dark
                        ? com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar
                        : com.google.android.material.R.style.Theme_Material3_Light_NoActionBar;
        return createMaterial3Context(base, style, dark);
    }

    private Context createMaterial3Context(Context base, int style, boolean night) {
        Configuration override = new Configuration();
        override.uiMode =
                (base.getResources().getConfiguration().uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                        | (night
                                ? Configuration.UI_MODE_NIGHT_YES
                                : Configuration.UI_MODE_NIGHT_NO);
        Context configured = base.createConfigurationContext(override);
        Resources.Theme theme = configured.getResources().newTheme();
        theme.applyStyle(style, true);
        Context preview = new ContextThemeWrapper(configured, theme);
        return isDynamicColorsEnabled() ? DynamicColors.wrapContextIfAvailable(preview) : preview;
    }

    public boolean usesNightResources() {
        int systemNight =
                Resources.getSystem().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return isMaterial3Enabled() && systemNight == Configuration.UI_MODE_NIGHT_YES;
    }

    // The color picker selects a theme sample. Resource qualifiers use usesNightResources().
    public boolean canUseNightVariantColors() {
        return isMaterial3Enabled()
                && (getTheme().equals("Dark")
                        || !getTheme().equals("Light") && usesNightResources());
    }

    public int getDynamicColorOverlay(Context themedContext) {
        if (!isDynamicColorsEnabled() || !DynamicColors.isDynamicColorAvailable()) return 0;
        TypedArray attributes =
                themedContext.obtainStyledAttributes(
                        new int[] {com.google.android.material.R.attr.dynamicColorThemeOverlay});
        try {
            return attributes.getResourceId(0, 0);
        } finally {
            attributes.recycle();
        }
    }

    public ProjectLibraryBean getAppCombatLibraryBean() {
        return appCombatLibraryBean;
    }
}
