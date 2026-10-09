package pro.sketchware.activities.editor.view;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.activities.editor.view.item.ItemCardView;
import pro.sketchware.activities.editor.view.item.ItemLinearLayout;
import pro.sketchware.activities.editor.view.item.ItemTextView;
import pro.sketchware.beans.ViewBean;

/** The oracle is AAPT2 resource output, not a copy of the preview's encoding formula. */
@RunWith(AndroidJUnit4.class)
public class PreviewDimensionEncodingTest {
    private static final int[] DENSITIES = {160, 240, 320, 420, 480};
    private static final float[] FONT_SCALES = {1f, 1.3f};
    private static final DimensionCase[] CASES = {
            dimension("dp_1_66667", "1.66667dp"),
            dimension("dp_3_666667", "3.666667dp"),
            dimension("dp_2_33334", "2.33334dp"),
            dimension("dp_1_33334", "1.33334dp"),
            dimension("dp_1_71429", "1.71429dp"),
            dimension("dp_1_16667", "1.16667dp"),
            dimension("dp_1_83334", "1.83334dp"),
            dimension("sp_1_66667", "1.66667sp"),
            dimension("px_tiny", "0.00000001px"),
            dimension("px_zero", "0px"),
            dimension("dp_4", "4dp"),
            dimension("dp_6", "6dp"),
            dimension("dp_0_2", "0.2dp"),
            dimension("dp_0_25", "0.25dp"),
            dimension("px_0_5", "0.5px"),
            dimension("dip_1_66667", "1.66667dip"),
            dimension("px_1_66667", "1.66667px"),
            dimension("pt_1_16667", "1.16667pt"),
            dimension("in_fraction", "0.010416687in"),
            dimension("mm_fraction", "0.26458332mm"),
            dimension("dp_positive_sign", "+1.66667dp"),
            dimension("dp_without_integer", ".25dp"),
            dimension("px_exponent", "1e-8px"),
            dimension("dp_integer_float", "1.0dp"),
            dimension("px_below_1", "0.99999px"),
            dimension("px_above_1", "1.00001px"),
            dimension("px_255", "255px"),
            dimension("px_below_256", "255.99998px"),
            dimension("px_256", "256px"),
            dimension("px_above_256", "256.0001px"),
            dimension("px_65535", "65535px"),
            dimension("px_below_65536", "65535.99px"),
            dimension("px_65536", "65536px"),
            dimension("px_above_65536", "65536.25px"),
            dimension("px_max_integer", "8388607px")
    };
    private static final DimensionCase[] RAW_ONLY_CASES = {
            dimension("px_negative_zero", "-0px"),
            dimension("dp_positive_zero", "+0dp"),
            dimension("dp_negative_zero", "-0dp"),
            dimension("px_negative_tiny", "-0.00000001px"),
            dimension("dp_negative_half", "-0.5dp"),
            dimension("dp_negative_fraction", "-1.66667dp"),
            dimension("px_negative_below_1", "-0.99999px"),
            dimension("px_negative_below_256", "-255.99998px"),
            dimension("px_negative_above_256", "-256.0001px"),
            dimension("px_negative_below_65536", "-65535.99px"),
            dimension("px_negative_above_65536", "-65536.25px"),
            dimension("px_min_integer", "-8388608px"),
            dimension("px_scientific_integer", "1e6px")
    };

