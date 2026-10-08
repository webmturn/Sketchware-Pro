package pro.sketchware.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;

@RunWith(AndroidJUnit4.class)
public class CircleImageViewTest {
    @Test
    public void circlePreservesSourceColorsAndClearsCorners() {
        onMainThread(() -> {
            Bitmap source = quadrants(64, 64);
            Bitmap result = new CircleImageView(context()).createCircleBitmap(source);
            assertQuadrants(result);
            assertEquals(0, Color.alpha(result.getPixel(0, 0)));
            assertEquals(0, Color.alpha(result.getPixel(63, 63)));
            assertQuadrants(source);
            assertFalse(source.isRecycled());
        });
    }

    @Test
    public void sourceTransparencyIsPreservedInsideCircle() {
        onMainThread(() -> {
            Bitmap source = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
            source.eraseColor(Color.argb(128, 255, 0, 0));
            source.setPixel(32, 32, Color.TRANSPARENT);
            Bitmap result = new CircleImageView(context()).createCircleBitmap(source);
            assertEquals(Color.argb(128, 255, 0, 0), result.getPixel(24, 24));
            assertEquals(0, Color.alpha(result.getPixel(32, 32)));
            assertEquals(0, Color.alpha(result.getPixel(0, 0)));
        });
    }

    @Test
    public void resizedViewPreservesImageColors() {
        onMainThread(() -> {
            CircleImageView view = new CircleImageView(context());
            view.setImageBitmap(quadrants(64, 64));
            Bitmap result = draw(view, 128, 128);
            assertQuadrants(result);
            assertEquals(0, Color.alpha(result.getPixel(0, 0)));
        });
    }

    @Test
    public void libraryProjectRowUsesSourceImagePixels() {
        onMainThread(() -> {
            View row = LayoutInflater.from(context()).inflate(
                    R.layout.manage_library_popup_project_list_item, null, false);
            CircleImageView icon = row.findViewById(R.id.img_icon);
            icon.setImageBitmap(quadrants(64, 64));
            row.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            row.layout(0, 0, row.getMeasuredWidth(), row.getMeasuredHeight());
            assertQuadrants(draw(icon, icon.getWidth(), icon.getHeight()));
        });
    }

    @Test
    public void rectangularImageKeepsExistingDimensionsAndMask() {
        onMainThread(() -> {
            Bitmap source = quadrants(80, 48);
            Bitmap result = new CircleImageView(context()).createCircleBitmap(source);
            assertEquals(80, result.getWidth());
            assertEquals(48, result.getHeight());
            assertEquals(Color.RED, result.getPixel(32, 16));
            assertEquals(Color.GREEN, result.getPixel(48, 16));
            assertEquals(Color.YELLOW, result.getPixel(32, 32));
            assertEquals(Color.MAGENTA, result.getPixel(48, 32));
            assertEquals(0, Color.alpha(result.getPixel(0, 24)));
            assertEquals(0, Color.alpha(result.getPixel(79, 24)));
        });
    }

    @Test
    public void missingDrawableDrawsNothing() {
        onMainThread(() -> {
            Bitmap result = draw(new CircleImageView(context()), 64, 64);
            assertEquals(0, result.getPixel(32, 32));
        });
    }

    private static Context context() {
        return new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                R.style.Theme_SketchwarePro);
    }

    private static Bitmap quadrants(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bitmap.setPixel(x, y, y < height / 2
                        ? (x < width / 2 ? Color.RED : Color.GREEN)
                        : (x < width / 2 ? Color.YELLOW : Color.MAGENTA));
            }
        }
        return bitmap;
    }

    private static void assertQuadrants(Bitmap bitmap) {
        int left = bitmap.getWidth() * 3 / 8;
        int right = bitmap.getWidth() * 5 / 8;
        int top = bitmap.getHeight() * 3 / 8;
        int bottom = bitmap.getHeight() * 5 / 8;
        assertEquals(Color.RED, bitmap.getPixel(left, top));
        assertEquals(Color.GREEN, bitmap.getPixel(right, top));
        assertEquals(Color.YELLOW, bitmap.getPixel(left, bottom));
        assertEquals(Color.MAGENTA, bitmap.getPixel(right, bottom));
    }

    private static Bitmap draw(View view, int width, int height) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, width, height);
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(result));
        return result;
    }

    private static void onMainThread(Runnable action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                action.run();
            } catch (Throwable error) {
                failure.set(error);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("Circle rendering failed on the main thread", failure.get());
        }
    }
}
