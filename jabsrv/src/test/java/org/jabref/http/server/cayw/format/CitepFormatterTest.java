package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.Optional;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class CitepFormatterTest {

    private CAYWEntry stubEntry(String key) {
        BibEntry bib = mock(BibEntry.class);
        when(bib.getCitationKey()).thenReturn(Optional.of(key));

        CAYWEntry cayw = mock(CAYWEntry.class);
        when(cayw.bibEntry()).thenReturn(bib);
        return cayw;
    }

    @Test
    @DisplayName("getFormatNames() returns the two expected aliases in order")
    void testGetFormatNames() {
        CitepFormatter f = new CitepFormatter();
        assertEquals(List.of("citep", "cite"), f.getFormatNames());
    }

    @Test
    @DisplayName("format() produces \\citep{key1,key2}")
    void testFormat() {
        CitepFormatter f = new CitepFormatter();

        CAYWQueryParams qp = mock(CAYWQueryParams.class);          // command is ignored
        List<CAYWEntry> entries = List.of(stubEntry("key1"), stubEntry("key2"));

        String out = f.format(qp, entries);
        assertEquals("\\citep{key1,key2}", out);
    }
}
