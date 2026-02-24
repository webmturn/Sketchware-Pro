package pro.sketchware.core;

import java.io.OutputStream;

public class KeyStoreOutputStream extends OutputStream {
  public final KeyStoreManager a;
  
  public KeyStoreOutputStream(KeyStoreManager paramiI) {
    this.a = paramiI;
  }
  
  public void write(int paramInt) {
    this.a.keyBuffer.put((byte)paramInt);
  }
}
