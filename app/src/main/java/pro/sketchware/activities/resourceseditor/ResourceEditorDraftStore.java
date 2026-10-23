package pro.sketchware.activities.resourceseditor;

import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ByteArrayOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

import pro.sketchware.activities.resourceseditor.components.models.ArrayModel;
import pro.sketchware.activities.resourceseditor.components.models.DimenModel;
import pro.sketchware.activities.resourceseditor.components.models.StyleModel;
import pro.sketchware.core.resources.ColorModel;
import pro.sketchware.util.LogUtil;

/** Accessed on the application's serial draft queue. The manifest is the only commit point. */
final class ResourceEditorDraftStore {
    private static final Gson GSON = new Gson();
    private static final int SCHEMA = 1;

    /**
     * Test-only fault injection point. Production never sets this; the controller passes it to
     * every store so device tests can fail a specific commit without touching user projects.
     */
    @androidx.annotation.VisibleForTesting
    static volatile java.util.function.Consumer<String> testFault = point -> { };

    /**
     * Test-only: simulates a project XML that exists but cannot be read. Production never sets
     * this; device tests use it to prove an unreadable XML is not treated as a hash mismatch.
     */
    @androidx.annotation.VisibleForTesting
    static volatile java.util.function.IntPredicate xmlReadFault = page -> false;

    final File directory;
    private final String session;
    private final String project;
    private final String variant;
    private final ResourceEditorDraftController.Gate gate;
    private final long owner;
    private final Faults faults;
    private final String[] xmlPaths;
    private final ResourceEditorLegacyXmlBarrier legacyXml;
    private final boolean[] legacySupersededPages = new boolean[ResourceEditorTabs.COUNT];
    private boolean legacyXmlBlocked;
    private final boolean[] xmlCommittedPages = new boolean[ResourceEditorTabs.COUNT];
    private volatile Manifest manifest;
    private volatile boolean durable;
    // Only successful captures, never models handed to the UI by open(). Accessed on IO.
    private final ResourceEditorDraftState.Snapshot[] protectedModels = new ResourceEditorDraftState.Snapshot[ResourceEditorTabs.COUNT];
    private final long[] protectedModelRevisions = new long[ResourceEditorTabs.COUNT];

    interface Faults { void at(String point) throws IOException; }

    ResourceEditorDraftStore(File root, String session, String project, String variant,
                             ResourceEditorDraftController.Gate gate, long owner, Faults faults) throws IOException {
        this(root, session, project, variant, gate, owner, faults, null);
    }

    ResourceEditorDraftStore(File root, String session, String project, String variant,
                             ResourceEditorDraftController.Gate gate, long owner, Faults faults,
                             String[] xmlPaths) throws IOException {
        if (session == null || !session.equals(parseUuid(session))) throw new IOException("Invalid draft session");
        this.directory = new File(root, session);
        this.session = session;
        this.project = project;
        this.variant = variant;
        this.gate = gate;
        this.owner = owner;
        this.faults = faults;
        this.xmlPaths = xmlPaths == null ? null : xmlPaths.clone();
        legacyXml = new ResourceEditorLegacyXmlBarrier(root, project, variant, session, xmlPaths, faults);
    }

    private static String parseUuid(String value) throws IOException {
        try { return UUID.fromString(value).toString(); }
        catch (IllegalArgumentException failure) { throw new IOException("Invalid draft session", failure); }
    }

