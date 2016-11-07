package net.sf.jabref.collab;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.bibtex.comparator.EntryComparator;
import net.sf.jabref.logic.exporter.BibDatabaseWriter;
import net.sf.jabref.logic.exporter.BibtexDatabaseWriter;
import net.sf.jabref.logic.exporter.FileSaveSession;
import net.sf.jabref.logic.exporter.SaveException;
import net.sf.jabref.logic.exporter.SavePreferences;
import net.sf.jabref.logic.exporter.SaveSession;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.OpenDatabase;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.Defaults;
import net.sf.jabref.model.DuplicateCheck;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.database.EntrySorter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.groups.GroupTreeNode;
import net.sf.jabref.model.metadata.MetaData;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ChangeScanner implements Runnable {
    private static final Log LOGGER = LogFactory.getLog(ChangeScanner.class);

    private static final String[] SORT_BY = new String[] {FieldName.YEAR, FieldName.AUTHOR, FieldName.TITLE};

    private final File file;
    private final BibDatabase databaseInMemory;
    private final MetaData metadataInMemory;
    private final BasePanel panel;

    private final JabRefFrame frame;
    private BibDatabase databaseInTemp;
    private MetaData metadataInTemp;

    private static final double MATCH_THRESHOLD = 0.4;

    /**
     * We create an ArrayList to hold the changes we find. These will be added in the form
     * of UndoEdit objects. We instantiate these so that the changes found in the file on disk
     * can be reproduced in memory by calling redo() on them. REDO, not UNDO!
     */
    private final DefaultMutableTreeNode changes = new DefaultMutableTreeNode(Localization.lang("External changes"));

    //  NamedCompound edit = new NamedCompound("Merged external changes")

    public ChangeScanner(JabRefFrame frame, BasePanel bp, File file) {
        this.panel = bp;
        this.frame = frame;
        this.databaseInMemory = bp.getDatabase();
        this.metadataInMemory = bp.getBibDatabaseContext().getMetaData();
        this.file = file;
    }

    @Override
    public void run() {
        try {

            // Parse the temporary file.
            Path tempFile = Globals.getFileUpdateMonitor().getTempFile(panel.fileMonitorHandle());
            ImportFormatPreferences importFormatPreferences = Globals.prefs.getImportFormatPreferences();
            ParserResult result = OpenDatabase.loadDatabase(tempFile.toFile(), importFormatPreferences);
            databaseInTemp = result.getDatabase();
            metadataInTemp = result.getMetaData();

            // Parse the modified file.
            result = OpenDatabase.loadDatabase(file, importFormatPreferences);
            BibDatabase databaseOnDisk = result.getDatabase();
            MetaData metadataOnDisk = result.getMetaData();

            // Sort both databases according to a common sort key.
            EntryComparator comparator = new EntryComparator(false, true, SORT_BY[2]);
            comparator = new EntryComparator(false, true, SORT_BY[1], comparator);
            comparator = new EntryComparator(false, true, SORT_BY[0], comparator);
            EntrySorter sorterInTemp = databaseInTemp.getSorter(comparator);
            comparator = new EntryComparator(false, true, SORT_BY[2]);
            comparator = new EntryComparator(false, true, SORT_BY[1], comparator);
            comparator = new EntryComparator(false, true, SORT_BY[0], comparator);
            EntrySorter sorterOnDisk = databaseOnDisk.getSorter(comparator);
            comparator = new EntryComparator(false, true, SORT_BY[2]);
            comparator = new EntryComparator(false, true, SORT_BY[1], comparator);
            comparator = new EntryComparator(false, true, SORT_BY[0], comparator);
            EntrySorter sorterInMem = databaseInMemory.getSorter(comparator);

            // Start looking at changes.
            scanMetaData(metadataInMemory, metadataInTemp, metadataOnDisk);
            scanPreamble(databaseInMemory, databaseInTemp, databaseOnDisk);
            scanStrings(databaseInMemory, databaseInTemp, databaseOnDisk);

            scanEntries(sorterInMem, sorterInTemp, sorterOnDisk);

            scanGroups(metadataInTemp, metadataOnDisk);

        } catch (IOException ex) {
            LOGGER.warn("Problem running", ex);
        }
    }

    public boolean changesFound() {
        return changes.getChildCount() > 0;
    }

    public void displayResult(final DisplayResultCallback fup) {
        if (changes.getChildCount() > 0) {
            SwingUtilities.invokeLater(() -> {
                ChangeDisplayDialog changeDialog = new ChangeDisplayDialog(frame, panel, databaseInTemp, changes);
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
                SavePreferences prefs = SavePreferences.loadForSaveFromPreferences(Globals.prefs).withMakeBackup(false)
                        .withEncoding(panel.getBibDatabaseContext().getMetaData().getEncoding()
                                .orElse(Globals.prefs.getDefaultEncoding()));

                Defaults defaults = new Defaults(BibDatabaseMode
                        .fromPreference(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)));
                BibDatabaseWriter<SaveSession> databaseWriter = new BibtexDatabaseWriter<>(FileSaveSession::new);
                SaveSession ss = databaseWriter.saveDatabase(new BibDatabaseContext(databaseInTemp, metadataInTemp, defaults), prefs);
                ss.commit(Globals.getFileUpdateMonitor().getTempFile(panel.fileMonitorHandle()));
            } catch (SaveException ex) {
                LOGGER.warn("Problem updating tmp file after accepting external changes", ex);
            }
        });
    }

    private void scanMetaData(MetaData inMemory, MetaData onTmp, MetaData onDisk) {
        if (!onTmp.isEmpty()) {
            if (!inMemory.equals(onDisk)) {
                changes.add(new MetaDataChange(inMemory, onDisk));
            }
        } else {
            if (!onDisk.isEmpty() || !onTmp.equals(onDisk)) {
                changes.add(new MetaDataChange(inMemory, onDisk));
            }
        }
    }

    private void scanEntries(EntrySorter memorySorter, EntrySorter tmpSorter, EntrySorter diskSorter) {

        // Create pointers that are incremented as the entries of each base are used in
        // successive order from the beginning. Entries "further down" in the "disk" base
        // can also be matched.
        int piv1;
        int piv2 = 0;

        // Create a HashSet where we can put references to entry numbers in the "disk"
        // database that we have matched. This is to avoid matching them twice.
        Set<String> used = new HashSet<>(diskSorter.getEntryCount());
        Set<Integer> notMatched = new HashSet<>(tmpSorter.getEntryCount());

        // Loop through the entries of the "tmp" database, looking for exact matches in the "disk" one.
        // We must finish scanning for exact matches before looking for near matches, to avoid an exact
        // match being "stolen" from another entry.
        mainLoop:
        for (piv1 = 0; piv1 < tmpSorter.getEntryCount(); piv1++) {

            // First check if the similarly placed entry in the other base matches exactly.
            double comp = -1;
            // (if there are not any entries left in the "disk" database, comp will stay at -1,
            // and this entry will be marked as nonmatched).
            if (!used.contains(String.valueOf(piv2)) && (piv2 < diskSorter.getEntryCount())) {
                comp = DuplicateCheck.compareEntriesStrictly(tmpSorter.getEntryAt(piv1), diskSorter.getEntryAt(piv2));
            }
            if (comp > 1) {
                used.add(String.valueOf(piv2));
                piv2++;
                continue;
            }

            // No? Then check if another entry matches exactly.
            if (piv2 < (diskSorter.getEntryCount() - 1)) {
                for (int i = piv2 + 1; i < diskSorter.getEntryCount(); i++) {
                    if (used.contains(String.valueOf(i))) {
                        comp = -1;
                    } else {
                        comp = DuplicateCheck.compareEntriesStrictly(tmpSorter.getEntryAt(piv1), diskSorter.getEntryAt(i));
                    }

                    if (comp > 1) {
                        used.add(String.valueOf(i));
                        continue mainLoop;
                    }
                }
            }

            // No? Add this entry to the list of nonmatched entries.
            notMatched.add(piv1);
        }

        // Now we've found all exact matches, look through the remaining entries, looking
        // for close matches.
        if (!notMatched.isEmpty()) {

            for (Iterator<Integer> it = notMatched.iterator(); it.hasNext(); ) {

                piv1 = it.next();

                // These two variables will keep track of which entry most closely matches the
                // one we're looking at, in case none matches completely.
                int bestMatchI = -1;
                double bestMatch = 0;
                double comp;

                if (piv2 < (diskSorter.getEntryCount() - 1)) {
                    for (int i = piv2; i < diskSorter.getEntryCount(); i++) {
                        if (used.contains(String.valueOf(i))) {
                            comp = -1;
                        } else {
                            comp = DuplicateCheck.compareEntriesStrictly(tmpSorter.getEntryAt(piv1), diskSorter.getEntryAt(i));
                        }

                        if (comp > bestMatch) {
                            bestMatch = comp;
                            bestMatchI = i;
                        }
                    }
                }

                if (bestMatch > MATCH_THRESHOLD) {
                    used.add(String.valueOf(bestMatchI));
                    it.remove();

                    changes.add(new EntryChange(bestFit(tmpSorter, memorySorter, piv1), tmpSorter.getEntryAt(piv1),
                            diskSorter.getEntryAt(bestMatchI)));
                } else {
                    changes.add(
                            new EntryDeleteChange(bestFit(tmpSorter, memorySorter, piv1), tmpSorter.getEntryAt(piv1)));
                }

            }

        }

        // Finally, look if there are still untouched entries in the disk database. These
        // may have been added.
        if (used.size() < diskSorter.getEntryCount()) {
            for (int i = 0; i < diskSorter.getEntryCount(); i++) {
                if (!used.contains(String.valueOf(i))) {

                    // See if there is an identical dupe in the mem database:
                    boolean hasAlready = false;
                    for (int j = 0; j < memorySorter.getEntryCount(); j++) {
                        if (DuplicateCheck.compareEntriesStrictly(memorySorter.getEntryAt(j), diskSorter.getEntryAt(i)) >= 1) {
                            hasAlready = true;
                            break;
                        }
                    }
                    if (!hasAlready) {
                        changes.add(new EntryAddChange(diskSorter.getEntryAt(i)));
                    }
                }
            }
        }
    }

    /**
     * Finds the entry in neu best fitting the specified entry in old. If no entries get a score
     * above zero, an entry is still returned.
     *
     * @param oldSorter   EntrySorter
     * @param newSorter   EntrySorter
     * @param index int
     * @return BibEntry
     */
    private static BibEntry bestFit(EntrySorter oldSorter, EntrySorter newSorter, int index) {
        double comp = -1;
        int found = 0;
        for (int i = 0; i < newSorter.getEntryCount(); i++) {
            double res = DuplicateCheck.compareEntriesStrictly(oldSorter.getEntryAt(index), newSorter.getEntryAt(i));
            if (res > comp) {
                comp = res;
                found = i;
            }
            if (comp > 1) {
                break;
            }
        }
        return newSorter.getEntryAt(found);
    }

    private void scanPreamble(BibDatabase inMemory, BibDatabase onTmp, BibDatabase onDisk) {
        String mem = inMemory.getPreamble().orElse(null);
        Optional<String> tmp = onTmp.getPreamble();
        Optional<String> disk = onDisk.getPreamble();
        if (!tmp.isPresent()) {
            disk.ifPresent(diskContent -> changes.add(new PreambleChange(mem, diskContent)));
        } else {
            if (!disk.isPresent() || !tmp.equals(disk)) {
                changes.add(new PreambleChange(mem, disk.orElse(null)));
            }
        }
    }

    private void scanStrings(BibDatabase inMem1, BibDatabase inTmp, BibDatabase onDisk) {
        if (inTmp.hasNoStrings() && onDisk.hasNoStrings()) {
            return;
        }

        Set<Object> used = new HashSet<>();
        Set<Object> usedInMem = new HashSet<>();
        Set<String> notMatched = new HashSet<>(inTmp.getStringCount());

        // First try to match by string names.
        mainLoop:
        for (String key : inTmp.getStringKeySet()) {
            BibtexString tmp = inTmp.getString(key);

            for (String diskId : onDisk.getStringKeySet()) {
                if (!used.contains(diskId)) {
                    BibtexString disk = onDisk.getString(diskId);
                    if (disk.getName().equals(tmp.getName())) {
                        // We have found a string with a matching name.
                        if (!Objects.equals(tmp.getContent(), disk.getContent())) {
                            // But they have nonmatching contents, so we've found a change.
                            Optional<BibtexString> mem = findString(inMem1, tmp.getName(), usedInMem);
                            if (mem.isPresent()) {
                                changes.add(new StringChange(mem.get(), tmp, tmp.getName(), mem.get().getContent(),
                                        disk.getContent()));
                            } else {
                                changes.add(new StringChange(null, tmp, tmp.getName(), null, disk.getContent()));
                            }
                        }
                        used.add(diskId);
                        continue mainLoop;
                    }

                }
            }
            // If we get here, there was no match for this string.
            notMatched.add(tmp.getId());
        }

        // See if we can detect a name change for those entries that we couldn't match.
        if (!notMatched.isEmpty()) {
            for (Iterator<String> i = notMatched.iterator(); i.hasNext(); ) {
                BibtexString tmp = inTmp.getString(i.next());

                // If we get to this point, we found no string with matching name. See if we
                // can find one with matching content.
                for (String diskId : onDisk.getStringKeySet()) {

                    if (!used.contains(diskId)) {
                        BibtexString disk = onDisk.getString(diskId);

                        if (disk.getContent().equals(tmp.getContent())) {
                            // We have found a string with the same content. It cannot have the same
                            // name, or we would have found it above.

                            // Try to find the matching one in memory:
                            BibtexString bsMem = null;

                            for (String memId : inMem1.getStringKeySet()) {
                                BibtexString bsMemCandidate = inMem1.getString(memId);
                                if (bsMemCandidate.getContent().equals(disk.getContent())
                                        && !usedInMem.contains(memId)) {
                                    usedInMem.add(memId);
                                    bsMem = bsMemCandidate;
                                    break;
                                }
                            }

                            if (bsMem != null) {
                                changes.add(new StringNameChange(bsMem, tmp, bsMem.getName(), tmp.getName(),
                                        disk.getName(), tmp.getContent()));
                                i.remove();
                                used.add(diskId);
                            }

                        }
                    }
                }
            }
        }

        if (!notMatched.isEmpty()) {
            // Still one or more non-matched strings. So they must have been removed.
            for (String notMatchedId : notMatched) {
                BibtexString tmp = inTmp.getString(notMatchedId);
                // The removed string is not removed from the mem version.
                findString(inMem1, tmp.getName(), usedInMem).ifPresent(
                        x -> changes.add(new StringRemoveChange(tmp, tmp, x)));
            }
        }

        // Finally, see if there are remaining strings in the disk database. They
        // must have been added.
        for (String diskId : onDisk.getStringKeySet()) {
            if (!used.contains(diskId)) {
                BibtexString disk = onDisk.getString(diskId);
                used.add(diskId);
                changes.add(new StringAddChange(disk));
            }
        }
    }

    private static Optional<BibtexString> findString(BibDatabase base, String name, Set<Object> used) {
        if (!base.hasStringLabel(name)) {
            return Optional.empty();
        }
        for (String key : base.getStringKeySet()) {
            BibtexString bs = base.getString(key);
            if (bs.getName().equals(name) && !used.contains(key)) {
                used.add(key);
                return Optional.of(bs);
            }
        }
        return Optional.empty();
    }

    /**
     * This method only detects whether a change took place or not. It does not determine the type of change. This would
     * be possible, but difficult to do properly, so I rather only report the change.
     */
    private void scanGroups(MetaData inTemp, MetaData onDisk) {
        final Optional<GroupTreeNode> groupsTmp = inTemp.getGroups();
        final Optional<GroupTreeNode> groupsDisk = onDisk.getGroups();
        if (!groupsTmp.isPresent() && !groupsDisk.isPresent()) {
            return;
        }
        if ((groupsTmp.isPresent() && !groupsDisk.isPresent()) || !groupsTmp.isPresent()) {
            changes.add(new GroupChange(groupsDisk.orElse(null), groupsTmp.orElse(null)));
            return;
        }
        // Both present here
        if (!groupsTmp.equals(groupsDisk)) {
            changes.add(new GroupChange(groupsDisk.get(), groupsTmp.get()));
        }
    }


    @FunctionalInterface
    public interface DisplayResultCallback {
        void scanResultsResolved(boolean resolved);
    }
}
