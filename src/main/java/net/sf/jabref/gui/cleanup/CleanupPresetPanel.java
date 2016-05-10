package net.sf.jabref.gui.cleanup;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.cleanup.CleanupPreset;
import net.sf.jabref.logic.l10n.Localization;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class CleanupPresetPanel {

    private final BibDatabaseContext databaseContext;
    private JCheckBox cleanUpDOI;
    private JCheckBox cleanUpMovePDF;
    private JCheckBox cleanUpMakePathsRelative;
    private JCheckBox cleanUpRenamePDF;
    private JCheckBox cleanUpRenamePDFonlyRelativePaths;
    private JCheckBox cleanUpUpgradeExternalLinks;
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
        cleanUpBibLatex = new JCheckBox(Localization.lang(
                "Convert to BibLatex format (for example, move the value of the 'journal' field to 'journaltitle')"));

        cleanUpFormatters = new FieldFormatterCleanupsPanel(Localization.lang("Run field formatter:"),
                JabRefPreferences.CLEANUP_DEFAULT_PRESET.getFormatterCleanups());

        updateDisplay(cleanupPreset);

        FormLayout layout = new FormLayout("left:15dlu, pref:grow",
                "pref, pref, pref, pref, pref, pref, pref,pref, 190dlu, fill:pref:grow,");

        FormBuilder builder = FormBuilder.create().layout(layout);
        builder.add(cleanUpDOI).xyw(1, 1, 2);
        builder.add(cleanUpUpgradeExternalLinks).xyw(1, 2, 2);
        builder.add(cleanUpMovePDF).xyw(1, 3, 2);
        builder.add(cleanUpMakePathsRelative).xyw(1, 4, 2);
        builder.add(cleanUpRenamePDF).xyw(1, 5, 2);
        String currentPattern = Localization.lang("Filename format pattern").concat(": ")
                .concat(Globals.prefs.get(JabRefPreferences.PREF_IMPORT_FILENAMEPATTERN));
        builder.add(new JLabel(currentPattern)).xy(2, 6);
        builder.add(cleanUpRenamePDFonlyRelativePaths).xy(2, 7);
        builder.add(cleanUpBibLatex).xyw(1, 8, 2);
        builder.add(cleanUpFormatters).xyw(1, 9, 2);
        panel = builder.build();
    }

    private void updateDisplay(CleanupPreset preset) {
        cleanUpDOI.setSelected(preset.isCleanUpDOI());
        if (cleanUpMovePDF.isEnabled()) {
            cleanUpMovePDF.setSelected(preset.isMovePDF());
        }
        cleanUpMakePathsRelative.setSelected(preset.isMakePathsRelative());
        cleanUpRenamePDF.setSelected(preset.isRenamePDF());
        cleanUpRenamePDFonlyRelativePaths.setSelected(preset.isRenamePdfOnlyRelativePaths());
        cleanUpRenamePDFonlyRelativePaths.setEnabled(cleanUpRenamePDF.isSelected());
        cleanUpUpgradeExternalLinks.setSelected(preset.isCleanUpUpgradeExternalLinks());
        cleanUpBibLatex.setSelected(preset.isConvertToBiblatex());
        cleanUpFormatters.setValues(preset.getFormatterCleanups());
    }

    public JPanel getPanel() {
        return panel;
    }

    public CleanupPreset getCleanupPreset() {

        Set<CleanupPreset.CleanupStep> activeJobs = EnumSet.noneOf(CleanupPreset.CleanupStep.class);

        if (cleanUpMovePDF.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.MOVE_PDF);
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
        if (cleanUpBibLatex.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        }

        activeJobs.add(CleanupPreset.CleanupStep.FIX_FILE_LINKS);

        cleanupPreset = new CleanupPreset(activeJobs, cleanUpFormatters.getFormatterCleanups());
        return cleanupPreset;
    }
}
