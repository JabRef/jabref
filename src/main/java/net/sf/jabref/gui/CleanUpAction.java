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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.forms.factories.Borders;

import net.sf.jabref.AbstractWorker;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.CheckBoxMessage;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.ImportSettingsTab;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.Util;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.imports.HTMLConverter;
import net.sf.jabref.imports.CaseKeeper;
import net.sf.jabref.imports.UnitFormatter;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

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
		CLEANUP_UPGRADE_EXTERNAL_LINKS = "CleanUpUpgradeExternalLinks",
		CLEANUP_SUPERSCRIPTS = "CleanUpSuperscripts",
		CLEANUP_HTML = "CleanUpHTML",
		CLEANUP_CASE = "CleanUpCase",
		CLEANUP_LATEX = "CleanUpLaTeX",
		CLEANUP_UNITS = "CleanUpUnits",
		CLEANUP_UNICODE = "CleanUpUnicode",
		CLEANUP_CONVERTTOBIBLATEX = "CleanUpConvertToBiblatex";
                
	public static void putDefaults(HashMap<String, Object> defaults) {
		defaults.put(AKS_AUTO_NAMING_PDFS_AGAIN, Boolean.TRUE);
		defaults.put(CLEANUP_SUPERSCRIPTS, Boolean.TRUE);
		defaults.put(CLEANUP_DOI, Boolean.TRUE);
		defaults.put(CLEANUP_MONTH, Boolean.TRUE);
		defaults.put(CLEANUP_PAGENUMBERS, Boolean.TRUE);
		defaults.put(CLEANUP_MAKEPATHSRELATIVE, Boolean.TRUE);
		defaults.put(CLEANUP_RENAMEPDF, Boolean.TRUE);
		defaults.put(CLEANUP_RENAMEPDF_ONLYRELATIVE_PATHS, Boolean.FALSE);
		defaults.put(CLEANUP_UPGRADE_EXTERNAL_LINKS, Boolean.FALSE);
		defaults.put(CLEANUP_MAKEPATHSRELATIVE, Boolean.TRUE);
		defaults.put(CLEANUP_HTML, Boolean.TRUE);
		defaults.put(CLEANUP_CASE, Boolean.TRUE);
		defaults.put(CLEANUP_LATEX, Boolean.TRUE);
		defaults.put(CLEANUP_UNITS, Boolean.TRUE);
		defaults.put(CLEANUP_UNICODE, Boolean.TRUE);
		defaults.put(CLEANUP_CONVERTTOBIBLATEX, Boolean.FALSE);
	}
	
	private JCheckBox cleanUpSuperscrips;
	private JCheckBox cleanUpDOI;
	private JCheckBox cleanUpMonth;
	private JCheckBox cleanUpPageNumbers;
	private JCheckBox cleanUpMakePathsRelative;
	private JCheckBox cleanUpRenamePDF;
	private JCheckBox cleanUpRenamePDFonlyRelativePaths;
	private JCheckBox cleanUpUpgradeExternalLinks;
	private JCheckBox cleanUpHTML;
	private JCheckBox cleanUpCase;
	private JCheckBox cleanUpLaTeX;
	private JCheckBox cleanUpUnits;
	private JCheckBox cleanUpUnicode;
	private JCheckBox cleanUpBiblatex;
	
	private JPanel optionsPanel = new JPanel();
	private BasePanel panel;
	private JabRefFrame frame;
	
	// global variable to count unsuccessful renames
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
		cleanUpUpgradeExternalLinks = new JCheckBox(Globals.lang("Upgrade external PDF/PS links to use the '%0' field.", GUIGlobals.FILE_FIELD));
		cleanUpHTML = new JCheckBox(Globals.lang("Run HTML converter on title"));
		cleanUpCase = new JCheckBox(Globals.lang("Run filter on title keeping the case of selected words"));
		cleanUpLaTeX = new JCheckBox(Globals.lang("Remove unneccessary $, {, and } and move adjacent numbers into equations"));
		cleanUpUnits = new JCheckBox(Globals.lang("Add brackets and replace separators with their non-breaking version for units"));
		cleanUpUnicode = new JCheckBox(Globals.lang("Run Unicode converter on title, author(s), and abstract"));
		cleanUpBiblatex = new JCheckBox(Globals.lang("Convert to BibLatex format (for example, move the value of the 'journal' field to 'journaltitle')"));
		optionsPanel = new JPanel();
		retrieveSettings();

		FormLayout layout = new FormLayout("left:15dlu,pref:grow", "pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout,	optionsPanel);
        builder.border(Borders.DIALOG);
        CellConstraints cc = new CellConstraints();
        builder.add(cleanUpHTML, cc.xyw(1,1,2));
        builder.add(cleanUpUnicode, cc.xyw(1,2,2));
        builder.add(cleanUpCase, cc.xyw(1,3,2));
        builder.add(cleanUpLaTeX, cc.xyw(1,4,2));
        builder.add(cleanUpUnits, cc.xyw(1,5,2));
        builder.add(cleanUpSuperscrips, cc.xyw(1,6,2));
        builder.add(cleanUpDOI, cc.xyw(1,7,2));
        builder.add(cleanUpMonth, cc.xyw(1,8,2));
        builder.add(cleanUpPageNumbers, cc.xyw(1,9,2));
        builder.add(cleanUpUpgradeExternalLinks, cc.xyw(1, 10, 2));
        builder.add(cleanUpMakePathsRelative, cc.xyw(1,11,2));
        builder.add(cleanUpRenamePDF, cc.xyw(1,12,2));
        String currentPattern = Globals.lang("File name format pattern").concat(": ").concat(Globals.prefs.get(ImportSettingsTab.PREF_IMPORT_FILENAMEPATTERN));
        builder.add(new JLabel(currentPattern), cc.xyw(2,13,1));
        builder.add(cleanUpRenamePDFonlyRelativePaths, cc.xyw(2,14,1));
        builder.add(cleanUpBiblatex, cc.xyw(1,15,2));
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
		cleanUpUpgradeExternalLinks.setSelected(Globals.prefs.getBoolean(CLEANUP_UPGRADE_EXTERNAL_LINKS));
		cleanUpHTML.setSelected(Globals.prefs.getBoolean(CLEANUP_HTML));
		cleanUpCase.setSelected(Globals.prefs.getBoolean(CLEANUP_CASE));
		cleanUpLaTeX.setSelected(Globals.prefs.getBoolean(CLEANUP_LATEX));
		cleanUpUnits.setSelected(Globals.prefs.getBoolean(CLEANUP_UNITS));
		cleanUpUnicode.setSelected(Globals.prefs.getBoolean(CLEANUP_UNICODE));
		cleanUpBiblatex.setSelected(Globals.prefs.getBoolean(CLEANUP_CONVERTTOBIBLATEX));
	}
	
	private void storeSettings() {
	    Globals.prefs.putBoolean(CLEANUP_SUPERSCRIPTS, cleanUpSuperscrips.isSelected());
		Globals.prefs.putBoolean(CLEANUP_DOI, cleanUpDOI.isSelected());
		Globals.prefs.putBoolean(CLEANUP_MONTH, cleanUpMonth.isSelected());
		Globals.prefs.putBoolean(CLEANUP_PAGENUMBERS, cleanUpPageNumbers.isSelected());
		Globals.prefs.putBoolean(CLEANUP_MAKEPATHSRELATIVE, cleanUpMakePathsRelative.isSelected());
		Globals.prefs.putBoolean(CLEANUP_RENAMEPDF, cleanUpRenamePDF.isSelected());
		Globals.prefs.putBoolean(CLEANUP_RENAMEPDF_ONLYRELATIVE_PATHS, cleanUpRenamePDFonlyRelativePaths.isSelected());
		Globals.prefs.putBoolean(CLEANUP_UPGRADE_EXTERNAL_LINKS, cleanUpUpgradeExternalLinks.isSelected());
		Globals.prefs.putBoolean(CLEANUP_HTML, cleanUpHTML.isSelected());
		Globals.prefs.putBoolean(CLEANUP_CASE, cleanUpCase.isSelected());
		Globals.prefs.putBoolean(CLEANUP_LATEX, cleanUpLaTeX.isSelected());
		Globals.prefs.putBoolean(CLEANUP_UNITS, cleanUpUnits.isSelected());
		Globals.prefs.putBoolean(CLEANUP_UNICODE, cleanUpUnicode.isSelected());
		Globals.prefs.putBoolean(CLEANUP_CONVERTTOBIBLATEX, cleanUpBiblatex.isSelected());
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
    		choiceCleanUpUpgradeExternalLinks = cleanUpUpgradeExternalLinks.isSelected(),
    		choiceMakePathsRelative = cleanUpMakePathsRelative.isSelected(),
    		choiceRenamePDF = cleanUpRenamePDF.isSelected(),
	        choiceConvertHTML = cleanUpHTML.isSelected(),
	        choiceConvertCase = cleanUpCase.isSelected(),
	        choiceConvertLaTeX = cleanUpLaTeX.isSelected(),
	        choiceConvertUnits = cleanUpUnits.isSelected(),
	        choiceConvertUnicode = cleanUpUnicode.isSelected(),
    		choiceConvertToBiblatex = cleanUpBiblatex.isSelected();
    	
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
    	
    	// first upgrade the external links
    	// we have to use it separately as the Utils function generates a separate Named Compound
    	if (choiceCleanUpUpgradeExternalLinks) {
    		NamedCompound ce = Util.upgradePdfPsToFile(Arrays.asList(panel.getSelectedEntries()), new String[] {"pdf", "ps"});
    		if (ce.hasEdits()) {
	    		panel.undoManager.addEdit(ce);
	    		panel.markBaseChanged();
	    		panel.updateEntryEditorIfShowing();
	    		panel.output(Globals.lang("Upgraded links."));
    		}
    	}

    	for (BibtexEntry entry : panel.getSelectedEntries()) {
    		// undo granularity is on entry level
        	NamedCompound ce = new NamedCompound(Globals.lang("Cleanup entry"));
        	
        	if (choiceCleanUpSuperscripts) doCleanUpSuperscripts(entry, ce);
        	if (choiceCleanUpDOI) doCleanUpDOI(entry, ce);
        	if (choiceCleanUpMonth) doCleanUpMonth(entry, ce);
        	if (choiceCleanUpPageNumbers) doCleanUpPageNumbers(entry, ce);
        	fixWrongFileEntries(entry, ce);
        	if (choiceMakePathsRelative) doMakePathsRelative(entry, ce);
        	if (choiceRenamePDF) doRenamePDFs(entry, ce);
        	if (choiceConvertHTML) doConvertHTML(entry, ce);
        	if (choiceConvertUnits) doConvertUnits(entry, ce);
        	if (choiceConvertCase) doConvertCase(entry, ce);
        	if (choiceConvertLaTeX) doConvertLaTeX(entry, ce);
        	if (choiceConvertUnicode) doConvertUnicode(entry, ce);
        	if (choiceConvertToBiblatex) doConvertToBiblatex(entry, ce);
        	
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
    		message = Globals.lang("%0 entries needed a clean up", Integer.toString(modifiedEntriesCount));
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
        	}
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
		if (oldValue == null) {
                    return;
                }
		String newValue = oldValue;
		int month = Globals.ParseMonthToInteger(oldValue);
		if(month > 0)
		{
			newValue = "#" + Globals.MONTHS[month - 1] + '#';
		}

    	if (!oldValue.equals(newValue)) {
    		entry.setField("month", newValue);
    		ce.addEdit(new UndoableFieldChange(entry, "month", oldValue, newValue));
    	}
	}
	
	private void doCleanUpPageNumbers(BibtexEntry entry, NamedCompound ce) {
		String oldValue = entry.getField("pages");		
		if (oldValue == null) return;
		String newValue = oldValue.replaceAll(" *(\\d+) *- *(\\d+) *", "$1--$2");
		if (!oldValue.equals(newValue)) {
			entry.setField("pages", newValue);
			ce.addEdit(new UndoableFieldChange(entry, "pages", oldValue, newValue));
		}
	}
	
	private void fixWrongFileEntries(BibtexEntry entry, NamedCompound ce) {
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
			String link = flEntry.getLink();
			String description = flEntry.getDescription();
	    	if (link.equals("") && (!description.equals(""))) {
	    		// link and description seem to be switched, quickly fix that
	    		flEntry.setLink(flEntry.getDescription());
	    		flEntry.setDescription("");
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
				continue;
	
			String newFilename = Util.getLinkedFileName(panel.database(), entry);
			//String oldFilename = bes.getField(GUIGlobals.FILE_FIELD); // would have to be stored for undoing purposes
			
			//Add extension to newFilename
			newFilename = newFilename + "." + flModel.getEntry(i).getType().getExtension();
			
			//get new Filename with path
		    //Create new Path based on old Path and new filename
		    File expandedOldFile = Util.expandFilename(realOldFilename, panel.metaData().getFileDirectory(GUIGlobals.FILE_FIELD));
		    if (expandedOldFile.getParent() == null) {
		    	// something went wrong. Just skipt his entry
		    	continue;
		    }
		    String newPath = expandedOldFile.getParent().concat(System.getProperty("file.separator")).concat(newFilename);
		    
		    if (new File(newPath).exists())
		    	// we do not overwrite files
		    	// TODO: we could check here if the newPath file is linked with the current entry. And if not, we could add a link
		    	continue;
		    
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

	/**
	 * Converts HTML code to LaTeX code
	 */
    private void doConvertHTML(BibtexEntry entry, NamedCompound ce) {
        final String field = "title";
        String oldValue = entry.getField(field);
        if (oldValue == null) {
            return;
        }
        final HTMLConverter htmlConverter = new HTMLConverter();
        String newValue = htmlConverter.format(oldValue);
        if (!oldValue.equals(newValue)) {
            entry.setField(field, newValue);
            ce.addEdit(new UndoableFieldChange(entry, field, oldValue, newValue));
        }
    }

	/**
	 * Converts Unicode characters to LaTeX code
	 */
        private void doConvertUnicode(BibtexEntry entry, NamedCompound ce) {
        final String[] fields = {"title", "author", "abstract"};
            for (String field : fields) {
                String oldValue = entry.getField(field);
                if (oldValue == null) {
                    return;
                }
                final HTMLConverter htmlConverter = new HTMLConverter();
                String newValue = htmlConverter.formatUnicode(oldValue);
                if (!oldValue.equals(newValue)) {
                    entry.setField(field, newValue);
                    ce.addEdit(new UndoableFieldChange(entry, field, oldValue, newValue));
                }
            }
    }

	/**
	 * Adds curly brackets {} around keywords
	 */
    private void doConvertCase(BibtexEntry entry, NamedCompound ce) {
        final String field = "title";
        String oldValue = entry.getField(field);
        if (oldValue == null) {
            return;
        }
        final CaseKeeper caseKeeper = new CaseKeeper();
        String newValue = caseKeeper.format(oldValue);
        if (!oldValue.equals(newValue)) {
            entry.setField(field, newValue);
            ce.addEdit(new UndoableFieldChange(entry, field, oldValue, newValue));
        }
    }

    private void doConvertUnits(BibtexEntry entry, NamedCompound ce) {
        final String field = "title";
        String oldValue = entry.getField(field);
        if (oldValue == null) {
            return;
        }
        final UnitFormatter unitFormatter = new UnitFormatter();
        String newValue = unitFormatter.format(oldValue);
        if (!oldValue.equals(newValue)) {
            entry.setField(field, newValue);
            ce.addEdit(new UndoableFieldChange(entry, field, oldValue, newValue));
        }
    }

    private void doConvertLaTeX(BibtexEntry entry, NamedCompound ce) {
        final String field = "title";
        String oldValue = entry.getField(field);
        if (oldValue == null) {
            return;
        }
        String newValue = oldValue;

        // Remove redundant $, {, and }, but not if the } is part of a command argument: \mbox{-}{GPS} should not be adjusted
        newValue = newValue.replace("$$","").replaceAll("(?<!\\\\[\\p{Alpha}]{0,100}\\{[^\\}]{0,100})\\}([-/ ]?)\\{","$1");
        // Move numbers, +, -, /, and brackets into equations
        // System.err.println(newValue);
        newValue = newValue.replaceAll("(([^$]|\\\\\\$)*)\\$","$1@@"); // Replace $, but not \$ with @@
        // System.err.println(newValue);
        newValue = newValue.replaceAll("([^@]*)@@([^@]*)@@", "$1\\$$2@@"); // Replace every other @@ with $
        // System.err.println(newValue);
        //newValue = newValue.replaceAll("([0-9\\(\\.]+) \\$","\\$$1\\\\ "); // Move numbers followed by a space left of $ inside the equation, e.g., 0.35 $\mu$m
        // System.err.println(newValue);
        newValue = newValue.replaceAll("([0-9\\(\\.]+[ ]?[-+/]?[ ]?)\\$","\\$$1"); // Move numbers, possibly with operators +, -, or /,  left of $ into the equation
        // System.err.println(newValue);
        newValue = newValue.replaceAll("@@([ ]?[-+/]?[ ]?[0-9\\)\\.]+)"," $1@@"); // Move numbers right of @@ into the equation
        // System.err.println(newValue);
        newValue = newValue.replace("@@","$"); // Replace all @@ with $
        // System.err.println(newValue);
        newValue = newValue.replace("  "," "); // Clean up
        newValue = newValue.replace("$$","");
        newValue = newValue.replace(" )$",")$");

        if (!oldValue.equals(newValue)) {
            entry.setField(field, newValue);
            ce.addEdit(new UndoableFieldChange(entry, field, oldValue, newValue));
        }
    }
    
    /**
	 * Converts to BibLatex format
	 */
    private void doConvertToBiblatex(BibtexEntry entry, NamedCompound ce) {
        
    	for (Map.Entry<String, String> alias : BibtexEntry.FieldAliasesOldToNew.entrySet()) {
    	    String oldFieldName = alias.getKey();
    	    String newFieldName = alias.getValue();
    	    String oldValue = entry.getField(oldFieldName);
    	    String newValue = entry.getField(newFieldName);
    	    if(oldValue != null && oldValue.length() > 0
    	    		&& newValue == null)
    	    {
    	    	// There is content in the old field and no value in the new, so just copy
    	    	entry.setField(newFieldName, oldValue);
                ce.addEdit(new UndoableFieldChange(entry, newFieldName, null, oldValue));
                
                entry.setField(oldFieldName, null);
                ce.addEdit(new UndoableFieldChange(entry, oldFieldName, oldValue, null));
    	    }
    	}
    	
    	// Dates: create date out of year and month, save it and delete old fields
    	if(entry.getField("date") == null || entry.getField("date").length() == 0)
    	{
    		String newDate = entry.getFieldOrAlias("date");
    		String oldYear = entry.getField("year");
    		String oldMonth = entry.getField("month");
    		entry.setField("date", newDate);
    		entry.setField("year", null);
    		entry.setField("month", null);
    		
    		ce.addEdit(new UndoableFieldChange(entry, "date", null, newDate));
    		ce.addEdit(new UndoableFieldChange(entry, "year", oldYear, null));
    		ce.addEdit(new UndoableFieldChange(entry, "month", oldMonth, null));
    	}
    }

}
