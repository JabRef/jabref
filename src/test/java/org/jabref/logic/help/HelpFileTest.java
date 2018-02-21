package org.jabref.logic.help;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jabref.logic.net.URLDownload;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class HelpFileTest {
    private final String jabrefHelp = "https://help.jabref.org/en/";
    @Test
    public void referToValidPage() throws IOException {
        for (HelpFile help : HelpFile.values()) {
            URL url = new URL(jabrefHelp + help.getPageName());
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestProperty("User-Agent", URLDownload.USER_AGENT);
            assertEquals(200, http.getResponseCode());
        }
    }
}
