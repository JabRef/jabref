package org.jabref.gui.preferences.externalfiletypes;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.exporter.CreateModifyExporterDialogViewModel;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalFileTypesTabViewModel implements PreferenceTabViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateModifyExporterDialogViewModel.class);
    private final ObservableList<ExternalFileTypeItemViewModel> fileTypes = FXCollections.observableArrayList();

    private final FilePreferences filePreferences;
    private final DialogService dialogService;

    public ExternalFileTypesTabViewModel(FilePreferences filePreferences, DialogService dialogService) {
        this.filePreferences = filePreferences;
        this.dialogService = dialogService;
    }

    @Override
    public void setValues() {
        fileTypes.clear();
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

    public boolean addNewType() {
        ExternalFileTypeItemViewModel item = new ExternalFileTypeItemViewModel();
        showEditDialog(item, Localization.lang("Add new file type"));

        if (!isValidExternalFileType(item)) {
            return false;
        }

        fileTypes.add(item);
        return true;
    }

    public ObservableList<ExternalFileTypeItemViewModel> getFileTypes() {
        return fileTypes;
    }

    protected void showEditDialog(ExternalFileTypeItemViewModel item, String dialogTitle) {
        dialogService.showCustomDialogAndWait(new EditExternalFileTypeEntryDialog(item, dialogTitle, fileTypes));
    }

    public boolean edit(ExternalFileTypeItemViewModel type) {
        ExternalFileTypeItemViewModel typeToModify = new ExternalFileTypeItemViewModel(type.toExternalFileType());
        showEditDialog(typeToModify, Localization.lang("Edit file type"));

        if (!isValidExternalFileType(typeToModify)) {
            return false;
        }

        fileTypes.remove(type);
        fileTypes.add(typeToModify);
        return true;
    }

    public void remove(ExternalFileTypeItemViewModel type) {
        fileTypes.remove(type);
    }

    public boolean isValidExternalFileType(ExternalFileTypeItemViewModel item) {
        if (withEmptyValue(item)) {
            LOGGER.debug("One of the fields is empty or invalid.");
            return false;
        }

        if (!isUniqueExtension(item)) {
            LOGGER.debug("File Extension exists already.");
            return false;
        }

        return true;
    }

    private boolean withEmptyValue(ExternalFileTypeItemViewModel item) {
        return item.getName().isEmpty() || item.extensionProperty().get().isEmpty() || item.mimetypeProperty().get().isEmpty();
    }

    private boolean isUniqueExtension(ExternalFileTypeItemViewModel item) {
        // check extension need to be unique in the list
        String newExt = item.extensionProperty().get();
        for (ExternalFileTypeItemViewModel fileTypeItem : fileTypes) {
            if (newExt.equalsIgnoreCase(fileTypeItem.extensionProperty().get())) {
                return false;
            }
        }
        return true;
    }
}
