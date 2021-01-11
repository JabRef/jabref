package org.jabref.gui.protectedterms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
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

public class ManageProtectedTermsViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManageProtectedTermsViewModel.class);

    private final ProtectedTermsLoader termsLoader;
    private final ObservableList<ProtectedTermsList> termsFiles;
    private final PreferencesService preferences;
    private final DialogService dialogService;

    public ManageProtectedTermsViewModel(ProtectedTermsLoader termsLoader, DialogService dialogService, PreferencesService preferences) {
        this.termsLoader = termsLoader;
        this.dialogService = dialogService;
        this.termsFiles = FXCollections.observableArrayList(termsLoader.getProtectedTermsLists());
        this.preferences = preferences;
    }

    public ObservableList<ProtectedTermsList> getTermsFiles() {
        return termsFiles;
    }

    public void save() {
        List<String> enabledExternalList = new ArrayList<>();
        List<String> disabledExternalList = new ArrayList<>();
        List<String> enabledInternalList = new ArrayList<>();
        List<String> disabledInternalList = new ArrayList<>();

        for (ProtectedTermsList list : termsLoader.getProtectedTermsLists()) {
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

        preferences.storeProtectedTermsPreferences(new ProtectedTermsPreferences(
                enabledInternalList,
                enabledExternalList,
                disabledInternalList,
                disabledExternalList));
    }

    public void addFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(Localization.lang("Protected terms file"), StandardFileType.TERMS)
                .withDefaultExtension(Localization.lang("Protected terms file"), StandardFileType.TERMS)
                .withInitialDirectory(preferences.getWorkingDir())
                .build();

        dialogService.showFileOpenDialog(fileDialogConfiguration)
                     .ifPresent(file -> termsLoader.addProtectedTermsListFromFile(file.toAbsolutePath().toString(), true));
    }

    public void removeFile(ProtectedTermsList list) {
        if (!list.isInternalList() && dialogService.showConfirmationDialogAndWait(Localization.lang("Remove protected terms file"),
                Localization.lang("Are you sure you want to remove the protected terms file?"),
                Localization.lang("Remove protected terms file"),
                Localization.lang("Cancel"))) {
            if (!termsLoader.removeProtectedTermsList(list)) {
                LOGGER.info("Problem removing protected terms file");
            }
        }
    }

    public void createNewFile() {
        dialogService.showCustomDialogAndWait(new NewProtectedTermsFileDialog(termsLoader, dialogService));
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

    public void reloadFile(ProtectedTermsList file) {
        termsLoader.reloadProtectedTermsList(file);
    }
}
