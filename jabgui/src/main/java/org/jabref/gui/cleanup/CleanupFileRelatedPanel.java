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
import org.jabref.logic.cleanup.CleanupTabSelection;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.views.ViewLoader;

public class CleanupFileRelatedPanel extends VBox {

    @FXML private Label cleanupRenamePdfLabel;

    @FXML private CheckBox cleanupMovePdf;
    @FXML private CheckBox cleanupMakePathsRelative;
    @FXML private CheckBox cleanupRenamePdf;
    @FXML private CheckBox cleanupRenamePdfonlyRelativePaths;
    @FXML private CheckBox cleanupDeletedFiles;
    @FXML private CheckBox cleanupUpgradeExternalLinks;

    private final CleanupDialogViewModel viewModel;

    public CleanupFileRelatedPanel(BibDatabaseContext databaseContext,
                                   CleanupPreferences cleanupPreferences,
                                   FilePreferences filePreferences,
                                   CleanupDialogViewModel viewModel) {
        Objects.requireNonNull(databaseContext, "databaseContext must not be null");
        Objects.requireNonNull(cleanupPreferences, "cleanupPreferences must not be null");
        Objects.requireNonNull(filePreferences, "filePreferences must not be null");
        Objects.requireNonNull(viewModel, "viewModel must not be null");

        this.viewModel = viewModel;

        ViewLoader.view(this)
                  .root(this)
                  .load();

        init(databaseContext, cleanupPreferences, filePreferences);
    }

    private void init(BibDatabaseContext databaseContext, CleanupPreferences cleanupPreferences, FilePreferences filePreferences) {
        Optional<Path> firstExistingDir = databaseContext.getFirstExistingFileDir(filePreferences);
        if (firstExistingDir.isPresent()) {
            cleanupMovePdf.setText(Localization.lang("Move linked files to default file directory %0", firstExistingDir.get().toString()));
        } else {
            cleanupMovePdf.setText(Localization.lang("Move linked files to default file directory %0", "..."));

            cleanupMovePdf.setDisable(true);
            cleanupMovePdf.setSelected(false);
        }

        cleanupRenamePdfonlyRelativePaths.disableProperty().bind(cleanupRenamePdf.selectedProperty().not());

        cleanupUpgradeExternalLinks.setText(Localization.lang("Upgrade external PDF/PS links to use the '%0' field.", StandardField.FILE.getName()));

        String currentPattern = Localization.lang("Filename format pattern (from preferences)")
                                            .concat(filePreferences.getFileNamePattern());
        cleanupRenamePdfLabel.setText(currentPattern);

        updateDisplay(cleanupPreferences);
    }

    private void updateDisplay(CleanupPreferences preset) {
        if (!cleanupMovePdf.isDisabled()) {
            cleanupMovePdf.setSelected(preset.isActive(CleanupPreferences.CleanupStep.MOVE_PDF));
        }
        cleanupMakePathsRelative.setSelected(preset.isActive(CleanupPreferences.CleanupStep.MAKE_PATHS_RELATIVE));
        cleanupRenamePdf.setSelected(preset.isActive(CleanupPreferences.CleanupStep.RENAME_PDF));
        cleanupRenamePdfonlyRelativePaths.setSelected(preset.isActive(CleanupPreferences.CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS));
        cleanupUpgradeExternalLinks.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS));
        cleanupDeletedFiles.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CLEAN_UP_DELETED_LINKED_FILES));
    }

    public EnumSet<CleanupPreferences.CleanupStep> getSelectedJobs() {
        EnumSet<CleanupPreferences.CleanupStep> activeJobs = EnumSet.noneOf(CleanupPreferences.CleanupStep.class);

        if (cleanupMakePathsRelative.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.MAKE_PATHS_RELATIVE);
        }
        if (cleanupRenamePdf.isSelected()) {
            if (cleanupRenamePdfonlyRelativePaths.isSelected()) {
                activeJobs.add(CleanupPreferences.CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS);
            } else {
                activeJobs.add(CleanupPreferences.CleanupStep.RENAME_PDF);
            }
        }
        if (cleanupUpgradeExternalLinks.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        }
        if (cleanupDeletedFiles.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEAN_UP_DELETED_LINKED_FILES);
        }

        return activeJobs;
    }

    @FXML
    private void onApply() {
        CleanupTabSelection selectedTab = CleanupTabSelection.ofJobs(CleanupDialogViewModel.FILE_RELATED_JOBS, getSelectedJobs());
        viewModel.apply(selectedTab);
        getScene().getWindow().hide();
    }
}
