package org.jabref.gui.fieldeditors;

import java.util.List;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.TransferMode;

import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.ObservableOptionalValue;

public class GroupEditor extends SimpleEditor {

    private ObservableOptionalValue<BibEntry> bibEntry = EasyBind.wrapNullable(new SimpleObjectProperty<>());

    public GroupEditor(final Field field,
                       final SuggestionProvider<?> suggestionProvider,
                       final FieldCheckers fieldCheckers,
                       final PreferencesService preferences,
                       final boolean isMultiLine) {
        super(field, suggestionProvider, fieldCheckers, preferences, isMultiLine);

        this.setOnDragOver(event -> {
            if (event.getDragboard().hasContent(DragAndDropDataFormats.GROUP)) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        this.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasContent(DragAndDropDataFormats.GROUP)) {
                List<String> group = (List<String>) event.getDragboard().getContent(DragAndDropDataFormats.GROUP);
                if (bibEntry.isValuePresent()) {
                    String changedGroup = bibEntry.getValue()
                                                  .map(entry -> entry.getField(StandardField.GROUPS)
                                                                     .map(setGroup -> setGroup + (preferences.getBibEntryPreferences().getKeywordSeparator()) + (group.get(0)))
                                                                     .orElse(group.get(0)))
                                                  .orElse(null);
                    bibEntry.getValue().map(entry -> entry.setField(StandardField.GROUPS, changedGroup));
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        bibEntry = EasyBind.wrapNullable(new SimpleObjectProperty<>(entry));
        super.bindToEntry(entry);
    }
}
