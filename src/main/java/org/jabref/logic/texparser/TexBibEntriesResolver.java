package org.jabref.logic.texparser;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.texparser.TexBibEntriesResolverResult;
import org.jabref.model.texparser.TexParserResult;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TexBibEntriesResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(TexBibEntriesResolver.class);
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

        // Get all pairs Entry<String entryKey, Citation>.
        texParserResult.getCitations().entries().forEach(mapEntry ->
                // Load the associated BIB file.
                texParserResult.getBibFiles().get(mapEntry.getValue().getPath()).forEach(bibFile ->
                        // Get a specific entry from an entryKey.
                        bibDatabases.get(bibFile).getEntriesByKey(mapEntry.getKey()).forEach(entry -> {
                            Optional<BibEntry> databaseEntry = masterDatabase.getEntryByKey(entry.getCiteKeyOptional().orElse(""));
                            // Check if there is already an entry with the same key in the given database.
                            if (!databaseEntry.isPresent() || !databaseEntry.get().equals(entry)) {
                                // Add cross-referencing data to the entry (fill empty fields).
                                bibDatabases.get(bibFile).getReferencedEntry(entry).ifPresent(refEntry ->
                                        refEntry.getFields().forEach(field -> entry.getFieldMap().putIfAbsent(field, refEntry.getFieldOrAlias(field).orElse(""))));
                                resolverResult.addEntry(entry);
                            }
                        })));

        return resolverResult;
    }
}
