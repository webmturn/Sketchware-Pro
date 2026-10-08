package pro.sketchware.graphics;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Arrays;

/** Compares the runtime decoder with AAPT2 output from the identical source PNG. */
@RunWith(AndroidJUnit4.class)
public class NinePatchDecoderTest {
    private static final String[] FIXTURES = {
            "asymmetric", "multiple", "full", "implicit_padding", "white_border"};

    @Test
    public void generatedChunkMatchesAaptReference() throws Exception {
        for (String name : FIXTURES) {
            byte[] png = raw(name);
            Bitmap source = BitmapFactory.decodeByteArray(png, 0, png.length);
            byte[] actual = NinePatchDecoder.buildNinePatchChunk(source);
            byte[] expected = reference(name).getNinePatchChunk();
            status("ninepatch_chunk", name + ": expected=" + describe(expected) + "; actual=" + describe(actual));
            assertChunkEquals(expected, actual);
        }
    }

    @Test
    public void paddingMatchesAaptReference() throws Exception {
        for (String name : FIXTURES) {
            byte[] png = raw(name);
            Bitmap source = BitmapFactory.decodeByteArray(png, 0, png.length);
            byte[] actual = new byte[32];
            NinePatchDecoder.extractPadding(source, actual);
            Rect expected = padding(reference(name).getNinePatchChunk());
            status("ninepatch_padding", name + ": expected=" + expected + "; actual=" + padding(actual));
            assertEquals(name, expected, padding(actual));
        }
    }

    @Test
    public void rawDecodeKeepsInteriorPixelsAndAttachesValidChunk() throws Exception {
        for (String name : FIXTURES) {
            Bitmap actual = NinePatchDecoder.decode(new ByteArrayInputStream(raw(name)));
            Bitmap expected = reference(name);
            assertPixelsEqual(expected, actual);
            assertChunkEquals(expected.getNinePatchChunk(), actual.getNinePatchChunk());
            assertTrue(NinePatch.isNinePatchChunk(actual.getNinePatchChunk()));
        }
    }

    @Test
    public void nativeRenderingAndPaddingMatchAaptReference() throws Exception {
        for (String name : FIXTURES) {
            Bitmap expected = reference(name);
            Bitmap actual = NinePatchDecoder.decode(new ByteArrayInputStream(raw(name)));
            // Validate structure before passing bytes to the native NinePatch constructor.
            assertChunkEquals(expected.getNinePatchChunk(), actual.getNinePatchChunk());
            NinePatchDrawable reference = drawable(expected);
            NinePatchDrawable decoded = drawable(actual);
            Rect expectedPadding = new Rect();
            Rect actualPadding = new Rect();
            reference.getPadding(expectedPadding);
            decoded.getPadding(actualPadding);
            assertEquals(name, expectedPadding, actualPadding);
            // Diagnose the current ViewPane call separately; do not make its empty padding a contract.
            NinePatchDrawable previewConstructor = new NinePatchDrawable(testContext().getResources(),
                    actual, actual.getNinePatchChunk(), new Rect(), null);
            Rect previewPadding = new Rect();
            previewConstructor.getPadding(previewPadding);
            status("ninepatch_preview_constructor", name + ": expected=" + expectedPadding
                    + "; current ViewPane argument=" + previewPadding);
            for (int[] size : new int[][]{{expected.getWidth(), expected.getHeight()},
                    {40, 31}, {71, 23}, {2, 2}}) {
                assertPixelsEqual(render(reference, size[0], size[1]), render(decoded, size[0], size[1]));
            }
            status("ninepatch_native", name + ": four render sizes and padding match AAPT2");
        }
    }

    @Test
    public void compiledPngIsNotCroppedOrReinterpreted() throws Exception {
        for (String name : FIXTURES) {
            try (InputStream input = compiled(name)) {
                Bitmap actual = NinePatchDecoder.decode(input);
                Bitmap expected = reference(name);
                status("ninepatch_compiled", name + ": expected=" + expected.getWidth() + "x"
                        + expected.getHeight() + "; actual=" + actual.getWidth() + "x" + actual.getHeight());
                assertPixelsEqual(expected, actual);
                assertArrayEquals(expected.getNinePatchChunk(), actual.getNinePatchChunk());
            }
        }
    }

