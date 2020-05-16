package org.jabref.gui.documentviewer;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import javafx.animation.FadeTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;

import com.tobiasdiez.easybind.EasyBind;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualFlowHit;

public class DocumentViewerControl extends StackPane {

    private final TaskExecutor taskExecutor;

    private final ObjectProperty<Integer> currentPage = new SimpleObjectProperty<>(1);
    private final DoubleProperty scrollY = new SimpleDoubleProperty();
    private final DoubleProperty scrollYMax = new SimpleDoubleProperty();
    private VirtualFlow<DocumentPageViewModel, DocumentViewerPage> flow;
    private PageDimension desiredPageDimension = PageDimension.ofFixedWidth(600);

    public DocumentViewerControl(TaskExecutor taskExecutor) {
        this.taskExecutor = Objects.requireNonNull(taskExecutor);

        this.getStyleClass().add("document-viewer");

        // External changes to currentPage should result in scrolling to this page
        EasyBind.subscribe(currentPage, this::showPage);
    }

    public DoubleProperty scrollYMaxProperty() {
        return scrollYMax;
    }

    public DoubleProperty scrollYProperty() {
        return scrollY;
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

        // (Bidirectional) binding does not work, so use listeners instead
        flow.estimatedScrollYProperty().addListener((observable, oldValue, newValue) -> scrollY.setValue(newValue));
        scrollY.addListener((observable, oldValue, newValue) -> flow.estimatedScrollYProperty().setValue((double) newValue));
        flow.totalLengthEstimateProperty().addListener((observable, oldValue, newValue) -> scrollYMax.setValue(newValue));
    }

    private void updateCurrentPage(ObservableList<DocumentViewerPage> visiblePages) {
        if (flow == null) {
            return;
        }

        // We try to find the page that is displayed in the center of the viewport
        Optional<DocumentViewerPage> inMiddleOfViewport = Optional.empty();
        try {
            VirtualFlowHit<DocumentViewerPage> hit = flow.hit(0, flow.getHeight() / 2);
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

    public void setPageWidth(double width) {
        desiredPageDimension = PageDimension.ofFixedWidth(width);

        updateSizeOfDisplayedPages();
    }

    public void setPageHeight(double height) {
        desiredPageDimension = PageDimension.ofFixedHeight(height);

        updateSizeOfDisplayedPages();
    }

    private void updateSizeOfDisplayedPages() {
        if (flow != null) {
            for (DocumentViewerPage page : flow.visibleCells()) {
                page.updateSize();
            }
            flow.requestLayout();
        }
    }

    public void changePageWidth(int delta) {
        // Assuming the current page is A4 (or has same aspect ratio)
        setPageWidth(desiredPageDimension.getWidth(Math.sqrt(2)) + delta);
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
            imageHolder.getStyleClass().add("page");

            // Show progress indicator
            ProgressIndicator progress = new ProgressIndicator();
            progress.setMaxSize(50, 50);

            // Set empty background and create proper rendering in background (for smoother loading)
            background = new Rectangle(getDesiredWidth(), getDesiredHeight());
            background.setStyle("-fx-fill: WHITE");
            // imageView.setImage(new WritableImage(getDesiredWidth(), getDesiredHeight()));
            BackgroundTask<Image> generateImage = BackgroundTask
                    .wrap(() -> renderPage(initialPage))
                    .onSuccess(image -> {
                        imageView.setImage(image);
                        progress.setVisible(false);
                        background.setVisible(false);
                    });
            taskExecutor.execute(generateImage);

            imageHolder.getChildren().setAll(background, progress, imageView);
        }

        private int getDesiredHeight() {
            return desiredPageDimension.getHeight(page.getAspectRatio());
        }

        private int getDesiredWidth() {
            return desiredPageDimension.getWidth(page.getAspectRatio());
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

            // First hide old page and show background instead (recalculate size of background to make sure its correct)
            background.setWidth(getDesiredWidth());
            background.setHeight(getDesiredHeight());
            background.setVisible(true);
            imageView.setOpacity(0);

            BackgroundTask<Image> generateImage = BackgroundTask
                    .wrap(() -> renderPage(page))
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

        private Image renderPage(DocumentPageViewModel page) {
            return page.render(getDesiredWidth(), getDesiredHeight());
        }

        public int getPageNumber() {
            return page.getPageNumber();
        }

        public void updateSize() {
            background.setWidth(getDesiredWidth());
            background.setHeight(getDesiredWidth());
            updateItem(page);
            imageHolder.requestLayout();
        }
    }
}
