package pro.sketchware.activities.editor.view;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.activities.editor.view.item.ItemCardView;
import pro.sketchware.activities.editor.view.item.ItemImageView;
import pro.sketchware.activities.editor.view.item.ItemLinearLayout;
import pro.sketchware.activities.editor.view.item.ItemTextView;
import pro.sketchware.beans.ViewBean;

/** Padding rules are compared with Android inflation of AAPT2-compiled attributes. */
@RunWith(AndroidJUnit4.class)
public class PreviewNegativePaddingTest {
    private static final int[] DENSITIES = {160, 240, 320, 420, 480};
    private static final int[] DIRECTIONS = {View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL,
            View.LAYOUT_DIRECTION_LTR};
    private static final String[][] CASES = {
        {"uniform_half", " android:padding=\"-0.5dp\""},
        {"uniform_negative", " android:padding=\"-2dp\""},
        {"left", " android:paddingLeft=\"-2dp\""},
        {"right", " android:paddingRight=\"-2dp\""},
        {"top", " android:paddingTop=\"-2dp\""},
        {"bottom", " android:paddingBottom=\"-2dp\""},
        {"four_sides", " android:paddingLeft=\"-2dp\" android:paddingTop=\"-2dp\" android:paddingRight=\"-2dp\" android:paddingBottom=\"-2dp\""},
        {"horizontal", " android:paddingHorizontal=\"-2dp\""},
        {"vertical", " android:paddingVertical=\"-2dp\""},
        {"start", " android:paddingStart=\"-2dp\""},
        {"end", " android:paddingEnd=\"-2dp\""},
        {"relative_pair", " android:paddingStart=\"-2dp\" android:paddingEnd=\"-0.5dp\""},
        {"uniform_and_left", " android:padding=\"-2dp\" android:paddingLeft=\"7dp\""},
        {"uniform_and_horizontal", " android:padding=\"-2dp\" android:paddingHorizontal=\"7dp\""},
        {"uniform_and_start", " android:padding=\"-2dp\" android:paddingStart=\"7dp\""},
        {"uniform_and_negative_start", " android:padding=\"-2dp\" android:paddingStart=\"-0.5dp\""},
        {"left_and_start", " android:paddingLeft=\"-2dp\" android:paddingStart=\"7dp\""},
        {"horizontal_and_left", " android:paddingHorizontal=\"-2dp\" android:paddingLeft=\"7dp\""},
        {"horizontal_and_start", " android:paddingHorizontal=\"-2dp\" android:paddingStart=\"7dp\""},
        {"positive_uniform", " android:padding=\"7dp\" android:paddingLeft=\"-2dp\" android:paddingTop=\"-2dp\" android:paddingStart=\"-0.5dp\""},
        {"positive_horizontal", " android:paddingHorizontal=\"7dp\" android:paddingLeft=\"-2dp\" android:paddingRight=\"-2dp\""},
        {"explicit_zero", " android:padding=\"0dp\" android:paddingLeft=\"-2dp\" android:paddingStart=\"-0.5dp\""},
        {"positive_only", " android:padding=\"7dp\""},
        {"empty", ""},
        {"start_sentinel", " android:paddingStart=\"-6000000in\""},
        {"end_sentinel", " android:paddingEnd=\"-6000000in\""}
    };

