package org.jabref.logic.util;

import java.util.Arrays;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

public class TestEntry {

    private TestEntry() {
    }

    public static BibEntry getTestEntry() {

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setCitationKey("Smith2016");
        entry.setField(StandardField.AUTHOR, "Smith, Bill and Jones, Bob and Williams, Jeff");
        entry.setField(StandardField.EDITOR, "Taylor, Phil");
        entry.setField(StandardField.TITLE, "Title of the test entry");
        entry.setField(StandardField.NUMBER, "3");
        entry.setField(StandardField.VOLUME, "34");
        entry.setField(StandardField.ISSUE, "3");
        entry.setField(StandardField.YEAR, "2016");
        entry.setField(StandardField.PAGES, "45--67");
        entry.setField(StandardField.MONTH, "July");
        entry.setField(StandardField.FILE, ":testentry.pdf:PDF");
        entry.setField(StandardField.JOURNAL, "BibTeX Journal");
        entry.setField(StandardField.PUBLISHER, "JabRef Publishing");
        entry.setField(StandardField.ADDRESS, "Trondheim");
        entry.setField(StandardField.URL, "https://github.com/JabRef");
        entry.setField(StandardField.DOI, "10.1001/bla.blubb");
        entry.setField(StandardField.ABSTRACT,
                "This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger.");
        entry.setField(StandardField.COMMENT, "Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor incidunt ut labore et " +
                "dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquid ex ea commodi consequat. " +
                "Quis aute iure reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint obcaecat cupiditat non " +
                "proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
        entry.putKeywords(Arrays.asList("KeyWord1", "KeyWord2", "KeyWord3", "Keyword4"), ';');

        return entry;
    }

    public static BibEntry getTestEntryBook() {
        BibEntry entry = new BibEntry(StandardEntryType.Book);
        entry.setCitationKey("Harrer2018");
        entry.setField(StandardField.AUTHOR, "Simon Harrer and Jörg Lenhard and Linus Dietz");
        entry.setField(StandardField.EDITOR, "Andrea Steward");
        entry.setField(StandardField.TITLE, "Java by Comparison");
        entry.setField(StandardField.YEAR, "2018");
        entry.setField(StandardField.MONTH, "March");
        entry.setField(StandardField.PUBLISHER, "Pragmatic Bookshelf");
        entry.setField(StandardField.ADDRESS, "Raleigh, NC");
        return entry;
    }
}
