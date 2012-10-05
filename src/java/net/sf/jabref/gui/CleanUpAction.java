/*  Copyright (C) 2012 JabRef contributors.
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
package net.sf.jabref.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import net.sf.jabref.AbstractWorker;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.CheckBoxMessage;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.ImportSettingsTab;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.Util;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

public class CleanUpAction extends AbstractWorker {
	private Logger logger = Logger.getLogger(CleanUpAction.class.getName());

	public final static String 
		AKS_AUTO_NAMING_PDFS_AGAIN = "AskAutoNamingPDFsAgain",
		CLEANUP_DOI = "CleanUpDOI",
		CLEANUP_MONTH = "CleanUpMonth",
		CLEANUP_PAGENUMBERS = "CleanUpPageNumbers",
		CLEANUP_MAKEPATHSRELATIVE = "CleanUpMakePathsRelative",
		CLEANUP_RENAMEPDF = "CleanUpRenamePDF",
		CLEANUP_RENAMEPDF_ONLYRELATIVE_PATHS = "CleanUpRenamePDFonlyRelativePaths",
		CLEANUP_SUPERSCRIPTS = "CleanUpSuperscripts";
	
	public static void putDefaults(HashMap<String, Object> defaults) {
		defaults.put(AKS_AUTO_NAMING_PDFS_AGAIN, Boolean.TRUE);
		defaults.put(CLEANUP_SUPERSCRIPTS, Boolean.TRUE);
		defaults.put(CLEANUP_DOI, Boolean.TRUE);
		defaults.put(CLEANUP_MONTH, Boolean.TRUE);
		defaults.put(CLEANUP_PAGENUMBERS, Boolean.TRUE);
		defaults.put(CLEANUP_MAKEPATHSRELATIVE, Boolean.TRUE);
		defaults.put(CLEANUP_RENAMEPDF, Boolean.TRUE);
		defaults.put(CLEANUP_RENAMEPDF_ONLYRELATIVE_PATHS, Boolean.FALSE);
	}
	
	private JCheckBox cleanUpSuperscrips;
	private JCheckBox cleanUpDOI;
	private JCheckBox cleanUpMonth;
	private JCheckBox cleanUpPageNumbers;
	private JCheckBox cleanUpMakePathsRelative;
	private JCheckBox cleanUpRenamePDF;
	private JCheckBox cleanUpRenamePDFonlyRelativePaths;
	private JPanel optionsPanel = new JPanel();
	private BasePanel panel;
	private JabRefFrame frame;
	
	// global variable to count unsucessful Renames
    int unsuccesfullRenames = 0;
	
	public CleanUpAction(BasePanel panel) {
		this.panel = panel;
		this.frame = panel.frame();
		initOptionsPanel();
	}
	
	private void initOptionsPanel() {
	    cleanUpSuperscrips = new JCheckBox(Globals.lang("Convert 1st, 2nd, ... to real superscripts"));
		cleanUpDOI = new JCheckBox(Globals.lang("Move DOIs from note and URL field to DOI field and remove http prefix"));
		cleanUpMonth = new JCheckBox(Globals.lang("Format content of month field to #mon#"));
		cleanUpPageNumbers = new JCheckBox(Globals.lang("Ensure that page ranges are of the form num1--num2"));
		cleanUpMakePathsRelative = new JCheckBox(Globals.lang("Make paths of linked files relative (if possible)"));
		cleanUpRenamePDF = new JCheckBox(Globals.lang("Rename PDFs to given file name format pattern"));
		cleanUpRenamePDF.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				cleanUpRenamePDFonlyRelativePaths.setEnabled(cleanUpRenamePDF.isSelected());
			}
		});
		cleanUpRenamePDFonlyRelativePaths = new JCheckBox(Globals.lang("Rename only PDFs having a relative path"));
		optionsPanel = new JPanel();
		retrieveSettings();

		FormLayout layout = new FormLayout("left:15dlu,pref", "pref, pref, pref, pref, pref, pref, pref, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout,	optionsPanel);
        builder.setDefaultDialogBorder();
        CellConstraints cc = new CellConstraints();
        builder.add(cleanUpSuperscrips, cc.xyw(1,1,2));
        builder.add(cleanUpDOI, cc.xyw(1,2,2));
        builder.add(cleanUpMonth, cc.xyw(1,3,2));
        builder.add(cleanUpPageNumbers, cc.xyw(1,4,2));
        builder.add(cleanUpMakePathsRelative, cc.xyw(1,5,2));
        builder.add(cleanUpRenamePDF, cc.xyw(1,6,2));
        String currentPattern = Globals.lang("File name format pattern").concat(": ").concat(Globals.prefs.get(ImportSettingsTab.PREF_IMPORT_FILENAMEPATTERN));
        builder.add(new JLabel(currentPattern), cc.xyw(2,7,1));
        builder.add(cleanUpRenamePDFonlyRelativePaths, cc.xyw(2,8,1));
	}
	
	private void retrieveSettings() {
	    cleanUpSuperscrips.setSelected(Globals.prefs.getBoolean(CLEANUP_SUPERSCRIPTS));
		cleanUpDOI.setSelected(Globals.prefs.getBoolean(CLEANUP_DOI));
		cleanUpMonth.setSelected(Globals.prefs.getBoolean(CLEANUP_MONTH));
		cleanUpPageNumbers.setSelected(Globals.prefs.getBoolean(CLEANUP_PAGENUMBERS));
		cleanUpMakePathsRelative.setSelected(Globals.prefs.getBoolean(CLEANUP_MAKEPATHSRELATIVE));
		cleanUpRenamePDF.setSelected(Globals.prefs.getBoolean(CLEANUP_RENAMEPDF));
		cleanUpRenamePDFonlyRelativePaths.setSelected(Globals.prefs.getBoolean(CLEANUP_RENAMEPDF_ONLYRELATIVE_PATHS));
		cleanUpRenamePDFonlyRelativePaths.setEnabled(cleanUpRenamePDF.isSelected());
	}
	
	private void storeSettings() {
	    Globals.prefs.putBoolean(CLEANUP_SUPERSCRIPTS, cleanUpSuperscrips.isSelected());
		Globals.prefs.putBoolean(CLEANUP_DOI, cleanUpDOI.isSelected());
		Globals.prefs.putBoolean(CLEANUP_MONTH, cleanUpMonth.isSelected());
		Globals.prefs.putBoolean(CLEANUP_PAGENUMBERS, cleanUpPageNumbers.isSelected());
		Globals.prefs.putBoolean(CLEANUP_MAKEPATHSRELATIVE, cleanUpMakePathsRelative.isSelected());
		Globals.prefs.putBoolean(CLEANUP_RENAMEPDF, cleanUpRenamePDF.isSelected());
		Globals.prefs.putBoolean(CLEANUP_RENAMEPDF_ONLYRELATIVE_PATHS, cleanUpRenamePDFonlyRelativePaths.isSelected());
	}

	private int showCleanUpDialog() {
    	String dialogTitle = Globals.lang("Cleanup entries");

    	Object[] messages = {Globals.lang("What would you like to clean up?"), optionsPanel}; 
        return JOptionPane.showConfirmDialog(frame, messages, dialogTitle,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
	}
	
	
    boolean cancelled;
	int modifiedEntriesCount;
    int numSelected;
    
    public void init() {
    	cancelled = false;
    	modifiedEntriesCount = 0;
    	int numSelected = panel.getSelectedEntries().length;
        if (numSelected == 0) { // None selected. Inform the user to select entries first.
            JOptionPane.showMessageDialog(frame, Globals.lang("First select entries to clean up."), 
                                          Globals.lang("Cleanup entry"), JOptionPane.INFORMATION_MESSAGE);
            cancelled = true;
            return;
        }
        frame.block();
        panel.output(Globals.lang("Doing a cleanup for %0 entries...", Integer.toString(numSelected)));
    }
    
    public void run() {
    	if (cancelled) return;
    	int choice = showCleanUpDialog();
    	if (choice != JOptionPane.OK_OPTION) {
    		cancelled = true;
    		return;
    	}
    	storeSettings();
    	boolean
    	    choiceCleanUpSuperscripts = cleanUpSuperscrips.isSelected(),
    		choiceCleanUpDOI = cleanUpDOI.isSelected(),
    		choiceCleanUpMonth = cleanUpMonth.isSelected(),
    		choiceCleanUpPageNumbers = cleanUpPageNumbers.isSelected(),
    		choiceMakePathsRelative = cleanUpMakePathsRelative.isSelected(),
    		choiceRenamePDF = cleanUpRenamePDF.isSelected();
    	
    	if (choiceRenamePDF && Globals.prefs.getBoolean(AKS_AUTO_NAMING_PDFS_AGAIN)) { 
	        CheckBoxMessage cbm = new CheckBoxMessage(Globals.lang("Auto-generating PDF-Names does not support undo. Continue?"),
        		Globals.lang("Disable this confirmation dialog"), false);
	        int answer = JOptionPane.showConfirmDialog(frame, cbm, Globals.lang("Autogenerate PDF Names"),
                JOptionPane.YES_NO_OPTION);
	        if (cbm.isSelected())
	        	Globals.prefs.putBoolean(AKS_AUTO_NAMING_PDFS_AGAIN, false);
	        if (answer == JOptionPane.NO_OPTION) {
	            cancelled = true;
	            return;
	        }
	    }
    	
    	for (BibtexEntry entry : panel.getSelectedEntries()) {
    		// undo granularity is on entry level
        	NamedCompound ce = new NamedCompound(Globals.lang("Cleanup entry"));
        	
        	if (choiceCleanUpSuperscripts) doCleanUpSuperscripts(entry, ce);
        	if (choiceCleanUpDOI) doCleanUpDOI(entry, ce);
        	if (choiceCleanUpMonth) doCleanUpMonth(entry, ce);
        	if (choiceCleanUpPageNumbers) doCleanUpPageNumbers(entry, ce);
        	if (choiceMakePathsRelative) doMakePathsRelative(entry, ce);
        	if (choiceRenamePDF) doRenamePDFs(entry, ce);
        	
            ce.end();
            if (ce.hasEdits()) {
            	modifiedEntriesCount++;
            	panel.undoManager.addEdit(ce);
            }
    	}
    }
    	


	public void update() {
        if (cancelled) {
            frame.unblock();
            return;
        }
	    if(unsuccesfullRenames>0) { //Rename failed for at least one entry
	        JOptionPane.showMessageDialog(frame, Globals.lang("File rename failed for")+" "
	        		+ unsuccesfullRenames 
	        		+ " "+Globals.lang("entries") + ".",
	                Globals.lang("Autogenerate PDF Names"), JOptionPane.INFORMATION_MESSAGE);
	    }
    	if (modifiedEntriesCount>0) {
        	panel.updateEntryEditorIfShowing();
        	panel.markBaseChanged() ;
    	}
    	String message;
    	switch (modifiedEntriesCount) {
    	case 0:
    		message = Globals.lang("No entry needed a clean up");
    		break;
    	case 1:
    		message = Globals.lang("One entry needed a clean up");
    		break;
    	default:
    		message = Globals.lang("%0 entries needed a clean up");
    		break;
    	}
        panel.output(message);
        frame.unblock();
    }
	
	/**
	 * Converts the text in 1st, 2nd, ... to real superscripts by wrapping in \textsuperscript{st}, ...
	 */
    private void doCleanUpSuperscripts(BibtexEntry entry, NamedCompound ce) {
        final String field = "booktitle";
        String oldValue = entry.getField(field);
        if (oldValue == null) return;
        String newValue = oldValue.replaceAll(" (\\d+)(st|nd|rd|th) ", " $1\\\\textsuperscript{$2} ");
        if (!oldValue.equals(newValue)) {
            entry.setField(field, newValue);
            ce.addEdit(new UndoableFieldChange(entry, field, oldValue, newValue));
        }
    }

    /**
     * Removes the http://... for each DOI
     * Moves DOIs from URL and NOTE filed to DOI field
     * @param ce 
     */
    private void doCleanUpDOI(BibtexEntry bes, NamedCompound ce) {
        
        // fields to check
    	String[] fields = {"note", "url", "ee"};

        // First check if the DOI Field is empty
        if (bes.getField("doi") != null) {
        	String doiFieldValue = bes.getField("doi");
        	if (Util.checkForDOIwithHTTPprefix(doiFieldValue)) {
        		String newValue = Util.getDOI(doiFieldValue);
        		ce.addEdit(new UndoableFieldChange(bes, "doi", doiFieldValue, newValue));
        		bes.setField("doi", newValue);
        	};
        	if (Util.checkForPlainDOI(doiFieldValue)) {
        		// DOI field seems to contain DOI
        		// cleanup note, url, ee field
        		// we do NOT copy values to the DOI field as the DOI field contains a DOI!
            	for (String field: fields) {
            		if (Util.checkForPlainDOI(bes.getField(field))){
            			Util.removeDOIfromBibtexEntryField(bes, field, ce);
            		}
            	}
        	}
        } else {
        	// As the DOI field is empty we now check if note, url, or ee field contains a DOI
        	
        	for (String field: fields) {
        		if (Util.checkForPlainDOI(bes.getField(field))){
        			// update DOI
                	String oldValue = bes.getField("doi");
                	String newValue = Util.getDOI(bes.getField(field));                                	
        			ce.addEdit(new UndoableFieldChange(bes, "doi", oldValue, newValue));
        			bes.setField("doi", newValue);
        			
        			Util.removeDOIfromBibtexEntryField(bes, field, ce);
        		}
            } 
        }
    }
    
	private void doCleanUpMonth(BibtexEntry entry, NamedCompound ce) {
		// implementation based on patch 3470076 by Mathias Walter
		String oldValue = entry.getField("month");
		if (oldValue == null) return;
		String newValue = oldValue;
		try {
    		int month = Integer.parseInt(newValue);
    		newValue = new StringBuffer("#").append(Globals.MONTHS[month - 1]).append('#').toString();
    	} catch (NumberFormatException e) {
    		// adapt casing of newValue to follow entry in Globals_MONTH_STRINGS
    		String casedString = newValue.substring(0, 1).toUpperCase().concat(newValue.substring(1).toLowerCase());
        	if (Globals.MONTH_STRINGS.containsKey(newValue.toLowerCase()) ||
        			Globals.MONTH_STRINGS.containsValue(casedString)) {
        		newValue = new StringBuffer("#").append(newValue.substring(0, 3).toLowerCase()).append('#').toString();
        	}
    	}
    	if (!oldValue.equals(newValue)) {
    		entry.setField("month", newValue);
    		ce.addEdit(new UndoableFieldChange(entry, "month", oldValue, newValue));
    	}
	}
	
	private void doCleanUpPageNumbers(BibtexEntry entry, NamedCompound ce) {
		String oldValue = entry.getField("pages");		
		if (oldValue == null) return;
		String newValue = oldValue.replaceAll("(\\d+) *- *(\\d+)", "$1--$2");
		if (!oldValue.equals(newValue)) {
			entry.setField("pages", newValue);
			ce.addEdit(new UndoableFieldChange(entry, "pages", oldValue, newValue));
		}
	}

	private void doMakePathsRelative(BibtexEntry entry, NamedCompound ce) {
		String oldValue = entry.getField(GUIGlobals.FILE_FIELD);
		if (oldValue == null) return;
		FileListTableModel flModel = new FileListTableModel();
		flModel.setContent(oldValue);
		if (flModel.getRowCount() == 0) {
			return;
		}
		boolean changed = false;
		for (int i = 0; i<flModel.getRowCount(); i++) {
			FileListEntry flEntry = flModel.getEntry(i);
			String oldFileName = flEntry.getLink();
			String newFileName = Util.shortenFileName(new File(oldFileName), panel.metaData().getFileDirectory(GUIGlobals.FILE_FIELD)).toString();
			if (!oldFileName.equals(newFileName)) {
				flEntry.setLink(newFileName);
				changed = true;
			}
		}
		if (changed) {
	        String newValue = flModel.getStringRepresentation();
			assert(!oldValue.equals(newValue));
			entry.setField(GUIGlobals.FILE_FIELD, newValue);
			ce.addEdit(new UndoableFieldChange(entry, GUIGlobals.FILE_FIELD, oldValue, newValue));
		}
	}

    private void doRenamePDFs(BibtexEntry entry, NamedCompound ce) {
		//Extract the path
		String oldValue = entry.getField(GUIGlobals.FILE_FIELD);
		if (oldValue == null) return;
		FileListTableModel flModel = new FileListTableModel();
		flModel.setContent(oldValue);
		if (flModel.getRowCount() == 0) {
			return;
		}
		boolean changed = false;
		
		for (int i=0; i<flModel.getRowCount(); i++) {
			String realOldFilename = flModel.getEntry(i).getLink();
			
			if (cleanUpRenamePDFonlyRelativePaths.isSelected() && (new File(realOldFilename).isAbsolute()))
				return;
	
			String newFilename = Util.getLinkedFileName(panel.database(), entry);
			//String oldFilename = bes.getField(GUIGlobals.FILE_FIELD); // would have to be stored for undoing purposes
			
			//Add extension to newFilename
			newFilename = newFilename + "." + flModel.getEntry(i).getType().getExtension();
			
			//get new Filename with path
		    //Create new Path based on old Path and new filename
		    File expandedOldFile = Util.expandFilename(realOldFilename, panel.metaData().getFileDirectory(GUIGlobals.FILE_FIELD));
		    String newPath = expandedOldFile.getParent().concat(System.getProperty("file.separator")).concat(newFilename);
		    
		    if (new File(newPath).exists())
		    	// we do not overwrite files
		    	// TODO: we could check here if the newPath file is linked with the current entry. And if not, we could add a link
		    	return;
		    
			//do rename
			boolean renameSuccesfull = Util.renameFile(expandedOldFile.toString(), newPath);
			
			if (renameSuccesfull) {
				changed = true;
				
				//Change the path for this entry
				String description = flModel.getEntry(i).getDescription();
				ExternalFileType type = flModel.getEntry(i).getType();
				flModel.removeEntry(i);
				
				// we cannot use "newPath" to generate a FileListEntry as newPath is absolute, but we want to keep relative paths whenever possible
				File parent = (new File(realOldFilename)).getParentFile();
				String newFileEntryFileName;
				if (parent == null) {
					newFileEntryFileName = newFilename;
				} else {
					newFileEntryFileName = parent.toString().concat(System.getProperty("file.separator")).concat(newFilename);
				}
		        flModel.addEntry(i, new FileListEntry(description, newFileEntryFileName, type));
			}
			else {
				unsuccesfullRenames++;
			}
		}
		
		if (changed) {
	        String newValue = flModel.getStringRepresentation();
			assert(!oldValue.equals(newValue));
			entry.setField(GUIGlobals.FILE_FIELD, newValue);
			//we put an undo of the field content here
			//the file is not being renamed back, which leads to inconsostencies
			//if we put a null undo object here, the change by "doMakePathsRelative" would overwrite the field value nevertheless.
			ce.addEdit(new UndoableFieldChange(entry, GUIGlobals.FILE_FIELD, oldValue, newValue));
		}
	}
}
