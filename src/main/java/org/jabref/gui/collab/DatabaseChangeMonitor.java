package org.jabref.gui.collab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.jabref.Globals;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An update monitor for a file-based library (.bib file). Has to be re-instantiated if the location of the bib file
 * changes.
 */
public class DatabaseChangeMonitor implements FileUpdateListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseChangeMonitor.class);

    private BibDatabaseContext referenceDatabase;
    private Path databaseToBeMonitored;

    private final FileUpdateMonitor fileMonitor;
    private final List<DatabaseChangeListener> listeners = new ArrayList<>();
    private TaskExecutor taskExecutor;

    /**
     * @param databaseToBeMonitored The BibTeX database to be monitored
     * @param fileMonitor           The update monitor where to register to get notified about a change
     * @param taskExecutor          The scan for changes and notification is run with that task executor
     */
    public DatabaseChangeMonitor(BibDatabaseContext databaseToBeMonitored, FileUpdateMonitor fileMonitor, TaskExecutor taskExecutor) {
        this.fileMonitor = fileMonitor;
        this.taskExecutor = taskExecutor;

        databaseToBeMonitored.getDatabasePath().ifPresentOrElse(path -> {
            this.databaseToBeMonitored = path;
            loadReferenceDatabaseFromDatabaseToBeMonitored();
            try {
                // as last step, register this class
                fileMonitor.addListenerForFile(path, this);
            } catch (IOException e) {
                LOGGER.error("Error while trying to monitor " + path, e);
            }
        }, () -> {
            throw new IllegalStateException("Path has to be present");
        });
    }

    @Override
    public void fileUpdated() {
        // File on disk has changed, thus look for notable changes and notify listeners in case there are such changes

        BackgroundTask.wrap(() -> {
            BibDatabaseDiff differences;
            // no two threads may check for changes
            synchronized (referenceDatabase) {
                ParserResult result = OpenDatabase.loadDatabase(this.databaseToBeMonitored.toAbsolutePath().toString(), Globals.prefs.getImportFormatPreferences(), new DummyFileUpdateMonitor());
                BibDatabaseContext databaseOnDisk = result.getDatabaseContext();
                differences = BibDatabaseDiff.compare(this.referenceDatabase, databaseOnDisk);
                this.referenceDatabase = databaseOnDisk;
            }
            return differences;
        }).onSuccess(diff -> {
            listeners.forEach(listener -> listener.databaseChanged(diff));
        }).executeWith(taskExecutor);
    }

    public void addListener(DatabaseChangeListener listener) {
        listeners.add(listener);
    }

    public void unregister() {
        fileMonitor.removeListener(this.databaseToBeMonitored, this);
    }

    private void loadReferenceDatabaseFromDatabaseToBeMonitored() {
        // there is no clone of BibDatabaseContext - we do it "the hard way" und just reload the database
        ImportFormatPreferences importFormatPreferences = Globals.prefs.getImportFormatPreferences();
        ParserResult result = OpenDatabase.loadDatabase(this.databaseToBeMonitored.toAbsolutePath().toString(), importFormatPreferences, new DummyFileUpdateMonitor());
        this.referenceDatabase = result.getDatabaseContext();
    }

    /**
     * Call this if the database to monitor was saved
     */
    public void markAsSaved() {
        loadReferenceDatabaseFromDatabaseToBeMonitored();
    }
}
