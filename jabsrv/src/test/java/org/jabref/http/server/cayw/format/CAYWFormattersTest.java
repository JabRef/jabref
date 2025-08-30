package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.Optional;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.model.entry.BibEntry;

import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CAYWFormattersTest {

    private static CAYWEntry caywEntry(String key) {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setCitationKey(key);
        return new CAYWEntry(bibEntry, "", "", "");
    }

    private static CAYWQueryParams queryParams(String command) {
        CAYWQueryParams mock = Mockito.mock(CAYWQueryParams.class);
        Mockito.when(mock.getCommand()).thenReturn(Optional.ofNullable(command));
        return mock;
    }

    @Test
    void biblatex_noCommand() {
        BibLatexFormatter formatter = new BibLatexFormatter("autocite");
        String actual = formatter.format(queryParams(null), List.of(caywEntry("key1"), caywEntry("key2")));
        assertEquals("\\autocite{key1,key2}", actual);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, formatter.getMediaType());
    }

    @Test
    void biblatex_explicitCommand() {
        BibLatexFormatter formatter = new BibLatexFormatter("autocite");
        String actual = formatter.format(queryParams("cite"), List.of(caywEntry("key1")));
        assertEquals("\\cite{key1}", actual);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, formatter.getMediaType());
    }

    @Test
    void biblatex_missingKey() {
        // If you decide to DROP empties, change formatter + this expectation.
        BibLatexFormatter formatter = new BibLatexFormatter("autocite");
        CAYWEntry entry1 = caywEntry("key1");
        CAYWEntry entry2 = caywEntry("");
        CAYWEntry entry3 = caywEntry("key3");
        String actual = formatter.format(queryParams(null), List.of(entry1, entry2, entry3));
        assertEquals("\\autocite{key1,key3}", actual); // current implementation
        assertEquals(MediaType.TEXT_PLAIN_TYPE, formatter.getMediaType());
    }

    @Test
    void natbib_citep() {
        NatbibFormatter formatter = new NatbibFormatter("citep");
        String actual = formatter.format(queryParams(null), List.of(caywEntry("key1"), caywEntry("key2")));
        assertEquals("\\citep{key1,key2}", actual);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, formatter.getMediaType());
    }

    @Test
    void mmd() {
        MMDFormatter formatter = new MMDFormatter();
        String actual = formatter.format(queryParams(null), List.of(caywEntry("key1"), caywEntry("key2")));
        // Whatever your MMD formatter currently emits; adjust expected accordingly.
        assertEquals("[#key1][][#key2][]", actual);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, formatter.getMediaType());
    }

    @Test
    void pandoc() {
        PandocFormatter formatter = new PandocFormatter();
        String actual = formatter.format(queryParams(null), List.of(caywEntry("key1"), caywEntry("key2")));
        assertEquals("[@key1; @key2]", actual);
    }

    @Test
    void typst() {
        TypstFormatter formatter = new TypstFormatter();
        String actual = formatter.format(queryParams(null), List.of(caywEntry("key1"), caywEntry("key2")));
        assertEquals("@key1 @key2", actual);
    }
}
