package org.jabref.gui.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
public class ZipFileChooser extends BaseDialog<String> {

    /**
     * New ZIP file chooser.
     *
     * @param zipFile                   ZIP-Fle to choose from, must be readable
     */
    public ZipFileChooser(ZipFile zipFile) {
        setTitle(Localization.lang("Select file from ZIP-archive"));

        TableView<ZipEntry> table = new TableView<>(getSelectableZipEntries(zipFile));
        TableColumn<ZipEntry, String> nameColumn = new TableColumn<>(Localization.lang("Name"));
        TableColumn<ZipEntry, String> modifiedColumn = new TableColumn<>(Localization.lang("Last modified"));
        TableColumn<ZipEntry, Number> sizeColumn = new TableColumn<>(Localization.lang("Size"));
        table.getColumns().add(nameColumn);
        table.getColumns().add(modifiedColumn);
        table.getColumns().add(sizeColumn);
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        modifiedColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                ZonedDateTime.ofInstant(new Date(data.getValue().getTime()).toInstant(),
                        ZoneId.systemDefault())
                             .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))));
        sizeColumn.setCellValueFactory(data -> new ReadOnlyLongWrapper(data.getValue().getSize()));
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        getDialogPane().setContent(table);

        getDialogPane().getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL
        );

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return table.getSelectionModel().getSelectedItem().getName();
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
    private static ObservableList<ZipEntry> getSelectableZipEntries(ZipFile zipFile) {
        ObservableList<ZipEntry> entries = FXCollections.observableArrayList();
        Enumeration<? extends ZipEntry> e = zipFile.entries();
        for (ZipEntry entry : Collections.list(e)) {
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                entries.add(entry);
            }
        }
        return entries;
    }
}
