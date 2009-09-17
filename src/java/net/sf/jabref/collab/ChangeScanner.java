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

import net.sf.jabref.*;
import net.sf.jabref.export.FileActions;
import net.sf.jabref.export.SaveException;
import net.sf.jabref.export.SaveSession;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.imports.OpenDatabaseAction;
import net.sf.jabref.imports.ParserResult;


public class ChangeScanner extends Thread {

    final double MATCH_THRESHOLD = 0.4;
    final String[] sortBy = new String[] {"year", "author", "title" };
    File f;
    BibtexDatabase inMem, inTemp = null;
    MetaData mdInMem, mdInTemp;
    BasePanel panel;
    JabRefFrame frame;

    /**
     * We create an ArrayList to hold the changes we find. These will be added in the form
     * of UndoEdit objects. We instantiate these so that the changes found in the file on disk
     * can be reproduced in memory by calling redo() on them. REDO, not UNDO!
     */
    //ArrayList changes = new ArrayList();
    DefaultMutableTreeNode changes = new DefaultMutableTreeNode(Globals.lang("External changes"));

    //  NamedCompound edit = new NamedCompound("Merged external changes")

    public ChangeScanner(JabRefFrame frame, BasePanel bp) { //, BibtexDatabase inMem, MetaData mdInMem) {
        panel = bp;
        this.frame = frame;
        this.inMem = bp.database();
        this.mdInMem = bp.metaData();
        // Set low priority:
        setPriority(Thread.MIN_PRIORITY);

    }

    public void changeScan(File f) {
        this.f = f;
        start();
    }

