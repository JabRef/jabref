/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.collab;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.*;
import net.sf.jabref.exporter.FileActions;
import net.sf.jabref.exporter.SaveException;
import net.sf.jabref.exporter.SaveSession;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.importer.OpenDatabaseAction;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.bibtex.DuplicateCheck;
import net.sf.jabref.model.database.EntrySorter;
import net.sf.jabref.bibtex.comparator.EntryComparator;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexString;

public class ChangeScanner implements Runnable {

    private final String[] sortBy = new String[] {"year", "author", "title"};

    private final File f;

    private final BibtexDatabase inMem;
    private final MetaData mdInMem;
    private final BasePanel panel;
    private final JabRefFrame frame;

    private BibtexDatabase inTemp;
    private MetaData mdInTemp;

    private static final Log LOGGER = LogFactory.getLog(ChangeScanner.class);


    /**
     * We create an ArrayList to hold the changes we find. These will be added in the form
     * of UndoEdit objects. We instantiate these so that the changes found in the file on disk
     * can be reproduced in memory by calling redo() on them. REDO, not UNDO!
     */
    //ArrayList changes = new ArrayList();
    private final DefaultMutableTreeNode changes = new DefaultMutableTreeNode(Localization.lang("External changes"));


    //  NamedCompound edit = new NamedCompound("Merged external changes")

    public ChangeScanner(JabRefFrame frame, BasePanel bp, File file) { //, BibtexDatabase inMem, MetaData mdInMem) {
        this.panel = bp;
        this.frame = frame;
        this.inMem = bp.database();
        this.mdInMem = bp.metaData();
        this.f = file;
    }

