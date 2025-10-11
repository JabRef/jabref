package org.jabref.gui.welcome.components;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.welcome.quicksettings.EntryTableConfigurationDialog;
import org.jabref.gui.welcome.quicksettings.LargeLibraryOptimizationDialog;
import org.jabref.gui.welcome.quicksettings.MainFileDirectoryDialog;
import org.jabref.gui.welcome.quicksettings.OnlineServicesDialog;
import org.jabref.gui.welcome.quicksettings.PushApplicationDialog;
import org.jabref.gui.welcome.quicksettings.ThemeDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;

public class QuickSettings extends VBox {
    private final GuiPreferences preferences;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    private final Label header;
    private boolean isScrollEnabled = true;

    public QuickSettings(GuiPreferences preferences, DialogService dialogService, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        getStyleClass().add("welcome-section");

        header = new Label(Localization.lang("Quick settings"));
        header.getStyleClass().add("welcome-header-label");
        enableScroll();
    }

    public void enableScroll() {
        if (isScrollEnabled) {
            return;
        }
        isScrollEnabled = true;
        getChildren().clear();
        getChildren().addAll(header, createScrollPane(createContent()));
    }

    public void disableScroll() {
        if (!isScrollEnabled) {
            return;
        }
        isScrollEnabled = false;
        getChildren().clear();
        getChildren().addAll(header, createContent());
    }

    private VBox createContent() {
        Button mainFileDirButton = createButton(
                Localization.lang("Set main file directory"),
                IconTheme.JabRefIcons.FOLDER,
                this::showMainFileDirectoryDialog);

        Button themeButton = createButton(
                Localization.lang("Change visual theme"),
                IconTheme.JabRefIcons.PREFERENCES,
                this::showThemeDialog);

        Button largeLibraryButton = createButton(
                Localization.lang("Optimize for large libraries"),
                IconTheme.JabRefIcons.SELECTORS,
                this::showLargeLibraryOptimizationDialog);

        Button pushApplicationButton = createButton(
                Localization.lang("Configure push to applications"),
                IconTheme.JabRefIcons.APPLICATION_GENERIC,
                this::showPushApplicationConfigurationDialog);

        Button onlineServicesButton = createButton(
                Localization.lang("Configure web search services"),
                IconTheme.JabRefIcons.WWW,
                this::showOnlineServicesConfigurationDialog);

        Button entryTableButton = createButton(
                Localization.lang("Customize entry table"),
                IconTheme.JabRefIcons.TOGGLE_GROUPS,
                this::showEntryTableConfigurationDialog);

        VBox newContent = new VBox(mainFileDirButton,
                themeButton,
                largeLibraryButton,
                entryTableButton,
                pushApplicationButton,
                onlineServicesButton);
        newContent.getStyleClass().add("quick-settings-container");
        return newContent;
    }

    private ScrollPane createScrollPane(VBox contentPane) {
        ScrollPane scrollPane = new ScrollPane(contentPane);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("quick-settings-scroll-pane");
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return scrollPane;
    }

    private Button createButton(String text, IconTheme.JabRefIcons icon, Runnable action) {
        Button button = new Button(text);
        button.setGraphic(icon.getGraphicNode());
        button.getStyleClass().add("quick-settings-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> action.run());
        return button;
    }

    private void showMainFileDirectoryDialog() {
        dialogService.showCustomDialogAndWait(new MainFileDirectoryDialog(preferences, dialogService));
    }

    private void showThemeDialog() {
        dialogService.showCustomDialogAndWait(new ThemeDialog(preferences, dialogService));
    }

    private void showLargeLibraryOptimizationDialog() {
        dialogService.showCustomDialogAndWait(new LargeLibraryOptimizationDialog(preferences));
    }

    private void showPushApplicationConfigurationDialog() {
        dialogService.showCustomDialogAndWait(new PushApplicationDialog(preferences, dialogService, taskExecutor));
    }

    private void showOnlineServicesConfigurationDialog() {
        dialogService.showCustomDialogAndWait(new OnlineServicesDialog(preferences));
    }

    private void showEntryTableConfigurationDialog() {
        dialogService.showCustomDialogAndWait(new EntryTableConfigurationDialog(preferences));
    }
}
