package org.jabref.logic.texparser;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.texparser.LatexBibEntriesResolverResult;
import org.jabref.model.texparser.LatexParserResults;
import org.jabref.model.util.DirectoryUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TexBibEntriesResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(TexBibEntriesResolver.class);

    private final BibDatabase masterDatabase;
    private final ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileMonitor;
    private final DirectoryUpdateMonitor directoryUpdateMonitor;

    public TexBibEntriesResolver(BibDatabase masterDatabase, ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileMonitor, DirectoryUpdateMonitor directoryUpdateMonitor) {
        this.masterDatabase = masterDatabase;
        this.importFormatPreferences = importFormatPreferences;
        this.fileMonitor = fileMonitor;
        this.directoryUpdateMonitor = directoryUpdateMonitor;
    }

    /**
     * Resolve all BibTeX entries and check if they are in the given database.
     */
    public LatexBibEntriesResolverResult resolve(LatexParserResults latexParserResults) {
        LatexBibEntriesResolverResult resolverResult = new LatexBibEntriesResolverResult(latexParserResults);

        // Preload databases from BIB files.
        List<BibDatabase> bibDatabases =
                latexParserResults.getBibFiles().stream().map(path -> {
                    try {
                        return OpenDatabase.loadDatabase(path, importFormatPreferences, fileMonitor, directoryUpdateMonitor).getDatabase();
                    } catch (IOException e) {
                        LOGGER.error("Error opening file '{}'", path, e);
                        return ParserResult.fromError(e).getDatabase();
                    }
                }).toList();

        // Add all new entries to the newEntries set.
        List<BibEntry> newEntries = findNewEntries(bibDatabases, latexParserResults.getCitations().keySet());
        resolverResult.getNewEntries().addAll(newEntries);

        return resolverResult;
    }

    private List<BibEntry> findNewEntries(List<BibDatabase> bibDatabases, Set<String> citations) {
        return bibDatabases
                .stream()
                .flatMap(database ->
                        citations.stream()
                                 .flatMap(citation -> database.getEntriesByCitationKey(citation).stream())
                                 // Check if there is already an entry with the same key in the given database.
                                 .filter(entry -> !entry.equals(masterDatabase.getEntryByCitationKey(entry.getCitationKey().orElse("")).orElse(new BibEntry())))
                                 // Add cross-referencing data to the entry (fill empty fields).
                                 .map(entry -> addCrossReferencingData(entry, database)))
                .toList();
    }

    private BibEntry addCrossReferencingData(BibEntry entry, BibDatabase bibDatabase) {
        bibDatabase.getReferencedEntry(entry).ifPresent(refEntry ->
                refEntry.getFields().forEach(field -> entry.getFieldMap().putIfAbsent(field, refEntry.getFieldOrAlias(field).orElse(""))));

        return entry;
    }
}
