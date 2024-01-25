package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class JournalAbbreviationDirectorySynchronizer implements JournalAbbreviationDirectoryChangeListener {

    private JournalAbbreviationPreferences preferences;

    private final Map<String, Consumer<Path>> journalExternalFilesActions = new HashMap<>();

    public JournalAbbreviationDirectorySynchronizer(JournalAbbreviationPreferences preferences) {
        init(preferences);
    }

    public void init(JournalAbbreviationPreferences preferences) {
        this.preferences = preferences;
        initialSync();
        initJournalExternalFilesActionsMap();
    }

    private void initialSync() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(preferences.getJournalAbbreviationsDirectory().getValue())) {
            List<String> filesPresentInDirectory = new ArrayList<>();
            stream.iterator().forEachRemaining((path -> {
                if (!Files.isDirectory(path) && JournalAbbreviationsUtils.isCSVFile(path)) {
                    filesPresentInDirectory.add(path.toString());
                }
            }));
            List<String> savedExternalFiles = preferences.getExternalJournalLists().stream().filter(
                    file -> file.startsWith(preferences.getJournalAbbreviationsDirectory().getValue().toString())).toList();
            preferences.getExternalJournalLists().removeAll(savedExternalFiles);
            preferences.getExternalJournalLists().addAll(filesPresentInDirectory);
            for (String path : preferences.getExternalJournalLists()) {
                JournalAbbreviationStorer.store(Path.of(path));
            }
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initJournalExternalFilesActionsMap() {
        journalExternalFilesActions.put(StandardWatchEventKinds.ENTRY_CREATE.name(), path -> preferences.getExternalJournalLists().add(String.valueOf(
                preferences.getJournalAbbreviationsDirectory().getValue().resolve(path))));
        journalExternalFilesActions.put(StandardWatchEventKinds.ENTRY_DELETE.name(), path -> preferences.getExternalJournalLists().remove(String.valueOf(
                preferences.getJournalAbbreviationsDirectory().getValue().resolve(path))));
    }

    @Override
    public void onJournalAbbreviationDirectoryChangeListener(Path dir, WatchEvent<Path> event) {
        Consumer<Path> journalExternalFilesAction = journalExternalFilesActions.get(event.kind().name());
        if (journalExternalFilesAction != null) {
            journalExternalFilesAction.accept(dir.resolve(event.context()));
        }
    }
}
