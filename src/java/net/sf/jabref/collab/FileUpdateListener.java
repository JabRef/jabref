package net.sf.jabref.collab;

public interface FileUpdateListener {

  /**
   * The file has been updated. A new call will not result until the file has been modified again.
   */
  public void fileUpdated();

  /**
   * The file does no longer exist.
   */
  public void fileRemoved();

}
