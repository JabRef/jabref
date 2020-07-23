package org.jabref.gui.preferences;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.ThemeLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportThemeDialog extends BaseDialog<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportThemeDialog.class);

    @FXML private TableView<AppearanceThemeModel> table;
    @FXML private TableColumn<AppearanceThemeModel, String> columnName;
    @FXML private TableColumn<AppearanceThemeModel, String> columnPath;

    private final JabRefPreferences preferences;
    private final DialogService dialogService;

    public ExportThemeDialog(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;

        ViewLoader
                .view(this)
                .load()
                .setAsDialogPane(this);

        this.setTitle(Localization.lang("Export Theme"));
    }

    @FXML
    public void initialize() {
        columnName.setCellValueFactory(new PropertyValueFactory<>("name"));
        columnPath.setCellValueFactory(new PropertyValueFactory<>("path"));

        ObservableList<AppearanceThemeModel> data =
                FXCollections.observableArrayList(new AppearanceThemeModel("Light theme", ThemeLoader.MAIN_CSS), new AppearanceThemeModel("Dark theme", ThemeLoader.DARK_CSS));

        String customTheme = preferences.getTheme();
        if (!(customTheme.equals(ThemeLoader.MAIN_CSS) || customTheme.equals(ThemeLoader.DARK_CSS))) {
            data.add(new AppearanceThemeModel("Custom theme", customTheme));
        }

        table.setItems(data);

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

        table.setRowFactory(tv -> {
            TableRow<AppearanceThemeModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> handleSelectedRowEvent(row));
            return row;
        });
    }

    private void handleSelectedRowEvent(TableRow<AppearanceThemeModel> row) {
        if (!row.isEmpty()) {
            exportCSSFile(row.getItem().getPath());
        }
    }

    private void exportCSSFile(String theme) {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.CSS)
                .withDefaultExtension(StandardFileType.CSS)
                .withInitialDirectory(preferences.setLastPreferencesExportPath())
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(exportFile -> {
                         try (OutputStream os = Files.newOutputStream(exportFile.getFileName())) {
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
