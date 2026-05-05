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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

    @Nested
    class PandocFormatterTest {

        private PandocFormatter formatter;

        @BeforeEach
        void setUp() {
            formatter = new PandocFormatter();
        }

        @Test
        void noProperties() {
            String actual = formatter.format(queryParams(null), caywEntries("key1", "key2"));
            assertEquals("[@key1; @key2]", actual);
        }

        @Test
        void withAllProperties() {
            CitationProperties props = new CitationProperties()
                    .withLocatorType(LocatorType.PAGE)
                    .withLocatorValue("42")
                    .withPrefix("see")
                    .withSuffix("for details");

            String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
            assertEquals("[see @key1, p. 42 for details]", actual);
        }

        @Test
        void omitAuthor() {
            CitationProperties props = new CitationProperties()
                    .withOmitAuthor(true);

            String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
            assertEquals("[-@key1]", actual);
        }

        @Test
        void prefixOnly() {
            CitationProperties props = new CitationProperties()
                    .withPrefix("see");

            String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
            assertEquals("[see @key1]", actual);
        }

        @Test
        void mixedEntries() {
            CitationProperties props1 = new CitationProperties()
                    .withPrefix("see")
                    .withLocatorType(LocatorType.PAGE)
                    .withLocatorValue("42");

            String actual = formatter.format(queryParams(null), List.of(
                    caywEntryWithProperties("key1", props1),
                    caywEntryWithProperties("key2", new CitationProperties())
            ));
            assertEquals("[see @key1, p. 42; @key2]", actual);
        }
    }

    @Nested
    class BibLatexFormatterTest {

        private BibLatexFormatter formatter;

        @BeforeEach
        void setUp() {
            formatter = new BibLatexFormatter("autocite");
        }

        @Test
        void noCommand() {
            String actual = formatter.format(queryParams(null), caywEntries("key1", "key2"));
            assertEquals("\\autocite{key1,key2}", actual);
            assertEquals(MediaType.TEXT_PLAIN_TYPE, formatter.getMediaType());
        }

        @Test
        void explicitCommand() {
            String actual = formatter.format(queryParams("cite"), caywEntries("key1"));
            assertEquals("\\cite{key1}", actual);
        }

        @Test
        void missingKey() {
            String actual = formatter.format(queryParams(null), caywEntries("key1", "", "key3"));
            assertEquals("\\autocite{key1,key3}", actual);
        }

        @Test
        void withProperties() {
            CitationProperties props = new CitationProperties()
                    .withLocatorType(LocatorType.CHAPTER)
                    .withLocatorValue("3")
                    .withPrefix("see");

            String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
            assertEquals("\\autocite[see][ch. 3]{key1}", actual);
        }

        @Test
        void omitAuthor() {
            CitationProperties props = new CitationProperties()
                    .withOmitAuthor(true);

            String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
            assertEquals("\\autocite*{key1}", actual);
        }

        @Test
        void omitAuthor_unsupportedCommand() {
            BibLatexFormatter textciteFormatter = new BibLatexFormatter("textcite");
            CitationProperties props = new CitationProperties()
                    .withOmitAuthor(true);

            String actual = textciteFormatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
            assertEquals("\\textcite{key1}", actual);
        }

        @Test
        void omitAuthor_multipleEntries_ignored() {
            CitationProperties props1 = new CitationProperties()
                    .withOmitAuthor(true);

            String actual = formatter.format(queryParams(null), List.of(
                    caywEntryWithProperties("key1", props1),
                    caywEntryWithProperties("key2", new CitationProperties())
            ));
            assertEquals("\\autocites{key1}{key2}", actual);
        }

        @Test
        void locatorAndSuffix() {
            CitationProperties props = new CitationProperties()
                    .withLocatorType(LocatorType.PAGE)
                    .withLocatorValue("42")
                    .withSuffix("emphasis added");

            String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
            assertEquals("\\autocite[][p. 42, emphasis added]{key1}", actual);
        }
    }

    @Nested
    class NatbibFormatterTest {

        private NatbibFormatter formatter;

        @BeforeEach
        void setUp() {
            formatter = new NatbibFormatter("citep");
        }

        @Test
        void noProperties() {
            String actual = formatter.format(queryParams(null), caywEntries("key1", "key2"));
            assertEquals("\\citep{key1,key2}", actual);
            assertEquals(MediaType.TEXT_PLAIN_TYPE, formatter.getMediaType());
        }

        @Test
        void withProperties() {
            CitationProperties props = new CitationProperties()
                    .withLocatorType(LocatorType.PAGE)
                    .withLocatorValue("42")
                    .withPrefix("see");

            String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
            assertEquals("\\citep[see][p. 42]{key1}", actual);
        }

        @Test
        void mixedEntries() {
            CitationProperties props1 = new CitationProperties()
                    .withPrefix("see");

            String actual = formatter.format(queryParams(null), List.of(
                    caywEntryWithProperties("key1", props1),
                    caywEntryWithProperties("key2", new CitationProperties())
            ));
            assertEquals("\\citep[see][]{key1}\\citep{key2}", actual);
        }
    }

    @Nested
    class TypstFormatterTest {

        private TypstFormatter formatter;

        @BeforeEach
        void setUp() {
            formatter = new TypstFormatter();
        }

        @Test
        void noProperties() {
            String actual = formatter.format(queryParams(null), caywEntries("key1", "key2"));
            assertEquals("#cite(<key1>) #cite(<key2>)", actual);
        }

        @Test
        void slashInKey() {
            String actual = formatter.format(queryParams(null), caywEntries("key1", "key2/slash", "key3"));
            assertEquals("#cite(<key1>) #cite(label(\"key2/slash\")) #cite(<key3>)", actual);
        }

        @Test
        void withLocator() {
            CitationProperties props = new CitationProperties()
                    .withLocatorType(LocatorType.PAGE)
                    .withLocatorValue("42");

            String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
            assertEquals("#cite(<key1>, supplement: [p. 42])", actual);
        }

        @Test
        void omitAuthor() {
            CitationProperties props = new CitationProperties()
                    .withOmitAuthor(true);

            String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
            assertEquals("#cite(<key1>, form: \"year\")", actual);
        }
    }

    @Nested
    class MMDFormatterTest {

        private MMDFormatter formatter;

        @BeforeEach
        void setUp() {
            formatter = new MMDFormatter();
        }

        @Test
        void noProperties() {
            String actual = formatter.format(queryParams(null), caywEntries("key1", "key2"));
            assertEquals("[#key1][#key2]", actual);
            assertEquals(MediaType.TEXT_PLAIN_TYPE, formatter.getMediaType());
        }

        @Test
        void withPrefix() {
            CitationProperties props = new CitationProperties()
                    .withPrefix("see");

            String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
            assertEquals("[see][#key1]", actual);
        }

        @Test
        void withLocator() {
            CitationProperties props = new CitationProperties()
                    .withLocatorType(LocatorType.PAGE)
                    .withLocatorValue("42");

            String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
            assertEquals("[\\]\\[p. 42][#key1]", actual);
        }

        @Test
        void withPrefixAndLocator() {
            CitationProperties props = new CitationProperties()
                    .withPrefix("see")
                    .withLocatorType(LocatorType.PAGE)
                    .withLocatorValue("42");

            String actual = formatter.format(queryParams(null), List.of(caywEntryWithProperties("key1", props)));
            assertEquals("[see\\]\\[p. 42][#key1]", actual);
        }
    }
}
