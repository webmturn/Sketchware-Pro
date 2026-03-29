package pro.sketchware.fragments.settings.language;

import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.core.os.ConfigurationCompat;

import java.util.Locale;

public final class LanguageOverrideResources extends Resources {

    private final Resources baseResources;

    public LanguageOverrideResources(@NonNull Resources baseResources) {
        super(baseResources.getAssets(), baseResources.getDisplayMetrics(), baseResources.getConfiguration());
        this.baseResources = baseResources;
    }

    @NonNull
    @Override
    public CharSequence getText(int id) throws NotFoundException {
        String override = LanguageOverrideManager.getInstance().getOverride(id);
        return override != null ? override : baseResources.getText(id);
    }

    @NonNull
    @Override
    public CharSequence getText(int id, CharSequence def) {
        String override = LanguageOverrideManager.getInstance().getOverride(id);
        return override != null ? override : baseResources.getText(id, def);
    }

    @NonNull
    @Override
    public String getString(int id) throws NotFoundException {
        String override = LanguageOverrideManager.getInstance().getOverride(id);
        return override != null ? override : baseResources.getString(id);
    }

    @NonNull
    @Override
    public String getString(int id, Object... formatArgs) throws NotFoundException {
        return String.format(getFormatLocale(), getString(id), formatArgs);
    }

    @NonNull
    @Override
    public CharSequence getQuantityText(int id, int quantity) throws NotFoundException {
        return baseResources.getQuantityText(id, quantity);
    }

    @NonNull
    @Override
    public String getQuantityString(int id, int quantity, Object... formatArgs) throws NotFoundException {
        return baseResources.getQuantityString(id, quantity, formatArgs);
    }

    @NonNull
    @Override
    public String getQuantityString(int id, int quantity) throws NotFoundException {
        return baseResources.getQuantityString(id, quantity);
    }

    @NonNull
    @Override
    public CharSequence[] getTextArray(int id) throws NotFoundException {
        return baseResources.getTextArray(id);
    }

    @NonNull
    @Override
    public String[] getStringArray(int id) throws NotFoundException {
        return baseResources.getStringArray(id);
    }

    @NonNull
    private Locale getFormatLocale() {
        Locale locale = ConfigurationCompat.getLocales(baseResources.getConfiguration()).get(0);
        return locale != null ? locale : Locale.getDefault();
    }
}
