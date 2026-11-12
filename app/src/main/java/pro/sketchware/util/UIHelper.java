package pro.sketchware.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import java.util.LinkedHashMap;
import java.util.Map;

public class UIHelper {
    /** Reads tagged form controls without knowing how their values will be stored. */
    public static Map<String, String> collectTaggedViewValues(View... views) {
        Map<String, String> values = new LinkedHashMap<>();
        for (View view : views) {
            if (view.getTag() == null) continue;
            String key = (String) view.getTag();
            if (view instanceof EditText editText) {
                values.put(key, Helper.getText(editText));
            } else if (view instanceof Checkable checkable) {
                values.put(key, Boolean.toString(checkable.isChecked()));
            } else if (view instanceof RadioGroup radioGroup) {
                String value = "";
                for (int i = 0; i < radioGroup.getChildCount(); i++) {
                    RadioButton radioButton = (RadioButton) radioGroup.getChildAt(i);
                    if (radioButton.isChecked()) {
                        value = Helper.getText(radioButton);
                        break;
                    }
                }
                values.put(key, value);
            }
        }
        return values;
    }

    public static long lastClickTime;

    public static void hideKeyboard(Context context, EditText editText) {
        ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public static void disableTemporarily(View view) {
        view.setEnabled(false);
        (new Handler(Looper.getMainLooper())).postDelayed(() -> view.setEnabled(true), 100L);
    }

    public static boolean isClickThrottled() {
        if (SystemClock.elapsedRealtime() - lastClickTime < 100L) return true;
        lastClickTime = SystemClock.elapsedRealtime();
        return false;
    }
}
