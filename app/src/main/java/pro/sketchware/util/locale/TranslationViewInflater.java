package pro.sketchware.util.locale;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatTextView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.theme.MaterialComponentsViewInflater;

public final class TranslationViewInflater extends MaterialComponentsViewInflater {
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static final String APP = "http://schemas.android.com/apk/res-auto";
    private final LanguageOverrideManager translations = LanguageOverrideManager.getInstance();

    @Override
    protected AppCompatTextView createTextView(Context context, AttributeSet attrs) {
        return translate(super.createTextView(context, attrs), attrs);
    }

    @Override
    protected AppCompatButton createButton(Context context, AttributeSet attrs) {
        return translate(super.createButton(context, attrs), attrs);
    }

    @Override
    protected AppCompatEditText createEditText(Context context, AttributeSet attrs) {
        return translate(super.createEditText(context, attrs), attrs);
    }

    @Override
    protected AppCompatCheckBox createCheckBox(Context context, AttributeSet attrs) {
        return translate(super.createCheckBox(context, attrs), attrs);
    }

    @Override
    protected AppCompatRadioButton createRadioButton(Context context, AttributeSet attrs) {
        return translate(super.createRadioButton(context, attrs), attrs);
    }

    @Override
    protected View createView(Context context, String name, AttributeSet attrs) {
        if (translations.getActivePack(context.getResources()) == null) return null;
        return switch (name) {
            case "androidx.appcompat.widget.AppCompatEditText" ->
                    translate(new AppCompatEditText(context, attrs), attrs);
            case "com.google.android.material.button.MaterialButton" ->
                    translate(new MaterialButton(context, attrs), attrs);
            case "com.google.android.material.checkbox.MaterialCheckBox" ->
                    translate(new MaterialCheckBox(context, attrs), attrs);
            case "com.google.android.material.chip.Chip" ->
                    translate(new Chip(context, attrs), attrs);
            case "com.google.android.material.materialswitch.MaterialSwitch" ->
                    translate(new MaterialSwitch(context, attrs), attrs);
            case "com.google.android.material.radiobutton.MaterialRadioButton" ->
                    translate(new MaterialRadioButton(context, attrs), attrs);
            case "com.google.android.material.textview.MaterialTextView" ->
                    translate(new MaterialTextView(context, attrs), attrs);
            case "com.google.android.material.textfield.TextInputEditText" ->
                    translate(new TextInputEditText(context, attrs), attrs);
            case "com.google.android.material.textfield.TextInputLayout" -> {
                var view = new TextInputLayout(context, attrs);
                String hint = getOverride(view, attrs, ANDROID, "hint");
                if (hint != null) view.setHint(hint);
                yield view;
            }
            case "com.google.android.material.appbar.MaterialToolbar" -> {
                var view = new MaterialToolbar(context, attrs);
                String title = getOverride(view, attrs, APP, "title");
                String subtitle = getOverride(view, attrs, APP, "subtitle");
                if (title != null) view.setTitle(title);
                if (subtitle != null) view.setSubtitle(subtitle);
                yield view;
            }
            default -> null;
        };
    }

    private <T extends TextView> T translate(T view, AttributeSet attrs) {
        String text = getOverride(view, attrs, ANDROID, "text");
        String hint = getOverride(view, attrs, ANDROID, "hint");
        if (text != null) view.setText(text);
        if (hint != null) view.setHint(hint);
        return view;
    }

    private String getOverride(View view, AttributeSet attrs, String namespace, String name) {
        int id = attrs.getAttributeResourceValue(namespace, name, 0);
        return id == 0 ? null : translations.getOverride(view.getResources(), id);
    }
}
