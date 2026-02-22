package a.a.a;

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

public class iI {
  public KeyStore a;
  
  public ByteBuffer b;
  
  public iI() {
    Security.addProvider((Provider)new BouncyCastleProvider());
    this.a = (KeyStore)new JksKeyStore();
  }
  
  public String a() {
    Enumeration<String> enumeration = this.a.aliases();
    return enumeration.hasMoreElements() ? enumeration.nextElement() : "";
  }
  
  public void a(InputStream paramInputStream, String paramString) throws Exception {
    if (paramInputStream == null)
      return; 
    try {
      this.a.load(paramInputStream, paramString.toCharArray());
    } catch (Exception exception) {
      exception.printStackTrace();
      throw new Exception(exception.getMessage());
    } finally {
      try {
        paramInputStream.close();
      } catch (Exception exception) {}
    }
  }
  
  public void a(String paramString1, String paramString2) {
    a(new FileInputStream(new File(paramString1)), paramString2);
  }
  
  public void a(String paramString1, String paramString2, int paramInt, String paramString3, String paramString4) {
    byte[] arrayOfByte = a(paramString2, paramInt, paramString3, paramString4);
    File file = new File(wq.i());
    if (!file.exists())
      file.mkdirs(); 
    (new oB()).a(paramString1, arrayOfByte);
  }
  
  public final byte[] a(String paramString) {
    if (this.a == null)
      return null; 
    this.b = ByteBuffer.allocate(8192);
    hI hI = new hI(this);
    this.a.store(hI, paramString.toCharArray());
    byte[] arrayOfByte = new byte[this.b.position()];
    System.arraycopy(this.b.array(), 0, arrayOfByte, 0, this.b.position());
    int i = arrayOfByte.length;
    paramString = "";
    for (byte b = 0; b < i; b++) {
      byte b1 = arrayOfByte[b];
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(paramString);
      stringBuilder.append(String.format("%02X", new Object[] { Byte.valueOf(b1) }));
      paramString = stringBuilder.toString();
    } 
    return arrayOfByte;
  }
  
  public byte[] a(String paramString1, int paramInt, String paramString2, String paramString3) {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(1024, SecureRandom.getInstance("SHA1PRNG"));
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      X509V3CertificateGenerator x509V3CertificateGenerator = new X509V3CertificateGenerator();
      X509Principal x509Principal = new X509Principal(paramString1);
      SecureRandom secureRandom = new SecureRandom();
      int i = secureRandom.nextInt();
      int j = i;
      if (i < 0)
        j = i * -1; 
      x509V3CertificateGenerator.setSerialNumber(BigInteger.valueOf(j));
      x509V3CertificateGenerator.setIssuerDN((X509Name)x509Principal);
      Date date = new Date(System.currentTimeMillis());
      x509V3CertificateGenerator.setNotBefore(date);
      date = new Date(System.currentTimeMillis() + paramInt * 31536000000L);
      x509V3CertificateGenerator.setNotAfter(date);
      x509V3CertificateGenerator.setSubjectDN((X509Name)x509Principal);
      x509V3CertificateGenerator.setPublicKey(keyPair.getPublic());
      x509V3CertificateGenerator.setSignatureAlgorithm("MD5WithRSAEncryption");
      X509Certificate x509Certificate = x509V3CertificateGenerator.generateX509Certificate(keyPair.getPrivate());
      JksKeyStore jksKeyStore = new JksKeyStore();
      this.a = (KeyStore)jksKeyStore;
      this.a.load(null, paramString3.toCharArray());
      this.a.setKeyEntry(paramString2, keyPair.getPrivate(), paramString3.toCharArray(), new Certificate[] { x509Certificate });
      return a(paramString3);
    } catch (LoadKeystoreException loadKeystoreException) {
      Log.e("ERROR", "Failed to access keystore. incorrect passward");
      throw loadKeystoreException;
    } catch (Exception exception) {
      throw exception;
    } 
  }
  
  public ZipSigner b(String paramString) {
    ZipSigner zipSigner = new ZipSigner();
    zipSigner.issueLoadingCertAndKeysProgressEvent();
    String str = a();
    zipSigner.setKeys("custom", (X509Certificate)this.a.getCertificate(str), (PrivateKey)this.a.getKey(str, paramString.toCharArray()), "SHA1WITHRSA", null);
    return zipSigner;
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\iI.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */