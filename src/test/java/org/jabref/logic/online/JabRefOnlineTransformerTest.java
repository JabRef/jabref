package org.jabref.logic.online;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.jabref.jabrefonline.UserChangesQuery.Node;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.junit.jupiter.api.Test;

public class JabRefOnlineTransformerTest {

    @Test
    public void transform() {
        var result = new JabRefOnlineTransformer().toBibEntry(new Node(
            "JournalArticle", 
            "testId", 
            List.of("testKey"), 
            ZonedDateTime.of(2022, 10, 1, 0, 0, 0, 0, ZoneId.of("Z")),
            ZonedDateTime.of(2022, 10, 1, 0, 0, 0, 0, ZoneId.of("Z")),
            "testTitle", 
            "testSubtitle",
            null,
            "testAbstract",
            List.of(),
            "testNote",
            List.of(),
            "testPublicationState",
            "testDoi",
             List.of(),
             null));

        var expected = new BibEntry()
            .withCitationKey("testKey")
            .withField(StandardField.MODIFICATIONDATE, "2022-10-01T00:00:00Z")
            .withField(StandardField.CREATIONDATE, "2022-10-01T00:00:00Z")
            .withField(StandardField.TITLE, "testTitle")
            .withField(StandardField.SUBTITLE, "testSubtitle")
            .withField(StandardField.ABSTRACT, "testAbstract")
            .withField(StandardField.NOTE, "testNote")
            .withField(StandardField.PUBSTATE, "testPublicationState")
            .withField(StandardField.DOI, "testDoi");
        expected.setRevision(new LocalRevision("testId", null, null));

        assertEquals(expected, result);
    }
}
