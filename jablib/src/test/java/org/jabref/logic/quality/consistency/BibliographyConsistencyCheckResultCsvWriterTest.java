package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.prefs.BackingStoreException;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.InternalPreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.MrDlibPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.preferences.LastFilesOpenedPreferences;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.logic.push.PushToApplicationPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class BibliographyConsistencyCheckResultCsvWriterTest {

    private final BibEntryTypesManager bibEntryTypesManager = new BibEntryTypesManager();
    private final BibtexImporter importer = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
    private final CliPreferences cliPreferences = new CliPreferences() {
        @Override
        public void clear() throws BackingStoreException {

        }

        @Override
        public BibEntryTypesManager getCustomEntryTypesRepository(BibEntryTypesManager bibEntryTypesManager) {
            return bibEntryTypesManager;
        }

        @Override
        public void deleteKey(String key) throws IllegalArgumentException {

        }

        @Override
        public void flush() {

        }

        @Override
        public void exportPreferences(Path file) throws JabRefException {

        }

        @Override
        public void importPreferences(Path file) throws JabRefException {

        }

        @Override
        public InternalPreferences getInternalPreferences() {
            return null;
        }

        @Override
        public BibEntryPreferences getBibEntryPreferences() {
            return null;
        }

        @Override
        public JournalAbbreviationPreferences getJournalAbbreviationPreferences() {
            return null;
        }

        @Override
        public FilePreferences getFilePreferences() {
            return null;
        }

        @Override
        public FieldPreferences getFieldPreferences() {
            return null;
        }

        @Override
        public Map<String, Object> getPreferences() {
            return Map.of();
        }

        @Override
        public Map<String, Object> getDefaults() {
            return Map.of();
        }

        @Override
        public LayoutFormatterPreferences getLayoutFormatterPreferences() {
            return null;
        }

        @Override
        public ImportFormatPreferences getImportFormatPreferences() {
            return null;
        }

        @Override
        public SelfContainedSaveConfiguration getSelfContainedExportConfiguration() {
            return null;
        }

        public BibEntryTypesManager getCustomEntryTypesRepository() {
            return null;
        }

        @Override
        public void storeCustomEntryTypesRepository(BibEntryTypesManager entryTypesManager) {

        }

        @Override
        public CleanupPreferences getCleanupPreferences() {
            return null;
        }

        @Override
        public CleanupPreferences getDefaultCleanupPreset() {
            return null;
        }

        @Override
        public LibraryPreferences getLibraryPreferences() {
            return null;
        }

        @Override
        public DOIPreferences getDOIPreferences() {
            return null;
        }

        @Override
        public OwnerPreferences getOwnerPreferences() {
            return null;
        }

        @Override
        public TimestampPreferences getTimestampPreferences() {
            return null;
        }

        @Override
        public RemotePreferences getRemotePreferences() {
            return null;
        }

        @Override
        public ProxyPreferences getProxyPreferences() {
            return null;
        }

        @Override
        public SSLPreferences getSSLPreferences() {
            return null;
        }

        @Override
        public CitationKeyPatternPreferences getCitationKeyPatternPreferences() {
            return null;
        }

        @Override
        public AutoLinkPreferences getAutoLinkPreferences() {
            return null;
        }

        @Override
        public ExportPreferences getExportPreferences() {
            return null;
        }

        @Override
        public ImporterPreferences getImporterPreferences() {
            return null;
        }

        @Override
        public GrobidPreferences getGrobidPreferences() {
            return null;
        }

        @Override
        public XmpPreferences getXmpPreferences() {
            return null;
        }

        @Override
        public NameFormatterPreferences getNameFormatterPreferences() {
            return null;
        }

        @Override
        public SearchPreferences getSearchPreferences() {
            return null;
        }

        @Override
        public MrDlibPreferences getMrDlibPreferences() {
            return null;
        }

        @Override
        public ProtectedTermsPreferences getProtectedTermsPreferences() {
            return null;
        }

        @Override
        public AiPreferences getAiPreferences() {
            return null;
        }

        @Override
        public LastFilesOpenedPreferences getLastFilesOpenedPreferences() {
            return null;
        }

        @Override
        public OpenOfficePreferences getOpenOfficePreferences(JournalAbbreviationRepository journalAbbreviationRepository) {
            return null;
        }

        @Override
        public PushToApplicationPreferences getPushToApplicationPreferences() {
            return null;
        }

        @Override
        public GitPreferences getGitPreferences() {
            return null;
        }
    };

    @Test
    void checkSimpleLibrary(@TempDir Path tempDir) throws IOException {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PUBLISHER, "publisher");
        BibDatabase database = new BibDatabase();
        database.insertEntry(first);
        database.insertEntry(second);

        BibDatabaseContext bibContext = new BibDatabaseContext(database);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck(cliPreferences, bibEntryTypesManager).check(bibContext, (count, total) -> { });

        Path csvFile = tempDir.resolve("checkSimpleLibrary-result.csv");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }
        assertEquals("""
                entry type,citation key,Pages,Publisher
                Article,first,o,-
                Article,second,-,?
                """, Files.readString(csvFile).replace("\r\n", "\n"));
    }

    @Test
    void checkDifferentOutputSymbols(@TempDir Path tempDir) throws IOException {
        UnknownField customField = new UnknownField("custom");
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One") // required
                .withField(StandardField.TITLE, "Title") // required
                .withField(StandardField.PAGES, "some pages") // optional
                .withField(customField, "custom"); // unknown
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One");
        BibDatabase database = new BibDatabase();
        database.insertEntry(first);
        database.insertEntry(second);

        BibDatabaseContext bibContext = new BibDatabaseContext(database);
        bibContext.setMode(BibDatabaseMode.BIBTEX);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck(cliPreferences, bibEntryTypesManager).check(bibContext, (count, total) -> { });

        Path csvFile = tempDir.resolve("checkDifferentOutputSymbols-result.csv");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }

        assertEquals("""
                entry type,citation key,Custom,Pages,Title
                Article,first,?,o,x
                Article,second,-,-,-
                """, Files.readString(csvFile).replace("\r\n", "\n"));
    }

    @Test
    void checkComplexLibrary(@TempDir Path tempDir) throws IOException {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PUBLISHER, "publisher");

        BibEntry third = new BibEntry(StandardEntryType.InProceedings, "third")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.LOCATION, "location")
                .withField(StandardField.YEAR, "2024")
                .withField(StandardField.PAGES, "some pages");
        BibEntry fourth = new BibEntry(StandardEntryType.InProceedings, "fourth")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.YEAR, "2024")
                .withField(StandardField.PUBLISHER, "publisher");
        BibEntry fifth = new BibEntry(StandardEntryType.InProceedings, "fifth")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.YEAR, "2024");

        BibDatabase database = new BibDatabase();
        database.insertEntry(first);
        database.insertEntry(second);
        database.insertEntry(third);
        database.insertEntry(fourth);
        database.insertEntry(fifth);

        BibDatabaseContext bibContext = new BibDatabaseContext(database);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck(cliPreferences, bibEntryTypesManager).check(bibContext, (count, total) -> { });

        Path csvFile = tempDir.resolve("checkSimpleLibrary-result.csv");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }
        assertEquals("""
                entry type,citation key,Location,Pages,Publisher
                Article,first,-,o,-
                Article,second,-,-,?
                InProceedings,fifth,-,-,-
                InProceedings,fourth,-,-,o
                InProceedings,third,?,o,-
                """, Files.readString(csvFile).replace("\r\n", "\n"));
    }

    @Test
    void checkLibraryWithoutIssues(@TempDir Path tempDir) throws IOException {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibDatabase database = new BibDatabase();
        database.insertEntry(first);
        database.insertEntry(second);

        BibDatabaseContext bibContext = new BibDatabaseContext(database);
        bibContext.setMode(BibDatabaseMode.BIBTEX);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck(cliPreferences, bibEntryTypesManager).check(bibContext, (count, total) -> { });

        Path csvFile = tempDir.resolve("checkLibraryWithoutIssues-result.csv");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }
        assertEquals("""
                entry type,citation key
                """, Files.readString(csvFile).replace("\r\n", "\n"));
    }

    @Test
    @Disabled("This test is only for manual generation of a report")
    void checkManualInput() throws IOException {
        Path file = Path.of("C:\\TEMP\\JabRef\\biblio-anon.bib");
        Path csvFile = file.resolveSibling("biblio-cited.csv");
        BibDatabaseContext databaseContext = importer.importDatabase(file).getDatabaseContext();
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck(cliPreferences, bibEntryTypesManager).check(databaseContext, (_, _) -> { });
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }
    }
}
