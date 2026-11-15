package org.jabref.gui.ai.chat;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

import com.airhacks.afterburner.views.ViewLoader;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class AiChatFindBar extends HBox {
    @FXML private TextField findField;
    @FXML private Label findResultLabel;

    private final AiChatFindViewModel viewModel = new AiChatFindViewModel();

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
        findResultLabel.textProperty().bind(Bindings.createStringBinding(() -> {
                    if (viewModel.getActiveQuery().isEmpty()) {
                        return "";
                    }
                    int total = viewModel.totalProperty().get();
                    int current = total == 0 ? 0 : viewModel.getCurrent() + 1;
                    return current + "/" + total;
                },
                viewModel.visibleProperty(),
                viewModel.queryProperty(),
                viewModel.currentProperty(),
                viewModel.totalProperty()
        ));
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

    public void show() {
        viewModel.visibleProperty().set(true);
    }

    public void hide() {
        viewModel.visibleProperty().set(false);
    }

    public void focusQuery() {
        findField.requestFocus();
        findField.selectAll();
    }

    public StringProperty queryProperty() {
        return viewModel.queryProperty();
    }

    public String getActiveQuery() {
        return viewModel.getActiveQuery();
    }

    public int getCurrent() {
        return viewModel.getCurrent();
    }

    public boolean setTotal(int total) {
        return viewModel.setTotal(total);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onNavigateProperty() {
        return viewModel.onNavigateProperty();
    }

    public @Nullable EventHandler<ActionEvent> getOnNavigate() {
        return onNavigateProperty().get();
    }

    public void setOnNavigate(@Nullable EventHandler<ActionEvent> onNavigate) {
        onNavigateProperty().set(onNavigate);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onCloseProperty() {
        return viewModel.onCloseProperty();
    }

    public @Nullable EventHandler<ActionEvent> getOnClose() {
        return onCloseProperty().get();
    }

    public void setOnClose(@Nullable EventHandler<ActionEvent> onClose) {
        onCloseProperty().set(onClose);
    }

    @FXML
    private void findNext() {
        viewModel.next();
    }

    @FXML
    private void findPrevious() {
        viewModel.previous();
    }

    @FXML
    private void closeFind() {
        viewModel.close();
    }
}
