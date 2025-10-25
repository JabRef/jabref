package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.net.URLDownload;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.net.URL;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class INSPIREFetcher_HeadersTimeoutTest {

    @Test
    void getUrlDownload_setsHeadersAndTimeout() throws Exception {
        ImportFormatPreferences prefs = mock(ImportFormatPreferences.class);
        INSPIREFetcher fetcher = new INSPIREFetcher(prefs);

        URL url = new URL("https://inspirehep.net/api/literature/?q=test");

        try (MockedConstruction<URLDownload> mocked =
                     mockConstruction(URLDownload.class, (mock, context) -> {})) {

            URLDownload dl = fetcher.getUrlDownload(url);

            // 拿到刚被构造的实例，校验调用
            URLDownload constructed = mocked.constructed().get(0);
            verify(constructed).addHeader(eq("Accept"),  contains("bibtex"));
            verify(constructed).addHeader(eq("User-Agent"), startsWith("JabRef/"));
            verify(constructed).setConnectTimeout(eq(Duration.ofMillis(10_000)));
        }
    }
}