    @Test
    public void complexEncodingMatchesCompiledResourceData() throws Exception {
        Context test = InstrumentationRegistry.getInstrumentation().getContext();
        Resources resources = test.getResources();
        Method encode = PreviewPadding.class.getDeclaredMethod("dimensionData", float.class, int.class);
        encode.setAccessible(true);
        int checks = 0;
        for (DimensionCase[] cases : new DimensionCase[][]{CASES, RAW_ONLY_CASES}) {
            for (DimensionCase testCase : cases) {
                int id = resourceId(test, testCase);
                TypedValue compiled = new TypedValue();
                resources.getValue(id, compiled, true);
                assertEquals(testCase.literal + " must be compiled as a dimension", TypedValue.TYPE_DIMENSION, compiled.type);
                float number = Float.parseFloat(testCase.literal.replaceFirst("(dp|dip|px|sp|pt|in|mm)$", ""));
                int unit = compiled.data & TypedValue.COMPLEX_UNIT_MASK;
                assertEquals(testCase.literal + " raw AAPT2 complex value", Integer.valueOf(compiled.data),
                        encode.invoke(null, number, unit));
                checks++;
            }
        }
        // The preview rejects values outside the signed complex representation. AAPT2 may
        // accept and wrap these literals; this is our range policy, not compiler rejection.
        for (float number : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                Float.MAX_VALUE, -Float.MAX_VALUE, 8388608f, 1e10f, 1.5e12f, -1.5e12f}) {
            assertNull("Unencodable number " + number, encode.invoke(null, number, TypedValue.COMPLEX_UNIT_DIP));
        }
        Method parse = PreviewPadding.class.getDeclaredMethod("dimension", Context.class, String.class);
        parse.setAccessible(true);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (String literal : new String[]{"1e99dp", "-1e99px", "NaNdp", "Infinitydp", "1e13dp", "-1e13dp",
                "8388608px", "1e10px", "1.5e12px", "-1.5e12px"}) {
            assertNull("Invalid literal " + literal, parse.invoke(null, context, literal));
        }
        status("f16_raw_complex_checks", checks);
    }

    @Test
    public void ordinaryPaddingMatchesCompiledDimensions() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                for (float fontScale : FONT_SCALES) {
                    Environment environment = new Environment(dpi, fontScale);
                    View[] views = {new ItemLinearLayout(environment.preview), new ItemTextView(environment.preview)};
                    for (DimensionCase testCase : CASES) {
                        int expected = environment.pixels(testCase);
                        for (View view : views) {
                            apply(view, testCase.literal);
                            assertPadding(environment.label(testCase, view), expected, view);
                            checks++;
                        }
                    }
                }
            }
            status("f16_ordinary_padding_checks", checks);
        });
    }

    @Test
    public void cardContentPaddingMatchesCompiledDimensions() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                for (float fontScale : FONT_SCALES) {
                    Environment environment = new Environment(dpi, fontScale);
                    ItemCardView card = new ItemCardView(environment.preview);
                    for (DimensionCase testCase : CASES) {
                        apply(card, testCase.literal);
                        assertPadding(environment.label(testCase, card), environment.pixels(testCase), card);
                        checks++;
                    }
                }
            }
            status("f16_card_padding_checks", checks);
        });
    }

    @Test
    public void replacingBeanClearsPreviousPadding() {
        onMain(() -> {
            int transitions = 0;
            for (int dpi : DENSITIES) {
                for (float fontScale : FONT_SCALES) {
                    Environment environment = new Environment(dpi, fontScale);
                    View[] views = {new ItemLinearLayout(environment.preview), new ItemTextView(environment.preview),
                            new ItemCardView(environment.preview)};
                    for (DimensionCase testCase : CASES) {
                        for (View view : views) {
                            String label = environment.label(testCase, view);
                            ViewBean previous = apply(view, testCase.literal);
                            String previousInjection = previous.inject;
                            assertPadding(label + " before replacement", environment.pixels(testCase), view);
                            apply(view, "0px");
                            assertPadding(label + " after replacement", 0, view);
                            assertEquals("Applying a new bean must not mutate its predecessor", previousInjection, previous.inject);
                            transitions++;
                        }
                    }
                }
            }
            status("f16_bean_replacement_transitions", transitions);
        });
    }

    private static ViewBean apply(View view, String literal) {
        ViewBean bean = new ViewBean("dimension_subject", ViewBean.VIEW_TYPE_LAYOUT_LINEAR);
        String attribute = view instanceof ItemCardView ? "app:contentPadding" : "android:padding";
        bean.inject = " " + attribute + "=\"" + literal + "\"";
        ((ItemView) view).setBean(bean);
        PreviewPadding.apply(view, bean);
        return bean;
    }

    private static void assertPadding(String label, int expected, View view) {
        int[] actual;
        if (view instanceof ItemCardView card) {
            actual = new int[]{card.getContentPaddingLeft(), card.getContentPaddingTop(),
                    card.getContentPaddingRight(), card.getContentPaddingBottom()};
        } else {
            actual = new int[]{view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom()};
        }
        assertArrayEquals(label, new int[]{expected, expected, expected, expected}, actual);
    }

    private static DimensionCase dimension(String name, String literal) {
        return new DimensionCase("preview_encoding_" + name, literal);
    }

    private static final class DimensionCase {
        final String resourceName;
        final String literal;

        DimensionCase(String resourceName, String literal) {
            this.resourceName = resourceName;
            this.literal = literal;
        }
    }

    private static final class Environment {
        final int dpi;
        final float fontScale;
        final Context preview;
        final Context oracle;

        Environment(int dpi, float fontScale) {
            this.dpi = dpi;
            this.fontScale = fontScale;
            Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Context test = InstrumentationRegistry.getInstrumentation().getContext();
            preview = new ContextThemeWrapper(configured(target, dpi, fontScale), R.style.Theme_SketchwarePro);
            oracle = configured(test, dpi, fontScale);
            assertEquals(dpi, preview.getResources().getConfiguration().densityDpi);
            assertEquals(fontScale, preview.getResources().getConfiguration().fontScale, 0f);
            assertEquals(preview.getResources().getDisplayMetrics().density,
                    oracle.getResources().getDisplayMetrics().density, 0f);
            assertEquals(preview.getResources().getDisplayMetrics().scaledDensity,
                    oracle.getResources().getDisplayMetrics().scaledDensity, 0f);
            assertEquals(preview.getResources().getDisplayMetrics().xdpi,
                    oracle.getResources().getDisplayMetrics().xdpi, 0f);
        }

        int pixels(DimensionCase testCase) {
            return oracle.getResources().getDimensionPixelSize(resourceId(oracle, testCase));
        }

        String label(DimensionCase testCase, View view) {
            return testCase.literal + " at " + dpi + " dpi, fontScale=" + fontScale
                    + ", " + view.getClass().getSimpleName();
        }
    }

    private static int resourceId(Context context, DimensionCase testCase) {
        int id = context.getResources().getIdentifier(testCase.resourceName, "dimen", context.getPackageName());
        assertNotEquals("Missing AAPT2 oracle resource " + testCase.resourceName, 0, id);
        return id;
    }

    private static Context configured(Context base, int dpi, float fontScale) {
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.densityDpi = dpi;
        configuration.fontScale = fontScale;
        return base.createConfigurationContext(configuration);
    }

    private static void status(String key, int value) {
        Bundle bundle = new Bundle();
        bundle.putString(key, Integer.toString(value));
        InstrumentationRegistry.getInstrumentation().sendStatus(0, bundle);
    }

    private static void onMain(Runnable check) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                check.run();
            } catch (Throwable failure) {
                error.set(failure);
            }
        });
        if (error.get() != null) throw new AssertionError(error.get());
    }
}
