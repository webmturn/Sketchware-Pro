package pro.sketchware.util;

import android.graphics.Typeface;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.io.File;

import pro.sketchware.R;

/** File-backed previews must reset both text and typeface when a recycled row is rebound. */
public final class FontPreview {
    private FontPreview() {}

    public static void bind(TextView preview, @Nullable String path, @StringRes int sample) {
        Typeface typeface = null;
        if (path != null && !path.isEmpty()) {
            try {
                File file = new File(path);
                if (file.isFile() && file.length() > 0) {
                    Typeface loaded = Typeface.createFromFile(file);
                    // Android may silently return DEFAULT for an existing but invalid font.
                    if (loaded != Typeface.DEFAULT) typeface = loaded;
                }
            } catch (RuntimeException e) {
                Log.w("FontPreview", "Failed to load font preview: " + path, e);
            }
        }
        preview.setTypeface(typeface != null ? typeface : Typeface.DEFAULT);
        preview.setText(typeface != null ? sample : R.string.font_load_failed);
    }
}
