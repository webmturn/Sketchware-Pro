package pro.sketchware.core;

import java.io.OutputStream;

public class KeyStoreOutputStream extends OutputStream {
  public final KeyStoreManager keyStoreManager;
  
  public KeyStoreOutputStream(KeyStoreManager paramiI) {
    this.keyStoreManager = paramiI;
  }
  
  public void write(int paramInt) {
    this.keyStoreManager.keyBuffer.put((byte)paramInt);
  }
}
