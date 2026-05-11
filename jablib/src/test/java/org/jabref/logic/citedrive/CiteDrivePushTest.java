package org.jabref.logic.citedrive;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.SaveOrder;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import kong.unirest.core.HttpRequestWithBody;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.RequestBodyEntity;
import kong.unirest.core.Unirest;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CiteDrivePushTest {

    @Test
    @SuppressWarnings("unchecked")
    void pushSendsSerializedBibtexBody() throws IOException {
        HttpRequestWithBody postRequest = mock(HttpRequestWithBody.class);
        RequestBodyEntity bodyEntity = mock(RequestBodyEntity.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(postRequest.header(anyString(), anyString())).thenReturn(postRequest);
        AtomicReference<String> requestBody = new AtomicReference<>();
        doAnswer(invocation -> {
            requestBody.set(invocation.getArgument(0));
            return bodyEntity;
        }).when(postRequest).body(anyString());
        when(bodyEntity.asString()).thenReturn(response);

        NotificationService notificationService = mock(NotificationService.class);

        try (MockedStatic<Unirest> unirestMock = Mockito.mockStatic(Unirest.class)) {
            unirestMock.when(() -> Unirest.post("https://example.com/jabref/push/")).thenReturn(postRequest);
            CiteDrivePush.push(
                    createDatabaseContext(),
                    new BearerAccessToken("access-token"),
                    createPreferences(),
                    notificationService,
                    URI.create("https://example.com/jabref/push/"));

            verify(postRequest).header("Authorization", "Bearer access-token");
            verify(postRequest).header("Content-Type", "application/x-bibtex");
            String body = requestBody.get();
            assertNotNull(body);
            assertTrue(body.contains("@Article{test-key,"), body);
            assertTrue(body.contains("  author = {Doe, Jane},"), body);
            verify(response).ifSuccess(Mockito.any());
        }
    }

    private BibDatabaseContext createDatabaseContext() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test-key")
                .withField(StandardField.AUTHOR, "Doe, Jane")
                .withField(StandardField.TITLE, "CiteDrive push")
                .withChanged(true);

        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);
        return new BibDatabaseContext(database);
    }

    private CliPreferences createPreferences() {
        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getSelfContainedExportConfiguration()).thenReturn(new SelfContainedSaveConfiguration(
                SaveOrder.getDefaultSaveOrder(),
                false,
                BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA,
                false));
        when(preferences.getFieldPreferences()).thenReturn(new FieldPreferences(true, List.of(), List.of()));
        when(preferences.getCitationKeyPatternPreferences()).thenReturn(mock(CitationKeyPatternPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(preferences.getCustomEntryTypesRepository()).thenReturn(new BibEntryTypesManager());
        return preferences;
    }
}
