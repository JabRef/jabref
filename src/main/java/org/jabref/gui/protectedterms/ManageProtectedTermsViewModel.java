package org.jabref.gui.protectedterms;

import java.io.IOException;
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
        preferences.setProtectedTermsPreferences(termsLoader);
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
        NewProtectedTermsFileDialog newDialog = new NewProtectedTermsFileDialog(termsLoader, dialogService);
        newDialog.showAndWait();
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
