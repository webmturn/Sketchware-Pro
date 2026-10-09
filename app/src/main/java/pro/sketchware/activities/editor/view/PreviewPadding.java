package pro.sketchware.activities.editor.view;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pro.sketchware.activities.editor.view.item.ItemCardView;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.layout.PaddingAttributes;

/** Applies the generator's padding attributes after the preview background is installed. */
public final class PreviewPadding {
    private static final int COMPLEX_FRACTION_BITS = 23;
    private static final int COMPLEX_INTEGER_LIMIT = 1 << COMPLEX_FRACTION_BITS;
    private static final long COMPLEX_FRACTION_MASK = COMPLEX_INTEGER_LIMIT - 1L;
    private static final Pattern DIMENSION = Pattern.compile(
            "([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?)(dp|dip|px|sp|pt|in|mm)");
    // Only relative padding needs to be resolved again when a parent changes direction.
    // Values contain pixels only, with no reference back to the weakly held view.
    private static final Map<View, RelativePadding> RELATIVE = new WeakHashMap<>();

    private PreviewPadding() { }

    public static void apply(View view, ViewBean bean) {
        boolean card = view instanceof ItemCardView;
        Map<String, String> attributes = PaddingAttributes.effective(bean, card);
        String prefix = card ? "app:contentPadding" : "android:padding";
        Rect padding = new Rect();
        boolean backgroundPadding = !card && view.getBackground() != null && view.getBackground().getPadding(padding);
        int backgroundLeft = padding.left;
        int backgroundRight = padding.right;
        Integer uniform = dimension(view.getContext(), attributes.get(prefix));
        if (card && uniform != null) padding.set(uniform, uniform, uniform, uniform);
        padding.left = value(view, attributes, prefix + "Left", padding.left);
        padding.top = value(view, attributes, prefix + "Top", padding.top);
        padding.right = value(view, attributes, prefix + "Right", padding.right);
        padding.bottom = value(view, attributes, prefix + "Bottom", padding.bottom);
        RELATIVE.remove(view);
        if (card) {
            ((ItemCardView) view).setContentPaddingPixels(padding.left, padding.top, padding.right, padding.bottom);
            return;
        }
        Integer horizontal = dimension(view.getContext(), attributes.get(prefix + "Horizontal"));
        Integer vertical = dimension(view.getContext(), attributes.get(prefix + "Vertical"));
        if (horizontal != null && horizontal >= 0) padding.left = padding.right = horizontal;
        if (vertical != null && vertical >= 0) padding.top = padding.bottom = vertical;
        if (uniform != null && uniform >= 0) padding.set(uniform, uniform, uniform, uniform);
        Integer start = dimension(view.getContext(), attributes.get(prefix + "Start"));
        Integer end = dimension(view.getContext(), attributes.get(prefix + "End"));
        boolean supportsRtl = (view.getContext().getApplicationInfo().flags & ApplicationInfo.FLAG_SUPPORTS_RTL) != 0;
        if (!supportsRtl) {
            if (!defined(view, attributes, prefix, "Left", uniform, horizontal) && start != null) padding.left = start;
            if (!defined(view, attributes, prefix, "Right", uniform, horizontal) && end != null) padding.right = end;
            setPixels(view, padding.left, padding.top, padding.right, padding.bottom);
        } else if (start != null || end != null) {
            // Android installs background padding after parsing absolute sides. With relative
            // padding, it restores only aggregate/horizontal attributes before resolving RTL.
            if (backgroundPadding && (uniform == null || uniform < 0) && (horizontal == null || horizontal < 0)) {
                padding.left = backgroundLeft;
                padding.right = backgroundRight;
            }
            RelativePadding relative = new RelativePadding(padding, start, end);
            RELATIVE.put(view, relative);
            relative.apply(view, view.getLayoutDirection());
        } else {
            setPixels(view, padding.left, padding.top, padding.right, padding.bottom);
        }
    }

    private static boolean defined(View view, Map<String, String> attributes, String prefix, String side,
                                   Integer uniform, Integer horizontal) {
        return uniform != null || horizontal != null || dimension(view.getContext(), attributes.get(prefix + side)) != null;
    }

    private static int value(View view, Map<String, String> attributes, String name, int fallback) {
        Integer value = dimension(view.getContext(), attributes.get(name));
        return value == null || value < 0 ? fallback : value;
    }

