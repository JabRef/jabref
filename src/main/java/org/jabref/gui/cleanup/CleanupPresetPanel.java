package org.jabref.gui.cleanup;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javafx.scene.Group;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;

import org.jabref.Globals;
import org.jabref.logic.cleanup.CleanupPreset;
import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.FieldName;
import org.jabref.preferences.JabRefPreferences;

public class CleanupPresetPanel extends ScrollPane {

    private final BibDatabaseContext databaseContext;
    private CheckBox cleanUpDOI;
    private CheckBox cleanUpISSN;
    private CheckBox cleanUpMovePDF;
    private CheckBox cleanUpMakePathsRelative;
    private CheckBox cleanUpRenamePDF;
    private CheckBox cleanUpRenamePDFonlyRelativePaths;
    private CheckBox cleanUpUpgradeExternalLinks;
    private CheckBox cleanUpBiblatex;
    private CheckBox cleanUpBibtex;
    private FieldFormatterCleanupsPanel cleanUpFormatters;

    private CleanupPreset cleanupPreset;

    public CleanupPresetPanel(BibDatabaseContext databaseContext, CleanupPreset cleanupPreset) {
        this.cleanupPreset = Objects.requireNonNull(cleanupPreset);
        this.databaseContext = Objects.requireNonNull(databaseContext);
        init();
    }

    private void init() {
        cleanUpDOI = new CheckBox(
                Localization.lang("Move DOIs from note and URL field to DOI field and remove http prefix"));
        cleanUpISSN = new CheckBox(Localization.lang("Reformat ISSN"));
        Optional<Path> firstExistingDir = databaseContext
                .getFirstExistingFileDir(JabRefPreferences.getInstance().getFilePreferences());
        if (firstExistingDir.isPresent()) {
            cleanUpMovePDF = new CheckBox(Localization.lang("Move linked files to default file directory %0",
                    firstExistingDir.get().toString()));
        } else {
            cleanUpMovePDF = new CheckBox(Localization.lang("Move linked files to default file directory %0", "..."));
            cleanUpMovePDF.setDisable(true);
            // Since the directory does not exist, we cannot move it to there. So, this option is not checked - regardless of the presets stored in the preferences.
            cleanUpMovePDF.setSelected(false);
        }
        cleanUpMakePathsRelative = new CheckBox(
                Localization.lang("Make paths of linked files relative (if possible)"));
        cleanUpRenamePDF = new CheckBox(Localization.lang("Rename PDFs to given filename format pattern"));
        cleanUpRenamePDF.selectedProperty().addListener(
                                                        event -> cleanUpRenamePDFonlyRelativePaths.setDisable(!cleanUpRenamePDF.isSelected()));
        cleanUpRenamePDFonlyRelativePaths = new CheckBox(Localization.lang("Rename only PDFs having a relative path"));
        cleanUpUpgradeExternalLinks = new CheckBox(
                Localization.lang("Upgrade external PDF/PS links to use the '%0' field.", FieldName.FILE));
        cleanUpBiblatex = new CheckBox(Localization.lang(
                "Convert to biblatex format (for example, move the value of the 'journal' field to 'journaltitle')"));
        cleanUpBibtex = new CheckBox(Localization.lang(
                "Convert to BibTeX format (for example, move the value of the 'journaltitle' field to 'journal')"));
        Group biblatexConversion = new Group(); // Only make "to Biblatex" or "to BibTeX" selectable
        biblatexConversion.getChildren().add(cleanUpBiblatex);
        biblatexConversion.getChildren().add(cleanUpBibtex);

        cleanUpFormatters = new FieldFormatterCleanupsPanel(Localization.lang("Run field formatter:"),
                Cleanups.DEFAULT_SAVE_ACTIONS);

        updateDisplay(cleanupPreset);

        GridPane container = new GridPane();
        container.add(cleanUpDOI, 0, 0);
        container.add(cleanUpUpgradeExternalLinks, 0, 1);
        container.add(cleanUpMovePDF, 0, 2);
        container.add(cleanUpMakePathsRelative, 0, 3);
        container.add(cleanUpRenamePDF, 0, 4);
        String currentPattern = Localization.lang("Filename format pattern").concat(": ");
        currentPattern = currentPattern.concat(Globals.prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN));
        container.add(new Label(currentPattern), 0, 5);
        container.add(cleanUpRenamePDFonlyRelativePaths, 0, 6);
        container.add(cleanUpBibtex, 0, 7);
        container.add(cleanUpBiblatex, 0, 8);
        container.add(cleanUpISSN, 0, 9);
        container.add(cleanUpFormatters, 0, 10);

        setContent(container);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    }

    private void updateDisplay(CleanupPreset preset) {
        cleanUpDOI.setSelected(preset.isCleanUpDOI());
        if (!cleanUpMovePDF.isDisabled()) {
            cleanUpMovePDF.setSelected(preset.isMovePDF());
        }
        cleanUpMakePathsRelative.setSelected(preset.isMakePathsRelative());
        cleanUpRenamePDF.setSelected(preset.isRenamePDF());
        cleanUpRenamePDFonlyRelativePaths.setSelected(preset.isRenamePdfOnlyRelativePaths());
        cleanUpRenamePDFonlyRelativePaths.setDisable(!cleanUpRenamePDF.isSelected());
        cleanUpUpgradeExternalLinks.setSelected(preset.isCleanUpUpgradeExternalLinks());
        cleanUpBiblatex.setSelected(preset.isConvertToBiblatex());
        cleanUpBibtex.setSelected(preset.isConvertToBibtex());
        cleanUpISSN.setSelected(preset.isCleanUpISSN());
        cleanUpFormatters.setValues(preset.getFormatterCleanups());
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
