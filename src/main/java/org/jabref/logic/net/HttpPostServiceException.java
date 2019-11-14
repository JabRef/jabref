package org.jabref.logic.net;

import org.jabref.JabRefException;

public class HttpPostServiceException extends JabRefException {

  public HttpPostServiceException() {
    super("An error occurred connecting to the external http server.");
  }

  public HttpPostServiceException(String message) {
    super(message);
  }

}
