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

public class WelcomePage {

    private final VBox welcomePageContainer;
    private final HBox welcomeMainContainer;
    private final VBox recentLibrariesBox;
    private final JabRefFrame frame;
    private final GuiPreferences preferences;
    private final AiService aiService;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private final CountingUndoManager undoManager;
    private final ClipBoardManager clipBoardManager;
    private final TaskExecutor taskExecutor;
    private final FileHistoryMenu fileHistoryMenu;

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

        this.frame = frame;
        this.preferences = preferences;
        this.aiService = aiService;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
        this.undoManager = undoManager;
        this.clipBoardManager = clipBoardManager;
        this.taskExecutor = taskExecutor;
        this.fileHistoryMenu = fileHistoryMenu;

        this.recentLibrariesBox = new VBox(5);

        this.welcomePageContainer = new VBox(20);
        welcomePageContainer.setAlignment(Pos.CENTER);

        this.welcomeMainContainer = new HBox(20);
        welcomeMainContainer.setAlignment(Pos.CENTER);

        VBox welcomeBox = createWelcomeBox();
        VBox startBox = createWelcomeStartBox();
        VBox recentBox = createWelcomeRecentBox();

        welcomePageContainer.getChildren().addAll(welcomeBox, startBox, recentBox);

        welcomeMainContainer.getChildren().add(welcomePageContainer);
    }

    public HBox getWelcomeMainContainer() {
        return welcomeMainContainer;
    }

    private VBox createWelcomeBox() {
        Label welcomeLabel = new Label(Localization.lang("Welcome to JabRef"));
        welcomeLabel.getStyleClass().add("welcome-label");

        Label descriptionLabel = new Label(Localization.lang("Stay on top of your Literature"));
        descriptionLabel.getStyleClass().add("welcome-description-label");

        return createVBoxContainer(welcomeLabel, descriptionLabel);
    }

    private VBox createWelcomeStartBox() {
        Label startLabel = new Label(Localization.lang("Start"));
        startLabel.getStyleClass().add("welcome-header-label");

        Hyperlink newLibraryLink = new Hyperlink(Localization.lang("New Library"));
        newLibraryLink.getStyleClass().add("welcome-hyperlink");
        newLibraryLink.setOnAction(e -> new NewDatabaseAction(frame, preferences).execute());

        Hyperlink openLibraryLink = new Hyperlink(Localization.lang("Open Library"));
        openLibraryLink.getStyleClass().add("welcome-hyperlink");
        openLibraryLink.setOnAction(e -> new OpenDatabaseAction(frame, preferences, aiService, dialogService,
                stateManager, fileUpdateMonitor, entryTypesManager, undoManager, clipBoardManager,
                taskExecutor).execute());

        return createVBoxContainer(startLabel, newLibraryLink, openLibraryLink);
    }

    private VBox createWelcomeRecentBox() {
        Label recentLabel = new Label(Localization.lang("Recent"));
        recentLabel.getStyleClass().add("welcome-header-label");

        recentLibrariesBox.setAlignment(Pos.TOP_LEFT);
        updateWelcomeRecentLibraries();

        fileHistoryMenu.getItems().addListener((ListChangeListener<MenuItem>) change -> updateWelcomeRecentLibraries());

        return createVBoxContainer(recentLabel, recentLibrariesBox);
    }

    private void updateWelcomeRecentLibraries() {
        recentLibrariesBox.getChildren().clear();

        if (fileHistoryMenu.getItems().isEmpty()) {
            Label noRecentLibrariesLabel = new Label(Localization.lang("No Recent Libraries"));
            noRecentLibrariesLabel.getStyleClass().add("welcome-no-recent-label");
            recentLibrariesBox.getChildren().add(noRecentLibrariesLabel);
        } else {
            for (MenuItem item : fileHistoryMenu.getItems()) {
                Hyperlink recentLibraryLink = new Hyperlink(item.getText());
                recentLibraryLink.getStyleClass().add("welcome-hyperlink");
                recentLibraryLink.setOnAction(item.getOnAction());
                recentLibrariesBox.getChildren().add(recentLibraryLink);
            }
        }
    }

    private VBox createVBoxContainer(Node... nodes) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.TOP_LEFT);
        box.getChildren().addAll(nodes);
        return box;
    }
}
