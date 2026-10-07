package pro.sketchware.util.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class EncryptedFileUtilTest {
    @Test public void appliesThePolicyOfEachWriteOnTheSameUtility() throws Exception {
        EncryptedFileUtil fileUtil = new EncryptedFileUtil();
        String content = "{\"name\":\"测试项目\"}";
        byte[] plain = content.getBytes(StandardCharsets.UTF_8);

        assertArrayEquals(plain, fileUtil.encryptString(content, false));
        byte[] encrypted = fileUtil.encryptString(content, true);
        assertFalse(Arrays.equals(plain, encrypted));
        assertEquals(content, fileUtil.decryptToString(encrypted));
        assertArrayEquals(plain, fileUtil.encryptString(content, false));
        assertEquals(content, fileUtil.decryptToString(plain));
    }

    @Test public void legacyConstructorFlagDoesNotOverrideTheExplicitPolicy() throws Exception {
        EncryptedFileUtil fileUtil = new EncryptedFileUtil(true);
        assertArrayEquals("plain".getBytes(StandardCharsets.UTF_8), fileUtil.encryptString("plain", false));
        assertEquals("encrypted", fileUtil.decryptToString(fileUtil.encryptString("encrypted", true)));
    }
}
