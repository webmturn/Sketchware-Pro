package pro.sketchware.util.apk;

import java.io.IOException;
import java.security.GeneralSecurityException;


public class TestkeySignBridge {
    private TestkeySignBridge() {
    }

    public static void signWithTestkey(String inputPath, String outputPath) throws GeneralSecurityException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        ApkSigner signer = new ApkSigner();
        signer.signWithTestKey(inputPath, outputPath, null);
    }
}
