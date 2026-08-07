package org.jabref.gui.externalfiles;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.externalfiles.DateRange;
import org.jabref.logic.externalfiles.ExternalFileSorter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;

import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.WizardPane;

public class SearchConfigurationPage extends WizardPane {

    private final UnlinkedFilesDialogViewModel viewModel;
    private final ControlsFxVisualizer validationVisualizer;

    private final BooleanProperty invalidProperty = new SimpleBooleanProperty(false);

    private TextField directoryPathField;
    private Button browseButton;
    private ComboBox<FileExtensionViewModel> fileTypeCombo;
    private ComboBox<DateRange> fileDateCombo;
    private ComboBox<ExternalFileSorter> fileSortCombo;

    public SearchConfigurationPage(UnlinkedFilesDialogViewModel viewModel,
                                   BibDatabaseContext bibDatabaseContext,
                                   GuiPreferences preferences) {
        this.viewModel = viewModel;
        this.validationVisualizer = new ControlsFxVisualizer();

        setHeaderText(Localization.lang("Configure search"));
        setupUI(bibDatabaseContext, preferences);
        setupValidation();
    }

    public BooleanProperty invalidProperty() {
        return invalidProperty;
    }

    private void setupUI(BibDatabaseContext bibDatabaseContext, GuiPreferences preferences) {
        VBox wrapper = new VBox();
        wrapper.setFillWidth(true);
        wrapper.setPadding(new Insets(10));
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setMaxHeight(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(grid, Priority.ALWAYS);

        /// Column Constraints for proper alignment and resizing
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setHgrow(Priority.NEVER);
        labelColumn.setMinWidth(100);
        labelColumn.setPrefWidth(100);

        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        fieldColumn.setFillWidth(true);
        fieldColumn.setMinWidth(300);

        grid.getColumnConstraints().addAll(labelColumn, fieldColumn);

        int row = 0;

        ///Directory selection
        Label dirLabel = new Label(Localization.lang("Directory"));
        grid.add(dirLabel, 0, row);

        HBox directoryBox = new HBox(5);
        directoryBox.setAlignment(Pos.CENTER_LEFT);
        directoryBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(directoryBox, Priority.ALWAYS);

        directoryPathField = new TextField();
        directoryPathField.textProperty().bindBidirectional(viewModel.directoryPathProperty());
        directoryPathField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(directoryPathField, Priority.ALWAYS);

        browseButton = new Button();
        browseButton.setGraphic(IconTheme.JabRefIcons.OPEN.getGraphicNode());
        browseButton.setOnAction(e -> viewModel.browseFileDirectory());
        browseButton.setMinWidth(30);
        browseButton.setPrefWidth(30);

        directoryBox.getChildren().addAll(directoryPathField, browseButton);
        GridPane.setHgrow(directoryBox, Priority.ALWAYS);
        grid.add(directoryBox, 1, row);

        row++;

        /// File type filter
        Label fileTypeLabel = new Label(Localization.lang("File type"));
        grid.add(fileTypeLabel, 0, row);

        fileTypeCombo = new ComboBox<>();
        fileTypeCombo.setItems(viewModel.getFileFilters());
        fileTypeCombo.valueProperty().bindBidirectional(viewModel.selectedExtensionProperty());

        new ViewModelListCellFactory<FileExtensionViewModel>()
                .withText(FileExtensionViewModel::getDescription)
                .withIcon(this::getIconForExtension)
                .install(fileTypeCombo);

        fileTypeCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(fileTypeCombo, Priority.ALWAYS);
        grid.add(fileTypeCombo, 1, row);

        row++;

        /// Date filter
        Label dateLabel = new Label(Localization.lang("Last edited"));
        grid.add(dateLabel, 0, row);

        fileDateCombo = new ComboBox<>();
        fileDateCombo.setItems(viewModel.getDateFilters());
        fileDateCombo.valueProperty().bindBidirectional(viewModel.selectedDateProperty());

        new ViewModelListCellFactory<DateRange>()
                .withText(DateRange::getDateRange)
                .install(fileDateCombo);

        fileDateCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(fileDateCombo, Priority.ALWAYS);
        grid.add(fileDateCombo, 1, row);

        row++;

        /// Sort order
        Label sortLabel = new Label(Localization.lang("Sort by"));
        grid.add(sortLabel, 0, row);

        fileSortCombo = new ComboBox<>();
        fileSortCombo.setItems(viewModel.getSorters());
        fileSortCombo.valueProperty().bindBidirectional(viewModel.selectedSortProperty());

        new ViewModelListCellFactory<ExternalFileSorter>()
                .withText(ExternalFileSorter::getSorter)
                .install(fileSortCombo);

        fileSortCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(fileSortCombo, Priority.ALWAYS);
        grid.add(fileSortCombo, 1, row);

        wrapper.getChildren().add(grid);
        setContent(wrapper);
        loadConfiguration(bibDatabaseContext, preferences);
    }

    ///  Returns the appropriate icon for a file extension.
    ///  Returns null for "Any file" to display no icon.
    private JabRefIcon getIconForExtension(FileExtensionViewModel fileExtension) {
        if (fileExtension == null) {
            return null;
        }

        String name = fileExtension.getName().toLowerCase();

        return switch (name) {
            case "pdf" ->
                    IconTheme.JabRefIcons.PDF_FILE;
            case "html" ->
                    IconTheme.JabRefIcons.WWW;
            case "markdown",
                 "md" ->
                    IconTheme.JabRefIcons.FILE_TEXT;
            case "any file" ->
                    null;
            default ->
                    null;
        };
    }

    private void setupValidation() {
        validationVisualizer.setDecoration(new IconValidationDecorator());

        Platform.runLater(() -> {
            validationVisualizer.initVisualization(
                    viewModel.directoryPathValidationStatus(),
                    directoryPathField);
        });

        invalidProperty().bind(viewModel.directoryPathValidationStatus().validProperty().not());
    }

    private void loadConfiguration(BibDatabaseContext bibDatabaseContext, GuiPreferences preferences) {
        directoryPathField.setText(
                bibDatabaseContext.getFirstExistingFileDir(preferences.getFilePreferences())
                                  .map(Path::toString)
                                  .orElse("")
        );

        UnlinkedFilesDialogPreferences savedPrefs = preferences.getUnlinkedFilesDialogPreferences();

        FileExtensionViewModel selectedExtension = fileTypeCombo.getItems()
                                                                .stream()
                                                                .filter(item -> Objects.equals(item.getName(), savedPrefs.getUnlinkedFilesSelectedExtension()))
                                                                .findFirst()
                                                                .orElseGet(() -> new FileExtensionViewModel(
                                                                        StandardFileType.ANY_FILE,
                                                                        preferences.getExternalApplicationsPreferences()));
        fileTypeCombo.getSelectionModel().select(selectedExtension);
        fileDateCombo.getSelectionModel().select(savedPrefs.getUnlinkedFilesSelectedDateRange());
        fileSortCombo.getSelectionModel().select(savedPrefs.getUnlinkedFilesSelectedSort());
    }

    @Override
    public void onEnteringPage(Wizard wizard) {
        viewModel.treeRootProperty().setValue(Optional.empty());
        Platform.runLater(() -> {
            Node nextButton = this.lookupButton(ButtonType.NEXT);
            if (nextButton != null) {
                nextButton.disableProperty().bind(invalidProperty());
            }
            /// Force layout refresh
            if (getScene() != null && getScene().getWindow() != null) {
                getScene().getWindow().sizeToScene();
            }
        });
    }
}
