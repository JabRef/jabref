package org.jabref.gui.preferences.entry;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class EntryTab extends AbstractPreferenceTabView<EntryTabViewModel> implements PreferencesTab {

    @FXML private ComboBox<BibDatabaseMode> biblatexMode;

    @FXML private TextField keywordSeparator;

    @FXML private CheckBox resolveStrings;
    @FXML private TextField resolveStringsForFields;
    @FXML private TextField nonWrappableFields;

    @FXML private CheckBox markOwner;
    @FXML private TextField markOwnerName;
    @FXML private CheckBox markOwnerOverwrite;
    @FXML private Button markOwnerHelp;

    @FXML private CheckBox addCreationDate;
    @FXML private CheckBox addModificationDate;

    @Inject private KeyBindingRepository keyBindingRepository;

    public EntryTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new EntryTabViewModel(preferencesService);

        new ViewModelListCellFactory<BibDatabaseMode>()
                .withText(BibDatabaseMode::getFormattedName)
                .install(biblatexMode);
        biblatexMode.itemsProperty().bind(viewModel.biblatexModeListProperty());
        biblatexMode.valueProperty().bindBidirectional(viewModel.selectedBiblatexModeProperty());

        keywordSeparator.textProperty().bindBidirectional(viewModel.keywordSeparatorProperty());

        resolveStrings.selectedProperty().bindBidirectional(viewModel.resolveStringsProperty());
        resolveStringsForFields.textProperty().bindBidirectional(viewModel.resolveStringsForFieldsProperty());
        nonWrappableFields.textProperty().bindBidirectional(viewModel.nonWrappableFieldsProperty());

        markOwner.selectedProperty().bindBidirectional(viewModel.markOwnerProperty());
        markOwnerName.textProperty().bindBidirectional(viewModel.markOwnerNameProperty());
        markOwnerName.disableProperty().bind(markOwner.selectedProperty().not());
        markOwnerOverwrite.selectedProperty().bindBidirectional(viewModel.markOwnerOverwriteProperty());
        markOwnerOverwrite.disableProperty().bind(markOwner.selectedProperty().not());

        addCreationDate.selectedProperty().bindBidirectional(viewModel.addCreationDateProperty());
        addModificationDate.selectedProperty().bindBidirectional(viewModel.addModificationDateProperty());

        ActionFactory actionFactory = new ActionFactory(keyBindingRepository);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.OWNER, dialogService), markOwnerHelp);
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry");
    }
}
