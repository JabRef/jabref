package org.jabref.gui.ai.chat;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

import com.airhacks.afterburner.views.ViewLoader;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class AiChatFindBar extends HBox {
    @FXML private TextField findField;
    @FXML private Label findResultLabel;

    private final AiChatFindViewModel viewModel = new AiChatFindViewModel();
    private Runnable onNavigate = () -> {
    };
    private Runnable onClose = () -> {
    };

    public AiChatFindBar() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        managedProperty().bind(visibleProperty());
        visibleProperty().bind(viewModel.visibleProperty());
        findField.textProperty().bindBidirectional(viewModel.queryProperty());
        findResultLabel.textProperty().bind(viewModel.resultTextProperty());
        findField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isControlDown() && !event.isMetaDown() && !event.isAltDown()) {
                if (event.isShiftDown()) {
                    findPrevious();
                } else {
                    findNext();
                }
                event.consume();
            }
        });
    }

    AiChatFindViewModel getViewModel() {
        return viewModel;
    }

    void focusQuery() {
        findField.requestFocus();
        findField.selectAll();
    }

    void setOnNavigate(Runnable onNavigate) {
        this.onNavigate = onNavigate;
    }

    void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    @FXML
    private void findNext() {
        viewModel.next();
        onNavigate.run();
    }

    @FXML
    private void findPrevious() {
        viewModel.previous();
        onNavigate.run();
    }

    @FXML
    private void closeFind() {
        onClose.run();
    }
}
