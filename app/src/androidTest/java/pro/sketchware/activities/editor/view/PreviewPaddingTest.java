package pro.sketchware.activities.editor.view;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.LinearLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import pro.sketchware.R;
import pro.sketchware.activities.editor.view.item.*;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.layout.PaddingAttributes;
import pro.sketchware.graphics.NinePatchDecoder;

@RunWith(AndroidJUnit4.class)
public class PreviewPaddingTest {
    private static final String[] PIXEL_WIDGETS = {
        "ItemAdView",
        "ItemAnalogClock",
        "ItemAutoCompleteTextView",
        "ItemBadgeView",
        "ItemBottomNavigationView",
        "ItemButton",
        "ItemCalendarView",
        "ItemCheckBox",
        "ItemCircleImageView",
        "ItemCodeView",
        "ItemDatePicker",
        "ItemDigitalClock",
        "ItemEditText",
        "ItemGridView",
        "ItemHorizontalScrollView",
        "ItemImageView",
        "ItemLinearLayout",
        "ItemListView",
        "ItemLottieAnimation",
        "ItemMapView",
        "ItemMaterialButton",
        "ItemMultiAutoCompleteTextView",
        "ItemOTPView",
        "ItemPatternLockView",
        "ItemProgressBar",
        "ItemRadioButton",
        "ItemRatingBar",
        "ItemRecyclerView",
        "ItemRelativeLayout",
        "ItemSearchView",
        "ItemSeekBar",
        "ItemSignInButton",
        "ItemSpinner",
        "ItemSwitch",
        "ItemTabLayout",
        "ItemTextView",
        "ItemTimePicker",
        "ItemVerticalScrollView",
        "ItemVideoView",
        "ItemViewPager",
        "ItemWaveSideBar",
        "ItemWebView",
        "ItemYoutubePlayer"
    };

