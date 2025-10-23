// src/test/java/org/jabref/logic/importer/fetcher/INSPIREFetcher_TexkeysTest.java
package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class INSPIREFetcherTexkeysTest {

    @Test
    void texkeysAppliedAndCleared() throws Exception {
        var prefs = mock(ImportFormatPreferences.class);
        var fetcher = Mockito.spy(new INSPIREFetcher(prefs));

        String bib = """
                @article{dummy,
                  title={T},
                  texkeys={Smith2020,Another}
                }
                """;
        Parser parser = mock(Parser.class);
        when(parser.parseEntries(any()))
            .thenReturn(new BibtexParser(prefs)
                    .parseEntries(new ByteArrayInputStream(bib.getBytes())));
        doReturn(parser).when(fetcher).getParser();

        URLDownload dl = mock(URLDownload.class);
        when(dl.asInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doReturn(dl).when(fetcher).getUrlDownload(any(URL.class));

        var base = new BibEntry();
        base.setField(StandardField.DOI, "10.1000/xyz");

        var out = fetcher.performSearch(base);
        assertThat(out).hasSize(1);
        var e = out.get(0);
        assertThat(e.getCitationKey()).hasValue("Smith2020");
        assertThat(e.getField("texkeys")).isEmpty();
    }
}

