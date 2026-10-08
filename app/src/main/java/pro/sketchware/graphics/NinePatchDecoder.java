package pro.sketchware.graphics;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.content.res.Resources;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class NinePatchDecoder {
    private static final int CHUNK_HEADER_SIZE = 32;
    private static final int PADDING_LEFT_OFFSET = 12;
    private static final int PADDING_RIGHT_OFFSET = 16;
    private static final int PADDING_TOP_OFFSET = 20;
    private static final int PADDING_BOTTOM_OFFSET = 24;
    private static final int MAX_REGION_COUNT = 127;
    private static final int MAX_DIVISION_COUNT = 255;
    private static final int NO_COLOR = 1;

    public static Bitmap decode(InputStream inputStream) throws Exception {
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        if (bitmap == null || NinePatch.isNinePatchChunk(bitmap.getNinePatchChunk())) {
            return bitmap;
        }
        byte[] chunk = buildNinePatchChunk(bitmap);
        if (chunk.length == 0) {
            return bitmap;
        }

        Bitmap interior = Bitmap.createBitmap(bitmap, 1, 1, bitmap.getWidth() - 2, bitmap.getHeight() - 2);
        Field field = Bitmap.class.getDeclaredField("mNinePatchChunk");
        field.setAccessible(true);
        field.set(interior, chunk);
        bitmap.recycle();
        return interior;
    }

    public static Bitmap decodeFile(String input) throws Exception {
        try (FileInputStream fileInputStream = new FileInputStream(input)) {
            return decode(fileInputStream);
        }
    }

    /** File resources are copied into a density-qualified drawable directory by the builder. */
    public static Drawable decodeDrawable(Resources resources, String input, int sourceDensity) throws Exception {
        Bitmap bitmap = decodeFile(input);
        if (bitmap == null) return null;
        bitmap.setDensity(sourceDensity);
        byte[] chunk = bitmap.getNinePatchChunk();
        if (!NinePatch.isNinePatchChunk(chunk)) return new BitmapDrawable(resources, bitmap);
        ByteBuffer data = ByteBuffer.wrap(chunk).order(ByteOrder.nativeOrder());
        Rect padding = new Rect(data.getInt(PADDING_LEFT_OFFSET), data.getInt(PADDING_TOP_OFFSET),
                data.getInt(PADDING_RIGHT_OFFSET), data.getInt(PADDING_BOTTOM_OFFSET));
        return new NinePatchDrawable(resources, bitmap, chunk, padding, null);
    }

    public static void extractPadding(Bitmap bitmap, byte[] data) {
        int[] horizontal = paddingBounds(edgePixels(bitmap, true, false), edgePixels(bitmap, true, true));
        int[] vertical = paddingBounds(edgePixels(bitmap, false, false), edgePixels(bitmap, false, true));
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder());
        buffer.putInt(PADDING_LEFT_OFFSET, horizontal[0]);
        buffer.putInt(PADDING_RIGHT_OFFSET, horizontal[1]);
        buffer.putInt(PADDING_TOP_OFFSET, vertical[0]);
        buffer.putInt(PADDING_BOTTOM_OFFSET, vertical[1]);
    }

    private static int[] paddingBounds(int[] paddingPixels, int[] stretchPixels) {
        List<Integer> divisions = markerDivisions(paddingPixels);
        if (divisions.isEmpty()) {
            // Android derives omitted padding from the first and last stretch regions.
            divisions = markerDivisions(stretchPixels);
        }
        return divisions.isEmpty() ? new int[]{0, 0}
                : new int[]{divisions.get(0), paddingPixels.length - divisions.get(divisions.size() - 1)};
    }

    public static void writeIntLE(OutputStream outputStream, int value) throws IOException {
        outputStream.write(value & 0xFF);
        outputStream.write(value >> 8 & 0xFF);
        outputStream.write(value >> 16 & 0xFF);
        outputStream.write(value >> 24 & 0xFF);
    }

    public static void putIntLE(byte[] data, int offset, int value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >> 8);
        data[offset + 2] = (byte) (value >> 16);
        data[offset + 3] = (byte) (value >> 24);
    }

    public static byte[] buildNinePatchChunk(Bitmap bitmap) throws IOException {
        if (bitmap.getWidth() < 3 || bitmap.getHeight() < 3) {
            return new byte[0];
        }
        int width = bitmap.getWidth() - 2;
        int height = bitmap.getHeight() - 2;
        List<Integer> horizontal = markerDivisions(edgePixels(bitmap, true, true));
        List<Integer> vertical = markerDivisions(edgePixels(bitmap, false, true));
        if (horizontal.isEmpty() || vertical.isEmpty()) {
            return new byte[0];
        }
        if (horizontal.size() > MAX_DIVISION_COUNT || vertical.size() > MAX_DIVISION_COUNT) {
            throw new IOException("Too many regions in NinePatch image");
        }
        int colorCount = segmentCount(horizontal, width) * segmentCount(vertical, height);
        if (colorCount > MAX_REGION_COUNT) {
            throw new IOException("Too many regions in NinePatch image");
        }

        int verticalOffset = CHUNK_HEADER_SIZE + horizontal.size() * Integer.BYTES;
        int colorsOffset = verticalOffset + vertical.size() * Integer.BYTES;
        ByteBuffer buffer = ByteBuffer.allocate(colorsOffset + colorCount * Integer.BYTES)
                .order(ByteOrder.nativeOrder());
        // Res_png_9patch header, followed by X divisions, Y divisions and region colors.
        buffer.put((byte) 1);
        buffer.put((byte) horizontal.size());
        buffer.put((byte) vertical.size());
        buffer.put((byte) colorCount);
        buffer.putInt(CHUNK_HEADER_SIZE);
        buffer.putInt(verticalOffset);
        buffer.putInt(0).putInt(0).putInt(0).putInt(0);
        buffer.putInt(colorsOffset);
        for (int division : horizontal) {
            buffer.putInt(division);
        }
        for (int division : vertical) {
            buffer.putInt(division);
        }
        for (int i = 0; i < colorCount; i++) {
            // No solid-color shortcut: sample the original pixels in every region.
            buffer.putInt(NO_COLOR);
        }
        byte[] chunk = buffer.array();
        extractPadding(bitmap, chunk);
        return chunk;
    }

    private static int[] edgePixels(Bitmap bitmap, boolean horizontal, boolean leading) {
        int length = (horizontal ? bitmap.getWidth() : bitmap.getHeight()) - 2;
        int[] pixels = new int[length];
        int x = horizontal ? 1 : (leading ? 0 : bitmap.getWidth() - 1);
        int y = horizontal ? (leading ? 0 : bitmap.getHeight() - 1) : 1;
        bitmap.getPixels(pixels, 0, horizontal ? length : 1, x, y,
                horizontal ? length : 1, horizontal ? 1 : length);
        return pixels;
    }

    private static List<Integer> markerDivisions(int[] pixels) {
        List<Integer> divisions = new ArrayList<>();
        boolean previousMarked = false;
        for (int i = 0; i < pixels.length; i++) {
            boolean marked = pixels[i] == Color.BLACK;
            if (marked != previousMarked) {
                divisions.add(i);
                previousMarked = marked;
            }
        }
        if (previousMarked) {
            divisions.add(pixels.length);
        }
        return divisions;
    }

    private static int segmentCount(List<Integer> divisions, int length) {
        int count = divisions.size() + 1;
        if (divisions.get(0) == 0) {
            count--;
        }
        if (divisions.get(divisions.size() - 1) == length) {
            count--;
        }
        return count;
    }
}
