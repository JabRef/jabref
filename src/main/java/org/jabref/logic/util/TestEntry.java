package org.jabref.logic.util;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

public class TestEntry {

    private TestEntry() {
    }

    public static BibEntry getTestEntry() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2016")
                .withField(StandardField.AUTHOR, "Smith, Bill and Jones, Bob and Williams, Jeff")
                .withField(StandardField.EDITOR, "Taylor, Phil")
                .withField(StandardField.TITLE, "Title of the test entry")
                .withField(StandardField.NUMBER, "3")
                .withField(StandardField.VOLUME, "34")
                .withField(StandardField.ISSUE, "7")
                .withField(StandardField.YEAR, "2016")
                .withField(StandardField.PAGES, "45--67")
                .withField(StandardField.MONTH, "July")
                .withField(StandardField.FILE, ":testentry.pdf:PDF")
                .withField(StandardField.JOURNAL, "BibTeX Journal")
                .withField(StandardField.PUBLISHER, "JabRef Publishing")
                .withField(StandardField.ADDRESS, "Trondheim")
                .withField(StandardField.URL, "https://github.com/JabRef")
                .withField(StandardField.DOI, "10.1001/bla.blubb")
                .withField(StandardField.ABSTRACT,
                        "This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger.")
                .withField(StandardField.COMMENT, """
                        Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor incidunt ut labore et
                          dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquid ex ea commodi consequat.
                          Quis aute iure reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint obcaecat cupiditat non
                          proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
                        """);
        entry.putKeywords(List.of("KeyWord1", "KeyWord2", "KeyWord3", "Keyword4"), ';');
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
