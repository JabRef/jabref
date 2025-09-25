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
    @FXML private CheckBox cleanupRenamePdfOnlyRelativePaths;
    @FXML private CheckBox cleanupDeletedFiles;
    @FXML private CheckBox cleanupUpgradeExternalLinks;

    private final CleanupFileViewModel viewModel;
    private final CleanupDialogViewModel dialogViewModel;

    public CleanupFileRelatedPanel(BibDatabaseContext databaseContext,
                                   CleanupPreferences cleanupPreferences,
                                   FilePreferences filePreferences,
                                   CleanupDialogViewModel dialogViewModel) {
        Objects.requireNonNull(databaseContext, "databaseContext must not be null");
        Objects.requireNonNull(cleanupPreferences, "cleanupPreferences must not be null");
        Objects.requireNonNull(filePreferences, "filePreferences must not be null");
        Objects.requireNonNull(dialogViewModel, "viewModel must not be null");

        this.dialogViewModel = dialogViewModel;
        this.viewModel = new CleanupFileViewModel(cleanupPreferences);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        init(databaseContext, filePreferences);
        bindProperties();
    }

    private void init(BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        Optional<Path> firstExistingDir = databaseContext.getFirstExistingFileDir(filePreferences);
        if (firstExistingDir.isPresent()) {
            cleanupMovePdf.setText(Localization.lang("Move linked files to default file directory %0", firstExistingDir.get().toString()));
        } else {
            cleanupMovePdf.setText(Localization.lang("Move linked files to default file directory %0", "..."));
            viewModel.movePdfEnabled.set(false);
            viewModel.movePdfSelected.set(false);
        }

        cleanupRenamePdfOnlyRelativePaths.disableProperty().bind(cleanupRenamePdf.selectedProperty().not());

        cleanupUpgradeExternalLinks.setText(Localization.lang("Upgrade external PDF/PS links to use the '%0' field.", StandardField.FILE.getName()));

        String currentPattern = Localization.lang("Filename format pattern (from preferences)")
                                            .concat(filePreferences.getFileNamePattern());
        cleanupRenamePdfLabel.setText(currentPattern);
    }

    private void bindProperties() {
        cleanupMovePdf.selectedProperty().bindBidirectional(viewModel.movePdfSelected);
        cleanupMovePdf.disableProperty().bind(viewModel.movePdfEnabled.not());
        cleanupMakePathsRelative.selectedProperty().bindBidirectional(viewModel.makePathsRelativeSelected);
        cleanupRenamePdf.selectedProperty().bindBidirectional(viewModel.renamePdfSelected);
        cleanupRenamePdfOnlyRelativePaths.selectedProperty().bindBidirectional(viewModel.renamePdfOnlyRelativeSelected);
        cleanupDeletedFiles.selectedProperty().bindBidirectional(viewModel.deleteFilesSelected);
        cleanupUpgradeExternalLinks.selectedProperty().bindBidirectional(viewModel.upgradeLinksSelected);
    }

    @FXML
    private void onApply() {
        EnumSet<CleanupPreferences.CleanupStep> selectedJobs = viewModel.getSelectedJobs();
        CleanupTabSelection selectedTab = CleanupTabSelection.ofJobs(CleanupFileViewModel.FILE_RELATED_JOBS, selectedJobs);
        dialogViewModel.apply(selectedTab);
        getScene().getWindow().hide();
    }
}
