package pro.sketchware.core.project;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

import com.google.gson.JsonSyntaxException;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.function.Consumer;

/** Protects the existing fingerprint format, independently of project loading on Android. */
public class ProjectContentSnapshotTest {
    @Test public void preservesNumericSpellingAndStringEscapes() throws Exception {
        String[][] cases = {
                {"{\"z\":null,\"a\":true,\"b\":false}", "{\"a\":true,\"b\":false,\"z\":null}"},
                {"{\"z\":1.00,\"a\":1e+2,\"b\":-0,\"c\":-0.0,\"d\":1e-300}",
                        "{\"a\":1e+2,\"b\":-0,\"c\":-0.0,\"d\":1e-300,\"z\":1.00}"},
                {"{\"z\":922337203685477580812345,\"a\":0.00000001,\"b\":1E308}",
                        "{\"a\":0.00000001,\"b\":1E308,\"z\":922337203685477580812345}"},
                {"{\"z\":\"<html>&/中文\",\"a\":\"quote\\\"backslash\\\\tab\\tline\\n\"}",
                        "{\"a\":\"quote\\\"backslash\\\\tab\\tline\\n\",\"z\":\"<html>&/中文\"}"},
                {"{\"z\":\"\\u2028\\u2029\",\"a\":\"\\u0000\\u0001\"}",
                        "{\"a\":\"\\u0000\\u0001\",\"z\":\"\\u2028\\u2029\"}"},
                {"{\"z\":NaN,\"a\":Infinity,\"b\":-Infinity}",
                        "{\"a\":\"Infinity\",\"b\":\"-Infinity\",\"z\":\"NaN\"}"}
        };
        for (String[] value : cases) {
            assertArrayEquals(value[0], framedDigest("\n@main\n" + value[1] + "\n"),
                    fingerprint("@main\n" + value[0] + "\n"));
        }
    }

    @Test public void objectAndSectionOrderAreIgnoredButNestedValuesArePreserved() throws Exception {
        String first = "@z\n{\"z\":[3,2,1,[],{}],\"a\":{\"BB\":1,\"Aa\":2}}\n\n@a\n2:name\n@empty\n";
        String reordered = "@empty\n@a\n2:name\n@z\n{\"a\":{\"Aa\":2,\"BB\":1},\"z\":[3,2,1,[],{}]}\n";
        byte[] expected = framedDigest("\n@a\n2:name\n@empty\n@z\n"
                + "{\"a\":{\"Aa\":2,\"BB\":1},\"z\":[3,2,1,[],{}]}\n");
        assertArrayEquals(expected, fingerprint(first));
        assertArrayEquals(expected, fingerprint(reordered));
    }

    @Test public void listOrderAndScalarTypesRemainMeaningful() throws Exception {
        assertDifferent("{\"x\":[1,2]}", "{\"x\":[2,1]}");
        assertDifferent("{\"x\":1}", "{\"x\":\"1\"}");
        assertDifferent("{\"x\":true}", "{\"x\":\"true\"}");
        assertDifferent("{\"x\":null}", "{\"x\":\"null\"}");
        assertDifferent("{\"x\":{\"n\":1}}", "{\"x\":{\"n\":2}}");
    }

    @Test public void manyDistinctObjectShapesKeepCanonicalOrdering() throws Exception {
        StringBuilder input = new StringBuilder("@main\n");
        StringBuilder expected = new StringBuilder("\n@main\n");
        for (int i = 0; i < 300; i++) {
            input.append("{\"z\":1,\"k").append(i).append("\":2,\"a\":3}\n");
            expected.append("{\"a\":3,\"k").append(i).append("\":2,\"z\":1}\n");
        }
        assertArrayEquals(framedDigest(expected.toString()), fingerprint(input.toString()));
    }

    @Test(expected = JsonSyntaxException.class) public void malformedJsonDoesNotProduceAUsableFingerprint() throws Exception {
        fingerprint("@main\n{\"x\":]}\n");
    }

    private static void assertDifferent(String first, String second) throws Exception {
        assertFalse(Arrays.equals(fingerprint("@main\n" + first + "\n"),
                fingerprint("@main\n" + second + "\n")));
    }

    private static byte[] fingerprint(String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        Method append = ProjectContentSnapshot.class.getDeclaredMethod("append", MessageDigest.class, Consumer.class);
        append.setAccessible(true);
        try {
            append.invoke(null, digest, (Consumer<StringBuilder>) buffer -> buffer.append(content));
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            throw error;
        }
        return digest.digest();
    }

    private static byte[] framedDigest(String normalized) throws Exception {
        byte[] bytes = normalized.getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        return digest.digest(bytes);
    }
}
