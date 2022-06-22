package org.jabref.gui.cleanup;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.commonfxcontrols.FieldFormatterCleanupsPanel;
import org.jabref.logic.cleanup.CleanupPreset;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.FilePreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class CleanupPresetPanel extends VBox {

    private final BibDatabaseContext databaseContext;
    @FXML private Label cleanupRenamePDFLabel;
    @FXML private CheckBox cleanUpDOI;
    @FXML private CheckBox cleanUpEprint;
    @FXML private CheckBox cleanUpISSN;
    @FXML private CheckBox cleanUpMovePDF;
    @FXML private CheckBox cleanUpMakePathsRelative;
    @FXML private CheckBox cleanUpRenamePDF;
    @FXML private CheckBox cleanUpRenamePDFonlyRelativePaths;
    @FXML private CheckBox cleanUpUpgradeExternalLinks;
    @FXML private CheckBox cleanUpBiblatex;
    @FXML private CheckBox cleanUpBibtex;
    @FXML private CheckBox cleanUpTimestampToCreationDate;
    @FXML private CheckBox cleanUpTimestampToModificationDate;
    @FXML private FieldFormatterCleanupsPanel formatterCleanupsPanel;

    public CleanupPresetPanel(BibDatabaseContext databaseContext, CleanupPreset cleanupPreset, FilePreferences filePreferences) {
        this.databaseContext = Objects.requireNonNull(databaseContext);

        // Load FXML
        ViewLoader.view(this)
                  .root(this)
                  .load();

        init(cleanupPreset, filePreferences);
    }

    private void init(CleanupPreset cleanupPreset, FilePreferences filePreferences) {
        Optional<Path> firstExistingDir = databaseContext.getFirstExistingFileDir(filePreferences);
        if (firstExistingDir.isPresent()) {
            cleanUpMovePDF.setText(Localization.lang("Move linked files to default file directory %0", firstExistingDir.get().toString()));
        } else {
            cleanUpMovePDF.setText(Localization.lang("Move linked files to default file directory %0", "..."));

            // Since the directory does not exist, we cannot move it to there. So, this option is not checked - regardless of the presets stored in the preferences.
            cleanUpMovePDF.setDisable(true);
            cleanUpMovePDF.setSelected(false);
        }

        cleanUpRenamePDFonlyRelativePaths.disableProperty().bind(cleanUpRenamePDF.selectedProperty().not());

        cleanUpUpgradeExternalLinks.setText(Localization.lang("Upgrade external PDF/PS links to use the '%0' field.", StandardField.FILE.getDisplayName()));

        String currentPattern = Localization.lang("Filename format pattern")
                                            .concat(": ")
                                            .concat(filePreferences.getFileNamePattern());
        cleanupRenamePDFLabel.setText(currentPattern);
        cleanUpBibtex.selectedProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue) {
                        cleanUpBiblatex.selectedProperty().setValue(false);
                    }
                });
        cleanUpBiblatex.selectedProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue) {
                        cleanUpBibtex.selectedProperty().setValue(false);
                    }
                });
        cleanUpTimestampToCreationDate.selectedProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue) {
                        cleanUpTimestampToModificationDate.selectedProperty().setValue(false);
                    }
                });
        cleanUpTimestampToModificationDate.selectedProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue) {
                        cleanUpTimestampToCreationDate.selectedProperty().setValue(false);
                    }
                });
        updateDisplay(cleanupPreset);
    }

    private void updateDisplay(CleanupPreset preset) {
        cleanUpDOI.setSelected(preset.isActive(CleanupPreset.CleanupStep.CLEAN_UP_DOI));
        cleanUpEprint.setSelected(preset.isActive(CleanupPreset.CleanupStep.CLEANUP_EPRINT));
        if (!cleanUpMovePDF.isDisabled()) {
            cleanUpMovePDF.setSelected(preset.isActive(CleanupPreset.CleanupStep.MOVE_PDF));
        }
        cleanUpMakePathsRelative.setSelected(preset.isActive(CleanupPreset.CleanupStep.MAKE_PATHS_RELATIVE));
        cleanUpRenamePDF.setSelected(preset.isRenamePDFActive());
        cleanUpRenamePDFonlyRelativePaths.setSelected(preset.isActive(CleanupPreset.CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS));
        cleanUpUpgradeExternalLinks.setSelected(preset.isActive(CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS));
        cleanUpBiblatex.setSelected(preset.isActive(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX));
        cleanUpBibtex.setSelected(preset.isActive(CleanupPreset.CleanupStep.CONVERT_TO_BIBTEX));
        cleanUpTimestampToCreationDate.setSelected(preset.isActive(CleanupPreset.CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE));
        cleanUpTimestampToModificationDate.setSelected(preset.isActive(CleanupPreset.CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE));
        cleanUpTimestampToModificationDate.setSelected(preset.isActive(CleanupPreset.CleanupStep.DO_NOT_CONVERT_TIMESTAMP));
        cleanUpISSN.setSelected(preset.isActive(CleanupPreset.CleanupStep.CLEAN_UP_ISSN));
        formatterCleanupsPanel.cleanupsDisableProperty().setValue(!preset.getFormatterCleanups().isEnabled());
        formatterCleanupsPanel.cleanupsProperty().setValue(FXCollections.observableArrayList(preset.getFormatterCleanups().getConfiguredActions()));
    }

    public CleanupPreset getCleanupPreset() {
        Set<CleanupPreset.CleanupStep> activeJobs = EnumSet.noneOf(CleanupPreset.CleanupStep.class);

        if (cleanUpMovePDF.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.MOVE_PDF);
        }
        if (cleanUpDOI.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        }
        if (cleanUpEprint.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CLEANUP_EPRINT);
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
        if (cleanUpTimestampToCreationDate.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE);
        }
        if (cleanUpTimestampToModificationDate.isSelected()) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE);
        }

        activeJobs.add(CleanupPreset.CleanupStep.FIX_FILE_LINKS);

        return new CleanupPreset(activeJobs, new FieldFormatterCleanups(
                !formatterCleanupsPanel.cleanupsDisableProperty().getValue(),
                formatterCleanupsPanel.cleanupsProperty()));
    }
}
