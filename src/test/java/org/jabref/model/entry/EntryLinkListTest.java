package org.jabref.model.entry;

import org.jabref.model.database.BibDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class EntryLinkListTest {

    String fieldValue = "test";
    BibDatabase bibDatabase = new BibDatabase();

    @Test
    public void givenFieldValueAndDatabaseWhenParsingThenExpectLink() {
        List<ParsedEntryLink> links = EntryLinkList.parse(fieldValue, bibDatabase);
        ParsedEntryLink link = links.get(0);

        assertEquals(fieldValue, link.getKey());
        assertEquals(bibDatabase, link.getDataBase());
        assertEquals(Optional.empty(), link.getLinkedEntry());
    }

    @Test
    public void givenNullFieldValueAndDatabaseWhenParsingThenExpectLinksSizeZero() {
        List<ParsedEntryLink> links = EntryLinkList.parse(null, bibDatabase);

        assertEquals(0, links.size());
    }
}
