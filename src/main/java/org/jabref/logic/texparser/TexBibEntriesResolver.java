package org.jabref.logic.texparser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.texparser.Citation;
import org.jabref.model.texparser.LatexBibEntriesResolverResult;
import org.jabref.model.texparser.LatexParserResult;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.GeneralPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TexBibEntriesResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(TexBibEntriesResolver.class);

    private final BibDatabase masterDatabase;
    private final GeneralPreferences generalPreferences;
    private final ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileMonitor;

    public TexBibEntriesResolver(BibDatabase masterDatabase, GeneralPreferences generalPreferences, ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileMonitor) {
        this.masterDatabase = masterDatabase;
        this.generalPreferences = generalPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.fileMonitor = fileMonitor;
    }

    /**
     * Resolve all BibTeX entries and check if they are in the given database.
     */
    public LatexBibEntriesResolverResult resolve(LatexParserResult latexParserResult) {
        LatexBibEntriesResolverResult resolverResult = new LatexBibEntriesResolverResult(latexParserResult);

        // Preload databases from BIB files.
        Map<Path, BibDatabase> bibDatabases = resolverResult.getBibFiles().values().stream().distinct().collect(Collectors.toMap(
                Function.identity(), path -> {
                    try {
                        return OpenDatabase.loadDatabase(path, generalPreferences, importFormatPreferences, fileMonitor).getDatabase();
                    } catch (IOException e) {
                        LOGGER.error("Error opening file '{}'", path, e);
                        return ParserResult.fromError(e).getDatabase();
                    }
                }));

        // Get all pairs Entry<String entryKey, Citation>.
        Stream<Map.Entry<String, Citation>> citationsStream = latexParserResult.getCitations().entries().stream().distinct();

        Set<BibEntry> newEntries = citationsStream.flatMap(mapEntry -> apply(mapEntry, latexParserResult, bibDatabases)).collect(Collectors.toSet());

        // Add all new entries to the newEntries set.
        resolverResult.getNewEntries().addAll(newEntries);

        return resolverResult;
    }

    private Stream<? extends BibEntry> apply(Map.Entry<String, Citation> mapEntry, LatexParserResult latexParserResult, Map<Path, BibDatabase> bibDatabases) {
        return latexParserResult.getBibFiles().get(mapEntry.getValue().getPath()).stream().distinct().flatMap(bibFile ->
                // Get a specific entry from an entryKey and a BIB file.
                bibDatabases.get(bibFile).getEntriesByCitationKey(mapEntry.getKey()).stream().distinct()
                            // Check if there is already an entry with the same key in the given database.
                            .filter(entry -> !entry.equals(masterDatabase.getEntryByCitationKey(entry.getCitationKey().orElse("")).orElse(new BibEntry())))
                            // Add cross-referencing data to the entry (fill empty fields).
                            .map(entry -> addCrossReferencingData(entry, bibFile, bibDatabases)));
    }

    private BibEntry addCrossReferencingData(BibEntry entry, Path bibFile, Map<Path, BibDatabase> bibDatabases) {
        bibDatabases.get(bibFile).getReferencedEntry(entry).ifPresent(refEntry ->
                refEntry.getFields().forEach(field -> entry.getFieldMap().putIfAbsent(field, refEntry.getFieldOrAlias(field).orElse(""))));

        return entry;
    }
}
