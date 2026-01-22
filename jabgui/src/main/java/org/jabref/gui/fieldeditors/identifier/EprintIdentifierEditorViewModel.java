package org.jabref.gui.fieldeditors.identifier;

import javax.swing.undo.UndoManager;

import javafx.collections.MapChangeListener;
import javafx.collections.WeakMapChangeListener;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ARK;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.EprintIdentifier;

import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EprintIdentifierEditorViewModel extends BaseIdentifierEditorViewModel<EprintIdentifier> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EprintIdentifierEditorViewModel.class);

    // The following listener will be wrapped in a weak reference change listener, thus it will be garbage collected
    // automatically once this object is disposed.
    // https://en.wikipedia.org/wiki/Lapsed_listener_problem
    private MapChangeListener<Field, String> eprintTypeFieldListener = change -> {
        Field changedField = change.getKey();
        if (StandardField.EPRINTTYPE == changedField) {
            updateIdentifier();
        }
    };

    public EprintIdentifierEditorViewModel(SuggestionProvider<?> suggestionProvider,
                                           FieldCheckers fieldCheckers,
                                           DialogService dialogService,
                                           TaskExecutor taskExecutor,
                                           GuiPreferences preferences,
                                           UndoManager undoManager,
                                           StateManager stateManager) {
        super(StandardField.EPRINT, suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences, undoManager, stateManager);
        EasyBind.subscribe(identifier, newIdentifier -> newIdentifier.ifPresent(id -> {
            // TODO: We already have a common superclass between ArXivIdentifier and ARK. This could be refactored further.
            if (id instanceof ArXivIdentifier) {
                configure(true, false, false);
            } else if (id instanceof ARK) {
                configure(false, false, false);
            } else {
                LOGGER.warn("Unknown eprint identifier type: {}", id.getClass().getName());
                configure(false, false, false);
            }
        }));
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        super.bindToEntry(entry);
        // Unlike other identifiers (they only depend on their own field value), eprint  depends on eprinttype thus
        // its identity changes whenever the eprinttype field changes .e.g. If eprinttype equals 'arxiv' then the eprint identity
        // will be of type ArXivIdentifier and if it equals 'ark' then it switches to type ARK.
        entry.getFieldsObservable().addListener(new WeakMapChangeListener<>(eprintTypeFieldListener));
    }
}
