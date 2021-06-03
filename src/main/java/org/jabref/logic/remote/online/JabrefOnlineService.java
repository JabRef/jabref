package org.jabref.logic.remote.online;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.remote.online.dto.EntryDto;
import org.jabref.logic.remote.online.dto.FieldDto;
import org.jabref.logic.remote.online.dto.GetByIdGraphQLQuery;
import org.jabref.logic.remote.online.dto.GraphQLGetByIdResponseData;
import org.jabref.logic.remote.online.dto.GraphQLResponseDto;
import org.jabref.logic.remote.online.dto.GraphQLSaveResponseData;
import org.jabref.logic.remote.online.dto.SaveEntryGraphQLQuery;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.UnknownField;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JabrefOnlineService {

    private static final String JABREF_ONLINE = "https://jabref-online.herokuapp.com/api";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public JabrefOnlineService() {
    }

    public BibEntry getSharedEntry(String id) throws JabrefOnlineException {
        try {
            URL url = new URL(JABREF_ONLINE);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("content-type", "application/json");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            GetByIdGraphQLQuery getByIdGraphQLQuery = new GetByIdGraphQLQuery(id);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = MAPPER.writeValueAsString(getByIdGraphQLQuery).getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            EntryDto entryDto;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                GraphQLResponseDto<GraphQLGetByIdResponseData> graphQLResponseDto = MAPPER.readValue(response.toString(), new TypeReference<>() {
                });
                entryDto = graphQLResponseDto.getData().getEntryDto();
            }

            BibEntry bibEntry = new BibEntry();
            for (FieldDto field : entryDto.getFields()) {
                Field fieldFromDto = new UnknownField(field.getField());
                bibEntry.setField(fieldFromDto, field.getValue());
            }

            bibEntry.setCitationKey(entryDto.getCitationKey());
            return bibEntry;
        } catch (IOException e) {
            throw new JabrefOnlineException();
        }
    }

    public String save(BibEntry entry) throws JabrefOnlineException {
        try {
            URL url = new URL(JABREF_ONLINE);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("content-Type", "application/json");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            List<FieldDto> fieldDtos = entry.getFields().stream()
                                            .filter(field -> !field.getName().equals(InternalField.KEY_FIELD.getName()))
                                            .filter(field -> !field.getName().equals("entrytype"))
                                            .map(field -> new FieldDto(field.getName(), entry.getField(field).orElse("")))
                                            .collect(Collectors.toList());
            EntryDto entryDto = new EntryDto(entry.getType().getName(), entry.getCitationKey().orElse(""), fieldDtos);
            SaveEntryGraphQLQuery saveEntryGraphQLQuery = new SaveEntryGraphQLQuery(entryDto);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = MAPPER.writeValueAsString(saveEntryGraphQLQuery).getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                GraphQLResponseDto<GraphQLSaveResponseData> graphQLResponseDto = MAPPER.readValue(response.toString(), new TypeReference<>() {
                });
                return graphQLResponseDto.getData().getAddUserDocumentRaw().getId();
            }
        } catch (IOException e) {
            throw new JabrefOnlineException();
        }
    }
}
