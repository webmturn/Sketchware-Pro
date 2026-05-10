package pro.sketchware.core.build;

import pro.sketchware.core.util.EncryptedFileUtil;
import pro.sketchware.core.project.SketchwarePaths;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
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
    keyStore = (KeyStore)new JksKeyStore();
  }
  
  public String getFirstAlias() throws KeyStoreException {
    Enumeration<String> enumeration = keyStore.aliases();
    return enumeration.hasMoreElements() ? enumeration.nextElement() : "";
  }
  
  public void loadKeyStore(InputStream inputStream, String password) throws IOException, GeneralSecurityException {
    if (inputStream == null)
      return; 
    try (InputStream closeableInputStream = inputStream) {
      keyStore.load(closeableInputStream, password.toCharArray());
    } catch (IOException | GeneralSecurityException exception) {
      Log.w("KeyStoreManager", "Failed to load keystore", exception);
      throw exception;
    }
  }
  
  public void loadKeyStoreFromFile(String keyStorePath, String password) throws IOException, GeneralSecurityException {
    loadKeyStore(new FileInputStream(new File(keyStorePath)), password);
  }
  
  public void generateAndSaveKeyStore(String keyStorePath, String distinguishedName, int validityYears, String alias, String password) throws IOException, GeneralSecurityException {
    byte[] keyStoreBytes = generateKeyPair(distinguishedName, validityYears, alias, password);
    File keystoreDir = new File(SketchwarePaths.getKeystoreDirPath());
    if (!keystoreDir.exists() && !keystoreDir.mkdirs())
      throw new IOException("Failed to create keystore directory: " + keystoreDir.getAbsolutePath());
    boolean saved = (new EncryptedFileUtil()).writeBytes(keyStorePath, keyStoreBytes);
    if (!saved)
      throw new IOException("Failed to write keystore: " + keyStorePath);
  }
  
  public final byte[] exportKeyStore(String password) throws IOException, GeneralSecurityException {
    if (keyStore == null)
      return null; 
    keyBuffer = ByteBuffer.allocate(8192);
    KeyStoreOutputStream keyStoreOutputStream = new KeyStoreOutputStream(this);
    keyStore.store(keyStoreOutputStream, password.toCharArray());
    byte[] keyStoreBytes = new byte[keyBuffer.position()];
    System.arraycopy(keyBuffer.array(), 0, keyStoreBytes, 0, keyBuffer.position());
    int keyStoreLength = keyStoreBytes.length;
    String hexString = "";
    for (int byteIdx = 0; byteIdx < keyStoreLength; byteIdx++) {
      byte currentByte = keyStoreBytes[byteIdx];
      hexString = hexString + String.format("%02X", new Object[] { Byte.valueOf(currentByte) });
    } 
    return keyStoreBytes;
  }
  
  public byte[] generateKeyPair(String distinguishedName, int validityYears, String alias, String password) throws IOException, GeneralSecurityException {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(1024, SecureRandom.getInstance("SHA1PRNG"));
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      X509V3CertificateGenerator x509V3CertificateGenerator = new X509V3CertificateGenerator();
      X509Principal x509Principal = new X509Principal(distinguishedName);
      SecureRandom secureRandom = new SecureRandom();
      int randomInt = secureRandom.nextInt();
      int serialNumber = randomInt;
      if (randomInt < 0)
        serialNumber = randomInt * -1; 
      x509V3CertificateGenerator.setSerialNumber(BigInteger.valueOf(serialNumber));
      x509V3CertificateGenerator.setIssuerDN((X509Name)x509Principal);
      Date date = new Date(System.currentTimeMillis());
      x509V3CertificateGenerator.setNotBefore(date);
      date = new Date(System.currentTimeMillis() + validityYears * 31536000000L);
      x509V3CertificateGenerator.setNotAfter(date);
      x509V3CertificateGenerator.setSubjectDN((X509Name)x509Principal);
      x509V3CertificateGenerator.setPublicKey(keyPair.getPublic());
      x509V3CertificateGenerator.setSignatureAlgorithm("MD5WithRSAEncryption");
      X509Certificate x509Certificate = x509V3CertificateGenerator.generateX509Certificate(keyPair.getPrivate());
      JksKeyStore jksKeyStore = new JksKeyStore();
      keyStore = (KeyStore)jksKeyStore;
      keyStore.load(null, password.toCharArray());
      keyStore.setKeyEntry(alias, keyPair.getPrivate(), password.toCharArray(), new Certificate[] { x509Certificate });
      return exportKeyStore(password);
    } catch (LoadKeystoreException loadKeystoreException) {
      Log.e("KeyStoreManager", "Failed to access keystore", loadKeystoreException);
      throw loadKeystoreException;
    } 
  }
  
  public ZipSigner createZipSigner(String unused) throws GeneralSecurityException, ClassNotFoundException, IllegalAccessException, InstantiationException {
    ZipSigner zipSigner = new ZipSigner();
    zipSigner.issueLoadingCertAndKeysProgressEvent();
    String alias = getFirstAlias();
    zipSigner.setKeys("custom", (X509Certificate)keyStore.getCertificate(alias), (PrivateKey)keyStore.getKey(alias, alias.toCharArray()), "SHA1WITHRSA", null);
    return zipSigner;
  }
}
