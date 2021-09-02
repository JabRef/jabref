package org.jabref.gui.documentviewer;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;

import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.OnlyIntegerFormatter;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class DocumentViewerView extends BaseDialog<Void> {

    @FXML private ScrollBar scrollBar;
    @FXML private ComboBox<LinkedFile> fileChoice;
    @FXML private BorderPane mainPane;
    @FXML private ToggleButton modeLive;
    @FXML private ToggleButton modeLock;
    @FXML private TextField currentPage;
    @FXML private Label maxPages;

    @Inject private StateManager stateManager;
    @Inject private TaskExecutor taskExecutor;
    @Inject private PreferencesService preferencesService;

    private DocumentViewerControl viewer;
    private DocumentViewerViewModel viewModel;

    public DocumentViewerView() {
        this.setTitle(Localization.lang("Document viewer"));
        this.initModality(Modality.NONE);

        ViewLoader.view(this)
                  .load()
                  .setAsContent(this.getDialogPane());

        // Remove button bar at bottom, but add close button to keep the dialog closable by clicking the "x" window symbol
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        getDialogPane().getChildren().removeIf(node -> node instanceof ButtonBar);
    }

    @FXML
    private void initialize() {
        viewModel = new DocumentViewerViewModel(stateManager, preferencesService);

        setupViewer();
        setupScrollbar();
        setupFileChoice();
        setupPageControls();
        setupModeButtons();
    }

    private void setupModeButtons() {
        viewModel.liveModeProperty().bind(modeLive.selectedProperty());
        modeLock.selectedProperty().bind(modeLive.selectedProperty().not());
    }

    private void setupScrollbar() {
        scrollBar.valueProperty().bindBidirectional(viewer.scrollYProperty());
        scrollBar.maxProperty().bind(viewer.scrollYMaxProperty());
    }

    private void setupPageControls() {
        OnlyIntegerFormatter integerFormatter = new OnlyIntegerFormatter(1);
        viewModel.currentPageProperty().bindBidirectional(integerFormatter.valueProperty());
        currentPage.setTextFormatter(integerFormatter);
        maxPages.textProperty().bind(viewModel.maxPagesProperty().asString());
    }

    private void setupFileChoice() {
        ViewModelListCellFactory<LinkedFile> cellFactory = new ViewModelListCellFactory<LinkedFile>()
                .withText(LinkedFile::getLink);
        fileChoice.setButtonCell(cellFactory.call(null));
        fileChoice.setCellFactory(cellFactory);
        fileChoice.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> viewModel.switchToFile(newValue));
        // We always want that the first item is selected after a change
        // This also automatically selects the first file on the initial load
        fileChoice.itemsProperty().addListener(
                (observable, oldValue, newValue) -> fileChoice.getSelectionModel().selectFirst());
        fileChoice.itemsProperty().bind(viewModel.filesProperty());
    }

    private void setupViewer() {
        viewer = new DocumentViewerControl(taskExecutor);
        viewModel.currentDocumentProperty().addListener((observable, oldDocument, newDocument) -> {
            if (newDocument != null) {
                viewer.show(newDocument);
            }
        });
        viewModel.currentPageProperty().bindBidirectional(viewer.currentPageProperty());
        mainPane.setCenter(viewer);
    }

    public void setLiveMode(boolean liveMode) {
        modeLive.setSelected(liveMode);
    }

    public void gotoPage(int pageNumber) {
        viewModel.showPage(pageNumber);
    }

    public void nextPage(ActionEvent actionEvent) {
        viewModel.showNextPage();
    }

    public void previousPage(ActionEvent actionEvent) {
        viewModel.showPreviousPage();
    }

    public void fitWidth(ActionEvent actionEvent) {
        viewer.setPageWidth(viewer.getWidth());
    }

    public void zoomIn(ActionEvent actionEvent) {
        viewer.changePageWidth(100);
    }

    public void zoomOut(ActionEvent actionEvent) {
        viewer.changePageWidth(-100);
    }

    public void fitSinglePage(ActionEvent actionEvent) {
        viewer.setPageHeight(viewer.getHeight());
    }
}
