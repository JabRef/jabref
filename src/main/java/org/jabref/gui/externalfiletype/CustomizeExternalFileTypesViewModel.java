package org.jabref.gui.externalfiletype;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.icon.IconTheme;

public class CustomizeExternalFileTypesViewModel {
    private ObservableList<ExternalFileType> fileTypes;

    public CustomizeExternalFileTypesViewModel() {
        Set<ExternalFileType> types = ExternalFileTypes.getInstance().getExternalFileTypeSelection();
        fileTypes = FXCollections.observableArrayList(types);
        fileTypes.sort(Comparator.comparing(ExternalFileType::getName));
    }

    /**
     * Stores the list of external entry types in the preferences.
     */
    public void storeSettings() {
        ExternalFileTypes.getInstance().setExternalFileTypes(fileTypes);
    }

    public void resetToDefaults() {
        List<ExternalFileType> list = ExternalFileTypes.getDefaultExternalFileTypes();
        fileTypes.setAll(list);
        fileTypes.sort(Comparator.comparing(ExternalFileType::getName));
    }

    public void addNewType() {
        CustomExternalFileType type = new CustomExternalFileType("", "", "", "", "new", IconTheme.JabRefIcons.FILE);
        fileTypes.add(type);
        edit(type);
    }

    public ObservableList<ExternalFileType> getFileTypes() {
        return fileTypes;
    }

    public void edit(ExternalFileType type) {
        CustomExternalFileType typeForEdit;
        if (type instanceof CustomExternalFileType) {
            typeForEdit = (CustomExternalFileType) type;
        } else {
            typeForEdit = new CustomExternalFileType(type);
        }

        ExternalFileTypeEntryEditor entryEditor = new ExternalFileTypeEntryEditor(typeForEdit);
        entryEditor.setVisible(true);
    }

    public void remove(ExternalFileType type) {
        fileTypes.remove(type);
    }
}
