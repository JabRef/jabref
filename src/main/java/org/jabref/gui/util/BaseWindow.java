package org.jabref.gui.util;

import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.gui.icon.IconTheme;

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
    private final ObservableList<String> stylesheets = FXCollections.observableArrayList();

    public BaseWindow() {
        this.initModality(Modality.NONE);
        this.getIcons().add(IconTheme.getJabRefImage());

        setScene(new Scene(new Pane()));

        stylesheets.addListener((ListChangeListener<String>) c -> getScene().getStylesheets().setAll(stylesheets));
        sceneProperty().addListener((obs, oldValue, newValue) -> newValue.getStylesheets().setAll(stylesheets));
    }

    public void applyStylesheets(ObservableList<String> stylesheets) {
        this.stylesheets.setAll(stylesheets);
    }
}
