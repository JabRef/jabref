package org.jabref.gui;

import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.edit.OpenBrowserAction;
import org.jabref.gui.frame.FileHistoryMenu;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.CountingUndoManager;
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

        this.recentLibrariesBox = new VBox(5);

        VBox welcomePageContainer = new VBox(20);
        welcomePageContainer.setAlignment(Pos.CENTER);

        HBox welcomeMainContainer = new HBox(20);
        welcomeMainContainer.setAlignment(Pos.CENTER);

        welcomeMainContainer.setPadding(new Insets(10, 10, 10, 50));

        ScrollPane scrollPane = new ScrollPane(welcomeMainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        scrollPane.widthProperty().addListener((_, _, newWidth) -> {
            double dynamicPadding = Math.max(20, newWidth.doubleValue() * 0.05);
            welcomeMainContainer.setPadding(new Insets(10, 10, 10, dynamicPadding));
        });

        setContent(new StackPane(scrollPane));

        VBox welcomeBox = createWelcomeBox();
        VBox startBox = createWelcomeStartBox();
        VBox recentBox = createWelcomeRecentBox();

        welcomePageContainer.getChildren().addAll(welcomeBox, startBox, recentBox);
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

        fileHistoryMenu.getItems().addListener((ListChangeListener<MenuItem>) change -> updateWelcomeRecentLibraries());

        return createVBoxContainer(recentLabel, recentLibrariesBox);
    }

    private void updateWelcomeRecentLibraries() {
        recentLibrariesBox.getChildren().clear();

        if (fileHistoryMenu.getItems().isEmpty()) {
            Label noRecentLibrariesLabel = new Label(Localization.lang("No recent libraries"));
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

    private VBox createFooter() {
        Label communityLabel = new Label(Localization.lang("Community"));
        communityLabel.getStyleClass().add("welcome-footer-label");

        HBox iconLinksContainer = new HBox(15);
        iconLinksContainer.setAlignment(Pos.CENTER);

        Hyperlink onlineHelpLink = createFooterLink("Online help", StandardActions.HELP, IconTheme.JabRefIcons.HELP);
        Hyperlink forumLink = createFooterLink("Forum for support", StandardActions.OPEN_FORUM, IconTheme.JabRefIcons.FORUM);
        Hyperlink mastodonLink = createFooterLink("Mastodon", StandardActions.OPEN_MASTODON, IconTheme.JabRefIcons.MASTODON);
        Hyperlink linkedInLink = createFooterLink("LinkedIn", StandardActions.OPEN_LINKEDIN, IconTheme.JabRefIcons.LINKEDIN);
        Hyperlink donationLink = createFooterLink("Donation", StandardActions.DONATE, IconTheme.JabRefIcons.DONATE);

        iconLinksContainer.getChildren().addAll(onlineHelpLink, forumLink, mastodonLink, linkedInLink, donationLink);

        HBox textLinksContainer = new HBox(15);
        textLinksContainer.setAlignment(Pos.CENTER);

        Hyperlink devVersionLink = createFooterLink("Download Development version", StandardActions.OPEN_DEV_VERSION_LINK, null);
        Hyperlink changelogLink = createFooterLink("CHANGELOG", StandardActions.OPEN_CHANGELOG, null);

        textLinksContainer.getChildren().addAll(devVersionLink, changelogLink);

        HBox versionContainer = new HBox(15);
        versionContainer.setAlignment(Pos.CENTER);
        Label versionLabel = new Label(Localization.lang("Current JabRef version") + ": " + buildInfo.version);
        versionLabel.getStyleClass().add("welcome-footer-version");
        versionContainer.getChildren().add(versionLabel);

        VBox footerBox = new VBox(10);
        footerBox.setAlignment(Pos.CENTER);
        footerBox.getChildren().addAll(communityLabel, iconLinksContainer, textLinksContainer, versionContainer);
        footerBox.setPadding(new Insets(10, 0, 10, 0));
        footerBox.getStyleClass().add("welcome-footer-container");

        return footerBox;
    }

    private Hyperlink createFooterLink(String text, StandardActions action, IconTheme.JabRefIcons icon) {
        Hyperlink link = new Hyperlink(text);
        link.getStyleClass().add("welcome-footer-link");

        String url = switch (action) {
            case HELP ->
                    "https://help.jabref.org/";
            case OPEN_FORUM ->
                    "https://discourse.jabref.org/";
            case OPEN_MASTODON ->
                    "https://foojay.social/@jabref";
            case OPEN_LINKEDIN ->
                    "https://linkedin.com/company/jabref/";
            case DONATE ->
                    "https://donate.jabref.org";
            case OPEN_DEV_VERSION_LINK ->
                    "https://builds.jabref.org/master/";
            case OPEN_CHANGELOG ->
                    "https://github.com/JabRef/jabref/blob/main/CHANGELOG.md";
            default ->
                    null;
        };

        if (url != null) {
            link.setOnAction(e -> new OpenBrowserAction(url, dialogService, preferences.getExternalApplicationsPreferences()).execute());
        }

        if (icon != null) {
            link.setGraphic(icon.getGraphicNode());
        }

        return link;
    }
}
