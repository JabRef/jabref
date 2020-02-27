package org.jabref.gui.preferences;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import org.jabref.JabRefException;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.ThemeLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.preferences.JabRefPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportThemeDialog extends BaseDialog<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportThemeDialog.class);

    @FXML private TableView<Theme> table;
    @FXML private TableColumn<Theme, String> columnName;
    @FXML private TableColumn<Theme, String> columnPath;

    private JabRefPreferences preferences;
    private DialogService dialogService;

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
    private void initialize() {
        columnName.setCellValueFactory(new PropertyValueFactory<>("name"));
        columnPath.setCellValueFactory(new PropertyValueFactory<>("path"));

        ObservableList<Theme> data =
                FXCollections.observableArrayList(new Theme("Light theme", ThemeLoader.MAIN_CSS), new Theme("Dark theme", ThemeLoader.DARK_CSS));

        if (!(ThemeLoader.CUSTOM_CSS.isBlank() || ThemeLoader.CUSTOM_CSS.isEmpty())) {
            data.add(new Theme("Custom theme", ThemeLoader.CUSTOM_CSS));
        }

        table.setItems(data);

        table.setOnKeyPressed(event -> {
            TablePosition tablePosition;
            if (event.getCode().equals(KeyCode.ENTER)) {
                tablePosition = table.getFocusModel().getFocusedCell();
                final int row = tablePosition.getRow();
                ObservableList<Theme> list = table.getItems();
                Theme theme = list.get(row);
                exportCSSFile(theme.getPath());
            }
        });

        table.setRowFactory(tv -> {
            TableRow<Theme> row = new TableRow<>();
            row.setOnMouseClicked(event -> handleSelectedRowEvent(row));
            return row;
        });
    }

    private void handleSelectedRowEvent(TableRow<Theme> row) {
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
                         try {
                             preferences.exportTheme(exportFile.getFileName(), theme);
                         } catch (JabRefException ex) {
                             LOGGER.warn(ex.getMessage(), ex);
                             dialogService.showErrorDialogAndWait(Localization.lang("Export theme"), ex);
                         }
                     });
    }

    public static class Theme {
        private SimpleStringProperty name;
        private SimpleStringProperty path;

        public Theme(String name, String path) {
            this.name = new SimpleStringProperty(name);
            this.path = new SimpleStringProperty(path);
        }

        public String getName() {
            return name.get();
        }

        public String getPath() {
            return path.get();
        }
    }
}