    @Test
    public void decodeFileUsesSamePathAndReleasesInputFile() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File fixture = File.createTempFile("F06NinePatch.", ".9.png", context.getCacheDir());
        status("ninepatch_owned_fixture", fixture.getAbsolutePath());
        try {
            Files.write(fixture.toPath(), raw("asymmetric"));
            Bitmap decoded = NinePatchDecoder.decodeFile(fixture.getAbsolutePath());
            assertPixelsEqual(reference("asymmetric"), decoded);
            assertChunkEquals(reference("asymmetric").getNinePatchChunk(), decoded.getNinePatchChunk());
        } finally {
            assertTrue(fixture.delete());
            status("ninepatch_cleaned_fixture", fixture.getAbsolutePath());
        }
    }

    @Test
    public void transparentInteriorPixelsSurviveNativeRendering() throws Exception {
        byte[] png = raw("asymmetric");
        Bitmap source = BitmapFactory.decodeByteArray(png, 0, png.length).copy(Bitmap.Config.ARGB_8888, true);
        source.setPixel(4, 3, Color.TRANSPARENT);
        source.setPixel(3, 3, Color.argb(128, 255, 0, 0));
        Bitmap decoded = NinePatchDecoder.decode(new ByteArrayInputStream(png(source)));
        assertChunkEquals(reference("asymmetric").getNinePatchChunk(), decoded.getNinePatchChunk());
        Bitmap rendered = render(drawable(decoded), decoded.getWidth(), decoded.getHeight());
        assertEquals(0, Color.alpha(rendered.getPixel(3, 2)));
        assertEquals(Color.argb(128, 255, 0, 0), rendered.getPixel(2, 2));
    }

    @Test
    public void tooSmallAndUnmarkedImagesKeepOrdinaryBitmapBehavior() throws Exception {
        for (int size : new int[]{1, 2, 5}) {
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.GREEN);
            Bitmap decoded = NinePatchDecoder.decode(new ByteArrayInputStream(png(bitmap)));
            assertPixelsEqual(bitmap, decoded);
            assertNull(decoded.getNinePatchChunk());
        }
    }

    @Test
    public void imageWithOnlyOneStretchAxisIsNotSentToNative() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_8888);
        bitmap.setPixel(2, 0, Color.BLACK);
        assertEquals(0, NinePatchDecoder.buildNinePatchChunk(bitmap).length);
        Bitmap decoded = NinePatchDecoder.decode(new ByteArrayInputStream(png(bitmap)));
        assertPixelsEqual(bitmap, decoded);
        assertNull(decoded.getNinePatchChunk());
    }

    @Test
    public void unreadableInputReturnsNoBitmap() throws Exception {
        assertNull(NinePatchDecoder.decode(new ByteArrayInputStream(new byte[0])));
        assertNull(NinePatchDecoder.decode(new ByteArrayInputStream(new byte[]{1, 2, 3})));
    }

    @Test
    public void callerOwnedStreamRemainsOpen() throws Exception {
        TrackingStream stream = new TrackingStream(raw("asymmetric"));
        assertNotNull(NinePatchDecoder.decode(stream));
        assertFalse(stream.closed);
    }

    @Test
    public void oversizedChunkCountsAreRejectedBeforeNativeUse() throws Exception {
        for (int[] size : new int[][]{{300, 3}, {32, 32}}) {
            Bitmap bitmap = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888);
            for (int x = 1; x < size[0] - 1; x += 2) bitmap.setPixel(x, 0, Color.BLACK);
            for (int y = 1; y < size[1] - 1; y += 2) bitmap.setPixel(0, y, Color.BLACK);
            try {
                NinePatchDecoder.buildNinePatchChunk(bitmap);
                fail("Expected oversized NinePatch rejection");
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains("regions"));
            }
        }
    }

    @Test
    public void minimumThreeByThreeSourceRendersOnePixel() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(3, 3, Bitmap.Config.ARGB_8888);
        bitmap.setPixel(1, 0, Color.BLACK);
        bitmap.setPixel(0, 1, Color.BLACK);
        bitmap.setPixel(1, 1, Color.RED);
        Bitmap decoded = NinePatchDecoder.decode(new ByteArrayInputStream(png(bitmap)));
        assertEquals(1, decoded.getWidth());
        assertEquals(1, decoded.getHeight());
        byte[] chunk = decoded.getNinePatchChunk();
        assertEquals(2, chunk[1] & 255);
        assertEquals(2, chunk[2] & 255);
        assertEquals(1, chunk[3] & 255);
        assertArrayEquals(new int[]{0, 1, 0, 1}, divs(chunk));
        Bitmap rendered = render(drawable(decoded), 10, 10);
        assertEquals(Color.RED, rendered.getPixel(5, 5));
    }

    private static Context testContext() {
        return InstrumentationRegistry.getInstrumentation().getContext();
    }

    private static byte[] raw(String name) throws IOException {
        try (InputStream input = testContext().getAssets().open("ninepatch/" + name + ".9.png")) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static InputStream compiled(String name) {
        Resources resources = testContext().getResources();
        int id = resources.getIdentifier("f06_" + name, "drawable", testContext().getPackageName());
        assertNotEquals(0, id);
        return resources.openRawResource(id);
    }

    private static Bitmap reference(String name) throws IOException {
        try (InputStream input = compiled(name)) {
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            assertNotNull(bitmap);
            assertTrue(NinePatch.isNinePatchChunk(bitmap.getNinePatchChunk()));
            return bitmap;
        }
    }

    private static byte[] png(Bitmap bitmap) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        return output.toByteArray();
    }

    private static int[] divs(byte[] chunk) {
        int count = (chunk[1] & 255) + (chunk[2] & 255);
        int[] divs = new int[count];
        ByteBuffer buffer = ByteBuffer.wrap(chunk).order(ByteOrder.nativeOrder());
        buffer.position(32);
        buffer.asIntBuffer().get(divs);
        return divs;
    }

    private static Rect padding(byte[] chunk) {
        ByteBuffer buffer = ByteBuffer.wrap(chunk).order(ByteOrder.nativeOrder());
        return new Rect(buffer.getInt(12), buffer.getInt(20), buffer.getInt(16), buffer.getInt(24));
    }

    private static String describe(byte[] chunk) {
        return "counts=" + (chunk[1] & 255) + "," + (chunk[2] & 255) + "," + (chunk[3] & 255)
                + ", divs=" + Arrays.toString(divs(chunk)) + ", padding=" + padding(chunk);
    }

    private static void assertChunkEquals(byte[] expected, byte[] actual) {
        assertNotNull(actual);
        assertEquals(1, actual[0]);
        assertEquals(expected[1] & 255, actual[1] & 255);
        assertEquals(expected[2] & 255, actual[2] & 255);
        assertEquals(expected[3] & 255, actual[3] & 255);
        assertEquals(32 + 4 * ((actual[1] & 255) + (actual[2] & 255) + (actual[3] & 255)), actual.length);
        assertArrayEquals(divs(expected), divs(actual));
        assertEquals(padding(expected), padding(actual));
        ByteBuffer expectedBuffer = ByteBuffer.wrap(expected).order(ByteOrder.nativeOrder());
        ByteBuffer actualBuffer = ByteBuffer.wrap(actual).order(ByteOrder.nativeOrder());
        for (int offset : new int[]{4, 8, 28}) {
            assertEquals(expectedBuffer.getInt(offset), actualBuffer.getInt(offset));
        }
    }

    private static NinePatchDrawable drawable(Bitmap bitmap) {
        return new NinePatchDrawable(testContext().getResources(), bitmap, bitmap.getNinePatchChunk(),
                padding(bitmap.getNinePatchChunk()), "F06 fixture");
    }

    private static Bitmap render(NinePatchDrawable drawable, int width, int height) {
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(new Canvas(result));
        return result;
    }

    private static void assertPixelsEqual(Bitmap expected, Bitmap actual) {
        assertNotNull(actual);
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        int[] expectedPixels = new int[expected.getWidth() * expected.getHeight()];
        int[] actualPixels = new int[expectedPixels.length];
        expected.getPixels(expectedPixels, 0, expected.getWidth(), 0, 0, expected.getWidth(), expected.getHeight());
        actual.getPixels(actualPixels, 0, actual.getWidth(), 0, 0, actual.getWidth(), actual.getHeight());
        assertArrayEquals(expectedPixels, actualPixels);
    }

    private static void status(String key, String value) {
        Bundle bundle = new Bundle();
        bundle.putString(key, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, bundle);
    }

    private static class TrackingStream extends ByteArrayInputStream {
        boolean closed;

        TrackingStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