    @Override
    public void run() {
        try {
            //long startTime = System.currentTimeMillis();

            // Parse the temporary file.
            File tempFile = Globals.fileUpdateMonitor.getTempFile(panel.fileMonitorHandle());
            ParserResult pr = OpenDatabaseAction.loadDatabase(tempFile, Globals.prefs.getDefaultEncoding());
            inTemp = pr.getDatabase();
            mdInTemp = pr.getMetaData();
            // Parse the modified file.
            pr = OpenDatabaseAction.loadDatabase(f, Globals.prefs.getDefaultEncoding());
            BibtexDatabase onDisk = pr.getDatabase();
            MetaData mdOnDisk = pr.getMetaData();

            // Sort both databases according to a common sort key.
            EntryComparator comp = new EntryComparator(false, true, sortBy[2]);
            comp = new EntryComparator(false, true, sortBy[1], comp);
            comp = new EntryComparator(false, true, sortBy[0], comp);
            EntrySorter sInTemp = inTemp.getSorter(comp);
            comp = new EntryComparator(false, true, sortBy[2]);
            comp = new EntryComparator(false, true, sortBy[1], comp);
            comp = new EntryComparator(false, true, sortBy[0], comp);
            EntrySorter sOnDisk = onDisk.getSorter(comp);
            comp = new EntryComparator(false, true, sortBy[2]);
            comp = new EntryComparator(false, true, sortBy[1], comp);
            comp = new EntryComparator(false, true, sortBy[0], comp);
            EntrySorter sInMem = inMem.getSorter(comp);

            // Start looking at changes.
            scanMetaData(mdInMem, mdInTemp, mdOnDisk);
            scanPreamble(inMem, inTemp, onDisk);
            scanStrings(inMem, inTemp, onDisk);

            scanEntries(sInMem, sInTemp, sOnDisk);

            scanGroups(mdInTemp, mdOnDisk);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean changesFound() {
        return changes.getChildCount() > 0;
    }

    public void displayResult(final DisplayResultCallback fup) {
        if (changes.getChildCount() > 0) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    ChangeDisplayDialog dial = new ChangeDisplayDialog(frame, panel, inTemp, changes);
                    PositionWindow.placeDialog(dial, frame);
                    dial.setVisible(true); // dial.show(); -> deprecated since 1.5
                    fup.scanResultsResolved(dial.isOkPressed());
                    if (dial.isOkPressed()) {
                        // Overwrite the temp database:
                        storeTempDatabase();
                    }
                }
            });

        } else {
            JOptionPane.showMessageDialog(frame, Localization.lang("No actual changes found."),
                    Localization.lang("External changes"), JOptionPane.INFORMATION_MESSAGE);
            fup.scanResultsResolved(true);
        }
    }

    private void storeTempDatabase() {
        JabRefExecutorService.INSTANCE.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    SaveSession ss = FileActions.saveDatabase(inTemp, mdInTemp,
                            Globals.fileUpdateMonitor.getTempFile(panel.fileMonitorHandle()), Globals.prefs,
                            false, false, panel.getEncoding(), true);
                    ss.commit();
                } catch (SaveException ex) {
                    LOGGER.warn("Problem updating tmp file after accepting external changes", ex);
                }

            }
        });
    }

    private void scanMetaData(MetaData inMem1, MetaData inTemp1, MetaData onDisk) {
        MetaDataChange mdc = new MetaDataChange(inMem1, inTemp1);
        ArrayList<String> handledOnDisk = new ArrayList<>();
        // Loop through the metadata entries of the "tmp" database, looking for
        // matches
        for (String key : inTemp1) {
            // See if the key is missing in the disk database:
            Vector<String> vod = onDisk.getData(key);
            if (vod == null) {
                mdc.insertMetaDataRemoval(key);
            } else {
                // Both exist. Check if they are different:
                Vector<String> vit = inTemp1.getData(key);
                if (!vod.equals(vit)) {
                    mdc.insertMetaDataChange(key, vod);
                }
                // Remember that we've handled this one:
                handledOnDisk.add(key);
            }
        }

        // See if there are unhandled keys in the disk database:
        for (String key : onDisk) {
            if (!handledOnDisk.contains(key)) {
                mdc.insertMetaDataAddition(key, onDisk.getData(key));
            }
        }

        if (mdc.getChangeCount() > 0) {
            changes.add(mdc);
        }
    }

    private void scanEntries(EntrySorter mem, EntrySorter tmp, EntrySorter disk) {

        // Create pointers that are incremented as the entries of each base are used in
        // successive order from the beginning. Entries "further down" in the "disk" base
        // can also be matched.
        int piv1;
        int piv2 = 0;

        // Create a HashSet where we can put references to entry numbers in the "disk"
        // database that we have matched. This is to avoid matching them twice.
        HashSet<String> used = new HashSet<>(disk.getEntryCount());
        HashSet<Integer> notMatched = new HashSet<>(tmp.getEntryCount());

        // Loop through the entries of the "tmp" database, looking for exact matches in the "disk" one.
        // We must finish scanning for exact matches before looking for near matches, to avoid an exact
        // match being "stolen" from another entry.
        mainLoop: for (piv1 = 0; piv1 < tmp.getEntryCount(); piv1++) {

            // First check if the similarly placed entry in the other base matches exactly.
            double comp = -1;
            // (if there are not any entries left in the "disk" database, comp will stay at -1,
            // and this entry will be marked as nonmatched).
            if (!used.contains(String.valueOf(piv2)) && (piv2 < disk.getEntryCount())) {
                comp = DuplicateCheck.compareEntriesStrictly(tmp.getEntryAt(piv1), disk.getEntryAt(piv2));
            }
            if (comp > 1) {
                used.add(String.valueOf(piv2));
                piv2++;
                continue;
            }

            // No? Then check if another entry matches exactly.
            if (piv2 < (disk.getEntryCount() - 1)) {
                for (int i = piv2 + 1; i < disk.getEntryCount(); i++) {
                    if (!used.contains(String.valueOf(i))) {
                        comp = DuplicateCheck.compareEntriesStrictly(tmp.getEntryAt(piv1), disk.getEntryAt(i));
                    } else {
                        comp = -1;
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

            for (Iterator<Integer> it = notMatched.iterator(); it.hasNext();) {

                piv1 = it.next();

                // These two variables will keep track of which entry most closely matches the
                // one we're looking at, in case none matches completely.
                int bestMatchI = -1;
                double bestMatch = 0;
                double comp;

                if (piv2 < (disk.getEntryCount() - 1)) {
                    for (int i = piv2; i < disk.getEntryCount(); i++) {
                        if (!used.contains(String.valueOf(i))) {
                            comp = DuplicateCheck.compareEntriesStrictly(tmp.getEntryAt(piv1),
                                    disk.getEntryAt(i));
                        } else {
                            comp = -1;
                        }

                        if (comp > bestMatch) {
                            bestMatch = comp;
                            bestMatchI = i;
                        }
                    }
                }

                double MATCH_THRESHOLD = 0.4;
                if (bestMatch > MATCH_THRESHOLD) {
                    used.add(String.valueOf(bestMatchI));
                    it.remove();

                    EntryChange ec = new EntryChange(bestFit(tmp, mem, piv1), tmp.getEntryAt(piv1),
                            disk.getEntryAt(bestMatchI));
                    changes.add(ec);

                    // Create an undo edit to represent this change:
                    //NamedCompound ce = new NamedCompound("Modified entry");
                    //ce.addEdit(new UndoableRemoveEntry(inMem, disk.getEntryAt(bestMatchI), panel));
                    //ce.addEdit(new UndoableInsertEntry(inMem, tmp.getEntryAt(piv1), panel));
                    //ce.end();
                    //changes.add(ce);

                    //System.out.println("Possible match for entry:");
                    //System.out.println("----------------------------------------------");

                }
                else {
                    EntryDeleteChange ec = new EntryDeleteChange(bestFit(tmp, mem, piv1), tmp.getEntryAt(piv1));
                    changes.add(ec);
                    /*NamedCompound ce = new NamedCompound("Removed entry");
                    ce.addEdit(new UndoableInsertEntry(inMem, tmp.getEntryAt(piv1), panel));
                    ce.end();
                    changes.add(ce);*/

                }

            }

        }

        // Finally, look if there are still untouched entries in the disk database. These
        // may have been added.
        if (used.size() < disk.getEntryCount()) {
            for (int i = 0; i < disk.getEntryCount(); i++) {
                if (!used.contains(String.valueOf(i))) {

                    // See if there is an identical dupe in the mem database:
                    boolean hasAlready = false;
                    for (int j = 0; j < mem.getEntryCount(); j++) {
                        if (DuplicateCheck.compareEntriesStrictly(mem.getEntryAt(j),
                                disk.getEntryAt(i)) >= 1) {
                            hasAlready = true;
                            break;
                        }
                    }
                    if (!hasAlready) {
                        EntryAddChange ec = new EntryAddChange(disk.getEntryAt(i));
                        changes.add(ec);
                    }
                    /*NamedCompound ce = new NamedCompound("Added entry");
                    ce.addEdit(new UndoableRemoveEntry(inMem, disk.getEntryAt(i), panel));
                    ce.end();
                    changes.add(ce);*/
                }
            }
            //System.out.println("Suspected new entries in file: "+(disk.getEntryCount()-used.size()));
        }
    }

    /**
     * Finds the entry in neu best fitting the specified entry in old. If no entries get a score
     * above zero, an entry is still returned.
     * @param old EntrySorter
     * @param neu EntrySorter
     * @param index int
     * @return BibtexEntry
     */
    private static BibtexEntry bestFit(EntrySorter old, EntrySorter neu, int index) {
        double comp = -1;
        int found = 0;
        for (int i = 0; i < neu.getEntryCount(); i++) {
            double res = DuplicateCheck.compareEntriesStrictly(old.getEntryAt(index),
                    neu.getEntryAt(i));
            if (res > comp) {
                comp = res;
                found = i;
            }
            if (comp > 1) {
                break;
            }
        }
        return neu.getEntryAt(found);
    }

    private void scanPreamble(BibtexDatabase inMem1, BibtexDatabase onTmp, BibtexDatabase onDisk) {
        String mem = inMem1.getPreamble();
        String tmp = onTmp.getPreamble();
        String disk = onDisk.getPreamble();
        if (tmp != null) {
            if ((disk == null) || !tmp.equals(disk)) {
                changes.add(new PreambleChange(tmp, mem, disk));
            }
        }
        else if ((disk != null) && !disk.isEmpty()) {
            changes.add(new PreambleChange(tmp, mem, disk));
        }
    }

    private void scanStrings(BibtexDatabase inMem1, BibtexDatabase onTmp, BibtexDatabase onDisk) {
        int nTmp = onTmp.getStringCount();
        int nDisk = onDisk.getStringCount();
        if ((nTmp == 0) && (nDisk == 0)) {
            return;
        }

        HashSet<Object> used = new HashSet<>();
        HashSet<Object> usedInMem = new HashSet<>();
        HashSet<String> notMatched = new HashSet<>(onTmp.getStringCount());

        // First try to match by string names.
        //int piv2 = -1;
        mainLoop: for (String key : onTmp.getStringKeySet()) {
            BibtexString tmp = onTmp.getString(key);

            //      for (int j=piv2+1; j<nDisk; j++)
            for (String diskId : onDisk.getStringKeySet()) {
                if (!used.contains(diskId)) {
                    BibtexString disk = onDisk.getString(diskId);
                    if (disk.getName().equals(tmp.getName())) {
                        // We have found a string with a matching name.
                        if ((tmp.getContent() != null) && !tmp.getContent().equals(disk.getContent())) {
                            // But they have nonmatching contents, so we've found a change.
                            BibtexString mem = findString(inMem1, tmp.getName(), usedInMem);
                            if (mem != null) {
                                changes.add(
                                        new StringChange(mem, tmp, tmp.getName(), mem.getContent(), disk.getContent()));
                            } else {
                                changes.add(new StringChange(null, tmp, tmp.getName(), null, disk.getContent()));
                            }
                        }
                        used.add(diskId);
                        //if (j==piv2)
                        //  piv2++;
                        continue mainLoop;
                    }

                }
            }
            // If we get here, there was no match for this string.
            notMatched.add(tmp.getId());
        }

        // See if we can detect a name change for those entries that we couldn't match.
        if (!notMatched.isEmpty()) {
            for (Iterator<String> i = notMatched.iterator(); i.hasNext();) {
                BibtexString tmp = onTmp.getString(i.next());

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
                                BibtexString bsMem_cand = inMem1.getString(memId);
                                if (bsMem_cand.getContent().equals(disk.getContent()) &&
                                        !usedInMem.contains(memId)) {
                                    usedInMem.add(memId);
                                    bsMem = bsMem_cand;
                                    break;
                                }
                            }

                            changes.add(new StringNameChange(bsMem, tmp, bsMem.getName(),
                                    tmp.getName(), disk.getName(),
                                    tmp.getContent()));
                            i.remove();
                            used.add(diskId);

                        }
                    }
                }
            }
        }

        if (!notMatched.isEmpty()) {
            // Still one or more non-matched strings. So they must have been removed.
            for (String nmId : notMatched) {
                BibtexString tmp = onTmp.getString(nmId);
                BibtexString mem = findString(inMem1, tmp.getName(), usedInMem);
                if (mem != null) { // The removed string is not removed from the mem version.
                    changes.add(new StringRemoveChange(tmp, tmp, mem));
                }
            }
        }

        // Finally, see if there are remaining strings in the disk database. They
        // must have been added.
        for (String diskId : onDisk.getStringKeySet()) {
            if (!used.contains(diskId)) {
                BibtexString disk = onDisk.getString(diskId);
                //System.out.println(disk.getName());
                used.add(diskId);
                changes.add(new StringAddChange(disk));
            }
        }
    }

    private static BibtexString findString(BibtexDatabase base, String name, HashSet<Object> used) {
        if (!base.hasStringLabel(name)) {
            return null;
        }
        for (String key : base.getStringKeySet()) {
            BibtexString bs = base.getString(key);
            if (bs.getName().equals(name) && !used.contains(key)) {
                used.add(key);
                return bs;
            }
        }
        return null;
    }

    /**
     * This method only detects whether a change took place or not. It does not determine the type of change. This would
     * be possible, but difficult to do properly, so I rather only report the change.
     */
    private void scanGroups(MetaData onTmp, MetaData onDisk) {
        final GroupTreeNode groupsTmp = onTmp.getGroups();
        final GroupTreeNode groupsDisk = onDisk.getGroups();
        if ((groupsTmp == null) && (groupsDisk == null)) {
            return;
        }
        if (((groupsTmp != null) && (groupsDisk == null)) || (groupsTmp == null)) {
            changes.add(new GroupChange(groupsDisk, groupsTmp));
            return;
        }
        if (!groupsTmp.equals(groupsDisk)) {
            changes.add(new GroupChange(groupsDisk, groupsTmp));
        }
    }


    public interface DisplayResultCallback {
        void scanResultsResolved(boolean resolved);
    }
}
