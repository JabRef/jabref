package org.jabref.gui.ai.components.apikeymissing;

import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.preferences.ShowPreferencesAction;
import org.jabref.gui.preferences.ai.AiTab;

import com.airhacks.afterburner.views.ViewLoader;

public class ApiKeyMissingComponent extends BorderPane {
    private final LibraryTabContainer libraryTabContainer;
    private final DialogService dialogService;

    public ApiKeyMissingComponent(LibraryTabContainer libraryTabContainer, DialogService dialogService) {
        this.libraryTabContainer = libraryTabContainer;
        this.dialogService = dialogService;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void onHereHyperlinkClick() {
        new ShowPreferencesAction(libraryTabContainer, AiTab.class, dialogService).execute();
    }
}
