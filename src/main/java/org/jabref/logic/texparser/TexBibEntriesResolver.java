package org.jabref.logic.texparser;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.texparser.TexBibEntriesResolverResult;
import org.jabref.model.texparser.TexParserResult;
import org.jabref.model.util.FileUpdateMonitor;

public class TexBibEntriesResolver {

    private final BibDatabase masterDatabase;
    private final ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileMonitor;

    public TexBibEntriesResolver(BibDatabase masterDatabase, ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileMonitor) {
        this.masterDatabase = masterDatabase;
        this.importFormatPreferences = importFormatPreferences;
        this.fileMonitor = fileMonitor;
    }

    /**
     * Resolve all BibTeX entries and check if they are in the given database.
     */
    public TexBibEntriesResolverResult resolve(TexParserResult texParserResult) {
        TexBibEntriesResolverResult resolverResult = new TexBibEntriesResolverResult(texParserResult);

        // Preload databases from BIB files.
        Map<Path, BibDatabase> bibDatabases = resolverResult.getBibFiles().values().stream().distinct().collect(Collectors.toMap(
                Function.identity(), path -> OpenDatabase.loadDatabase(path.toString(), importFormatPreferences, fileMonitor).getDatabase()));

        // Add all new entries to the newEntries set.
        resolverResult.getNewEntries().addAll(
                // Get all pairs Entry<String entryKey, Citation>.
                texParserResult.getCitations().entries().stream().distinct().flatMap(mapEntry ->
                        // Load the associated BIB file.
                        texParserResult.getBibFiles().get(mapEntry.getValue().getPath()).stream().distinct().flatMap(bibFile ->
                                // Get a specific entry from an entryKey and a BIB file.
                                bibDatabases.get(bibFile).getEntriesByKey(mapEntry.getKey()).stream().distinct()
                                            // Check if there is already an entry with the same key in the given database.
                                            .filter(entry -> !entry.equals(masterDatabase.getEntryByKey(entry.getCiteKeyOptional().orElse("")).orElse(new BibEntry())))
                                            // Add cross-referencing data to the entry (fill empty fields).
                                            .map(entry -> {
                                                bibDatabases.get(bibFile).getReferencedEntry(entry).ifPresent(refEntry ->
                                                        refEntry.getFields().forEach(field -> entry.getFieldMap().putIfAbsent(field, refEntry.getFieldOrAlias(field).orElse(""))));
                                                return entry;
                                            })))
                               .collect(Collectors.toSet()));

        return resolverResult;
    }
}
