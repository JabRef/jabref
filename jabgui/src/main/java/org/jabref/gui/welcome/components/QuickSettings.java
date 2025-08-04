package org.jabref.gui.welcome.components;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.welcome.quicksettings.EntryTableConfigurationDialog;
import org.jabref.gui.welcome.quicksettings.LargeLibraryOptimizationDialog;
import org.jabref.gui.welcome.quicksettings.MainFileDirectoryDialog;
import org.jabref.gui.welcome.quicksettings.OnlineServicesDialog;
import org.jabref.gui.welcome.quicksettings.PushApplicationDialog;
import org.jabref.gui.welcome.quicksettings.ThemeDialog;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class QuickSettings extends VBox {
    @Inject private GuiPreferences preferences;
    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;
    @Inject private ThemeManager themeManager;

    public QuickSettings() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void showMainFileDirectoryDialog() {
        dialogService.showCustomDialogAndWait(new MainFileDirectoryDialog(preferences, dialogService, themeManager));
    }

    @FXML
    private void showThemeDialog() {
        dialogService.showCustomDialogAndWait(new ThemeDialog(preferences, dialogService, themeManager));
    }

    @FXML
    private void showLargeLibraryOptimizationDialog() {
        dialogService.showCustomDialogAndWait(new LargeLibraryOptimizationDialog(preferences, themeManager));
    }

    @FXML
    private void showPushApplicationConfigurationDialog() {
        dialogService.showCustomDialogAndWait(new PushApplicationDialog(preferences, dialogService, taskExecutor, themeManager));
    }

    @FXML
    private void showOnlineServicesConfigurationDialog() {
        dialogService.showCustomDialogAndWait(new OnlineServicesDialog(preferences, themeManager));
    }

    @FXML
    private void showEntryTableConfigurationDialog() {
        dialogService.showCustomDialogAndWait(new EntryTableConfigurationDialog(preferences, themeManager));
    }
}
