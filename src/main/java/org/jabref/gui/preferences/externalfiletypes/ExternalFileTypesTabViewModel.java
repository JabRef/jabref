package org.jabref.gui.preferences.externalfiletypes;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.FilePreferences;

public class ExternalFileTypesTabViewModel implements PreferenceTabViewModel {

    private final ObservableList<ExternalFileTypeItemViewModel> fileTypes = FXCollections.observableArrayList();

    private final FilePreferences filePreferences;
    private final DialogService dialogService;

    public ExternalFileTypesTabViewModel(FilePreferences filePreferences, DialogService dialogService) {
        this.filePreferences = filePreferences;
        this.dialogService = dialogService;
    }

    @Override
    public void setValues() {
        fileTypes.addAll(filePreferences.getExternalFileTypes().stream()
                       .map(ExternalFileTypeItemViewModel::new)
                       .toList());
        fileTypes.sort(Comparator.comparing(ExternalFileTypeItemViewModel::getName));
    }

    public void storeSettings() {
        Set<ExternalFileType> saveList = new HashSet<>();

        fileTypes.stream().map(ExternalFileTypeItemViewModel::toExternalFileType)
                 .forEach(type -> ExternalFileTypes.getDefaultExternalFileTypes().stream()
                                                   .filter(type::equals).findAny()
                                                   .ifPresentOrElse(saveList::add, () -> saveList.add(type)));

        filePreferences.getExternalFileTypes().clear();
        filePreferences.getExternalFileTypes().addAll(saveList);
    }

    public void resetToDefaults() {
        fileTypes.setAll(ExternalFileTypes.getDefaultExternalFileTypes().stream()
                                          .map(ExternalFileTypeItemViewModel::new)
                                          .toList());
        fileTypes.sort(Comparator.comparing(ExternalFileTypeItemViewModel::getName));
    }

    public void addNewType() {
        ExternalFileTypeItemViewModel item = new ExternalFileTypeItemViewModel();
        fileTypes.add(item);
        showEditDialog(item, Localization.lang("Add new file type"));
    }

    public ObservableList<ExternalFileTypeItemViewModel> getFileTypes() {
        return fileTypes;
    }

    private void showEditDialog(ExternalFileTypeItemViewModel item, String dialogTitle) {
        dialogService.showCustomDialogAndWait(new EditExternalFileTypeEntryDialog(item, dialogTitle));
    }

    public void edit(ExternalFileTypeItemViewModel type) {
        showEditDialog(type, Localization.lang("Edit file type"));
    }

    public void remove(ExternalFileTypeItemViewModel type) {
        fileTypes.remove(type);
    }
}
