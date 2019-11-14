package org.jabref.logic.net;

import java.net.URISyntaxException;

import org.jabref.Globals;
import org.jabref.JabRefException;
import org.jabref.preferences.JabRefPreferences;

/**
 * Implements an API to a GROBID server, as described at
 * https://grobid.readthedocs.io/en/latest/Grobid-service/#grobid-web-services
 */
public class GrobidClient {

  private HttpPostService httpPostService;

  public GrobidClient() throws JabRefException {
    try {
      httpPostService = new HttpPostService(Globals.prefs.get(JabRefPreferences.CUSTOM_GROBID_SERVER));
    } catch (URISyntaxException e) {
      throw new JabRefException("Something seems wrong with your custom GROBID server address.");
    }
  }



}
