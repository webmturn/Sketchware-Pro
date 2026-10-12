package pro.sketchware.util;

import android.util.AtomicFile;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** JSON definition files must never be replaced with an empty list after a read error. */
public final class DefinitionFiles {
    private DefinitionFiles() {}

    public static String read(File file) throws IOException {
        AtomicFile atomic = new AtomicFile(file);
        if (!file.exists() && !new File(file.getPath() + ".bak").exists()) return null;
        return new String(atomic.readFully(), StandardCharsets.UTF_8);
    }

    public static <T> ArrayList<T> readList(File file, Class<T> type) throws IOException {
        String json = read(file);
        if (json == null) return new ArrayList<>();
        try {
            ArrayList<T> items = GsonUtils.getGson().fromJson(json,
                    TypeToken.getParameterized(ArrayList.class, type).getType());
            if (items == null || items.contains(null)) throw new JsonParseException("Invalid definition list");
            return items;
        } catch (JsonParseException | IllegalStateException e) {
            throw new IOException("Invalid definitions in " + file.getName(), e);
        }
    }

    public static void write(File file, Object value) throws IOException {
        writeText(file, GsonUtils.getGson().toJson(value));
    }

    public static void writeText(File file, String text) throws IOException {
        AtomicFile atomic = new AtomicFile(file);
        FileOutputStream stream = null;
        try {
            stream = atomic.startWrite();
            stream.write(text.getBytes(StandardCharsets.UTF_8));
            stream.getFD().sync();
            atomic.finishWrite(stream);
        } catch (IOException e) {
            if (stream != null) atomic.failWrite(stream);
            throw e;
        }
    }
}
