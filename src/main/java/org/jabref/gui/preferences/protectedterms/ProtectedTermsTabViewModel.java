package org.jabref.gui.preferences.protectedterms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.protectedterms.ProtectedTermsList;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.OptionalUtil;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtectedTermsTabViewModel implements PreferenceTabViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtectedTermsTabViewModel.class);

    private final ProtectedTermsLoader termsLoader;
    private final ListProperty<ProtectedTermsList> termsFilesProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final PreferencesService preferences;
    private final DialogService dialogService;

    public ProtectedTermsTabViewModel(ProtectedTermsLoader termsLoader,
                                      DialogService dialogService,
                                      PreferencesService preferences) {
        this.termsLoader = termsLoader;
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    @Override
    public void setValues() {
        termsFilesProperty.setValue(FXCollections.observableArrayList(termsLoader.getProtectedTermsLists()));
    }

    public void storeSettings() {
        List<String> enabledExternalList = new ArrayList<>();
        List<String> disabledExternalList = new ArrayList<>();
        List<String> enabledInternalList = new ArrayList<>();
        List<String> disabledInternalList = new ArrayList<>();

        for (ProtectedTermsList list : termsFilesProperty.getValue()) {
            if (list.isInternalList()) {
                if (list.isEnabled()) {
                    enabledInternalList.add(list.getLocation());
                } else {
                    disabledInternalList.add(list.getLocation());
                }
            } else {
                if (list.isEnabled()) {
                    enabledExternalList.add(list.getLocation());
                } else {
                    disabledExternalList.add(list.getLocation());
                }
            }
        }

        ProtectedTermsPreferences newPreferences = new ProtectedTermsPreferences(
                enabledInternalList,
                enabledExternalList,
                disabledInternalList,
                disabledExternalList);

        preferences.storeProtectedTermsPreferences(newPreferences);
        termsLoader.update(newPreferences);
    }

    public void addFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(Localization.lang("Protected terms file"), StandardFileType.TERMS)
                .withDefaultExtension(Localization.lang("Protected terms file"), StandardFileType.TERMS)
                .withInitialDirectory(preferences.getWorkingDir())
                .build();

        dialogService.showFileOpenDialog(fileDialogConfiguration)
                     .ifPresent(file -> {
                         String fileName = file.toAbsolutePath().toString();
                         try {
                             termsFilesProperty.add(ProtectedTermsLoader.readProtectedTermsListFromFile(new File(fileName), true));
                         } catch (FileNotFoundException e) {
                             LOGGER.warn("Cannot find protected terms file " + fileName, e);
                         }
                     });
    }

    public void removeFile(ProtectedTermsList list) {
        if (!list.isInternalList() && dialogService.showConfirmationDialogAndWait(Localization.lang("Remove protected terms file"),
                Localization.lang("Are you sure you want to remove the protected terms file?"),
                Localization.lang("Remove protected terms file"),
                Localization.lang("Cancel"))) {

            list.setEnabled(false);
            if (!termsFilesProperty.remove(list)) {
                LOGGER.info("Problem removing protected terms file");
            }
        }
    }

    public void createNewFile() {
        dialogService.showCustomDialogAndWait(new NewProtectedTermsFileDialog(termsFilesProperty, dialogService));
    }

    public void edit(ProtectedTermsList file) {
        Optional<ExternalFileType> termsFileType = OptionalUtil.<ExternalFileType>orElse(
                ExternalFileTypes.getInstance().getExternalFileTypeByExt("terms"),
                ExternalFileTypes.getInstance().getExternalFileTypeByExt("txt")
        );

        String fileName = file.getLocation();
        try {
            JabRefDesktop.openExternalFileAnyFormat(new BibDatabaseContext(), fileName, termsFileType);
        } catch (IOException e) {
            LOGGER.warn("Problem open protected terms file editor", e);
        }
    }

    public void displayContent(ProtectedTermsList list) {
        dialogService.showInformationDialogAndWait(
                list.getDescription() + " - " + list.getLocation(),
                list.getTermListing()
        );
    }

    public void reloadFile(ProtectedTermsList oldList) {
        try {
            ProtectedTermsList newList = ProtectedTermsLoader.readProtectedTermsListFromFile(new File(oldList.getLocation()), oldList.isEnabled());
            int index = termsFilesProperty.indexOf(oldList);
            if (index >= 0) {
                termsFilesProperty.set(index, newList);
            } else {
                LOGGER.warn("Problem reloading protected terms file {}.", oldList.getLocation());
            }
        } catch (IOException e) {
            LOGGER.warn("Problem reloading protected terms file {}.", oldList.getLocation(), e);
        }
    }

    public ListProperty<ProtectedTermsList> termsFilesProperty() {
        return termsFilesProperty;
    }
}
