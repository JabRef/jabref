package org.jabref.gui.preferences;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.ThemeLoader;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportThemeDialog extends BaseDialog<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportThemeDialog.class);

    @FXML private TableView<AppearanceThemeModel> table;
    @FXML private TableColumn<AppearanceThemeModel, String> columnName;
    @FXML private TableColumn<AppearanceThemeModel, String> columnPath;

    private final PreferencesService preferences;
    private final DialogService dialogService;

    public ExportThemeDialog(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.setTitle(Localization.lang("Export theme"));
    }

    @FXML
    public void initialize() {
        columnName.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getName()));
        columnPath.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getPath()));

        ObservableList<AppearanceThemeModel> themesList = FXCollections.observableArrayList(
                new AppearanceThemeModel(Localization.lang("Light theme"), ThemeLoader.MAIN_CSS),
                new AppearanceThemeModel(Localization.lang("Dark theme"), ThemeLoader.DARK_CSS));

        String customTheme = preferences.getTheme();
        if (!(customTheme.equals(ThemeLoader.MAIN_CSS) || customTheme.equals(ThemeLoader.DARK_CSS))) {
            themesList.add(new AppearanceThemeModel(Localization.lang("Custom theme"), customTheme));
        }

        table.setItems(themesList);

        table.setOnKeyPressed(event -> {
            TablePosition<?, ?> tablePosition;
            if (event.getCode().equals(KeyCode.ENTER)) {
                tablePosition = table.getFocusModel().getFocusedCell();
                final int row = tablePosition.getRow();
                ObservableList<AppearanceThemeModel> list = table.getItems();
                AppearanceThemeModel appearanceThemeModel = list.get(row);
                exportCSSFile(appearanceThemeModel.getPath());
            }
        });

        new ViewModelTableRowFactory<AppearanceThemeModel>()
                .withOnMouseClickedEvent((theme, event) -> {
                    if (theme != null && event.getButton().equals(MouseButton.PRIMARY)) {
                        exportCSSFile(theme.getPath());
                    }
                })
                .install(table);
    }

    private void exportCSSFile(String theme) {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.CSS)
                .withDefaultExtension(StandardFileType.CSS)
                .withInitialDirectory(preferences.setLastPreferencesExportPath())
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(exportFile -> {
                         try (OutputStream os = Files.newOutputStream(exportFile)) {
                             if (theme.equals(ThemeLoader.MAIN_CSS) || theme.equals(ThemeLoader.DARK_CSS)) {
                                 Path path = new File(JabRefFrame.class.getResource(theme).toURI()).toPath();
                                 Files.copy(path, os);
                             } else {
                                 Path path = new File(theme).toPath();
                                 Files.copy(path, os);
                             }
                         } catch (IOException | URISyntaxException ex) {
                             LOGGER.warn(ex.getMessage(), ex);
                             dialogService.showErrorDialogAndWait(Localization.lang("Export theme"), ex);
                         }
                     });
    }
}
