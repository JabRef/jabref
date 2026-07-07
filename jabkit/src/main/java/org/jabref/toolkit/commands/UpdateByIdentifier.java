package org.jabref.toolkit.commands;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.toolkit.exception.CliException;
import org.jabref.toolkit.exception.ExportServiceException;
import org.jabref.toolkit.exception.ImportServiceException;
import org.jabref.toolkit.service.ExportService;
import org.jabref.toolkit.service.ImportService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/// Headless equivalent of the GUI's "update entries with bibliographic information" batch action,
/// but with a simpler merge policy: existing field values are never touched, only fields the
/// entry is missing get filled in from the fetched data (see [BibEntry#mergeWith(BibEntry)]).
@Command(name = "update-by-id",
        description = "Updates entries in a .bib file with data fetched by their DOI/ISBN/ArXiv identifier. "
                + "Existing field values are kept; only missing fields are filled in.")
class UpdateByIdentifier implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateByIdentifier.class);

    private static final List<Field> IDENTIFIER_FIELDS = List.of(StandardField.DOI, StandardField.ISBN, StandardField.EPRINT);

    @CommandLine.ParentCommand
    private JabKit argumentProcessor;

    @CommandLine.Mixin
    private JabKit.SharedOptions sharedOptions;

    @Parameters(paramLabel = "FILE", description = "the .bib file to update")
    private Path inputFile;

    @Option(names = "--output", description = "write the updated library here instead of updating FILE in place")
    private Path outputFile;

    @Option(names = {"-k", "--citation-key"}, description = "restrict the update to these citation keys (default: all entries)", split = ",")
    private List<String> citationKeys = List.of();

    @Override
    public Integer call() throws ImportServiceException, ExportServiceException, CliException {
        ParserResult parserResult = ImportService.importBibTexFile(inputFile, argumentProcessor.cliPreferences, sharedOptions.porcelain);
        BibDatabaseContext databaseContext = parserResult.getDatabaseContext();

        List<BibEntry> entriesToUpdate = selectEntriesToUpdate(databaseContext);

        int consideredEntries = 0;
        int updatedEntries = 0;
        for (BibEntry entry : entriesToUpdate) {
            Optional<BibEntry> fetchedEntry = fetchByFirstIdentifier(entry);
            if (fetchedEntry.isEmpty()) {
                continue;
            }
            consideredEntries++;

            Set<Field> fieldsBefore = Set.copyOf(entry.getFields());
            entry.mergeWith(fetchedEntry.get());
            if (!entry.getFields().equals(fieldsBefore)) {
                updatedEntries++;
            }
        }

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang(
                    "Considered %0 entries with an identifier, updated %1 with new fields.",
                    String.valueOf(consideredEntries),
                    String.valueOf(updatedEntries)));
        }

        ExportService.create(argumentProcessor.cliPreferences, sharedOptions.porcelain)
                     .saveDatabaseContext(databaseContext, outputFile != null ? outputFile : inputFile);

        return CommandLine.ExitCode.OK;
    }

    /// Without `--citation-key`, every entry in the library is a candidate. With it, resolves each
    /// given key via [org.jabref.model.database.BibDatabase#getEntriesByCitationKey(String)] and
    /// fails fast if a key doesn't exist, rather than silently updating a subset.
    private List<BibEntry> selectEntriesToUpdate(BibDatabaseContext databaseContext) throws CliException {
        if (citationKeys.isEmpty()) {
            return databaseContext.getEntries();
        }

        List<BibEntry> entries = new ArrayList<>();
        for (String citationKey : citationKeys) {
            List<BibEntry> matches = databaseContext.getDatabase().getEntriesByCitationKey(citationKey);
            if (matches.isEmpty()) {
                throw new CliException(
                        "No entry found for citation key '" + citationKey + "'",
                        Localization.lang("No entry found for citation key '%0'", citationKey),
                        CommandLine.ExitCode.USAGE);
            }
            entries.addAll(matches);
        }
        return entries;
    }

    /// Tries identifier fields in a fixed priority order (DOI, then ISBN, then ArXiv) and returns
    /// the first successful fetch, mirroring [org.jabref.logic.importer.fetcher.MergingIdBasedFetcher]'s
    /// field priority.
    private Optional<BibEntry> fetchByFirstIdentifier(BibEntry entry) {
        for (Field field : IDENTIFIER_FIELDS) {
            Optional<String> identifier = entry.getField(field);
            if (identifier.isEmpty()) {
                continue;
            }

            Optional<IdBasedFetcher> fetcher = WebFetchers.getIdBasedFetcherForField(
                    field, argumentProcessor.cliPreferences.getImportFormatPreferences());
            if (fetcher.isEmpty()) {
                continue;
            }

            try {
                Optional<BibEntry> fetchedEntry = fetcher.get().performSearchById(identifier.get());
                if (fetchedEntry.isPresent()) {
                    return fetchedEntry;
                }
            } catch (FetcherException e) {
                LOGGER.warn("Could not fetch bibliographic data for entry {} using {} '{}'",
                        entry.getCitationKey().orElse("[no key]"), field, identifier.get(), e);
            }
        }
        return Optional.empty();
    }
}
