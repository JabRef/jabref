/*
Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

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

import net.sf.jabref.undo.*;
import net.sf.jabref.export.*;
//import net.sf.jabref.groups.QuickSearchRule;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.datatransfer.*;
import javax.swing.undo.*;

public class BasePanel extends JSplitPane implements MouseListener {
    //, ClipboardOwner {

    BasePanel ths = this;

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

    boolean baseChanged = false;
    // Used to track whether the base has changed since last save.

    EntryTableModel tableModel = null;
    EntryTable entryTable = null;

    HashMap entryTypeForms = new HashMap();
    // Hashmap to keep track of which entries currently have open
    // EntryTypeForm dialogs.
    
    PreambleEditor preambleEditor = null;
    // Keeps track of the preamble dialog if it is open.

    StringDialog stringDialog = null;
    // Keeps track of the string dialog if it is open.

    //SearchPane searchDialog = null;
    // The search pane.

    boolean showingSearchResults = false, 
	showingGroup = false;

    // The sidepane manager takes care of populating the sidepane.
    SidePaneManager sidePaneManager;

    // MetaData parses, keeps and writes meta data.
    MetaData metaData;

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

    /**
     * BaseAction is used to define actions that are called from the
     * base frame through runCommand(). runCommand() finds the
     * appropriate BaseAction object, and runs its action() method.
     */
    abstract class BaseAction {
	abstract void action();
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
			
			// First we check that no editor is already open for this
			// entry.
			if (!entryTypeForms.containsKey(id)) {
			    BibtexEntry be = database.getEntryById(id);
			    EntryTypeForm form = new EntryTypeForm
				(frame, ths, be, prefs);
			    Util.placeDialog(form, frame); // We want to center the editor.
			    form.setVisible(true);
			    entryTypeForms.put(id, form);
			} else {
			    ((EntryTypeForm)(entryTypeForms.get(id))).setVisible(true);
			}
		    }
		}
	    	       
	    });

	// The action for saving a database.
	actions.put("save", new BaseAction() {
		public void action() {
		    if (file == null)
			runCommand("saveAs");
		    else {
			try {
			    FileActions.saveDatabase(database, metaData, file,
						     prefs, false, false);
			    undoManager.markUnchanged();
			    // (Only) after a successful save the following
			    // statement marks that the base is unchanged
			    // since last save:
			    baseChanged = false;
			    frame.setTabTitle(ths, file.getName());
			    frame.output(Globals.lang("Saved database")+" '"
					 +file.getPath()+"'.");
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
			    }
			    ex.printStackTrace();
			    JOptionPane.showMessageDialog
				(frame, Globals.lang("Could not save file")
				 +".\n"+ex.getMessage(), 
				 Globals.lang("Save database"),
				 JOptionPane.ERROR_MESSAGE);
			}		
		    }	
		}
	    });

	actions.put("saveAs", new BaseAction () {
		public void action() {
		    JFileChooser chooser = new JFileChooser
			(prefs.get("workingDirectory"));
		    Util.pr("BasePanel: must set file filter");
		    //chooser.setFileFilter(fileFilter);
		    int returnVal = chooser.showSaveDialog(frame);
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
			String name = chooser.getSelectedFile().getName(),
			    path = chooser.getSelectedFile().getParent();
			if (!name.endsWith(".bib"))
			    name = name+".bib";
			file = new File(path, name);
			if (!file.exists() || 
			    (JOptionPane.showConfirmDialog
			     (frame, "File '"+name+"' exists. Overwrite?",
			      "Save database", JOptionPane.OK_CANCEL_OPTION) 
			     == JOptionPane.OK_OPTION)) {
			    runCommand("save");
			    prefs.put("workingDirectory", path);
			}
			else
			    file = null;
		    } else {
			// Cancelled.
		    }
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
    }

    /**
     * This method is called from JabRefFrame is a database specific
     * action is requested by the user. Runs the command if it is
     * defined, or prints an error message to the standard error
     * stream.
    */
    public void runCommand(String command) { 
	if (actions.get(command) == null)
	    Util.pr("No action defined for'"+command+"'");
	else ((BaseAction)actions.get(command)).action();
    }

    public void setupMainPanel() {
	tableModel = new EntryTableModel(frame, this, database);
	entryTable = new EntryTable(tableModel, frame.prefs);
	entryTable.addMouseListener(this);
	entryTable.getInputMap().put(GUIGlobals.copyStroke, "copy");
	entryTable.getInputMap().put(GUIGlobals.pasteStroke, "paste");
	//entryTable.getActionMap().put("copy", copyAction);
	//entryTable.getActionMap().put("paste", pasteAction);
	Util.pr("BasePanel: must set bindings for copy and paste.");

	// Set the right-click menu for the entry table.
	//RightClickMenu rcm = new RightClickMenu(this, metaData);
	//entryTable.setRightClickMenu(rcm);
	Util.pr("BasePanel: must add right click menu");

	setRightComponent(entryTable.getPane());
	sidePaneManager = new SidePaneManager
	    (frame, this, prefs, metaData);
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
	Util.pr("BasePanel: there is no stringDialog.");
	/*
	if (stringDialog != null)
	    stringDialog.assureNotEditing();
	*/
    }

    public void updateStringDialog() {
	Util.pr("BasePanel: there is no stringDialog.");
	/*
	if (stringDialog != null)
	    stringDialog.refreshTable();
	*/
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

    public synchronized void markChangedOrUnChanged() {
	if (undoManager.hasChanged()) {
	    if (!baseChanged)
		markBaseChanged();
	}
	else if (baseChanged) {
	    baseChanged = false;
	    if (file != null)
		frame.setTabTitle(ths, file.getName());
	    else
		frame.setTabTitle(ths, Globals.lang("untitled"));
	}
    }

    public void showSearchResults(String searchValueField) {
	//Util.pr("BasePanel: must show search results.");
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

    public void stopShowingSearchResults() {
	showingSearchResults = false;
	entryTable.setShowingSearchResults(showingSearchResults,
					   showingGroup);
	refreshTable();
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

    protected BibtexDatabase getDatabase(){
		return database ; 
    }

    public void entryTypeFormClosing(String id) {
	// Called by EntryTypeForm when closing.
	entryTypeForms.remove(id);
    }

    public void preambleEditorClosing() {
	preambleEditor = null;
    }

    public void stringsClosing() {
	stringDialog = null;
    }


    // The action for copying selected entries.
    /*
    CopyAction copyAction = new CopyAction(this);
    class CopyAction extends AbstractAction {
	BibtexBaseFrame parent;
	public CopyAction(BibtexBaseFrame parent_) {
	    super("Copy",
		  new ImageIcon(GUIGlobals.copyIconFile));
	    putValue(SHORT_DESCRIPTION, "Copy");
	    parent = parent_;
	}
   
	public void actionPerformed(ActionEvent e) {
	    BibtexEntry[] bes = entryTable.getSelectedEntries();

	    // Entries are copied if only the first or multiple
	    // columns are selected.
	    if ((bes != null) && (bes.length > 0)) {
		TransferableBibtexEntry trbe = new TransferableBibtexEntry(bes);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(trbe, parent);
		output("Copied "+(bes.length>1 ? bes.length+" entries." : "1 entry."));
	    } else {
		// The user maybe selected a single cell.
		int[] rows = entryTable.getSelectedRows(),
		    cols = entryTable.getSelectedColumns();
		if ((cols.length == 1) && (rows.length == 1)) {
		    // Copy single value.
		    Object o = tableModel.getValueAt(rows[0], cols[0]);
		    if (o != null) {
			StringSelection ss = new StringSelection(o.toString());
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, parent);
			output("Copied cell contents.");
		    }
		}
	    }
	}
    }

    // The action for pasting entries.
    PasteAction pasteAction = new PasteAction();
    class PasteAction extends AbstractAction {
	public PasteAction() {
	    super("Paste",
		  new ImageIcon(GUIGlobals.pasteIconFile));
	    putValue(SHORT_DESCRIPTION, "Paste");
	}
   
	public void actionPerformed(ActionEvent e) {
	    // We pick an object from the clipboard, check if it exists, and if it is a set of entries.
	    Transferable content = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
	    if (content != null) {
		DataFlavor[] flavor = content.getTransferDataFlavors();
		if ((flavor != null) && (flavor.length > 0) && flavor[0].equals(TransferableBibtexEntry.entryFlavor)) {
		    // We have determined that the clipboard data is a set of entries.
		    BibtexEntry[] bes = null;
		    try {
			bes = (BibtexEntry[])(content.getTransferData(TransferableBibtexEntry.entryFlavor));
		    } catch (UnsupportedFlavorException ex) {
		    } catch (IOException ex) {}

		    if ((bes != null) && (bes.length > 0)) {
			NamedCompound ce = new NamedCompound
			    (bes.length > 1 ? "paste entries" : "paste entry");
			for (int i=0; i<bes.length; i++) {
			    try { 
				BibtexEntry be = (BibtexEntry)(bes[i].clone());
				// We have to clone the entries, since the pasted
				// entries must exist independently of the copied
				// ones.
				be.setId(Util.createID(be.getType(), database));
				database.insertEntry(be);
				ce.addEdit(new UndoableInsertEntry
					   (database, be, entryTypeForms));
			    } catch (KeyCollisionException ex) {
				Util.pr("KeyCollisionException... this shouldn't happen.");
			    }
			}
			ce.end();
			undoManager.addEdit(ce);
			tableModel.remap();
			entryTable.clearSelection();
			entryTable.revalidate();
			output("Pasted "+(bes.length>1 ? bes.length+" entries." : "1 entry."));
			refreshTable();
			markBaseChanged();
		    }
		}
		if ((flavor != null) && (flavor.length > 0) && flavor[0].equals(DataFlavor.stringFlavor)) { 
		    // We have determined that the clipboard data is a string.
		    int[] rows = entryTable.getSelectedRows(),
			cols = entryTable.getSelectedColumns();
		    if ((cols != null) && (cols.length == 1) && (cols[0] != 0)
			&& (rows != null) && (rows.length == 1)) {
			try {
			    tableModel.setValueAt((String)(content.getTransferData(DataFlavor.stringFlavor)), rows[0], cols[0]);
			    refreshTable();
			    markBaseChanged();			   
			    output("Pasted cell contents");
			} catch (UnsupportedFlavorException ex) {
			} catch (IOException ex) {
			} catch (IllegalArgumentException ex) {
			    output("Can't paste.");
			}
		    }
		}
	    }
	}
    }

*/


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


    public void mouseClicked(MouseEvent e) {
	// Intercepts mouse clicks from the JTable showing the base contents.
	// A double click on an entry should open the entry's editor.
	if (e.getClickCount() == 2) {
	    runCommand("edit");
	}
	
    }

    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}

    // Method pertaining to the ClipboardOwner interface.
    public void lostOwnership(Clipboard clipboard, Transferable contents) {}

}
