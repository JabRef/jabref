package org.jabref.gui;

import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.frame.FileHistoryMenu;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

public class WelcomeTab extends Tab {

    private final VBox recentLibrariesBox;
    private final LibraryTabContainer tabContainer;
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

    public WelcomeTab(LibraryTabContainer tabContainer,
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

        super(Localization.lang("Welcome"));
        setClosable(true);

        this.tabContainer = tabContainer;
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

        VBox welcomePageContainer = new VBox(20);
        welcomePageContainer.setAlignment(Pos.CENTER);

        HBox welcomeMainContainer = new HBox(20);
        welcomeMainContainer.setAlignment(Pos.CENTER);

        VBox welcomeBox = createWelcomeBox();
        VBox startBox = createWelcomeStartBox();
        VBox recentBox = createWelcomeRecentBox();

        welcomePageContainer.getChildren().addAll(welcomeBox, startBox, recentBox);
        welcomeMainContainer.getChildren().add(welcomePageContainer);

        setContent(welcomeMainContainer);
    }

    private VBox createWelcomeBox() {
        Label welcomeLabel = new Label(Localization.lang("Welcome to JabRef"));
        welcomeLabel.getStyleClass().add("welcome-label");

        Label descriptionLabel = new Label(Localization.lang("Stay on top of your literature"));
        descriptionLabel.getStyleClass().add("welcome-description-label");

        return createVBoxContainer(welcomeLabel, descriptionLabel);
    }

    private VBox createWelcomeStartBox() {
        Label startLabel = new Label(Localization.lang("Start"));
        startLabel.getStyleClass().add("welcome-header-label");

        Hyperlink newLibraryLink = new Hyperlink(Localization.lang("New library"));
        newLibraryLink.getStyleClass().add("welcome-hyperlink");
        newLibraryLink.setOnAction(e -> new NewDatabaseAction(tabContainer, preferences).execute());

        Hyperlink openLibraryLink = new Hyperlink(Localization.lang("Open library"));
        openLibraryLink.getStyleClass().add("welcome-hyperlink");
        openLibraryLink.setOnAction(e -> new OpenDatabaseAction(tabContainer, preferences, aiService, dialogService,
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
