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

        String id = "ckphc813n00351wykr17yksjl";
        BibEntry bibEntry = service.getSharedEntry(id);

        System.out.println(bibEntry);
    }

    @Test
    public void parseSaveResponse() throws JsonProcessingException {
        String s = "{\"data\":{\"addUserDocumentRaw\":{\"id\":\"ckphc813n00351wykr17yksjl\"}}}";
        ObjectMapper mapper = new ObjectMapper();
        GraphQLResponseDto<GraphQLSaveResponseData> graphQLResponseDto = mapper.readValue(s, GraphQLResponseDto.class);
    }

    @Test
    public void parseGetByIdResponse() throws JsonProcessingException {
        String json = "{\n" +
                "  \"data\": {\n" +
                "    \"getUserDocumentRaw\": {\n" +
                "      \"type\": \"article\",\n" +
                "      \"citationKey\": \"citeme\",\n" +
                "      \"fields\": [\n" +
                "        {\n" +
                "          \"field\": \"author\",\n" +
                "          \"value\": \"JabRef devs\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"field\": \"documentId\",\n" +
                "          \"value\": \"ckph1zzv600141wzgtgbe2eg2\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        GraphQLResponseDto<GraphQLGetByIdResponseData> graphQLResponseDto = mapper.readValue(json, GraphQLResponseDto.class);
        System.out.println(graphQLResponseDto);
    }
}