package org.jabref.integration;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.formatter.bibtexfields.ReplaceTabsBySpaceFormater;
import org.jabref.model.entry.Author;
import org.jabref.model.study.StudyCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class FormAuthStudyIntegrationTest { // this class integrates Study, Formatter, Author
    @Test
    public void integration() {
        Formatter FORMAT = new ReplaceTabsBySpaceFormater();
        Author author = new Author("joe\tWoah", "", "", "others", "null");
        String result = FORMAT.format(author.getGivenName().orElse(""));
        String expected = "joe Woah";
        StudyCatalog study = new StudyCatalog(author.getGivenName().orElse(""), true);
        assertNotSame(study, author);
        assertEquals(expected, result);
    }
}
