/*
Copyright (C) 2003 Morten O. Alver and Nizar N. Batada

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/

package net.sf.jabref;

import java.io.*;
import java.util.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.undo.*;

import net.sf.jabref.export.*;
import net.sf.jabref.groups.*;
import net.sf.jabref.imports.*;
import net.sf.jabref.labelPattern.*;
import net.sf.jabref.undo.*;

public class BasePanel extends JSplitPane implements ClipboardOwner {

    BasePanel ths = this;
    JSplitPane splitPane;
    PreviewPanel previewPanel = null;

    JabRefFrame frame;
    BibtexDatabase database;
    JabRefPreferences prefs;
    // The database shown in this panel.
    File file = null,
	fileToOpen = null; // The filename of the database.

    //Hashtable autoCompleters = new Hashtable();
    // Hashtable that holds as keys the names of the fields where
    // autocomplete is active, and references to the autocompleter objects.

    // The undo manager.
    public CountingUndoManager undoManager = new CountingUndoManager();
    UndoAction undoAction = new UndoAction();
    RedoAction redoAction = new RedoAction();

    //ExampleFileFilter fileFilter;
    // File filter for .bib files.

    boolean baseChanged = false, nonUndoableChange = false;
    // Used to track whether the base has changed since last save.

    EntryTableModel tableModel = null;
    EntryTable entryTable = null;

    // The sidepane manager takes care of populating the sidepane.
    SidePaneManager sidePaneManager;

    SearchManager2 searchManager;
    MedlineFetcher medlineFetcher;
    MedlineAuthorFetcher medlineAuthorFetcher;
    RightClickMenu rcm;

    BibtexEntry showing = null;
    // To indicate which entry is currently shown.
    HashMap entryEditors = new HashMap();
    // To contain instantiated entry editors. This is to save time
    // in switching between entries.

    //HashMap entryTypeForms = new HashMap();
    // Hashmap to keep track of which entries currently have open
    // EntryTypeForm dialogs.

    PreambleEditor preambleEditor = null;
    // Keeps track of the preamble dialog if it is open.

    StringDialog stringDialog = null;
    // Keeps track of the string dialog if it is open.

    GroupSelector groupSelector;
    // The group selector component for this database. Instantiated by the SidePaneManager if necessary,
    // or from this class if merging groups from a different database.

    boolean showingSearchResults = false,
	showingGroup = false,
        previewEnabled = true;

    // MetaData parses, keeps and writes meta data.
    MetaData metaData;
    HashMap fieldExtras = new HashMap();
    //## keep track of all keys for duplicate key warning and unique key generation
    //private HashMap allKeys  = new HashMap();	// use a map instead of a set since i need to know how many of each key is inthere

    private boolean suppressOutput = false;

    private HashMap actions = new HashMap();

    public BasePanel(JabRefFrame frame, JabRefPreferences prefs) {
	database = new BibtexDatabase();
	metaData = new MetaData();
	this.frame = frame;
	this.prefs = prefs;
	setupActions();
	setupMainPanel();
    }

    public BasePanel(JabRefFrame frame, BibtexDatabase db, File file,
		     HashMap meta, JabRefPreferences prefs) {
	super(JSplitPane.HORIZONTAL_SPLIT, true);
	this.frame = frame;
        database = db;
	this.prefs = prefs;
	parseMetaData(meta);
	setupActions();
	setupMainPanel();
	/*if (prefs.getBoolean("autoComplete")) {
	    db.setCompleters(autoCompleters);
	    }*/

        this.file = file;

    }

    public BibtexDatabase database() { return database; }
    public JabRefFrame frame() { return frame; }

    public void output(String s) {
	if (!suppressOutput)
	    frame.output(s);
    }

    /**
     * BaseAction is used to define actions that are called from the
     * base frame through runCommand(). runCommand() finds the
     * appropriate BaseAction object, and runs its action() method.
     */
    abstract class BaseAction {
	abstract void action() throws Throwable;
    }

    private void setupActions() {

	actions.put("undo", undoAction);
	actions.put("redo", redoAction);

	// The action for opening an entry editor.
	actions.put("edit", new BaseAction() {
		public void action() {
		    int clickedOn = -1;
		    // We demand that one and only one row is selected.
		    if (entryTable.getSelectedRowCount() == 1) {
			clickedOn = entryTable.getSelectedRow();
		    }
		    if (clickedOn >= 0) {
			String id =  tableModel.getNameFromNumber(clickedOn);
			BibtexEntry be = database.getEntryById(id);
			showEntry(be);
		    }
		}

	    });

	// The action for saving a database.
	actions.put("save", new BaseAction() {
		public void action() throws Throwable {
		    if (file == null)
			runCommand("saveAs");
		    else {
			saveDatabase(file, false);
			undoManager.markUnchanged();
			// (Only) after a successful save the following
			// statement marks that the base is unchanged
			// since last save:
			nonUndoableChange = false;
			baseChanged = false;
			frame.setTabTitle(ths, file.getName());
			frame.output(Globals.lang("Saved database")+" '"
				     +file.getPath()+"'.");
		    }
		}
	    });

	actions.put("saveAs", new BaseAction () {
		public void action() throws Throwable {

                  String chosenFile = Globals.getNewFile(frame, prefs, new File(prefs.get("workingDirectory")), ".bib",
                                                         JFileChooser.SAVE_DIALOG, false);

                  if (chosenFile != null) {
                    file = new File(chosenFile);
                    if (!file.exists() ||
                        (JOptionPane.showConfirmDialog
                         (frame, "'"+file.getName()+"' "+Globals.lang("exists. Overwrite file?"),
                          Globals.lang("Save database"), JOptionPane.OK_CANCEL_OPTION)
                         == JOptionPane.OK_OPTION)) {
                      runCommand("save");
                      prefs.put("workingDirectory", file.getParent());
                      frame.fileHistory.newFile(file.getPath());
                    }
                    else
                      file = null;
		    }
		}
	    });

	actions.put("saveSelectedAs", new BaseAction () {
		public void action() throws Throwable {

                  String chosenFile = Globals.getNewFile(frame, prefs, new File(prefs.get("workingDirectory")), ".bib",
                                                         JFileChooser.SAVE_DIALOG, false);
                  if (chosenFile != null) {
                    File expFile = new File(chosenFile);
                    if (!expFile.exists() ||
                        (JOptionPane.showConfirmDialog
                         (frame, "'"+expFile.getName()+"' "+
                          Globals.lang("exists. Overwrite file?"),
                          Globals.lang("Save database"), JOptionPane.OK_CANCEL_OPTION)
                         == JOptionPane.OK_OPTION)) {
                      saveDatabase(expFile, true);
                      //runCommand("save");
                      frame.fileHistory.newFile(expFile.getPath());
                      frame.output(Globals.lang("Saved selected to")+" '"
                                   +expFile.getPath()+"'.");
			}
		    }
		}
	    });

	// The action for copying selected entries.
	actions.put("copy", new BaseAction() {
		public void action() {
		    BibtexEntry[] bes = entryTable.getSelectedEntries();

		    if ((bes != null) && (bes.length > 0)) {
			TransferableBibtexEntry trbe
			    = new TransferableBibtexEntry(bes);
			Toolkit.getDefaultToolkit().getSystemClipboard()
			    .setContents(trbe, ths);
			output(Globals.lang("Copied")+" "+(bes.length>1 ? bes.length+" "
							   +Globals.lang("entries")
							   : "1 "+Globals.lang("entry")+"."));
		    } else {
			// The user maybe selected a single cell.
			int[] rows = entryTable.getSelectedRows(),
			    cols = entryTable.getSelectedColumns();
			if ((cols.length == 1) && (rows.length == 1)) {
			    // Copy single value.
			    Object o = tableModel.getValueAt(rows[0], cols[0]);
			    if (o != null) {
				StringSelection ss = new StringSelection(o.toString());
				Toolkit.getDefaultToolkit().getSystemClipboard()
				    .setContents(ss, ths);

				output(Globals.lang("Copied cell contents")+".");
			    }
			}
		    }
		}
	    });

	actions.put("cut", new BaseAction() {
		public void action() throws Throwable {
		    runCommand("copy");
		    BibtexEntry[] bes = entryTable.getSelectedEntries();
		    if ((bes != null) && (bes.length > 0)) {
			// Create a CompoundEdit to make the action undoable.
			NamedCompound ce = new NamedCompound
			    (bes.length > 1 ? "cut entries" : "cut entry");
			// Loop through the array of entries, and delete them.
			for (int i=0; i<bes.length; i++) {
			    database.removeEntry(bes[i].getId());
			    ensureNotShowing(bes[i]);
			    ce.addEdit(new UndoableRemoveEntry
				       (database, bes[i], ths));
			}
			entryTable.clearSelection();
			frame.output(Globals.lang("Cut_pr")+" "+
				     (bes.length>1 ? bes.length
				      +" "+ Globals.lang("entries")
				      : Globals.lang("entry"))+".");
			ce.end();
			undoManager.addEdit(ce);
			refreshTable();
			markBaseChanged();
		    }
		}
	    });

	actions.put("delete", new BaseAction() {
		public void action() {
		    BibtexEntry[] bes = entryTable.getSelectedEntries();

		    if (bes.length > 0) {
			//&& (database.getEntryCount() > 0) && (entryTable.getSelectedRow() < database.getEntryCount())) {

			/*
			   I have removed the confirmation dialog, since I converted
			   the "remove" action to a "cut". That means the user can
			   always paste the entries, in addition to pressing undo.
			   So the confirmation seems redundant.

			String msg = Globals.lang("Really delete the selected")
			    +" "+Globals.lang("entry")+"?",
			    title = Globals.lang("Delete entry");
			if (rows.length > 1) {
			    msg = Globals.lang("Really delete the selected")
				+" "+rows.length+" "+Globals.lang("entries")+"?";
			    title = Globals.lang("Delete multiple entries");
			}
			int answer = JOptionPane.showConfirmDialog(frame, msg, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (answer == JOptionPane.YES_OPTION) {*/

			// Create a CompoundEdit to make the action undoable.
			NamedCompound ce = new NamedCompound
			    (bes.length > 1 ? Globals.lang("delete entries")
			     : Globals.lang("delete entry"));
			// Loop through the array of entries, and delete them.
			for (int i=0; i<bes.length; i++) {
			    database.removeEntry(bes[i].getId());
			    ensureNotShowing(bes[i]);
			    ce.addEdit(new UndoableRemoveEntry(database, bes[i], ths));
			}
			entryTable.clearSelection();
			frame.output(Globals.lang("Deleted")+" "+
				     (bes.length>1 ? bes.length
				      +" "+ Globals.lang("entries")
				      : Globals.lang("entry"))+".");
			ce.end();
			undoManager.addEdit(ce);
			refreshTable();
			markBaseChanged();
		    }
		}



	    });

	// The action for pasting entries or cell contents.
        // Edited by Seb Wills <saw27@mrao.cam.ac.uk> on 14-Apr-04:
        //  - more robust detection of available content flavors (doesn't only look at first one offered)
        //  - support for parsing string-flavor clipboard contents which are bibtex entries.
        //    This allows you to (a) paste entire bibtex entries from a text editor, web browser, etc
        //                       (b) copy and paste entries between multiple instances of JabRef (since
        //         only the text representation seems to get as far as the X clipboard, at least on my system)
	actions.put("paste", new BaseAction() {
		public void action() {
                    // Get clipboard contents, and see if TransferableBibtexEntry is among the content flavors offered
		    Transferable content = Toolkit.getDefaultToolkit()
			.getSystemClipboard().getContents(null);
		    if (content != null) {
                        BibtexEntry[] bes = null;
			if (content.isDataFlavorSupported(TransferableBibtexEntry.entryFlavor)) {
			    // We have determined that the clipboard data is a set of entries.
			    try {
				bes = (BibtexEntry[])(content.getTransferData(TransferableBibtexEntry.entryFlavor));
			    } catch (UnsupportedFlavorException ex) {
				ex.printStackTrace();
			    } catch (IOException ex) {
				ex.printStackTrace();
			    }
			} else if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                            // We have determined that no TransferableBibtexEntry is available, but
                            // there is a string, which we will handle according to context:
                            int[] rows = entryTable.getSelectedRows(),
                                cols = entryTable.getSelectedColumns();
                            Util.pr(rows.length+" x "+cols.length);
                            if ((cols != null) && (cols.length == 1) && (cols[0] != 0)
                                && (rows != null) && (rows.length == 1)) {
                                // A single cell is highlighted, so paste the string straight into it without parsing
                                try {
                                    tableModel.setValueAt((String)(content.getTransferData(DataFlavor.stringFlavor)), rows[0], cols[0]);
                                    refreshTable();
                                    markBaseChanged();
                                    output("Pasted cell contents");
                                } catch (UnsupportedFlavorException ex) {
                                    ex.printStackTrace();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                } catch (IllegalArgumentException ex) {
                                    output("Can't paste.");
                                }
                            } else {
                              // no single cell is selected, so try parsing the clipboard contents as bibtex entries instead
                              try {
                                  BibtexParser bp = new BibtexParser
                                      (new java.io.StringReader( (String) (content.getTransferData(
                                      DataFlavor.stringFlavor))));
                                  BibtexDatabase db = bp.parse().getDatabase();
                                  Util.pr("Parsed " + db.getEntryCount() + " entries from clipboard text");
                                  if(db.getEntryCount()>0) {
                                      Set keySet = db.getKeySet();
                                      if (keySet != null) {
                                          // Copy references to the entries into a BibtexEntry array.
                                          // Could import directly from db, but going via bes allows re-use
                                          // of the same pasting code as used for TransferableBibtexEntries
                                          bes = new BibtexEntry[db.getEntryCount()];
                                          Iterator it = keySet.iterator();
                                          for (int i=0; it.hasNext();i++) {
                                              bes[i]=db.getEntryById((String) (it.next()));
                                          }
                                      }
                                  } else {
                                      output(Globals.lang("Unable to parse clipboard text as Bibtex entries."));
                                  }
                              } catch (UnsupportedFlavorException ex) {
                                  ex.printStackTrace();
                              } catch (Throwable ex) {
                                  ex.printStackTrace();
                              }
                            }
			}

                        // finally we paste in the entries (if any), which either came from TransferableBibtexEntries
                        // or were parsed from a string
                        if ((bes != null) && (bes.length > 0)) {
                          NamedCompound ce = new NamedCompound
                              (bes.length > 1 ? "paste entries" : "paste entry");
                          for (int i=0; i<bes.length; i++) {
                            try {
                              BibtexEntry be = (BibtexEntry)(bes[i].clone());
                              // We have to clone the
                              // entries, since the pasted
                              // entries must exist
                              // independently of the copied
                              // ones.
                              be.setId(Util.createId(be.getType(), database));
                              database.insertEntry(be);
                              ce.addEdit(new UndoableInsertEntry
                                         (database, be, ths));
                            } catch (KeyCollisionException ex) {
                              Util.pr("KeyCollisionException... this shouldn't happen.");
                            }
                          }
                          ce.end();
                          undoManager.addEdit(ce);
                          tableModel.remap();
                          entryTable.clearSelection();
                          entryTable.revalidate();
                          output(Globals.lang("Pasted")+" "+
                                 (bes.length>1 ? bes.length+" "+
                                  Globals.lang("entries") : "1 "+Globals.lang("entry"))
                                 +".");
                          refreshTable();
                          markBaseChanged();
                        }
                      }
                      /*Util.pr(flavor.length+"");
                          Util.pr(flavor[0].toString());
                          Util.pr(flavor[1].toString());
                          Util.pr(flavor[2].toString());
                          Util.pr(flavor[3].toString());
                          Util.pr(flavor[4].toString());
                       */

                    }

});

	actions.put("selectAll", new BaseAction() {
		public void action() {
		    entryTable.selectAll();
		}
	    });

	// The action for opening the preamble editor
	actions.put("editPreamble", new BaseAction() {
		public void action() {
		    if (preambleEditor == null) {
			PreambleEditor form = new PreambleEditor
			    (frame, ths, database, prefs);
			Util.placeDialog(form, frame);
			form.setVisible(true);
			preambleEditor = form;
		    } else {
			preambleEditor.setVisible(true);
		    }

		}
	    });

	// The action for opening the string editor
	actions.put("editStrings", new BaseAction() {
		public void action() {
		    if (stringDialog == null) {
			StringDialog form = new StringDialog
			    (frame, ths, database, prefs);
			Util.placeDialog(form, frame);
			form.setVisible(true);
			stringDialog = form;
		    } else {
			stringDialog.setVisible(true);
		    }

		}
	    });

	// The action for toggling the groups interface
	actions.put("toggleGroups", new BaseAction() {
		public void action() {
		    sidePaneManager.togglePanel("groups");
		}
	    });

	actions.put("pushToLyX",new BaseAction(){
		public void action(){
		    int[] rows = entryTable.getSelectedRows();
		    int numSelected = rows.length;
		    BibtexEntry bes = null;
		    // Globals.logger("Pushing " +numSelected+(numSelected>1? " entries" : "entry") + " to LyX");
		    // check if lyxpipe is defined
		    File lyxpipe = new File( prefs.get("lyxpipe") +".in"); // this needs to fixed because it gives "asdf" when going prefs.get("lyxpipe")
		    if( !lyxpipe.exists() || !lyxpipe.canWrite()){
			output("ERROR: verify that LyX is running and that the lyxpipe is valid. [" + prefs.get("lyxpipe") +"]");
			return;
		    }
		    if( numSelected > 0){
			try {
			    BufferedWriter lyx_out = new BufferedWriter(new FileWriter(lyxpipe));
			    String citeStr="", citeKey="", message="";
			    for(int i=0; i< numSelected; i++){
				bes = database.getEntryById( tableModel.getNameFromNumber( rows[i] ));
				citeKey= (String)bes.getField(GUIGlobals.KEY_FIELD);
				// if the key is empty we give a warning and ignore this entry
				if(citeKey==null || citeKey.equals(""))
				    continue;
				if(citeStr.equals(""))
				    citeStr= citeKey;
				else
				    citeStr += "," + citeKey;
				message+= ", " + rows[i];
			    }
			    if(citeStr.equals(""))
				output("Please define citekey first");
			    else{
				citeStr="LYXCMD:sampleclient:citation-insert:"+ citeStr;
				lyx_out.write(citeStr +"\n");
				output("Pushed the citations for the following rows to Lyx: " + message);
			    }
			    lyx_out.close();

			}
			catch (IOException excep) {
			    output("ERROR: unable to write to " + prefs.get("lyxpipe") +".in");
			}
		    }
		}
	    });
	// The action for auto-generating keys.
	actions.put("makeKey", new BaseAction() {
		public void action() {
		    int[] rows = entryTable.getSelectedRows() ;
		    int numSelected = rows.length ;
		    BibtexEntry bes = null ;

		    /*if (numSelected > 0) {
			int answer = JOptionPane.showConfirmDialog
			    (frame, "Generate bibtex key"+
			     (numSelected>1 ? "s for the selected "
			      +numSelected+" entries?" :
			      " for the selected entry?"),
			     "Autogenerate Bibtexkey",
			     JOptionPane.YES_NO_CANCEL_OPTION);
			if (answer != JOptionPane.YES_OPTION) {
			    return ;

			    }
			*/
		    if (numSelected == 0) { // None selected. Inform the user to select entries first.
			JOptionPane.showMessageDialog(frame, Globals.lang("First select the entries you want keys to be generated for."),
						      Globals.lang("Autogenerate BibTeX key"), JOptionPane.INFORMATION_MESSAGE);
			return ;
		    }

 		    output(Globals.lang("Generating BibTeX key for")+" "+
			   numSelected+" "+(numSelected>1 ? Globals.lang("entries")
					    : Globals.lang("entry")));

		    NamedCompound ce = new NamedCompound("autogenerate keys");
		    //BibtexEntry be;
		    Object oldValue;
		    for(int i = 0 ; i < numSelected ; i++){
			bes = database.getEntryById(tableModel.getNameFromNumber(rows[i]));
			oldValue = bes.getField(GUIGlobals.KEY_FIELD);
			//bes = frame.labelMaker.applyRule(bes, database) ;
			bes = LabelPatternUtil.makeLabel(prefs.getKeyPattern(), database, bes);
			ce.addEdit(new UndoableKeyChange
				   (database, bes.getId(), (String)oldValue,
				    (String)bes.getField(GUIGlobals.KEY_FIELD)));
		    }
		    ce.end();
		    undoManager.addEdit(ce);
		    markBaseChanged() ;
		    refreshTable() ;

                    output(Globals.lang("Generated BibTeX key for")+" "+
                           numSelected+" "+(numSelected>1 ? Globals.lang("entries")
                                            : Globals.lang("entry")));
		}
	    });

	actions.put("search", new BaseAction() {
		public void action() {
		    sidePaneManager.ensureVisible("search");
		    searchManager.startSearch();
		}
	    });

	actions.put("incSearch", new BaseAction() {
		public void action() {
		    sidePaneManager.ensureVisible("search");
		    searchManager.startIncrementalSearch();
		}
	    });

	// The action for copying the selected entry's key.
	actions.put("copyKey", new BaseAction() {
		public void action() {
		    BibtexEntry[] bes = entryTable.getSelectedEntries();
		    if ((bes != null) && (bes.length > 0)) {
			//String[] keys = new String[bes.length];
			Vector keys = new Vector();
			// Collect all non-null keys.
			for (int i=0; i<bes.length; i++)
			    if (bes[i].getField(Globals.KEY_FIELD) != null)
				keys.add(bes[i].getField(Globals.KEY_FIELD));
			if (keys.size() == 0) {
			    output("None of the selected entries have BibTeX keys.");
			    return;
			}
			StringBuffer sb = new StringBuffer((String)keys.elementAt(0));
			for (int i=1; i<keys.size(); i++) {
			    sb.append(',');
			    sb.append((String)keys.elementAt(i));
			}

			StringSelection ss = new StringSelection(sb.toString());
			Toolkit.getDefaultToolkit().getSystemClipboard()
			    .setContents(ss, ths);

			if (keys.size() == bes.length)
			    // All entries had keys.
			    output(Globals.lang((bes.length > 1) ? "Copied keys"
						: "Copied key")+".");
			else
			    output(Globals.lang("Warning")+": "+(bes.length-keys.size())
				   +" "+Globals.lang("out of")+" "+bes.length+" "+
				   Globals.lang("entries have undefined BibTeX key")+".");
		    }
		}
	    });

	// The action for copying a cite for the selected entry.
	actions.put("copyCiteKey", new BaseAction() {
		public void action() {
		    BibtexEntry[] bes = entryTable.getSelectedEntries();
		    if ((bes != null) && (bes.length > 0)) {
			//String[] keys = new String[bes.length];
			Vector keys = new Vector();
			// Collect all non-null keys.
			for (int i=0; i<bes.length; i++)
			    if (bes[i].getField(Globals.KEY_FIELD) != null)
				keys.add(bes[i].getField(Globals.KEY_FIELD));
			if (keys.size() == 0) {
			    output("None of the selected entries have BibTeX keys.");
			    return;
			}
			StringBuffer sb = new StringBuffer((String)keys.elementAt(0));
			for (int i=1; i<keys.size(); i++) {
			    sb.append(',');
			    sb.append((String)keys.elementAt(i));
			}

			StringSelection ss = new StringSelection
			    ("\\cite{"+sb.toString()+"}");
			Toolkit.getDefaultToolkit().getSystemClipboard()
			    .setContents(ss, ths);

			if (keys.size() == bes.length)
			    // All entries had keys.
			    output(Globals.lang((bes.length > 1) ? "Copied keys"
						: "Copied key")+".");
			else
			    output(Globals.lang("Warning")+": "+(bes.length-keys.size())
				   +" "+Globals.lang("out of")+" "+bes.length+" "+
				   Globals.lang("entries have undefined BibTeX key")+".");
		    }
		}
	    });

          actions.put("mergeDatabase", new BaseAction() {
            public void action() {

                final MergeDialog md = new MergeDialog(frame, Globals.lang("Append database"), true);
                Util.placeDialog(md, ths);
                md.setVisible(true);
		if (md.okPressed) {
                  String chosenFile = Globals.getNewFile(frame, prefs, new File(prefs.get("workingDirectory")),
                                                         null, JFileChooser.OPEN_DIALOG, false);
                  /*JFileChooser chooser = (prefs.get("workingDirectory") == null) ?
                      new JabRefFileChooser((File)null) :
                      new JabRefFileChooser(new File(prefs.get("workingDirectory")));
                  chooser.addChoosableFileFilter( new OpenFileFilter() );//nb nov2
                  int returnVal = chooser.showOpenDialog(ths);*/
                  if(chosenFile == null)
                    return;
                  fileToOpen = new File(chosenFile);

                  // Run the actual open in a thread to prevent the program
                  // locking until the file is loaded.
                  if (fileToOpen != null) {
                    (new Thread() {
                      public void run() {
                        openIt(md.importEntries(), md.importStrings(),
                               md.importGroups(), md.importSelectorWords());
                      }
                    }).start();
                    frame.fileHistory.newFile(fileToOpen.getPath());
                  }
                }
              }

              void openIt(boolean importEntries, boolean importStrings,
                          boolean importGroups, boolean importSelectorWords) {
                if ((fileToOpen != null) && (fileToOpen.exists())) {
                  try {
                    prefs.put("workingDirectory", fileToOpen.getPath());
                    // Should this be done _after_ we know it was successfully opened?

                    ParserResult pr = frame.loadDatabase(fileToOpen);
                    BibtexDatabase db = pr.getDatabase();
                    MetaData meta = new MetaData(pr.getMetaData());
                    NamedCompound ce = new NamedCompound("Append database");

                    if (importEntries) { // Add entries
                      Iterator i = db.getKeySet().iterator();
                      while (i.hasNext()) {
                        BibtexEntry be = (BibtexEntry)(db.getEntryById((String)i.next()).clone());
                        be.setId(Util.createNeutralId());
                        database.insertEntry(be);
                        ce.addEdit(new UndoableInsertEntry(database, be, ths));
                      }
                    }

                    if (importStrings) {
                      BibtexString bs;
                      int pos = 0;
                      for (int i=0; i<db.getStringCount(); i++) {
                        bs = (BibtexString)(db.getString(i).clone());
                        if (!database.hasStringLabel(bs.getName())) {
                          pos = database.getStringCount();
                          database.addString(bs, pos);
                          ce.addEdit(new UndoableInsertString(ths, database, bs, pos));
                        }
                      }
                    }

                    if (importGroups) {
                      Vector newGroups = meta.getData("groups");
                      if (newGroups != null) {
                        if (groupSelector == null) {
                          // The current database has no group selector defined, so we must instantiate one.
                          groupSelector = new GroupSelector
                              (frame, ths, new Vector(), sidePaneManager, prefs);
                          sidePaneManager.register("groups", groupSelector);
                        }

                        groupSelector.addGroups(newGroups, ce);
                        groupSelector.revalidateList();
                      }
                    }

                    if (importSelectorWords) {
                      Iterator i=meta.iterator();
                      while (i.hasNext()) {
                        String s = (String)i.next();
                        if (s.startsWith(Globals.SELECTOR_META_PREFIX)) {
                          metaData.putData(s, meta.getData(s));
                        }
                      }
                    }

                    ce.end();
                    undoManager.addEdit(ce);
                    markBaseChanged();
                    refreshTable();
                    output("Imported from database '"+fileToOpen.getPath()+"':");
                    fileToOpen = null;
                  } catch (Throwable ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog
                        (ths, ex.getMessage(),
                         "Open database", JOptionPane.ERROR_MESSAGE);
                  }
                }
              }
            });

         actions.put("openFile", new BaseAction() {
                 public void action() {
                     BibtexEntry[] bes = entryTable.getSelectedEntries();
                     String field = "ps";
                     if ((bes != null) && (bes.length == 1)) {
                         Object link = bes[0].getField("ps");
                         if (bes[0].getField("pdf") != null) {
                           link = bes[0].getField("pdf");
                           field = "pdf";
                         }
                         String filepath = null;
                         if (link != null) {
                           filepath = link.toString();
                         } else {
                           // see if we can fall back to a filename based on the bibtex key
                           String basefile;
                           Object key = bes[0].getField(Globals.KEY_FIELD);
                           if(key != null) {
                             basefile = key.toString();
                             String dir = prefs.get("pdfDirectory");
                             if (dir.endsWith(System.getProperty("file.separator"))) {
                               basefile = dir + basefile;
                             }
                             else {
                               basefile = dir + System.getProperty("file.separator") +
                                   basefile;
                             }
                             final String[] typesToTry = new String[] {
                                 "html", "ps", "pdf"};
                             for (int i = 0; i < typesToTry.length; i++) {
                               File f = new File(basefile + "." + typesToTry[i]);
                               Util.pr("Checking for "+f);
                               if (f.exists()) {
                                 field = typesToTry[i];
                                 filepath = f.getPath();
                                 break;
                               }
                             }
                           }
                         }

                         if (filepath != null) {
                           //output(Globals.lang("Calling external viewer..."));
                           try {
                             Util.pr("Opening external "+field+" file '"+filepath+"'.");
                             Util.openExternalViewer(filepath, field, prefs);
                             output(Globals.lang("External viewer called")+".");
                           } catch (IOException ex) {
                             output(Globals.lang("Error: check your External viewer settings in Preferences")+".");
                           }
                         }
                         else
                             output(Globals.lang("No pdf or ps defined, and no file matching Bibtex key found")+".");
                     } else
                       output(Globals.lang("No entries or multiple entries selected."));
                 }
	      });

              actions.put("openUrl", new BaseAction() {
                      public void action() {
                          BibtexEntry[] bes = entryTable.getSelectedEntries();
                          String field = "doi";
                          if ((bes != null) && (bes.length == 1)) {
                              Object link = bes[0].getField("doi");
                              if (bes[0].getField("url") != null) {
                                link = bes[0].getField("url");
                                field = "url";
                              }
                              if (link != null) {
                                //output(Globals.lang("Calling external viewer..."));
                                try {
                                  Util.openExternalViewer(link.toString(), field, prefs);
                                  output(Globals.lang("External viewer called")+".");
                                } catch (IOException ex) {
                                  output(Globals.lang("Error: check your External viewer settings in Preferences")+".");
                                }
                              }
                              else
                                  output(Globals.lang("No url defined")+".");
                          } else
                            output(Globals.lang("No entries or multiple entries selected."));
                      }
	      });

	  actions.put("replaceAll", new BaseAction() {
	  	  public void action() {
		      ReplaceStringDialog rsd = new ReplaceStringDialog(frame);
		      rsd.show();
		      if (!rsd.okPressed())
			  return;
		      int counter = 0;
		      NamedCompound ce = new NamedCompound("Replace string");
		      if (!rsd.selOnly()) {
			  for (Iterator i=database.getKeySet().iterator();
			       i.hasNext();)
			      counter += rsd.replace(database.getEntryById((String)i.next()), ce);
		      } else {
			  BibtexEntry[] bes = entryTable.getSelectedEntries();
			  for (int i=0; i<bes.length; i++)
			      counter += rsd.replace(bes[i], ce);
		      }
		      output(Globals.lang("Replaced")+" "+counter+" "+
			     Globals.lang(counter==1?"occurence":"occurences")+".");
		      if (counter > 0) {
			  ce.end();
			  undoManager.addEdit(ce);
			  markBaseChanged();
			  refreshTable();
		      }
		  }
	      });

              actions.put("dupliCheck", new BaseAction() {
                public void action() {
                  NamedCompound ce = null;
                  int dupl = 0;
                  output(Globals.lang("Searching for duplicates..."));
                  BibtexEntry[] bes = entryTable.getSelectedEntries();
                  if ((bes == null) || (bes.length < 2))
                    return;
                  DuplicateResolverDialog drd = null;
                  for (int i = 0; i < bes.length - 1; i++)
                    for (int j = i + 1; j < bes.length; j++) {
                      boolean eq = Util.isDuplicate(bes[i], bes[j],
                                                    Globals.duplicateThreshold);

                      // Show the duplicate resolver dialog if they are deemed duplicates,
                      // AND they both are still present in the database.
                      if (eq && (database.getEntryById(bes[i].getId()) != null) &&
                          (database.getEntryById(bes[j].getId()) != null)) {
                        if (drd == null)
                          drd = new DuplicateResolverDialog(frame, bes[i], bes[j]);
                        else
                          drd.setEntries(bes[i], bes[j]);
                        drd.show();
                        //drd.setVisible(true);
                        int answer = drd.getSelected();
                        if (answer == DuplicateResolverDialog.KEEP_UPPER) {
                          if (ce == null) ce = new NamedCompound("duplicate removal");
                          database.removeEntry(bes[j].getId());
                          refreshTable();
                          markBaseChanged();
                          ce.addEdit(new UndoableRemoveEntry(database, bes[j], ths));
                        }
                        else if (answer == DuplicateResolverDialog.KEEP_LOWER) {
                          if (ce == null) ce = new NamedCompound("duplicate removal");
                          database.removeEntry(bes[i].getId());
                          refreshTable();
                          markBaseChanged();
                          ce.addEdit(new UndoableRemoveEntry(database, bes[i], ths));
                        }
                        dupl++;
                        //Util.pr("---------------------------------------------------");
                        //Util.pr("--> "+i+" and "+j+" ...");
                        //Util.pr("---------------------------------------------------");
                      }
                    }
                  if (drd != null)
                    drd.dispose();
                  output(Globals.lang("Duplicate pairs found") + ": " + dupl);

                  if (ce != null) {
                    ce.end();
                    //Util.pr("ox");
                    undoManager.addEdit(ce);
                    //markBaseChanged();
                    //refreshTable();
                  }
                }
              });

              actions.put("markEntries", new BaseAction() {
                public void action() {
                  BibtexEntry[] bes = entryTable.getSelectedEntries();
                  for (int i=0; i<bes.length; i++)
                    bes[i].setField(Globals.MARKED, "0");
                  refreshTable();
                }
              });

              actions.put("unmarkEntries", new BaseAction() {
                public void action() {
                  BibtexEntry[] bes = entryTable.getSelectedEntries();
                  for (int i=0; i<bes.length; i++)
                    bes[i].setField(Globals.MARKED, null);
                  refreshTable();
                }
              });

              actions.put("togglePreview", new BaseAction() {
                  public void action() {
                    previewEnabled = !previewEnabled;
                    if (!previewEnabled)
                      hidePreview();
                    else {
                      BibtexEntry[] bes = entryTable.getSelectedEntries();
                      if ((bes != null) && (bes.length > 0))
                        updateWiewToSelected(bes[0]);
                    }
                  }
                });

    }

    /**
     * This method is called from JabRefFrame is a database specific
     * action is requested by the user. Runs the command if it is
     * defined, or prints an error message to the standard error
     * stream.
     *
     * @param command The name of the command to run.
    */
    public void runCommand(String command) throws Throwable {
	if (actions.get(command) == null)
	    Util.pr("No action defined for'"+command+"'");
	else ((BaseAction)actions.get(command)).action();
    }

    private void saveDatabase(File file, boolean selectedOnly) throws SaveException {
	try {
	    if (!selectedOnly)
		FileActions.saveDatabase(database, metaData, file,
					 prefs, false, false);
	    else
		FileActions.savePartOfDatabase(database, metaData, file,
					       prefs, entryTable.getSelectedEntries());
	} catch (SaveException ex) {
	    if (ex.specificEntry()) {
		// Error occured during processing of
		// be. Highlight it:
		int row = tableModel.getNumberFromName
		    (ex.getEntry().getId()),
		    topShow = Math.max(0, row-3);
		//Util.pr(""+row);
		entryTable.setRowSelectionInterval(row, row);
		entryTable.setColumnSelectionInterval
		    (0, entryTable.getColumnCount()-1);
		entryTable.scrollTo(topShow);
		showEntry(ex.getEntry());
	    }
	    else ex.printStackTrace();

	    JOptionPane.showMessageDialog
		(frame, Globals.lang("Could not save file")
		 +".\n"+ex.getMessage(),
		 Globals.lang("Save database"),
		 JOptionPane.ERROR_MESSAGE);
	    throw new SaveException("rt");
	}
    }


    /**
     * This method is called from JabRefFrame when the user wants to
     * create a new entry. If the argument is null, the user is
     * prompted for an entry type.
     *
     * @param type The type of the entry to create.
     */
    public void newEntry(BibtexEntryType type) {
	if (type == null) {
	    // Find out what type is wanted.
	    EntryTypeDialog etd = new EntryTypeDialog(frame);
	    // We want to center the dialog, to make it look nicer.
	    Util.placeDialog(etd, frame);
	    etd.setVisible(true);
	    type = etd.getChoice();
	}
	if (type != null) { // Only if the dialog was not cancelled.
	    String id = Util.createId(type, database);
	    BibtexEntry be = new BibtexEntry(id, type);
	    try {
		database.insertEntry(be);
	    // Create new Bibtex entry
	    // Create new Bibtex entry
	    // Set owner field to default value
	    be.setField( "owner", prefs.get("defaultOwner") );

	    // Set owner field to default value
	    be.setField( "owner", prefs.get("defaultOwner") );
		// Create an UndoableInsertEntry object.
		undoManager.addEdit(new UndoableInsertEntry(database, be, ths));
		output(Globals.lang("Added new")+" '"+type.getName().toLowerCase()+"' "
		       +Globals.lang("entry")+".");
		refreshTable();
		markBaseChanged(); // The database just changed.
		if (prefs.getBoolean("autoOpenForm")) {
		    showEntry(be);
		    //EntryTypeForm etf = new EntryTypeForm(frame, ths, be, prefs);
		    //Util.placeDialog(etf, frame);
		    //etf.setVisible(true);
		    //entryTypeForms.put(id, etf);
		}
	    } catch (KeyCollisionException ex) {
		Util.pr(ex.getMessage());
	    }
	}

    }

    public void validateMainPanel() {
    }

    public void setupTable() {
	tableModel = new EntryTableModel(frame, this, database);
	entryTable = new EntryTable(tableModel, ths, frame.prefs);
	entryTable.getActionMap().put("cut", new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    try { runCommand("cut");
		    } catch (Throwable ex) {
			ex.printStackTrace();
		    }
		}
	    });
	entryTable.getActionMap().put("copy", new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    try { runCommand("copy");
		    } catch (Throwable ex) {
			ex.printStackTrace();
		    }
		}
	    });
	entryTable.getActionMap().put("paste", new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    try { runCommand("paste");
		    } catch (Throwable ex) {
			ex.printStackTrace();
		    }
		}
	    });

	/*
	entryTable.getInputMap().put(prefs.getKey("Edit entry"), "Edit");
	entryTable.getActionMap().put("Edit", new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    Util.pr("eueo");
		    try { runCommand("edit");
		    } catch (Throwable ex) {
			ex.printStackTrace();
		    }
		}
	    });
	*/

	entryTable.addKeyListener(new KeyAdapter() {

		public void keyPressed(KeyEvent e) {
		    if (e.getKeyCode() == KeyEvent.VK_ENTER){
			try { runCommand("edit");
			} catch (Throwable ex) {
			    ex.printStackTrace();
			}
		    }
		    else if(e.getKeyCode() == KeyEvent.VK_DELETE){
			try { runCommand("delete");
			} catch (Throwable ex) {
			    ex.printStackTrace();
			}
		    }
                    /*
                    if (((e.getKeyCode() == KeyEvent.VK_DOWN) || (e.getKeyCode() == KeyEvent.VK_UP))
                      && (e.getModifiers() == 0)) {

                      Util.pr(entryTable.getSelectedRow()+"");
                    }*/
                  }
	    });


	// Set the right-click menu for the entry table.
	//rcm = new RightClickMenu(this, metaData);
	entryTable.setRightClickMenu(rcm);
	int pos = splitPane.getDividerLocation();
	splitPane.setTopComponent(entryTable.getPane());
	splitPane.setDividerLocation(pos);
	//splitPane.revalidate();
    }

    public void setupMainPanel() {

	splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	splitPane.setDividerSize(GUIGlobals.SPLIT_PANE_DIVIDER_SIZE);
	// We replace the default FocusTraversalPolicy with a subclass
	// that only allows FieldEditor components to gain keyboard focus,
	// if there is an entry editor open.
	/*splitPane.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
		protected boolean accept(Component c) {
		    Util.pr("jaa");
		    if (showing == null)
			return super.accept(c);
		    else
			return (super.accept(c) &&
				(c instanceof FieldEditor));
		}
		});*/

	setupTable();
	// If an entry is currently being shown, make sure it stays shown,
	// otherwise set the bottom component to null.
	if (showing == null) {
          splitPane.setBottomComponent(previewPanel);
          if (previewPanel != null)
            splitPane.setDividerLocation(splitPane.getHeight()-GUIGlobals.PREVIEW_HEIGHT);
        }
	else
	    showEntry(showing);

	setRightComponent(splitPane);
	sidePaneManager = new SidePaneManager
	    (frame, this, prefs, metaData);
        medlineFetcher = new MedlineFetcher(this, sidePaneManager);
        sidePaneManager.register("fetchMedline", medlineFetcher);
        medlineAuthorFetcher = new MedlineAuthorFetcher(this, sidePaneManager);
        sidePaneManager.register("fetchAuthorMedline", medlineAuthorFetcher);
	searchManager = new SearchManager2(frame, prefs, sidePaneManager);
	sidePaneManager.add("search", searchManager);
	sidePaneManager.populatePanel();

	//mainPanel.setDividerLocation(GUIGlobals.SPLIT_PANE_DIVIDER_LOCATION);
	setDividerSize(GUIGlobals.SPLIT_PANE_DIVIDER_SIZE);
	setResizeWeight(0);
	revalidate();
    }

    /**
     * This method is called after a database has been parsed. The
     * hashmap contains the contents of all comments in the .bib file
     * that started with the meta flag (GUIGlobals.META_FLAG).
     * In this method, the meta data are input to their respective
     * handlers.
     *
     * @param meta Metadata to input.
     */
    public void parseMetaData(HashMap meta) {
	metaData = new MetaData(meta);

    }

    public void refreshTable() {
	// This method is called by EntryTypeForm when a field value is
	// stored. The table is scheduled for repaint.
	tableModel.remap();
	entryTable.revalidate();
	entryTable.repaint();
    }

    public void updatePreamble() {
	if (preambleEditor != null)
	    preambleEditor.updatePreamble();
    }

    public void assureStringDialogNotEditing() {
	if (stringDialog != null)
	    stringDialog.assureNotEditing();
    }

    public void updateStringDialog() {
	if (stringDialog != null)
	    stringDialog.refreshTable();
    }

    public void updateWiewToSelected(BibtexEntry be) {
      // First, if the entry editor is visible, we should update it to the selected entry.
      if (showing != null) {
        BibtexEntry[] bes = entryTable.getSelectedEntries();
        if ((bes != null) && (bes.length > 0))
          showEntry(bes[0]);
        return;
      }
      // If no entry editor is visible we must either instantiate a new preview panel or update the one we have.
      if (!previewEnabled)
        return; // Do nothing if previews are disabled.

      if (previewPanel == null) {
        previewPanel = new PreviewPanel(be);
      } else
        previewPanel.setEntry(be);
      splitPane.setBottomComponent(previewPanel);
      splitPane.setDividerLocation(splitPane.getHeight()-GUIGlobals.PREVIEW_HEIGHT);
      previewPanel.repaint();
    }

    /**
     * Ensure that no preview is shown. Called when preview is turned off. Must chech if
     * a preview is in fact visible before doing anything rash.
     */
    public void hidePreview() {
      previewPanel = null;
      Component c = splitPane.getBottomComponent();
      if ((c != null) && (c instanceof PreviewPanel))
        splitPane.setBottomComponent(null);
    }

    public boolean isShowingEditor() {
      return ((splitPane.getBottomComponent() != null)
              && (splitPane.getBottomComponent() instanceof EntryEditor));
    }

    public void showEntry(BibtexEntry be) {
	if (showing == be) {
	    if (splitPane.getBottomComponent() == null) {
		// This is the special occasion when showing is set to an
		// entry, but no entry editor is in fact shown. This happens
		// after Preferences dialog is closed, and it means that we
		// must make sure the same entry is shown again. We do this by
		// setting showing to null, and recursively calling this method.
	        showing = null;
		showEntry(be);
	    }
	    return;
	}

	EntryEditor form;
	int divLoc = -1, visPan = -1;
	if (showing != null)
	    visPan = ((EntryEditor)splitPane.getBottomComponent()).
		getVisiblePanel();
	if (showing != null)
	    divLoc = splitPane.getDividerLocation();

	if (entryEditors.containsKey(be.getType().getName())) {
	    // We already have an editor for this entry type.
	    form = (EntryEditor)entryEditors.get
		((be.getType().getName()));
	    form.switchTo(be);
	    if (visPan >= 0)
		form.setVisiblePanel(visPan);
	    splitPane.setBottomComponent(form);
	} else {
	    // We must instantiate a new editor for this type.
	    form = new EntryEditor
		(frame, ths, be, prefs);
	    if (visPan >= 0)
		form.setVisiblePanel(visPan);
	    splitPane.setBottomComponent(form);
	    entryEditors.put(be.getType().getName(), form);
	}
	if (showing != null)
	    splitPane.setDividerLocation(divLoc);
	else
	    splitPane.setDividerLocation
		(GUIGlobals.VERTICAL_DIVIDER_LOCATION);
	new FocusRequester(form);
	//form.requestFocus();

	showing = be;
    }

    /**
     * Closes the entry editor.
     *
     */
    public void hideEntryEditor() {
	splitPane.setBottomComponent(previewPanel);
        if (previewPanel != null)
          splitPane.setDividerLocation(splitPane.getHeight()-GUIGlobals.PREVIEW_HEIGHT);
	new FocusRequester(entryTable);
	showing = null;
    }

    /**
     * Closes the entry editor if it is showing the given entry.
     *
     * @param be a <code>BibtexEntry</code> value
     */
    public void ensureNotShowing(BibtexEntry be) {
	if (showing == be) {
	    hideEntryEditor();
	    showing = null;
	}
    }

    public void markBaseChanged() {
	baseChanged = true;

	// Put an asterix behind the file name to indicate the
	// database has changed.
	String oldTitle = frame.getTabTitle(this);
	if (!oldTitle.endsWith("*"))
	    frame.setTabTitle(this, oldTitle+"*");

	// If the status line states that the base has been saved, we
	// remove this message, since it is no longer relevant. If a
	// different message is shown, we leave it.
	if (frame.statusLine.getText().startsWith("Saved database"))
	    frame.output(" ");
    }

    public void markNonUndoableBaseChanged() {
	nonUndoableChange = true;
	markBaseChanged();
    }

    public synchronized void markChangedOrUnChanged() {
	if (undoManager.hasChanged()) {
	    if (!baseChanged)
		markBaseChanged();
	}
	else if (baseChanged && !nonUndoableChange) {
	    baseChanged = false;
	    if (file != null)
		frame.setTabTitle(ths, file.getName());
	    else
		frame.setTabTitle(ths, Globals.lang("untitled"));
	}
    }

    /**
     * Shows either normal search results or group search, depending
     * on the searchValueField. This is done by reordering entries and
     * graying out non-hits.
     *
     * @param searchValueField Which field to show search for: Globals.SEARCH or
     * Globals.GROUPSEARCH.
     *
     */
    public void showSearchResults(String searchValueField) {
	//entryTable.scrollTo(0);

	if (searchValueField == Globals.SEARCH)
	    showingSearchResults = true;
	else if (searchValueField == Globals.GROUPSEARCH)
	    showingGroup = true;

	entryTable.setShowingSearchResults(showingSearchResults,
					   showingGroup);
	entryTable.clearSelection();
	entryTable.scrollTo(0);
	refreshTable();

    }

    /**
     * Selects all entries with a non-zero value in the field
     * Globals.SEARCH.
     */
    public void selectSearchResults() {

	entryTable.clearSelection();
	for (int i=0; i<entryTable.getRowCount(); i++) {
	    String value = (String)(database.getEntryById
				    (tableModel.getNameFromNumber(i)))
		.getField(Globals.SEARCH);
	    if ((value != null) && !value.equals("0"))
		entryTable.addRowSelectionInterval(i, i);
	}
    }

    /**
     * Selects a single entry, and scrolls the table to center it.
     *
     * @param pos Current position of entry to select.
     *
     */
    public void selectSingleEntry(int pos) {
	entryTable.clearSelection();
	entryTable.addRowSelectionInterval(pos, pos);
	entryTable.scrollToCenter(pos, 0);
    }

    public void stopShowingSearchResults() {
	showingSearchResults = false;
	entryTable.setShowingSearchResults(showingSearchResults,
					   showingGroup);
	refreshTable();
	entryTable.requestFocus();
    }

    public void stopShowingGroup() {
	showingGroup = false;
	entryTable.setShowingSearchResults(showingSearchResults,
					   showingGroup);
	refreshTable();
    }

    protected EntryTableModel getTableModel(){
		return tableModel ;
    }

    public BibtexDatabase getDatabase(){
	return database ;
    }

    public void entryTypeFormClosing(String id) {
	// Called by EntryTypeForm when closing.
	// Deprecated, since EntryEditor has replaced EntryTypeForm.
    }

    public void preambleEditorClosing() {
	preambleEditor = null;
    }

    public void stringsClosing() {
	stringDialog = null;
    }

    public void addToGroup(String groupName, String regexp, String field) {

	boolean giveWarning = false;
	for (int i=0; i<GUIGlobals.ALL_FIELDS.length; i++) {
	    if (field.equals(GUIGlobals.ALL_FIELDS[i])
		&& !field.equals("keywords")) {
		giveWarning = true;
		break;
	    }
	}
	if (giveWarning) {
	    String message = "This action will modify the '"+field+"' field "
		+"of your entries.\nThis could cause undesired changes to "
		+"your entries, so it\nis recommended that you change the field "
		+"in your group\ndefinition to 'keywords' or a non-standard name."
		+"\n\nDo you still want to continue?";
	    int choice = JOptionPane.showConfirmDialog
		(this, message, "Warning", JOptionPane.YES_NO_OPTION,
		 JOptionPane.WARNING_MESSAGE);

	    if (choice == JOptionPane.NO_OPTION)
		return;
	}

	BibtexEntry[] bes = entryTable.getSelectedEntries();
	if ((bes != null) && (bes.length > 0)) {
	    QuickSearchRule qsr = new QuickSearchRule(field, regexp);
	    NamedCompound ce = new NamedCompound("add to group");
	    boolean hasEdits = false;
	    for (int i=0; i<bes.length; i++) {
		if (qsr.applyRule(null, bes[i]) == 0) {
		    String oldContent = (String)bes[i].getField(field),
			pre = " ",
			post = "";
		    String newContent =
			(oldContent==null ? "" : oldContent+pre)
			+regexp+post;
		    bes[i].setField
			(field, newContent);

		    // Store undo information.
		    ce.addEdit(new UndoableFieldChange
			       (bes[i], field, oldContent, newContent));
		    hasEdits = true;
		}
	    }
	    if (hasEdits) {
		ce.end();
		undoManager.addEdit(ce);
		refreshTable();
		markBaseChanged();
	    }

	    output("Appended '"+regexp+"' to the '"
		   +field+"' field of "+bes.length+" entr"+
		   (bes.length > 1 ? "ies." : "y."));
	}
    }

    public void removeFromGroup
	(String groupName, String regexp, String field) {

	boolean giveWarning = false;
	for (int i=0; i<GUIGlobals.ALL_FIELDS.length; i++) {
	    if (field.equals(GUIGlobals.ALL_FIELDS[i])
		&& !field.equals("keywords")) {
		giveWarning = true;
		break;
	    }
	}
	if (giveWarning) {
	    String message = "This action will modify the '"+field+"' field "
		+"of your entries.\nThis could cause undesired changes to "
		+"your entries, so it\nis recommended that you change the field "
		+"in your group\ndefinition to 'keywords' or a non-standard name."
		+"\n\nDo you still want to continue?";
	    int choice = JOptionPane.showConfirmDialog
		(this, message, "Warning", JOptionPane.YES_NO_OPTION,
		 JOptionPane.WARNING_MESSAGE);

	    if (choice == JOptionPane.NO_OPTION)
		return;
	}

	BibtexEntry[] bes = entryTable.getSelectedEntries();
	if ((bes != null) && (bes.length > 0)) {
	    QuickSearchRule qsr = new QuickSearchRule(field, regexp);
	    NamedCompound ce = new NamedCompound("remove from group");
	    boolean hasEdits = false;
	    for (int i=0; i<bes.length; i++) {
		if (qsr.applyRule(null, bes[i]) > 0) {
		    String oldContent = (String)bes[i].getField(field);
		    qsr.removeMatches(bes[i]);
		    		    // Store undo information.
		    ce.addEdit(new UndoableFieldChange
			       (bes[i], field, oldContent,
				bes[i].getField(field)));
		    hasEdits = true;
		}
	    }
	    if (hasEdits) {
		ce.end();
		undoManager.addEdit(ce);
		refreshTable();
		markBaseChanged();
	    }

	    output("Removed '"+regexp+"' from the '"
		   +field+"' field of "+bes.length+" entr"+
		   (bes.length > 1 ? "ies." : "y."));
	}

    }

    public void changeType(BibtexEntryType type) {
	BibtexEntry[] bes = entryTable.getSelectedEntries();
	if ((bes == null) || (bes.length == 0)) {
	    output("First select the entries you wish to change type "+
		   "for.");
	    return;
	}
	if (bes.length > 1) {
	    int choice = JOptionPane.showConfirmDialog
		(this, "Multiple entries selected. Do you want to change"
		 +"\nthe type of all these to '"+type.getName()+"'?",
		 "Change type", JOptionPane.YES_NO_OPTION,
		 JOptionPane.WARNING_MESSAGE);
	    if (choice == JOptionPane.NO_OPTION)
		return;
	}

	NamedCompound ce = new NamedCompound("change type");
	for (int i=0; i<bes.length; i++) {
	    ce.addEdit(new UndoableChangeType(bes[i],
					      bes[i].getType(),
					      type));
	    bes[i].setType(type);
	}

	output(Globals.lang("Changed type to")+" '"+type.getName()+"' "
	       +Globals.lang("for")+" "+bes.length
	       +" "+Globals.lang("entries")+".");
	ce.end();
	undoManager.addEdit(ce);
	refreshTable();
	markBaseChanged();
    }

    class UndoAction extends BaseAction {
	public void action() {
	    try {
		String name = undoManager.getUndoPresentationName();
		undoManager.undo();
		markBaseChanged();
		refreshTable();
		frame.output(name);
	    } catch (CannotUndoException ex) {
		frame.output(Globals.lang("Nothing to undo")+".");
	    }
	    // After everything, enable/disable the undo/redo actions
	    // appropriately.
	    //updateUndoState();
	    //redoAction.updateRedoState();
	    markChangedOrUnChanged();
	}
    }

    class RedoAction extends BaseAction {
	public void action() {
	    try {
		String name = undoManager.getRedoPresentationName();
		undoManager.redo();
		markBaseChanged();
		refreshTable();
		frame.output(name);
	    } catch (CannotRedoException ex) {
		frame.output(Globals.lang("Nothing to redo")+".");
	    }
	    // After everything, enable/disable the undo/redo actions
	    // appropriately.
	    //updateRedoState();
	    //undoAction.updateUndoState();
	    markChangedOrUnChanged();
	}
    }

    // Method pertaining to the ClipboardOwner interface.
    public void lostOwnership(Clipboard clipboard, Transferable contents) {}

  public boolean previewEnabled() {
    return previewEnabled;
  }

}
