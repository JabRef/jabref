package org.jabref.gui.util;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.logic.l10n.Localization;

/**
 * Dialog to allow users to choose a file contained in a ZIP file.
 */
public class ZipFileChooser extends BaseDialog<Path> {

    /**
     * New ZIP file chooser.
     *
     * @param zipFile ZIP-Fle to choose from, must be readable
     */
    public ZipFileChooser(FileSystem zipFile) throws IOException {
        setTitle(Localization.lang("Select file from ZIP-archive"));

        TableView<Path> table = new TableView<>(getSelectableZipEntries(zipFile));
        TableColumn<Path, String> nameColumn = new TableColumn<>(Localization.lang("Name"));
        TableColumn<Path, String> modifiedColumn = new TableColumn<>(Localization.lang("Last modified"));
        TableColumn<Path, Number> sizeColumn = new TableColumn<>(Localization.lang("Size"));
        table.getColumns().add(nameColumn);
        table.getColumns().add(modifiedColumn);
        table.getColumns().add(sizeColumn);
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().toString()));
        modifiedColumn.setCellValueFactory(data -> {
            try {
                return new ReadOnlyStringWrapper(
                        ZonedDateTime.ofInstant(Files.getLastModifiedTime(data.getValue()).toInstant(),
                                ZoneId.systemDefault())
                                     .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
            } catch (IOException e) {
                // Ignore
                return new ReadOnlyStringWrapper("");
            }
        });
        sizeColumn.setCellValueFactory(data -> {
            try {
                return new ReadOnlyLongWrapper(Files.size(data.getValue()));
            } catch (IOException e) {
                // Ignore
                return new ReadOnlyLongWrapper(0);
            }
        });
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        getDialogPane().setContent(table);

        getDialogPane().getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL
        );

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return table.getSelectionModel().getSelectedItem();
            } else {
                return null;
            }
        });
    }

    /**
     * Entries that can be selected with this dialog.
     *
     * @param zipFile ZIP-File
     * @return entries that can be selected
     */
    private static ObservableList<Path> getSelectableZipEntries(FileSystem zipFile) throws IOException {
        Path rootDir = zipFile.getRootDirectories().iterator().next();

        return FXCollections.observableArrayList(
                Files.walk(rootDir)
                     .filter(file -> file.endsWith(".class"))
                     .collect(Collectors.toList()));
    }
}
