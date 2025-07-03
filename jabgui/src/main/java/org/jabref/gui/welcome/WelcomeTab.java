package org.jabref.gui.welcome;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.edit.OpenBrowserAction;
import org.jabref.gui.frame.FileHistoryMenu;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.push.PushToApplication;
import org.jabref.gui.push.PushToApplicationPreferences;
import org.jabref.gui.push.PushToApplications;
import org.jabref.gui.slr.StudyCatalogItem;
import org.jabref.gui.theme.Theme;
import org.jabref.gui.theme.ThemeTypes;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.URLs;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.CompositeSearchBasedFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.util.FileUpdateMonitor;

import org.jetbrains.annotations.NotNull;
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

        this.recentLibrariesBox = new VBox();
        recentLibrariesBox.getStyleClass().add("welcome-recent-libraries");

        VBox topTitles = createTopTitles();
        HBox columnsContainer = createColumnsContainer();

        VBox mainContainer = new VBox();
        mainContainer.getStyleClass().add("welcome-main-container");
        mainContainer.getChildren().addAll(topTitles, columnsContainer);

        VBox container = new VBox();
        container.getChildren().add(mainContainer);
        VBox.setVgrow(mainContainer, Priority.ALWAYS);
        container.setAlignment(Pos.CENTER);
        setContent(container);
    }

    private Optional<ButtonType> createQuickSettingsDialog(String titleKey, String headerKey, Node... children) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Localization.lang(titleKey));
        dialog.setHeaderText(Localization.lang(headerKey));

        VBox content = new VBox();
        content.getStyleClass().add("quick-settings-dialog-container");
        content.getChildren().addAll(children);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        return dialogService.showCustomDialogAndWait(dialog);
    }

    private @NotNull Button createQuickSettingsButton(String text, IconTheme.JabRefIcons icon, Runnable action) {
        Button button = new Button(text);
        button.setGraphic(icon.getGraphicNode());
        button.getStyleClass().add("quick-settings-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(_ -> action.run());
        return button;
    }

    private @NotNull Button createHelpButton(String url) {
        Button helpButton = new Button();
        helpButton.setGraphic(IconTheme.JabRefIcons.HELP.getGraphicNode());
        helpButton.getStyleClass().add("help-button");
        helpButton.setOnAction(_ -> new OpenBrowserAction(url, dialogService, preferences.getExternalApplicationsPreferences()).execute());
        return helpButton;
    }

    private VBox createTopTitles() {
        Label welcomeLabel = new Label(Localization.lang("Welcome to JabRef"));
        welcomeLabel.getStyleClass().add("welcome-label");

        Label descriptionLabel = new Label(Localization.lang("Stay on top of your literature"));
        descriptionLabel.getStyleClass().add("welcome-description-label");

        VBox topTitles = new VBox();
        topTitles.getStyleClass().add("welcome-top-titles");
        topTitles.getChildren().addAll(welcomeLabel, descriptionLabel);

        return topTitles;
    }

    private HBox createColumnsContainer() {
        VBox leftColumn = createLeftColumn();
        VBox rightColumn = createRightColumn();

        HBox columnsContainer = new HBox();
        columnsContainer.getStyleClass().add("welcome-columns-container");

        leftColumn.getStyleClass().add("welcome-left-column");
        rightColumn.getStyleClass().add("welcome-right-column");

        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        columnsContainer.getChildren().addAll(leftColumn, rightColumn);

        return columnsContainer;
    }

    private VBox createLeftColumn() {
        VBox startBox = createWelcomeStartBox();
        VBox recentBox = createWelcomeRecentBox();

        VBox leftColumn = new VBox();
        leftColumn.getStyleClass().add("welcome-content-column");
        leftColumn.getChildren().addAll(startBox, recentBox);

        return leftColumn;
    }

    private VBox createRightColumn() {
        VBox quickSettingsBox = createQuickSettingsBox();
        VBox communityBox = createCommunityBox();

        VBox rightColumn = new VBox();
        rightColumn.getStyleClass().add("welcome-content-column");
        rightColumn.getChildren().addAll(quickSettingsBox, communityBox);

        return rightColumn;
    }

    private VBox createQuickSettingsBox() {
        Label header = new Label(Localization.lang("Quick Settings"));
        header.getStyleClass().add("welcome-header-label");

        VBox actions = new VBox();
        actions.getStyleClass().add("quick-settings-content");

        Button mainFileDirButton = createQuickSettingsButton(
                Localization.lang("Set main file directory"),
                IconTheme.JabRefIcons.FOLDER,
                this::showMainFileDirectoryDialog
        );

        Button themeButton = createQuickSettingsButton(
                Localization.lang("Change visual theme"),
                IconTheme.JabRefIcons.PREFERENCES,
                this::showThemeDialog
        );

        Button largeLibraryButton = createQuickSettingsButton(
                Localization.lang("Optimize for large libraries"),
                IconTheme.JabRefIcons.SELECTORS,
                this::showLargeLibraryOptimizationDialog
        );

        Button pushApplicationButton = createQuickSettingsButton(
                Localization.lang("Configure push to applications"),
                IconTheme.JabRefIcons.APPLICATION_GENERIC,
                this::showPushApplicationConfigurationDialog
        );

        Button onlineServicesButton = createQuickSettingsButton(
                Localization.lang("Configure web search services"),
                IconTheme.JabRefIcons.WWW,
                this::showOnlineServicesConfigurationDialog
        );

        Button entryTableButton = createQuickSettingsButton(
                Localization.lang("Customize entry table"),
                IconTheme.JabRefIcons.TOGGLE_GROUPS,
                this::showEntryTableConfigurationDialog
        );

        actions.getChildren().addAll(mainFileDirButton, themeButton, largeLibraryButton, pushApplicationButton, onlineServicesButton, entryTableButton);

        return createVBoxContainer(header, actions);
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

    private void showMainFileDirectoryDialog() {
        TextField pathField = new TextField();
        pathField.setPromptText(Localization.lang("Main file directory path"));
        FilePreferences filePreferences = preferences.getFilePreferences();
        pathField.setText(filePreferences.getMainFileDirectory()
                                         .map(Path::toString).orElse(""));

        Button browseButton = new Button();
        browseButton.setGraphic(IconTheme.JabRefIcons.OPEN.getGraphicNode());
        browseButton.getStyleClass().addAll("icon-button", "narrow");
        browseButton.setOnAction(_ -> {
            DirectoryDialogConfiguration dirConfig = new DirectoryDialogConfiguration.Builder()
                    .withInitialDirectory(filePreferences.getWorkingDirectory())
                    .build();
            dialogService.showDirectorySelectionDialog(dirConfig)
                         .ifPresent(selectedDir -> pathField.setText(selectedDir.toString()));
        });

        HBox pathContainer = new HBox(
                new Label(Localization.lang("Directory path") + ":"),
                pathField,
                browseButton
        );

        Optional<ButtonType> result = createQuickSettingsDialog(
                "Set main file directory",
                "Choose the default directory for storing attached files",
                pathContainer
        );

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        filePreferences.setMainFileDirectory(pathField.getText());
        filePreferences.setStoreFilesRelativeToBibFile(false);
    }

    private void showThemeDialog() {
        ToggleGroup themeGroup = new ToggleGroup();
        HBox radioContainer = new HBox();

        WorkspacePreferences workspacePreferences = preferences.getWorkspacePreferences();
        Theme currentTheme = workspacePreferences.getTheme();

        RadioButton lightRadio = new RadioButton(ThemeTypes.LIGHT.getDisplayName());
        lightRadio.setToggleGroup(themeGroup);
        lightRadio.setUserData(ThemeTypes.LIGHT);
        VBox lightBox = createThemeOption(lightRadio, new ThemeWireFrameComponent("light"));
        radioContainer.getChildren().add(lightBox);

        RadioButton darkRadio = new RadioButton(ThemeTypes.DARK.getDisplayName());
        darkRadio.setToggleGroup(themeGroup);
        darkRadio.setUserData(ThemeTypes.DARK);
        VBox darkBox = createThemeOption(darkRadio, new ThemeWireFrameComponent("dark"));
        radioContainer.getChildren().add(darkBox);

        RadioButton customRadio = new RadioButton(ThemeTypes.CUSTOM.getDisplayName());
        customRadio.setToggleGroup(themeGroup);
        customRadio.setUserData(ThemeTypes.CUSTOM);
        VBox customBox = createThemeOption(customRadio, new ThemeWireFrameComponent("custom"));
        radioContainer.getChildren().add(customBox);

        switch (currentTheme.getType()) {
            case DEFAULT -> lightRadio.setSelected(true);
            case EMBEDDED -> darkRadio.setSelected(true);
            case CUSTOM -> customRadio.setSelected(true);
        }

        TextField customThemePath = new TextField();
        customThemePath.setPromptText(Localization.lang("Custom theme file path"));
        customThemePath.setText(currentTheme.getType() == Theme.Type.CUSTOM ? currentTheme.getName() : "");

        Button browseButton = new Button();
        browseButton.setGraphic(new JabRefIconView(IconTheme.JabRefIcons.OPEN));
        browseButton.setTooltip(new Tooltip(Localization.lang("Browse")));
        browseButton.getStyleClass().addAll("icon-button", "narrow");
        browseButton.setPrefHeight(20.0);
        browseButton.setPrefWidth(20.0);

        HBox customThemePathBox = new HBox(4.0, customThemePath, browseButton);
        customThemePathBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(customThemePath, Priority.ALWAYS);

        themeGroup.selectedToggleProperty().addListener((_, _, newValue) -> {
            boolean isCustom = newValue != null && newValue.getUserData() == ThemeTypes.CUSTOM;
            customThemePathBox.setManaged(isCustom);
            customThemePathBox.setVisible(isCustom);
            customThemePathBox.getScene().getWindow().sizeToScene();
        });

        browseButton.setOnAction(_ -> {
            String fileDir = customThemePath.getText().isEmpty() ?
                    preferences.getInternalPreferences().getLastPreferencesExportPath().toString() :
                    customThemePath.getText();

            FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                    .addExtensionFilter(StandardFileType.CSS)
                    .withDefaultExtension(StandardFileType.CSS)
                    .withInitialDirectory(fileDir)
                    .build();

            dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file ->
                    customThemePath.setText(file.toAbsolutePath().toString()));
        });

        if (currentTheme.getType() != Theme.Type.CUSTOM) {
            customThemePathBox.setVisible(false);
            customThemePathBox.setManaged(false);
        }

        Optional<ButtonType> result = createQuickSettingsDialog(
                "Change visual theme",
                "Select your preferred theme for the application",
                radioContainer,
                customThemePathBox
        );
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        Toggle selectedToggle = themeGroup.getSelectedToggle();
        if (selectedToggle != null) {
            ThemeTypes selectedTheme = (ThemeTypes) selectedToggle.getUserData();
            Theme newTheme = switch (selectedTheme) {
                case LIGHT -> Theme.light();
                case DARK -> Theme.dark();
                case CUSTOM -> {
                    String customPath = customThemePath.getText().trim();
                    if (customPath.isEmpty()) {
                        dialogService.showErrorDialogAndWait(
                                Localization.lang("Error"),
                                Localization.lang("Specify a custom theme file path"));
                        yield null;
                    }
                    yield Theme.custom(customPath);
                }
            };
            if (newTheme != null) {
                workspacePreferences.setTheme(newTheme);
            }
        }
    }

    private void showLargeLibraryOptimizationDialog() {
        Label performanceOptimizationLabel = new Label(Localization.lang("Select performance optimizations to apply"));
        performanceOptimizationLabel.setWrapText(true);
        performanceOptimizationLabel.setMaxWidth(400);

        HBox performanceOptimizationHeader = new HBox(
                performanceOptimizationLabel,
                createHelpButton("https://docs.jabref.org/faq#q-i-have-a-huge-library.-what-can-i-do-to-mitigate-performance-issues")
        );

        CheckBox disableFulltextIndexing = new CheckBox(Localization.lang("Disable fulltext indexing"));
        disableFulltextIndexing.setSelected(true);

        CheckBox disableCreationDate = new CheckBox(Localization.lang("Disable creation date timestamps"));
        disableCreationDate.setSelected(true);

        CheckBox disableModificationDate = new CheckBox(Localization.lang("Disable modification date timestamps"));
        disableModificationDate.setSelected(true);

        CheckBox disableAutosave = new CheckBox(Localization.lang("Disable automatic saving"));
        disableAutosave.setSelected(true);

        CheckBox disableGroupCount = new CheckBox(Localization.lang("Disable group entry counts"));
        disableGroupCount.setSelected(true);

        Optional<ButtonType> result = createQuickSettingsDialog(
                "Optimize for large libraries",
                "Improve performance when working with libraries containing many entries",
                performanceOptimizationHeader,
                disableFulltextIndexing,
                disableCreationDate,
                disableModificationDate,
                disableAutosave,
                disableGroupCount
        );

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        if (disableFulltextIndexing.isSelected()) {
            preferences.getFilePreferences().setFulltextIndexLinkedFiles(false);
        }
        if (disableCreationDate.isSelected()) {
            preferences.getTimestampPreferences().setAddCreationDate(false);
        }
        if (disableModificationDate.isSelected()) {
            preferences.getTimestampPreferences().setAddModificationDate(false);
        }
        if (disableAutosave.isSelected()) {
            preferences.getLibraryPreferences().setAutoSave(false);
        }
        if (disableGroupCount.isSelected()) {
            preferences.getGroupsPreferences().setDisplayGroupCount(false);
        }
    }

    private void showPushApplicationConfigurationDialog() {
        Label explanationLabel = new Label(Localization.lang("Detected applications are highlighted"));
        explanationLabel.setWrapText(true);
        explanationLabel.setMaxWidth(400);

        ListView<PushToApplication> applicationsList = new ListView<>();
        applicationsList.getStyleClass().add("applications-list");

        List<PushToApplication> allApplications = PushToApplications.getAllApplications(dialogService, preferences);
        List<PushToApplication> detectedApplications = detectAvailableApplications(allApplications);

        List<PushToApplication> sortedApplications = new ArrayList<>(detectedApplications);
        allApplications.stream()
                       .filter(app -> !detectedApplications.contains(app))
                       .forEach(sortedApplications::add);

        applicationsList.getItems().addAll(sortedApplications);
        applicationsList.setCellFactory(_ -> new PushApplicationListCell(detectedApplications));

        PushToApplicationPreferences pushToApplicationPreferences = preferences.getPushToApplicationPreferences();
        String currentAppName = pushToApplicationPreferences.getActiveApplicationName();
        if (!currentAppName.isEmpty()) {
            sortedApplications.stream()
                              .filter(app -> app.getDisplayName().equals(currentAppName))
                              .findFirst()
                              .ifPresent(applicationsList.getSelectionModel()::select);
        }

        Optional<ButtonType> result = createQuickSettingsDialog(
                "Configure push to applications",
                "Select your text editor or LaTeX application for pushing citations",
                explanationLabel,
                applicationsList
        );

        if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
            return;
        }
        PushToApplication selectedApp = applicationsList.getSelectionModel().getSelectedItem();
        if (selectedApp != null) {
            pushToApplicationPreferences.setActiveApplicationName(selectedApp.getDisplayName());
        }
    }

    private List<PushToApplication> detectAvailableApplications(List<PushToApplication> allApplications) {
        return allApplications.stream().filter(this::isApplicationAvailable).toList();
    }

    private boolean isApplicationAvailable(PushToApplication application) {
        String appName = application.getDisplayName().toLowerCase();

        String[] possibleNames = switch (appName) {
            case "emacs" -> new String[] {"emacs", "emacsclient"};
            case "lyx/kile" -> new String[] {"lyx", "kile"};
            case "texmaker" -> new String[] {"texmaker"};
            case "texstudio" -> new String[] {"texstudio"};
            case "texworks" -> new String[] {"texworks"};
            case "vim" -> new String[] {"vim", "nvim", "gvim"};
            case "winedt" -> new String[] {"winedt"};
            case "sublime text" -> new String[] {"subl", "sublime_text"};
            case "texshop" -> new String[] {"texshop"};
            case "vscode" -> new String[] {"code", "code-insiders"};
            default -> new String[] {appName.replace(" ", "").toLowerCase()};
        };

        for (String executable : possibleNames) {
            if (isExecutableInPath(executable)) {
                return true;
            }
        }

        return false;
    }

    private boolean isExecutableInPath(String executable) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", executable);
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            try {
                ProcessBuilder pb = new ProcessBuilder("where", executable);
                Process process = pb.start();
                return process.waitFor() == 0;
            } catch (IOException | InterruptedException ex) {
                return false;
            }
        }
    }

    private static class PushApplicationListCell extends ListCell<PushToApplication> {
        private final List<PushToApplication> detectedApplications;

        public PushApplicationListCell(List<PushToApplication> detectedApplications) {
            this.detectedApplications = detectedApplications;
            this.getStyleClass().add("application-item");
        }

        @Override
        protected void updateItem(PushToApplication application, boolean empty) {
            super.updateItem(application, empty);

            if (empty || application == null) {
                setText(null);
                setGraphic(null);
                getStyleClass().removeAll("detected-application");
                return;
            }

            setText(application.getDisplayName());
            setGraphic(application.getApplicationIcon().getGraphicNode());

            if (detectedApplications.contains(application)) {
                if (!getStyleClass().contains("detected-application")) {
                    getStyleClass().add("detected-application");
                }
            } else {
                getStyleClass().removeAll("detected-application");
            }
        }
    }

    private void showOnlineServicesConfigurationDialog() {
        CheckBox versionCheckBox = new CheckBox(Localization.lang("Check for updates at startup"));
        versionCheckBox.setSelected(preferences.getInternalPreferences().isVersionCheckEnabled());

        CheckBox webSearchBox = new CheckBox(Localization.lang("Enable web search"));
        webSearchBox.setSelected(preferences.getImporterPreferences().areImporterEnabled());

        CheckBox grobidCheckBox = new CheckBox(Localization.lang("Enable metadata extraction service"));
        grobidCheckBox.setSelected(preferences.getGrobidPreferences().isGrobidEnabled());

        HBox grobidUrl = new HBox();
        Label grobidUrlLabel = new Label(Localization.lang("Service URL") + ":");
        TextField grobidUrlField = new TextField(preferences.getGrobidPreferences().getGrobidURL());
        HBox.setHgrow(grobidUrlField, Priority.ALWAYS);
        grobidUrl.getChildren().addAll(
                grobidUrlLabel,
                grobidUrlField,
                createHelpButton("https://docs.jabref.org/collect/newentryfromplaintext#grobid")
        );

        grobidUrl.visibleProperty().bind(grobidCheckBox.selectedProperty());
        grobidUrl.managedProperty().bind(grobidCheckBox.selectedProperty());

        Label fetchersLabel = new Label(Localization.lang("Web search databases") + ":");
        HBox fetchersHeader = new HBox();
        fetchersHeader.getChildren().addAll(
                fetchersLabel,
                createHelpButton("https://docs.jabref.org/collect/import-using-online-bibliographic-database")
        );

        List<StudyCatalogItem> availableFetchers = WebFetchers
                .getSearchBasedFetchers(preferences.getImportFormatPreferences(), preferences.getImporterPreferences())
                .stream()
                .map(SearchBasedFetcher::getName)
                .filter(name -> !CompositeSearchBasedFetcher.FETCHER_NAME.equals(name))
                .map(name -> {
                    boolean enabled = preferences.getImporterPreferences().getCatalogs().contains(name);
                    return new StudyCatalogItem(name, enabled);
                })
                .toList();

        VBox fetchersContainer = new VBox();
        fetchersContainer.getStyleClass().add("fetchers-container");
        List<CheckBox> fetcherCheckBoxes = new ArrayList<>();

        for (StudyCatalogItem fetcher : availableFetchers) {
            CheckBox fetcherCheckBox = new CheckBox(fetcher.getName());
            fetcherCheckBox.setSelected(fetcher.isEnabled());
            fetcherCheckBoxes.add(fetcherCheckBox);
            fetchersContainer.getChildren().add(fetcherCheckBox);
        }

        ScrollPane fetchersScrollPane = new ScrollPane(fetchersContainer);
        fetchersScrollPane.setFitToWidth(true);
        fetchersScrollPane.setMaxHeight(288);
        fetchersScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fetchersScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Optional<ButtonType> result = createQuickSettingsDialog(
                "Configure web search services",
                "Enable and configure online databases and services for importing entries",
                versionCheckBox,
                webSearchBox,
                grobidCheckBox,
                grobidUrl,
                fetchersHeader,
                fetchersScrollPane
        );

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        preferences.getInternalPreferences().setVersionCheckEnabled(versionCheckBox.isSelected());
        preferences.getImporterPreferences().setImporterEnabled(webSearchBox.isSelected());
        preferences.getGrobidPreferences().setGrobidEnabled(grobidCheckBox.isSelected());
        preferences.getGrobidPreferences().setGrobidURL(grobidUrlField.getText());

        List<String> enabledFetchers = new ArrayList<>();
        for (int i = 0; i < fetcherCheckBoxes.size(); i++) {
            if (fetcherCheckBoxes.get(i).isSelected()) {
                enabledFetchers.add(availableFetchers.get(i).getName());
            }
        }
        preferences.getImporterPreferences().setCatalogs(enabledFetchers);
    }

    private void showEntryTableConfigurationDialog() {
        CheckBox showCitationKeyBox = new CheckBox(Localization.lang("Show citation key column"));

        ColumnPreferences columnPreferences = preferences.getMainTablePreferences()
                                                         .getColumnPreferences();
        boolean isCitationKeyVisible = columnPreferences
                .getColumns()
                .stream()
                .anyMatch(column -> column.getType() == MainTableColumnModel.Type.NORMALFIELD
                        && InternalField.KEY_FIELD.getName().equals(column.getQualifier()));

        showCitationKeyBox.setSelected(isCitationKeyVisible);

        Optional<ButtonType> result = createQuickSettingsDialog(
                "Customize entry table",
                "Configure which columns are displayed in the entry table",
                showCitationKeyBox
        );

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        boolean shouldShow = showCitationKeyBox.isSelected();

        if (shouldShow && !isCitationKeyVisible) {
            MainTableColumnModel citationKeyColumn = new MainTableColumnModel(
                    MainTableColumnModel.Type.NORMALFIELD,
                    InternalField.KEY_FIELD.getName()
            );
            columnPreferences.getColumns().addFirst(citationKeyColumn);
        } else if (!shouldShow && isCitationKeyVisible) {
            columnPreferences.getColumns().removeIf(column ->
                    column.getType() == MainTableColumnModel.Type.NORMALFIELD
                            && InternalField.KEY_FIELD.getName().equals(column.getQualifier()));
        }
    }

    private VBox createThemeOption(RadioButton radio, Node wireframe) {
        VBox container = new VBox();
        container.getStyleClass().add("theme-option");
        container.getChildren().addAll(radio, wireframe);
        container.setOnMouseClicked(_ -> radio.fire());
        return container;
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
        } catch (IOException ex) {
            LOGGER.error("Failed to load example library", ex);
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

    private VBox createVBoxContainer(Node... nodes) {
        VBox box = new VBox();
        box.getStyleClass().add("welcome-section");
        box.getChildren().addAll(nodes);
        return box;
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
            case HELP -> URLs.HELP_URL;
            case OPEN_FORUM -> URLs.FORUM_URL;
            case OPEN_MASTODON -> URLs.MASTODON_URL;
            case OPEN_LINKEDIN -> URLs.LINKEDIN_URL;
            case DONATE -> URLs.DONATE_URL;
            case OPEN_DEV_VERSION_LINK -> URLs.DEV_VERSION_LINK_URL;
            case OPEN_CHANGELOG -> URLs.CHANGELOG_URL;
            case OPEN_PRIVACY_POLICY -> URLs.PRIVACY_POLICY_URL;
            default -> null;
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
}