    @Test public void pixelEntryDoesNotConvertAgain() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : new int[]{160, 240, 320, 420, 480}) {
                Context context = context(dpi);
                for (String name : PIXEL_WIDGETS) {
                    View view = (View) Class.forName("pro.sketchware.activities.editor.view.item." + name)
                            .getConstructor(Context.class).newInstance(context);
                    ((ItemView) view).setPaddingPixels(7, 11, 13, 17);
                    assertArrayEquals(name + " at " + dpi, new int[]{7, 11, 13, 17}, padding(view));
                    checks++;
                }
            }
            status("f14_pixel_checks", Integer.toString(checks));
        });
    }

    @Test public void relativePaddingTracksParentDirection() {
        onMain(() -> {
            Context context = context(420);
            ApplicationInfo info = context.getApplicationInfo();
            int flags = info.flags;
            info.flags |= ApplicationInfo.FLAG_SUPPORTS_RTL;
            try {
                for (View view : Arrays.asList(new ItemLinearLayout(context), new ItemTextView(context), new ItemImageView(context))) {
                    ViewBean bean = bean();
                    bean.inject = " android:paddingStart=\"13dp\"";
                    PreviewPadding.apply(view, bean);
                    LinearLayout parent = new LinearLayout(context);
                    parent.addView(view);
                    for (int direction : new int[]{View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL, View.LAYOUT_DIRECTION_LTR}) {
                        parent.setLayoutDirection(direction);
                        parent.measure(View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY));
                        parent.layout(0, 0, 300, 300);
                        assertEquals(direction, view.getLayoutDirection());
                        assertArrayEquals(view.getClass().getSimpleName(), direction == View.LAYOUT_DIRECTION_RTL
                                ? new int[]{18, 8, 34, 24} : new int[]{34, 8, 24, 24}, padding(view));
                    }
                }
            } finally { info.flags = flags; }
        });
    }

    @Test public void cardContentPaddingUsesPixelsAndSideOverrides() {
        onMain(() -> {
            ItemCardView card = new ItemCardView(context(420));
            ViewBean bean = bean();
            bean.inject = " app:contentPadding=\"6dp\" app:contentPaddingLeft=\"0dp\"";
            PreviewPadding.apply(card, bean);
            assertArrayEquals(new int[]{0, 8, 24, 24}, new int[]{card.getContentPaddingLeft(), card.getContentPaddingTop(),
                    card.getContentPaddingRight(), card.getContentPaddingBottom()});
            bean.layout.paddingLeft = bean.layout.paddingTop = bean.layout.paddingRight = bean.layout.paddingBottom = 0;
            PreviewPadding.apply(card, bean);
            assertArrayEquals(new int[]{0, 16, 16, 16}, new int[]{card.getContentPaddingLeft(), card.getContentPaddingTop(),
                    card.getContentPaddingRight(), card.getContentPaddingBottom()});
        });
    }

    @Test public void legacyDpEntryRemainsUnchanged() {
        onMain(() -> {
            ItemLinearLayout linear = new ItemLinearLayout(context(420));
            linear.setPadding(4, 4, 4, 4);
            assertArrayEquals(new int[]{10, 10, 10, 10}, padding(linear));
            ItemCardView card = new ItemCardView(context(420));
            card.setContentPadding(6, 6, 6, 6);
            assertEquals(15, card.getContentPaddingLeft());
        });
    }

    @Test public void namespaceAliasesAndExplicitZeroAreRetained() {
        ViewBean bean = bean();
        bean.inject = " xmlns:a=\"http://schemas.android.com/apk/res/android\" a:padding=\"0dp\" tools:replace=\"android:paddingLeft\"";
        Map<String, String> attributes = PaddingAttributes.effective(bean, false);
        assertEquals("0dp", attributes.get("android:padding"));
        assertFalse(attributes.containsKey("android:paddingLeft"));
        assertTrue(PaddingAttributes.attributesToReplace(bean).contains("android:paddingLeft"));
    }

    @Test public void ninePatchPaddingUsesSourceDensity() {
        onMain(() -> {
            File root = new File(context(320).getCacheDir(), "F14Fix.Pixel." + UUID.randomUUID());
            assertTrue(root.mkdir());
            status("f14_owned_fixture", root.getAbsolutePath());
            File file = new File(root, "source.9.png");
            try {
                try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("ninepatch/asymmetric.9.png")) {
                    Files.copy(input, file.toPath());
                }
                for (int dpi : new int[]{160, 240, 320, 420, 480}) {
                    Drawable drawable = NinePatchDecoder.decodeDrawable(context(dpi).getResources(), file.getAbsolutePath(), 320);
                    Rect padding = new Rect();
                    assertTrue(drawable.getPadding(padding));
                    assertEquals(Math.round(dpi / 320f), padding.left);
                    assertEquals(Math.round(2 * dpi / 320f), padding.top);
                    assertEquals(padding.top, padding.right);
                    assertEquals(padding.left, padding.bottom);
                }
            } finally {
                if (file.exists()) assertTrue(file.delete());
                assertTrue(root.delete());
                status("f14_cleaned_fixture", root.getAbsolutePath());
            }
        });
    }

    private static ViewBean bean() {
        ViewBean bean = new ViewBean("subject", ViewBean.VIEW_TYPE_LAYOUT_LINEAR);
        bean.layout.paddingLeft = 7; bean.layout.paddingTop = 3;
        bean.layout.paddingRight = 9; bean.layout.paddingBottom = 9;
        return bean;
    }
    private static Context context(int dpi) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.densityDpi = dpi;
        return new ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_SketchwarePro);
    }
    private static int[] padding(View view) {
        return new int[]{view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom()};
    }
    private static void status(String name, String value) {
        Bundle bundle = new Bundle(); bundle.putString(name, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, bundle);
    }
    private interface Check { void run() throws Exception; }
    private static void onMain(Check check) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try { check.run(); } catch (Throwable failure) { error.set(failure); }
        });
        if (error.get() != null) throw new AssertionError(error.get());
    }
}
