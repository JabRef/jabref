package org.jabref.http.server.cayw;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationPropertiesTest {

    @Test
    void formattedLocator_withTypeAndValue() {
        CitationProperties props = new CitationProperties();
        props.setLocatorType(LocatorType.PAGE);
        props.setLocatorValue("42");
        assertEquals(Optional.of("p. 42"), props.getFormattedLocator());
    }

    @Test
    void formattedLocator_withChapter() {
        CitationProperties props = new CitationProperties();
        props.setLocatorType(LocatorType.CHAPTER);
        props.setLocatorValue("3");
        assertEquals(Optional.of("ch. 3"), props.getFormattedLocator());
    }

    @Test
    void formattedLocator_emptyWhenNoType() {
        CitationProperties props = new CitationProperties();
        props.setLocatorValue("42");
        assertEquals(Optional.empty(), props.getFormattedLocator());
    }

    @Test
    void formattedLocator_emptyWhenNoValue() {
        CitationProperties props = new CitationProperties();
        props.setLocatorType(LocatorType.PAGE);
        assertEquals(Optional.empty(), props.getFormattedLocator());
    }

    @Test
    void formattedLocator_emptyWhenBlankValue() {
        CitationProperties props = new CitationProperties();
        props.setLocatorType(LocatorType.PAGE);
        props.setLocatorValue("   ");
        assertEquals(Optional.empty(), props.getFormattedLocator());
    }

    @Test
    void postnote_locatorAndSuffix() {
        CitationProperties props = new CitationProperties();
        props.setLocatorType(LocatorType.PAGE);
        props.setLocatorValue("42");
        props.setSuffix("emphasis added");
        assertEquals(Optional.of("p. 42, emphasis added"), props.getPostnote());
    }

    @Test
    void postnote_locatorOnly() {
        CitationProperties props = new CitationProperties();
        props.setLocatorType(LocatorType.PAGE);
        props.setLocatorValue("42");
        assertEquals(Optional.of("p. 42"), props.getPostnote());
    }

    @Test
    void postnote_suffixOnly() {
        CitationProperties props = new CitationProperties();
        props.setSuffix("emphasis added");
        assertEquals(Optional.of("emphasis added"), props.getPostnote());
    }

    @Test
    void postnote_emptyWhenNothing() {
        CitationProperties props = new CitationProperties();
        assertEquals(Optional.empty(), props.getPostnote());
    }

    @Test
    void prefix_stripped() {
        CitationProperties props = new CitationProperties();
        props.setPrefix("  see  ");
        assertEquals(Optional.of("see"), props.getPrefix());
    }

    @Test
    void prefix_emptyWhenBlank() {
        CitationProperties props = new CitationProperties();
        props.setPrefix("   ");
        assertEquals(Optional.empty(), props.getPrefix());
    }

    @Test
    void prefix_emptyWhenNull() {
        CitationProperties props = new CitationProperties();
        assertEquals(Optional.empty(), props.getPrefix());
    }

    @Test
    void suffix_stripped() {
        CitationProperties props = new CitationProperties();
        props.setSuffix("  emphasis added  ");
        assertEquals(Optional.of("emphasis added"), props.getSuffix());
    }

    @Test
    void suffix_emptyWhenBlank() {
        CitationProperties props = new CitationProperties();
        props.setSuffix("   ");
        assertEquals(Optional.empty(), props.getSuffix());
    }

    @Test
    void hasProperties_falseWhenEmpty() {
        CitationProperties props = new CitationProperties();
        assertFalse(props.hasProperties());
    }

    @Test
    void hasProperties_trueWithPrefix() {
        CitationProperties props = new CitationProperties();
        props.setPrefix("see");
        assertTrue(props.hasProperties());
    }

    @Test
    void hasProperties_trueWithSuppressAuthor() {
        CitationProperties props = new CitationProperties();
        props.setOmitAuthor(true);
        assertTrue(props.hasProperties());
    }

    @Test
    void hasProperties_trueWithLocator() {
        CitationProperties props = new CitationProperties();
        props.setLocatorType(LocatorType.PAGE);
        props.setLocatorValue("42");
        assertTrue(props.hasProperties());
    }

    @Test
    void hasProperties_falseWithLocatorTypeButNoValue() {
        CitationProperties props = new CitationProperties();
        props.setLocatorType(LocatorType.PAGE);
        assertFalse(props.hasProperties());
    }
}
