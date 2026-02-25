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
            props.withLocatorType(LocatorType.PAGE).withLocatorValue("42");
            assertEquals(Optional.of("p. 42"), props.getFormattedLocator());
        }

        @Test
        void withChapter() {
            props.withLocatorType(LocatorType.CHAPTER).withLocatorValue("3");
            assertEquals(Optional.of("ch. 3"), props.getFormattedLocator());
        }

        @Test
        void emptyWhenNoType() {
            props.withLocatorValue("42");
            assertEquals(Optional.empty(), props.getFormattedLocator());
        }

        @Test
        void emptyWhenNoValue() {
            props.withLocatorType(LocatorType.PAGE);
            assertEquals(Optional.empty(), props.getFormattedLocator());
        }

        @Test
        void emptyWhenBlankValue() {
            props.withLocatorType(LocatorType.PAGE).withLocatorValue("   ");
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
            props.withLocatorType(LocatorType.PAGE).withLocatorValue("42").withSuffix("emphasis added");
            assertEquals(Optional.of("p. 42, emphasis added"), props.getPostnote());
        }

        @Test
        void locatorOnly() {
            props.withLocatorType(LocatorType.PAGE).withLocatorValue("42");
            assertEquals(Optional.of("p. 42"), props.getPostnote());
        }

        @Test
        void suffixOnly() {
            props.withSuffix("emphasis added");
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
            props.withPrefix("  see  ");
            assertEquals(Optional.of("see"), props.getPrefix());
        }

        @Test
        void emptyWhenBlank() {
            props.withPrefix("   ");
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
            props.withSuffix("  emphasis added  ");
            assertEquals(Optional.of("emphasis added"), props.getSuffix());
        }

        @Test
        void emptyWhenBlank() {
            props.withSuffix("   ");
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
            props.withPrefix("see");
            assertTrue(props.hasProperties());
        }

        @Test
        void trueWithOmitAuthor() {
            props.withOmitAuthor(true);
            assertTrue(props.hasProperties());
        }

        @Test
        void trueWithLocator() {
            props.withLocatorType(LocatorType.PAGE).withLocatorValue("42");
            assertTrue(props.hasProperties());
        }

        @Test
        void falseWithLocatorTypeButNoValue() {
            props.withLocatorType(LocatorType.PAGE);
            assertFalse(props.hasProperties());
        }
    }
}
