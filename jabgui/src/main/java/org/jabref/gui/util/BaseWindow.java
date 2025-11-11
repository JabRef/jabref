package org.jabref.gui.util;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;

import com.airhacks.afterburner.injection.Injector;

/**
 * A base class for non-modal windows of JabRef.
 * <p>
 * You can create a new instance of this class and set the title in the constructor. After that you can call
 * {@link org.jabref.gui.DialogService#showCustomWindow(BaseWindow)} in order to show the window. All the JabRef styles
 * will be applied.
 * <p>
 * See {@link org.jabref.gui.ai.components.aichat.AiChatWindow} for example.
 */
public class BaseWindow extends Stage {
    public BaseWindow() {
        this.initModality(Modality.NONE);
        this.getIcons().add(IconTheme.getJabRefImage());

        setScene(new Scene(new Pane()));

        sceneProperty().addListener((obs, oldValue, newValue) -> {
            newValue.setOnKeyPressed(event -> {
                KeyBindingRepository keyBindingRepository = Injector.instantiateModelOrService(KeyBindingRepository.class);
                if (keyBindingRepository.checkKeyCombinationEquality(KeyBinding.CLOSE, event)) {
                    close();
                    onCloseRequestProperty().get().handle(null);
                }
            });
        });
    }

    public void applyStylesheets(ObservableList<String> stylesheets) {
        this.getScene().getStylesheets().setAll(stylesheets);
    }
}
