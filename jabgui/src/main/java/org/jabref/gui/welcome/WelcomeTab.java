package org.jabref.gui.welcome;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.edit.OpenBrowserAction;
import org.jabref.gui.frame.FileHistoryMenu;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.URLs;
import org.jabref.gui.walkthrough.utils.WalkthroughUtils;
import org.jabref.gui.welcome.components.DonationProvider;
import org.jabref.gui.welcome.components.QuickSettings;
import org.jabref.gui.welcome.components.Walkthroughs;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WelcomeTab extends Tab {
    private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeTab.class);

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
    private final Stage stage;
    private final WorkspacePreferences workspacePreferences;

    private final VBox main;
    private Walkthroughs walkthroughs;
    private QuickSettings quickSettings;
    private final DonationProvider donationProvider;

    public WelcomeTab(Stage stage,
                      LibraryTabContainer tabContainer,
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
                      BuildInfo buildInfo,
                      WorkspacePreferences workspacePreferences) {
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
        this.stage = stage;
        this.workspacePreferences = workspacePreferences;
        this.recentLibrariesBox = new VBox();
        recentLibrariesBox.getStyleClass().add("welcome-recent-libraries");

        main = new VBox(createTopTitles(), new VBox(), createCommunityBox());
        main.getStyleClass().add("welcome-main-container");
        initializeColumns();

        VBox container = new VBox(main);
        container.setAlignment(Pos.CENTER);

        StackPane rootPane = new StackPane(container);
        setContent(rootPane);

        donationProvider = new DonationProvider(rootPane, preferences, dialogService);
        donationProvider.showIfNeeded();

        setOnClosed(_ -> donationProvider.cleanUp());
    }

    private VBox createTopTitles() {
        Label welcomeLabel = new Label(Localization.lang("Welcome to JabRef"));
        welcomeLabel.getStyleClass().add("welcome-label");
        Label descriptionLabel = new Label(Localization.lang("Stay on top of your literature"));
        descriptionLabel.getStyleClass().add("welcome-description-label");
        VBox topTitles = new VBox(welcomeLabel, descriptionLabel);
        topTitles.getStyleClass().add("welcome-top-titles");
        return topTitles;
    }

    private void initializeColumns() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("welcome-columns-container");

        VBox leftColumn = createLeftColumn();
        GridPane.setHgrow(leftColumn, Priority.ALWAYS);

        VBox rightColumn = createRightColumn();
        GridPane.setHgrow(rightColumn, Priority.ALWAYS);

        WalkthroughUtils.DebouncedRunnable updater = WalkthroughUtils.debounced(() -> updateColumnsLayout(grid, leftColumn, rightColumn, grid.getWidth()));

        grid.localToSceneTransformProperty().addListener(_ -> updater.run());
        grid.layoutBoundsProperty().addListener(_ -> updater.run());
        contentProperty().addListener((_, _, newContent) -> {
            if (newContent == null) {
                return;
            }
            newContent.boundsInParentProperty().addListener(_ -> updater.run());
            newContent.parentProperty().addListener((_, _, newParent) -> {
                if (newParent == null) {
                    return;
                }
                newParent.boundsInParentProperty().addListener(_ -> updater.run());
            });
        });
        VBox.setVgrow(grid, Priority.ALWAYS);
        updater.run();
    }

    private VBox createLeftColumn() {
        VBox leftColumn = new VBox(
                createWelcomeStartBox(),
                createWelcomeRecentBox()
        );
        leftColumn.getStyleClass().add("welcome-content-column");
        return leftColumn;
    }

    private VBox createRightColumn() {
        this.quickSettings = new QuickSettings(preferences, dialogService, taskExecutor);
        this.walkthroughs = new Walkthroughs(stage, tabContainer, stateManager, preferences);
        VBox rightColumn = new VBox(quickSettings, walkthroughs);
        rightColumn.getStyleClass().add("welcome-content-column");
        return rightColumn;
    }

    private void updateColumnsLayout(@NonNull GridPane grid,
                                     @NonNull VBox leftColumn,
                                     @NonNull VBox rightColumn,
                                     double availableWidth) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();

        boolean scrollNeeded = false;
        double threshold = 48 * workspacePreferences.getMainFontSize();
        if (availableWidth < threshold) {
            grid.getStyleClass().add("compact");
            ColumnConstraints single = new ColumnConstraints();
            single.setPercentWidth(100);
            grid.getColumnConstraints().add(single);
            grid.add(leftColumn, 0, 0);
            grid.add(rightColumn, 0, 1);
            scrollNeeded = true;
        } else {
            grid.getStyleClass().remove("compact");
            ColumnConstraints half = new ColumnConstraints();
            half.setPercentWidth(50);
            grid.getColumnConstraints().addAll(half, half);
            grid.add(leftColumn, 0, 0);
            grid.add(rightColumn, 1, 0);
        }

        if (!scrollNeeded) {
            if (!(main.getChildren().get(1) instanceof GridPane)) {
                main.getChildren().set(1, grid);
            }
            quickSettings.enableScroll();
            walkthroughs.enableScroll();
            return;
        }

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("welcome-columns-scroll");
        scrollPane.setStyle("-fx-background-color: transparent;"); // Using class selector is insufficient to prevent background from turning white on click.
        if (!(main.getChildren().get(1) instanceof ScrollPane)) {
            main.getChildren().set(1, scrollPane);
        }
        quickSettings.disableScroll();
        walkthroughs.disableScroll();
    }

    private VBox createWelcomeStartBox() {
        Label header = new Label(Localization.lang("Start"));
        header.getStyleClass().add("welcome-header-label");

        Hyperlink newLibraryLink = createActionLink(Localization.lang("New empty library"),
                () -> new NewDatabaseAction(tabContainer, preferences).execute());

        Hyperlink openLibraryLink = createActionLink(Localization.lang("Open library"),
                () -> new OpenDatabaseAction(tabContainer, preferences, aiService, dialogService,
                        stateManager, fileUpdateMonitor, entryTypesManager, undoManager, clipBoardManager,
                        taskExecutor).execute());

        Hyperlink openExampleLibraryLink = createActionLink(Localization.lang("New example library"),
                this::openExampleLibrary);

        VBox container = new VBox();
        container.getStyleClass().add("welcome-links-content");
        container.getChildren().addAll(newLibraryLink, openExampleLibraryLink, openLibraryLink);

        return createVBoxContainer(header, container);
    }

    private VBox createWelcomeRecentBox() {
        Label header = new Label(Localization.lang("Recent"));
        header.getStyleClass().add("welcome-header-label");

        updateWelcomeRecentLibraries();
        fileHistoryMenu.getItems().addListener((ListChangeListener<MenuItem>) _ -> updateWelcomeRecentLibraries());

        return createVBoxContainer(header, recentLibrariesBox);
    }

    private Hyperlink createActionLink(String text, Runnable action) {
        Hyperlink link = new Hyperlink(text);
        link.getStyleClass().add("welcome-hyperlink");
        link.setOnAction(_ -> action.run());
        return link;
    }

    private void openExampleLibrary() {
        try (InputStream in = WelcomeTab.class.getClassLoader().getResourceAsStream("Chocolate.bib")) {
            if (in == null) {
                LOGGER.warn("Example library file not found.");
                return;
            }
            Reader reader = Importer.getReader(in);
            BibtexParser bibtexParser = new BibtexParser(preferences.getImportFormatPreferences(), fileUpdateMonitor);
            ParserResult result = bibtexParser.parse(reader);
            BibDatabaseContext databaseContext = result.getDatabaseContext();
            LibraryTab libraryTab = LibraryTab.createLibraryTab(databaseContext, tabContainer, dialogService, aiService,
                    preferences, stateManager, fileUpdateMonitor, entryTypesManager, undoManager, clipBoardManager, taskExecutor);
            tabContainer.addTab(libraryTab, true);
        } catch (IOException e) {
            LOGGER.error("Failed to load example library", e);
        }
    }

    private void updateWelcomeRecentLibraries() {
        if (fileHistoryMenu.getItems().isEmpty()) {
            displayNoRecentLibrariesMessage();
            return;
        }
        recentLibrariesBox.getChildren().clear();
        recentLibrariesBox.getStyleClass().add("welcome-links-content");
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

    private VBox createCommunityBox() {
        Label header = new Label(Localization.lang("Community"));
        header.getStyleClass().add("welcome-header-label");
        FlowPane iconLinksContainer = createIconLinksContainer();
        HBox textLinksContainer = createTextLinksContainer();
        HBox versionContainer = createVersionContainer();
        VBox container = new VBox();
        container.getStyleClass().add("welcome-community-content");
        container.getChildren().addAll(iconLinksContainer, textLinksContainer, versionContainer);
        return createVBoxContainer(header, container);
    }

    private FlowPane createIconLinksContainer() {
        FlowPane container = new FlowPane();
        container.getStyleClass().add("welcome-community-icons");

        Hyperlink onlineHelpLink = createFooterLink(Localization.lang("Online help"), StandardActions.HELP, IconTheme.JabRefIcons.HELP);
        Hyperlink privacyPolicyLink = createFooterLink(Localization.lang("Privacy policy"), StandardActions.OPEN_PRIVACY_POLICY, IconTheme.JabRefIcons.BOOK);
        Hyperlink forumLink = createFooterLink(Localization.lang("Community forum"), StandardActions.OPEN_FORUM, IconTheme.JabRefIcons.FORUM);
        Hyperlink mastodonLink = createFooterLink(Localization.lang("Mastodon"), StandardActions.OPEN_MASTODON, IconTheme.JabRefIcons.MASTODON);
        Hyperlink linkedInLink = createFooterLink(Localization.lang("LinkedIn"), StandardActions.OPEN_LINKEDIN, IconTheme.JabRefIcons.LINKEDIN);
        Hyperlink donationLink = createFooterLink(Localization.lang("Donation"), StandardActions.DONATE, IconTheme.JabRefIcons.DONATE);

        container.getChildren().addAll(onlineHelpLink, privacyPolicyLink, forumLink, mastodonLink, linkedInLink, donationLink);
        return container;
    }

    private HBox createTextLinksContainer() {
        HBox container = new HBox();
        container.getStyleClass().add("welcome-community-links");

        Hyperlink devVersionLink = createFooterLink(Localization.lang("Download development version"), StandardActions.OPEN_DEV_VERSION_LINK, null);
        Hyperlink changelogLink = createFooterLink(Localization.lang("CHANGELOG"), StandardActions.OPEN_CHANGELOG, null);

        container.getChildren().addAll(devVersionLink, changelogLink);
        return container;
    }

    private Hyperlink createFooterLink(String text, StandardActions action, IconTheme.JabRefIcons icon) {
        Hyperlink link = new Hyperlink(text);
        link.getStyleClass().add("welcome-community-link");
        String url = switch (action) {
            case HELP ->
                    URLs.HELP_URL;
            case OPEN_FORUM ->
                    URLs.FORUM_URL;
            case OPEN_MASTODON ->
                    URLs.MASTODON_URL;
            case OPEN_LINKEDIN ->
                    URLs.LINKEDIN_URL;
            case DONATE ->
                    URLs.DONATE_URL;
            case OPEN_DEV_VERSION_LINK ->
                    URLs.DEV_VERSION_LINK_URL;
            case OPEN_CHANGELOG ->
                    URLs.CHANGELOG_URL;
            case OPEN_PRIVACY_POLICY ->
                    URLs.PRIVACY_POLICY_URL;
            default ->
                    null;
        };
        if (url != null) {
            link.setOnAction(_ -> new OpenBrowserAction(url, dialogService, preferences.getExternalApplicationsPreferences()).execute());
        }
        if (icon != null) {
            link.setGraphic(icon.getGraphicNode());
        }
        return link;
    }

    private HBox createVersionContainer() {
        HBox container = new HBox();
        container.getStyleClass().add("welcome-community-version");
        Label versionLabel = new Label(Localization.lang("Current JabRef version: %0", buildInfo.version));
        versionLabel.getStyleClass().add("welcome-community-version-text");
        container.getChildren().add(versionLabel);
        return container;
    }

    private VBox createVBoxContainer(Node... nodes) {
        VBox box = new VBox();
        box.getStyleClass().add("welcome-section");
        box.getChildren().addAll(nodes);
        return box;
    }
}
