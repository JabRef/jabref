package org.jabref.gui;

import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.edit.OpenBrowserAction;
import org.jabref.gui.frame.FileHistoryMenu;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.URLs;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BuildInfo;
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
    private final BuildInfo buildInfo;

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
                      FileHistoryMenu fileHistoryMenu,
                      BuildInfo buildInfo) {

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
        this.buildInfo = buildInfo;

        this.recentLibrariesBox = new VBox(10);

        VBox welcomeBox = createWelcomeBox();
        VBox startBox = createWelcomeStartBox();
        VBox recentBox = createWelcomeRecentBox();

        VBox welcomePageContainer = new VBox(10);
        welcomePageContainer.setAlignment(Pos.CENTER);
        welcomePageContainer.getChildren().addAll(welcomeBox, startBox, recentBox);

        HBox welcomeMainContainer = new HBox(10);
        welcomeMainContainer.setAlignment(Pos.CENTER);
        welcomeMainContainer.setPadding(new Insets(10, 10, 10, 50));

        welcomeMainContainer.getChildren().add(welcomePageContainer);

        BorderPane rootLayout = new BorderPane();
        rootLayout.setCenter(welcomeMainContainer);
        rootLayout.setBottom(createFooter());

        VBox container = new VBox();
        container.getChildren().add(rootLayout);
        VBox.setVgrow(rootLayout, Priority.ALWAYS);
        setContent(container);
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

        fileHistoryMenu.getItems().addListener((ListChangeListener<MenuItem>) _ -> updateWelcomeRecentLibraries());

        return createVBoxContainer(recentLabel, recentLibrariesBox);
    }

    private void updateWelcomeRecentLibraries() {
        if (fileHistoryMenu.getItems().isEmpty()) {
            displayNoRecentLibrariesMessage();
            return;
        }

        recentLibrariesBox.getChildren().clear();
        fileHistoryMenu.disableProperty().unbind();
        fileHistoryMenu.setDisable(false);

        for (MenuItem item : fileHistoryMenu.getItems()) {
            Hyperlink recentLibraryLink = new Hyperlink(item.getText());
            recentLibraryLink.getStyleClass().add("welcome-hyperlink");
            recentLibraryLink.setOnAction(item.getOnAction());
            recentLibrariesBox.getChildren().add(recentLibraryLink);
        }
    }

    private void displayNoRecentLibrariesMessage() {
        recentLibrariesBox.getChildren().clear();
        Label noRecentLibrariesLabel = new Label(Localization.lang("No recent libraries"));
        noRecentLibrariesLabel.getStyleClass().add("welcome-no-recent-label");
        recentLibrariesBox.getChildren().add(noRecentLibrariesLabel);

        fileHistoryMenu.disableProperty().unbind();
        fileHistoryMenu.setDisable(true);
    }

    private VBox createVBoxContainer(Node... nodes) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.TOP_LEFT);
        box.getChildren().addAll(nodes);
        return box;
    }

    private VBox createFooter() {
        // Heading for the footer area
        Label communityLabel = createFooterLabel(Localization.lang("Community"));

        HBox iconLinksContainer = createIconLinksContainer();
        HBox textLinksContainer = createTextLinksContainer();
        HBox versionContainer = createVersionContainer();

        VBox footerBox = new VBox(10);
        footerBox.setAlignment(Pos.CENTER);
        footerBox.getChildren().addAll(communityLabel, iconLinksContainer, textLinksContainer, versionContainer);
        footerBox.setPadding(new Insets(10, 0, 10, 0));
        footerBox.getStyleClass().add("welcome-footer-container");

        return footerBox;
    }

    private Label createFooterLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("welcome-footer-label");
        return label;
    }

    private HBox createIconLinksContainer() {
        HBox container = new HBox(10);
        container.setAlignment(Pos.CENTER);

        Hyperlink onlineHelpLink = createFooterLink(Localization.lang("Online help"), StandardActions.HELP, IconTheme.JabRefIcons.HELP);
        Hyperlink forumLink = createFooterLink(Localization.lang("Community forum"), StandardActions.OPEN_FORUM, IconTheme.JabRefIcons.FORUM);
        Hyperlink mastodonLink = createFooterLink(Localization.lang("Mastodon"), StandardActions.OPEN_MASTODON, IconTheme.JabRefIcons.MASTODON);
        Hyperlink linkedInLink = createFooterLink(Localization.lang("LinkedIn"), StandardActions.OPEN_LINKEDIN, IconTheme.JabRefIcons.LINKEDIN);
        Hyperlink donationLink = createFooterLink(Localization.lang("Donation"), StandardActions.DONATE, IconTheme.JabRefIcons.DONATE);

        container.getChildren().addAll(onlineHelpLink, forumLink, mastodonLink, linkedInLink, donationLink);
        return container;
    }

    private HBox createTextLinksContainer() {
        HBox container = new HBox(10);
        container.setAlignment(Pos.CENTER);

        Hyperlink devVersionLink = createFooterLink(Localization.lang("Download development version"), StandardActions.OPEN_DEV_VERSION_LINK, null);
        Hyperlink changelogLink = createFooterLink(Localization.lang("CHANGELOG"), StandardActions.OPEN_CHANGELOG, null);

        container.getChildren().addAll(devVersionLink, changelogLink);
        return container;
    }

    private Hyperlink createFooterLink(String text, StandardActions action, IconTheme.JabRefIcons icon) {
        Hyperlink link = new Hyperlink(text);
        link.getStyleClass().add("welcome-footer-link");

        String url = switch (action) {
            case HELP -> URLs.HELP_URL;
            case OPEN_FORUM -> URLs.FORUM_URL;
            case OPEN_MASTODON -> URLs.MASTODON_URL;
            case OPEN_LINKEDIN -> URLs.LINKEDIN_URL;
            case DONATE -> URLs.DONATE_URL;
            case OPEN_DEV_VERSION_LINK -> URLs.DEV_VERSION_LINK_URL;
            case OPEN_CHANGELOG -> URLs.CHANGELOG_URL;
            default -> null;
        };

        if (url != null) {
            link.setOnAction(e -> new OpenBrowserAction(url, dialogService, preferences.getExternalApplicationsPreferences()).execute());
        }

        if (icon != null) {
            link.setGraphic(icon.getGraphicNode());
        }

        return link;
    }

    private HBox createVersionContainer() {
        HBox container = new HBox(10);
        container.setAlignment(Pos.CENTER);

        Label versionLabel = new Label(Localization.lang("Current JabRef version: %0", buildInfo.version));
        versionLabel.getStyleClass().add("welcome-footer-version");

        container.getChildren().add(versionLabel);
        return container;
    }
}
