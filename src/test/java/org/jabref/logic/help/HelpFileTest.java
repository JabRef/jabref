package org.jabref.logic.help;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.stream.Stream;

import org.jabref.logic.net.URLDownload;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelpFileTest {

    private final String jabrefHelp = "https://docs.jabref.org/";

    static Stream<HelpFile> getAllHelpFiles() {
        return Arrays.stream(HelpFile.values());
    }

    @ParameterizedTest
    @MethodSource("getAllHelpFiles")
    void referToValidPage(HelpFile help) throws IOException {
        URL url = new URL(jabrefHelp + help.getPageName());
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestProperty("User-Agent", URLDownload.USER_AGENT);
        assertEquals(200, http.getResponseCode(), "Wrong URL: " + url.toString());
    }
}
