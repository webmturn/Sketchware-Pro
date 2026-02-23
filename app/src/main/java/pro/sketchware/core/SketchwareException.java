package pro.sketchware.core;

/**
 * Exception thrown during Sketchware project compilation/build operations.
 * Originally named By, renamed to avoid Windows case collision with by (interface).
 */
public class SketchwareException extends Exception {
  public SketchwareException(String message) {
    super(message);
  }
}
