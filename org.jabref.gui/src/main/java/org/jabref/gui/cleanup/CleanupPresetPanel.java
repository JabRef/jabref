package org.jabref.gui.cleanup;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jabref.Globals;
import org.jabref.logic.cleanup.CleanupPreset;
import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.FieldName;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class CleanupPresetPanel {

    private final BibDatabaseContext databaseContext;
    private JCheckBox cleanUpDOI;
    private JCheckBox cleanUpISSN;
    private JCheckBox cleanUpMovePDF;
    private JCheckBox cleanUpMakePathsRelative;
    private JCheckBox cleanUpRenamePDF;
    private JCheckBox cleanUpRenamePDFonlyRelativePaths;
    private JCheckBox cleanUpUpgradeExternalLinks;
    private JCheckBox cleanUpBiblatex;
    private JCheckBox cleanUpBibtex;
    private FieldFormatterCleanupsPanel cleanUpFormatters;

    private JPanel panel;
    private JScrollPane scrollPane;
    private CleanupPreset cleanupPreset;

    public CleanupPresetPanel(BibDatabaseContext databaseContext, CleanupPreset cleanupPreset) {
        this.cleanupPreset = Objects.requireNonNull(cleanupPreset);
        this.databaseContext = Objects.requireNonNull(databaseContext);
        init();
    }

    private void init() {
        cleanUpDOI = new JCheckBox(
                Localization.lang("Move DOIs from note and URL field to DOI field and remove http prefix"));
        cleanUpISSN = new JCheckBox(Localization.lang("Reformat ISSN"));

        Optional<Path> firstExistingDir = databaseContext
                .getFirstExistingFileDir(JabRefPreferences.getInstance().getFileDirectoryPreferences());
        if (firstExistingDir.isPresent()) {
            cleanUpMovePDF = new JCheckBox(Localization.lang("Move linked files to default file directory %0",
                    firstExistingDir.get().toString()));
        } else {
            cleanUpMovePDF = new JCheckBox(Localization.lang("Move linked files to default file directory %0", "..."));
            cleanUpMovePDF.setEnabled(false);
            // Since the directory does not exist, we cannot move it to there. So, this option is not checked - regardless of the presets stored in the preferences.
            cleanUpMovePDF.setSelected(false);
        }

        cleanUpMakePathsRelative = new JCheckBox(
                Localization.lang("Make paths of linked files relative (if possible)"));
        cleanUpRenamePDF = new JCheckBox(Localization.lang("Rename PDFs to given filename format pattern"));
        cleanUpRenamePDF.addChangeListener(
                event -> cleanUpRenamePDFonlyRelativePaths.setEnabled(cleanUpRenamePDF.isSelected()));
        cleanUpRenamePDFonlyRelativePaths = new JCheckBox(Localization.lang("Rename only PDFs having a relative path"));
        cleanUpUpgradeExternalLinks = new JCheckBox(
                Localization.lang("Upgrade external PDF/PS links to use the '%0' field.", FieldName.FILE));
        cleanUpBiblatex = new JCheckBox(Localization.lang(
                "Convert to biblatex format (for example, move the value of the 'journal' field to 'journaltitle')"));
        cleanUpBibtex = new JCheckBox(Localization.lang(
                "Convert to BibTeX format (for example, move the value of the 'journaltitle' field to 'journal')"));
        ButtonGroup biblatexConversion = new ButtonGroup(); // Only make "to Biblatex" or "to BibTeX" selectable
        biblatexConversion.add(cleanUpBiblatex);
        biblatexConversion.add(cleanUpBibtex);

        cleanUpFormatters = new FieldFormatterCleanupsPanel(Localization.lang("Run field formatter:"),
                Cleanups.DEFAULT_SAVE_ACTIONS);

        updateDisplay(cleanupPreset);

        FormLayout layout = new FormLayout("left:15dlu, fill:pref:grow",
                "pref, pref, pref, pref, pref, fill:pref:grow, pref,pref, pref, pref,190dlu, fill:pref:grow,");

        FormBuilder builder = FormBuilder.create().layout(layout);
        builder.add(cleanUpDOI).xyw(1, 1, 2);
        builder.add(cleanUpUpgradeExternalLinks).xyw(1, 2, 2);
        builder.add(cleanUpMovePDF).xyw(1, 3, 2);
        builder.add(cleanUpMakePathsRelative).xyw(1, 4, 2);
        builder.add(cleanUpRenamePDF).xyw(1, 5, 2);
        String currentPattern = Localization.lang("Filename format pattern").concat(": ");
        currentPattern = currentPattern.concat(Globals.prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN));
        builder.add(new JLabel(currentPattern)).xy(2, 6);
        builder.add(cleanUpRenamePDFonlyRelativePaths).xy(2, 7);
        builder.add(cleanUpBibtex).xyw(1, 8, 2);
        builder.add(cleanUpBiblatex).xyw(1, 9, 2);
        builder.add(cleanUpISSN).xyw(1, 10, 2);
        builder.add(cleanUpFormatters).xyw(1, 11, 2);
        panel = builder.build();
        scrollPane = new JScrollPane(panel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVisible(true);
        scrollPane.setBorder(null);
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
        cleanUpBiblatex.setSelected(preset.isConvertToBiblatex());
        cleanUpBibtex.setSelected(preset.isConvertToBibtex());
        cleanUpISSN.setSelected(preset.isCleanUpISSN());
        cleanUpFormatters.setValues(preset.getFormatterCleanups());
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public CleanupPreset getCleanupPreset() {

        Set<CleanupPreset.CleanupStep> activeJobs = EnumSet.noneOf(CleanupPreset.CleanupStep.class);

        if (cleanUpMovePDF.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.MOVE_PDF);
        }

        if (cleanUpDOI.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        }
        if (cleanUpISSN.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CLEAN_UP_ISSN);
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
        if (cleanUpBiblatex.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        }
        if (cleanUpBibtex.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_TO_BIBTEX);
        }

        activeJobs.add(CleanupPreset.CleanupStep.FIX_FILE_LINKS);

        cleanupPreset = new CleanupPreset(activeJobs, cleanUpFormatters.getFormatterCleanups());
        return cleanupPreset;
    }
}
