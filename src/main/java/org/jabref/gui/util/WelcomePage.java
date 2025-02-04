package org.jabref.gui.util;

import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.frame.FileHistoryMenu;
import org.jabref.gui.frame.JabRefFrame;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

public class WelcomePage extends VBox {

    private final VBox recentLibrariesBox;

    public WelcomePage(JabRefFrame frame,
                       GuiPreferences preferences,
                       AiService aiService,
                       DialogService dialogService,
                       StateManager stateManager,
                       FileUpdateMonitor fileUpdateMonitor,
                       BibEntryTypesManager entryTypesManager,
                       CountingUndoManager undoManager,
                       ClipBoardManager clipBoardManager,
                       TaskExecutor taskExecutor,
                       FileHistoryMenu fileHistoryMenu) {

        setAlignment(Pos.CENTER);
        setSpacing(10);

        this.recentLibrariesBox = new VBox(5);

        VBox welcomeBox = createWelcomeBox();
        VBox startBox = createStartBox(frame, preferences, aiService, dialogService, stateManager, fileUpdateMonitor, entryTypesManager, undoManager, clipBoardManager, taskExecutor);
        VBox recentBox = createRecentBox(fileHistoryMenu);

        VBox container = new VBox(20, welcomeBox, startBox, recentBox);
        container.setAlignment(Pos.CENTER);

        HBox mainContainer = new HBox(20, container);
        mainContainer.setAlignment(Pos.CENTER);

        getChildren().add(mainContainer);
    }

    private VBox createWelcomeBox() {
        Label welcomeLabel = new Label(Localization.lang("Welcome to JabRef"));
        welcomeLabel.getStyleClass().add("welcome-label");

        Label descriptionLabel = new Label(Localization.lang("Stay on top of your Literature"));
        descriptionLabel.getStyleClass().add("description-label");

        return createBox(Pos.TOP_LEFT, welcomeLabel, descriptionLabel);
    }

    private VBox createStartBox(JabRefFrame frame, GuiPreferences preferences, AiService aiService, DialogService dialogService, StateManager stateManager, FileUpdateMonitor fileUpdateMonitor, BibEntryTypesManager entryTypesManager, CountingUndoManager undoManager, ClipBoardManager clipBoardManager, TaskExecutor taskExecutor) {
        Label startLabel = new Label(Localization.lang("Start"));
        startLabel.getStyleClass().add("header-label");

        Hyperlink newLibrary = new Hyperlink(Localization.lang("New Library"));
        newLibrary.getStyleClass().add("welcome-hyperlink");
        newLibrary.setOnAction(e -> new NewDatabaseAction(frame, preferences).execute());

        Hyperlink openLibrary = new Hyperlink(Localization.lang("Open Library"));
        openLibrary.getStyleClass().add("welcome-hyperlink");
        openLibrary.setOnAction(e -> new OpenDatabaseAction(frame, preferences, aiService, dialogService,
                stateManager, fileUpdateMonitor, entryTypesManager, undoManager, clipBoardManager,
                taskExecutor).execute());

        return createBox(Pos.TOP_LEFT, startLabel, newLibrary, openLibrary);
    }

    private VBox createRecentBox(FileHistoryMenu fileHistoryMenu) {
        Label recentLabel = new Label(Localization.lang("Recent"));
        recentLabel.getStyleClass().add("header-label");

        recentLibrariesBox.setAlignment(Pos.TOP_LEFT);
        updateRecentLibraries(fileHistoryMenu);

        fileHistoryMenu.getItems().addListener((ListChangeListener<MenuItem>) change -> updateRecentLibraries(fileHistoryMenu));

        return createBox(Pos.TOP_LEFT, recentLabel, recentLibrariesBox);
    }

    private void updateRecentLibraries(FileHistoryMenu fileHistoryMenu) {
        recentLibrariesBox.getChildren().clear();

        if (fileHistoryMenu.getItems().isEmpty()) {
            Label noRecentLibrariesLabel = new Label(Localization.lang("No Recent Libraries"));
            noRecentLibrariesLabel.getStyleClass().add("no-recent-label");
            recentLibrariesBox.getChildren().add(noRecentLibrariesLabel);
        } else {
            for (MenuItem item : fileHistoryMenu.getItems()) {
                String filePath = item.getText();
                Hyperlink recentLibraryLink = new Hyperlink(filePath);
                recentLibraryLink.getStyleClass().add("welcome-hyperlink");
                recentLibraryLink.setOnAction(item.getOnAction());
                recentLibrariesBox.getChildren().add(recentLibraryLink);
            }
        }
    }

    private VBox createBox(Pos alignment, Node... nodes) {
        VBox box = new VBox(5);
        box.setAlignment(alignment);
        box.getChildren().addAll(nodes);
        return box;
    }
}
