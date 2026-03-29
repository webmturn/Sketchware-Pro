package pro.sketchware.fragments.settings.language;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;

import androidx.annotation.NonNull;

public class LanguageOverrideContextWrapper extends ContextWrapper {

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
        return new LanguageOverrideResources(super.getResources());
    }

    @Override
    public Context getApplicationContext() {
        Context baseApplicationContext = super.getApplicationContext();
        if (baseApplicationContext != null) {
            return baseApplicationContext;
        }

        Context baseContext = getBaseContext();
        return baseContext != null ? baseContext : this;
    }
}
