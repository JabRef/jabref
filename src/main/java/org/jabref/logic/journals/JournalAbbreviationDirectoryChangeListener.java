package org.jabref.logic.journals;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public interface JournalAbbreviationDirectoryChangeListener {

    void onJournalAbbreviationDirectoryChangeListener(Path dir, WatchEvent<Path> event);
}
