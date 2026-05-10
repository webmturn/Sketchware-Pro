package pro.sketchware.core.build;

import java.io.OutputStream;

public class KeyStoreOutputStream extends OutputStream {
  public final KeyStoreManager keyStoreManager;
  
  public KeyStoreOutputStream(KeyStoreManager keyStoreManager) {
    this.keyStoreManager = keyStoreManager;
  }
  
  public void write(int value) {
    keyStoreManager.keyBuffer.put((byte)value);
  }
}
