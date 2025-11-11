package org.jabref.gui.documentviewer;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.LinkedFile;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class DocumentViewerView extends BaseDialog<Void> {

    @FXML private ComboBox<LinkedFile> fileChoice;
    @FXML private BorderPane mainPane;
    @FXML private ToggleGroup toggleGroupMode;
    @FXML private ToggleButton modeLive;
    @FXML private ToggleButton modeLock;

    @Inject private StateManager stateManager;
    @Inject private CliPreferences preferences;

    private final PdfDocumentViewer viewer = new PdfDocumentViewer();
    private DocumentViewerViewModel viewModel;

    public DocumentViewerView() {
        this.setTitle(Localization.lang("Document viewer"));
        this.initModality(Modality.NONE);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        // Remove button bar at bottom, but add close button to keep the dialog closable by clicking the "x" window symbol
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        getDialogPane().getChildren().removeIf(ButtonBar.class::isInstance);
    }

    @FXML
    private void initialize() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        viewModel = new DocumentViewerViewModel(stateManager, preferences, dialogService);

        setupViewer();
        setupFileChoice();
        setupModeButtons();
    }

    private void setupModeButtons() {
        // make sure that always one toggle is selected
        toggleGroupMode.selectedToggleProperty().addListener((_, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
            }
        });

        modeLive.selectedProperty().addListener((_, _, newValue) -> {
            if (newValue) {
                viewModel.setLiveMode(true);
            }
        });

        modeLock.selectedProperty().addListener((_, _, newValue) -> {
            if (newValue) {
                viewModel.setLiveMode(false);
            }
        });
    }

    private void setupFileChoice() {
        ViewModelListCellFactory<LinkedFile> cellFactory = new ViewModelListCellFactory<LinkedFile>()
                .withText(LinkedFile::getLink);
        fileChoice.setButtonCell(cellFactory.call(null));
        fileChoice.setCellFactory(cellFactory);
        fileChoice.getSelectionModel().selectedItemProperty().addListener(
                (_, _, newValue) -> {
                    if (newValue != null && !fileChoice.getItems().isEmpty()) {
                        viewModel.switchToFile(newValue);
                    }
                });

        fileChoice.itemsProperty().addListener((_, _, newValue) -> {
            if (newValue != null && !newValue.isEmpty()) {
                if (!fileChoice.getItems().isEmpty()) {
                    fileChoice.getSelectionModel().selectFirst();
                }
            }
        });
        fileChoice.itemsProperty().bind(viewModel.filesProperty());
    }

    private void setupViewer() {
        viewModel.currentDocumentProperty().addListener((_, _, newDocument) -> {
            viewer.show(newDocument);
        });
        viewModel.currentPageProperty().bindBidirectional(viewer.currentPageProperty());
        viewModel.highlightTextProperty().bindBidirectional(viewer.highlightTextProperty());
        mainPane.setCenter(viewer);
    }

    public void disableLiveMode() {
        modeLock.setSelected(true);
    }

    public void switchToFile(LinkedFile file) {
        fileChoice.getSelectionModel().select(file);
    }

    public void gotoPage(int pageNumber) {
        viewModel.showPage(pageNumber);
    }

    public void highlightText(String text) {
        viewModel.highlightText(text);
    }
}
