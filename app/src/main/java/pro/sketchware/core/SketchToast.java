package pro.sketchware.core;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;

import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.CustomToastBinding;
import pro.sketchware.utility.ThemeUtils;

public class SketchToast {

    public static final int TOAST_NORMAL = 0;
    public static final int TOAST_WARNING = 1;

    public static Toast toast(Context context, int resString, int duration) {
        return toast(context, Helper.getResString(resString), duration, 48, 0.0f, 64.0f, TOAST_NORMAL);
    }

    public static Toast toast(Context context, CharSequence message, int duration) {
        return toast(context, message, duration, 48, 0.0f, 64.0f, TOAST_NORMAL);
    }

    public static Toast toast(Context context, CharSequence message, int duration, int gravity, float xOffset, float yOffset, int toastType) {
        try {
            Context themedContext = new ContextThemeWrapper(context, R.style.Theme_SketchwarePro);

            CustomToastBinding binding = CustomToastBinding.inflate(LayoutInflater.from(themedContext));

            if (toastType == TOAST_WARNING) {
                int errorColor = ThemeUtils.getColor(binding.getRoot(), R.attr.colorError);

                binding.cardView.setCardBackgroundColor(ContextCompat.getColor(themedContext, R.color.color_error_bg));
                binding.cardView.setStrokeColor(errorColor);
                binding.tvStoast.setTextColor(errorColor);
            }

            binding.tvStoast.setText(message);

            Toast toast = new Toast(themedContext);
            toast.setDuration(duration);
            toast.setGravity(
                    gravity,
                    (int) ViewUtil.dpToPx(themedContext, xOffset),
                    (int) ViewUtil.dpToPx(themedContext, yOffset)
            );
            toast.setView(binding.getRoot());

            return toast;
        } catch (Exception e) {
            return Toast.makeText(context, message, duration);
        }
    }

    public static Toast warning(Context context, int resString, int duration) {
        return toast(context, Helper.getResString(resString), duration, 48, 0.0f, 64.0f, TOAST_WARNING);
    }

    public static Toast warning(Context context, CharSequence message, int duration) {
        return toast(context, message, duration, 48, 0.0f, 64.0f, TOAST_WARNING);
    }
}
