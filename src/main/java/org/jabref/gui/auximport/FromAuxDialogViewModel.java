package org.jabref.gui.auximport;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.auxparser.AuxParser;
import org.jabref.logic.auxparser.AuxParserResult;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class FromAuxDialogViewModel {

    private final BooleanProperty parseFailedProperty = new SimpleBooleanProperty(false);
    private final StringProperty auxFileProperty = new SimpleStringProperty();
    private final StringProperty statusTextProperty = new SimpleStringProperty();
    private final ListProperty<String> notFoundList = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<BibDatabaseContext> librariesProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<BibDatabaseContext> selectedLibraryProperty = new SimpleObjectProperty<>();

    private final LibraryTabContainer tabContainer;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;

    private AuxParserResult auxParserResult;

    public FromAuxDialogViewModel(LibraryTabContainer tabContainer,
                                  DialogService dialogService,
                                  PreferencesService preferencesService,
                                  StateManager stateManager) {
        this.tabContainer = tabContainer;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;

        librariesProperty.setAll(stateManager.getOpenDatabases());
        selectedLibraryProperty.set(tabContainer.getCurrentLibraryTab().getBibDatabaseContext());
        EasyBind.listen(selectedLibraryProperty, (obs, oldValue, newValue) -> {
            if (auxParserResult != null) {
                parse();
            }
        });
    }

    public String getDatabaseName(BibDatabaseContext databaseContext) {
        Optional<String> dbOpt = Optional.empty();
        if (databaseContext.getDatabasePath().isPresent()) {
            dbOpt = FileUtil.getUniquePathFragment(stateManager.collectAllDatabasePaths(), databaseContext.getDatabasePath().get());
        }
        if (databaseContext.getLocation() == DatabaseLocation.SHARED) {
            return databaseContext.getDBMSSynchronizer().getDBName() + " [" + Localization.lang("shared") + "]";
        }

        return dbOpt.orElse(Localization.lang("untitled"));
    }

    public void browse() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.AUX)
                .withDefaultExtension(StandardFileType.AUX)
                .withInitialDirectory(preferencesService.getFilePreferences().getWorkingDirectory()).build();
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> auxFileProperty.setValue(file.toAbsolutePath().toString()));
    }

    public void parse() {
        parseFailedProperty.set(false);
        notFoundList.clear();
        statusTextProperty.setValue("");
        BibDatabase referenceDatabase = selectedLibraryProperty.get().getDatabase();
        String auxName = auxFileProperty.get();

        if ((auxName != null) && (referenceDatabase != null) && !auxName.isEmpty()) {
            AuxParser auxParser = new DefaultAuxParser(referenceDatabase);
            auxParserResult = auxParser.parse(Path.of(auxName));
            notFoundList.setAll(auxParserResult.getUnresolvedKeys());
            statusTextProperty.set(new AuxParserResultViewModel(auxParserResult).getInformation(false));

            if (!auxParserResult.getGeneratedBibDatabase().hasEntries()) {
                // The generated database contains no entries -> no active generate-button
                statusTextProperty.set(statusTextProperty.get() + "\n" + Localization.lang("empty library"));
                parseFailedProperty.set(true);
            }
        } else {
            parseFailedProperty.set(true);
        }
    }

    public void addResultToTabContainer() {
        BibDatabaseContext context = new BibDatabaseContext(auxParserResult.getGeneratedBibDatabase());
        tabContainer.addTab(context, true);
    }

    public BooleanProperty parseFailedProperty() {
        return parseFailedProperty;
    }

    public StringProperty auxFileProperty() {
        return auxFileProperty;
    }

    public StringProperty statusTextProperty() {
        return statusTextProperty;
    }

    public ReadOnlyListProperty<String> notFoundList() {
        return notFoundList;
    }

    public ReadOnlyListProperty<BibDatabaseContext> librariesProperty() {
        return librariesProperty;
    }

    public ObjectProperty<BibDatabaseContext> selectedLibraryProperty() {
        return selectedLibraryProperty;
    }
}
