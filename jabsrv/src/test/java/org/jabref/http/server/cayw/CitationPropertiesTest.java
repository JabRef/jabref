package org.jabref.http.server.cayw;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationPropertiesTest {

    @Nested
    class FormattedLocatorTest {

        private CitationProperties props;

        @BeforeEach
        void setUp() {
            props = new CitationProperties();
        }

        @Test
        void withTypeAndValue() {
            props.setLocatorType(LocatorType.PAGE);
            props.setLocatorValue("42");
            assertEquals(Optional.of("p. 42"), props.getFormattedLocator());
        }

        @Test
        void withChapter() {
            props.setLocatorType(LocatorType.CHAPTER);
            props.setLocatorValue("3");
            assertEquals(Optional.of("ch. 3"), props.getFormattedLocator());
        }

        @Test
        void emptyWhenNoType() {
            props.setLocatorValue("42");
            assertEquals(Optional.empty(), props.getFormattedLocator());
        }

        @Test
        void emptyWhenNoValue() {
            props.setLocatorType(LocatorType.PAGE);
            assertEquals(Optional.empty(), props.getFormattedLocator());
        }

        @Test
        void emptyWhenBlankValue() {
            props.setLocatorType(LocatorType.PAGE);
            props.setLocatorValue("   ");
            assertEquals(Optional.empty(), props.getFormattedLocator());
        }
    }

    @Nested
    class PostnoteTest {

        private CitationProperties props;

        @BeforeEach
        void setUp() {
            props = new CitationProperties();
        }

        @Test
        void locatorAndSuffix() {
            props.setLocatorType(LocatorType.PAGE);
            props.setLocatorValue("42");
            props.setSuffix("emphasis added");
            assertEquals(Optional.of("p. 42, emphasis added"), props.getPostnote());
        }

        @Test
        void locatorOnly() {
            props.setLocatorType(LocatorType.PAGE);
            props.setLocatorValue("42");
            assertEquals(Optional.of("p. 42"), props.getPostnote());
        }

        @Test
        void suffixOnly() {
            props.setSuffix("emphasis added");
            assertEquals(Optional.of("emphasis added"), props.getPostnote());
        }

        @Test
        void emptyWhenNothing() {
            assertEquals(Optional.empty(), props.getPostnote());
        }
    }

    @Nested
    class PrefixTest {

        private CitationProperties props;

        @BeforeEach
        void setUp() {
            props = new CitationProperties();
        }

        @Test
        void stripped() {
            props.setPrefix("  see  ");
            assertEquals(Optional.of("see"), props.getPrefix());
        }

        @Test
        void emptyWhenBlank() {
            props.setPrefix("   ");
            assertEquals(Optional.empty(), props.getPrefix());
        }

        @Test
        void emptyWhenNull() {
            assertEquals(Optional.empty(), props.getPrefix());
        }
    }

    @Nested
    class SuffixTest {

        private CitationProperties props;

        @BeforeEach
        void setUp() {
            props = new CitationProperties();
        }

        @Test
        void stripped() {
            props.setSuffix("  emphasis added  ");
            assertEquals(Optional.of("emphasis added"), props.getSuffix());
        }

        @Test
        void emptyWhenBlank() {
            props.setSuffix("   ");
            assertEquals(Optional.empty(), props.getSuffix());
        }
    }

    @Nested
    class HasPropertiesTest {

        private CitationProperties props;

        @BeforeEach
        void setUp() {
            props = new CitationProperties();
        }

        @Test
        void falseWhenEmpty() {
            assertFalse(props.hasProperties());
        }

        @Test
        void trueWithPrefix() {
            props.setPrefix("see");
            assertTrue(props.hasProperties());
        }

        @Test
        void trueWithOmitAuthor() {
            props.setOmitAuthor(true);
            assertTrue(props.hasProperties());
        }

        @Test
        void trueWithLocator() {
            props.setLocatorType(LocatorType.PAGE);
            props.setLocatorValue("42");
            assertTrue(props.hasProperties());
        }

        @Test
        void falseWithLocatorTypeButNoValue() {
            props.setLocatorType(LocatorType.PAGE);
            assertFalse(props.hasProperties());
        }
    }
}
