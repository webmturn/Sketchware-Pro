package a.a.a;

import java.io.OutputStream;

public class KeyStoreOutputStream extends OutputStream {
  public final KeyStoreManager a;
  
  public KeyStoreOutputStream(KeyStoreManager paramiI) {
    this.a = paramiI;
  }
  
  public void write(int paramInt) {
    this.a.b.put((byte)paramInt);
  }
}