    @Test public void negativePaddingMatchesCompiledLayoutsAcrossDirections() {
        onMain(() -> {
            int checks = 0;
            for (boolean supportsRtl : new boolean[]{false, true}) {
                setSupportsRtl(supportsRtl);
                for (int dpi : DENSITIES) {
                    Environment environment = new Environment(dpi);
                    LinearLayout compiled = environment.inflate();
                    LinearLayout preview = new LinearLayout(environment.preview);
                    List<View> oracles = new ArrayList<>();
                    List<View> subjects = new ArrayList<>();
                    for (boolean background : new boolean[]{false, true}) {
                        for (String[] spec : CASES) {
                            View oracle = environment.subject(compiled, spec[0], background);
                            for (View view : widgets(environment.preview)) {
                                copyBackground(oracle, view);
                                PreviewPadding.apply(view, bean(spec[1]));
                                preview.addView(view, new LinearLayout.LayoutParams(100, 100));
                                subjects.add(view);
                                oracles.add(oracle);
                            }
                        }
                    }
                    for (int direction : DIRECTIONS) {
                        layout(compiled, direction);
                        layout(preview, direction);
                        for (int i = 0; i < subjects.size(); i++) {
                            View view = subjects.get(i);
                            View oracle = oracles.get(i);
                            assertArrayEquals(view.getClass().getSimpleName() + " dpi=" + dpi
                                    + " supportsRtl=" + supportsRtl + " direction=" + direction,
                                    padding(oracle), padding(view));
                            checks++;
                        }
                    }
                }
            }
            status("negative_padding_compiled_checks", checks);
        });
    }

    @Test public void applyingNewAttributesClearsOldRelativePadding() {
        onMain(() -> {
            int checks = 0;
            for (boolean supportsRtl : new boolean[]{false, true}) {
                setSupportsRtl(supportsRtl);
                for (int dpi : DENSITIES) {
                    Environment environment = new Environment(dpi);
                    LinearLayout compiled = environment.inflate();
                    for (boolean background : new boolean[]{false, true}) {
                        for (View view : widgets(environment.preview)) {
                            copyBackground(environment.subject(compiled, "empty", background), view);
                            LinearLayout parent = new LinearLayout(environment.preview);
                            parent.addView(view, new LinearLayout.LayoutParams(100, 100));
                            for (String name : new String[]{"horizontal_and_start", "positive_only", "empty"}) {
                                String inject = null;
                                for (String[] spec : CASES) if (spec[0].equals(name)) inject = spec[1];
                                assertNotNull(inject);
                                PreviewPadding.apply(view, bean(inject));
                                for (int direction : DIRECTIONS) {
                                    layout(compiled, direction);
                                    layout(parent, direction);
                                    assertArrayEquals(name + " dpi=" + dpi + " supportsRtl=" + supportsRtl,
                                            padding(environment.subject(compiled, name, background)), padding(view));
                                    checks++;
                                }
                            }
                        }
                    }
                }
            }
            status("negative_padding_reapply_checks", checks);
        });
    }

