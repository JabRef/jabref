package org.jabref.gui.documentviewer;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import javafx.animation.FadeTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualFlowHit;

public class DocumentViewerControl extends StackPane {

    private TaskExecutor taskExecutor;

    private ObjectProperty<Integer> currentPage = new SimpleObjectProperty<>(1);
    private VirtualFlow<DocumentPageViewModel, DocumentViewerPage> flow;

    public DocumentViewerControl(TaskExecutor taskExecutor) {
        this.taskExecutor = Objects.requireNonNull(taskExecutor);

        this.getStyleClass().setAll("document-viewer");

        // External changes to currentPage should result in scrolling to this page
        EasyBind.subscribe(currentPage, this::showPage);
    }

    public int getCurrentPage() {
        return currentPage.get();
    }

    public ObjectProperty<Integer> currentPageProperty() {
        return currentPage;
    }

    private void showPage(int pageNumber) {
        if (flow != null) {
            flow.show(pageNumber - 1);
        }
    }

    public void show(DocumentViewModel document) {
        flow = VirtualFlow.createVertical(document.getPages(), DocumentViewerPage::new);
        getChildren().setAll(flow);
        flow.visibleCells().addListener((ListChangeListener<? super DocumentViewerPage>) c -> updateCurrentPage(flow.visibleCells()));
    }

    private void updateCurrentPage(ObservableList<DocumentViewerPage> visiblePages) {
        if (flow == null) {
            return;
        }

        // We try to find the page that is displayed in the center of the viewport
        Optional<DocumentViewerPage> inMiddleOfViewport = Optional.empty();
        try {
            VirtualFlowHit<DocumentViewerPage> hit = flow.hit(0, 800 / 2);
            if (hit.isCellHit()) {
                // Successful hit
                inMiddleOfViewport = Optional.of(hit.getCell());
            }
        } catch (NoSuchElementException exception) {
            // Sometimes throws exception if no page is found -> ignore
        }

        if (inMiddleOfViewport.isPresent()) {
            // Successful hit
            currentPage.set(inMiddleOfViewport.get().getPageNumber());
        } else {
            // Heuristic missed, so try to get page number from first shown page
            currentPage.set(
                    visiblePages.stream().findFirst().map(DocumentViewerPage::getPageNumber).orElse(1));
        }
    }

    /**
     * Represents the viewport for a page. Note: the instances of {@link DocumentViewerPage} are reused, i.e., not every
     * page is rendered in a new instance but instead {@link DocumentViewerPage#updateItem(Object)} is called.
     */
    private class DocumentViewerPage implements Cell<DocumentPageViewModel, StackPane> {
        private final ImageView imageView;
        private final StackPane imageHolder;
        private final Rectangle background;
        private DocumentPageViewModel page;

        public DocumentViewerPage(DocumentPageViewModel initialPage) {
            page = initialPage;

            imageView = new ImageView();
            imageHolder = new StackPane();
            imageHolder.getStyleClass().setAll("page");

            // Show progress indicator
            ProgressIndicator progress = new ProgressIndicator();
            progress.setMaxSize(50, 50);

            // Set empty background and create proper rendering in background (for smoother loading)
            background = new Rectangle(600, 800);
            background.setStyle("-fx-fill: WHITE");
            imageView.setImage(new WritableImage(600, 800));
            BackgroundTask<Image> generateImage = BackgroundTask
                    .run(initialPage::render)
                    .onSuccess(image -> {
                        imageView.setImage(image);
                        progress.setVisible(false);
                        background.setVisible(false);
                    });
            taskExecutor.execute(generateImage);

            imageHolder.getChildren().setAll(background, progress, imageView);
        }

        @Override
        public StackPane getNode() {
            return imageHolder;
        }

        @Override
        public boolean isReusable() {
            return true;
        }

        @Override
        public void updateItem(DocumentPageViewModel page) {
            this.page = page;

            // First hide old page and show background instead
            background.setVisible(true);
            imageView.setOpacity(0);

            BackgroundTask<Image> generateImage = BackgroundTask
                    .run(page::render)
                    .onSuccess(image -> {
                        imageView.setImage(image);

                        // Fade new page in for smoother transition
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(100), imageView);
                        fadeIn.setFromValue(0);
                        fadeIn.setToValue(1);
                        fadeIn.play();
                    });
            taskExecutor.execute(generateImage);
        }

        public int getPageNumber() {
            return page.getPageNumber();
        }
    }
}
