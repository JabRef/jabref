package org.jabref.model.entry;

import java.util.ArrayList;
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

    @Test
    void keyLookupDoesNotInheritForbiddenSortkeyFromXData() {
        BibEntry xdataWithSortkey = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("CAT")
                .withField(StandardField.SORTKEY, "xdata-sortkey");

        BibEntry book = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Knuth")
                .withField(StandardField.XDATA, "CAT");

        BibDatabase db = new BibDatabase(List.of(xdataWithSortkey, book));

        assertTrue(book.getResolvedFieldOrAlias(StandardField.SORTKEY, db).isEmpty());
        assertTrue(book.getResolvedFieldOrAlias(StandardField.KEY, db).isEmpty());

        BibEntry xdataWithKey = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("CAT-KEY")
                .withField(StandardField.KEY, "xdata-key");
        db.insertEntry(xdataWithKey);
        book.setField(StandardField.XDATA, "CAT, CAT-KEY");

        assertEquals(Optional.of("xdata-key"), book.getResolvedFieldOrAlias(StandardField.KEY, db));

        book.setField(StandardField.SORTKEY, "own-sortkey");
        assertEquals(Optional.of("own-sortkey"), book.getResolvedFieldOrAlias(StandardField.KEY, db));
    }

    @Test
    void resolvesDeepXDataChainIteratively() {
        List<BibEntry> entries = new ArrayList<>();
        int chainLength = 64;
        for (int i = 0; i < chainLength; i++) {
            BibEntry xdata = new BibEntry(new UnknownEntryType("xdata"))
                    .withCitationKey("xdata-" + i);
            if (i < (chainLength - 1)) {
                xdata.withField(StandardField.XDATA, "xdata-" + (i + 1));
            } else {
                xdata.withField(StandardField.PUBLISHER, "Deep Publisher");
            }
            entries.add(xdata);
        }

        BibEntry book = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Knuth")
                .withField(StandardField.XDATA, "xdata-0");
        entries.add(book);

        BibDatabase db = new BibDatabase(entries);

        assertEquals(Optional.of("Deep Publisher"), book.getResolvedFieldOrAlias(StandardField.PUBLISHER, db));
    }

    @Test
    void resolvesArticleFieldsThroughTransitiveXDataChain() {
        BibEntry journalXData = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("journal-pub")
                .withField(StandardField.JOURNALTITLE, "Journal of XData Testing")
                .withField(StandardField.PUBLISHER, "Macmillan")
                .withField(StandardField.LOCATION, "New York and London");

        BibEntry issueXData = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("journal-issue")
                .withField(StandardField.VOLUME, "42")
                .withField(StandardField.NUMBER, "3")
                .withField(StandardField.DATE, "2024")
                .withField(StandardField.NOTE, "Special Issue")
                .withField(StandardField.XDATA, "journal-pub");

        BibEntry article = new BibEntry(StandardEntryType.Article)
                .withCitationKey("art1")
                .withField(StandardField.AUTHOR, "Ellington, Edward")
                .withField(StandardField.TITLE, "Transitive Article XData")
                .withField(StandardField.XDATA, "journal-issue");

        BibDatabase db = new BibDatabase(List.of(journalXData, issueXData, article));

        assertEquals(Optional.of("Journal of XData Testing"), article.getResolvedFieldOrAlias(StandardField.JOURNALTITLE, db));
        assertEquals(Optional.of("Journal of XData Testing"), article.getResolvedFieldOrAlias(StandardField.JOURNAL, db));
        assertEquals(Optional.of("Macmillan"), article.getResolvedFieldOrAlias(StandardField.PUBLISHER, db));
        assertEquals(Optional.of("New York and London"), article.getResolvedFieldOrAlias(StandardField.LOCATION, db));
        assertEquals(Optional.of("New York and London"), article.getResolvedFieldOrAlias(StandardField.ADDRESS, db));
        assertEquals(Optional.of("42"), article.getResolvedFieldOrAlias(StandardField.VOLUME, db));
        assertEquals(Optional.of("3"), article.getResolvedFieldOrAlias(StandardField.NUMBER, db));
        assertEquals(Optional.of("2024"), article.getResolvedFieldOrAlias(StandardField.DATE, db));
        assertEquals(Optional.of("2024"), article.getResolvedFieldOrAlias(StandardField.YEAR, db));
        assertEquals(Optional.of("Special Issue"), article.getResolvedFieldOrAlias(StandardField.NOTE, db));
    }

    @Test
    void resolvesArticleFieldsFromMultipleXDataEntriesWithLeftToRightPrecedence() {
        BibEntry issue1 = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("issue-1")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.NOTE, "Primary Note");

        BibEntry issue2 = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("issue-2")
                .withField(StandardField.NUMBER, "2")
                .withField(StandardField.NOTE, "Secondary Note")
                .withField(StandardField.PUBLISHER, "Macmillan")
                .withField(StandardField.EID, "e123");

        BibEntry article = new BibEntry(StandardEntryType.Article)
                .withCitationKey("art2")
                .withField(StandardField.AUTHOR, "Pillington, Peter")
                .withField(StandardField.XDATA, "issue-1, issue-2");

        BibDatabase db = new BibDatabase(List.of(issue1, issue2, article));

        assertEquals(Optional.of("1"), article.getResolvedFieldOrAlias(StandardField.NUMBER, db));
        assertEquals(Optional.of("Primary Note"), article.getResolvedFieldOrAlias(StandardField.NOTE, db));
        assertEquals(Optional.of("Macmillan"), article.getResolvedFieldOrAlias(StandardField.PUBLISHER, db));
        assertEquals(Optional.of("e123"), article.getResolvedFieldOrAlias(StandardField.EID, db));
    }

    @Test
    void directArticleFieldTakesPrecedenceOverXData() {
        BibEntry xdata = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("macmillan-art")
                .withField(StandardField.JOURNALTITLE, "Biber Quarterly")
                .withField(StandardField.PUBLISHER, "Macmillan")
                .withField(StandardField.LOCATION, "New York and London")
                .withField(StandardField.NUMBER, "7")
                .withField(StandardField.NOTE, "A Note");

        BibEntry article = new BibEntry(StandardEntryType.Article)
                .withCitationKey("art-direct")
                .withField(StandardField.AUTHOR, "Pillington, Peter")
                .withField(StandardField.PUBLISHER, "Routledge")
                .withField(StandardField.NUMBER, "12")
                .withField(StandardField.XDATA, "macmillan-art");

        BibDatabase db = new BibDatabase(List.of(xdata, article));

        assertEquals(Optional.of("Routledge"), article.getResolvedFieldOrAlias(StandardField.PUBLISHER, db));
        assertEquals(Optional.of("12"), article.getResolvedFieldOrAlias(StandardField.NUMBER, db));
        assertEquals(Optional.of("New York and London"), article.getResolvedFieldOrAlias(StandardField.LOCATION, db));
        assertEquals(Optional.of("Biber Quarterly"), article.getResolvedFieldOrAlias(StandardField.JOURNALTITLE, db));
        assertEquals(Optional.of("A Note"), article.getResolvedFieldOrAlias(StandardField.NOTE, db));
    }

    @Test
    void circularAndMissingXDataReferencesInArticleTerminateSafely() {
        BibEntry loop1 = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("loop:1")
                .withField(StandardField.NOTE, "Note");
        BibEntry loop2 = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("loop:2")
                .withField(StandardField.XDATA, "loop:3");
        BibEntry loop3 = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("loop:3")
                .withField(StandardField.XDATA, "loop");
        BibEntry loop = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("loop")
                .withField(StandardField.XDATA, "loop:1, loop:2");

        BibEntry article = new BibEntry(StandardEntryType.Article)
                .withCitationKey("art-loop")
                .withField(StandardField.AUTHOR, "Ellington, Edward")
                .withField(StandardField.DATE, "1923")
                .withField(StandardField.XDATA, "missingxd, loop");

        BibDatabase db = new BibDatabase(List.of(loop1, loop2, loop3, loop, article));

        assertEquals(Optional.of("Note"), article.getResolvedFieldOrAlias(StandardField.NOTE, db));
        assertTrue(article.getResolvedFieldOrAlias(StandardField.PUBLISHER, db).isEmpty());
    }
}

