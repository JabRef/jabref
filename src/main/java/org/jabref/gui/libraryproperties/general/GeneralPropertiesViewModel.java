package org.jabref.gui.libraryproperties.general;

import java.nio.charset.Charset;
import java.nio.file.Path;

import javax.swing.undo.UndoManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.libraryproperties.PropertiesTabViewModel;
import org.jabref.gui.undo.UndoablePreambleChange;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Encodings;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.PreferencesService;

public class GeneralPropertiesViewModel implements PropertiesTabViewModel {

    private final BooleanProperty encodingDisableProperty = new SimpleBooleanProperty();
    private final ListProperty<Charset> encodingsProperty = new SimpleListProperty<>(FXCollections.observableArrayList(Encodings.getCharsets()));
    private final ObjectProperty<Charset> selectedEncodingProperty = new SimpleObjectProperty<>(Encodings.getCharsets().get(0));
    private final ListProperty<BibDatabaseMode> databaseModesProperty = new SimpleListProperty<>(FXCollections.observableArrayList(BibDatabaseMode.values()));
    private final SimpleObjectProperty<BibDatabaseMode> selectedDatabaseModeProperty = new SimpleObjectProperty<>(BibDatabaseMode.BIBLATEX);
    private final StringProperty generalFileDirectoryProperty = new SimpleStringProperty("");
    private final StringProperty userSpecificFileDirectoryProperty = new SimpleStringProperty("");
    private final StringProperty laTexFileDirectoryProperty = new SimpleStringProperty("");
    private final StringProperty preambleProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final PreferencesService preferencesService;

    private final BibDatabaseContext databaseContext;
    private final MetaData metaData;
    private final DirectoryDialogConfiguration directoryDialogConfiguration;
    private final UndoManager undoManager;

    GeneralPropertiesViewModel(BibDatabaseContext databaseContext, DialogService dialogService, PreferencesService preferencesService, UndoManager undoManager) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.undoManager = undoManager;
        this.databaseContext = databaseContext;
        this.metaData = databaseContext.getMetaData();

        this.directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(preferencesService.getFilePreferences().getWorkingDirectory()).build();
    }

    @Override
    public void setValues() {
        boolean isShared = (databaseContext.getLocation() == DatabaseLocation.SHARED);
        encodingDisableProperty.setValue(isShared); // the encoding of shared database is always UTF-8

        selectedEncodingProperty.setValue(metaData.getEncoding().orElse(preferencesService.getGeneralPreferences().getDefaultEncoding()));
        selectedDatabaseModeProperty.setValue(metaData.getMode().orElse(BibDatabaseMode.BIBLATEX));
        generalFileDirectoryProperty.setValue(metaData.getDefaultFileDirectory().orElse("").trim());
        userSpecificFileDirectoryProperty.setValue(metaData.getUserFileDirectory(preferencesService.getFilePreferences().getUser()).orElse("").trim());
        laTexFileDirectoryProperty.setValue(metaData.getLatexFileDirectory(preferencesService.getFilePreferences().getUser()).map(Path::toString).orElse(""));

        preambleProperty.setValue(databaseContext.getDatabase().getPreamble().orElse(""));
    }

    @Override
    public void storeSettings() {
        MetaData newMetaData = databaseContext.getMetaData();

        newMetaData.setEncoding(selectedEncodingProperty.getValue());
        newMetaData.setMode(selectedDatabaseModeProperty.getValue());

        String generalFileDirectory = generalFileDirectoryProperty.getValue().trim();
        if (generalFileDirectory.isEmpty()) {
            newMetaData.clearDefaultFileDirectory();
        } else {
            newMetaData.setDefaultFileDirectory(generalFileDirectory);
        }

        String userSpecificFileDirectory = userSpecificFileDirectoryProperty.getValue();
        if (userSpecificFileDirectory.isEmpty()) {
            newMetaData.clearUserFileDirectory(preferencesService.getFilePreferences().getUser());
        } else {
            newMetaData.setUserFileDirectory(preferencesService.getFilePreferences().getUser(), userSpecificFileDirectory);
        }

        String latexFileDirectory = laTexFileDirectoryProperty.getValue();
        if (latexFileDirectory.isEmpty()) {
            newMetaData.clearLatexFileDirectory(preferencesService.getFilePreferences().getUser());
        } else {
            newMetaData.setLatexFileDirectory(preferencesService.getFilePreferences().getUser(), Path.of(latexFileDirectory));
        }

        databaseContext.setMetaData(newMetaData);

        String newPreamble = preambleProperty.getValue();
        if (!databaseContext.getDatabase().getPreamble().orElse("").equals(newPreamble)) {
            undoManager.addEdit(new UndoablePreambleChange(databaseContext.getDatabase(), databaseContext.getDatabase().getPreamble().orElse(null), newPreamble));
            databaseContext.getDatabase().setPreamble(newPreamble);
        }
    }

    public void browseGeneralDir() {
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(dir -> generalFileDirectoryProperty.setValue(dir.toAbsolutePath().toString()));
    }

    public void browseUserDir() {
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(dir -> userSpecificFileDirectoryProperty.setValue(dir.toAbsolutePath().toString()));
    }

    public void browseLatexDir() {
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(dir -> laTexFileDirectoryProperty.setValue(dir.toAbsolutePath().toString()));
    }

    public BooleanProperty encodingDisableProperty() {
        return encodingDisableProperty;
    }

    public ListProperty<Charset> encodingsProperty() {
        return this.encodingsProperty;
    }

    public ObjectProperty<Charset> selectedEncodingProperty() {
        return selectedEncodingProperty;
    }

    public ListProperty<BibDatabaseMode> databaseModesProperty() {
        return databaseModesProperty;
    }

    public SimpleObjectProperty<BibDatabaseMode> selectedDatabaseModeProperty() {
        return selectedDatabaseModeProperty;
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

    public StringProperty preambleProperty() {
        return this.preambleProperty;
    }
}
