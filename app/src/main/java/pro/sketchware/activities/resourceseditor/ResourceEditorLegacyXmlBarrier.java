package pro.sketchware.activities.resourceseditor;

import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Serial-IO metadata. A legacy source can outlive its session UUID and CLOSED manifest. */
final class ResourceEditorLegacyXmlBarrier {
    private static final Gson GSON = new Gson();
    private static final String MISSING = "missing";
    private final String project;
    private final String variant;
    private final String session;
    private final String[] xmlPaths;
    private final ResourceEditorDraftStore.Faults faults;
    private final File directory;
    private final File scopeFile;
    private volatile File file;
    private String scope;
    private volatile boolean pendingSources;
    private final Map<Integer, Set<String>> remembered = new HashMap<>();

    ResourceEditorLegacyXmlBarrier(File root, String project, String variant, String[] xmlPaths,
                                   ResourceEditorDraftStore.Faults faults) {
        this(root, project, variant, UUID.randomUUID().toString(), xmlPaths, faults);
    }

    ResourceEditorLegacyXmlBarrier(File root, String project, String variant, String session, String[] xmlPaths,
                                   ResourceEditorDraftStore.Faults faults) {
        this.project = project; this.variant = variant;
        this.session = session;
        this.xmlPaths = xmlPaths == null ? null : xmlPaths.clone();
        this.faults = faults;
        directory = new File(root, "legacy-xml-barriers");
        scopeFile = xmlPaths != null && xmlPaths.length > 0 && xmlPaths[0] != null
                ? new File(new File(xmlPaths[0]).getAbsoluteFile().getParentFile(), ".resource-editor-legacy-scope") : null;
        if (scopeFile == null) file = barrierFile("private-root");
    }

    boolean hasHistory() {
        return pendingSources || scopeFile != null && (scopeFile.exists() || Files.isSymbolicLink(scopeFile.toPath()))
                || file != null && (file.exists() || Files.isSymbolicLink(file.toPath()));
    }

    private File barrierFile(String token) { return new File(directory, hash(tuple(tuple(project, variant), token)) + ".json"); }

    private File resolve(boolean create) throws IOException {
        if (scopeFile == null) return file;
        if (Files.isSymbolicLink(scopeFile.toPath())) throw new IOException("Invalid legacy project scope");
        if (!scopeFile.exists()) {
            if (scope != null) throw new IOException("Legacy project scope disappeared");
            if (!create) return null;
            createScope();
        }
        if (!scopeFile.isFile()) throw new IOException("Invalid legacy project scope");
        Scope saved;
        try {
            saved = GSON.fromJson(new String(Files.readAllBytes(scopeFile.toPath()), StandardCharsets.UTF_8), Scope.class);
            if (saved == null || saved.schema == null || saved.schema != 1 || saved.project == null || saved.variant == null
                    || saved.token == null || !saved.token.equals(UUID.fromString(saved.token).toString()))
                throw new IOException("Invalid legacy project scope");
        } catch (RuntimeException invalid) { throw new IOException("Invalid legacy project scope", invalid); }
        if (!project.equals(saved.project) || !variant.equals(saved.variant)) {
            if (scope != null) throw new IOException("Legacy project scope identity changed");
            if (!create) return null;
            createScope();
            return resolve(false);
        }
        String token = saved.token;
        if (scope != null && !scope.equals(token)) throw new IOException("Legacy project scope mismatch");
        scope = token; file = barrierFile(token);
        return file;
    }

    private void createScope() throws IOException {
        Scope saved = new Scope(); saved.schema = 1; saved.project = project; saved.variant = variant;
        saved.token = UUID.randomUUID().toString();
        faults.at("before_legacy_scope");
        ResourceXmlWriter.write(scopeFile, writer -> GSON.toJson(saved, writer));
    }

    static String fingerprint(Bundle page) {
        return fingerprint(page.getString("entries"), page.getString("notes"));
    }

    static String fingerprint(String entries, String notes) {
        return hash(tuple(canonical(entries), canonical(notes)));
    }

    private static String tuple(String a, String b) { return a.length() + ":" + a + b.length() + ":" + b; }

