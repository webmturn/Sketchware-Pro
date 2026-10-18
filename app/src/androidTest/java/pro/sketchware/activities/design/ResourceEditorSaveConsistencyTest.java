package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.inspector.WindowInspector;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.ResourceEditorSaveResult;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.SketchwarePaths;

/**
 * Task 1 regression: per-page XML results, draft failure degradation, restore-reference safety,
 * RESULT_OK and retry. Faults are injected only through the draft store test hook or a blocked
 * destination directory; no user project or device storage is modified.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class ResourceEditorSaveConsistencyTest {
    private static final String STORE_CLASS =
            "pro.sketchware.activities.resourceseditor.ResourceEditorDraftStore";

    @After
    public void resetFault() {
        try {
            setFault(point -> { });
        } catch (Exception ignored) {
        }
    }

    @Test
    public void saveAndExitReturnsOkAndWritesXml() throws Exception {
        withEditor((fixture, scenario) -> {
            File xml = resourceFile(fixture.id, "strings.xml");
            on(scenario, a -> {
                a.stringsEditor.addString("saved_one", "kept", "note");
                call(a, "saveAndExit");
                assertTrue("save and exit must finish", a.isFinishing());
                assertEquals("caller must receive RESULT_OK", Activity.RESULT_OK, resultCode(a));
            });
            assertTrue(read(xml).contains("saved_one"));
        });
    }

    @Test
    public void draftFailureXmlOnlyDiscardsOldDraftAndKeepsSavedContent() throws Exception {
        withEditor((fixture, scenario) -> {
            File xml = resourceFile(fixture.id, "strings.xml");
            String[] oldSession = new String[1];
            on(scenario, a -> {
                a.stringsEditor.addString("draft_a", "A", "");
                call(a, "protectDrafts");
                oldSession[0] = session(a);
            });
            SystemClock.sleep(300);

            AtomicInteger remaining = new AtomicInteger(1);
            setFault(point -> {
                if ("before_manifest".equals(point) && remaining.getAndDecrement() > 0) {
                    throw new RuntimeException("injected draft protection failure");
                }
            });
            ResourceEditorSaveResult[] failed = new ResourceEditorSaveResult[1];
            on(scenario, a -> {
                a.stringsEditor.addString("draft_b", "B", "");
                failed[0] = (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                assertTrue("draft failure must offer the explicit XML-only path", failed[0].canSaveXmlOnly);
                assertFalse("default path must not have written XML", failed[0].xmlAttempted);
            });
            assertFalse("XML must stay untouched while the default save is blocked",
                    read(xml).contains("draft_b"));

            setFault(point -> { });
            on(scenario, a -> {
                callWithNull(a, "performXmlOnlySave", Runnable.class);
                assertNotEquals("XML-only save must rotate to a new session", oldSession[0], session(a));
            });
            assertTrue(read(xml).contains("draft_b"));
            assertTrue("old draft session must be CLOSED", manifestClosed(oldSession[0]));

            scenario.recreate();
            awaitLoaded(scenario);
            on(scenario, a -> {
                assertTrue("saved B must survive recreation", hasString(a, "draft_b"));
                assertTrue("saved B must keep the earlier row too", hasString(a, "draft_a"));
                assertFalse(a.stringsEditor.hasUnsavedChanges);
            });
        });
    }

    @Test
    public void partialXmlFailureKeepsFailedPageDirtyAndRetrySucceeds() throws Exception {
        withEditor((fixture, scenario) -> {
            File stringsXml = resourceFile(fixture.id, "strings.xml");
            File colorsXml = resourceFile(fixture.id, "colors.xml");
            File blocked = new File(stringsXml.getParentFile(), "blocked-" + System.nanoTime());
            assertTrue(blocked.mkdir());
            byte[] originalStrings = Files.readAllBytes(stringsXml.toPath());
            try {
                ResourceEditorSaveResult[] first = new ResourceEditorSaveResult[1];
                on(scenario, a -> {
                    a.stringsEditor.addString("failed_probe", "recoverable", "");
                    a.colorsEditor.colorList.get(0).setColorValue("#445566");
                    a.colorsEditor.markDraftChanged();
                    a.stringsEditor.filePath = blocked.getPath();
                    first[0] = (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                    assertEquals(1, first[0].failedPages);
                    assertFalse(first[0].navigationAllowed);
                    assertTrue("failed strings page must stay dirty", a.stringsEditor.hasUnsavedChanges);
                    assertFalse("saved colors page must become clean", a.colorsEditor.hasUnsavedChanges);
                    assertFalse(a.isFinishing());
                });
                assertArrayEquals("failed XML must not be replaced", originalStrings,
                        Files.readAllBytes(stringsXml.toPath()));
                assertFalse(read(stringsXml).contains("failed_probe"));
                assertTrue(read(colorsXml).contains("#445566"));

                on(scenario, a -> {
                    a.stringsEditor.filePath = a.stringsFilePath;
                    ResourceEditorSaveResult retried =
                            (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                    assertEquals(0, retried.failedPages);
                    assertTrue(retried.navigationAllowed);
                    assertFalse(a.stringsEditor.hasUnsavedChanges);
                });
                assertTrue("retry must write the failed page", read(stringsXml).contains("failed_probe"));
            } finally {
                assertTrue(blocked.delete() || !blocked.exists());
            }
        });
    }

    @Test
    public void cleanCommitFailureKeepsWrittenContentAndRestoresDirtyB() throws Exception {
        withEditor((fixture, scenario) -> {
            File xml = resourceFile(fixture.id, "strings.xml");
            String[] session = new String[1];
            on(scenario, a -> {
                a.stringsEditor.addString("older_a", "A", "");
                notes(a).put(1, "clean note");
                call(a, "protectDrafts");
                session[0] = session(a);
            });
            AtomicInteger commits = new AtomicInteger();
            setFault(point -> {
                if ("before_manifest".equals(point) && commits.incrementAndGet() >= 2) {
                    throw new RuntimeException("injected persistent CLEAN commit failure");
                }
            });
            ResourceEditorSaveResult[] result = new ResourceEditorSaveResult[1];
            on(scenario, a -> {
                a.stringsEditor.addString("newer_b", "B", "");
                result[0] = (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                assertEquals(0, result[0].failedPages);
                assertEquals("XML is written but the CLEAN manifest is not confirmed",
                        ResourceEditorSaveResult.DraftState.CONTENT_PROTECTED, result[0].draftState);
                assertTrue("content is protected even if not clean", result[0].navigationAllowed);
            });
            assertTrue(read(xml).contains("newer_b"));
            assertTrue("the manifest before recreation must still be dirty B",
                    manifestPageDirty(session[0], 0));
            // Keep the fault active through the state save, so recreation cannot silently CLEAN.
            scenario.recreate();
            awaitLoaded(scenario);
            on(scenario, a -> {
                assertTrue("recreation must restore B, not the older draft", hasString(a, "newer_b"));
                assertEquals("draft comments must be restored", "clean note", notes(a).get(1));
                assertTrue("dirty B must still be dirty", a.stringsEditor.hasUnsavedChanges);
            });
            assertTrue("recreation must not repair the manifest", manifestPageDirty(session[0], 0));
            setFault(point -> { });
            on(scenario, a -> {
                ResourceEditorSaveResult retried =
                        (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                assertEquals(ResourceEditorSaveResult.DraftState.CLEAN_CONFIRMED, retried.draftState);
                assertFalse(a.stringsEditor.hasUnsavedChanges);
            });
            assertTrue(read(xml).contains("newer_b"));
            assertTrue("the retried XML must keep the comment", read(xml).contains("clean note"));
        });
    }

    @Test
    public void cleanCommitFailureRetryAfterFaultClearedSucceeds() throws Exception {
        withEditor((fixture, scenario) -> {
            File xml = resourceFile(fixture.id, "strings.xml");
            AtomicInteger commits = new AtomicInteger();
            setFault(point -> {
                if ("before_manifest".equals(point) && commits.incrementAndGet() == 2) {
                    throw new RuntimeException("injected CLEAN commit failure");
                }
            });
            ResourceEditorSaveResult[] blocked = new ResourceEditorSaveResult[1];
            on(scenario, a -> {
                a.stringsEditor.addString("retry_clean", "B", "");
                blocked[0] = (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                assertEquals(ResourceEditorSaveResult.DraftState.CONTENT_PROTECTED, blocked[0].draftState);
            });
            setFault(point -> { });
            on(scenario, a -> {
                ResourceEditorSaveResult retried =
                        (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                assertEquals(ResourceEditorSaveResult.DraftState.CLEAN_CONFIRMED, retried.draftState);
                assertFalse(a.stringsEditor.hasUnsavedChanges);
            });
            assertTrue(read(xml).contains("retry_clean"));
        });
    }

    @Test
    public void retryAfterDraftFailureCommitsLatestRevision() throws Exception {
        withEditor((fixture, scenario) -> {
            File xml = resourceFile(fixture.id, "strings.xml");
            AtomicInteger remaining = new AtomicInteger(1);
            setFault(point -> {
                if ("before_manifest".equals(point) && remaining.getAndDecrement() > 0) {
                    throw new RuntimeException("injected draft failure");
                }
            });
            on(scenario, a -> {
                a.stringsEditor.addString("retry_b", "B", "");
                ResourceEditorSaveResult blocked =
                        (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                assertTrue(blocked.canSaveXmlOnly);
            });
            setFault(point -> { });
            on(scenario, a -> {
                ResourceEditorSaveResult retried =
                        (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                assertEquals(0, retried.failedPages);
                assertEquals(ResourceEditorSaveResult.DraftState.CLEAN_CONFIRMED, retried.draftState);
                assertTrue(retried.navigationAllowed);
                assertFalse(a.stringsEditor.hasUnsavedChanges);
            });
            assertTrue(read(xml).contains("retry_b"));
        });
    }

    @Test
    public void closedFailureStaleUuidLoadsSavedXmlNotOldDraft() throws Exception {
        withEditor((fixture, scenario) -> {
            File xml = resourceFile(fixture.id, "strings.xml");
            String[] old = new String[1];
            on(scenario, a -> {
                a.stringsEditor.addString("draft_a", "A", "");
                call(a, "protectDrafts");
                old[0] = session(a);
            });
            SystemClock.sleep(300);
            setFault(point -> {
                if ("before_manifest".equals(point) || "before_close_manifest".equals(point)) {
                    throw new RuntimeException("injected draft/close failure");
                }
            });
            on(scenario, a -> {
                a.stringsEditor.addString("draft_b", "B", "");
                ResourceEditorSaveResult failed =
                        (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                assertTrue(failed.canSaveXmlOnly);
                callWithNull(a, "performXmlOnlySave", Runnable.class);
                assertTrue("close failure must mark the restore reference unsafe", unsafe(a));
                assertFalse("XML was written, so the page itself is clean", a.stringsEditor.hasUnsavedChanges);
            });
            assertTrue(read(xml).contains("draft_b"));

            Bundle saved = new Bundle();
            on(scenario, a -> {
                callWith(a, "onSaveInstanceState", Bundle.class, saved);
                assertEquals("unsafe state must still reference the old session for per-page recovery",
                        old[0], saved.getString("resource_draft_session"));
            });

            Application app = (Application) InstrumentationRegistry.getInstrumentation()
                    .getTargetContext().getApplicationContext();
            Application.ActivityLifecycleCallbacks stale = new EmptyCallbacks() {
                @Override
                public void onActivityPostSaveInstanceState(Activity activity, Bundle state) {
                    state.putString("resource_draft_session", old[0]);
                }
            };
            app.registerActivityLifecycleCallbacks(stale);
            try {
                scenario.recreate();
            } finally {
                app.unregisterActivityLifecycleCallbacks(stale);
            }
            awaitLoaded(scenario);
            on(scenario, a -> {
                assertEquals("recreate must open the stale old UUID", old[0], session(a));
                assertTrue("saved XML B must win over the old draft A", hasString(a, "draft_b"));
                assertFalse(a.stringsEditor.hasUnsavedChanges);
            });
            setFault(point -> { });
        });
    }

    @Test
    public void partialXmlStaleUuidKeepsFailedDraftButNotSavedXml() throws Exception {
        withEditor((fixture, scenario) -> {
            File stringsXml = resourceFile(fixture.id, "strings.xml");
            File colorsXml = resourceFile(fixture.id, "colors.xml");
            File blocked = new File(stringsXml.getParentFile(), "blocked-stale-" + System.nanoTime());
            assertTrue(blocked.mkdir());
            try {
                String[] old = new String[1];
                on(scenario, a -> {
                    a.stringsEditor.addString("draft_a", "A", "");
                    notes(a).put(1, "partial note");
                    call(a, "protectDrafts");
                    old[0] = session(a);
                });
                SystemClock.sleep(300);
                setFault(point -> {
                    if ("before_manifest".equals(point) || "before_close_manifest".equals(point)) {
                        throw new RuntimeException("injected draft/close failure");
                    }
                });
                on(scenario, a -> {
                    a.stringsEditor.addString("draft_b", "B", "");
                    a.colorsEditor.colorList.get(0).setColorValue("#445566");
                    a.colorsEditor.markDraftChanged();
                    a.stringsEditor.filePath = blocked.getPath();
                    ResourceEditorSaveResult failed =
                            (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                    assertTrue(failed.canSaveXmlOnly);
                    callWithNull(a, "performXmlOnlySave", Runnable.class);
                    assertTrue(unsafe(a));
                });
                assertTrue("saved colors XML must be B", read(colorsXml).contains("#445566"));
                assertFalse("blocked strings XML must stay at A", read(stringsXml).contains("draft_b"));

                Application app = (Application) InstrumentationRegistry.getInstrumentation()
                        .getTargetContext().getApplicationContext();
                Application.ActivityLifecycleCallbacks stale = new EmptyCallbacks() {
                    @Override
                    public void onActivityPostSaveInstanceState(Activity activity, Bundle state) {
                        state.putString("resource_draft_session", old[0]);
                    }
                };
                app.registerActivityLifecycleCallbacks(stale);
                try {
                    scenario.recreate();
                } finally {
                    app.unregisterActivityLifecycleCallbacks(stale);
                }
                awaitLoaded(scenario);
                on(scenario, a -> {
                    assertEquals("recreate must open the stale old UUID", old[0], session(a));
                    assertTrue("failed page keeps the last protected A", hasString(a, "draft_a"));
                    assertFalse("failed page B was never written", hasString(a, "draft_b"));
                    assertTrue("failed page stays dirty", a.stringsEditor.hasUnsavedChanges);
                    assertEquals("failed page comments must survive", "partial note", notes(a).get(1));
                    assertEquals("saved colors XML must not be replaced by A",
                            "#445566", a.colorsEditor.colorList.get(0).getColorValue());
                    assertFalse("saved colors page is clean", a.colorsEditor.hasUnsavedChanges);
                });
                setFault(point -> { });
            } finally {
                assertTrue(blocked.delete() || !blocked.exists());
            }
        });
    }

    @Test
    public void closedFailureBlocksBackAndVariantThenRetryExits() throws Exception {
        withEditor((fixture, scenario) -> {
            on(scenario, a -> {
                a.stringsEditor.addString("nav_b", "B", "");
            });
            setFault(point -> {
                if ("before_manifest".equals(point) || "before_close_manifest".equals(point)) {
                    throw new RuntimeException("injected draft/close failure");
                }
            });
            on(scenario, a -> {
                ResourceEditorSaveResult failed =
                        (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                assertTrue(failed.canSaveXmlOnly);
                callWithNull(a, "performXmlOnlySave", Runnable.class);
                assertTrue(unsafe(a));
            });
            // System back must be intercepted by the unsafe dialog, not finish.
            on(scenario, a -> {
                a.getOnBackPressedDispatcher().onBackPressed();
                assertFalse(a.isFinishing());
            });
            clickDialog(scenario, R.string.resources_editor_unsafe_navigation_title, android.R.id.button3);
            // Variant entry must be intercepted too.
            on(scenario, a -> {
                MaterialToolbar toolbar = a.findViewById(R.id.topAppBar);
                assertTrue(a.onOptionsItemSelected(toolbar.getMenu().findItem(R.id.action_select_variant)));
                assertFalse(a.isFinishing());
            });
            clickDialog(scenario, R.string.resources_editor_unsafe_navigation_title, android.R.id.button3);

            setFault(point -> { });
            on(scenario, a -> {
                ResourceEditorSaveResult retried =
                        (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                assertEquals(ResourceEditorSaveResult.DraftState.CLEAN_CONFIRMED, retried.draftState);
                assertFalse(unsafe(a));
                a.stringsEditor.addString("after_retry", "C", "");
                ResourceEditorSaveResult edited =
                        (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                assertEquals(0, edited.failedPages);
                assertFalse(unsafe(a));
            });
            on(scenario, a -> {
                a.getOnBackPressedDispatcher().onBackPressed();
                assertTrue(a.isFinishing());
                assertEquals(Activity.RESULT_OK, resultCode(a));
            });
        });
    }

    @Test
    public void closedFailureExplicitDiscardReturnsCanceled() throws Exception {
        withEditor((fixture, scenario) -> {
            on(scenario, a -> a.stringsEditor.addString("discard_b", "B", ""));
            setFault(point -> {
                if ("before_manifest".equals(point) || "before_close_manifest".equals(point)) {
                    throw new RuntimeException("injected draft/close failure");
                }
            });
            on(scenario, a -> {
                ResourceEditorSaveResult failed =
                        (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                assertTrue(failed.canSaveXmlOnly);
                callWithNull(a, "performXmlOnlySave", Runnable.class);
                assertTrue(unsafe(a));
                a.getOnBackPressedDispatcher().onBackPressed();
                View dialog = dialogWithTitle(a.getString(R.string.resources_editor_unsafe_navigation_title));
                assertNotNull(dialog);
                assertTrue(dialog.findViewById(android.R.id.button2).performClick());
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            on(scenario, a -> {
                assertTrue(a.isFinishing());
                assertEquals(Activity.RESULT_CANCELED, resultCode(a));
            });
            setFault(point -> { });
        });
    }

    @Test
    public void xmlOnlyDialogCancelKeepsDraftThenConfirmRotates() throws Exception {
        withEditor((fixture, scenario) -> {
            File xml = resourceFile(fixture.id, "strings.xml");
            String[] before = new String[1];
            on(scenario, a -> {
                a.stringsEditor.addString("dialog_b", "B", "");
                before[0] = session(a);
            });
            setFault(point -> {
                if ("before_manifest".equals(point)) {
                    throw new RuntimeException("injected draft failure");
                }
            });
            on(scenario, a -> callWithNull(a, "attemptSave", Runnable.class));
            clickDialog(scenario, R.string.resources_editor_draft_xml_only_title, android.R.id.button2);
            on(scenario, a -> {
                assertFalse("cancel must not write XML", read(xml).contains("dialog_b"));
                assertEquals("cancel must not rotate the session", before[0], session(a));
                assertTrue(a.stringsEditor.hasUnsavedChanges);
            });
            on(scenario, a -> callWithNull(a, "attemptSave", Runnable.class));
            on(scenario, a -> {
                View dialog = dialogWithTitle(a.getString(R.string.resources_editor_draft_xml_only_title));
                assertNotNull(dialog);
                setFault(point -> { });
                assertTrue(dialog.findViewById(android.R.id.button1).performClick());
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertTrue("confirm must write XML", read(xml).contains("dialog_b"));
            on(scenario, a -> {
                assertNotEquals("confirm must rotate the session", before[0], session(a));
                assertFalse(unsafe(a));
                assertFalse(a.stringsEditor.hasUnsavedChanges);
            });
        });
    }

    @Test
    public void partialXmlRetryMergesEarlierSavedPage() throws Exception {
        withEditor((fixture, scenario) -> {
            File stringsXml = resourceFile(fixture.id, "strings.xml");
            File colorsXml = resourceFile(fixture.id, "colors.xml");
            File blocked = new File(stringsXml.getParentFile(), "blocked-merge-" + System.nanoTime());
            assertTrue(blocked.mkdir());
            try {
                String[] old = new String[1];
                on(scenario, a -> {
                    a.stringsEditor.addString("merge_a", "A", "");
                    a.colorsEditor.colorList.get(0).setColorValue("#112233");
                    a.colorsEditor.markDraftChanged();
                    call(a, "protectDrafts");
                    old[0] = session(a);
                });
                SystemClock.sleep(300);
                setFault(point -> {
                    if ("before_manifest".equals(point) || "before_close_manifest".equals(point)) {
                        throw new RuntimeException("injected draft/close failure");
                    }
                });
                on(scenario, a -> {
                    a.stringsEditor.addString("merge_b", "B", "");
                    a.colorsEditor.colorList.get(0).setColorValue("#445566");
                    a.colorsEditor.markDraftChanged();
                    a.stringsEditor.filePath = blocked.getPath();
                    ResourceEditorSaveResult failed =
                            (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                    assertTrue(failed.canSaveXmlOnly);
                    callWithNull(a, "performXmlOnlySave", Runnable.class);
                    assertTrue(unsafe(a));
                });
                assertTrue(read(colorsXml).contains("#445566"));
                assertFalse(read(stringsXml).contains("merge_b"));

                // Colors is clean now; only strings is dirty. The retry must merge the old colors hash.
                on(scenario, a -> {
                    a.stringsEditor.filePath = a.stringsFilePath;
                    ResourceEditorSaveResult failed =
                            (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                    assertTrue(failed.canSaveXmlOnly);
                    callWithNull(a, "performXmlOnlySave", Runnable.class);
                    assertTrue(unsafe(a));
                });
                assertTrue(read(stringsXml).contains("merge_b"));

                Application app = (Application) InstrumentationRegistry.getInstrumentation()
                        .getTargetContext().getApplicationContext();
                Application.ActivityLifecycleCallbacks stale = new EmptyCallbacks() {
                    @Override
                    public void onActivityPostSaveInstanceState(Activity activity, Bundle state) {
                        state.putString("resource_draft_session", old[0]);
                    }
                };
                app.registerActivityLifecycleCallbacks(stale);
                try {
                    scenario.recreate();
                } finally {
                    app.unregisterActivityLifecycleCallbacks(stale);
                }
                awaitLoaded(scenario);
                on(scenario, a -> {
                    assertEquals(old[0], session(a));
                    assertTrue("retried strings B must win", hasString(a, "merge_b"));
                    assertEquals("the earlier saved colors B must keep its merged hash",
                            "#445566", a.colorsEditor.colorList.get(0).getColorValue());
                    assertFalse(a.stringsEditor.hasUnsavedChanges);
                    assertFalse(a.colorsEditor.hasUnsavedChanges);
                });
                setFault(point -> { });
            } finally {
                assertTrue(blocked.delete() || !blocked.exists());
            }
        });
    }

    @Test
    public void failedRetryKeepsEarlierSavedXmlHashSet() throws Exception {
        withEditor((fixture, scenario) -> {
            File stringsXml = resourceFile(fixture.id, "strings.xml");
            File colorsXml = resourceFile(fixture.id, "colors.xml");
            File blockedStrings = new File(stringsXml.getParentFile(), "blocked-strings-" + System.nanoTime());
            File blockedColors = new File(colorsXml.getParentFile(), "blocked-colors-" + System.nanoTime());
            assertTrue(blockedStrings.mkdir());
            assertTrue(blockedColors.mkdir());
            try {
                String[] old = new String[1];
                on(scenario, a -> {
                    a.stringsEditor.addString("merge_a", "A", "");
                    a.colorsEditor.colorList.get(0).setColorValue("#112233");
                    a.colorsEditor.markDraftChanged();
                    call(a, "protectDrafts");
                    old[0] = session(a);
                });
                SystemClock.sleep(300);
                setFault(point -> {
                    if ("before_manifest".equals(point) || "before_close_manifest".equals(point)) {
                        throw new RuntimeException("injected draft/close failure");
                    }
                });
                // First attempt: strings fails, colors is saved as B.
                on(scenario, a -> {
                    a.stringsEditor.addString("merge_b", "B", "");
                    a.colorsEditor.colorList.get(0).setColorValue("#445566");
                    a.colorsEditor.markDraftChanged();
                    a.stringsEditor.filePath = blockedStrings.getPath();
                    ResourceEditorSaveResult failed =
                            (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                    assertTrue(failed.canSaveXmlOnly);
                    callWithNull(a, "performXmlOnlySave", Runnable.class);
                    assertTrue(unsafe(a));
                });
                assertTrue(read(colorsXml).contains("#445566"));
                assertFalse(read(stringsXml).contains("merge_b"));

                // Second attempt changes colors to C but fails to write it. The old colors B must survive.
                on(scenario, a -> {
                    a.stringsEditor.filePath = a.stringsFilePath;
                    a.colorsEditor.contentPath = blockedColors.getPath();
                    a.colorsEditor.colorList.get(0).setColorValue("#778899");
                    a.colorsEditor.markDraftChanged();
                    ResourceEditorSaveResult failed =
                            (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                    assertTrue(failed.canSaveXmlOnly);
                    callWithNull(a, "performXmlOnlySave", Runnable.class);
                    assertTrue(unsafe(a));
                });
                assertTrue(read(stringsXml).contains("merge_b"));
                assertTrue("colors XML must still be B", read(colorsXml).contains("#445566"));

                Application app = (Application) InstrumentationRegistry.getInstrumentation()
                        .getTargetContext().getApplicationContext();
                Application.ActivityLifecycleCallbacks stale = new EmptyCallbacks() {
                    @Override
                    public void onActivityPostSaveInstanceState(Activity activity, Bundle state) {
                        state.putString("resource_draft_session", old[0]);
                    }
                };
                app.registerActivityLifecycleCallbacks(stale);
                try {
                    scenario.recreate();
                } finally {
                    app.unregisterActivityLifecycleCallbacks(stale);
                }
                awaitLoaded(scenario);
                on(scenario, a -> {
                    assertEquals(old[0], session(a));
                    assertTrue(hasString(a, "merge_b"));
                    assertEquals("the earlier saved colors B must not fall back to A",
                            "#445566", a.colorsEditor.colorList.get(0).getColorValue());
                    assertFalse(a.stringsEditor.hasUnsavedChanges);
                    assertFalse(a.colorsEditor.hasUnsavedChanges);
                });
                setFault(point -> { });
            } finally {
                assertTrue(blockedStrings.delete() || !blockedStrings.exists());
                assertTrue(blockedColors.delete() || !blockedColors.exists());
            }
        });
    }

    @Test
    public void xmlRecoveryKeepsSavedBWhenAnotherChunkIsEdited() throws Exception {
        withLargeStringsEditor(1024, (fixture, scenario) -> {
            File xml = resourceFile(fixture.id, "strings.xml");
            String[] old = new String[1];
            on(scenario, a -> {
                row(a, 700).put("text", "A-700");
                notes(a).put(700, "old A note");
                a.stringsEditor.markStringChanged(700, false);
                assertTrue("draft A must be protected first", (Boolean) call(a, "protectDrafts"));
                old[0] = session(a);
            });
            // XML B is written, but neither the CLEAN commit nor the session close succeed.
            setFault(point -> {
                if ("before_manifest".equals(point) || "before_close_manifest".equals(point)) {
                    throw new RuntimeException("injected draft/close failure");
                }
            });
            on(scenario, a -> {
                row(a, 700).put("text", "saved B at 700");
                notes(a).remove(700);
                a.stringsEditor.markStringChanged(700, false);
                ResourceEditorSaveResult failed =
                        (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                assertTrue(failed.canSaveXmlOnly);
                callWithNull(a, "performXmlOnlySave", Runnable.class);
                assertTrue("the CLOSED manifest must stay unwritten", unsafe(a));
                assertFalse(a.stringsEditor.hasUnsavedChanges);
            });
            assertTrue(read(xml).contains("saved B at 700"));

            // Draft A is superseded by XML B; the CLEAN commit keeps failing.
            scenario.recreate();
            awaitStringsLoaded(scenario, 1024);
            on(scenario, a -> {
                assertEquals("the old session is the recovery reference", old[0], session(a));
                assertEquals("XML B must win over draft A", "saved B at 700", row(a, 700).get("text"));
                assertNull("XML B removed the old comment", notes(a).get(700));
                assertFalse(a.stringsEditor.hasUnsavedChanges);
                // Edit a different chunk before any complete B snapshot exists.
                row(a, 10).put("text", "point C at 10");
                a.stringsEditor.markStringChanged(10, false);
                assertFalse("CLEAN must still fail", (Boolean) call(a, "protectDrafts"));
            });
            setFault(point -> { });
            on(scenario, a -> assertTrue("a complete B+C snapshot must be protected",
                    (Boolean) call(a, "protectDrafts")));

            scenario.recreate();
            awaitStringsLoaded(scenario, 1024);
            on(scenario, a -> {
                assertEquals(1024, a.stringsEditor.listmap.size());
                assertEquals("saved B at 700", row(a, 700).get("text"));
                assertNull("the superseded A comment must not return", notes(a).get(700));
                assertEquals("point C at 10", row(a, 10).get("text"));
                assertEquals("value 11", row(a, 11).get("text"));
                assertTrue(a.stringsEditor.hasUnsavedChanges);
            });
        });
    }

    private static void on(ActivityScenario<ResourcesEditorActivity> scenario, Action action) {
        scenario.onActivity(activity -> {
            try {
                action.run(activity);
            } catch (Exception failure) {
                throw new AssertionError(failure);
            }
        });
    }

    private interface Action {
        void run(ResourcesEditorActivity activity) throws Exception;
    }

    private static View dialogWithTitle(String title) {
        for (View root : WindowInspector.getGlobalWindowViews()) {
            if (containsText(root, title)) return root;
        }
        return null;
    }

    private static boolean containsText(View view, String text) {
        if (view instanceof TextView label) return text.contentEquals(label.getText());
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsText(group.getChildAt(i), text)) return true;
            }
        }
        return false;
    }

    private static void clickDialog(ActivityScenario<ResourcesEditorActivity> scenario,
                                    int titleRes, int buttonId) {
        on(scenario, a -> {
            View dialog = dialogWithTitle(a.getString(titleRes));
            assertNotNull("dialog not found: " + a.getString(titleRes), dialog);
            View button = dialog.findViewById(buttonId);
            assertNotNull("dialog button not found", button);
            assertTrue(button.performClick());
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, String> notes(ResourcesEditorActivity activity) throws Exception {
        Field field = Class.forName("pro.sketchware.activities.resourceseditor.components.fragments.StringsEditor")
                .getDeclaredField("notesMap");
        field.setAccessible(true);
        return (Map<Integer, String>) field.get(activity.stringsEditor);
    }

    private static boolean manifestPageDirty(String session, int page) throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File file = new File(new File(context.getNoBackupFilesDir(), "resource-editor-drafts"),
                session + "/manifest.json");
        JsonObject json = JsonParser.parseString(new String(Files.readAllBytes(file.toPath()),
                StandardCharsets.UTF_8)).getAsJsonObject();
        return json.getAsJsonArray("pages").get(page).getAsJsonObject().get("dirty").getAsBoolean();
    }

    private static boolean unsafe(ResourcesEditorActivity activity) throws Exception {
        Field field = ResourcesEditorActivity.class.getDeclaredField("restoreReferenceUnsafe");
        field.setAccessible(true);
        return field.getBoolean(activity);
    }

    private static class EmptyCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override public void onActivityCreated(Activity activity, Bundle state) { }
        @Override public void onActivityStarted(Activity activity) { }
        @Override public void onActivityResumed(Activity activity) { }
        @Override public void onActivityPaused(Activity activity) { }
        @Override public void onActivityStopped(Activity activity) { }
        @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
        @Override public void onActivityDestroyed(Activity activity) { }
    }

    private void withEditor(Check check) throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture()) {
            fixture.seed(false, false);
            seedResources(fixture.id);
            ProjectDataManager.reloadProjectFromData(fixture.id);
            Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                    ResourcesEditorActivity.class).putExtra("sc_id", fixture.id);
            try (ActivityScenario<ResourcesEditorActivity> scenario = ActivityScenario.launch(intent)) {
                awaitLoaded(scenario);
                check.run(fixture, scenario);
            }
        }
    }

    private void withLargeStringsEditor(int rows, Check check) throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture()) {
            fixture.seed(false, false);
            seedLargeResources(fixture.id, rows);
            ProjectDataManager.reloadProjectFromData(fixture.id);
            Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                    ResourcesEditorActivity.class).putExtra("sc_id", fixture.id);
            try (ActivityScenario<ResourcesEditorActivity> scenario = ActivityScenario.launch(intent)) {
                awaitStringsLoaded(scenario, rows);
                check.run(fixture, scenario);
            }
        }
    }

    private static void seedLargeResources(String id, int rows) throws Exception {
        StringBuilder strings = new StringBuilder("<resources>\n");
        strings.append("    <string name=\"app_name\">SaveTest</string>\n");
        for (int i = 1; i < rows; i++) {
            strings.append("    <string name=\"row_").append(i).append("\">value ")
                    .append(i).append("</string>\n");
        }
        strings.append("</resources>");
        String[] names = {"strings.xml", "colors.xml", "styles.xml", "themes.xml", "arrays.xml", "dimens.xml"};
        String[] content = {
                strings.toString(),
                "<resources><color name=\"probe_color\">#336699</color></resources>",
                "<resources><style name=\"ProbeStyle\"><item name=\"android:textColor\">#336699</item></style></resources>",
                "<resources><style name=\"ProbeTheme\" parent=\"Theme.AppCompat.Light.NoActionBar\"><item name=\"colorPrimary\">#336699</item></style></resources>",
                "<resources><string-array name=\"probe_array\"><item>one</item></string-array></resources>",
                "<resources><dimen name=\"probe_size\">12dp</dimen></resources>"
        };
        for (int i = 0; i < names.length; i++) {
            File file = resourceFile(id, names[i]);
            assertTrue(file.getParentFile().mkdirs() || file.getParentFile().isDirectory());
            Files.write(file.toPath(), content[i].getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void awaitStringsLoaded(ActivityScenario<ResourcesEditorActivity> scenario, int rows)
            throws Exception {
        Field loaded = ResourcesEditorActivity.class.getDeclaredField("editorsLoaded");
        loaded.setAccessible(true);
        long deadline = SystemClock.uptimeMillis() + 20000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] ready = {false};
            on(scenario, a -> {
                try {
                    ready[0] = loaded.getBoolean(a) && a.stringsEditor.listmap.size() == rows;
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            });
            if (ready[0]) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                return;
            }
            SystemClock.sleep(20);
        }
        fail("Strings editors did not load " + rows + " rows");
    }

    private static Map<String, Object> row(ResourcesEditorActivity activity, int index) {
        return activity.stringsEditor.listmap.get(index);
    }

    private static void seedResources(String id) throws Exception {
        String[] names = {"strings.xml", "colors.xml", "styles.xml", "themes.xml", "arrays.xml", "dimens.xml"};
        String[] content = {
                "<resources><string name=\"app_name\">SaveTest</string><string name=\"probe\">base</string></resources>",
                "<resources><color name=\"probe_color\">#336699</color></resources>",
                "<resources><style name=\"ProbeStyle\"><item name=\"android:textColor\">#336699</item></style></resources>",
                "<resources><style name=\"ProbeTheme\" parent=\"Theme.AppCompat.Light.NoActionBar\"><item name=\"colorPrimary\">#336699</item></style></resources>",
                "<resources><string-array name=\"probe_array\"><item>one</item></string-array></resources>",
                "<resources><dimen name=\"probe_size\">12dp</dimen></resources>"
        };
        for (int i = 0; i < names.length; i++) {
            File file = resourceFile(id, names[i]);
            assertTrue(file.getParentFile().mkdirs() || file.getParentFile().isDirectory());
            Files.write(file.toPath(), content[i].getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void awaitLoaded(ActivityScenario<ResourcesEditorActivity> scenario) throws Exception {
        Field loaded = ResourcesEditorActivity.class.getDeclaredField("editorsLoaded");
        loaded.setAccessible(true);
        long deadline = SystemClock.uptimeMillis() + 15000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] ready = {false};
            on(scenario, a -> {
                try {
                    ready[0] = loaded.getBoolean(a) && a.stringsEditor.listmap.stream()
                            .anyMatch(row -> "probe".equals(row.get("key")) && "base".equals(row.get("text")));
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            });
            if (ready[0]) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                return;
            }
            SystemClock.sleep(20);
        }
        fail("Editors did not load");
    }

    private static File resourceFile(String id, String name) {
        return new File(SketchwarePaths.getProjectResourceValuesFilePath(id, name));
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static boolean hasString(ResourcesEditorActivity activity, String key) {
        return activity.stringsEditor.listmap.stream().anyMatch(row -> key.equals(row.get("key")));
    }

    private static Object call(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Object callWithNull(Object target, String name, Class<?> parameter) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameter);
        method.setAccessible(true);
        return method.invoke(target, new Object[]{null});
    }

    private static Object callWith(Object target, String name, Class<?> parameter, Object value) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameter);
        method.setAccessible(true);
        return method.invoke(target, value);
    }

    private static int resultCode(Activity activity) throws Exception {
        Field field = Activity.class.getDeclaredField("mResultCode");
        field.setAccessible(true);
        return field.getInt(activity);
    }

    private static String session(ResourcesEditorActivity activity) throws Exception {
        Field controller = ResourcesEditorActivity.class.getDeclaredField("draftController");
        controller.setAccessible(true);
        Object value = controller.get(activity);
        assertNotNull("draft controller must exist", value);
        Field session = value.getClass().getDeclaredField("session");
        session.setAccessible(true);
        return (String) session.get(value);
    }

    private static boolean manifestClosed(String session) throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File file = new File(new File(context.getNoBackupFilesDir(), "resource-editor-drafts"),
                session + "/manifest.json");
        JsonObject json = JsonParser.parseString(new String(Files.readAllBytes(file.toPath()),
                StandardCharsets.UTF_8)).getAsJsonObject();
        return json.has("closed") && json.get("closed").getAsBoolean();
    }

    private static void setFault(Consumer<String> fault) throws Exception {
        Field field = Class.forName(STORE_CLASS).getDeclaredField("testFault");
        field.setAccessible(true);
        field.set(null, fault);
    }

    private interface Check {
        void run(ProjectEditorPersistenceTest.Fixture fixture,
                 ActivityScenario<ResourcesEditorActivity> scenario) throws Exception;
    }
}
