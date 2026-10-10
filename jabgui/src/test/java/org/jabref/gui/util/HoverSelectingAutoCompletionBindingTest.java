package org.jabref.gui.util;

import java.util.List;

import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.jabref.gui.testutils.JavaFxTest;

import impl.org.controlsfx.skin.AutoCompletePopup;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@NullMarked
class HoverSelectingAutoCompletionBindingTest extends JavaFxTest {

    /// Canary for the ControlsFX internals [HoverSelectingAutoCompletionBinding] leans on: the popup's
    /// skin node must be the suggestion [ListView] the binding casts to in its constructor. A ControlsFX
    /// upgrade that changes `AutoCompletePopupSkin`'s shape silently breaks the dialog's
    /// hover-to-highlight feature, so bumping the ControlsFX version requires this test to stay green.
    @Nullable private HoverSelectingAutoCompletionBinding<String> binding;

    @Override
    public void start(Stage stage) {
        TextField textField = new TextField();
        binding = new HoverSelectingAutoCompletionBinding<>(textField,
                request -> List.of("auth", "author", "bibliography"));
        stage.setScene(new Scene(textField));
        stage.show();
    }

    @Test
    void popupSkinNodeIsTheSuggestionListView() {
        // Mirrors the cast in HoverSelectingAutoCompletionBinding's constructor.
        assertInstanceOf(ListView.class, popup().getSkin().getNode(),
                "The popup's skin node must stay the suggestion ListView");
    }

    private AutoCompletePopup<String> popup() {
        assertNotNull(binding);
        return binding.getAutoCompletionPopup();
    }
}
