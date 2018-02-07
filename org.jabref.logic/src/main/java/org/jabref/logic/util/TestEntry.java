package org.jabref.logic.util;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

public class TestEntry {

    private TestEntry() {
    }

    public static BibEntry getTestEntry() {

        BibEntry entry = new BibEntry("article");
        entry.setCiteKey("Smith2016");
        entry.setField(FieldName.AUTHOR, "Smith, Bill and Jones, Bob and Williams, Jeff");
        entry.setField(FieldName.EDITOR, "Taylor, Phil");
        entry.setField(FieldName.TITLE, "Title of the test entry");
        entry.setField(FieldName.NUMBER, "3");
        entry.setField(FieldName.VOLUME, "34");
        entry.setField(FieldName.YEAR, "2016");
        entry.setField(FieldName.PAGES, "45--67");
        entry.setField(FieldName.MONTH, "July");
        entry.setField(FieldName.FILE, ":testentry.pdf:PDF");
        entry.setField(FieldName.JOURNAL, "BibTeX Journal");
        entry.setField(FieldName.PUBLISHER, "JabRef Publishing");
        entry.setField(FieldName.ADDRESS, "Trondheim");
        entry.setField(FieldName.URL, "https://github.com/JabRef");
        entry.setField(FieldName.ABSTRACT,
                "This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger.");
        return entry;
    }
}
