package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.model.entry.BibEntry;

import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CAYWFormattersTest {

    private List<CAYWEntry> caywEntries(String... keys) {
        if (keys == null) {
            return List.of();
        }
        return Stream.of(keys)
                     .map(key -> new CAYWEntry(new BibEntry().withCitationKey(key), key, key, ""))
                     .collect(Collectors.toList());
    }

    private CAYWQueryParams queryParams(String command) {
        CAYWQueryParams mock = Mockito.mock(CAYWQueryParams.class);
        Mockito.when(mock.getCommand()).thenReturn(Optional.ofNullable(command));
        return mock;
    }

    @Test
    void biblatex_noCommand() {
        BibLatexFormatter formatter = new BibLatexFormatter("autocite");
        String actual = formatter.format(queryParams(null), caywEntries("key1", "key2"));
        assertEquals("\\autocite{key1,key2}", actual);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, formatter.getMediaType());
    }

    @Test
    void biblatex_explicitCommand() {
        BibLatexFormatter formatter = new BibLatexFormatter("autocite");
        String actual = formatter.format(queryParams("cite"), caywEntries("key1"));
        assertEquals("\\cite{key1}", actual);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, formatter.getMediaType());
    }

    @Test
    void biblatex_missingKey() {
        BibLatexFormatter formatter = new BibLatexFormatter("autocite");
        String actual = formatter.format(queryParams(null), caywEntries("key1", "", "key3"));
        assertEquals("\\autocite{key1,key3}", actual);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, formatter.getMediaType());
    }

    @Test
    void natbib_citep() {
        NatbibFormatter formatter = new NatbibFormatter("citep");
        String actual = formatter.format(queryParams(null), caywEntries("key1", "key2"));
        assertEquals("\\citep{key1,key2}", actual);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, formatter.getMediaType());
    }

    @Test
    void mmd() {
        MMDFormatter formatter = new MMDFormatter();
        String actual = formatter.format(queryParams(null), caywEntries("key1", "key2"));
        assertEquals("[#key1][][#key2][]", actual);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, formatter.getMediaType());
    }

    @Test
    void pandoc() {
        PandocFormatter formatter = new PandocFormatter();
        String actual = formatter.format(queryParams(null), caywEntries("key1", "key2"));
        assertEquals("[@key1; @key2]", actual);
    }

    @Test
    void typst() {
        TypstFormatter formatter = new TypstFormatter();
        String actual = formatter.format(queryParams(null), caywEntries("key1", "key2"));
        assertEquals("#cite(<key1>) #cite(<key2>)", actual);
    }

    @Test
    void typst_slashInKey() {
        TypstFormatter formatter = new TypstFormatter();
        String actual = formatter.format(queryParams(null), caywEntries("key1", "key2/slash", "key3"));
        assertEquals("#cite(<key1>) #cite(label(\"key2/slash\")) #cite(<key3>)", actual);
    }
}
