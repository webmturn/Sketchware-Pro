package pro.sketchware.utility;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula;
import io.github.rosemoe.sora.widget.schemes.SchemeEclipse;
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub;
import io.github.rosemoe.sora.widget.schemes.SchemeNotepadXX;
import io.github.rosemoe.sora.widget.schemes.SchemeVS2019;
import pro.sketchware.R;

/**
 * Centralized preferences manager for all sora-editor based code editors.
 * <p>
 * Each editor context (e.g. "src" for SrcCodeEditor, "dlg" for AsdDialog,
 * "vce" for ViewCodeEditorActivity, "viewer" for SrcViewerActivity)
 * stores its own set of preferences under a shared SharedPreferences file.
 */
public class CodeEditorPreferences {

    private static final String PREFS_NAME = "code_editor_settings";

    // Preference key suffixes
    private static final String KEY_TEXT_SIZE = "_text_size";
    private static final String KEY_THEME = "_theme";
    private static final String KEY_WORD_WRAP = "_word_wrap";
    private static final String KEY_AUTO_COMPLETE = "_auto_complete";
    private static final String KEY_SYMBOL_PAIR = "_symbol_pair";
    private static final String KEY_LINE_NUMBERS = "_line_numbers";

    // Default values
    public static final int DEFAULT_TEXT_SIZE = 14;
    public static final int DEFAULT_THEME = 3; // Darcula
    public static final boolean DEFAULT_WORD_WRAP = false;
    public static final boolean DEFAULT_AUTO_COMPLETE = true;
    public static final boolean DEFAULT_SYMBOL_PAIR = true;
    public static final boolean DEFAULT_LINE_NUMBERS = true;

    // Theme indices (matching KNOWN_THEME_NAMES order)
    public static final int THEME_DEFAULT = 0;
    public static final int THEME_GITHUB = 1;
    public static final int THEME_ECLIPSE = 2;
    public static final int THEME_DARCULA = 3;
    public static final int THEME_VS2019 = 4;
    public static final int THEME_NOTEPADXX = 5;

    public static final String[] THEME_NAMES = {
            "Default", "GitHub", "Eclipse", "Darcula", "VS2019", "NotepadXX"
    };

    private final SharedPreferences prefs;
    private final String prefix;

