package pro.sketchware.core;

import java.io.OutputStream;

public class KeyStoreOutputStream extends OutputStream {
  public final KeyStoreManager keyStoreManager;
  
  public KeyStoreOutputStream(KeyStoreManager keyStoreManager) {
    this.keyStoreManager = keyStoreManager;
  }
  
  public void write(int value) {
    this.keyStoreManager.keyBuffer.put((byte)value);
  }
}
