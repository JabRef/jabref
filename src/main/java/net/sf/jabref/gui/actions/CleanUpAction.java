/*  Copyright (C) 2012-2015 JabRef contributors.
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
package net.sf.jabref.gui.actions;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.*;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.preftabs.ImportSettingsTab;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.importer.HTMLConverter;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.BibtexFieldFormatters;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.MonthUtil;
import net.sf.jabref.model.entry.EntryConverter;
import net.sf.jabref.util.Util;

public class CleanUpAction extends AbstractWorker {

    private static final String AKS_AUTO_NAMING_PDFS_AGAIN = "AskAutoNamingPDFsAgain";
    private static final String CLEANUP_DOI = "CleanUpDOI";
    private static final String CLEANUP_MONTH = "CleanUpMonth";
    private static final String CLEANUP_PAGENUMBERS = "CleanUpPageNumbers";
    private static final String CLEANUP_DATE = "CleanUpDate";
    private static final String CLEANUP_MAKEPATHSRELATIVE = "CleanUpMakePathsRelative";
    private static final String CLEANUP_RENAMEPDF = "CleanUpRenamePDF";
    private static final String CLEANUP_RENAMEPDF_ONLYRELATIVE_PATHS = "CleanUpRenamePDFonlyRelativePaths";
    private static final String CLEANUP_UPGRADE_EXTERNAL_LINKS = "CleanUpUpgradeExternalLinks";
    private static final String CLEANUP_SUPERSCRIPTS = "CleanUpSuperscripts";
    private static final String CLEANUP_HTML = "CleanUpHTML";
    private static final String CLEANUP_CASE = "CleanUpCase";
    private static final String CLEANUP_LATEX = "CleanUpLaTeX";
    private static final String CLEANUP_UNITS = "CleanUpUnits";
    private static final String CLEANUP_UNICODE = "CleanUpUnicode";
    private static final String CLEANUP_CONVERTTOBIBLATEX = "CleanUpConvertToBiblatex";

    public static void putDefaults(HashMap<String, Object> defaults) {
        defaults.put(AKS_AUTO_NAMING_PDFS_AGAIN, Boolean.TRUE);
        defaults.put(CLEANUP_SUPERSCRIPTS, Boolean.TRUE);
        defaults.put(CLEANUP_DOI, Boolean.TRUE);
        defaults.put(CLEANUP_MONTH, Boolean.TRUE);
        defaults.put(CLEANUP_PAGENUMBERS, Boolean.TRUE);
        defaults.put(CLEANUP_DATE, Boolean.TRUE);
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

    private JCheckBox cleanUpSuperscripts;
    private JCheckBox cleanUpDOI;
    private JCheckBox cleanUpMonth;
    private JCheckBox cleanUpPageNumbers;
    private JCheckBox cleanUpDate;
    private JCheckBox cleanUpMakePathsRelative;
    private JCheckBox cleanUpRenamePDF;
    private JCheckBox cleanUpRenamePDFonlyRelativePaths;
    private JCheckBox cleanUpUpgradeExternalLinks;
    private JCheckBox cleanUpHTML;
    private JCheckBox cleanUpCase;
    private JCheckBox cleanUpLaTeX;
    private JCheckBox cleanUpUnits;
    private JCheckBox cleanUpUnicode;
    private JCheckBox cleanUpBibLatex;

    private JPanel optionsPanel = new JPanel();
    private final BasePanel panel;
    private final JabRefFrame frame;

    // global variable to count unsuccessful renames
    private int unsuccessfulRenames;


    public CleanUpAction(BasePanel panel) {
        this.panel = panel;
        this.frame = panel.frame();
        initOptionsPanel();
    }

    private static void removeFieldValue(BibtexEntry entry, String fieldName, NamedCompound compound) {
        String origValue = entry.getField(fieldName);
        compound.addEdit(new UndoableFieldChange(entry, fieldName, origValue, null));
        entry.setField(fieldName, null);
    }

    /**
     * Converts to BibLatex format
     */
    public static void convertToBiblatex(BibtexEntry entry, NamedCompound ce) {
        for (Map.Entry<String, String> alias : EntryConverter.FIELD_ALIASES_TEX_TO_LTX.entrySet()) {
            String oldFieldName = alias.getKey();
            String newFieldName = alias.getValue();
            String oldValue = entry.getField(oldFieldName);
            String newValue = entry.getField(newFieldName);
            if ((oldValue != null) && (!oldValue.isEmpty()) && (newValue == null)) {
                // There is content in the old field and no value in the new, so just copy
                entry.setField(newFieldName, oldValue);
                ce.addEdit(new UndoableFieldChange(entry, newFieldName, null, oldValue));

                entry.setField(oldFieldName, null);
                ce.addEdit(new UndoableFieldChange(entry, oldFieldName, oldValue, null));
            }
        }

        // Dates: create date out of year and month, save it and delete old fields
        if ((entry.getField("date") == null) || (entry.getField("date").isEmpty())) {
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

    private void initOptionsPanel() {
        cleanUpSuperscripts = new JCheckBox(Localization.lang("Convert 1st, 2nd, ... to real superscripts"));
        cleanUpDOI = new JCheckBox(Localization.lang("Move DOIs from note and URL field to DOI field and remove http prefix"));
        cleanUpMonth = new JCheckBox(Localization.lang("Format content of month field to #mon#"));
        cleanUpPageNumbers = new JCheckBox(Localization.lang("Ensure that page ranges are of the form num1--num2"));
        cleanUpDate = new JCheckBox(Localization.lang("Format date field in the form yyyy-mm or yyyy-mm-dd"));
        cleanUpMakePathsRelative = new JCheckBox(Localization.lang("Make paths of linked files relative (if possible)"));
        cleanUpRenamePDF = new JCheckBox(Localization.lang("Rename PDFs to given filename format pattern"));
        cleanUpRenamePDF.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent arg0) {
                cleanUpRenamePDFonlyRelativePaths.setEnabled(cleanUpRenamePDF.isSelected());
            }
        });
        cleanUpRenamePDFonlyRelativePaths = new JCheckBox(Localization.lang("Rename only PDFs having a relative path"));
        cleanUpUpgradeExternalLinks = new JCheckBox(Localization.lang("Upgrade external PDF/PS links to use the '%0' field.", Globals.FILE_FIELD));
        cleanUpHTML = new JCheckBox(Localization.lang("Run HTML converter on title"));
        cleanUpCase = new JCheckBox(Localization.lang("Run filter on title keeping the case of selected words"));
        cleanUpLaTeX = new JCheckBox(Localization.lang("Remove unneccessary $, {, and } and move adjacent numbers into equations"));
        cleanUpUnits = new JCheckBox(Localization.lang("Add brackets and replace separators with their non-breaking version for units"));
        cleanUpUnicode = new JCheckBox(Localization.lang("Run Unicode converter on title, author(s), and abstract"));
        cleanUpBibLatex = new JCheckBox(Localization.lang("Convert to BibLatex format (for example, move the value of the 'journal' field to 'journaltitle')"));
        retrieveSettings();

        FormLayout layout = new FormLayout("left:15dlu,pref:grow",
                "pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref");
        FormBuilder builder = FormBuilder.create().layout(layout);
        builder.add(cleanUpHTML).xyw(1, 1, 2);
        builder.add(cleanUpUnicode).xyw(1, 2, 2);
        builder.add(cleanUpCase).xyw(1, 3, 2);
        builder.add(cleanUpLaTeX).xyw(1, 4, 2);
        builder.add(cleanUpUnits).xyw(1, 5, 2);
        builder.add(cleanUpSuperscripts).xyw(1, 6, 2);
        builder.add(cleanUpDOI).xyw(1, 7, 2);
        builder.add(cleanUpMonth).xyw(1, 8, 2);
        builder.add(cleanUpPageNumbers).xyw(1, 9, 2);
        builder.add(cleanUpDate).xyw(1, 10, 2);
        builder.add(cleanUpUpgradeExternalLinks).xyw(1, 11, 2);
        builder.add(cleanUpMakePathsRelative).xyw(1, 12, 2);
        builder.add(cleanUpRenamePDF).xyw(1, 13, 2);
        String currentPattern = Localization.lang("Filename format pattern").concat(": ").concat(Globals.prefs.get(ImportSettingsTab.PREF_IMPORT_FILENAMEPATTERN));
        builder.add(new JLabel(currentPattern)).xy(2, 14);
        builder.add(cleanUpRenamePDFonlyRelativePaths).xy(2, 15);
        builder.add(cleanUpBibLatex).xyw(1, 16, 2);
        optionsPanel = builder.build();

    }

    private void retrieveSettings() {
        cleanUpSuperscripts.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_SUPERSCRIPTS));
        cleanUpDOI.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_DOI));
        cleanUpMonth.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_MONTH));
        cleanUpPageNumbers.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_PAGENUMBERS));
        cleanUpDate.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_DATE));
        cleanUpMakePathsRelative.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_MAKEPATHSRELATIVE));
        cleanUpRenamePDF.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_RENAMEPDF));
        cleanUpRenamePDFonlyRelativePaths.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_RENAMEPDF_ONLYRELATIVE_PATHS));
        cleanUpRenamePDFonlyRelativePaths.setEnabled(cleanUpRenamePDF.isSelected());
        cleanUpUpgradeExternalLinks.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_UPGRADE_EXTERNAL_LINKS));
        cleanUpHTML.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_HTML));
        cleanUpCase.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_CASE));
        cleanUpLaTeX.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_LATEX));
        cleanUpUnits.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_UNITS));
        cleanUpUnicode.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_UNICODE));
        cleanUpBibLatex.setSelected(Globals.prefs.getBoolean(CleanUpAction.CLEANUP_CONVERTTOBIBLATEX));
    }

    private void storeSettings() {
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_SUPERSCRIPTS, cleanUpSuperscripts.isSelected());
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_DOI, cleanUpDOI.isSelected());
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_MONTH, cleanUpMonth.isSelected());
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_PAGENUMBERS, cleanUpPageNumbers.isSelected());
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_DATE, cleanUpDate.isSelected());
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_MAKEPATHSRELATIVE, cleanUpMakePathsRelative.isSelected());
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_RENAMEPDF, cleanUpRenamePDF.isSelected());
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_RENAMEPDF_ONLYRELATIVE_PATHS, cleanUpRenamePDFonlyRelativePaths.isSelected());
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_UPGRADE_EXTERNAL_LINKS, cleanUpUpgradeExternalLinks.isSelected());
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_HTML, cleanUpHTML.isSelected());
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_CASE, cleanUpCase.isSelected());
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_LATEX, cleanUpLaTeX.isSelected());
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_UNITS, cleanUpUnits.isSelected());
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_UNICODE, cleanUpUnicode.isSelected());
        Globals.prefs.putBoolean(CleanUpAction.CLEANUP_CONVERTTOBIBLATEX, cleanUpBibLatex.isSelected());
    }

    private int showCleanUpDialog() {
        String dialogTitle = Localization.lang("Cleanup entries");

        Object[] messages = {Localization.lang("What would you like to clean up?"), optionsPanel};
        return JOptionPane.showConfirmDialog(frame, messages, dialogTitle,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
    }


    private boolean cancelled;
    private int modifiedEntriesCount;


    @Override
    public void init() {
        cancelled = false;
        modifiedEntriesCount = 0;
        int numSelected = panel.getSelectedEntries().length;
        if (numSelected == 0) { // None selected. Inform the user to select entries first.
            JOptionPane.showMessageDialog(frame, Localization.lang("First select entries to clean up."),
                    Localization.lang("Cleanup entry"), JOptionPane.INFORMATION_MESSAGE);
            cancelled = true;
            return;
        }
        frame.block();
        panel.output(Localization.lang("Doing a cleanup for %0 entries...", Integer.toString(numSelected)));
    }

    @Override
    public void run() {
        if (cancelled) {
            return;
        }
        int choice = showCleanUpDialog();
        if (choice != JOptionPane.OK_OPTION) {
            cancelled = true;
            return;
        }
        storeSettings();
        boolean choiceCleanUpSuperscripts = cleanUpSuperscripts.isSelected();
        boolean choiceCleanUpDOI = cleanUpDOI.isSelected();
        boolean choiceCleanUpMonth = cleanUpMonth.isSelected();
        boolean choiceCleanUpPageNumbers = cleanUpPageNumbers.isSelected();
        boolean choiceCleanUpDate = cleanUpDate.isSelected();
        boolean choiceCleanUpUpgradeExternalLinks = cleanUpUpgradeExternalLinks.isSelected();
        boolean choiceMakePathsRelative = cleanUpMakePathsRelative.isSelected();
        boolean choiceRenamePDF = cleanUpRenamePDF.isSelected();
        boolean choiceConvertHTML = cleanUpHTML.isSelected();
        boolean choiceConvertCase = cleanUpCase.isSelected();
        boolean choiceConvertLaTeX = cleanUpLaTeX.isSelected();
        boolean choiceConvertUnits = cleanUpUnits.isSelected();
        boolean choiceConvertUnicode = cleanUpUnicode.isSelected();
        boolean choiceConvertToBiblatex = cleanUpBibLatex.isSelected();

        if (choiceRenamePDF && Globals.prefs.getBoolean(CleanUpAction.AKS_AUTO_NAMING_PDFS_AGAIN)) {
            CheckBoxMessage cbm = new CheckBoxMessage(Localization.lang("Auto-generating PDF-Names does not support undo. Continue?"),
                    Localization.lang("Disable this confirmation dialog"), false);
            int answer = JOptionPane.showConfirmDialog(frame, cbm, Localization.lang("Autogenerate PDF Names"),
                    JOptionPane.YES_NO_OPTION);
            if (cbm.isSelected()) {
                Globals.prefs.putBoolean(CleanUpAction.AKS_AUTO_NAMING_PDFS_AGAIN, false);
            }
            if (answer == JOptionPane.NO_OPTION) {
                cancelled = true;
                return;
            }
        }

        // first upgrade the external links
        // we have to use it separately as the Utils function generates a separate Named Compound
        if (choiceCleanUpUpgradeExternalLinks) {
            NamedCompound ce = Util.upgradePdfPsToFile(Arrays.asList(panel.getSelectedEntries()), new String[]{"pdf", "ps"});
            if (ce.hasEdits()) {
                panel.undoManager.addEdit(ce);
                panel.markBaseChanged();
                panel.updateEntryEditorIfShowing();
                panel.output(Localization.lang("Upgraded links."));
            }
        }

        for (BibtexEntry entry : panel.getSelectedEntries()) {
            // undo granularity is on entry level
            NamedCompound ce = new NamedCompound(Localization.lang("Cleanup entry"));

            if (choiceCleanUpSuperscripts) {
                doCleanUpSuperscripts(entry, ce);
            }
            if (choiceCleanUpDOI) {
                doCleanUpDOI(entry, ce);
            }
            if (choiceCleanUpMonth) {
                doCleanUpMonth(entry, ce);
            }
            if (choiceCleanUpPageNumbers) {
                doCleanUpPageNumbers(entry, ce);
            }
            if (choiceCleanUpDate) {
                doCleanUpDate(entry, ce);
            }

            fixWrongFileEntries(entry, ce);
            if (choiceMakePathsRelative) {
                doMakePathsRelative(entry, ce);
            }
            if (choiceRenamePDF) {
                doRenamePDFs(entry, ce);
            }
            if (choiceConvertHTML) {
                doConvertHTML(entry, ce);
            }
            if (choiceConvertUnits) {
                doConvertUnits(entry, ce);
            }
            if (choiceConvertCase) {
                doConvertCase(entry, ce);
            }
            if (choiceConvertLaTeX) {
                doConvertLaTeX(entry, ce);
            }
            if (choiceConvertUnicode) {
                doConvertUnicode(entry, ce);
            }
            if (choiceConvertToBiblatex) {
                convertToBiblatex(entry, ce);
            }

            ce.end();
            if (ce.hasEdits()) {
                modifiedEntriesCount++;
                panel.undoManager.addEdit(ce);
            }
        }
    }

    @Override
    public void update() {
        if (cancelled) {
            frame.unblock();
            return;
        }
        if (unsuccessfulRenames > 0) { //Rename failed for at least one entry
            JOptionPane.showMessageDialog(frame,
                    Localization.lang("File rename failed for %0 entries.", Integer.toString(unsuccessfulRenames)),
                    Localization.lang("Autogenerate PDF Names"), JOptionPane.INFORMATION_MESSAGE);
        }
        if (modifiedEntriesCount > 0) {
            panel.updateEntryEditorIfShowing();
            panel.markBaseChanged();
        }
        String message;
        switch (modifiedEntriesCount) {
        case 0:
            message = Localization.lang("No entry needed a clean up");
            break;
        case 1:
            message = Localization.lang("One entry needed a clean up");
            break;
        default:
            message = Localization.lang("%0 entries needed a clean up", Integer.toString(modifiedEntriesCount));
            break;
        }
        panel.output(message);
        frame.unblock();
    }

    /**
     * Converts the text in 1st, 2nd, ... to real superscripts by wrapping in \textsuperscript{st}, ...
     */
    private static void doCleanUpSuperscripts(BibtexEntry entry, NamedCompound ce) {
        for(String name: entry.getFieldNames()) {
            String oldValue = entry.getField(name);
            // run formatter
            String newValue = BibtexFieldFormatters.SUPERSCRIPTS.format(oldValue);
            // undo action
            if(!oldValue.equals(newValue)) {
                entry.setField(name, newValue);
                ce.addEdit(new UndoableFieldChange(entry, name, oldValue, newValue));
            }
        }
    }

    /**
     * Removes the http://... for each DOI
     * Moves DOIs from URL and NOTE filed to DOI field
     * @param ce
     */
    private static void doCleanUpDOI(BibtexEntry bes, NamedCompound ce) {
        // fields to check
        String[] fields = {"note", "url", "ee"};

        // First check if the Doi Field is empty
        if (bes.getField("doi") != null) {
            String doiFieldValue = bes.getField("doi");

            Optional<DOI> doi = DOI.build(doiFieldValue);

            if (doi.isPresent()) {
                String newValue = doi.get().getDOI();
                if (!doiFieldValue.equals(newValue)) {
                    ce.addEdit(new UndoableFieldChange(bes, "doi", doiFieldValue, newValue));
                    bes.setField("doi", newValue);
                }

                // Doi field seems to contain Doi
                // -> cleanup note, url, ee field
                for (String field : fields) {
                    DOI.build(bes.getField((field))).ifPresent( unused -> removeFieldValue(bes, field, ce));
                }
            }
        } else {
            // As the Doi field is empty we now check if note, url, or ee field contains a Doi
            for (String field : fields) {
                Optional<DOI> doi = DOI.build(bes.getField(field));

                if (doi.isPresent()) {
                    // update Doi
                    String oldValue = bes.getField("doi");
                    String newValue = doi.get().getDOI();
                    ce.addEdit(new UndoableFieldChange(bes, "doi", oldValue, newValue));
                    bes.setField("doi", newValue);

                    removeFieldValue(bes, field, ce);
                }
            }
        }
    }

    private static void doCleanUpMonth(BibtexEntry entry, NamedCompound ce) {
        // implementation based on patch 3470076 by Mathias Walter
        String oldValue = entry.getField("month");
        if (oldValue == null) {
            return;
        }
        String newValue = oldValue;
        MonthUtil.Month month = MonthUtil.getMonth(oldValue);
        if (month.isValid())
        {
            newValue = month.bibtexFormat;
        }

        if (!oldValue.equals(newValue)) {
            entry.setField("month", newValue);
            ce.addEdit(new UndoableFieldChange(entry, "month", oldValue, newValue));
        }
    }

    private static void doCleanUpPageNumbers(BibtexEntry entry, NamedCompound ce) {
        doFieldFormatterCleanup(entry, FieldFormatterCleanup.PAGE_NUMBERS, ce);
    }

    private static void fixWrongFileEntries(BibtexEntry entry, NamedCompound ce) {
        String oldValue = entry.getField(Globals.FILE_FIELD);
        if (oldValue == null) {
            return;
        }
        FileListTableModel flModel = new FileListTableModel();
        flModel.setContent(oldValue);
        if (flModel.getRowCount() == 0) {
            return;
        }
        boolean changed = false;
        for (int i = 0; i < flModel.getRowCount(); i++) {
            FileListEntry flEntry = flModel.getEntry(i);
            String link = flEntry.getLink();
            String description = flEntry.getDescription();
            if ("".equals(link) && (!"".equals(description))) {
                // link and description seem to be switched, quickly fix that
                flEntry.setLink(flEntry.getDescription());
                flEntry.setDescription("");
                changed = true;
            }
        }
        if (changed) {
            String newValue = flModel.getStringRepresentation();
            assert (!oldValue.equals(newValue));
            entry.setField(Globals.FILE_FIELD, newValue);
            ce.addEdit(new UndoableFieldChange(entry, Globals.FILE_FIELD, oldValue, newValue));
        }
    }

    private void doMakePathsRelative(BibtexEntry entry, NamedCompound ce) {
        String oldValue = entry.getField(Globals.FILE_FIELD);
        if (oldValue == null) {
            return;
        }
        FileListTableModel flModel = new FileListTableModel();
        flModel.setContent(oldValue);
        if (flModel.getRowCount() == 0) {
            return;
        }
        boolean changed = false;
        for (int i = 0; i < flModel.getRowCount(); i++) {
            FileListEntry flEntry = flModel.getEntry(i);
            String oldFileName = flEntry.getLink();
            String newFileName = FileUtil.shortenFileName(new File(oldFileName), panel.metaData().getFileDirectory(Globals.FILE_FIELD)).toString();
            if (!oldFileName.equals(newFileName)) {
                flEntry.setLink(newFileName);
                changed = true;
            }
        }
        if (changed) {
            String newValue = flModel.getStringRepresentation();
            assert (!oldValue.equals(newValue));
            entry.setField(Globals.FILE_FIELD, newValue);
            ce.addEdit(new UndoableFieldChange(entry, Globals.FILE_FIELD, oldValue, newValue));
        }
    }

    private void doRenamePDFs(BibtexEntry entry, NamedCompound ce) {
        //Extract the path
        String oldValue = entry.getField(Globals.FILE_FIELD);
        if (oldValue == null) {
            return;
        }
        FileListTableModel flModel = new FileListTableModel();
        flModel.setContent(oldValue);
        if (flModel.getRowCount() == 0) {
            return;
        }
        boolean changed = false;

        for (int i = 0; i < flModel.getRowCount(); i++) {
            String realOldFilename = flModel.getEntry(i).getLink();

            if (cleanUpRenamePDFonlyRelativePaths.isSelected() && (new File(realOldFilename).isAbsolute())) {
                continue;
            }

            String newFilename = Util.getLinkedFileName(panel.database(), entry);
            //String oldFilename = bes.getField(GUIGlobals.FILE_FIELD); // would have to be stored for undoing purposes

            //Add extension to newFilename
            newFilename = newFilename + "." + flModel.getEntry(i).getType().getExtension();

            //get new Filename with path
            //Create new Path based on old Path and new filename
            File expandedOldFile = FileUtil.expandFilename(realOldFilename, panel.metaData().getFileDirectory(Globals.FILE_FIELD));
            if (expandedOldFile.getParent() == null) {
                // something went wrong. Just skip this entry
                continue;
            }
            String newPath = expandedOldFile.getParent().concat(System.getProperty("file.separator")).concat(newFilename);

            if (new File(newPath).exists()) {
                // we do not overwrite files
                // TODO: we could check here if the newPath file is linked with the current entry. And if not, we could add a link
                continue;
            }

            //do rename
            boolean renameSuccessful = FileUtil.renameFile(expandedOldFile.toString(), newPath);

            if (renameSuccessful) {
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
                unsuccessfulRenames++;
            }
        }

        if (changed) {
            String newValue = flModel.getStringRepresentation();
            assert (!oldValue.equals(newValue));
            entry.setField(Globals.FILE_FIELD, newValue);
            //we put an undo of the field content here
            //the file is not being renamed back, which leads to inconsistencies
            //if we put a null undo object here, the change by "doMakePathsRelative" would overwrite the field value nevertheless.
            ce.addEdit(new UndoableFieldChange(entry, Globals.FILE_FIELD, oldValue, newValue));
        }
    }

    /**
     * Converts HTML code to LaTeX code
     */
    private static void doConvertHTML(BibtexEntry entry, NamedCompound ce) {
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
    private static void doConvertUnicode(BibtexEntry entry, NamedCompound ce) {
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
    private static void doConvertCase(BibtexEntry entry, NamedCompound ce) {
        doFieldFormatterCleanup(entry, FieldFormatterCleanup.TITLE_CASE, ce);
    }

    private static void doConvertUnits(BibtexEntry entry, NamedCompound ce) {
        doFieldFormatterCleanup(entry, FieldFormatterCleanup.TITLE_UNITS, ce);
    }

    private static void doConvertLaTeX(BibtexEntry entry, NamedCompound ce) {
        doFieldFormatterCleanup(entry, FieldFormatterCleanup.TITLE_LATEX, ce);
    }

    /**
     * Format dates correctly (yyyy-mm-dd or yyyy-mm)
     */
    private static void doCleanUpDate(BibtexEntry entry, NamedCompound ce) {
        doFieldFormatterCleanup(entry, FieldFormatterCleanup.DATES, ce);
    }

    /**
     * Runs the field formatter on the entry and records the change.
     */
    private static void doFieldFormatterCleanup(BibtexEntry entry, FieldFormatterCleanup cleaner, NamedCompound ce) {
        String oldValue = entry.getField(cleaner.getField());
        if (oldValue == null) {
            return;
        }

        // run formatter
        cleaner.cleanup(entry);

        String newValue = entry.getField(cleaner.getField());

        // undo action
        if (!oldValue.equals(newValue)) {
            ce.addEdit(new UndoableFieldChange(entry, cleaner.getField(), oldValue, newValue));
        }
    }
}