    public CodeEditorPreferences(Context context, String prefix) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        this.prefix = prefix;
    }

    // --- Getters ---

    public int getTextSize() {
        return prefs.getInt(prefix + KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE);
    }

    public int getTheme() {
        return prefs.getInt(prefix + KEY_THEME, DEFAULT_THEME);
    }

    public boolean getWordWrap() {
        return prefs.getBoolean(prefix + KEY_WORD_WRAP, DEFAULT_WORD_WRAP);
    }

    public boolean getAutoComplete() {
        return prefs.getBoolean(prefix + KEY_AUTO_COMPLETE, DEFAULT_AUTO_COMPLETE);
    }

    public boolean getSymbolPair() {
        return prefs.getBoolean(prefix + KEY_SYMBOL_PAIR, DEFAULT_SYMBOL_PAIR);
    }

    public boolean getLineNumbers() {
        return prefs.getBoolean(prefix + KEY_LINE_NUMBERS, DEFAULT_LINE_NUMBERS);
    }

    // --- Setters ---

    public void setTextSize(int size) {
        prefs.edit().putInt(prefix + KEY_TEXT_SIZE, size).apply();
    }

    public void setTheme(int theme) {
        prefs.edit().putInt(prefix + KEY_THEME, theme).apply();
    }

    public void setWordWrap(boolean wrap) {
        prefs.edit().putBoolean(prefix + KEY_WORD_WRAP, wrap).apply();
    }

    public void setAutoComplete(boolean enabled) {
        prefs.edit().putBoolean(prefix + KEY_AUTO_COMPLETE, enabled).apply();
    }

    public void setSymbolPair(boolean enabled) {
        prefs.edit().putBoolean(prefix + KEY_SYMBOL_PAIR, enabled).apply();
    }

    public void setLineNumbers(boolean enabled) {
        prefs.edit().putBoolean(prefix + KEY_LINE_NUMBERS, enabled).apply();
    }

    // --- Apply to editor ---

    /**
     * Apply all saved preferences to a CodeEditor instance.
     *
     * @param editor    the CodeEditor to configure
     * @param applyTheme whether to also apply the saved color scheme theme
     */
    public void applyToEditor(CodeEditor editor, boolean applyTheme) {
        editor.setTextSize(getTextSize());
        editor.setWordwrap(getWordWrap());
        editor.setLineNumberEnabled(getLineNumbers());
        editor.getProps().symbolPairAutoCompletion = getSymbolPair();
        editor.getComponent(EditorAutoCompletion.class).setEnabled(getAutoComplete());
        if (applyTheme) {
            applyTheme(editor, getTheme());
        }
    }

    /**
     * Save the current text size from the editor (useful for pinch-to-zoom persistence).
     */
    public void saveTextSizeFromEditor(CodeEditor editor, float scaledDensity) {
        setTextSize((int) (editor.getTextSizePx() / scaledDensity));
    }

    // --- Static theme helpers ---

    /**
     * Apply a theme by index to the given CodeEditor.
     * Will not override TextMate-based color schemes.
     */
    public static void applyTheme(CodeEditor editor, int themeIndex) {
        if (editor.getColorScheme() instanceof TextMateColorScheme) {
            return;
        }
        EditorColorScheme scheme = switch (themeIndex) {
            case THEME_GITHUB -> new SchemeGitHub();
            case THEME_ECLIPSE -> new SchemeEclipse();
            case THEME_DARCULA -> new SchemeDarcula();
            case THEME_VS2019 -> new SchemeVS2019();
            case THEME_NOTEPADXX -> new SchemeNotepadXX();
            default -> new EditorColorScheme();
        };
        editor.setColorScheme(scheme);
    }

    /**
     * Get the current theme index from the editor's color scheme.
     */
    public static int detectThemeIndex(CodeEditor editor) {
        EditorColorScheme scheme = editor.getColorScheme();
        if (scheme instanceof SchemeGitHub) return THEME_GITHUB;
        if (scheme instanceof SchemeEclipse) return THEME_ECLIPSE;
        if (scheme instanceof SchemeDarcula) return THEME_DARCULA;
        if (scheme instanceof SchemeVS2019) return THEME_VS2019;
        if (scheme instanceof SchemeNotepadXX) return THEME_NOTEPADXX;
        return THEME_DEFAULT;
    }

    // --- Dialog helpers ---

    /**
     * Show a font size picker dialog.
     *
     * @param activity the host activity
     * @param editor   the CodeEditor to update
     * @param onSizeChanged optional callback after size is changed, may be null
     */
    public void showFontSizeDialog(Activity activity, CodeEditor editor, Runnable onSizeChanged) {
        NumberPicker picker = new NumberPicker(activity);
        picker.setMinValue(8);
        picker.setMaxValue(32);
        picker.setWrapSelectorWheel(false);
        picker.setValue(getTextSize());

        LinearLayout layout = new LinearLayout(activity);
        layout.setGravity(Gravity.CENTER);
        layout.addView(picker, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.code_editor_select_font_size)
                .setIcon(R.drawable.ic_mtrl_formattext)
                .setView(layout)
                .setPositiveButton(R.string.common_word_apply, (dialog, which) -> {
                    int size = picker.getValue();
                    setTextSize(size);
                    editor.setTextSize(size);
                    if (onSizeChanged != null) onSizeChanged.run();
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    /**
     * Show a theme selection dialog.
     *
     * @param activity the host activity
     * @param editor   the CodeEditor to update
     */
    public void showThemeDialog(Activity activity, CodeEditor editor) {
        int currentTheme = detectThemeIndex(editor);
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.code_editor_select_theme)
                .setSingleChoiceItems(THEME_NAMES, currentTheme, (dialog, which) -> {
                    applyTheme(editor, which);
                    setTheme(which);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }
}
