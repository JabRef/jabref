package org.jabref.gui.preferences.externalfiletypes;

import java.util.Comparator;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.CustomExternalFileType;
import org.jabref.gui.externalfiletype.EditExternalFileTypeEntryDialog;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;

public class ExternalFileTypesTabViewModel implements PreferenceTabViewModel {

    private final ExternalFileTypes externalFileTypes;
    private final ObservableList<ExternalFileType> fileTypes = FXCollections.observableArrayList();

    public ExternalFileTypesTabViewModel(ExternalFileTypes externalFileTypes) {
        this.externalFileTypes = externalFileTypes;
    }

    @Override
    public void setValues() {
        fileTypes.setAll(externalFileTypes.getExternalFileTypeSelection());
        fileTypes.sort(Comparator.comparing(ExternalFileType::getName));
    }

    public void storeSettings() {
        externalFileTypes.setExternalFileTypes(fileTypes);
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