    private static String canonical(String json) {
        if (json == null) return "null-source";
        try { return "json:" + GSON.toJson(sorted(JsonParser.parseString(json), 0)); }
        catch (RuntimeException invalid) { return "raw:" + json; }
    }

    private static JsonElement sorted(JsonElement value, int depth) {
        if (depth > 64) throw new IllegalArgumentException("Legacy JSON nesting limit");
        if (value.isJsonObject()) {
            JsonObject result = new JsonObject();
            for (String key : new TreeSet<>(value.getAsJsonObject().keySet()))
                result.add(key, sorted(value.getAsJsonObject().get(key), depth + 1));
            return result;
        }
        if (value.isJsonArray()) {
            JsonArray result = new JsonArray();
            for (JsonElement entry : value.getAsJsonArray()) result.add(sorted(entry, depth + 1));
            return result;
        }
        return value;
    }

    void remember(int page, String source) {
        remembered.computeIfAbsent(page, ignored -> new HashSet<>()).add(source);
        pendingSources = true;
    }

    /** Sources are registered before their Bundle can be released after a durable checkpoint. */
    void flushSources() throws IOException {
        if (remembered.isEmpty()) return;
        State state = read(true);
        boolean changed = false;
        for (var page : remembered.entrySet()) {
            Map<String, Entry> sources = state.pages.computeIfAbsent(page.getKey(), ignored -> new HashMap<>());
            for (String source : page.getValue()) {
                Entry entry = sources.get(source);
                if (entry == null) {
                    entry = new Entry(); entry.confirmed = false; entry.targets = new ArrayList<>();
                    entry.sessions = new ArrayList<>(); sources.put(source, entry); changed = true;
                }
                if (!entry.confirmed && !entry.sessions.contains(session)) { entry.sessions.add(session); changed = true; }
            }
        }
        if (changed) write(state, "before_legacy_registry");
        remembered.clear();
        pendingSources = false;
    }

    boolean superseded(int page, String source) throws IOException {
        State state = read(remembered.containsKey(page) && remembered.get(page).contains(source));
        Map<String, Entry> sources = state.pages.get(page);
        Entry entry = sources == null ? null : sources.get(source);
        if (entry == null || !entry.confirmed && entry.targets.isEmpty()) return false;
        if (entry.confirmed) return true;
        String current = xmlState(page);
        if (entry.targets.contains(current)) return true;
        if (current.equals(entry.base)) return false;
        throw new IOException("Unable to resolve legacy XML commit for page " + page);
    }

    /** Returns a confirmation token, or null when every known source is already superseded. */
    String prepare(int page, String xml) throws IOException {
        flushSources();
        State state = read(false);
        Map<String, Entry> sources = state.pages.get(page);
        if (sources == null || sources.values().stream().allMatch(entry -> entry.confirmed)) return null;
        String current = xmlState(page);
        String target = null;
        boolean changed = false;
        for (Entry entry : sources.values()) {
            if (entry.confirmed) continue;
            if (entry.targets.contains(current)) {
                confirm(entry); changed = true;
                continue;
            }
            if (target == null) target = hash(xml);
            if (entry.targets.isEmpty()) entry.base = current;
            if (!entry.targets.contains(target)) { entry.targets.add(target); changed = true; }
        }
        if (changed) write(state, "before_legacy_xml_intent");
        faults.at("after_legacy_xml_intent");
        return target;
    }

    void confirm(int page, String target) throws IOException {
        if (target == null) return;
        State state = read(false);
        Map<String, Entry> sources = state.pages.get(page);
        boolean changed = false;
        if (sources != null) for (Entry entry : sources.values()) {
            if (!entry.confirmed && entry.targets.contains(target)) { confirm(entry); changed = true; }
        }
        if (changed) write(state, "before_legacy_xml_confirm");
    }

    private static void confirm(Entry entry) {
        entry.confirmed = true; entry.base = null; entry.targets.clear(); entry.sessions.clear();
    }

