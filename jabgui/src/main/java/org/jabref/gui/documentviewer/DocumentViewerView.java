package org.jabref.gui.documentviewer;

import java.nio.file.Path;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;

import org.jabref.gui.util.BaseDialog;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DocumentViewerView extends BaseDialog<Void> {

    private final PdfDocumentViewer viewer = new PdfDocumentViewer();
    private final DocumentViewerViewModel viewModel;

    public DocumentViewerView() {
        this(new DocumentViewerViewModel());
    }

    public DocumentViewerView(@Nullable Path document) {
        this(new DocumentViewerViewModel(document));
    }

    public DocumentViewerView(DocumentViewerViewModel viewModel) {
        this.viewModel = viewModel;

        this.initModality(Modality.NONE);
        this.titleProperty().bind(viewModel.titleProperty());

        BorderPane mainPane = new BorderPane();
        mainPane.setPrefSize(750.0, 600.0);
        mainPane.getStyleClass().add("padding-0");
        mainPane.setCenter(viewer);

        getDialogPane().setId("document-viewer-dialog");
        getDialogPane().getStyleClass().add("document-viewer-dialog");
        getDialogPane().setContent(mainPane);

        // Remove button bar at bottom, but add close button to keep the dialog closable by clicking the "x" window symbol
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        getDialogPane().getChildren().removeIf(ButtonBar.class::isInstance);

        setupViewer();
    }

    private void setupViewer() {
        viewModel.currentDocumentProperty().addListener((_, _, newDocument) -> viewer.show(newDocument));
        viewModel.currentPageProperty().bindBidirectional(viewer.currentPageProperty());
        viewModel.highlightTextProperty().bindBidirectional(viewer.highlightTextProperty());

        viewModel.getCurrentDocument().ifPresent(viewer::show);
    }

    public void showDocument(@Nullable Path document) {
        viewModel.showDocument(document);
    }

    public void gotoPage(int pageNumber) {
        viewModel.showPage(pageNumber);
    }

    public void highlightText(String text) {
        viewModel.highlightText(text);
    }
}