    private static Integer dimension(Context context, String text) {
        if (text == null) return null;
        String value = text.trim();
        try {
            if (value.startsWith("@android:dimen/")) {
                int id = context.getResources().getIdentifier(value.substring(15), "dimen", "android");
                return id == 0 ? null : context.getResources().getDimensionPixelSize(id);
            }
            Matcher matcher = DIMENSION.matcher(value);
            if (!matcher.matches()) return null;
            float number = Float.parseFloat(matcher.group(1));
            int unit = switch (matcher.group(2)) {
                case "dp", "dip" -> TypedValue.COMPLEX_UNIT_DIP;
                case "sp" -> TypedValue.COMPLEX_UNIT_SP;
                case "pt" -> TypedValue.COMPLEX_UNIT_PT;
                case "in" -> TypedValue.COMPLEX_UNIT_IN;
                case "mm" -> TypedValue.COMPLEX_UNIT_MM;
                default -> TypedValue.COMPLEX_UNIT_PX;
            };
            Integer data = dimensionData(number, unit);
            return data == null ? null : TypedValue.complexToDimensionPixelSize(
                    data, context.getResources().getDisplayMetrics());
        } catch (NumberFormatException | Resources.NotFoundException exception) {
            return null;
        }
    }

    private static Integer dimensionData(float number, int unit) {
        // Reject values outside Android's signed complex range instead of reproducing overflow.
        if (!Float.isFinite(number) || number < -COMPLEX_INTEGER_LIMIT || number >= COMPLEX_INTEGER_LIMIT) return null;
        // Match AAPT2's float intermediate and truncation before selecting a complex radix.
        long bits = (long) (Math.abs(number) * COMPLEX_INTEGER_LIMIT + 0.5f);
        int radix;
        int shift;
        if ((bits & COMPLEX_FRACTION_MASK) == 0) {
            radix = TypedValue.COMPLEX_RADIX_23p0;
            shift = COMPLEX_FRACTION_BITS;
        } else if ((bits >>> COMPLEX_FRACTION_BITS) == 0) {
            radix = TypedValue.COMPLEX_RADIX_0p23;
            shift = 0;
        } else if ((bits >>> (COMPLEX_FRACTION_BITS + Byte.SIZE)) == 0) {
            radix = TypedValue.COMPLEX_RADIX_8p15;
            shift = Byte.SIZE;
        } else if ((bits >>> (COMPLEX_FRACTION_BITS + Short.SIZE)) == 0) {
            radix = TypedValue.COMPLEX_RADIX_16p7;
            shift = Short.SIZE;
        } else {
            radix = TypedValue.COMPLEX_RADIX_23p0;
            shift = COMPLEX_FRACTION_BITS;
        }
        int mantissa = (int) ((bits >>> shift) & TypedValue.COMPLEX_MANTISSA_MASK);
        if (number < 0) mantissa = -mantissa & TypedValue.COMPLEX_MANTISSA_MASK;
        return (mantissa << TypedValue.COMPLEX_MANTISSA_SHIFT)
                | (radix << TypedValue.COMPLEX_RADIX_SHIFT) | unit;
    }

    private static void setPixels(View view, int left, int top, int right, int bottom) {
        if (view instanceof ItemView item) item.setPaddingPixels(left, top, right, bottom);
        else view.setPadding(left, top, right, bottom);
    }

    public static void onLayoutDirectionChanged(View view, int direction) {
        RelativePadding padding = RELATIVE.get(view);
        if (padding != null) padding.apply(view, direction);
    }

    private static final class RelativePadding {
        private final Rect absolute;
        private final Integer start, end;
        private boolean applying;

        RelativePadding(Rect absolute, Integer start, Integer end) {
            this.absolute = absolute;
            this.start = start;
            this.end = end;
        }

        void apply(View view, int direction) {
            if (applying) return;
            applying = true;
            try {
                boolean rtl = direction == View.LAYOUT_DIRECTION_RTL;
                int left = rtl ? end == null ? absolute.left : end : start == null ? absolute.left : start;
                int right = rtl ? start == null ? absolute.right : start : end == null ? absolute.right : end;
                setPixels(view, left, absolute.top, right, absolute.bottom);
            } finally {
                applying = false;
            }
        }
    }
}