    /** A successful close is also an explicit discard of this session's legacy inputs. */
    void discardSession() throws IOException {
        flushSources();
        State state = read(false);
        boolean changed = false;
        for (Map<String, Entry> sources : state.pages.values()) for (Entry entry : sources.values()) {
            if (!entry.confirmed && entry.sessions.contains(session)) { confirm(entry); changed = true; }
        }
        if (changed) write(state, "before_legacy_discard");
    }

    private String xmlState(int page) throws IOException {
        if (xmlPaths == null || page >= xmlPaths.length || xmlPaths[page] == null)
            throw new IOException("Missing resource XML path for legacy barrier");
        faults.at("before_legacy_barrier_xml_read");
        if (ResourceEditorDraftStore.xmlReadFault.test(page)) throw new IOException("Injected XML read failure");
        File xml = new File(xmlPaths[page]);
        if (!xml.exists()) return MISSING;
        if (!xml.isFile()) throw new IOException("Resource XML is not a file");
        return hash(Files.readAllBytes(xml.toPath()));
    }

    private State read(boolean createScope) throws IOException {
        File resolved = resolve(createScope);
        if (resolved != null && Files.isSymbolicLink(resolved.toPath())) throw new IOException("Invalid legacy XML barrier path");
        if (resolved != null && !resolved.exists() && scopeFile != null && !createScope)
            throw new IOException("Legacy XML source registry is missing");
        if (resolved == null || !resolved.exists()) {
            State state = new State(); state.schema = 1; state.project = project; state.variant = variant;
            state.pages = new HashMap<>();
            return state;
        }
        if (!resolved.isFile()) throw new IOException("Invalid legacy XML barrier path");
        try {
            State state = GSON.fromJson(new String(Files.readAllBytes(resolved.toPath()), StandardCharsets.UTF_8), State.class);
            if (state == null || state.schema == null || state.schema != 1 || !project.equals(state.project)
                    || !variant.equals(state.variant) || state.pages == null) throw new IOException("Invalid legacy XML barrier identity");
            for (var page : state.pages.entrySet()) {
                if (page.getKey() == null || page.getKey() < 0 || page.getKey() >= ResourceEditorTabs.COUNT
                        || page.getValue() == null) throw new IOException("Invalid legacy barrier page");
                for (var source : page.getValue().entrySet()) {
                    Entry entry = source.getValue();
                    if (!isHash(source.getKey()) || entry == null || entry.confirmed == null || entry.targets == null
                            || entry.targets.stream().anyMatch(target -> !isHash(target))
                            || entry.confirmed && (entry.base != null || !entry.targets.isEmpty())
                            || !entry.confirmed && entry.targets.isEmpty() && entry.base != null
                            || !entry.targets.isEmpty() && !(MISSING.equals(entry.base) || isHash(entry.base)))
                        throw new IOException("Invalid legacy barrier source");
                    // Initial in-development metadata did not record the importing session.
                    if (entry.sessions == null) entry.sessions = new ArrayList<>();
                    for (String owner : entry.sessions) {
                        if (owner == null || !owner.equals(UUID.fromString(owner).toString()))
                            throw new IOException("Invalid legacy source owner");
                    }
                }
            }
            return state;
        } catch (RuntimeException invalid) { throw new IOException("Invalid legacy XML barrier", invalid); }
    }

    private void write(State state, String point) throws IOException {
        faults.at(point);
        ResourceXmlWriter.write(resolve(true), writer -> GSON.toJson(state, writer));
    }

    private static boolean isHash(String value) { return value != null && value.matches("[0-9a-f]{64}"); }
    private static String hash(String value) { return hash(value.getBytes(StandardCharsets.UTF_8)); }
    private static String hash(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            char[] hex = "0123456789abcdef".toCharArray(); char[] out = new char[digest.length * 2];
            for (int i = 0; i < digest.length; i++) { out[i*2] = hex[(digest[i]&255) >>> 4]; out[i*2+1] = hex[digest[i]&15]; }
            return new String(out);
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static final class State {
        Integer schema;
        String project, variant;
        Map<Integer, Map<String, Entry>> pages;
    }
    private static final class Scope {
        Integer schema;
        String project, variant, token;
    }
    private static final class Entry {
        Boolean confirmed;
        String base;
        List<String> targets;
        List<String> sessions;
    }
}
