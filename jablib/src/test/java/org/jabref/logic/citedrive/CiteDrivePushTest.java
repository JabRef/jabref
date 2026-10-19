package org.jabref.logic.citedrive;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexParser;
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
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Guards what leaves the machine: the request body is plain BibTeX regardless of the user's export settings.
/// JabRef's metadata (here: the library-specific file directory) must not be uploaded, and the token has to arrive as a bearer header.
///
/// [utest->req~citedrive.push~1]
@ResourceLock("Localization.lang")
class CiteDrivePushTest {

    private static Stream<Arguments> batches() {
        Stream<Arguments> generated = Stream.of(2, 1_000).map(count -> Arguments.of(count, Optional.<Path>empty()));
        Stream<Arguments> fixture = Optional.ofNullable(System.getProperty("citedrive.batch.fixture"))
                                            .stream()
                                            .map(path -> Arguments.of(100_000, Optional.of(Path.of(path))));
        return Stream.concat(generated, fixture);
    }

    @ParameterizedTest
    @MethodSource("batches")
    @SuppressWarnings("unchecked")
    void batchUploadSendsEveryEntryInOneRequest(int count, Optional<Path> fixture, TestReporter reporter) throws IOException {
        ImportFormatPreferences importPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        long started = System.nanoTime();
        BibDatabaseContext context;
        if (fixture.isEmpty()) {
            BibDatabase database = new BibDatabase();
            IntStream.range(0, count).mapToObj(index -> new BibEntry(StandardEntryType.Article)
                             .withCitationKey("batch-" + index)
                             .withField(StandardField.AUTHOR, "Müller, Jane")
                             .withField(StandardField.TITLE, "Batch entry " + index)
                             .withChanged(true))
                     .forEach(database::insertEntry);
            context = new BibDatabaseContext(database);
        } else {
            try (Reader reader = Files.newBufferedReader(fixture.orElseThrow(), StandardCharsets.UTF_8)) {
                context = new BibtexParser(importPreferences).parse(reader).getDatabaseContext();
            }
        }
        long loaded = System.nanoTime();
        assertEquals(count, context.getEntries().size());
        List<BibEntry> entries = context.getEntries().stream().map(BibEntry::new).toList();
        long copied = System.nanoTime();
        HttpRequestWithBody postRequest = mock(HttpRequestWithBody.class);
        RequestBodyEntity bodyEntity = mock(RequestBodyEntity.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(postRequest.header(anyString(), anyString())).thenReturn(postRequest);
        when(postRequest.body(anyString())).thenReturn(bodyEntity);
        when(bodyEntity.asString()).thenReturn(response);
        when(response.isSuccess()).thenReturn(true);

        try (MockedStatic<Unirest> unirestMock = Mockito.mockStatic(Unirest.class)) {
            unirestMock.when(() -> Unirest.post("https://example.com/jabref/push/")).thenReturn(postRequest);

            assertTrue(CiteDrivePush.push(context, entries, new BearerAccessToken("access-token"), createPreferences(),
                    mock(NotificationService.class), URI.create("https://example.com/jabref/push/")));
            long serialized = System.nanoTime();

            ArgumentCaptor<String> requestBody = ArgumentCaptor.forClass(String.class);
            verify(postRequest).body(requestBody.capture());
            unirestMock.verify(() -> Unirest.post("https://example.com/jabref/push/"));
            List<BibEntry> uploaded = new BibtexParser(importPreferences).parse(Reader.of(requestBody.getValue())).getDatabase().getEntries();
            Comparator<BibEntry> byKey = Comparator.comparing(entry -> entry.getCitationKey().orElseThrow());
            assertEquals(entries.stream().sorted(byKey).toList(), uploaded.stream().sorted(byKey).toList());
            assertEquals(context.getEntries().stream().sorted(byKey).toList(), entries.stream().sorted(byKey).toList());
            reporter.publishEntry(Map.of(
                    "entries", String.valueOf(count),
                    "bytes", String.valueOf(requestBody.getValue().getBytes(StandardCharsets.UTF_8).length),
                    "loadMs", String.valueOf((loaded - started) / 1_000_000),
                    "snapshotMs", String.valueOf((copied - loaded) / 1_000_000),
                    "serializeAndPrepareMs", String.valueOf((serialized - copied) / 1_000_000)));
        }
    }

    @Test
    void pushRejectsUnencryptedEndpointBeforeSendingCredentials() {
        try (MockedStatic<Unirest> unirestMock = Mockito.mockStatic(Unirest.class)) {
            BibDatabaseContext context = createDatabaseContext();
            assertThrows(IOException.class, () -> CiteDrivePush.push(context, context.getEntries(), new BearerAccessToken("access-token"),
                    createPreferences(), mock(NotificationService.class), URI.create("http://api.example/jabref/push/")));
            unirestMock.verifyNoInteractions();
        }
    }

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
        when(response.isSuccess()).thenReturn(true);

        NotificationService notificationService = mock(NotificationService.class);

        try (MockedStatic<Unirest> unirestMock = Mockito.mockStatic(Unirest.class)) {
            unirestMock.when(() -> Unirest.post("https://example.com/jabref/push/")).thenReturn(postRequest);
            BibDatabaseContext context = createDatabaseContext();
            boolean pushed = CiteDrivePush.push(
                    context,
                    context.getEntries(),
                    new BearerAccessToken("access-token"),
                    createPreferences(),
                    notificationService,
                    URI.create("https://example.com/jabref/push/"));

            verify(postRequest).header("Authorization", "Bearer access-token");
            verify(postRequest).header("Content-Type", "application/x-bibtex");
            String body = requestBody.get();
            assertNotNull(body);
            assertEquals("""
                    @Article{test-key,
                      author = {Doe, Jane},
                      title  = {CiteDrive push},
                    }
                    """.stripLeading(), body);
            verify(response).ifSuccess(Mockito.any());
            assertTrue(pushed);
        }
    }

    private BibDatabaseContext createDatabaseContext() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test-key")
                .withField(StandardField.AUTHOR, "Doe, Jane")
                .withField(StandardField.TITLE, "CiteDrive push")
                .withChanged(true);

        BibDatabase database = new BibDatabase();
        // The writer uses the library's separator; pinning it keeps the expected body the same on Windows
        database.setNewLineSeparator("\n");
        database.insertEntry(entry);
        BibDatabaseContext context = new BibDatabaseContext(database);
        context.getMetaData().setLibrarySpecificFileDirectory("/home/user/papers");
        return context;
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
