package org.jabref.model.entry;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XDataTest {

    @Test
    void resolvesFieldsFromSingleXDataEntry() {
        BibEntry xdata = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("CAT")
                .withField(StandardField.PUBLISHER, "Addison-Wesley")
                .withField(StandardField.ADDRESS, "Reading, MA")
                .withField(StandardField.SERIES, "Computers and Typesetting");

        BibEntry book = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Knuth")
                .withField(StandardField.TITLE, "The {\\TeX} Book")
                .withField(StandardField.YEAR, "1984")
                .withField(StandardField.AUTHOR, "Knuth, Donald E.")
                .withField(StandardField.XDATA, "CAT")
                .withField(StandardField.NUMBER, "1");

        BibDatabase db = new BibDatabase(List.of(xdata, book));

        assertEquals(Optional.of("Addison-Wesley"), book.getResolvedFieldOrAlias(StandardField.PUBLISHER, db));
        assertEquals(Optional.of("Reading, MA"), book.getResolvedFieldOrAlias(StandardField.ADDRESS, db));
        assertEquals(Optional.of("Reading, MA"), book.getResolvedFieldOrAlias(StandardField.LOCATION, db));
        assertEquals(Optional.of("Computers and Typesetting"), book.getResolvedFieldOrAlias(StandardField.SERIES, db));
        assertEquals(Optional.of("1984"), book.getResolvedFieldOrAlias(StandardField.DATE, db));
    }

    @Test
    void directFieldTakesPrecedenceOverXData() {
        BibEntry xdata = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("CAT")
                .withField(StandardField.PUBLISHER, "Addison-Wesley")
                .withField(StandardField.ADDRESS, "Reading, MA");

        BibEntry book = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Knuth")
                .withField(StandardField.PUBLISHER, "Custom Publisher")
                .withField(StandardField.XDATA, "CAT");

        BibDatabase db = new BibDatabase(List.of(xdata, book));

        assertEquals(Optional.of("Custom Publisher"), book.getResolvedFieldOrAlias(StandardField.PUBLISHER, db));
        assertEquals(Optional.of("Reading, MA"), book.getResolvedFieldOrAlias(StandardField.ADDRESS, db));
    }

    @Test
    void resolvesFieldsFromMultipleCommaSeparatedXDataEntries() {
        BibEntry xdata1 = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("publisher-aw")
                .withField(StandardField.PUBLISHER, "Addison-Wesley")
                .withField(StandardField.ADDRESS, "Reading, MA");

        BibEntry xdata2 = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("series-cat")
                .withField(StandardField.PUBLISHER, "Fallback Publisher")
                .withField(StandardField.SERIES, "Computers and Typesetting");

        BibEntry book = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Knuth")
                .withField(StandardField.XDATA, "publisher-aw, series-cat");

        BibDatabase db = new BibDatabase(List.of(xdata1, xdata2, book));

        assertEquals(Optional.of("Addison-Wesley"), book.getResolvedFieldOrAlias(StandardField.PUBLISHER, db));
        assertEquals(Optional.of("Reading, MA"), book.getResolvedFieldOrAlias(StandardField.ADDRESS, db));
        assertEquals(Optional.of("Computers and Typesetting"), book.getResolvedFieldOrAlias(StandardField.SERIES, db));
    }

    @Test
    void resolvesChainedTransitiveXDataInheritance() {
        BibEntry publisherXData = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("publisher-aw")
                .withField(StandardField.PUBLISHER, "Addison-Wesley")
                .withField(StandardField.LOCATION, "Reading, MA");

        BibEntry seriesXData = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("series-cat")
                .withField(StandardField.SERIES, "Computers and Typesetting")
                .withField(StandardField.XDATA, "publisher-aw");

        BibEntry book = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Knuth")
                .withField(StandardField.XDATA, "series-cat");

        BibDatabase db = new BibDatabase(List.of(publisherXData, seriesXData, book));

        assertEquals(Optional.of("Computers and Typesetting"), book.getResolvedFieldOrAlias(StandardField.SERIES, db));
        assertEquals(Optional.of("Addison-Wesley"), book.getResolvedFieldOrAlias(StandardField.PUBLISHER, db));
        assertEquals(Optional.of("Reading, MA"), book.getResolvedFieldOrAlias(StandardField.LOCATION, db));
    }

    @Test
    void resolvesXDataCombinedWithCrossref() {
        BibEntry xdataParent = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("pub-parent")
                .withField(StandardField.PUBLISHER, "Parent Publisher")
                .withField(StandardField.ADDRESS, "Parent City");

        BibEntry xdataChild = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("pub-child")
                .withField(StandardField.ADDRESS, "Child City");

        BibEntry parent = new BibEntry(StandardEntryType.Proceedings)
                .withCitationKey("proc")
                .withField(StandardField.TITLE, "Proceedings Title")
                .withField(StandardField.XDATA, "pub-parent");

        BibEntry child = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("paper")
                .withField(StandardField.CROSSREF, "proc")
                .withField(StandardField.XDATA, "pub-child");

        BibDatabase db = new BibDatabase(List.of(xdataParent, xdataChild, parent, child));

        // Child's own xdata takes precedence over crossref's xdata
        assertEquals(Optional.of("Child City"), child.getResolvedFieldOrAlias(StandardField.ADDRESS, db));
        // Falls back to crossref parent's xdata when not present on child or child's xdata
        assertEquals(Optional.of("Parent Publisher"), child.getResolvedFieldOrAlias(StandardField.PUBLISHER, db));
        // Crossref title mapping still works alongside xdata
        assertEquals(Optional.of("Proceedings Title"), child.getResolvedFieldOrAlias(StandardField.BOOKTITLE, db));
    }

    @Test
    void circularXDataReferencesTerminateSafely() {
        BibEntry xdata1 = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("xdata1")
                .withField(StandardField.PUBLISHER, "Addison-Wesley")
                .withField(StandardField.XDATA, "xdata2");

        BibEntry xdata2 = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("xdata2")
                .withField(StandardField.XDATA, "xdata1");

        BibEntry book = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Knuth")
                .withField(StandardField.XDATA, "xdata1");

        BibDatabase db = new BibDatabase(List.of(xdata1, xdata2, book));

        assertEquals(Optional.of("Addison-Wesley"), book.getResolvedFieldOrAlias(StandardField.PUBLISHER, db));
        assertTrue(book.getResolvedFieldOrAlias(StandardField.SERIES, db).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = StandardField.class, names = {"IDS", "CROSSREF", "XREF", "ENTRYSET", "RELATED", "SORTKEY"})
    void forbiddenFieldsAreNotInheritedFromXData(StandardField field) {
        BibEntry xdata = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("CAT")
                .withField(field, "xdata-value");

        BibEntry book = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Knuth")
                .withField(StandardField.XDATA, "CAT");

        BibDatabase db = new BibDatabase(List.of(xdata, book));

        assertTrue(book.getResolvedFieldOrAlias(field, db).isEmpty());
    }
}

