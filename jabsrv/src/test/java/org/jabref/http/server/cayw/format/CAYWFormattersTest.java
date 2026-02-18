package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.CitationProperties;
import org.jabref.http.server.cayw.LocatorType;
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
                     .map(key -> new CAYWEntry(new BibEntry().withCitationKey(key), key, key, "", new CitationProperties()))
                     .collect(Collectors.toList());
    }

    private CAYWEntry caywEntryWithProperties(String key, CitationProperties properties) {
        return new CAYWEntry(new BibEntry().withCitationKey(key), key, key, "", properties);
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

    @Test
    void pandoc_withAllProperties() {
        CitationProperties props = new CitationProperties();
        props.setLocatorType(LocatorType.PAGE);
        props.setLocatorValue("42");
        props.setPrefix("see");
        props.setSuffix("for details");

        PandocFormatter formatter = new PandocFormatter();
        String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
        assertEquals("[see @key1, p. 42, for details]", actual);
    }

    @Test
    void pandoc_omitAuthor() {
        CitationProperties props = new CitationProperties();
        props.setOmitAuthor(true);

        PandocFormatter formatter = new PandocFormatter();
        String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
        assertEquals("[-@key1]", actual);
    }

    @Test
    void pandoc_prefixOnly() {
        CitationProperties props = new CitationProperties();
        props.setPrefix("see");

        PandocFormatter formatter = new PandocFormatter();
        String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
        assertEquals("[see @key1]", actual);
    }

    @Test
    void pandoc_mixedEntries() {
        CitationProperties props1 = new CitationProperties();
        props1.setPrefix("see");
        props1.setLocatorType(LocatorType.PAGE);
        props1.setLocatorValue("42");

        PandocFormatter formatter = new PandocFormatter();
        String actual = formatter.format(queryParams(null), List.of(
                caywEntryWithProperties("key1", props1),
                caywEntryWithProperties("key2", new CitationProperties())
        ));
        assertEquals("[see @key1, p. 42; @key2]", actual);
    }

    @Test
    void biblatex_withProperties() {
        CitationProperties props = new CitationProperties();
        props.setLocatorType(LocatorType.CHAPTER);
        props.setLocatorValue("3");
        props.setPrefix("see");

        BibLatexFormatter formatter = new BibLatexFormatter("autocite");
        String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
        assertEquals("\\autocites[see][ch. 3]{key1}", actual);
    }

    @Test
    void biblatex_omitAuthor() {
        CitationProperties props = new CitationProperties();
        props.setOmitAuthor(true);

        BibLatexFormatter formatter = new BibLatexFormatter("autocite");
        String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
        assertEquals("\\autocite*{key1}", actual);
    }
    
    @Test
    void biblatex_omitAuthor_singleEntry() {
        CitationProperties props = new CitationProperties();
        props.setOmitAuthor(true);

        BibLatexFormatter formatter = new BibLatexFormatter("autocite");
        String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
        assertEquals("\\autocite*{key1}", actual);
    }

    @Test
    void biblatex_omitAuthor_unsupportedCommand() {
        CitationProperties props = new CitationProperties();
        props.setOmitAuthor(true);

        BibLatexFormatter formatter = new BibLatexFormatter("textcite");
        String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
        assertEquals("\\textcite{key1}", actual);
    }

    @Test
    void biblatex_omitAuthor_multipleEntries_ignored() {
        CitationProperties props1 = new CitationProperties();
        props1.setOmitAuthor(true);

        BibLatexFormatter formatter = new BibLatexFormatter("autocite");
        String actual = formatter.format(queryParams(null), List.of(
                caywEntryWithProperties("key1", props1),
                caywEntryWithProperties("key2", new CitationProperties())
        ));
        assertEquals("\\autocites{key1}{key2}", actual);
    }

    @Test
    void biblatex_locatorAndSuffix() {
        CitationProperties props = new CitationProperties();
        props.setLocatorType(LocatorType.PAGE);
        props.setLocatorValue("42");
        props.setSuffix("emphasis added");

        BibLatexFormatter formatter = new BibLatexFormatter("autocite");
        String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
        assertEquals("\\autocites[][p. 42, emphasis added]{key1}", actual);
    }

    @Test
    void natbib_withProperties() {
        CitationProperties props = new CitationProperties();
        props.setLocatorType(LocatorType.PAGE);
        props.setLocatorValue("42");
        props.setPrefix("see");

        NatbibFormatter formatter = new NatbibFormatter("citep");
        String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
        assertEquals("\\citep[see][p. 42]{key1}", actual);
    }

    @Test
    void natbib_mixedEntries() {
        CitationProperties props1 = new CitationProperties();
        props1.setPrefix("see");

        NatbibFormatter formatter = new NatbibFormatter("citep");
        String actual = formatter.format(queryParams(null), List.of(
                caywEntryWithProperties("key1", props1),
                caywEntryWithProperties("key2", new CitationProperties())
        ));
        assertEquals("\\citep[see][]{key1}\\citep{key2}", actual);
    }

    @Test
    void typst_withLocator() {
        CitationProperties props = new CitationProperties();
        props.setLocatorType(LocatorType.PAGE);
        props.setLocatorValue("42");

        TypstFormatter formatter = new TypstFormatter();
        String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
        assertEquals("#cite(<key1>, supplement: [p. 42])", actual);
    }

    @Test
    void typst_omitAuthor() {
        CitationProperties props = new CitationProperties();
        props.setOmitAuthor(true);

        TypstFormatter formatter = new TypstFormatter();
        String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
        assertEquals("#cite(<key1>, form: \"year\")", actual);
    }

    @Test
    void mmd_withLocator() {
        CitationProperties props = new CitationProperties();
        props.setLocatorType(LocatorType.PAGE);
        props.setLocatorValue("42");

        MMDFormatter formatter = new MMDFormatter();
        String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
        assertEquals("[#key1, p. 42][]", actual);
    }

    @Test
    void mmd_withPrefix() {
        CitationProperties props = new CitationProperties();
        props.setPrefix("see");

        MMDFormatter formatter = new MMDFormatter();
        String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
        assertEquals("[see][#key1]", actual);
    }
}
