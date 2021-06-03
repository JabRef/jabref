package org.jabref.logic.remote.online;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.UnknownEntryType;

public class JabrefOnlineService {

    private static final String JABREF_ONLINE = "https://jabref-online.herokuapp.com/api";

    public JabrefOnlineService() {
    }

    public Optional<BibEntry> getSharedEntry(String id) {
        try {
            URL url = new URL(JABREF_ONLINE);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            GetByIdGraphQLQuery getByIdGraphQLQuery = new GetByIdGraphQLQuery(id);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = getByIdGraphQLQuery.asJson().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            EntryDto entryDto = null;
            BibEntry bibEntry = new BibEntry();
            for (FieldDto field : entryDto.getFields()) {
                Field fieldFromDto = new UnknownField(field.getName());
                bibEntry.setField(fieldFromDto, field.getValue());
            }

            bibEntry.setCitationKey(entryDto.getCitationKey());
            bibEntry.setType(new UnknownEntryType("asd"));
            return Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void save(BibEntry entry) throws JabrefOnlineException {
        try {
            URL url = new URL(JABREF_ONLINE);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            List<FieldDto> fieldDtos = entry.getFields().stream()
                                            .map(field -> new FieldDto(field.getName(), entry.getField(field).orElse("")))
                                            .collect(Collectors.toList());
            EntryDto entryDto = new EntryDto(entry.getType().getName(), entry.getCitationKey().orElse(""), fieldDtos);

            SaveEntryGraphQLQuery saveEntryGraphQLQuery = new SaveEntryGraphQLQuery(entryDto);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = saveEntryGraphQLQuery.asJson().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println(response);
            }
        } catch (IOException e) {
            throw new JabrefOnlineException();
        }
    }
}
