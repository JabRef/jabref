package org.jabref.gui.externalfiletype;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;

public class CustomizeExternalFileTypesViewModel {

    private final ObservableList<ExternalFileType> fileTypes;

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
        showEditDialog(type, Localization.lang("Add new file type"));
    }

    public ObservableList<ExternalFileType> getFileTypes() {
        return fileTypes;
    }

    private void showEditDialog(ExternalFileType type, String dialogTitle) {
        CustomExternalFileType typeForEdit;
        if (type instanceof CustomExternalFileType) {
            typeForEdit = (CustomExternalFileType) type;
        } else {
            typeForEdit = new CustomExternalFileType(type);
            fileTypes.add(fileTypes.indexOf(type), typeForEdit);
            fileTypes.remove(type);
        }
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService.showCustomDialogAndWait(new EditExternalFileTypeEntryDialog(typeForEdit, dialogTitle));
    }

    public void edit(ExternalFileType type) {
        showEditDialog(type, Localization.lang("Edit file type"));
    }

    public void remove(ExternalFileType type) {
        fileTypes.remove(type);
    }
}
