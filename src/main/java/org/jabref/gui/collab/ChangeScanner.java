package org.jabref.gui.collab;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.logic.bibtex.comparator.BibStringDiff;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.FileSaveSession;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.SaveSession;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.metadata.MetaData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeScanner implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeScanner.class);

    private final File file;
    private final Path tempFile;
    private final BibDatabaseContext databaseInMemory;
    private final MetaData metadataInMemory;

    private final BasePanel panel;
    private final JabRefFrame frame;
    private BibDatabaseContext databaseInTemp;

    /**
     * We create an ArrayList to hold the changes we find. These will be added in the form
     * of UndoEdit objects. We instantiate these so that the changes found in the file on disk
     * can be reproduced in memory by calling redo() on them. REDO, not UNDO!
     */
    private final DefaultMutableTreeNode changes = new DefaultMutableTreeNode(Localization.lang("External changes"));

    //  NamedCompound edit = new NamedCompound("Merged external changes")

    public ChangeScanner(JabRefFrame frame, BasePanel bp, File file, Path tempFile) {
        this.panel = bp;
        this.frame = frame;
        this.databaseInMemory = bp.getBibDatabaseContext();
        this.metadataInMemory = bp.getBibDatabaseContext().getMetaData();
        this.file = file;
        this.tempFile = tempFile;
    }

    public boolean changesFound() {
        return changes.getChildCount() > 0;
    }

    /**
     * Finds the entry in the list best fitting the specified entry. Even if no entries get a score above zero, an entry
     * is still returned.
     */
    private static BibEntry bestFit(BibEntry targetEntry, List<BibEntry> entries) {
        return entries.stream()
                .max(Comparator.comparingDouble(candidate -> DuplicateCheck.compareEntriesStrictly(targetEntry, candidate)))
                .orElse(null);
    }

    public void displayResult(final DisplayResultCallback fup) {
        if (changes.getChildCount() > 0) {
            SwingUtilities.invokeLater(() -> {
                ChangeDisplayDialog changeDialog = new ChangeDisplayDialog(frame, panel, databaseInTemp.getDatabase(), changes);
                changeDialog.setLocationRelativeTo(frame);
                changeDialog.setVisible(true);
                fup.scanResultsResolved(changeDialog.isOkPressed());
                if (changeDialog.isOkPressed()) {
                    // Overwrite the temp database:
                    storeTempDatabase();
                }
            });
        } else {
            JOptionPane.showMessageDialog(frame, Localization.lang("No actual changes found."),
                    Localization.lang("External changes"), JOptionPane.INFORMATION_MESSAGE);
            fup.scanResultsResolved(true);
        }
    }

    private void storeTempDatabase() {
        JabRefExecutorService.INSTANCE.execute(() -> {
            try {
                SavePreferences prefs = Globals.prefs.loadForSaveFromPreferences().withMakeBackup(false)
                        .withEncoding(panel.getBibDatabaseContext().getMetaData().getEncoding()
                                .orElse(Globals.prefs.getDefaultEncoding()));

                BibDatabaseWriter<SaveSession> databaseWriter = new BibtexDatabaseWriter<>(FileSaveSession::new);
                SaveSession ss = databaseWriter.saveDatabase(databaseInTemp, prefs);
                ss.commit(tempFile);
            } catch (SaveException ex) {
                LOGGER.warn("Problem updating tmp file after accepting external changes", ex);
            }
        });
    }

    @Override
    public void run() {
        try {

            // Parse the temporary file.
            ImportFormatPreferences importFormatPreferences = Globals.prefs.getImportFormatPreferences();
            ParserResult result = OpenDatabase.loadDatabase(tempFile.toFile(), importFormatPreferences, Globals.getFileUpdateMonitor());
            databaseInTemp = result.getDatabaseContext();

            // Parse the modified file.
            result = OpenDatabase.loadDatabase(file, importFormatPreferences, Globals.getFileUpdateMonitor());
            BibDatabaseContext databaseOnDisk = result.getDatabaseContext();

            // Start looking at changes.
            BibDatabaseDiff differences = BibDatabaseDiff.compare(databaseInTemp, databaseOnDisk);
            differences.getMetaDataDifferences().ifPresent(diff -> {
                changes.add(new MetaDataChangeViewModel(metadataInMemory, diff));
                diff.getGroupDifferences().ifPresent(groupDiff -> changes.add(new GroupChangeViewModel(groupDiff)));
            });
            differences.getPreambleDifferences().ifPresent(diff -> changes.add(new PreambleChangeViewModel(databaseInMemory.getDatabase().getPreamble().orElse(""), diff)));
            differences.getBibStringDifferences().forEach(diff -> changes.add(createBibStringDiff(diff)));
            differences.getEntryDifferences().forEach(diff -> changes.add(createBibEntryDiff(diff)));
        } catch (IOException ex) {
            LOGGER.warn("Problem running", ex);
        }
    }

    private ChangeViewModel createBibStringDiff(BibStringDiff diff) {
        if (diff.getOriginalString() == null) {
            return new StringAddChangeViewModel(diff.getNewString());
        }

        if (diff.getNewString() == null) {
            Optional<BibtexString> current = databaseInMemory.getDatabase().getStringByName(diff.getOriginalString().getName());
            return new StringRemoveChangeViewModel(diff.getOriginalString(), current.orElse(null));
        }

        if (diff.getOriginalString().getName().equals(diff.getNewString().getName())) {
            Optional<BibtexString> current = databaseInMemory.getDatabase().getStringByName(diff.getOriginalString().getName());
            return new StringChangeViewModel(current.orElse(null), diff.getOriginalString(), diff.getNewString().getContent());
        }

        Optional<BibtexString> current = databaseInMemory.getDatabase().getStringByName(diff.getOriginalString().getName());
        return new StringNameChangeViewModel(current.orElse(null), diff.getOriginalString(), current.map(BibtexString::getName).orElse(""), diff.getNewString().getName());
    }

    private ChangeViewModel createBibEntryDiff(BibEntryDiff diff) {
        if (diff.getOriginalEntry() == null) {
            return new EntryAddChangeViewModel(diff.getNewEntry());
        }

        if (diff.getNewEntry() == null) {
            return new EntryDeleteChangeViewModel(bestFit(diff.getOriginalEntry(), databaseInMemory.getEntries()), diff.getOriginalEntry());
        }

        return new EntryChangeViewModel(bestFit(diff.getOriginalEntry(), databaseInMemory.getEntries()), diff.getOriginalEntry(), diff.getNewEntry());
    }

    @FunctionalInterface
    public interface DisplayResultCallback {
        void scanResultsResolved(boolean resolved);
    }
}
