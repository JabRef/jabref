package net.sf.jabref.collab;

import net.sf.jabref.*;
import net.sf.jabref.undo.*;
import net.sf.jabref.imports.ImportFormatReader;
import java.io.File;
import java.io.IOException;
import net.sf.jabref.imports.ParserResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JTree;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import java.util.Vector;
import net.sf.jabref.groups.GroupSelector;


public class ChangeScanner {

  final double MATCH_THRESHOLD = 0.4;
  final String[] sortBy = new String[] {"year", "author", "title" };
  BibtexDatabase inMem;
  MetaData mdInMem;
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
  }

  public void changeScan(File f) throws IOException {

    // Parse the temporary file.
    File tempFile = Globals.fileUpdateMonitor.getTempFile(panel.fileMonitorHandle());
    ParserResult pr = ImportFormatReader.loadDatabase(tempFile,
        Globals.prefs.get("defaultEncoding"));
    BibtexDatabase inTemp = pr.getDatabase();
    MetaData mdInTemp = new MetaData(pr.getMetaData());
    //Util.pr(tempFile.getPath()+": "+inMem.getEntryCount());

    // Parse the modified file.
    pr = ImportFormatReader.loadDatabase(f, Globals.prefs.get("defaultEncoding"));
    BibtexDatabase onDisk = pr.getDatabase();
    MetaData mdOnDisk = new MetaData(pr.getMetaData());

    //Util.pr(f.getPath()+": "+onDisk.getEntryCount());

    // Sort both databases according to a common sort key.
    EntrySorter sInTemp = inTemp.getSorter(new EntryComparator(
      true, true, true, sortBy[0], sortBy[1], sortBy[2]));
    EntrySorter sOnDisk = onDisk.getSorter(new EntryComparator(
      true, true, true, sortBy[0], sortBy[1], sortBy[2]));
    EntrySorter sInMem = inMem.getSorter(new EntryComparator(
      true, true, true, sortBy[0], sortBy[1], sortBy[2]));

    // Start looking at changes.
    scanPreamble(inMem, inTemp, onDisk);
    scanStrings(inMem, inTemp, onDisk);
    scanEntries(sInMem, sInTemp, sOnDisk);
    scanGroups(mdInMem, mdInTemp, mdOnDisk);

    if (changes.getChildCount() > 0) {
      ChangeDisplayDialog dial = new ChangeDisplayDialog(frame, panel, changes);
      Util.placeDialog(dial, frame);
      dial.show();
    } else {
      JOptionPane.showMessageDialog(frame, Globals.lang("No actual changes found."),
                                    Globals.lang("External changes"), JOptionPane.INFORMATION_MESSAGE);
    }


  }

  private void scanEntries(EntrySorter mem, EntrySorter tmp, EntrySorter disk) {

    // Create pointers that are incremented as the entries of each base are used in
    // successive order from the beginning. Entries "further down" in the "disk" base
    // can also be matched.
    int piv1 = 0, piv2 = 0;

    // Create a HashSet where we can put references to entry numbers in the "disk"
    // database that we have matched. This is to avoid matching them twice.
    HashSet used = new HashSet(disk.getEntryCount());
    HashSet notMatched = new HashSet(tmp.getEntryCount());

    // Loop through the entries of the "mem" database, looking for exact matches in the "disk" one.
    // We must finish scanning for exact matches before looking for near matches, to avoid an exact
    // match being "stolen" from another entry.
    mainLoop: for (piv1=0; piv1<tmp.getEntryCount(); piv1++) {

      // First check if the similarly placed entry in the other base matches exactly.
      double comp = -1;
      if (!used.contains(""+piv2)) {
        comp = Util.compareEntriesStrictly(tmp.getEntryAt(piv1),
                                           disk.getEntryAt(piv2));
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
            comp = Util.compareEntriesStrictly(tmp.getEntryAt(piv1), disk.getEntryAt(i));
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
      //Util.pr("Could not find exact match for "+notMatched.size()+" entries.");

      fuzzyLoop: for (Iterator it=notMatched.iterator(); it.hasNext();) {

        Integer integ = (Integer)it.next();
        piv1 = integ.intValue();

        //Util.printEntry(mem.getEntryAt(piv1));

        // These two variables will keep track of which entry most closely matches the
        // one we're looking at, in case none matches completely.
        int bestMatchI = -1;
        double bestMatch = 0;
        double comp = -1;

        if (piv2 < disk.getEntryCount()-1) {
          for (int i = piv2; i < disk.getEntryCount(); i++) {
            //Util.pr("This one? "+i);
            if (!used.contains(""+i)) {
              //Util.pr("Fuzzy matching for entry: "+piv1+" - "+i);
              comp = Util.compareEntriesStrictly(tmp.getEntryAt(piv1),
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
          //Util.printEntry(mem.getEntryAt(piv1));
          //System.out.println("----------------------------------------------");
          //Util.printEntry(disk.getEntryAt(bestMatchI));
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
    // can be assumed to have been added.
    if (used.size() < disk.getEntryCount()) {
      for (int i=0; i<disk.getEntryCount(); i++) {
        if (!used.contains(""+i)) {
          EntryAddChange ec = new EntryAddChange(disk.getEntryAt(i));
          changes.add(ec);
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
      double res = Util.compareEntriesStrictly(old.getEntryAt(index),
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

    HashSet used = new HashSet();
    HashSet usedInMem = new HashSet();
    HashSet notMatched = new HashSet(onTmp.getStringCount());

    // First try to match by string names.
    //int piv2 = -1;
    mainLoop: for (Iterator i=onTmp.getStringKeySet().iterator(); i.hasNext();) {
	Object tmpId = i.next();
      BibtexString tmp = onTmp.getString(tmpId);
      //      for (int j=piv2+1; j<nDisk; j++)
      for (Iterator j=onDisk.getStringKeySet().iterator(); j.hasNext();) {
	  Object diskId = i.next();
        if (!used.contains(diskId)) {
          BibtexString disk = onDisk.getString(j.next());
          if (disk.getName().equals(tmp.getName())) {
            // We have found a string with a matching name.
            if ((tmp.getContent() != null) && !tmp.getContent().equals(disk.getContent())) {
              // But they have nonmatching contents, so we've found a change.
              BibtexString mem = findString(inMem, tmp.getName(), usedInMem);
              if (mem != null)
                changes.add(new StringChange(mem, tmp.getName(),
                                             mem.getContent(),
                                             tmp.getContent(), disk.getContent()));
              else
		  changes.add(new StringChange(null, tmp.getName(), null, tmp.getContent(), disk.getContent()));
            }
            used.add(disk.getId());
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
      for (Iterator i = notMatched.iterator(); i.hasNext(); ) {
	  Object nmId = i.next();
        BibtexString tmp = onTmp.getString(nmId);

        // If we get to this point, we found no string with matching name. See if we
        // can find one with matching content.
        String tmpContent = tmp.getContent();
	//for (Iterator i=onTmp.getStringKeySet().iterator(); i.hasNext();) {
	for (Iterator j=onDisk.getStringKeySet().iterator(); j.hasNext();) {
	    Object diskId = j.next(); 
	    //for (int j = piv2 + 1; j < nDisk; j++)
          if (!used.contains(diskId)) {
            BibtexString disk = onDisk.getString(diskId);
            if (disk.getContent().equals(tmp.getContent())) {
              // We have found a string with the same content. It cannot have the same
              // name, or we would have found it above.

              // Try to find the matching one in memory:
              BibtexString bsMem = null;
              findInMem: for (Iterator k=inMem.getStringKeySet().iterator(); k.hasNext();) {
		  Object memId = k.next();
	      //for (int k = 0; k < inMem.getStringCount(); k++) {
                BibtexString bsMem_cand = inMem.getString(memId);
                if (bsMem_cand.getContent().equals(disk.getContent()) &&
                    !usedInMem.contains(memId)) {
                  usedInMem.add(memId);
                  bsMem = bsMem_cand;
                  break findInMem;
                }
              }

              changes.add(new StringNameChange(bsMem, bsMem.getName(),
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
      for (Iterator i = notMatched.iterator(); i.hasNext(); ) {
	  Object nmId = i.next();
        BibtexString tmp = onTmp.getString(nmId);
        BibtexString mem = findString(inMem, tmp.getName(), usedInMem);
        if (mem != null) { // The removed string is not removed from the mem version.
          changes.add(new StringRemoveChange(tmp, mem));
        }
      }
    }

    System.out.println(used.size());

    // Finally, see if there are remaining strings in the disk database. They
    // must have been added.
    for (Iterator i=onDisk.getStringKeySet().iterator(); i.hasNext();) {
	Object diskId = i.next();
	if (!used.contains(diskId)) {
	    BibtexString disk = onDisk.getString(diskId);
	    used.remove(diskId);
	    changes.add(new StringAddChange(disk));
	}
    }
  }

    private BibtexString findString(BibtexDatabase base, String name, HashSet used) {
     if (!base.hasStringLabel(name))
	 return null;
     for (Iterator i=base.getStringKeySet().iterator(); i.hasNext();) {
      BibtexString bs = base.getString(i.next());
      if (bs.getName().equals(name) && !used.contains(""+i)) {
        used.add(""+i);
        return bs;
      }
    }
    return null;
  }

  public void scanGroups(MetaData inMem, MetaData onTmp, MetaData onDisk) {
    Vector vInMem = inMem.getData("groups");
    Vector vOnTmp = onTmp.getData("groups");
    Vector vOnDisk = onDisk.getData("groups");

    if (((vOnTmp == null) || (vOnTmp.size()==0)) && ((vOnDisk == null) || (vOnDisk.size()==0))) {
      // No groups defined in either the tmp or disk version.
      return;
    }

    // To avoid checking for null all the time, make empty vectors to replace null refs. We clone
    // non-null vectors so we can remove the elements as we finish with them.
    if (vOnDisk == null)
      vOnDisk = new Vector(0);
    else
      vOnDisk = (Vector)vOnDisk.clone();
    if (vOnTmp == null)
      vOnTmp = new Vector(0);
    else
      vOnTmp = (Vector)vOnTmp.clone();
    if (vInMem == null)
      vInMem = new Vector(0);
    else
      vInMem = (Vector)vInMem.clone();

    // If the tmp version has groups, iterate through these and compare with disk version:
    while (vOnTmp.size() >= GroupSelector.OFFSET+GroupSelector.DIM) {
      String field = (String)vOnTmp.elementAt(GroupSelector.OFFSET);
      vOnTmp.removeElementAt(GroupSelector.OFFSET);
      String name = (String)vOnTmp.elementAt(GroupSelector.OFFSET);
      vOnTmp.removeElementAt(GroupSelector.OFFSET);
      String regexp = (String)vOnTmp.elementAt(GroupSelector.OFFSET);
      vOnTmp.removeElementAt(GroupSelector.OFFSET);
      //Util.pr("Name: "+name+"\nField: "+field+"\nRegex: "+regexp);
      int pos = Util.findGroup(name, vOnDisk);
      if (pos == -1) {
        // Couldn't find the group.
        changes.add(new GroupAddOrRemove(field, name, regexp, false));
      } else {
        String diskField = (String)vOnDisk.elementAt(pos);
        String diskRegexp = (String)vOnDisk.elementAt(pos+2);

        if (!diskField.equals(field) || !diskRegexp.equals(regexp)) {
          // Group has changed.
          changes.add(new GroupChange(inMem, name, field, diskField, regexp, diskRegexp));
        }

        // Remove this group, since it's been accounted for.
        vOnDisk.remove(pos);
        vOnDisk.remove(pos);
        vOnDisk.remove(pos);
      }
    }

    // If there are entries left in the disk version, these must have been added.
    while (vOnDisk.size() >= GroupSelector.OFFSET+GroupSelector.DIM) {
      String field = (String)vOnDisk.elementAt(GroupSelector.OFFSET);
      vOnDisk.removeElementAt(GroupSelector.OFFSET);
      String name = (String)vOnDisk.elementAt(GroupSelector.OFFSET);
      vOnDisk.removeElementAt(GroupSelector.OFFSET);
      String regexp = (String)vOnDisk.elementAt(GroupSelector.OFFSET);
      vOnDisk.removeElementAt(GroupSelector.OFFSET);
      changes.add(new GroupAddOrRemove(field, name, regexp, true));
    }
  }

}
