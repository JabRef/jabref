package net.sf.jabref.imports;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import net.sf.jabref.BaseAction;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexString;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.KeyCollisionException;
import net.sf.jabref.MergeDialog;
import net.sf.jabref.MetaData;
import net.sf.jabref.Util;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.groups.AbstractGroup;
import net.sf.jabref.groups.AllEntriesGroup;
import net.sf.jabref.groups.ExplicitGroup;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertEntry;
import net.sf.jabref.undo.UndoableInsertString;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: May 18, 2006
 * Time: 9:49:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class AppendDatabaseAction extends BaseAction {
    private JabRefFrame frame;
    private BasePanel panel;
    private List<File> filesToOpen = new ArrayList<File>();

    public AppendDatabaseAction(JabRefFrame frame, BasePanel panel) {
        this.frame = frame;
        this.panel = panel;
    }

    public void action() {

        filesToOpen.clear();
        final MergeDialog md = new MergeDialog(frame, Globals.lang("Append database"), true);
        Util.placeDialog(md, panel);
        md.setVisible(true);
        if (md.isOkPressed()) {
            String[] chosen = FileDialogs.getMultipleFiles(frame, new File(Globals.prefs.get("workingDirectory")),
                    null, false);
          //String chosenFile = Globals.getNewFile(frame, new File(Globals.prefs.get("workingDirectory")),
          //                                       null, JFileChooser.OPEN_DIALOG, false);
          if(chosen == null)
            return;
          for (int i=0; i<chosen.length; i++)
            filesToOpen.add(new File(chosen[i]));

            // Run the actual open in a thread to prevent the program
            // locking until the file is loaded.
            (new Thread() {
                public void run() {
                    openIt(md.importEntries(), md.importStrings(),
                            md.importGroups(), md.importSelectorWords());
                }
            }).start();
            //frame.getFileHistory().newFile(panel.fileToOpen.getPath());
        }

      }

    void openIt(boolean importEntries, boolean importStrings,
                boolean importGroups, boolean importSelectorWords) {
        if (filesToOpen.size() == 0)
            return;
        for (Iterator<File> i = filesToOpen.iterator(); i.hasNext();) {
            File file = i.next();
            try {
                Globals.prefs.put("workingDirectory", file.getPath());
                // Should this be done _after_ we know it was successfully opened?
                String encoding = Globals.prefs.get("defaultEncoding");
                ParserResult pr = OpenDatabaseAction.loadDatabase(file, encoding);
                mergeFromBibtex(frame, panel, pr, importEntries, importStrings,
                        importGroups, importSelectorWords);
                panel.output(Globals.lang("Imported from database") + " '" + file.getPath() + "'");
            } catch (Throwable ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog
                        (panel, ex.getMessage(),
                                "Open database", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

	public static void mergeFromBibtex(JabRefFrame frame, BasePanel panel, ParserResult pr,
                                boolean importEntries, boolean importStrings,
                                boolean importGroups, boolean importSelectorWords)
              throws KeyCollisionException {

          BibtexDatabase fromDatabase = pr.getDatabase();
          ArrayList<BibtexEntry> appendedEntries = new ArrayList<BibtexEntry>();
          ArrayList<BibtexEntry> originalEntries = new ArrayList<BibtexEntry>();
          BibtexDatabase database = panel.database();
          BibtexEntry originalEntry;
          NamedCompound ce = new NamedCompound(Globals.lang("Append database"));
          MetaData meta = new MetaData(pr.getMetaData(), pr.getDatabase());

          if (importEntries) { // Add entries
              boolean overwriteOwner = Globals.prefs.getBoolean("overwriteOwner");
              boolean overwriteTimeStamp = Globals.prefs.getBoolean("overwriteTimeStamp");

        	  for (String key : fromDatabase.getKeySet()){
        	      originalEntry = fromDatabase.getEntryById(key);
                  BibtexEntry be = (BibtexEntry) (originalEntry.clone());
                  be.setId(Util.createNeutralId());
                  Util.setAutomaticFields(be, overwriteOwner, overwriteTimeStamp);
                  database.insertEntry(be);
                  appendedEntries.add(be);
                  originalEntries.add(originalEntry);
                  ce.addEdit(new UndoableInsertEntry(database, be, panel));
              }
          }

          if (importStrings) {
              for (BibtexString bs : fromDatabase.getStringValues()){
                  if (!database.hasStringLabel(bs.getName())) {
                      database.addString(bs);
                      ce.addEdit(new UndoableInsertString(panel, database, bs));
                  }
              }
          }

          if (importGroups) {
              GroupTreeNode newGroups = meta.getGroups();
              if (newGroups != null) {

                  // ensure that there is always only one AllEntriesGroup
                  if (newGroups.getGroup() instanceof AllEntriesGroup) {
                      // create a dummy group
                      ExplicitGroup group = new ExplicitGroup("Imported",
                              AbstractGroup.INDEPENDENT); // JZTODO lyrics
                      newGroups.setGroup(group);
                      for (int i = 0; i < appendedEntries.size(); ++i)
                          group.addEntry(appendedEntries.get(i));
                  }

                  // groupsSelector is always created, even when no groups
                  // have been defined. therefore, no check for null is
                  // required here
                  frame.groupSelector.addGroups(newGroups, ce);
                  // for explicit groups, the entries copied to the mother fromDatabase have to
                  // be "reassigned", i.e. the old reference is removed and the reference
                  // to the new fromDatabase is added.
                  GroupTreeNode node;
                  ExplicitGroup group;
                  BibtexEntry entry;
                  
                  for (Enumeration<GroupTreeNode> e = newGroups
					.preorderEnumeration(); e.hasMoreElements();) {
					node = e.nextElement();
					if (!(node.getGroup() instanceof ExplicitGroup))
						continue;
					group = (ExplicitGroup) node.getGroup();
					for (int i = 0; i < originalEntries.size(); ++i) {
						entry = originalEntries.get(i);
						if (group.contains(entry)) {
							group.removeEntry(entry);
							group.addEntry(appendedEntries.get(i));
						}
					}
				}
                  frame.groupSelector.revalidateGroups();
              }
          }

          if (importSelectorWords) {
        	  for (String s : meta){
                  if (s.startsWith(Globals.SELECTOR_META_PREFIX)) {
                      panel.metaData().putData(s, meta.getData(s));
                  }
              }
          }

          ce.end();
          panel.undoManager.addEdit(ce);
          panel.markBaseChanged();
      }


}
