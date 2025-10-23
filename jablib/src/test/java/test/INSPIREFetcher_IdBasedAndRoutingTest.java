// src/test/java/org/jabref/logic/importer/fetcher/INSPIREFetcher_IdBasedAndRoutingTest.java
package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class INSPIREFetcher_IdBasedAndRoutingTest {

    @Test
    void performSearchById_supportsArxivAndDoi() throws Exception {
        var prefs = mock(ImportFormatPreferences.class);
        var fetcher = Mockito.spy(new INSPIREFetcher(prefs));

        Parser parser = mock(Parser.class);
        when(parser.parseEntries(any())).thenReturn(List.of(new BibEntry()));
        doReturn(parser).when(fetcher).getParser();

        URLDownload dl = mock(URLDownload.class);
        when(dl.asInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doReturn(dl).when(fetcher).getUrlDownload(any(URL.class));

        assertThat(fetcher.performSearchById("arXiv:2101.00001")).isPresent();
        assertThat(fetcher.performSearchById("10.1145/123456")).isPresent();
        assertThat(fetcher.performSearchById("not-an-id")).isEmpty();
    }

    @Test
    void routing_prefersArxivOverDoiAndEmptyWhenNoId() throws Exception {
        var prefs = mock(ImportFormatPreferences.class);
        var fetcher = Mockito.spy(new INSPIREFetcher(prefs));

        Parser parser = mock(Parser.class);
        when(parser.parseEntries(any())).thenReturn(List.of(new BibEntry()));
        doReturn(parser).when(fetcher).getParser();

        URLDownload dl = mock(URLDownload.class);
        when(dl.asInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doReturn(dl).when(fetcher).getUrlDownload(any(URL.class));

        var arxiv = new BibEntry();
        arxiv.setField(StandardField.ARCHIVEPREFIX, "arXiv");
        arxiv.setField(StandardField.EPRINT, "2101.00001");
        assertThat(fetcher.performSearch(arxiv)).hasSize(1);

        var doi = new BibEntry();
        doi.setField(StandardField.DOI, "10.1000/abc");
        assertThat(fetcher.performSearch(doi)).hasSize(1);

        var none = new BibEntry();
        assertThat(fetcher.performSearch(none)).isEmpty();
    }
}

