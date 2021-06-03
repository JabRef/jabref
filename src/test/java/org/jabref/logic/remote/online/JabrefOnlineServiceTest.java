package org.jabref.logic.remote.online;

import java.util.HashMap;
import java.util.Map;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.Test;

import static org.jabref.model.entry.field.InternalField.TYPE_HEADER;

public class JabrefOnlineServiceTest {

    @Test
    public void init() throws JabrefOnlineException {
        JabrefOnlineService service = new JabrefOnlineService();

        Map<Field, String> fieldStringMap = new HashMap<>();
        fieldStringMap.put(TYPE_HEADER, "book");

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setCitationKey("asdsdad");
        entry.setField(fieldStringMap);

        service.save(entry);
    }
}