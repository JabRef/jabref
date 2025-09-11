package org.jabref.gui.cleanup;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.views.ViewLoader;

public class CleanupFileRelatedPanel extends VBox implements CleanupPanel {

    @FXML private Label cleanupRenamePdfLabel;

    @FXML private CheckBox cleanUpMovePdf;
    @FXML private CheckBox cleanUpMakePathsRelative;
    @FXML private CheckBox cleanUpRenamePdf;
    @FXML private CheckBox cleanUpRenamePdfonlyRelativePaths;
    @FXML private CheckBox cleanUpDeletedFiles;
    @FXML private CheckBox cleanUpUpgradeExternalLinks;

    public CleanupFileRelatedPanel(BibDatabaseContext databaseContext, CleanupPreferences cleanupPreferences, FilePreferences filePreferences) {
        Objects.requireNonNull(cleanupPreferences, "cleanupPreferences must not be null");

        ViewLoader.view(this)
                  .root(this)
                  .load();

        init(databaseContext, cleanupPreferences, filePreferences);
    }

    private void init(BibDatabaseContext databaseContext, CleanupPreferences cleanupPreferences, FilePreferences filePreferences) {
        Optional<Path> firstExistingDir = databaseContext.getFirstExistingFileDir(filePreferences);
        if (firstExistingDir.isPresent()) {
            cleanUpMovePdf.setText(Localization.lang("Move linked files to default file directory %0", firstExistingDir.get().toString()));
        } else {
            cleanUpMovePdf.setText(Localization.lang("Move linked files to default file directory %0", "..."));

            // Since the directory does not exist, we cannot move it to there. So, this option is not checked - regardless of the presets stored in the preferences.
            cleanUpMovePdf.setDisable(true);
            cleanUpMovePdf.setSelected(false);
        }

        cleanUpRenamePdfonlyRelativePaths.disableProperty().bind(cleanUpRenamePdf.selectedProperty().not());

        cleanUpUpgradeExternalLinks.setText(Localization.lang("Upgrade external PDF/PS links to use the '%0' field.", StandardField.FILE.getDisplayName()));

        String currentPattern = Localization.lang("Filename format pattern (from preferences)")
                                            .concat(filePreferences.getFileNamePattern());
        cleanupRenamePdfLabel.setText(currentPattern);

        updateDisplay(cleanupPreferences);
    }

    private void updateDisplay(CleanupPreferences preset) {
        if (!cleanUpMovePdf.isDisabled()) {
            cleanUpMovePdf.setSelected(preset.isActive(CleanupPreferences.CleanupStep.MOVE_PDF));
        }
        cleanUpMakePathsRelative.setSelected(preset.isActive(CleanupPreferences.CleanupStep.MAKE_PATHS_RELATIVE));
        cleanUpRenamePdf.setSelected(preset.isActive(CleanupPreferences.CleanupStep.RENAME_PDF));
        cleanUpRenamePdfonlyRelativePaths.setSelected(preset.isActive(CleanupPreferences.CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS));
        cleanUpUpgradeExternalLinks.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS));
        cleanUpDeletedFiles.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CLEAN_UP_DELETED_LINKED_FILES));
    }

    public Optional<CleanupPreferences> getCleanupPreferences() {
        EnumSet<CleanupPreferences.CleanupStep> activeJobs = EnumSet.noneOf(CleanupPreferences.CleanupStep.class);

        if (cleanUpMakePathsRelative.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.MAKE_PATHS_RELATIVE);
        }
        if (cleanUpRenamePdf.isSelected()) {
            if (cleanUpRenamePdfonlyRelativePaths.isSelected()) {
                activeJobs.add(CleanupPreferences.CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS);
            } else {
                activeJobs.add(CleanupPreferences.CleanupStep.RENAME_PDF);
            }
        }
        if (cleanUpUpgradeExternalLinks.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        }
        if (cleanUpDeletedFiles.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEAN_UP_DELETED_LINKED_FILES);
        }

        return Optional.of(new CleanupPreferences(activeJobs));
    }
}
