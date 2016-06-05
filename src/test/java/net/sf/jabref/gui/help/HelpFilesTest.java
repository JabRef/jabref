package net.sf.jabref.gui.help;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;

import static org.junit.Assert.*;

public class HelpFilesTest {
    private final String jabrefHelp = "http://help.jabref.org/en/";
    @Test
    public void referToValidPage() throws IOException {
        for (HelpFiles help : HelpFiles.values()) {
            URL url = new URL(jabrefHelp + help.getPageName());
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            assertEquals(200, http.getResponseCode());
        }
    }
}