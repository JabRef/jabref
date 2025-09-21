package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.icore.ConferenceRepository;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class ICORERankingEditor extends HBox implements FieldEditorFX {
    @FXML private ICORERankingEditorViewModel viewModel;
    @FXML private EditorTextField textField;
    @FXML private Button lookupICORERankButton;
    @FXML private Button visitICOREConferencePageButton;

    @Inject private DialogService dialogService;
    @Inject private UndoManager undoManager;
    @Inject private GuiPreferences preferences;
    @Inject private ConferenceRepository conferenceRepository;

    private Optional<BibEntry> entry = Optional.empty();

    public ICORERankingEditor(Field field,
                              SuggestionProvider<?> suggestionProvider,
                              FieldCheckers fieldCheckers) {

        Injector.registerExistingAndInject(this);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new ICORERankingEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                dialogService,
                undoManager,
                preferences,
                conferenceRepository
        );

        textField.textProperty().bindBidirectional(viewModel.textProperty());

        lookupICORERankButton.setTooltip(
                new Tooltip(Localization.lang("Look up conference rank"))
        );
        visitICOREConferencePageButton.setTooltip(
                new Tooltip(Localization.lang("Visit ICORE conference page"))
        );
        visitICOREConferencePageButton.disableProperty().bind(textField.textProperty().isEmpty());
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        this.entry = Optional.of(entry);
        viewModel.bindToEntry(entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }

    @FXML
    private void lookupRank() {
        entry.ifPresent(viewModel::lookupIdentifier);
    }

    @FXML
    private void openExternalLink() {
        viewModel.openExternalLink();
    }
}

