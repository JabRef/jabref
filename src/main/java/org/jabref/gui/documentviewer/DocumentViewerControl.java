package org.jabref.gui.documentviewer;

import java.util.Objects;

import javafx.animation.FadeTransition;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;

import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;

public class DocumentViewerControl extends StackPane {

    private TaskExecutor taskExecutor;

    public DocumentViewerControl(TaskExecutor taskExecutor) {
        this.taskExecutor = Objects.requireNonNull(taskExecutor);

        this.getStyleClass().setAll("document-viewer");
    }

    public void show(DocumentViewModel document) {
        VirtualFlow<DocumentPageViewModel, Cell<DocumentPageViewModel, StackPane>> flow
                = VirtualFlow.createVertical(document.getPages(), DocumentViewerPage::new);
        getChildren().setAll(flow);
    }

    /**
     * Represents the viewport for a page. Note: the instances of {@link DocumentViewerPage} are reused, i.e., not every
     * page is rendered in a new instance but instead {@link DocumentViewerPage#updateItem(Object)} is called.
     */
    private class DocumentViewerPage implements Cell<DocumentPageViewModel, StackPane> {
        private final ImageView imageView;
        private final StackPane imageHolder;
        private final Rectangle background;

        public DocumentViewerPage(DocumentPageViewModel initialPage) {
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
    }
}
