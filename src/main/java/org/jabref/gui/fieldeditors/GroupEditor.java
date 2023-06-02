package org.jabref.gui.fieldeditors;

import java.util.List;
import java.util.Optional;

import javafx.scene.input.TransferMode;

import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

public class GroupEditor extends SimpleEditor {

    private Optional<BibEntry> bibEntry;

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
            bibEntry.ifPresentOrElse((entry) -> {
                if (event.getDragboard().hasContent(DragAndDropDataFormats.GROUP)) {
                    List<String> group = (List<String>) event.getDragboard().getContent(DragAndDropDataFormats.GROUP);
                    String changedGroup = entry.getField(StandardField.GROUPS)
                                               .map(setGroup -> setGroup + (preferences.getBibEntryPreferences().getKeywordSeparator()) + (group.get(0)))
                                               .orElse(group.get(0));
                    entry.setField(StandardField.GROUPS, changedGroup);
                    event.setDropCompleted(true);
                }
                },
                    () -> {
                event.setDropCompleted(false); });
            event.consume();
        });
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        super.bindToEntry(entry);
        this.bibEntry = Optional.of(entry);
    }
}
