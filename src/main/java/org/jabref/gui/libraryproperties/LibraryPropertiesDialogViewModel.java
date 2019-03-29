package org.jabref.gui.libraryproperties;

import java.nio.charset.Charset;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Encodings;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.PreferencesService;

public class LibraryPropertiesDialogViewModel {

    private final StringProperty generalFileDirectoryProperty = new SimpleStringProperty("");
    private final StringProperty userSpecificFileDirectoryProperty = new SimpleStringProperty("");
    private final ListProperty<Charset> encodingsProperty = new SimpleListProperty<>(FXCollections.observableArrayList(Encodings.getCharsets()));
    private final ObjectProperty<Charset> selectedEncodingPropety = new SimpleObjectProperty<>(Encodings.getCharsets().get(0));
    private final BooleanProperty libraryProtectedProperty = new SimpleBooleanProperty();
    private final BooleanProperty encodingDisableProperty = new SimpleBooleanProperty();
    private final BooleanProperty protectDisableProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final DirectoryDialogConfiguration directoryDialogConfiguration;

    private final String oldUserSpecificFileDir;
    private final String oldGeneralFileDir;
    private final boolean oldLibraryProtected;

    public LibraryPropertiesDialogViewModel(BasePanel panel, DialogService dialogService, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        MetaData metaData = panel.getBibDatabaseContext().getMetaData();

        DatabaseLocation location = panel.getBibDatabaseContext().getLocation();
        boolean isShared = (location == DatabaseLocation.SHARED);
        encodingDisableProperty.setValue(isShared); // the encoding of shared database is always UTF-8
        protectDisableProperty.setValue(isShared);

        directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                                                                                 .withInitialDirectory(preferencesService.getWorkingDir()).build();

        Optional<Charset> charset = metaData.getEncoding();
        selectedEncodingPropety.setValue(charset.orElse(preferencesService.getDefaultEncoding()));

        Optional<String> fileD = metaData.getDefaultFileDirectory();
        fileD.ifPresent(path -> generalFileDirectoryProperty.setValue(path.trim()));

        Optional<String> fileDI = metaData.getUserFileDirectory(preferencesService.getUser());
        fileDI.ifPresent(userSpecificFileDirectoryProperty::setValue);

        oldUserSpecificFileDir = generalFileDirectoryProperty.getValue();
        oldGeneralFileDir = userSpecificFileDirectoryProperty.getValue();

        libraryProtectedProperty.setValue(metaData.isProtected());
        oldLibraryProtected = libraryProtectedProperty.getValue();
    }

    public StringProperty generalFileDirectoryPropertyProperty() {
        return this.generalFileDirectoryProperty;
    }

    public StringProperty userSpecificFileDirectoryProperty() {
        return this.userSpecificFileDirectoryProperty;
    }

    public ListProperty<Charset> encodingsProperty() {
        return this.encodingsProperty;
    }

    public ObjectProperty<Charset> selectedEncodingProperty() {
        return this.selectedEncodingPropety;
    }

    public void browseGeneralDir() {
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration).ifPresent(dir -> generalFileDirectoryProperty.setValue(dir.toAbsolutePath().toString()));
    }

    public void browseUserDir() {
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration).ifPresent(dir -> userSpecificFileDirectoryProperty.setValue(dir.toAbsolutePath().toString()));
    }

    public BooleanProperty libraryProtectedProperty() {
        return this.libraryProtectedProperty;
    }

    public boolean generalFileDirChanged() {
        return !oldGeneralFileDir.equals(generalFileDirectoryProperty.getValue());
    }

    public boolean userFileDirChanged() {
        return !oldUserSpecificFileDir.equals(userSpecificFileDirectoryProperty.getValue());
    }

    public boolean protectedValueChanged() {
        return !oldLibraryProtected == libraryProtectedProperty.getValue();
    }

    public BooleanProperty encodingDisableProperty() {
        return encodingDisableProperty;
    }

    public BooleanProperty protectDisableProperty() {
        return protectDisableProperty;
    }
}
