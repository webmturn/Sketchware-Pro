package pro.sketchware.core;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import kellinwood.security.zipsigner.ZipSigner;
import kellinwood.security.zipsigner.optional.JksKeyStore;
import kellinwood.security.zipsigner.optional.LoadKeystoreException;
import org.spongycastle.asn1.x509.X509Name;
import org.spongycastle.jce.X509Principal;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.x509.X509V3CertificateGenerator;

public class KeyStoreManager {
  public KeyStore keyStore;
  
  public ByteBuffer keyBuffer;
  
  public KeyStoreManager() {
    Security.addProvider((Provider)new BouncyCastleProvider());
    this.keyStore = (KeyStore)new JksKeyStore();
  }
  
  public String getFirstAlias() throws Exception {
    Enumeration<String> enumeration = this.keyStore.aliases();
    return enumeration.hasMoreElements() ? enumeration.nextElement() : "";
  }
  
  public void loadKeyStore(InputStream inputStream, String str) throws Exception {
    if (inputStream == null)
      return; 
    try {
      this.keyStore.load(inputStream, str.toCharArray());
    } catch (Exception exception) {
      exception.printStackTrace();
      throw new Exception(exception.getMessage());
    } finally {
      try {
        inputStream.close();
      } catch (Exception exception) {}
    }
  }
  
  public void loadKeyStoreFromFile(String key, String value) throws Exception {
    loadKeyStore(new FileInputStream(new File(key)), value);
  }
  
  public void generateAndSaveKeyStore(String key, String value, int index, String extra, String tag) throws Exception {
    byte[] bytes = generateKeyPair(value, index, extra, tag);
    File file = new File(SketchwarePaths.getKeystoreDirPath());
    if (!file.exists())
      file.mkdirs(); 
    (new EncryptedFileUtil()).writeBytes(key, bytes);
  }
  
  public final byte[] exportKeyStore(String str) throws Exception {
    if (this.keyStore == null)
      return null; 
    this.keyBuffer = ByteBuffer.allocate(8192);
    KeyStoreOutputStream hI = new KeyStoreOutputStream(this);
    this.keyStore.store(hI, str.toCharArray());
    byte[] bytes = new byte[this.keyBuffer.position()];
    System.arraycopy(this.keyBuffer.array(), 0, bytes, 0, this.keyBuffer.position());
    int i = bytes.length;
    str = "";
    for (int b = 0; b < i; b++) {
      byte b1 = bytes[b];
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(str);
      stringBuilder.append(String.format("%02X", new Object[] { Byte.valueOf(b1) }));
      str = stringBuilder.toString();
    } 
    return bytes;
  }
  
  public byte[] generateKeyPair(String key, int index, String value, String extra) throws Exception {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(1024, SecureRandom.getInstance("SHA1PRNG"));
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      X509V3CertificateGenerator x509V3CertificateGenerator = new X509V3CertificateGenerator();
      X509Principal x509Principal = new X509Principal(key);
      SecureRandom secureRandom = new SecureRandom();
      int i = secureRandom.nextInt();
      int j = i;
      if (i < 0)
        j = i * -1; 
      x509V3CertificateGenerator.setSerialNumber(BigInteger.valueOf(j));
      x509V3CertificateGenerator.setIssuerDN((X509Name)x509Principal);
      Date date = new Date(System.currentTimeMillis());
      x509V3CertificateGenerator.setNotBefore(date);
      date = new Date(System.currentTimeMillis() + index * 31536000000L);
      x509V3CertificateGenerator.setNotAfter(date);
      x509V3CertificateGenerator.setSubjectDN((X509Name)x509Principal);
      x509V3CertificateGenerator.setPublicKey(keyPair.getPublic());
      x509V3CertificateGenerator.setSignatureAlgorithm("MD5WithRSAEncryption");
      X509Certificate x509Certificate = x509V3CertificateGenerator.generateX509Certificate(keyPair.getPrivate());
      JksKeyStore jksKeyStore = new JksKeyStore();
      this.keyStore = (KeyStore)jksKeyStore;
      this.keyStore.load(null, extra.toCharArray());
      this.keyStore.setKeyEntry(value, keyPair.getPrivate(), extra.toCharArray(), new Certificate[] { x509Certificate });
      return exportKeyStore(extra);
    } catch (LoadKeystoreException loadKeystoreException) {
      Log.e("ERROR", "Failed to access keystore. incorrect passward");
      throw loadKeystoreException;
    } catch (Exception exception) {
      throw exception;
    } 
  }
  
  public ZipSigner createZipSigner(String input) throws Exception {
    ZipSigner zipSigner = new ZipSigner();
    zipSigner.issueLoadingCertAndKeysProgressEvent();
    String str = getFirstAlias();
    zipSigner.setKeys("custom", (X509Certificate)this.keyStore.getCertificate(str), (PrivateKey)this.keyStore.getKey(str, str.toCharArray()), "SHA1WITHRSA", null);
    return zipSigner;
  }
}
