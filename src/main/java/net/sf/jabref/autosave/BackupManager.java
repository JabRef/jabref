package net.sf.jabref.autosave;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.sf.jabref.Globals;
import net.sf.jabref.event.BibDatabaseContextChangedEvent;
import net.sf.jabref.logic.exporter.BibtexDatabaseWriter;
import net.sf.jabref.logic.exporter.FileSaveSession;
import net.sf.jabref.logic.exporter.SaveException;
import net.sf.jabref.logic.exporter.SavePreferences;
import net.sf.jabref.model.database.BibDatabaseContext;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Backups the given bib database file from {@link BibDatabaseContext} on every {@link BibDatabaseContextChangedEvent}.
 * An intelligent {@link ExecutorService} with a {@link BlockingQueue} prevents a high load while making backups and rejects all redundant backup tasks.
 */
public class BackupManager {

    private static final Log LOGGER = LogFactory.getLog(BackupManager.class);

    private static final String BACKUP_FILENAME_ENDING = ".swp";

    private static Set<BackupManager> runningInstances = new HashSet<>();

    private final BibDatabaseContext bibDatabaseContext;
    private final BlockingQueue<Runnable> workerQueue;
    private final ExecutorService executor;
    private final Charset charset;

    private Path originalPath;
    private Path backupPath;


    private BackupManager(BibDatabaseContext bibDatabaseContext) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.workerQueue = new ArrayBlockingQueue<>(1);
        this.executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, workerQueue);
        this.charset = bibDatabaseContext.getMetaData().getEncoding().orElse(Globals.prefs.getDefaultEncoding());
    }


    private final Runnable backupTask = new Runnable() {
        @Override
        public void run() {
            try {
                SavePreferences prefs = SavePreferences.loadForSaveFromPreferences(Globals.prefs).withEncoding(charset).withMakeBackup(false);
                new BibtexDatabaseWriter<>(FileSaveSession::new).saveDatabase(bibDatabaseContext, prefs).commit(backupPath);
            } catch (SaveException e) {
                LOGGER.error("Error while saving file.", e);
            }
        }
    };

    @Subscribe
    public synchronized void listen(@SuppressWarnings("unused") BibDatabaseContextChangedEvent event) {
        try {
            executor.submit(backupTask);
        } catch (RejectedExecutionException e) {
            LOGGER.debug("Rejecting while another backup process is already running.");
        }
    }

    /**
     * Unregisters the BackupManager from the eventBus of {@link BibDatabaseContext} and deletes the backup file.
     * This method should only be used when closing a database/JabRef legally.
     *
     * @param bibDatabaseContext Associated {@link BibDatabaseContext}
     */
    private void shutdown() {
        bibDatabaseContext.getDatabase().unregisterListener(this);
        bibDatabaseContext.getMetaData().unregisterListener(this);
        executor.shutdown();
        try {
            Files.delete(backupPath);
        } catch (IOException e) {
            LOGGER.error("Error while deleting the backup file.", e);
        }
    }

    /**
     * Starts the BackupManager which is associated with the given {@link BibDatabaseContext}.
     * If no database file is present in {@link BibDatabaseContext}, {@link BackupManager} will do nothing.
     *
     * @param bibDatabaseContext Associated {@link BibDatabaseContext}
     */
    public static BackupManager start(BibDatabaseContext bibDatabaseContext) {
        BackupManager backupManager = new BackupManager(bibDatabaseContext);

        Optional<File> originalFile = bibDatabaseContext.getDatabaseFile();

        if (originalFile.isPresent()) {
            backupManager.originalPath = originalFile.get().toPath();
            backupManager.backupPath = Paths.get(backupManager.originalPath.toString() + BACKUP_FILENAME_ENDING);
            bibDatabaseContext.getDatabase().registerListener(backupManager);
            bibDatabaseContext.getMetaData().registerListener(backupManager);
            runningInstances.add(backupManager);
        }

        return backupManager;
    }

    /**
     * Shuts down the BackupManager which is associated with the given {@link BibDatabaseContext}
     *
     * @param bibDatabaseContext Associated {@link BibDatabaseContext}
     */
    public static void shutdown(BibDatabaseContext bibDatabaseContext) {
        for (BackupManager backupManager : runningInstances) {
            if (backupManager.bibDatabaseContext == bibDatabaseContext) {
                backupManager.shutdown();
                runningInstances.remove(backupManager);
            }
        }
    }

    /**
     * Checks whether a backup file exists for the given database file.
     *
     * @param originalPath Path to the file a backup should be checked for.
     */
    public static boolean checkForBackupFile(Path originalPath) {
        Path backupPath = Paths.get(originalPath.toString() + BACKUP_FILENAME_ENDING);
        return Files.exists(backupPath) && !Files.isDirectory(backupPath);
    }

    /**
     * Restores the backup file by copying and overwriting the original one.
     *
     * @param originalPath Path to the file which should be equalized to the backup file.
     */
    public static void restoreBackup(Path originalPath) {
        Path backupPath = Paths.get(originalPath.toString() + BACKUP_FILENAME_ENDING);
        try {
            Files.copy(backupPath, originalPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Error while restoring the backup file.", e);
        }
    }
}