    public void run() {
        try {
            //long startTime = System.currentTimeMillis();

            // Parse the temporary file.
            File tempFile = Globals.fileUpdateMonitor.getTempFile(panel.fileMonitorHandle());
            ParserResult pr = OpenDatabaseAction.loadDatabase(tempFile,
            Globals.prefs.get("defaultEncoding"));
            inTemp = pr.getDatabase();
            mdInTemp = new MetaData(pr.getMetaData(),inTemp);
            // Parse the modified file.
            pr = OpenDatabaseAction.loadDatabase(f, Globals.prefs.get("defaultEncoding"));
            BibtexDatabase onDisk = pr.getDatabase();
            MetaData mdOnDisk = new MetaData(pr.getMetaData(),onDisk);

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
            
            scanGroups(mdInMem, mdInTemp, mdOnDisk);


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
                public void run() {
                    ChangeDisplayDialog dial = new ChangeDisplayDialog(frame, panel, inTemp, changes);
                    Util.placeDialog(dial, frame);
                    dial.setVisible(true); // dial.show(); -> deprecated since 1.5
                    fup.scanResultsResolved(dial.isOkPressed());
                    if (dial.isOkPressed()) {
                        // Overwrite the temp database:
                        storeTempDatabase();
                    }
                }
            });

        } else {
            JOptionPane.showMessageDialog(frame, Globals.lang("No actual changes found."),
            Globals.lang("External changes"), JOptionPane.INFORMATION_MESSAGE);
            fup.scanResultsResolved(true);
        }
    }

    private void storeTempDatabase() {
        new Thread(new Runnable() {
            public void run() {
                System.out.println("Storing temp database");
                try {
                    SaveSession ss = FileActions.saveDatabase(inTemp, mdInTemp,
                        Globals.fileUpdateMonitor.getTempFile(panel.fileMonitorHandle()), Globals.prefs,
                        false, false, panel.getEncoding(), true);
                    ss.commit();
                    System.out.println("done");
                } catch (SaveException ex) {
                    System.out.println("Problem updating tmp file after accepting external changes");
                }


            }
        }).start();
    }

    private void scanMetaData(MetaData inMem, MetaData inTemp, MetaData onDisk) {
        MetaDataChange mdc = new MetaDataChange(inMem, inTemp);
        ArrayList<String> handledOnDisk = new ArrayList<String>();
        // Loop through the metadata entries of the "tmp" database, looking for
        // matches
        for (Iterator i = inTemp.iterator(); i.hasNext();) {
            String key = (String)i.next();
            // See if the key is missing in the disk database:
            Vector<String> vod = onDisk.getData(key);
            if (vod == null) {
                mdc.insertMetaDataRemoval(key);
            }
            else {
                // Both exist. Check if they are different:
                Vector<String> vit = inTemp.getData(key);
                if (!vod.equals(vit))
                    mdc.insertMetaDataChange(key, vod);
                // Remember that we've handled this one:
                handledOnDisk.add(key);
            }
        }

        // See if there are unhandled keys in the disk database:
        for (Iterator i = onDisk.iterator(); i.hasNext();) {
            String key = (String)i.next();
            if (!handledOnDisk.contains(key)) {
                mdc.insertMetaDataAddition(key, onDisk.getData(key));
            }
        }

        if (mdc.getChangeCount() > 0)
            changes.add(mdc);
    }


    private void scanEntries(EntrySorter mem, EntrySorter tmp, EntrySorter disk) {

        // Create pointers that are incremented as the entries of each base are used in
        // successive order from the beginning. Entries "further down" in the "disk" base
        // can also be matched.
        int piv1 = 0, piv2 = 0;

        // Create a HashSet where we can put references to entry numbers in the "disk"
        // database that we have matched. This is to avoid matching them twice.
        HashSet<String> used = new HashSet<String>(disk.getEntryCount());
        HashSet<Integer> notMatched = new HashSet<Integer>(tmp.getEntryCount());

        // Loop through the entries of the "tmp" database, looking for exact matches in the "disk" one.
        // We must finish scanning for exact matches before looking for near matches, to avoid an exact
        // match being "stolen" from another entry.
        mainLoop: for (piv1=0; piv1<tmp.getEntryCount(); piv1++) {

            // First check if the similarly placed entry in the other base matches exactly.
            double comp = -1;
            // (if there are not any entries left in the "disk" database, comp will stay at -1,
            // and this entry will be marked as nonmatched).
            if (!used.contains(""+piv2) && (piv2<disk.getEntryCount())) {
                comp = DuplicateCheck.compareEntriesStrictly(tmp.getEntryAt(piv1), disk.getEntryAt(piv2));
            }
            if (comp > 1) {
                used.add(""+piv2);
                piv2++;
                continue mainLoop;
            }

            // No? Then check if another entry matches exactly.
            if (piv2 < disk.getEntryCount()-1) {
                for (int i = piv2+1; i < disk.getEntryCount(); i++) {
                    if (!used.contains(""+i))
                        comp = DuplicateCheck.compareEntriesStrictly(tmp.getEntryAt(piv1), disk.getEntryAt(i));
                    else
                        comp = -1;

                    if (comp > 1) {
                        used.add("" + i);
                        continue mainLoop;
                    }
                }
            }

            // No? Add this entry to the list of nonmatched entries.
            notMatched.add(new Integer(piv1));
        }


        // Now we've found all exact matches, look through the remaining entries, looking
        // for close matches.
        if (notMatched.size() > 0) {

            for (Iterator<Integer> it=notMatched.iterator(); it.hasNext();) {

                Integer integ = it.next();
                piv1 = integ.intValue();


                // These two variables will keep track of which entry most closely matches the
                // one we're looking at, in case none matches completely.
                int bestMatchI = -1;
                double bestMatch = 0;
                double comp = -1;

                if (piv2 < disk.getEntryCount()-1) {
                    for (int i = piv2; i < disk.getEntryCount(); i++) {
                        if (!used.contains(""+i)) {
                            comp = DuplicateCheck.compareEntriesStrictly(tmp.getEntryAt(piv1),
                            disk.getEntryAt(i));
                        }
                        else
                            comp = -1;

                        if (comp > bestMatch) {
                            bestMatch = comp;
                            bestMatchI = i;
                        }
                    }
                }

                if (bestMatch > MATCH_THRESHOLD) {
                    used.add(""+bestMatchI);
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
        // mayhave been added.
        if (used.size() < disk.getEntryCount()) {
            for (int i=0; i<disk.getEntryCount(); i++) {
                if (!used.contains(""+i)) {

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
    private BibtexEntry bestFit(EntrySorter old, EntrySorter neu, int index) {
        double comp = -1;
        int found = 0;
        loop: for (int i=0; i<neu.getEntryCount(); i++) {
            double res = DuplicateCheck.compareEntriesStrictly(old.getEntryAt(index),
            neu.getEntryAt(i));
            if (res > comp) {
                comp = res;
                found = i;
            }
            if (comp > 1)
                break loop;
        }
        return neu.getEntryAt(found);
    }

    private void scanPreamble(BibtexDatabase inMem, BibtexDatabase onTmp, BibtexDatabase onDisk) {
        String mem = inMem.getPreamble(),
        tmp = onTmp.getPreamble(),
        disk = onDisk.getPreamble();
        if (tmp != null) {
            if ((disk == null) || !tmp.equals(disk))
                changes.add(new PreambleChange(tmp, mem, disk));
        }
        else if ((disk != null) && !disk.equals("")) {
            changes.add(new PreambleChange(tmp, mem, disk));
        }
    }

    private void scanStrings(BibtexDatabase inMem, BibtexDatabase onTmp, BibtexDatabase onDisk) {
        int nTmp = onTmp.getStringCount(),
        nDisk = onDisk.getStringCount();
        if ((nTmp == 0) && (nDisk == 0))
            return;

        HashSet<Object> used = new HashSet<Object>();
        HashSet<Object> usedInMem = new HashSet<Object>();
        HashSet<String> notMatched = new HashSet<String>(onTmp.getStringCount());

        // First try to match by string names.
        //int piv2 = -1;
        mainLoop: for (String key : onTmp.getStringKeySet()){
            BibtexString tmp = onTmp.getString(key);

            //      for (int j=piv2+1; j<nDisk; j++)
            for (String diskId : onDisk.getStringKeySet()){
                if (!used.contains(diskId)) {
                    BibtexString disk = onDisk.getString(diskId);
                    if (disk.getName().equals(tmp.getName())) {
                        // We have found a string with a matching name.
                        if ((tmp.getContent() != null) && !tmp.getContent().equals(disk.getContent())) {
                            // But they have nonmatching contents, so we've found a change.
                            BibtexString mem = findString(inMem, tmp.getName(), usedInMem);
                            if (mem != null)
                                changes.add(new StringChange(mem, tmp, tmp.getName(),
                                mem.getContent(),
                                tmp.getContent(), disk.getContent()));
                            else
                                changes.add(new StringChange(null, tmp, tmp.getName(), null, tmp.getContent(), disk.getContent()));
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
        if (notMatched.size() > 0) {
            for (Iterator<String> i = notMatched.iterator(); i.hasNext();){
                BibtexString tmp = onTmp.getString(i.next());

                // If we get to this point, we found no string with matching name. See if we
                // can find one with matching content.
                for (String diskId : onDisk.getStringKeySet()){

                	if (!used.contains(diskId)) {
                        BibtexString disk = onDisk.getString(diskId);

                        if (disk.getContent().equals(tmp.getContent())) {
                            // We have found a string with the same content. It cannot have the same
                            // name, or we would have found it above.

                            // Try to find the matching one in memory:
                            BibtexString bsMem = null;
                            
                            for (String memId : inMem.getStringKeySet()){
                                BibtexString bsMem_cand = inMem.getString(memId);
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

        if (notMatched.size() > 0) {
            // Still one or more non-matched strings. So they must have been removed.
            for (Iterator<String> i = notMatched.iterator(); i.hasNext(); ) {
                String nmId = i.next();
                BibtexString tmp = onTmp.getString(nmId);
                BibtexString mem = findString(inMem, tmp.getName(), usedInMem);
                if (mem != null) { // The removed string is not removed from the mem version.
                    changes.add(new StringRemoveChange(tmp, tmp, mem));
                }
            }
        }


        // Finally, see if there are remaining strings in the disk database. They
        // must have been added.
        for (Iterator<String> i=onDisk.getStringKeySet().iterator(); i.hasNext();) {
            String diskId = i.next();
            if (!used.contains(diskId)) {
                BibtexString disk = onDisk.getString(diskId);
                //System.out.println(disk.getName());
                used.add(diskId);
                changes.add(new StringAddChange(disk));
            }
        }
    }

    private BibtexString findString(BibtexDatabase base, String name, HashSet<Object> used) {
        if (!base.hasStringLabel(name))
            return null;
        for (Iterator<String> i=base.getStringKeySet().iterator(); i.hasNext();) {
            String key = i.next();
            BibtexString bs = base.getString(key);
            if (bs.getName().equals(name) && !used.contains(key)) {
                used.add(key);
                return bs;
            }
        }
        return null;
    }

    /**
     * This method only detects wheter a change took place or not. It does not
     * determine the type of change. This would be possible, but difficult to do
     * properly, so I rather only report the change.
     */
    public void scanGroups(MetaData inMem, MetaData onTmp, MetaData onDisk) {
        final GroupTreeNode groupsTmp = onTmp.getGroups();
        final GroupTreeNode groupsDisk = onDisk.getGroups();
        if (groupsTmp == null && groupsDisk == null)
            return;
        if ((groupsTmp != null && groupsDisk == null)
                || (groupsTmp == null && groupsDisk != null)) {
            changes.add(new GroupChange(groupsDisk, groupsTmp));
            return;
        }
        if (groupsTmp.equals(groupsDisk))
            return;
        changes.add(new GroupChange(groupsDisk, groupsTmp));
        return;

//
//        if (((vOnTmp == null) || (vOnTmp.size()==0)) && ((vOnDisk == null) || (vOnDisk.size()==0))) {
//            // No groups defined in either the tmp or disk version.
//            return;
//        }
//
//        // To avoid checking for null all the time, make empty vectors to replace null refs. We clone
//        // non-null vectors so we can remove the elements as we finish with them.
//        if (vOnDisk == null)
//            vOnDisk = new Vector(0);
//        else
//            vOnDisk = (Vector)vOnDisk.clone();
//        if (vOnTmp == null)
//            vOnTmp = new Vector(0);
//        else
//            vOnTmp = (Vector)vOnTmp.clone();
//        if (vInMem == null)
//            vInMem = new Vector(0);
//        else
//            vInMem = (Vector)vInMem.clone();
//
//        // If the tmp version has groups, iterate through these and compare with disk version:
//        while (vOnTmp.size() >= 1) {
//            AbstractGroup group = (AbstractGroup)vOnTmp.firstElement();
//            vOnTmp.removeElementAt(0);
//            int pos = GroupSelector.findGroupByName(vOnDisk,group.getName());
//            if (pos == -1) {
//                // Couldn't find the group.
//                changes.add(new GroupAddOrRemove(group, false));
//            } else {
//                AbstractGroup diskGroup = (AbstractGroup)vOnDisk.elementAt(pos);
//
//                if (!diskGroup.equals(group)) {
//                    // Group has changed.
//                    changes.add(new GroupChange(inMem, group, diskGroup));
//                }
//
//                // Remove this group, since it's been accounted for.
//                vOnDisk.remove(pos);
//            }
//        }
//
//        // If there are entries left in the disk version, these must have been added.
//        while (vOnDisk.size() >= 1) {
//            AbstractGroup group = (AbstractGroup)vOnDisk.firstElement();
//            vOnDisk.removeElementAt(0);
//            changes.add(new GroupAddOrRemove(group, true));
//        }
    }


    public static interface DisplayResultCallback {
        public void scanResultsResolved(boolean resolved);
    }
}
