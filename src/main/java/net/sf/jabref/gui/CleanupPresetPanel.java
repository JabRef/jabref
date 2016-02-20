package net.sf.jabref.gui;

import java.util.EnumSet;
import java.util.Objects;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.preftabs.ImportSettingsTab;
import net.sf.jabref.logic.cleanup.CleanupPreset;
import net.sf.jabref.logic.l10n.Localization;

public class CleanupPresetPanel {

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

    private JPanel panel;
    private CleanupPreset cleanupPreset;

    public CleanupPresetPanel(CleanupPreset cleanupPreset) {
        this.cleanupPreset = Objects.requireNonNull(cleanupPreset);

        init();
    }

    private void init() {
        cleanUpSuperscripts = new JCheckBox(Localization.lang("Convert 1st, 2nd, ... to real superscripts"));
        cleanUpDOI = new JCheckBox(
                Localization.lang("Move DOIs from note and URL field to DOI field and remove http prefix"));
        cleanUpMonth = new JCheckBox(Localization.lang("Format content of month field to #mon#"));
        cleanUpPageNumbers = new JCheckBox(Localization.lang("Ensure that page ranges are of the form num1--num2"));
        cleanUpDate = new JCheckBox(Localization.lang("Format date field in the form yyyy-mm or yyyy-mm-dd"));
        cleanUpMakePathsRelative = new JCheckBox(
                Localization.lang("Make paths of linked files relative (if possible)"));
        cleanUpRenamePDF = new JCheckBox(Localization.lang("Rename PDFs to given filename format pattern"));
        cleanUpRenamePDF.addChangeListener(
                event -> cleanUpRenamePDFonlyRelativePaths.setEnabled(cleanUpRenamePDF.isSelected()));
        cleanUpRenamePDFonlyRelativePaths = new JCheckBox(Localization.lang("Rename only PDFs having a relative path"));
        cleanUpUpgradeExternalLinks = new JCheckBox(
                Localization.lang("Upgrade external PDF/PS links to use the '%0' field.", Globals.FILE_FIELD));
        cleanUpHTML = new JCheckBox(Localization.lang("Run HTML converter on title"));
        cleanUpCase = new JCheckBox(Localization.lang("Run filter on title keeping the case of selected words"));
        cleanUpLaTeX = new JCheckBox(
                Localization.lang("Remove unneccessary $, {, and } and move adjacent numbers into equations"));
        cleanUpUnits = new JCheckBox(
                Localization.lang("Add brackets and replace separators with their non-breaking version for units"));
        cleanUpUnicode = new JCheckBox(Localization.lang("Run Unicode converter on title, author(s), and abstract"));
        cleanUpBibLatex = new JCheckBox(Localization
                .lang("Convert to BibLatex format (for example, move the value of the 'journal' field to 'journaltitle')"));
        updateDisplay(cleanupPreset);

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
        String currentPattern = Localization.lang("Filename format pattern").concat(": ").concat(
                Globals.prefs.get(ImportSettingsTab.PREF_IMPORT_FILENAMEPATTERN));
        builder.add(new JLabel(currentPattern)).xy(2, 14);
        builder.add(cleanUpRenamePDFonlyRelativePaths).xy(2, 15);
        builder.add(cleanUpBibLatex).xyw(1, 16, 2);
        panel = builder.build();
    }

    public void updateDisplay(CleanupPreset preset) {
        cleanUpSuperscripts.setSelected(preset.isCleanUpSuperscripts());
        cleanUpDOI.setSelected(preset.isCleanUpDOI());
        cleanUpMonth.setSelected(preset.isCleanUpMonth());
        cleanUpPageNumbers.setSelected(preset.isCleanUpPageNumbers());
        cleanUpDate.setSelected(preset.isCleanUpDate());
        cleanUpMakePathsRelative.setSelected(preset.isMakePathsRelative());
        cleanUpRenamePDF.setSelected(preset.isRenamePDF());
        cleanUpRenamePDFonlyRelativePaths.setSelected(preset.isRenamePdfOnlyRelativePaths());
        cleanUpRenamePDFonlyRelativePaths.setEnabled(cleanUpRenamePDF.isSelected());
        cleanUpUpgradeExternalLinks.setSelected(preset.isCleanUpUpgradeExternalLinks());
        cleanUpHTML.setSelected(preset.isConvertHTMLToLatex());
        cleanUpCase.setSelected(preset.isConvertCase());
        cleanUpLaTeX.setSelected(preset.isConvertLaTeX());
        cleanUpUnits.setSelected(preset.isConvertUnits());
        cleanUpUnicode.setSelected(preset.isConvertUnicodeToLatex());
        cleanUpBibLatex.setSelected(preset.isConvertToBiblatex());
    }

    public JPanel getPanel() {
        return panel;
    }

    public CleanupPreset getCleanupPreset() {

        EnumSet<CleanupPreset.CleanupStep> activeJobs = EnumSet.noneOf(CleanupPreset.CleanupStep.class);

        if (cleanUpSuperscripts.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CLEAN_UP_SUPERSCRIPTS);
        }
        if (cleanUpDOI.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        }
        if (cleanUpMonth.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CLEAN_UP_MONTH);
        }
        if (cleanUpPageNumbers.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CLEAN_UP_PAGE_NUMBERS);
        }
        if (cleanUpDate.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CLEAN_UP_DATE);
        }
        if (cleanUpMakePathsRelative.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.MAKE_PATHS_RELATIVE);
        }
        if (cleanUpRenamePDF.isSelected()) {
            if (cleanUpRenamePDFonlyRelativePaths.isSelected()) {
                activeJobs.add(CleanupPreset.CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS);
            } else {
                activeJobs.add(CleanupPreset.CleanupStep.RENAME_PDF);
            }
        }

        if (cleanUpUpgradeExternalLinks.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        }
        if (cleanUpHTML.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_HTML_TO_LATEX);
        }
        if (cleanUpCase.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_CASE);
        }
        if (cleanUpLaTeX.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_LATEX);
        }
        if (cleanUpUnits.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_UNITS);
        }
        if (cleanUpUnicode.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_UNICODE_TO_LATEX);
        }
        if (cleanUpBibLatex.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        }

        activeJobs.add(CleanupPreset.CleanupStep.FIX_FILE_LINKS);

        cleanupPreset = new CleanupPreset(activeJobs);
        return cleanupPreset;
    }
}
