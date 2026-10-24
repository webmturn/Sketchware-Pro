package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Intent;
import android.view.WindowManager;
import androidx.appcompat.widget.Toolbar;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import io.github.rosemoe.sora.widget.CodeEditor;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import pro.sketchware.R;
import pro.sketchware.activities.editor.code.SrcCodeEditor;

@RunWith(AndroidJUnit4.class)
public class SourceXmlFormattingTest {
    private static final String XML = "<resources>\n<string name=\"key\">Value</string>\n</resources>";
    private static final int FORMAT = 7;

    @Test public void toolbarFormattingRunsOffMainAndMatchesExistingOutput() throws Exception {
        try (Fixture f = new Fixture(XML); Blocker blocked = new Blocker()) {
            f.run(a -> { f.format(a); assertEquals(XML, f.editor(a).getText().toString()); assertFalse(f.toolbar(a).getMenu().findItem(FORMAT).isEnabled()); });
            AtomicBoolean marker = new AtomicBoolean(); f.run(a -> marker.set(true)); assertTrue(marker.get());
            assertEquals(XML, new String(Files.readAllBytes(f.file.toPath()), StandardCharsets.UTF_8));
            blocked.release(); f.await();
            f.run(a -> assertEquals(SrcCodeEditor.prettifyXml(XML, 4, null), f.editor(a).getText().toString()));
        }
    }

    @Test public void typingDuringFormattingCannotBeOverwritten() throws Exception {
        try (Fixture f = new Fixture(XML); Blocker blocked = new Blocker()) {
            f.run(a -> { f.format(a); f.editor(a).getText().insert(0, 0, "<!--new input-->\n"); });
            blocked.release(); f.await();
            f.run(a -> assertEquals("<!--new input-->\n" + XML, f.editor(a).getText().toString()));
        }
    }

    @Test public void replacingDocumentCannotBeOverwritten() throws Exception {
        try (Fixture f = new Fixture(XML); Blocker blocked = new Blocker()) {
            f.run(a -> { f.format(a); f.editor(a).setText("<resources><string name=\"later\">Later</string></resources>"); });
            blocked.release(); f.await();
            f.run(a -> assertEquals("<resources><string name=\"later\">Later</string></resources>", f.editor(a).getText().toString()));
        }
    }

    @Test public void newerRequestWinsAndCancelledQueueEntryCannotApply() throws Exception {
        String later = "<resources>\n<string name=\"later\">Later</string>\n</resources>";
        try (Fixture f = new Fixture(XML); Blocker blocked = new Blocker()) {
            f.run(a -> {
                f.format(a); f.editor(a).setText(later);
                try { Method method = SrcCodeEditor.class.getDeclaredMethod("formatXmlAsync"); method.setAccessible(true); method.invoke(a); }
                catch (Exception e) { throw new AssertionError(e); }
            });
            blocked.release(); f.await();
            f.run(a -> assertEquals(SrcCodeEditor.prettifyXml(later, 4, null), f.editor(a).getText().toString()));
        }
    }

    @Test public void recreationCancelsOldRequestAndDoesNotModifyNewPage() throws Exception {
        try (Fixture f = new Fixture(XML); Blocker blocked = new Blocker()) {
            final Future<?>[] old = new Future<?>[1];
            f.run(a -> { f.format(a); old[0] = (Future<?>) field(a, "xmlFormatTask"); });
            f.scenario.recreate(); assertTrue(old[0].isCancelled());
            blocked.release(); f.await();
            f.run(a -> assertEquals(XML, f.editor(a).getText().toString()));
        }
    }

    @Test public void malformedXmlKeepsDocumentAndAllowsRetry() throws Exception {
        String invalid = "<resources><string>Broken</resources>";
        try (Fixture f = new Fixture(invalid)) {
            f.run(f::format); f.await();
            f.run(a -> { assertEquals(invalid, f.editor(a).getText().toString()); f.editor(a).setText(XML); f.format(a); });
            f.await(); f.run(a -> assertEquals(SrcCodeEditor.prettifyXml(XML, 4, null), f.editor(a).getText().toString()));
        }
    }

    @Test public void headerOptionStillUsesPresenceAndExternalEntitiesStayUnexpanded() throws Exception {
        try (Fixture f = new Fixture(XML, true)) {
            f.run(f::format); f.await();
            f.run(a -> { String actual = f.editor(a).getText().toString(); assertFalse(actual.startsWith("<?xml"));
                assertEquals(SrcCodeEditor.prettifyXml(XML, 4, new Intent().putExtra("disableHeader", false)), actual); });
            String secured = SrcCodeEditor.prettifyXml("<!DOCTYPE r [<!ENTITY secret SYSTEM '" + f.file.toURI() + "'>]><r>&secret;</r>", 4, null);
            assertTrue(secured == null || !secured.contains("Value"));
        }
    }

    private static Object field(Object owner, String name) {
        try { Field f = owner.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(owner); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    private static ExecutorService executor() {
        try { Field f = SrcCodeEditor.class.getDeclaredField("XML_FORMATTER"); f.setAccessible(true); return (ExecutorService) f.get(null); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    static final class Blocker implements AutoCloseable {
        final CountDownLatch entered = new CountDownLatch(1), released = new CountDownLatch(1);
        Blocker() throws Exception { executor().execute(() -> { entered.countDown(); try { released.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }); assertTrue(entered.await(5, TimeUnit.SECONDS)); }
        void release() { released.countDown(); }
        @Override public void close() { release(); }
    }

    static final class Fixture implements AutoCloseable {
        final int originalLanguage = SrcCodeEditor.languageId;
        final File directory, file;
        final ActivityScenario<SrcCodeEditor> scenario;
        Fixture(String source) throws Exception { this(source, false); }
        Fixture(String source, boolean omit) throws Exception {
            var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            directory = new File(context.getCacheDir(), "XmlFormat." + UUID.randomUUID()); assertTrue(directory.mkdir());
            file = new File(directory, "source.xml"); Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
            Intent intent = new Intent(context, SrcCodeEditor.class).putExtra("title", "source.xml").putExtra("content", file.getAbsolutePath()).putExtra("xml", true);
            if (omit) intent.putExtra("disableHeader", false);
            scenario = ActivityScenario.launch(intent); run(a -> a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        }
        CodeEditor editor(SrcCodeEditor a) { return a.findViewById(R.id.editor); }
        Toolbar toolbar(SrcCodeEditor a) { return a.findViewById(R.id.toolbar); }
        void format(SrcCodeEditor a) { assertTrue(toolbar(a).getMenu().performIdentifierAction(FORMAT, 0)); }
        void run(Consumer<SrcCodeEditor> action) { scenario.onActivity(action::accept); }
        void await() throws Exception {
            executor().submit(() -> {}).get(20, TimeUnit.SECONDS);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            run(a -> { assertNull(field(a, "xmlFormatTask")); assertTrue(toolbar(a).getMenu().findItem(FORMAT).isEnabled()); });
        }
        @Override public void close() throws Exception {
            scenario.close(); SrcCodeEditor.languageId = originalLanguage;
            assertEquals(directory.getCanonicalFile(), file.getCanonicalFile().getParentFile());
            Files.deleteIfExists(file.toPath()); Files.delete(directory.toPath());
        }
    }
}
