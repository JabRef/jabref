package org.jabref.gui.collab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.swing.SwingUtilities;

import org.jabref.JabRefExecutorService;
import org.jabref.gui.BasePanel;
import org.jabref.gui.SidePaneManager;
import org.jabref.logic.util.io.FileBasedLock;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseChangeMonitor implements FileUpdateListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseChangeMonitor.class);

    private final BibDatabaseContext database;
    private final FileUpdateMonitor fileMonitor;
    private final BasePanel panel;
    private boolean updatedExternally;
    private Path tmpFile;
    private long timeStamp;
    private long fileSize;

    public DatabaseChangeMonitor(BibDatabaseContext database, FileUpdateMonitor fileMonitor, BasePanel panel) {
        this.database = database;
        this.fileMonitor = fileMonitor;
        this.panel = panel;

        this.database.getDatabasePath().ifPresent(path -> {
            try {
                fileMonitor.addListenerForFile(path, this);
                timeStamp = Files.getLastModifiedTime(path).toMillis();
                fileSize = Files.size(path);
                tmpFile = Files.createTempFile("jabref", ".bib");
                tmpFile.toFile().deleteOnExit();
                copyToTemp(path);
            } catch (IOException e) {
                LOGGER.error("Error while trying to monitor " + path, e);
            }
        });
    }

    @Override
    public void fileUpdated() {
        if (panel.isSaving()) {
            // We are just saving the file, so this message is most likely due to bad timing.
            // If not, we'll handle it on the next polling.
            return;
        }

        updatedExternally = true;

        final ChangeScanner scanner = new ChangeScanner(panel.frame(), panel, database.getDatabaseFile().orElse(null), tmpFile);

        // Test: running scan automatically in background
        if (database.getDatabasePath().isPresent() && !FileBasedLock.waitForFileLock(database.getDatabasePath().get())) {
            // The file is locked even after the maximum wait. Do nothing.
            LOGGER.error("File updated externally, but change scan failed because the file is locked.");

            // Wait a bit and then try again
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Nothing to do
            }
            fileUpdated();
            return;
        }

        JabRefExecutorService.INSTANCE.executeInterruptableTaskAndWait(scanner);

        // Adding the sidepane component is Swing work, so we must do this in the Swing
        // thread:
        Runnable t = () -> {

            // Check if there is already a notification about external
            // changes:
            SidePaneManager sidePaneManager = panel.getSidePaneManager();
            boolean hasAlready = sidePaneManager.hasComponent(FileUpdatePanel.class);
            if (hasAlready) {
                sidePaneManager.hideComponent(FileUpdatePanel.class);
                sidePaneManager.unregisterComponent(FileUpdatePanel.class);
            }
            FileUpdatePanel pan = new FileUpdatePanel(panel, sidePaneManager,
                    database.getDatabaseFile().orElse(null), scanner);
            sidePaneManager.register(pan);
            sidePaneManager.show(FileUpdatePanel.class);
        };

        if (scanner.changesFound()) {
            SwingUtilities.invokeLater(t);
        } else {
            updatedExternally = false;
        }
    }

    /**
     * Forces a check on the file, and returns the result. Check if time stamp or the file size has changed.
     *
     * @return boolean true if the file has changed.
     */
    private boolean hasBeenModified() {
        Optional<Path> file = database.getDatabasePath();
        if (file.isPresent()) {
            try {
                long modified = Files.getLastModifiedTime(file.get()).toMillis();
                if (modified == 0L) {
                    // File deleted
                    return false;
                }
                long fileSizeNow = Files.size(file.get());
                return (timeStamp != modified) || (fileSize != fileSizeNow);
            } catch (IOException ex) {
                return false;
            }
        }
        return false;
    }

    public void unregister() {
        database.getDatabasePath().ifPresent(file -> fileMonitor.removeListener(file, this));
    }

    public boolean hasBeenModifiedExternally() {
        return updatedExternally || hasBeenModified();
    }

    public void markExternalChangesAsResolved() {
        updatedExternally = false;
        markAsSaved();
    }

    public void markAsSaved() {
        database.getDatabasePath().ifPresent(file -> {
            try {
                timeStamp = Files.getLastModifiedTime(file).toMillis();
                fileSize = Files.size(file);

                copyToTemp(file);
            } catch (IOException ex) {
                LOGGER.error("Error while getting file information", ex);
            }
        });
    }

    private void copyToTemp(Path file) {
        FileUtil.copyFile(file, tmpFile, true);
    }

    public Path getTempFile() {
        return tmpFile;
    }
}
