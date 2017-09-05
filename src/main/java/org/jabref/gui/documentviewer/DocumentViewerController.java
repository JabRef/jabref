package org.jabref.gui.documentviewer;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;

import org.jabref.gui.AbstractController;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.OnlyIntegerFormatter;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.model.entry.LinkedFile;

public class DocumentViewerController extends AbstractController<DocumentViewerViewModel> {

    @FXML private ScrollBar scrollBar;
    @FXML private ComboBox<LinkedFile> fileChoice;
    @FXML private BorderPane mainPane;
    @FXML private ToggleButton modeLive;
    @FXML private TextField currentPage;
    @FXML private Label maxPages;

    @Inject private StateManager stateManager;
    @Inject private TaskExecutor taskExecutor;
    private DocumentViewerControl viewer;

    @FXML
    private void initialize() {
        viewModel = new DocumentViewerViewModel(stateManager);

        setupViewer();
        setupScrollbar();
        setupFileChoice();
        setupPageControls();
        setupModeButtons();
    }

    private void setupModeButtons() {
        viewModel.liveModeProperty().bind(modeLive.selectedProperty());
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
