package pro.sketchware.activities.resourceseditor;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Versioned string-page format. Length-prefixed UTF-8 avoids JSON escaping and writeUTF's 64 KiB limit. */
final class ResourceEditorStringsSnapshot {
    static final String FORMAT = "strings-utf8-v1";
    static final String CHUNKS_FORMAT = "strings-chunks-utf8-v1";
    private static final int MAGIC = 0x53574453;
    private static final int VERSION = 1;

    private ResourceEditorStringsSnapshot() { }

    static void write(OutputStream output, String session, String project, String variant, long revision,
                      ResourceEditorDraftState.Snapshot snapshot) throws IOException {
        write(output, session, project, variant, revision, -1, snapshot);
    }

    static void write(OutputStream output, String session, String project, String variant, long revision,
                      int chunk, ResourceEditorDraftState.Snapshot snapshot) throws IOException {
        DataOutputStream data = new DataOutputStream(output);
        data.writeInt(MAGIC);
        data.writeInt(chunk < 0 ? VERSION : 2);
        writeString(data, session);
        writeString(data, project);
        writeString(data, variant);
        data.writeLong(revision);
        if (chunk >= 0) data.writeInt(chunk);
        data.writeInt(snapshot.entries.size());
        for (Object entry : snapshot.entries) {
            Map<?, ?> row = (Map<?, ?>) entry;
            int count = 0;
            for (Object value : row.values()) if (value != null) count++;
            data.writeInt(count);
            for (Map.Entry<?, ?> attribute : row.entrySet()) {
                // Preserve the legacy Gson omission of null-valued attributes.
                if (attribute.getValue() == null) continue;
                writeString(data, (String) attribute.getKey());
                writeString(data, (String) attribute.getValue());
            }
        }
        int count = 0;
        for (String note : snapshot.notes.values()) if (note != null) count++;
        data.writeInt(count);
        for (Map.Entry<Integer, String> note : snapshot.notes.entrySet()) {
            if (note.getValue() == null) continue;
            data.writeInt(note.getKey());
            writeString(data, note.getValue());
        }
    }

    static ResourceEditorDraftState.Snapshot read(byte[] bytes, String session, String project,
                                                 String variant, long revision) throws IOException {
        return read(bytes, session, project, variant, revision, -1);
    }

    static ResourceEditorDraftState.Snapshot read(byte[] bytes, String session, String project,
                                                 String variant, long revision, int chunk) throws IOException {
        DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes));
        if (data.readInt() != MAGIC || data.readInt() != (chunk < 0 ? VERSION : 2)
                || !session.equals(readString(data)) || !project.equals(readString(data))
                || !variant.equals(readString(data)) || data.readLong() != revision) {
            throw new IOException("String draft identity or format mismatch");
        }
        if (chunk >= 0 && data.readInt() != chunk) throw new IOException("String draft chunk mismatch");
        int rows = readCount(data, 4);
        List<Object> entries = new ArrayList<>(rows);
        for (int i = 0; i < rows; i++) {
            int attributes = readCount(data, 8);
            HashMap<String, Object> row = new HashMap<>();
            for (int j = 0; j < attributes; j++) {
                String key = readString(data);
                if (row.containsKey(key)) throw new IOException("Duplicate string draft attribute");
                row.put(key, readString(data));
            }
            entries.add(row);
        }
        int noteCount = readCount(data, 8);
        Map<Integer, String> notes = new HashMap<>();
        for (int i = 0; i < noteCount; i++) {
            int index = data.readInt();
            if (notes.containsKey(index)) throw new IOException("Duplicate string draft note");
            notes.put(index, readString(data));
        }
        if (data.available() != 0) throw new IOException("Trailing string draft data");
        return new ResourceEditorDraftState.Snapshot(entries, notes);
    }

    private static int readCount(DataInputStream input, int minimumBytes) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > input.available() / minimumBytes) throw new IOException("Invalid string draft count");
        return count;
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > input.available()) throw new IOException("Invalid string draft text length");
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
