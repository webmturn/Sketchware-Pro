package pro.sketchware.util.format;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.util.Arrays;

public class FormatUtilTest {
    @Test public void preservesEmptyOddLengthLeadingZerosAndCase() {
        assertArrayEquals(new byte[0], FormatUtil.hexStringToBytes(""));
        assertArrayEquals(new byte[] {0}, FormatUtil.hexStringToBytes("0"));
        assertArrayEquals(new byte[] {15}, FormatUtil.hexStringToBytes("f"));
        assertArrayEquals(new byte[] {10, (byte) 0xbc}, FormatUtil.hexStringToBytes("AbC"));
        assertArrayEquals(new byte[] {0, 0, 1}, FormatUtil.hexStringToBytes("00001"));
        assertArrayEquals(new byte[] {0, 1, (byte) 0xab, (byte) 0xcd, (byte) 0xef},
                FormatUtil.hexStringToBytes("0001aBcDeF"));
    }

    @Test public void decodesOddAndEvenLengthsAcrossSignedByteIndexBoundary() {
        for (int byteCount : new int[] {0, 1, 2, 126, 127, 128, 129, 130, 255, 256, 257}) {
            assertKnownBytes(byteCount);
        }
    }

    @Test public void decodesLongOddAndEvenInputs() {
        assertKnownBytes(2048);
        assertKnownBytes(32768);
    }

    @Test public void decodesEveryByteValue() {
        byte[] expected = new byte[256];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) i;
        }
        assertArrayEquals(expected, FormatUtil.hexStringToBytes(encodeKnownBytes(expected)));
    }

    @Test public void rejectsInvalidCharactersAtShortAndLongOffsets() {
        for (char invalid : new char[] {'g', 'G', ' ', '\t', '\n', '+', '-', 'x', '\0', '\uFF10'}) {
            for (int prefixLength : new int[] {0, 1, 254, 255, 256, 257, 65536}) {
                char[] prefixChars = new char[prefixLength];
                Arrays.fill(prefixChars, 'a');
                String prefix = new String(prefixChars);
                IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                        () -> FormatUtil.hexStringToBytes(prefix + invalid + "0"));
                assertEquals("invalid hex digit '" + invalid + "'", error.getMessage());
            }
        }
        assertThrows(IllegalArgumentException.class, () -> FormatUtil.hexStringToBytes("0x12"));
    }

    @Test public void rejectsNullInput() {
        assertThrows(NullPointerException.class, () -> FormatUtil.hexStringToBytes(null));
    }

    private static void assertKnownBytes(int byteCount) {
        byte[] expected = new byte[byteCount];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) (i * 37 + 19);
        }
        String even = encodeKnownBytes(expected);
        assertArrayEquals("even characters=" + even.length(), expected, FormatUtil.hexStringToBytes(even));

        byte[] oddExpected = new byte[byteCount + 1];
        oddExpected[0] = 7;
        System.arraycopy(expected, 0, oddExpected, 1, expected.length);
        assertArrayEquals("odd characters=" + (even.length() + 1), oddExpected,
                FormatUtil.hexStringToBytes("7" + even));
    }

    private static String encodeKnownBytes(byte[] values) {
        StringBuilder input = new StringBuilder(values.length * 2);
        for (int i = 0; i < values.length; i++) {
            String digits = (i % 2 == 0) ? "0123456789ABCDEF" : "0123456789abcdef";
            input.append(digits.charAt((values[i] & 0xff) >>> 4));
            input.append(digits.charAt(values[i] & 0xf));
        }
        return input.toString();
    }
}
