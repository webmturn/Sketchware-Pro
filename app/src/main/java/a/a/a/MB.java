package a.a.a;

import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

/**
 * @deprecated Use {@link BaseValidator} instead. Kept for binary compatibility with JAR classes (e.g. OB, PB, QB, WB).
 */
@Deprecated
public abstract class MB extends BaseValidator {
    public MB(Context context, TextInputLayout textInputLayout) {
        super(context, textInputLayout);
    }
}