    ResourceEditorDraftState.Snapshot[] open(boolean restoring) throws IOException {
        java.util.Arrays.fill(protectedModels, null);
        ResourceEditorDraftState.Snapshot[] pages = new ResourceEditorDraftState.Snapshot[ResourceEditorTabs.COUNT];
        File file = new File(directory, "manifest.json");
        if (!file.exists() && !restoring) {
            manifest = fresh();
            return pages;
        }
        try {
            Manifest restored = GSON.fromJson(read(file), Manifest.class);
            validate(restored);
            java.util.Arrays.fill(xmlCommittedPages, false);
            XmlCommitIntent intent = readIntent(restored);
            if (intent != null && intent.pages != null) {
                for (int i = 0; i < pages.length; i++) {
                    PageRef ref = restored.pages[i];
                    if (ref != null && ref.dirty && intent.pages.containsKey(i)) {
                        int state = xmlHashState(i, intent.pages.get(i));
                        // A read failure cannot prove the XML was not written. Applying the older
                        // draft here could overwrite that XML later, so fail the restore instead.
                        if (state == XML_UNREADABLE) {
                            throw new IOException("Unable to verify committed resource XML for page " + i);
                        }
                        if (state == XML_MATCH) xmlCommittedPages[i] = true;
                    }
                }
            }
            for (int i = 0; i < pages.length; i++) {
                if (xmlCommittedPages[i]) continue;
                PageRef ref = restored.pages[i];
                if (ref == null || ref.revision < 0) throw new IOException("Invalid draft page descriptor");
                if (!ref.dirty) {
                    if (ref.object != null || ref.blocks != null) throw new IOException("Clean page has a draft payload");
                    continue;
                }
                if (i != ResourceEditorTabs.STRINGS && ResourceEditorModelBlocks.FORMAT.equals(ref.format)) {
                    pages[i] = readModelBlocks(i, ref);
                    continue;
                }
                if (i == ResourceEditorTabs.STRINGS && ResourceEditorStringsSnapshot.CHUNKS_FORMAT.equals(ref.format)) {
                    pages[i] = readChunks(ref);
                    continue;
                }
                if (ref.object == null || !ref.object.equals(parseUuid(ref.object))) throw new IOException("Invalid draft object");
                if (ref.format != null && !(i == ResourceEditorTabs.STRINGS
                        && ResourceEditorStringsSnapshot.FORMAT.equals(ref.format))) throw new IOException("Unknown draft encoding");
                byte[] bytes = Files.readAllBytes(pageFile(i, ref).toPath());
                if (!digest(bytes).equals(ref.checksum)) throw new IOException("Draft checksum mismatch");
                if (ref.format != null) {
                    pages[i] = ResourceEditorStringsSnapshot.read(bytes, session, project, variant, ref.revision);
                    ref.rows = pages[i].entries.size();
                    continue;
                }
                JsonObject page = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);
                if (page.get("schema").getAsInt() != SCHEMA || !session.equals(page.get("session").getAsString())
                        || !project.equals(page.get("project").getAsString()) || !variant.equals(page.get("variant").getAsString())
                        || page.get("page").getAsInt() != i || page.get("revision").getAsLong() != ref.revision) {
                    throw new IOException("Draft page identity mismatch");
                }
                List<Object> entries = GSON.fromJson(page.get("entries"),
                        TypeToken.getParameterized(List.class, modelClass(i)).getType());
                Map<Integer, String> notes = GSON.fromJson(page.get("notes"),
                        new TypeToken<Map<Integer, String>>() { }.getType());
                if (entries == null || entries.contains(null) || notes == null || notes.containsKey(null)
                        || notes.containsValue(null)) throw new IOException("Incomplete draft page");
                pages[i] = new ResourceEditorDraftState.Snapshot(entries, notes);
            }
            manifest = restored;
            durable = true;
            try { cleanup(); }
            catch (IOException failure) { LogUtil.w("ResourceDrafts", "Draft cleanup deferred after restore", failure); }
            return pages;
        } catch (RuntimeException failure) {
            throw new IOException("Invalid draft manifest or page", failure);
        }
    }

    static Class<?> modelClass(int page) {
        return switch (page) {
            case ResourceEditorTabs.STRINGS -> HashMap.class;
            case ResourceEditorTabs.COLORS -> ColorModel.class;
            case ResourceEditorTabs.STYLES, ResourceEditorTabs.THEMES -> StyleModel.class;
            case ResourceEditorTabs.ARRAYS -> ArrayModel.class;
            case ResourceEditorTabs.DIMENS -> DimenModel.class;
            default -> throw new IllegalArgumentException("Invalid resource page");
        };
    }

    long revision(int page) { return manifest.pages[page].revision; }
    boolean protectedPage(int page, long revision, boolean dirty) {
        Manifest current = manifest;
        // A page whose XML already replaced the draft must not reuse the old manifest entry:
        // the next durable commit has to persist the complete page instead.
        return durable && current != null && !xmlCommittedPages[page]
                && current.pages[page].revision == revision && current.pages[page].dirty == dirty;
    }
    boolean isDurable() { return durable; }
    long generation() { return manifest == null ? -1 : manifest.generation; }
    boolean wasXmlCommitted(int page) { return xmlCommittedPages[page]; }
    boolean wasLegacySuperseded(int page) { return legacySupersededPages[page]; }
    boolean hasLegacyXmlHistory() { return legacyXml.hasHistory(); }

    String prepareLegacyXml(int page, String xml) throws IOException {
        synchronized (gate) {
            requireOwner();
            return legacyXml.prepare(page, xml);
        }
    }

    void confirmLegacyXml(int page, String token) throws IOException {
        synchronized (gate) {
            requireOwner();
            legacyXml.confirm(page, token);
        }
    }

    private void flushLegacySources() throws IOException {
        synchronized (gate) {
            requireOwner();
            legacyXml.flushSources();
        }
    }

    static final int LEGACY_NONE = 0;
    static final int LEGACY_MIGRATED = 1;
    static final int LEGACY_SUPERSEDED = 2;
    static final int LEGACY_PARTIAL = 3;
    static final int LEGACY_FAILED = 4;
    static final int LEGACY_DEFERRED = 5;

    /** Result of folding a legacy saved-state Bundle into this session. */
    static final class LegacyImport {
        final int state;
        final Map<Integer, ResourceEditorDraftState.Snapshot> pages;
        final Bundle corrupt;
        boolean xmlBarrierBlocked;

        LegacyImport(int state, Map<Integer, ResourceEditorDraftState.Snapshot> pages, Bundle corrupt) {
            this.state = state;
            this.pages = pages;
            this.corrupt = corrupt;
        }

        boolean covered() {
            return state == LEGACY_MIGRATED || state == LEGACY_SUPERSEDED || state == LEGACY_PARTIAL;
        }
    }

    private LegacyImport legacyResult(int state,
                                             Map<Integer, ResourceEditorDraftState.Snapshot> pages,
                                             Bundle corrupt) {
        LegacyImport result = new LegacyImport(state, pages == null ? new HashMap<>() : pages, corrupt);
        result.xmlBarrierBlocked = legacyXmlBlocked;
        return result;
    }

    /**
     * Imports the pre-session Bundle written by older versions. A newer dirty draft and a proven
     * XML commit win per page; unreadable pages are set aside instead of failing the other pages.
     * The commit is atomic, so a failure leaves the manifest and every legacy input untouched.
     */
    LegacyImport importLegacy(Bundle drafts) {
        Bundle corrupt = new Bundle();
        try {
            requireOwner();
            legacyXmlBlocked = false;
            java.util.Arrays.fill(legacySupersededPages, false);
            if (drafts == null || drafts.isEmpty()) return legacyResult(LEGACY_NONE, null, corrupt);
            Map<Integer, ResourceEditorDraftState.Snapshot> migrated = new HashMap<>();
            boolean anyXmlCommitted = false;
            for (boolean committed : xmlCommittedPages) anyXmlCommitted |= committed;
            for (int i = 0; i < ResourceEditorTabs.COUNT; i++) {
                Bundle page;
                try {
                    page = drafts.getBundle(Integer.toString(i));
                } catch (RuntimeException failure) {
                    // The bundle itself is unreadable; keep the caller's original source intact.
                    LogUtil.w("ResourceDrafts", "Unable to read legacy draft page " + i, failure);
                    return legacyResult(LEGACY_FAILED, null, corrupt);
                }
                if (page == null) continue;
                try {
                    String source = ResourceEditorLegacyXmlBarrier.fingerprint(page);
                    if (legacyXml.superseded(i, source)) {
                        legacySupersededPages[i] = true;
                        continue;
                    }
                    legacyXml.remember(i, source);
                } catch (IOException unverified) {
                    LogUtil.w("ResourceDrafts", "Unable to verify legacy XML source " + i, unverified);
                    legacySupersededPages[i] = true;
                    legacyXmlBlocked = true;
                    corrupt.putBundle(Integer.toString(i), new Bundle(page));
                    continue;
                }
                // Superseded input need not be parsed or retained as corrupt material.
                if (xmlCommittedPages[i] || manifest.pages[i].dirty) continue;
                ResourceEditorDraftState.Snapshot snapshot;
                try {
                    snapshot = ResourceEditorDraftState.fromLegacy(page, modelClass(i));
                    if (snapshot == null || snapshot.entries == null || snapshot.entries.contains(null)
                            || snapshot.notes == null || snapshot.notes.containsKey(null)
                            || snapshot.notes.containsValue(null)) {
                        throw new IllegalArgumentException("Incomplete legacy page");
                    }
                } catch (RuntimeException failure) {
                    LogUtil.w("ResourceDrafts", "Unreadable legacy draft page " + i, failure);
                    corrupt.putBundle(Integer.toString(i), new Bundle(page));
                    continue;
                }
                // A proven XML commit is newer than any legacy content, and an already dirty
                // manifest page is a newer draft. Neither may be replaced by the old Bundle.
                migrated.put(i, snapshot);
            }
            flushLegacySources();
            if (anyXmlCommitted && !migrated.isEmpty()) {
                // commit() rewrites every page, including the XML-committed one for which no
                // complete snapshot exists here. Defer until that page is loaded and protected.
                return legacyResult(LEGACY_DEFERRED, null, corrupt);
            }
            if (migrated.isEmpty()) {
                return legacyResult(corrupt.isEmpty() ? LEGACY_SUPERSEDED : LEGACY_PARTIAL, null, corrupt);
            }
            long[] revisions = new long[ResourceEditorTabs.COUNT];
            boolean[] dirty = new boolean[ResourceEditorTabs.COUNT];
            for (int i = 0; i < ResourceEditorTabs.COUNT; i++) {
                PageRef ref = manifest.pages[i];
                revisions[i] = ref.revision;
                dirty[i] = ref.dirty;
                if (migrated.containsKey(i)) {
                    revisions[i] = 1;
                    dirty[i] = true;
                }
            }
            try {
                commit(revisions, dirty, migrated);
            } catch (IOException failure) {
                LogUtil.e("ResourceDrafts", "Unable to migrate legacy draft", failure);
                return legacyResult(LEGACY_FAILED, null, corrupt);
            }
            // These migration snapshots are about to become editable UI models.
            for (Integer page : migrated.keySet()) protectedModels[page] = null;
            return legacyResult(corrupt.isEmpty() ? LEGACY_MIGRATED : LEGACY_PARTIAL, migrated, corrupt);
        } catch (IOException failure) {
            // Owner was replaced or the commit could not be attempted at all.
            LogUtil.e("ResourceDrafts", "Unable to migrate legacy draft", failure);
            return legacyResult(LEGACY_FAILED, null, corrupt);
        } catch (RuntimeException failure) {
            LogUtil.e("ResourceDrafts", "Unable to migrate legacy draft", failure);
            return legacyResult(LEGACY_FAILED, null, corrupt);
        }
    }

    /** Independent diagnostic material; never used as a draft or as an incremental base. */
    String archiveLegacyCorrupt(Bundle corrupt) throws IOException {
        requireOwner();
        Bundle material = new Bundle();
        material.putInt("schema", 1);
        material.putString("project", project);
        material.putString("variant", variant);
        material.putBundle("pages", corrupt);
        byte[] bytes = ResourceEditorLegacyState.parcelBytes(material);
        String identity = digest((project + "\u0000" + variant).getBytes(StandardCharsets.UTF_8));
        File archive = new File(new File(new File(directory.getParentFile(), "legacy-material"), identity),
                digest(bytes) + ".parcel");
        faults.at("before_legacy_material");
        synchronized (gate) {
            requireOwner();
            if (!archive.isFile() || !digest(Files.readAllBytes(archive.toPath())).equals(digest(bytes))) {
                ResourceXmlWriter.writeBytes(archive, stream -> stream.write(bytes), null);
            }
        }
        return identity;
    }

    /** Writes the per-page XML commit intent before the XML-only save touches project files. */
    boolean writeXmlCommitIntent(Map<Integer, String> hashes) throws IOException {
        requireOwner();
        File file = intentFile();
        if (hashes == null || hashes.isEmpty()) return true;
        for (Map.Entry<Integer, String> entry : hashes.entrySet()) {
            if (entry.getKey() == null || entry.getKey() < 0 || entry.getKey() >= ResourceEditorTabs.COUNT
                    || entry.getValue() == null || entry.getValue().length() != 64) {
                throw new IOException("Invalid XML commit intent page");
            }
        }
        XmlCommitIntent existing = readExistingIntent();
        Map<Integer, List<String>> merged = new HashMap<>();
        if (existing != null) {
            for (Map.Entry<Integer, List<String>> entry : existing.pages.entrySet()) {
                merged.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        for (Map.Entry<Integer, String> entry : hashes.entrySet()) {
            List<String> values = merged.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>());
            if (!values.contains(entry.getValue())) values.add(entry.getValue());
        }
        XmlCommitIntent intent = new XmlCommitIntent();
        intent.session = session;
        intent.project = project;
        intent.variant = variant;
        intent.generation = manifest == null ? 0 : manifest.generation;
        intent.pages = merged;
        ResourceXmlWriter.write(file, writer -> GSON.toJson(intent, writer));
        // The very next XML write may replace draft A on these pages. Until a complete page is
        // committed again, neither the old manifest entry nor its chunks may be reused.
        for (Integer page : hashes.keySet()) xmlCommittedPages[page] = true;
        return true;
    }

    /** Reads a same-generation intent so a retry keeps pages saved by the previous XML-only attempt. */
    private XmlCommitIntent readExistingIntent() throws IOException {
        File file = intentFile();
        if (!file.isFile()) return null;
        XmlCommitIntent intent = GSON.fromJson(read(file), XmlCommitIntent.class);
        if (intent == null || intent.schema != 1 || !session.equals(intent.session)
                || !project.equals(intent.project) || !variant.equals(intent.variant)
                || intent.pages == null) {
            throw new IOException("Invalid XML commit intent");
        }
        if (manifest == null || intent.generation != manifest.generation) return null;
        validateIntentPages(intent.pages);
        return intent;
    }

    private static void validateIntentPages(Map<Integer, List<String>> pages) throws IOException {
        for (Map.Entry<Integer, List<String>> entry : pages.entrySet()) {
            if (entry.getKey() == null || entry.getKey() < 0 || entry.getKey() >= ResourceEditorTabs.COUNT
                    || entry.getValue() == null || entry.getValue().isEmpty()) {
                throw new IOException("Invalid XML commit intent page");
            }
            for (String hash : entry.getValue()) {
                if (hash == null || hash.length() != 64) throw new IOException("Invalid XML commit intent hash");
            }
        }
    }

    private File intentFile() {
        return new File(directory, "xml-commit.json");
    }

    private XmlCommitIntent readIntent(Manifest restored) throws IOException {
        File file = intentFile();
        if (!file.isFile()) return null;
        XmlCommitIntent intent = GSON.fromJson(read(file), XmlCommitIntent.class);
        if (intent == null || intent.schema != 1 || !session.equals(intent.session)
                || !project.equals(intent.project) || !variant.equals(intent.variant)
                || intent.pages == null) {
            throw new IOException("Invalid XML commit intent");
        }
        validateIntentPages(intent.pages);
        return intent.generation == restored.generation ? intent : null;
    }

    private static final int XML_MISMATCH = 0;
    private static final int XML_MATCH = 1;
    private static final int XML_UNREADABLE = 2;

    /**
     * Distinguishes "read succeeded, hash differs" from "cannot judge". Only the former allows the
     * older draft to be applied; an unreadable XML is a restore failure, matching the corrupt
     * intent policy, and the draft directory stays untouched for manual recovery.
     */
    private int xmlHashState(int page, List<String> expected) {
        if (xmlPaths == null || page >= xmlPaths.length || xmlPaths[page] == null) return XML_MISMATCH;
        File file = new File(xmlPaths[page]);
        if (!file.isFile()) return XML_MISMATCH;
        try {
            if (xmlReadFault.test(page)) throw new IOException("Injected resource XML read failure");
            return expected.contains(digest(Files.readAllBytes(file.toPath()))) ? XML_MATCH : XML_MISMATCH;
        } catch (IOException failure) {
            LogUtil.w("ResourceDrafts", "Unable to fingerprint resource XML: " + file, failure);
            return XML_UNREADABLE;
        }
    }

    private static long intentGeneration(File file) {
        try {
            XmlCommitIntent intent = GSON.fromJson(read(file), XmlCommitIntent.class);
            return intent == null || intent.schema != 1 ? -1 : intent.generation;
        } catch (Exception failure) {
            return -1;
        }
    }

    /**
     * Returns the revision an incremental string draft may build on, or -1 when only a full
     * snapshot is safe. Draft A stops being a base as soon as its XML was replaced by B; until a
     * complete B-based snapshot is committed, mixing A's untouched chunks with later edits would
     * resurrect content that B already superseded.
     */
    long incrementalRevision(int page) {
        Manifest current = manifest;
        if (page != ResourceEditorTabs.STRINGS || !durable || current == null || xmlCommittedPages[page]) return -1;
        PageRef ref = current.pages[page];
        return ref.dirty && (ResourceEditorStringsSnapshot.FORMAT.equals(ref.format)
                || ResourceEditorStringsSnapshot.CHUNKS_FORMAT.equals(ref.format)) ? ref.revision : -1;
    }

    void commit(long[] revisions, boolean[] dirty, Map<Integer, ResourceEditorDraftState.Snapshot> snapshots) throws IOException {
        requireOwner();
        flushLegacySources();
        Manifest next = fresh();
        next.generation = manifest.generation + 1;
        for (int i = 0; i < ResourceEditorTabs.COUNT; i++) {
            PageRef ref = new PageRef();
            ref.revision = revisions[i];
            ref.dirty = dirty[i];
            if (dirty[i]) {
                if (protectedPage(i, revisions[i], true)) {
                    ref = manifest.pages[i];
                } else {
                    ResourceEditorDraftState.Snapshot snapshot = snapshots.get(i);
                    if (snapshot == null) throw new IOException("Missing changed draft page");
                    if (i != ResourceEditorTabs.STRINGS) {
                        if (snapshot.chunks != null) throw new IOException("Unexpected string chunks");
                        next.pages[i] = writeModelBlocks(i, revisions[i], snapshot);
                        continue;
                    }
                    if (snapshot.chunks != null) {
                        if (i != ResourceEditorTabs.STRINGS) throw new IOException("Unexpected incremental page");
                        next.pages[i] = writeChunks(revisions[i], snapshot.chunks);
                        continue;
                    }
                    ref.object = UUID.randomUUID().toString();
                    if (i == ResourceEditorTabs.STRINGS) {
                        ref.format = ResourceEditorStringsSnapshot.FORMAT;
                        ref.rows = snapshot.entries.size();
                    }
                    PageData page = new PageData();
                    page.session = session; page.project = project; page.variant = variant;
                    page.page = i; page.revision = revisions[i];
                    page.entries = snapshot.entries; page.notes = snapshot.notes;
                    File destination = pageFile(i, ref);
                    MessageDigest checksum = newDigest();
                    faults.at("before_page");
                    if (i == ResourceEditorTabs.STRINGS) {
                        ResourceXmlWriter.writeBytes(destination, stream -> {
                            faults.at("before_encode");
                            ResourceEditorStringsSnapshot.write(stream, session, project, variant, page.revision, snapshot);
                            faults.at("after_encode");
                        }, checksum);
                    } else {
                        ResourceXmlWriter.write(destination, writer -> {
                            faults.at("before_encode");
                            GSON.toJson(page, writer);
                            faults.at("after_encode");
                        }, checksum);
                    }
                    faults.at("after_write");
                    ref.checksum = hex(checksum.digest());
                    faults.at("after_page");
                }
            }
            next.pages[i] = ref;
        }
        faults.at("before_manifest");
        synchronized (gate) {
            requireOwner();
            ResourceXmlWriter.write(new File(directory, "manifest.json"), writer -> GSON.toJson(next, writer));
            manifest = next;
            durable = true;
            for (int i = 1; i < next.pages.length; i++) {
                if (!next.pages[i].dirty) protectedModels[i] = null;
                else if (snapshots.containsKey(i)) {
                    protectedModels[i] = snapshots.get(i);
                    protectedModelRevisions[i] = revisions[i];
                }
            }
            // Every page is represented by this manifest: the full snapshot, the reused protected
            // entry, or a clean page. Any earlier XML-triggered invalidation is now resolved.
            java.util.Arrays.fill(xmlCommittedPages, false);
        }
        // Cleanup failure cannot invalidate an already committed checkpoint.
        try { faults.at("after_manifest"); cleanup(); }
        catch (IOException failure) { LogUtil.w("ResourceDrafts", "Draft cleanup deferred", failure); }
    }

    private PageRef writeModelBlocks(int page, long revision, ResourceEditorDraftState.Snapshot snapshot) throws IOException {
        if (!validChunkNotes(snapshot)) throw new IOException("Invalid model page notes");
        PageRef previous = manifest.pages[page];
        ResourceEditorDraftState.Snapshot protectedSnapshot = protectedModels[page];
        boolean reusable = durable && !xmlCommittedPages[page]
                && ResourceEditorModelBlocks.FORMAT.equals(previous.format)
                && protectedSnapshot != null && protectedModelRevisions[page] == previous.revision;
        PageRef next = new PageRef();
        next.format = ResourceEditorModelBlocks.FORMAT;
        next.revision = revision;
        next.dirty = true;
        next.rows = snapshot.entries.size();
        int count = ResourceEditorModelBlocks.count(next.rows);
        next.blocks = new ArrayList<>(count);
        List<Integer> changed = new ArrayList<>();
        // Avoid boxing every row index when only a model attribute changed.
        boolean identicalNotes = reusable && protectedSnapshot.notes.equals(snapshot.notes);
        for (int block = 0; block < count; block++) {
            if (reusable && block < previous.blocks.size()
                    && !ResourceEditorModelBlocks.changed(protectedSnapshot, snapshot, block, identicalNotes)) {
                next.blocks.add(previous.blocks.get(block));
            } else {
                next.blocks.add(null);
                changed.add(block);
            }
        }
        writeSegment(page, revision, snapshot, next, changed);
        // A segment can contain both live and superseded blocks. Bound reachable waste,
        // rather than periodically rewriting solely because many small segments exist.
        if (needsModelCompaction(page, next)) {
            faults.at("before_compaction");
            changed.clear();
            for (int block = 0; block < count; block++) changed.add(block);
            writeSegment(page, revision, snapshot, next, changed);
            faults.at("after_compaction");
        }
        return next;
    }

    private void writeSegment(int page, long revision, ResourceEditorDraftState.Snapshot snapshot,
                              PageRef next, List<Integer> changed) throws IOException {
        if (changed.isEmpty()) return;
        String object = UUID.randomUUID().toString();
        long[] offset = {0};
        faults.at("before_page");
        ResourceXmlWriter.writeBytes(segmentFile(page, object), stream -> {
            EncodedBlock bytes = new EncodedBlock();
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(bytes, StandardCharsets.UTF_8), 65536)) {
                for (int index : changed) {
                    var slice = ResourceEditorModelBlocks.slice(snapshot, index);
                    if (!validChunkNotes(slice)) throw new IOException("Invalid model block notes");
                    PageData data = new PageData();
                    data.session = session; data.project = project; data.variant = variant;
                    data.page = page; data.revision = revision; data.block = index;
                    data.entries = slice.entries; data.notes = slice.notes;
                    bytes.reset();
                    faults.at("before_encode");
                    GSON.toJson(data, writer);
                    writer.flush();
                    faults.at("after_encode");
                    ModelBlockRef block = new ModelBlockRef();
                    block.object = object; block.offset = offset[0]; block.length = bytes.size();
                    MessageDigest checksum = newDigest(); checksum.update(bytes.buffer(), 0, bytes.size());
                    block.revision = revision; block.checksum = hex(checksum.digest());
                    stream.write(bytes.buffer(), 0, bytes.size());
                    offset[0] += bytes.size();
                    next.blocks.set(index, block);
                }
            }
        }, null);
        faults.at("after_write");
        faults.at("after_page");
    }

    private static final class EncodedBlock extends ByteArrayOutputStream {
        EncodedBlock() { super(65536); }
        byte[] buffer() { return buf; }
    }

    private boolean needsModelCompaction(int page, PageRef ref) throws IOException {
        Set<String> objects = new HashSet<>();
        long live = 0, retained = 0;
        for (ModelBlockRef block : ref.blocks) {
            live = Math.addExact(live, block.length);
            if (objects.add(block.object)) {
                File file = segmentFile(page, block.object);
                if (!file.isFile()) throw new IOException("Missing referenced model segment");
                retained = Math.addExact(retained, file.length());
            }
        }
        return retained - live > live;
    }

    private ResourceEditorDraftState.Snapshot readModelBlocks(int page, PageRef ref) throws IOException {
        if (ref.rows < 0 || ref.blocks == null || ref.blocks.size() != ResourceEditorModelBlocks.count(ref.rows)
                || ref.object != null || ref.chunks != null) throw new IOException("Invalid model block descriptor");
        List<Object> entries = new ArrayList<>(ref.rows);
        Map<Integer, String> notes = new HashMap<>();
        Map<String, RandomAccessFile> files = new HashMap<>();
        java.lang.reflect.Type blockType = TypeToken.getParameterized(ModelBlockData.class, modelClass(page)).getType();
        try {
            for (int index = 0; index < ref.blocks.size(); index++) {
                ModelBlockRef block = ref.blocks.get(index);
                if (block == null || block.revision < 0 || block.revision > ref.revision)
                    throw new IOException("Invalid model block revision");
                checkObject(block.object);
                RandomAccessFile file = files.get(block.object);
                if (file == null) {
                    file = new RandomAccessFile(segmentFile(page, block.object), "r");
                    files.put(block.object, file);
                }
                if (block.offset < 0 || block.length <= 0 || block.length > Integer.MAX_VALUE
                        || block.length > file.length() || block.offset > file.length() - block.length)
                    throw new IOException("Invalid model segment range");
                byte[] bytes = new byte[(int) block.length];
                file.seek(block.offset); file.readFully(bytes);
                if (!digest(bytes).equals(block.checksum)) throw new IOException("Model block checksum mismatch");
                ModelBlockData<?> data = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), blockType);
                if (data == null || data.schema == null || data.schema != SCHEMA
                        || !session.equals(data.session) || !project.equals(data.project) || !variant.equals(data.variant)
                        || data.page == null || data.page != page || data.revision == null || data.revision != block.revision
                        || data.block == null || data.block != index) throw new IOException("Model block identity mismatch");
                @SuppressWarnings("unchecked")
                List<Object> rows = (List<Object>)(List<?>)data.entries;
                Map<Integer, String> localNotes = data.notes;
                int start = index * ResourceEditorModelBlocks.ROWS;
                int expected = Math.min(ResourceEditorModelBlocks.ROWS, ref.rows - start);
                if (rows == null || rows.size() != expected || rows.contains(null) || localNotes == null
                        || localNotes.containsKey(null) || localNotes.containsValue(null))
                    throw new IOException("Incomplete model block");
                var slice = new ResourceEditorDraftState.Snapshot(rows, localNotes);
                if (!validChunkNotes(slice)) throw new IOException("Invalid model block notes");
                entries.addAll(rows);
                for (Map.Entry<Integer, String> note : localNotes.entrySet()) {
                    // Reused old trailing notes do not become row notes after an append.
                    if (note.getKey() < expected || index == ref.blocks.size() - 1)
                        notes.put(start + note.getKey(), note.getValue());
                }
            }
        } finally {
            for (RandomAccessFile file : files.values()) {
                try { file.close(); }
                catch (IOException failure) { LogUtil.w("ResourceDrafts", "Model segment close failed", failure); }
            }
        }
        return new ResourceEditorDraftState.Snapshot(entries, notes);
    }

    private File segmentFile(int page, String object) {
        return new File(new File(directory, "pages"), page + "-" + object + ".seg");
    }

    private PageRef writeChunks(long revision, ResourceEditorDraftState.StringChunks changes) throws IOException {
        PageRef previous = manifest.pages[ResourceEditorTabs.STRINGS];
        if (incrementalRevision(ResourceEditorTabs.STRINGS) < 0 || changes.baseRevision < 0
                || changes.baseRevision > previous.revision || revision <= previous.revision
                || changes.rows <= 0 || changes.changed.isEmpty()) throw new IOException("Invalid incremental draft base");
        PageRef next = new PageRef();
        next.revision = revision; next.dirty = true;
        next.format = ResourceEditorStringsSnapshot.CHUNKS_FORMAT;
        next.object = previous.object; next.checksum = previous.checksum;
        next.baseRevision = previous.chunks == null ? previous.revision : previous.baseRevision;
        next.rows = changes.rows;
        next.chunks = previous.chunks == null ? new HashMap<>() : new HashMap<>(previous.chunks);
        int count = ResourceEditorStringChanges.chunkCount(changes.rows);
        next.chunks.keySet().removeIf(index -> index >= count);
        for (Map.Entry<Integer, ResourceEditorDraftState.Snapshot> entry : changes.changed.entrySet()) {
            int index = entry.getKey();
            ResourceEditorDraftState.Snapshot snapshot = entry.getValue();
            int expected = Math.min(ResourceEditorStringChanges.CHUNK_SIZE,
                    changes.rows - index * ResourceEditorStringChanges.CHUNK_SIZE);
            if (index < 0 || index >= count || snapshot.chunks != null || snapshot.entries.size() != expected
                    || !validChunkNotes(snapshot)) throw new IOException("Invalid changed string chunk");
            ChunkRef chunk = new ChunkRef();
            chunk.object = UUID.randomUUID().toString(); chunk.revision = revision;
            MessageDigest checksum = newDigest();
            faults.at("before_page");
            ResourceXmlWriter.writeBytes(chunkFile(chunk), stream -> {
                faults.at("before_encode");
                ResourceEditorStringsSnapshot.write(stream, session, project, variant, revision, index, snapshot);
                faults.at("after_encode");
            }, checksum);
            faults.at("after_write");
            chunk.checksum = hex(checksum.digest());
            next.chunks.put(index, chunk);
            faults.at("after_page");
        }
        return next;
    }

    private ResourceEditorDraftState.Snapshot readChunks(PageRef ref) throws IOException {
        checkObject(ref.object);
        if (ref.baseRevision < 0 || ref.baseRevision > ref.revision || ref.rows <= 0 || ref.chunks == null
                || ref.chunks.isEmpty()) throw new IOException("Invalid chunked string page");
        byte[] baseBytes = checkedBytes(pageFile(ResourceEditorTabs.STRINGS, ref), ref.checksum);
        ResourceEditorDraftState.Snapshot base = ResourceEditorStringsSnapshot.read(
                baseBytes, session, project, variant, ref.baseRevision);
        int count = ResourceEditorStringChanges.chunkCount(ref.rows);
        if ((long)ref.rows > base.entries.size() + (long)ref.chunks.size() * ResourceEditorStringChanges.CHUNK_SIZE)
            throw new IOException("Invalid string chunk row count");
        for (Map.Entry<Integer, ChunkRef> entry : ref.chunks.entrySet()) {
            if (entry.getKey() == null || entry.getKey() < 0 || entry.getKey() >= count)
                throw new IOException("Invalid string chunk index");
            ChunkRef chunk = entry.getValue();
            if (chunk == null || chunk.revision <= ref.baseRevision || chunk.revision > ref.revision)
                throw new IOException("Invalid string chunk revision");
            checkObject(chunk.object);
        }
        List<Object> entries = new ArrayList<>(ref.rows);
        Map<Integer, String> notes = new HashMap<>();
        for (int index = 0; index < count; index++) {
            int start = index * ResourceEditorStringChanges.CHUNK_SIZE;
            int end = Math.min(ref.rows, start + ResourceEditorStringChanges.CHUNK_SIZE);
            ChunkRef chunk = ref.chunks.get(index);
            if (chunk == null) {
                if (end > base.entries.size()) throw new IOException("Missing appended string chunk");
                entries.addAll(base.entries.subList(start, end));
                for (int i = start; i < end + (end == ref.rows ? 1 : 0); i++) {
                    if (base.notes.containsKey(i)) notes.put(i, base.notes.get(i));
                }
            } else {
                ResourceEditorDraftState.Snapshot changed = ResourceEditorStringsSnapshot.read(
                        checkedBytes(chunkFile(chunk), chunk.checksum), session, project, variant, chunk.revision, index);
                if (changed.entries.size() != end - start || !validChunkNotes(changed))
                    throw new IOException("Invalid restored string chunk");
                entries.addAll(changed.entries);
                for (Map.Entry<Integer, String> note : changed.notes.entrySet()) {
                    int position = start + note.getKey();
                    // An old trailing comment becomes the next chunk's responsibility after append.
                    if (position < end || position == end && end == ref.rows) notes.put(position, note.getValue());
                }
            }
        }
        return new ResourceEditorDraftState.Snapshot(entries, notes);
    }

    private static boolean validChunkNotes(ResourceEditorDraftState.Snapshot snapshot) {
        for (Integer index : snapshot.notes.keySet())
            if (index == null || index < 0 || index > snapshot.entries.size()) return false;
        return true;
    }

    private static void checkObject(String object) throws IOException {
        if (object == null || !object.equals(parseUuid(object))) throw new IOException("Invalid draft object");
    }

    private static byte[] checkedBytes(File file, String checksum) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        if (!digest(bytes).equals(checksum)) throw new IOException("Draft checksum mismatch");
        return bytes;
    }

    private File chunkFile(ChunkRef ref) {
        return new File(new File(directory, "pages"), "0-" + ref.object + ".bin");
    }

    void close() throws IOException {
        synchronized (gate) {
            requireOwner();
            // The Activity publishes only durable session IDs. A session with no storage or
            // legacy recovery records needs only its in-memory owner invalidated on close.
            if (!durable && manifest.generation == 0
                    && Files.notExists(directory.toPath(), LinkOption.NOFOLLOW_LINKS)
                    && !legacyXml.hasHistory()) {
                gate.closed = true;
                return;
            }
            faults.at("before_close_manifest");
            if (legacyXml.hasHistory()) legacyXml.discardSession();
            Manifest closed = fresh();
            closed.closed = true;
            closed.generation = manifest.generation + 1;
            ResourceXmlWriter.write(new File(directory, "manifest.json"), writer -> GSON.toJson(closed, writer));
            manifest = closed;
            durable = true;
            gate.closed = true;
            java.util.Arrays.fill(protectedModels, null);
        }
        intentFile().delete();
        try { cleanup(); }
        catch (IOException failure) { LogUtil.w("ResourceDrafts", "Draft cleanup deferred after close", failure); }
        // Retain the small CLOSED manifest so an older saved-state reference cannot resurrect drafts.
    }

    private void requireOwner() throws IOException {
        if (gate.owner != owner || gate.closed) throw new InterruptedIOException("Obsolete draft owner");
    }

    private void cleanup() throws IOException {
        faults.at("before_cleanup");
        Set<String> referenced = new HashSet<>();
        for (int i = 0; i < manifest.pages.length; i++) {
            PageRef ref = manifest.pages[i];
            if (ref.dirty && ResourceEditorModelBlocks.FORMAT.equals(ref.format)) {
                for (ModelBlockRef block : ref.blocks) referenced.add(segmentFile(i, block.object).getName());
                continue;
            }
            if (ref.dirty) {
                referenced.add(pageFile(i, ref).getName());
                if (ref.chunks != null) for (ChunkRef chunk : ref.chunks.values()) referenced.add(chunkFile(chunk).getName());
            }
        }
        File[] files = new File(directory, "pages").listFiles();
        if (files != null) for (File file : files) {
            if (!referenced.contains(file.getName()) && file.isFile()
                    && (file.getName().matches("[0-5]-[0-9a-f-]{36}\\.(json|bin|seg)")
                    || file.getName().matches("\\.resource-.*\\.tmp"))) {
                if (!file.delete()) LogUtil.w("ResourceDrafts", "Unable to remove obsolete draft: " + file.getName());
            }
        }
        File[] temporaryManifests = directory.listFiles(file -> file.getName().matches("\\.resource-.*\\.tmp"));
        if (temporaryManifests != null) for (File file : temporaryManifests) file.delete();
        File intent = intentFile();
        if (intent.isFile()) {
            boolean anyDirty = false;
            for (PageRef ref : manifest.pages) {
                if (ref.dirty) {
                    anyDirty = true;
                    break;
                }
            }
            long intentGeneration = intentGeneration(intent);
            if (manifest.closed || !anyDirty
                    || (intentGeneration >= 0 && intentGeneration != manifest.generation)) {
                if (!intent.delete()) LogUtil.w("ResourceDrafts", "Unable to remove stale XML commit intent");
            }
        }
    }

    /** Only CLOSED sessions are age-pruned. Recoverable unsaved sessions are never evicted. */
    static void pruneClosedSessions(File root) {
        if (Files.isSymbolicLink(root.toPath())) return;
        File[] sessions = root.listFiles();
        if (sessions == null) return;
        long cutoff = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(7);
        for (File directory : sessions) pruneClosedSession(directory, Set.of(), cutoff);
    }

    static void pruneClosedSession(File directory, Set<String> activeSessions, long cutoff) {
        if (Files.isSymbolicLink(directory.getParentFile().toPath()) || !directory.isDirectory()
                || !directory.getName().matches("[0-9a-f-]{36}") || activeSessions.contains(directory.getName())
                || Files.isSymbolicLink(directory.toPath())) return;
        File manifest = new File(directory, "manifest.json");
        if (Files.isSymbolicLink(manifest.toPath()) || !manifest.isFile() || manifest.lastModified() >= cutoff) return;
        try {
            Manifest saved = GSON.fromJson(read(manifest), Manifest.class);
            if (saved == null || saved.schema != SCHEMA || !saved.closed || !directory.getName().equals(saved.session)) return;
            File pages = new File(directory, "pages");
            if (Files.isSymbolicLink(pages.toPath())) return;
            File[] objects = pages.listFiles();
            // A queued sweep must not act on owner eligibility captured in an earlier batch.
            if (activeSessions.contains(directory.getName())) return;
            if (objects != null) for (File object : objects) {
                if (object.isFile() && (object.getName().matches("[0-5]-[0-9a-f-]{36}\\.(json|bin|seg)")
                        || object.getName().matches("\\.resource-.*\\.tmp"))) object.delete();
            }
            if (pages.exists() && !pages.delete()) return;
            File[] temporary = directory.listFiles(file -> file.getName().matches("\\.resource-.*\\.tmp"));
            if (temporary != null) for (File file : temporary) file.delete();
            File intent = new File(directory, "xml-commit.json");
            if (intent.isFile()) intent.delete();
            manifest.delete();
            directory.delete();
        } catch (IOException | RuntimeException failure) {
            LogUtil.w("ResourceDrafts", "Closed-session cleanup deferred", failure);
        }
    }

    private File pageFile(int page, PageRef ref) {
        return new File(new File(directory, "pages"), page + "-" + ref.object + (ref.format == null ? ".json" : ".bin"));
    }
    private static String read(File file) throws IOException { return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); }
    private static String digest(byte[] bytes) { return hex(newDigest().digest(bytes)); }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static String hex(byte[] digest) {
        char[] result = new char[digest.length * 2];
        String digits = "0123456789abcdef";
        for (int i = 0; i < digest.length; i++) {
            int value = digest[i] & 255;
            result[i * 2] = digits.charAt(value >>> 4);
            result[i * 2 + 1] = digits.charAt(value & 15);
        }
        return new String(result);
    }

    private Manifest fresh() {
        Manifest result = new Manifest();
        result.session = session; result.project = project; result.variant = variant;
        for (int i = 0; i < result.pages.length; i++) result.pages[i] = new PageRef();
        return result;
    }
    private void validate(Manifest saved) throws IOException {
        if (saved == null || saved.schema != SCHEMA || saved.closed || !session.equals(saved.session)
                || !project.equals(saved.project) || !variant.equals(saved.variant)
                || saved.pages == null || saved.pages.length != ResourceEditorTabs.COUNT || saved.generation < 1) {
            throw new IOException("Invalid, closed, or mismatched draft session");
        }
    }
    private static final class XmlCommitIntent {
        int schema = 1;
        String session, project, variant;
        long generation;
        Map<Integer, List<String>> pages;
    }
    private static final class Manifest {
        int schema = SCHEMA;
        String session, project, variant;
        long generation;
        boolean closed;
        PageRef[] pages = new PageRef[ResourceEditorTabs.COUNT];
    }
    private static final class PageRef {
        long revision;
        boolean dirty;
        String object, checksum, format;
        long baseRevision;
        int rows;
        Map<Integer, ChunkRef> chunks;
        List<ModelBlockRef> blocks;
    }
    /** Nullable identity fields distinguish missing JSON headers from valid zero values. */
    private static final class ModelBlockData<T> {
        Integer schema, page, block;
        Long revision;
        String session, project, variant;
        List<T> entries;
        Map<Integer, String> notes;
    }
    private static final class ModelBlockRef {
        String object, checksum;
        long offset, length, revision;
    }
    private static final class ChunkRef {
        long revision;
        String object, checksum;
    }
    private static final class PageData {
        int schema = SCHEMA;
        String session, project, variant;
        int page;
        long revision;
        List<Object> entries;
        Map<Integer, String> notes;
        Integer block;
    }
}
