package org.jabref.gui.libraryproperties;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

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
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.PreferencesService;

public class LibraryPropertiesDialogViewModel {

    private final StringProperty generalFileDirectoryProperty = new SimpleStringProperty("");
    private final StringProperty userSpecificFileDirectoryProperty = new SimpleStringProperty("");
    private final StringProperty laTexFileDirectoryProperty = new SimpleStringProperty("");
    private final ListProperty<Charset> encodingsProperty = new SimpleListProperty<>(FXCollections.observableArrayList(Encodings.getCharsets()));
    private final ObjectProperty<Charset> selectedEncodingPropety = new SimpleObjectProperty<>(Encodings.getCharsets().get(0));
    private final SimpleStringProperty selectedDatabaseModeProperty = new SimpleStringProperty(BibDatabaseMode.BIBLATEX.getFormattedName());
    private final BooleanProperty libraryProtectedProperty = new SimpleBooleanProperty();
    private final BooleanProperty encodingDisableProperty = new SimpleBooleanProperty();
    private final BooleanProperty protectDisableProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final DirectoryDialogConfiguration directoryDialogConfiguration;

    private final String oldUserSpecificFileDir;
    private final String oldGeneralFileDir;
    private final String oldLaTexFileDir;
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
        selectedDatabaseModeProperty.setValue(metaData.getMode().orElse(BibDatabaseMode.BIBLATEX).getFormattedName());

        Optional<String> fileD = metaData.getDefaultFileDirectory();
        fileD.ifPresent(path -> generalFileDirectoryProperty.setValue(path.trim()));

        Optional<String> fileDI = metaData.getUserFileDirectory(preferencesService.getUser());
        fileDI.ifPresent(userSpecificFileDirectoryProperty::setValue);

        metaData.getLaTexFileDirectory(preferencesService.getUser()).map(Path::toString).ifPresent(laTexFileDirectoryProperty::setValue);

        oldUserSpecificFileDir = generalFileDirectoryProperty.getValue();
        oldGeneralFileDir = userSpecificFileDirectoryProperty.getValue();
        oldLaTexFileDir = laTexFileDirectoryProperty.getValue();

        libraryProtectedProperty.setValue(metaData.isProtected());
        oldLibraryProtected = libraryProtectedProperty.getValue();
    }

    public StringProperty generalFileDirectoryPropertyProperty() {
        return this.generalFileDirectoryProperty;
    }

    public StringProperty userSpecificFileDirectoryProperty() {
        return this.userSpecificFileDirectoryProperty;
    }

    public StringProperty laTexFileDirectoryProperty() {
        return this.laTexFileDirectoryProperty;
    }

    public ListProperty<Charset> encodingsProperty() {
        return this.encodingsProperty;
    }

    public ListProperty<String> databaseModesProperty() {
        return new SimpleListProperty<>(FXCollections.observableArrayList(
                Arrays.stream(BibDatabaseMode.values())
                        .map(BibDatabaseMode::getFormattedName)
                        .collect(Collectors.toList())
        ));
    }

    public SimpleStringProperty selectedDatabaseModeProperty() {
        return this.selectedDatabaseModeProperty;
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

    public void browseLaTexDir() {
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration).ifPresent(dir -> laTexFileDirectoryProperty.setValue(dir.toAbsolutePath().toString()));
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

    public boolean laTexFileDirChanged() {
        return !oldLaTexFileDir.equals(laTexFileDirectoryProperty.getValue());
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
