package pro.sketchware.util.apk;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ApkSignerOutputTest {
    @Test public void forwardsUnicodeLinesAndFinalFragmentWithoutClosingPreviousOutput() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream previousBytes = new ByteArrayOutputStream();
        try (PrintStream previous = new PrintStream(previousBytes, false, "UTF-8")) {
            System.setOut(previous);
            List<String> lines = new ArrayList<>();
            try (ApkSigner.LogWriter logger = new ApkSigner.LogWriter(lines::add)) {
                ApkSigner.runWithOutput(logger, () -> {
                    assertFalse(System.out.checkError());
                    System.out.print("工具输出：中文🙂\n最后一段");
                    assertFalse(System.out.checkError());
                });
                assertSame(previous, System.out);
                System.out.println("still open");
            }
            assertEquals(List.of("工具输出：中文🙂\n", "最后一段"), lines);
            assertEquals("still open" + System.lineSeparator(), previousBytes.toString("UTF-8"));
        } finally {
            System.setOut(original);
        }
    }

    @Test public void decodesSignedBytesAndSplitUnicodeOnlyAfterLineIsComplete() throws Exception {
        List<String> lines = new ArrayList<>();
        try (ApkSigner.LogWriter logger = new ApkSigner.LogWriter(lines::add)) {
            for (byte value : "中文🙂\n末尾".getBytes(StandardCharsets.UTF_8)) {
                logger.write(value);
            }
            assertEquals(List.of("中文🙂\n"), lines);
        }
        assertEquals(List.of("中文🙂\n", "末尾"), lines);
    }

    @Test public void restoresOutputWhenSigningThrowsAnException() throws Exception {
        assertRestoredAfterFailure(new IllegalStateException("signing failure"));
    }

    @Test public void restoresOutputWhenSigningThrowsAnError() throws Exception {
        assertRestoredAfterFailure(new AssertionError("signing error"));
    }

    private void assertRestoredAfterFailure(Throwable expected) throws Exception {
        PrintStream original = System.out;
        List<String> lines = new ArrayList<>();
        try (ApkSigner.LogWriter logger = new ApkSigner.LogWriter(lines::add)) {
            try {
                ApkSigner.runWithOutput(logger, () -> {
                    System.out.print("before failure");
                    if (expected instanceof Error) throw (Error) expected;
                    throw (RuntimeException) expected;
                });
                fail("Expected signing failure");
            } catch (RuntimeException | Error actual) {
                assertSame(expected, actual);
            }
            assertSame(original, System.out);
        } finally {
            System.setOut(original);
        }
        assertEquals(List.of("before failure"), lines);
    }

    @Test public void restoresOutputWhenCallbackThrowsWhileWriting() throws Exception {
        assertCallbackFailureRestoresOutput("line\n");
    }

    @Test public void restoresOutputWhenCallbackThrowsWhileClosing() throws Exception {
        assertCallbackFailureRestoresOutput("final fragment");
    }

    private void assertCallbackFailureRestoresOutput(String text) throws Exception {
        PrintStream original = System.out;
        IllegalStateException expected = new IllegalStateException("callback failure");
        List<String> delivered = new ArrayList<>();
        try (ApkSigner.LogWriter logger = new ApkSigner.LogWriter(line -> {
            delivered.add(line);
            throw expected;
        })) {
            try {
                ApkSigner.runWithOutput(logger, () -> System.out.print(text));
                fail("Expected callback failure");
            } catch (IllegalStateException actual) {
                assertSame(expected, actual);
            }
            assertSame(original, System.out);
        } finally {
            System.setOut(original);
        }
        assertEquals(List.of(text), delivered);
    }

    @Test public void signingWithoutCallbackKeepsExistingOutput() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream previous = new PrintStream(bytes, false, "UTF-8");
             ApkSigner.LogWriter logger = new ApkSigner.LogWriter(null)) {
            System.setOut(previous);
            ApkSigner.runWithOutput(logger, () -> {
                assertSame(previous, System.out);
                System.out.print("without callback");
            });
            assertSame(previous, System.out);
            System.out.print(" and still open");
            assertEquals("without callback and still open", bytes.toString("UTF-8"));
        } finally {
            System.setOut(original);
        }
    }

    @Test public void overlappingSigningIncludingNoCallbackCannotStealOutput() throws Exception {
        PrintStream original = System.out;
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        List<String> lines = new ArrayList<>();
        try (ApkSigner.LogWriter first = new ApkSigner.LogWriter(lines::add);
             ApkSigner.LogWriter second = new ApkSigner.LogWriter(null)) {
            Future<?> firstTask = workers.submit(() -> {
                ApkSigner.runWithOutput(first, () -> {
                    firstEntered.countDown();
                    await(releaseFirst);
                    System.out.print("first signer\n");
                });
                return null;
            });
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS));
            Future<?> secondTask = workers.submit(() -> {
                secondStarted.countDown();
                ApkSigner.runWithOutput(second, () -> {
                    secondEntered.countDown();
                    assertSame(original, System.out);
                });
                return null;
            });
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
            assertFalse(secondEntered.await(200, TimeUnit.MILLISECONDS));
            releaseFirst.countDown();
            firstTask.get(5, TimeUnit.SECONDS);
            secondTask.get(5, TimeUnit.SECONDS);
            assertEquals(List.of("first signer\n"), lines);
            assertSame(original, System.out);
        } finally {
            releaseFirst.countDown();
            workers.shutdownNow();
            try {
                assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
            } finally {
                System.setOut(original);
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for signer");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }
}
