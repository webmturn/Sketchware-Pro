package pro.sketchware.fragments.settings.language;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;

import androidx.annotation.NonNull;

public class LanguageOverrideContextWrapper extends ContextWrapper {

    private LanguageOverrideResources overrideResources;
    private Context overrideApplicationContext;

    private LanguageOverrideContextWrapper(@NonNull Context base) {
        super(base);
    }

    @NonNull
    public static Context wrap(@NonNull Context context) {
        if (context instanceof LanguageOverrideContextWrapper) {
            return context;
        }
        return new LanguageOverrideContextWrapper(context);
    }

    @Override
    public Resources getResources() {
        if (overrideResources == null) {
            overrideResources = new LanguageOverrideResources(super.getResources());
        }
        return overrideResources;
    }

    @Override
    public Context getApplicationContext() {
        Context baseApplicationContext = super.getApplicationContext();
        Context baseContext = getBaseContext();
        if (baseApplicationContext == null) {
            return baseContext != null ? baseContext : this;
        }
        if (baseApplicationContext == baseContext) {
            return this;
        }
        if (overrideApplicationContext == null) {
            overrideApplicationContext = wrap(baseApplicationContext);
        }
        return overrideApplicationContext;
    }
}
