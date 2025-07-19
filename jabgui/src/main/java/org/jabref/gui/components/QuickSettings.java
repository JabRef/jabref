package org.jabref.gui.components;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.push.GuiPushToApplication;
import org.jabref.gui.push.GuiPushToApplications;
import org.jabref.gui.slr.StudyCatalogItem;
import org.jabref.gui.theme.Theme;
import org.jabref.gui.theme.ThemeTypes;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.URLs;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.gui.welcome.components.PathSelectionField;
import org.jabref.gui.welcome.components.PushToApplicationCell;
import org.jabref.gui.welcome.components.PushToApplicationDetector;
import org.jabref.gui.welcome.components.QuickSettingsDialog;
import org.jabref.gui.welcome.components.ThemeWireFrame;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.CompositeSearchBasedFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.push.PushToApplication;
import org.jabref.logic.push.PushToApplicationPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.field.InternalField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickSettings extends VBox {
    public static final int SCROLL_PANE_HEIGHT = 35 * 4 + 8 * 3; // Scroll pane show exactly 4 items, item-height * 4 + gap * 3
    private static final Logger LOGGER = LoggerFactory.getLogger(QuickSettings.class);

    private final GuiPreferences preferences;
    private final DialogService dialogService;

    public QuickSettings(GuiPreferences preferences, DialogService dialogService) {
        this.preferences = preferences;
        this.dialogService = dialogService;

        initializeComponent();
    }

    private void initializeComponent() {
        Label header = new Label(Localization.lang("Quick Settings"));
        header.getStyleClass().add("welcome-header-label");

        VBox actions = new VBox();
        actions.getStyleClass().add("quick-settings-container");

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

        actions.getChildren().addAll(
                mainFileDirButton,
                themeButton,
                largeLibraryButton,
                pushApplicationButton,
                onlineServicesButton,
                entryTableButton
        );

        ScrollPane scrollPane = new ScrollPane(actions);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefHeight(SCROLL_PANE_HEIGHT);
        scrollPane.setMinHeight(SCROLL_PANE_HEIGHT);
        scrollPane.setMaxHeight(SCROLL_PANE_HEIGHT);

        this.getChildren().addAll(header, scrollPane);
        this.getStyleClass().add("welcome-content-column");
    }

    private Button createQuickSettingsButton(String text, IconTheme.JabRefIcons icon, Runnable action) {
        Button button = new Button(text);
        button.setGraphic(icon.getGraphicNode());
        button.getStyleClass().add("quick-settings-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(_ -> action.run());
        return button;
    }

    private void showMainFileDirectoryDialog() {
        FilePreferences filePreferences = preferences.getFilePreferences();
        HBox mainFileDirHeader = QuickSettingsDialog.createHeaderWithHelp(
                "Set the default directory for storing attached files. Files will be stored relative to this directory unless specified otherwise.",
                URLs.FILE_LINKS_DOC
        );
        PathSelectionField pathSelector = new PathSelectionField(Localization.lang("Main file directory path"));
        pathSelector.setText(filePreferences.getMainFileDirectory()
                                            .map(Path::toString)
                                            .orElse(""));
        pathSelector.setOnBrowseAction(() -> {
            DirectoryDialogConfiguration dirConfig = new DirectoryDialogConfiguration.Builder()
                    .withInitialDirectory(filePreferences.getWorkingDirectory())
                    .build();
            dialogService.showDirectorySelectionDialog(dirConfig)
                         .ifPresent(selectedDir -> pathSelector.setText(selectedDir.toString()));
        });
        Optional<ButtonType> result = QuickSettingsDialog
                .create()
                .title("Set main file directory")
                .header("Choose the default directory for storing attached files")
                .content(mainFileDirHeader, pathSelector)
                .show();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            filePreferences.setMainFileDirectory(pathSelector.getText());
            filePreferences.setStoreFilesRelativeToBibFile(false);
        }
    }

    private void showThemeDialog() {
        HBox themeHeader = QuickSettingsDialog.createHeaderWithHelp(
                "Choose between light, dark, or custom themes to personalize your JabRef experience.",
                URLs.CUSTOM_THEME_DOC
        );
        ToggleGroup themeGroup = new ToggleGroup();
        HBox radioContainer = new HBox();
        org.jabref.gui.WorkspacePreferences workspacePreferences = preferences.getWorkspacePreferences();
        Theme currentTheme = workspacePreferences.getTheme();

        RadioButton lightRadio = new RadioButton(ThemeTypes.LIGHT.getDisplayName());
        lightRadio.setToggleGroup(themeGroup);
        lightRadio.setUserData(ThemeTypes.LIGHT);
        VBox lightBox = createThemeOption(lightRadio, new ThemeWireFrame(ThemeTypes.LIGHT));
        radioContainer.getChildren().add(lightBox);

        RadioButton darkRadio = new RadioButton(ThemeTypes.DARK.getDisplayName());
        darkRadio.setToggleGroup(themeGroup);
        darkRadio.setUserData(ThemeTypes.DARK);
        VBox darkBox = createThemeOption(darkRadio, new ThemeWireFrame(ThemeTypes.DARK));
        radioContainer.getChildren().add(darkBox);

        RadioButton customRadio = new RadioButton(ThemeTypes.CUSTOM.getDisplayName());
        customRadio.setToggleGroup(themeGroup);
        customRadio.setUserData(ThemeTypes.CUSTOM);
        VBox customBox = createThemeOption(customRadio, new ThemeWireFrame(ThemeTypes.CUSTOM));
        radioContainer.getChildren().add(customBox);

        switch (currentTheme.getType()) {
            case DEFAULT -> lightRadio.setSelected(true);
            case EMBEDDED -> darkRadio.setSelected(true);
            case CUSTOM -> customRadio.setSelected(true);
        }

        PathSelectionField customThemePath = new PathSelectionField(Localization.lang("Custom theme file path"));
        customThemePath.setText(currentTheme.getType() == Theme.Type.CUSTOM ? currentTheme.getName() : "");
        customThemePath.setOnBrowseAction(() -> {
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
        customThemePath.setAlignment(Pos.CENTER_LEFT);

        themeGroup.selectedToggleProperty().addListener((_, _, newValue) -> {
            boolean isCustom = newValue != null && newValue.getUserData() == ThemeTypes.CUSTOM;
            customThemePath.setManaged(isCustom);
            customThemePath.setVisible(isCustom);
            customThemePath.getScene().getWindow().sizeToScene();
        });

        if (currentTheme.getType() != Theme.Type.CUSTOM) {
            customThemePath.setVisible(false);
            customThemePath.setManaged(false);
        }

        Optional<ButtonType> result = QuickSettingsDialog
                .create()
                .title("Change visual theme")
                .header("Select your preferred theme for the application")
                .validate(() -> Optional
                        .ofNullable(themeGroup.getSelectedToggle())
                        .map(toggle -> toggle.getUserData() != ThemeTypes.CUSTOM || Path.of(customThemePath.getText()).toFile().exists())
                        .orElse(false))
                .depend(List.of(customThemePath.getTextField().textProperty(), themeGroup.selectedToggleProperty()))
                .content(themeHeader, radioContainer, customThemePath)
                .show();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        Toggle selectedToggle = themeGroup.getSelectedToggle();
        if (selectedToggle != null) {
            ThemeTypes selectedTheme = (ThemeTypes) selectedToggle.getUserData();
            Theme newTheme = switch (selectedTheme) {
                case LIGHT -> Theme.light();
                case DARK -> Theme.dark();
                case CUSTOM -> Theme.custom(customThemePath.getText().trim());
            };
            workspacePreferences.setTheme(newTheme);
        }
    }

    private void showLargeLibraryOptimizationDialog() {
        HBox performanceOptimizationHeader = QuickSettingsDialog.createHeaderWithHelp(
                "Select features to disable. Disabling these features can significantly improve performance when working with large libraries.",
                URLs.PERFORMANCE_DOC
        );

        CheckBox disableFulltextIndexing = new CheckBox(Localization.lang("Fulltext indexing"));
        disableFulltextIndexing.setSelected(true);
        CheckBox disableCreationDate = new CheckBox(Localization.lang("Creation date timestamps"));
        disableCreationDate.setSelected(true);
        CheckBox disableModificationDate = new CheckBox(Localization.lang("Modification date timestamps"));
        disableModificationDate.setSelected(true);
        CheckBox disableAutosave = new CheckBox(Localization.lang("Automatic saving"));
        disableAutosave.setSelected(true);
        CheckBox disableGroupCount = new CheckBox(Localization.lang("Group entry counts"));
        disableGroupCount.setSelected(true);

        VBox checkboxes = new VBox(
                disableFulltextIndexing,
                disableCreationDate,
                disableModificationDate,
                disableAutosave,
                disableGroupCount
        );
        checkboxes.getStyleClass().add("optimization-checkboxes");

        Optional<ButtonType> result = QuickSettingsDialog
                .create()
                .title("Optimize for large libraries")
                .header("Improve performance when working with libraries containing many entries")
                .content(performanceOptimizationHeader, checkboxes)
                .show();

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
        HBox pushApplicationHeader = QuickSettingsDialog.createHeaderWithHelp(
                "Configure external applications to push citations to. Detected applications are highlighted. Applications that are not detected can be set manually by specifying the path to the executable.",
                URLs.PUSH_TO_APPLICATIONS_DOC
        );

        ListView<GuiPushToApplication> applicationsList = new ListView<>();
        applicationsList.getStyleClass().add("applications-list");

        PushToApplicationPreferences pushToApplicationPreferences = preferences.getPushToApplicationPreferences();
        List<GuiPushToApplication> allApplications = GuiPushToApplications.getAllGUIApplications(dialogService, pushToApplicationPreferences);
        applicationsList.getItems().addAll(allApplications);
        applicationsList.setCellFactory(_ -> new PushToApplicationCell(Set.of()));

        if (!pushToApplicationPreferences.getActiveApplicationName().isEmpty()) {
            allApplications.stream()
                           .filter(app -> app.getDisplayName().equals(pushToApplicationPreferences.getActiveApplicationName()))
                           .findFirst()
                           .ifPresent(applicationsList.getSelectionModel()::select);
        }

        PathSelectionField pathSelector = new PathSelectionField(Localization.lang("Path to application executable"));
        pathSelector.setOnBrowseAction(() -> {
            FileDialogConfiguration fileConfig = new FileDialogConfiguration.Builder()
                    .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                    .build();
            dialogService.showFileOpenDialog(fileConfig)
                         .ifPresent(selectedFile -> pathSelector.setText(selectedFile.toString()));
        });

        Map<PushToApplication, String> detectedApplicationPaths = new ConcurrentHashMap<>();
        TextField pathField = pathSelector.getTextField();

        applicationsList.getSelectionModel().selectedItemProperty().addListener((_, _, selectedApp) -> {
            if (selectedApp == null) {
                pathSelector.setText("");
                pathField.setPromptText(Localization.lang("Path to application executable"));
                return;
            }
            String existingPath = pushToApplicationPreferences.getCommandPaths().get(selectedApp.getDisplayName());
            pathSelector.setText(PushToApplicationDetector.isValidAbsolutePath(existingPath) ?
                    existingPath :
                    Objects.requireNonNullElse(detectedApplicationPaths.get(selectedApp), ""));
        });

        pathField.textProperty().addListener((_, _, newText) -> {
            if (newText == null || newText.trim().isEmpty()) {
                pathField.getStyleClass().removeAll("invalid-path");
                return;
            }
            if (PushToApplicationDetector.isValidAbsolutePath(newText)) {
                pathField.getStyleClass().removeAll("invalid-path");
            } else {
                if (!pathField.getStyleClass().contains("invalid-path")) {
                    pathField.getStyleClass().add("invalid-path");
                }
            }
        });

        CompletableFuture<Map<GuiPushToApplication, String>> detectionFuture =
                PushToApplicationDetector.detectApplicationPaths(allApplications);

        detectionFuture.thenAccept(detectedPaths -> Platform.runLater(() -> {
            detectedApplicationPaths.putAll(detectedPaths);
            applicationsList.setCellFactory(_ -> new PushToApplicationCell(detectedPaths.keySet()));
            List<GuiPushToApplication> sortedApplications = new ArrayList<>(detectedPaths.keySet());
            allApplications.stream()
                           .filter(app -> !detectedPaths.containsKey(app))
                           .forEach(sortedApplications::add);
            applicationsList.getItems().clear();
            applicationsList.getItems().addAll(sortedApplications);
            if (!pushToApplicationPreferences.getActiveApplicationName().isEmpty()) {
                sortedApplications.stream()
                                  .filter(app -> app.getDisplayName().equals(pushToApplicationPreferences.getActiveApplicationName()))
                                  .findFirst()
                                  .ifPresent(applicationsList.getSelectionModel()::select);
            }
            LOGGER.info("Application detection completed. Found {} applications", detectedPaths.size());
        })).exceptionally(throwable -> {
            LOGGER.warn("Application detection failed", throwable);
            return null;
        });

        Optional<ButtonType> result = QuickSettingsDialog
                .create()
                .title("Configure push to applications")
                .header("Select your text editor or LaTeX application for pushing citations")
                .validate(() -> validateDialogSubmission(applicationsList, pathSelector))
                .depend(List.of(pathSelector.getTextField().textProperty()))
                .content(pushApplicationHeader, applicationsList, pathSelector)
                .show();

        if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
            detectionFuture.cancel(true);
            return;
        }

        PushToApplication selectedApp = applicationsList.getSelectionModel().getSelectedItem();
        pushToApplicationPreferences.setActiveApplicationName(selectedApp.getDisplayName());
        Map<String, String> commandPaths = new HashMap<>(pushToApplicationPreferences.getCommandPaths());
        commandPaths.put(selectedApp.getDisplayName(), pathSelector.getText().trim());
        pushToApplicationPreferences.setCommandPaths(commandPaths);
    }

    private void showOnlineServicesConfigurationDialog() {
        HBox onlineServicesHeader = QuickSettingsDialog.createHeaderWithHelp(
                "Configure online databases and services for importing entries. Enable web search, update checking, and metadata extraction services.",
                URLs.ONLINE_SERVICES_DOC
        );

        CheckBox versionCheckBox = new CheckBox(Localization.lang("Check for updates at startup"));
        versionCheckBox.setSelected(preferences.getInternalPreferences().isVersionCheckEnabled());

        CheckBox webSearchBox = new CheckBox(Localization.lang("Enable web search"));
        webSearchBox.setSelected(preferences.getImporterPreferences().areImporterEnabled());

        CheckBox grobidCheckBox = new CheckBox(Localization.lang("Enable Grobid (metadata extraction service)"));
        grobidCheckBox.setSelected(preferences.getGrobidPreferences().isGrobidEnabled());

        HBox grobidUrl = new HBox();
        Label grobidUrlLabel = new Label(Localization.lang("Service URL"));
        TextField grobidUrlField = new TextField(preferences.getGrobidPreferences().getGrobidURL());
        HBox.setHgrow(grobidUrlField, Priority.ALWAYS);
        grobidUrl.getChildren().addAll(
                grobidUrlLabel,
                grobidUrlField,
                new HelpButton(URLs.GROBID_DOC)
        );
        grobidUrl.visibleProperty().bind(grobidCheckBox.selectedProperty());
        grobidUrl.managedProperty().bind(grobidCheckBox.selectedProperty());

        Label fetchersLabel = new Label(Localization.lang("Web search databases"));
        HBox fetchersHeader = new HBox();
        fetchersHeader.getChildren().addAll(
                fetchersLabel,
                new HelpButton(URLs.ONLINE_SERVICES_DOC)
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

        Optional<ButtonType> result = QuickSettingsDialog
                .create()
                .title("Configure web search services")
                .header("Enable and configure online databases and services for importing entries")
                .content(
                        onlineServicesHeader,
                        versionCheckBox,
                        webSearchBox,
                        grobidCheckBox,
                        grobidUrl,
                        fetchersHeader,
                        fetchersScrollPane
                )
                .show();

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
        HBox entryTableHeader = QuickSettingsDialog.createHeaderWithHelp(
                "Configure which columns are displayed in the entry table. The citation key column can be toggled to show or hide reference keys.",
                URLs.ENTRY_TABLE_COLUMNS_DOC
        );

        CheckBox showCitationKeyBox = new CheckBox(Localization.lang("Show citation key column"));
        ColumnPreferences columnPreferences = preferences.getMainTablePreferences()
                                                         .getColumnPreferences();
        boolean isCitationKeyVisible = columnPreferences
                .getColumns()
                .stream()
                .anyMatch(column -> column.getType() == MainTableColumnModel.Type.NORMALFIELD
                        && InternalField.KEY_FIELD.getName().equals(column.getQualifier()));
        showCitationKeyBox.setSelected(isCitationKeyVisible);

        Optional<ButtonType> result = QuickSettingsDialog
                .create()
                .title("Customize entry table")
                .header("Configure which columns are displayed in the entry table")
                .content(entryTableHeader, showCitationKeyBox)
                .show();

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

    private boolean validateDialogSubmission(ListView<GuiPushToApplication> applicationsList, PathSelectionField pathSelector) {
        PushToApplication selectedApp = applicationsList.getSelectionModel().getSelectedItem();
        if (selectedApp == null) {
            return false;
        }
        String pathText = pathSelector.getText().trim();
        Path path = Path.of(pathText);
        return !pathText.isEmpty() && path.isAbsolute() && path.toFile().exists();
    }
}
