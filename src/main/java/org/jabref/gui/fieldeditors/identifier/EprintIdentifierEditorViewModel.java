package org.jabref.gui.fieldeditors.identifier;

import javafx.collections.WeakMapChangeListener;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ARK;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.EprintIdentifier;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class EprintIdentifierEditorViewModel extends BaseIdentifierEditorViewModel<EprintIdentifier> {

    // Since it's not obvious when to unregister the field listener, let's delegate this task to the garbage collector.
    // https://en.wikipedia.org/wiki/Lapsed_listener_problem
    private WeakMapChangeListener<Field, String> weakEprintTypeFieldListener;

    public EprintIdentifierEditorViewModel(SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, DialogService dialogService, TaskExecutor taskExecutor, PreferencesService preferences) {
        super(StandardField.EPRINT, suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences);
        configure(false, false);
        EasyBind.subscribe(identifier, newIdentifier -> {
            newIdentifier.ifPresent(id -> {
                // TODO: We already have a common superclass between ArXivIdentifier and ARK. This could be refactored further.
                if (id instanceof ArXivIdentifier) {
                    configure(true, false);
                } else if (id instanceof ARK) {
                    configure(false, false);
                }
            });
        });
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        super.bindToEntry(entry);
        weakEprintTypeFieldListener = new WeakMapChangeListener<>(change -> {
                    Field changedField = change.getKey();
                    if (StandardField.EPRINTTYPE.equals(changedField)) {
                        updateIdentifier();
                    }
                }
        );
        // Unlike other identifiers (they only depend on their own field value), eprint  depends on eprinttype thus
        // its identity changes whenever the eprinttype field changes .e.g. If eprinttype equals 'arxiv' then the eprint identity
        // will be of type ArXivIdentifier and if it equals 'ark' then it switches to type ARK.
        entry.getFieldsObservable().addListener(weakEprintTypeFieldListener);
    }
}