    @Test public void cardContentPaddingRetainsNegativeSideOverrides() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                Environment environment = new Environment(dpi);
                int negative = environment.dimension("negative_padding_dp");
                int half = environment.dimension("negative_padding_half_dp");
                int halfPx = environment.dimension("negative_padding_half_px");
                int positive = environment.dimension("negative_padding_positive");
                String[] injections = {
                    " app:contentPadding=\"-2dp\"",
                    " app:contentPaddingLeft=\"-2dp\"",
                    " app:contentPaddingRight=\"-2dp\"",
                    " app:contentPaddingTop=\"-2dp\"",
                    " app:contentPaddingBottom=\"-2dp\"",
                    " app:contentPaddingLeft=\"-2dp\" app:contentPaddingTop=\"-2dp\" app:contentPaddingRight=\"-2dp\" app:contentPaddingBottom=\"-2dp\"",
                    " app:contentPadding=\"-2dp\" app:contentPaddingLeft=\"7dp\"",
                    " app:contentPadding=\"7dp\" app:contentPaddingLeft=\"-2dp\"",
                    " app:contentPadding=\"-0.5dp\" app:contentPaddingRight=\"-0.5px\""
                };
                int[][] expected = {{negative, negative, negative, negative}, {negative, 0, 0, 0},
                        {0, 0, negative, 0}, {0, negative, 0, 0}, {0, 0, 0, negative},
                        {negative, negative, negative, negative}, {positive, negative, negative, negative},
                        {negative, positive, positive, positive}, {half, half, halfPx, half}};
                ItemCardView card = new ItemCardView(environment.preview);
                for (int i = 0; i < injections.length; i++) {
                    PreviewPadding.apply(card, bean(injections[i]));
                    assertArrayEquals(injections[i] + " dpi=" + dpi, expected[i], contentPadding(card));
                    checks++;
                }
                PreviewPadding.apply(card, bean(""));
                assertArrayEquals(new int[4], contentPadding(card));
                checks++;
            }
            status("negative_card_padding_checks", checks);
        });
    }

    private static List<View> widgets(Context context) {
        return List.of(new ItemLinearLayout(context), new ItemTextView(context), new ItemImageView(context));
    }

    private static void copyBackground(View oracle, View preview) {
        Drawable background = oracle.getBackground();
        preview.setBackground(background == null ? null : background.getConstantState()
                .newDrawable(preview.getResources()).mutate());
    }

    private static ViewBean bean(String inject) {
        ViewBean bean = new ViewBean("subject", ViewBean.VIEW_TYPE_LAYOUT_LINEAR);
        bean.layout.paddingLeft = bean.layout.paddingTop = bean.layout.paddingRight = bean.layout.paddingBottom = 0;
        bean.inject = inject;
        return bean;
    }

    private static int[] padding(View view) {
        return new int[]{view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom()};
    }

    private static int[] contentPadding(ItemCardView view) {
        return new int[]{view.getContentPaddingLeft(), view.getContentPaddingTop(),
                view.getContentPaddingRight(), view.getContentPaddingBottom()};
    }

    private static void layout(View view, int direction) {
        view.setLayoutDirection(direction);
        view.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, 600, 600);
    }

    private static final class Environment {
        final Context preview;
        final Context oracle;

        Environment(int dpi) {
            preview = new ContextThemeWrapper(configured(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(), dpi), R.style.Theme_SketchwarePro);
            oracle = new ContextThemeWrapper(configured(
                    InstrumentationRegistry.getInstrumentation().getContext(), dpi), android.R.style.Theme_Material_Light_NoActionBar);
            assertEquals(dpi, preview.getResources().getDisplayMetrics().densityDpi);
            assertEquals(dpi, oracle.getResources().getDisplayMetrics().densityDpi);
        }

        LinearLayout inflate() {
            return (LinearLayout) LayoutInflater.from(oracle).inflate(resource("preview_negative_padding", "layout"), null, false);
        }

        View subject(View root, String name, boolean background) {
            View view = root.findViewById(resource("negative_" + (background ? "background_" : "") + name, "id"));
            assertNotNull(view);
            return view;
        }

        int dimension(String name) {
            return oracle.getResources().getDimensionPixelSize(resource(name, "dimen"));
        }

        private int resource(String name, String type) {
            int id = oracle.getResources().getIdentifier(name, type, oracle.getPackageName());
            assertNotEquals("Missing compiled oracle: " + name, 0, id);
            return id;
        }
    }

    private static Context configured(Context base, int dpi) {
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.densityDpi = dpi;
        return base.createConfigurationContext(config);
    }

    private static void setSupportsRtl(boolean supported) {
        for (Context context : List.of(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                InstrumentationRegistry.getInstrumentation().getContext())) {
            ApplicationInfo info = context.getApplicationInfo();
            info.flags = supported ? info.flags | ApplicationInfo.FLAG_SUPPORTS_RTL : info.flags & ~ApplicationInfo.FLAG_SUPPORTS_RTL;
        }
    }

    private static void status(String key, int value) {
        Bundle bundle = new Bundle();
        bundle.putInt(key, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, bundle);
    }

    private static void onMain(Runnable check) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ApplicationInfo target = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationInfo();
            ApplicationInfo test = InstrumentationRegistry.getInstrumentation().getContext().getApplicationInfo();
            int targetFlags = target.flags;
            int testFlags = test.flags;
            try { check.run(); } catch (Throwable failure) { error.set(failure); }
            finally { target.flags = targetFlags; test.flags = testFlags; }
        });
        if (error.get() != null) throw new AssertionError(error.get());
    }
}
