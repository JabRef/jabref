package net.sf.jabref.gui.cleanup;

import java.util.EnumSet;
import java.util.Objects;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.preftabs.ImportSettingsTab;
import net.sf.jabref.logic.cleanup.CleanupPreset;
import net.sf.jabref.logic.l10n.Localization;

public class CleanupPresetPanel {

    private final BibDatabaseContext databaseContext;
    private JCheckBox cleanUpSuperscripts;
    private JCheckBox cleanUpDOI;
    private JCheckBox cleanUpMovePDF;
    private JCheckBox cleanUpMakePathsRelative;
    private JCheckBox cleanUpRenamePDF;
    private JCheckBox cleanUpRenamePDFonlyRelativePaths;
    private JCheckBox cleanUpUpgradeExternalLinks;
    private JCheckBox cleanUpUnicode;
    private JCheckBox cleanUpBibLatex;
    private FieldFormatterCleanupsPanel cleanUpFormatters;

    private JPanel panel;
    private CleanupPreset cleanupPreset;



    public CleanupPresetPanel(BibDatabaseContext databaseContext, CleanupPreset cleanupPreset) {
        this.cleanupPreset = Objects.requireNonNull(cleanupPreset);
        this.databaseContext = Objects.requireNonNull(databaseContext);
        init();
    }

    private void init() {
        cleanUpSuperscripts = new JCheckBox(Localization.lang("Convert 1st, 2nd, ... to real superscripts"));
        cleanUpDOI = new JCheckBox(
                Localization.lang("Move DOIs from note and URL field to DOI field and remove http prefix"));
        if (databaseContext.getMetaData().getDefaultFileDirectory().isPresent()) {
            cleanUpMovePDF = new JCheckBox(Localization.lang("Move linked files to default file directory %0",
                    databaseContext.getMetaData().getDefaultFileDirectory().get()));
        } else {
            cleanUpMovePDF = new JCheckBox(Localization.lang("Move linked files to default file directory %0", "..."));
            cleanUpMovePDF.setEnabled(false);
            cleanUpMovePDF.setSelected(false);
        }


        cleanUpMakePathsRelative = new JCheckBox(
                Localization.lang("Make paths of linked files relative (if possible)"));
        cleanUpRenamePDF = new JCheckBox(Localization.lang("Rename PDFs to given filename format pattern"));
        cleanUpRenamePDF.addChangeListener(
                event -> cleanUpRenamePDFonlyRelativePaths.setEnabled(cleanUpRenamePDF.isSelected()));
        cleanUpRenamePDFonlyRelativePaths = new JCheckBox(Localization.lang("Rename only PDFs having a relative path"));
        cleanUpUpgradeExternalLinks = new JCheckBox(
                Localization.lang("Upgrade external PDF/PS links to use the '%0' field.", Globals.FILE_FIELD));
        cleanUpUnicode = new JCheckBox(Localization.lang("Run Unicode converter on title, author(s), and abstract"));
        cleanUpBibLatex = new JCheckBox(Localization.lang(
                "Convert to BibLatex format (for example, move the value of the 'journal' field to 'journaltitle')"));

        cleanUpFormatters = new FieldFormatterCleanupsPanel(Localization.lang("Run field formatter:"),
                JabRefPreferences.CLEANUP_DEFAULT_PRESET.getFormatterCleanups());

        updateDisplay(cleanupPreset);

        FormLayout layout = new FormLayout("left:15dlu, pref:grow",
                "pref, pref, pref, pref, pref, pref, pref, pref, pref,pref, 190dlu, fill:pref:grow,");

        FormBuilder builder = FormBuilder.create().layout(layout);
        builder.add(cleanUpUnicode).xyw(1, 1, 2);
        builder.add(cleanUpSuperscripts).xyw(1, 2, 2);
        builder.add(cleanUpDOI).xyw(1, 3, 2);
        builder.add(cleanUpUpgradeExternalLinks).xyw(1, 4, 2);
        builder.add(cleanUpMovePDF).xyw(1, 5, 2);
        builder.add(cleanUpMakePathsRelative).xyw(1, 6, 2);
        builder.add(cleanUpRenamePDF).xyw(1, 7, 2);
        String currentPattern = Localization.lang("Filename format pattern").concat(": ")
                .concat(Globals.prefs.get(ImportSettingsTab.PREF_IMPORT_FILENAMEPATTERN));
        builder.add(new JLabel(currentPattern)).xy(2, 8);
        builder.add(cleanUpRenamePDFonlyRelativePaths).xy(2, 9);
        builder.add(cleanUpBibLatex).xyw(1, 10, 2);
        builder.add(cleanUpFormatters).xyw(1, 11, 2);
        panel = builder.build();
    }

    private void updateDisplay(CleanupPreset preset) {
        cleanUpSuperscripts.setSelected(preset.isCleanUpSuperscripts());
        cleanUpDOI.setSelected(preset.isCleanUpDOI());
        if (cleanUpMovePDF.isEnabled()) {
            cleanUpMovePDF.setSelected(preset.isMovePDF());
        }
        cleanUpMakePathsRelative.setSelected(preset.isMakePathsRelative());
        cleanUpRenamePDF.setSelected(preset.isRenamePDF());
        cleanUpRenamePDFonlyRelativePaths.setSelected(preset.isRenamePdfOnlyRelativePaths());
        cleanUpRenamePDFonlyRelativePaths.setEnabled(cleanUpRenamePDF.isSelected());
        cleanUpUpgradeExternalLinks.setSelected(preset.isCleanUpUpgradeExternalLinks());
        cleanUpUnicode.setSelected(preset.isConvertUnicodeToLatex());
        cleanUpBibLatex.setSelected(preset.isConvertToBiblatex());
        cleanUpFormatters.setValues(preset.getFormatterCleanups());
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
        if (cleanUpUnicode.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_UNICODE_TO_LATEX);
        }
        if (cleanUpBibLatex.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        }

        activeJobs.add(CleanupPreset.CleanupStep.FIX_FILE_LINKS);

        cleanupPreset = new CleanupPreset(activeJobs, cleanUpFormatters.getFormatterCleanups());
        return cleanupPreset;
    }
}
