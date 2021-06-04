package org.jabref.logic.remote.online;

import java.util.HashMap;
import java.util.Map;

import org.jabref.logic.remote.online.dto.GraphQLGetByIdResponseData;
import org.jabref.logic.remote.online.dto.GraphQLResponseDto;
import org.jabref.logic.remote.online.dto.GraphQLSaveResponseData;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.types.StandardEntryType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.jabref.model.entry.field.StandardField.AUTHOR;

public class JabrefOnlineServiceTest {

    @Test
    public void init() throws JabrefOnlineException {
        JabrefOnlineService service = new JabrefOnlineService();

        Map<Field, String> fieldStringMap = new HashMap<>();
        fieldStringMap.put(AUTHOR, "JabRef devs");

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setCitationKey("some citation key");
        entry.setField(fieldStringMap);

        String id = service.save(entry);
        BibEntry bibEntry = service.getSharedEntry(id);

        System.out.println(bibEntry);
    }
}