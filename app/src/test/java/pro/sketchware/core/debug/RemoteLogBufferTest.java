package pro.sketchware.core.debug;

import static org.junit.Assert.*;

import org.junit.Test;

public class RemoteLogBufferTest {
    @Test public void replayThenReadOnlyNewLinesWithoutDuplicates() {
        RemoteLogBuffer buffer = new RemoteLogBuffer();
        buffer.append("com.example.app", "before opening viewer");
        var first = buffer.snapshot(0, -1);
        assertEquals(1, first.entries().size());
        assertTrue(buffer.snapshot(first.lastSequence(), first.generation()).entries().isEmpty());
        buffer.append("com.example.app", "new line");
        var next = buffer.snapshot(first.lastSequence(), first.generation());
        assertEquals(1, next.entries().size());
        assertEquals("new line", next.entries().get(0).line());
        assertEquals("com.example.app", next.entries().get(0).packageName());
    }

    @Test public void clearingNotifiesAnExistingViewerEvenWithoutNewLines() {
        RemoteLogBuffer buffer = new RemoteLogBuffer();
        buffer.append("com.example.app", "old line");
        var old = buffer.snapshot(0, -1);
        buffer.clear();
        var cleared = buffer.snapshot(old.lastSequence(), old.generation());
        assertNotEquals(old.generation(), cleared.generation());
        assertTrue(cleared.entries().isEmpty());
        buffer.append("com.example.other", "after clearing");
        var next = buffer.snapshot(old.lastSequence(), old.generation());
        assertEquals(1, next.entries().size());
        assertEquals("after clearing", next.entries().get(0).line());
    }

    @Test public void boundsMemoryWhileAnUnopenedViewerFallsBehind() {
        RemoteLogBuffer buffer = new RemoteLogBuffer();
        for (int index = 0; index < 600; index++) buffer.append("com.example.app", String.valueOf(index));
        var snapshot = buffer.snapshot(0, -1);
        assertEquals(500, snapshot.entries().size());
        assertEquals("100", snapshot.entries().get(0).line());
        buffer.append("com.example.app", "x".repeat(3000));
        assertEquals(2000, buffer.snapshot(snapshot.lastSequence(), snapshot.generation()).entries().get(0).line().length());
        assertEquals(500, buffer.snapshot(0, -1).entries().size());
    }
}
