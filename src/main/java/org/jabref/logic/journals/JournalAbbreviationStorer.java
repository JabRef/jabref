package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalAbbreviationStorer implements JournalAbbreviationDirectoryChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationStorer.class);

    public static void store(Path path) throws IOException {
        if (path.getParent() == null) {
            return;
        }
        String mvFileName = path.getFileName().toString().split("\\.")[0] + ".mv";
        Path mvFilePath = Path.of(path.getParent().toString(), mvFileName);
        if (shouldNotStore(mvFilePath, path)) {
            LOGGER.info("Skipping storing to mv file");
            return;
        }
        LOGGER.info("Storing to mv file");
        MVStore store = new MVStore.Builder().fileName(mvFilePath.toString()).compressHigh().open();
        MVMap<String, Abbreviation> fullToAbbreviation = store.openMap("FullToAbbreviation");
        Collection<Abbreviation> abbreviations = JournalAbbreviationLoader.readAbbreviationsFromCsvFile(path);
        Map<String, Abbreviation> abbreviationMap = abbreviations
                .stream()
                .collect(Collectors.toMap(
                        Abbreviation::getName,
                        abbreviation -> abbreviation,
                        (abbreviation1, abbreviation2) -> abbreviation2));
        fullToAbbreviation.putAll(abbreviationMap);
        store.commit();
        store.close();
    }

    private static boolean shouldNotStore(Path mvFilePath, Path csvPath) throws IOException {
        return Files.exists(mvFilePath) &&
                Files.readAttributes(mvFilePath, BasicFileAttributes.class).lastModifiedTime().
                     compareTo(Files.readAttributes(csvPath, BasicFileAttributes.class).lastModifiedTime()) > 0;
    }

    @Override
    public void onJournalAbbreviationDirectoryChangeListener(Path dir, WatchEvent<Path> event) {
        if (event.kind().name().equals(StandardWatchEventKinds.ENTRY_CREATE.name()) || event.kind().name().equals(StandardWatchEventKinds.ENTRY_MODIFY.name())) {
            try {
                store(dir.resolve(event.context()));
            } catch (
                    IOException e) {
                LOGGER.error("Error while storing mv file", e);
            }
        }
    }
}
