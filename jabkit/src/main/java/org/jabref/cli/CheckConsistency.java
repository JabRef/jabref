package org.jabref.cli;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.prefs.BackingStoreException;

import org.jabref.cli.converter.CygWinPathConverter;
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
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fetcher.MrDlibPreferences;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
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
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultCsvWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultTxtWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultWriter;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.BibEntryTypesManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "check-consistency", description = "Check consistency of the library.")
class CheckConsistency implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckConsistency.class);
    private static final BibEntryTypesManager bibEntryTypesManager = new BibEntryTypesManager();
    private static final CliPreferences cliPreferences = new CliPreferences() {
        @Override
        public void clear() throws BackingStoreException {

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

        @Override
        public BibEntryTypesManager getCustomEntryTypesRepository(BibEntryTypesManager bibEntryTypesManager) {
            return bibEntryTypesManager;
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

    @ParentCommand
    private ArgumentProcessor argumentProcessor;

    @Mixin
    private ArgumentProcessor.SharedOptions sharedOptions = new ArgumentProcessor.SharedOptions();

    @Option(names = {"--input"}, converter = CygWinPathConverter.class, description = "Input BibTeX file", required = true)
    private Path inputFile;

    @Option(names = {"--output-format"}, description = "Output format: txt or csv", defaultValue = "txt")
    private String outputFormat;

    @Override
    public Integer call() {
        Optional<ParserResult> parserResult = ArgumentProcessor.importFile(
                inputFile,
                "bibtex",
                argumentProcessor.cliPreferences,
                sharedOptions.porcelain);
        if (parserResult.isEmpty()) {
            System.out.println(Localization.lang("Unable to open file '%0'.", inputFile));
            return 2;
        }

        if (parserResult.get().isInvalid()) {
            System.out.println(Localization.lang("Input file '%0' is invalid and could not be parsed.", inputFile));
            return 2;
        }

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Checking consistency of '%0'.", inputFile));
            System.out.flush();
        }

        BibDatabaseContext databaseContext = parserResult.get().getDatabaseContext();

        BibliographyConsistencyCheck consistencyCheck = new BibliographyConsistencyCheck(cliPreferences, bibEntryTypesManager);
        BibliographyConsistencyCheck.Result result = consistencyCheck.check(databaseContext, (count, total) -> {
            if (!sharedOptions.porcelain) {
                System.out.println(Localization.lang("Checking consistency for entry type %0 of %1", count + 1, total));
            }
        });

        return writeCheckResult(result, databaseContext);
    }

    private int writeCheckResult(BibliographyConsistencyCheck.Result result, BibDatabaseContext databaseContext) {
        Writer writer = new OutputStreamWriter(System.out);
        BibliographyConsistencyCheckResultWriter checkResultWriter;

        if ("txt".equalsIgnoreCase(outputFormat)) {
            checkResultWriter = new BibliographyConsistencyCheckResultTxtWriter(
                    result,
                    writer,
                    sharedOptions.porcelain,
                    argumentProcessor.entryTypesManager,
                    databaseContext.getMode());
        } else {
            checkResultWriter = new BibliographyConsistencyCheckResultCsvWriter(
                    result,
                    writer,
                    sharedOptions.porcelain,
                    argumentProcessor.entryTypesManager,
                    databaseContext.getMode());
        }

        // System.out should not be closed, therefore no try-with-resources
        try {
            checkResultWriter.writeFindings();
            writer.flush();
        } catch (IOException e) {
            LOGGER.error("Error writing results", e);
            return 2;
        }

        if (!result.entryTypeToResultMap().isEmpty()) {
            return 1;
        }

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Consistency check completed"));
        }
        return 0;
    }
}
