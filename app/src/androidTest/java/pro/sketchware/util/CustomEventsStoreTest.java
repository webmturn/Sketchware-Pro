package pro.sketchware.util;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import pro.sketchware.beans.CustomEvent;
import pro.sketchware.beans.CustomListener;

@RunWith(AndroidJUnit4.class)
public class CustomEventsStoreTest {
    private File directory, eventsFile, listenersFile;
    private CustomEventsStore store;

    @Before public void setUp() throws Exception {
        directory = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "definitions-test-").toFile();
        eventsFile = new File(directory, "events.json");
        listenersFile = new File(directory, "listeners.json");
        store = new CustomEventsStore(eventsFile, listenersFile);
    }

    @After public void cleanUp() throws Exception {
        for (File file : directory.listFiles()) Files.deleteIfExists(file.toPath());
        Files.deleteIfExists(directory.toPath());
    }

    @Test public void unreadableFileCannotBeOverwrittenByASave() throws Exception {
        writeText(eventsFile, "{broken-json}");
        try {
            store.saveBoth(List.of(), List.of());
            fail("Unreadable definitions must block saving");
        } catch (IOException expected) {
            assertEquals("{broken-json}", read(eventsFile));
            assertFalse(listenersFile.exists());
            assertFalse(new File(directory, "custom-events-rollback.json").exists());
        }
    }

    @Test public void interruptedRenameRecoversBothOriginalFilesBeforeReading() throws Exception {
        String originalEvents = "[ {\"name\":\"original\",\"listener\":\"old\"} ]\n";
        String originalListeners = "[ {\"name\":\"old\",\"code\":\"旧代码\"} ]\n";
        writeText(eventsFile, "[{\"name\":\"original\",\"listener\":\"new\"}]");
        writeText(listenersFile, originalListeners);
        DefinitionFiles.write(new File(directory, "custom-events-rollback.json"),
                Map.of("version", 1, "events", originalEvents, "listeners", originalListeners));
        assertEquals("old", store.readEvents().get(0).getListener());
        assertEquals("old", store.readListeners().get(0).getName());
        assertEquals(originalEvents, read(eventsFile));
        assertEquals(originalListeners, read(listenersFile));
        assertFalse(new File(directory, "custom-events-rollback.json").exists());
    }

    @Test public void completedSaveKeepsNewPairOnNextOpen() throws Exception {
        CustomEvent event = new CustomEvent(); event.setName("event"); event.setListener("new");
        CustomListener listener = new CustomListener(); listener.setName("new");
        store.saveBoth(List.of(event), List.of(listener));
        CustomEventsStore reopened = new CustomEventsStore(eventsFile, listenersFile);
        assertEquals("new", reopened.readEvents().get(0).getListener());
        assertEquals("new", reopened.readListeners().get(0).getName());
        assertFalse(new File(directory, "custom-events-rollback.json").exists());
    }

    @Test public void invalidRecoveryRecordCannotDeleteDefinitions() throws Exception {
        writeText(eventsFile, "[]");
        writeText(listenersFile, "[]");
        writeText(new File(directory, "custom-events-rollback.json"), "{}");
        try {
            store.readEvents();
            fail("Invalid recovery record must block reads");
        } catch (IOException expected) {
            assertEquals("[]", read(eventsFile));
            assertEquals("[]", read(listenersFile));
        }
    }
    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void writeText(File file, String text) throws IOException {
        Files.write(file.toPath(), text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
