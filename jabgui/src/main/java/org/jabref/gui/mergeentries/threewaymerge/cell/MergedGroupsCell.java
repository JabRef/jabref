package org.jabref.gui.mergeentries.threewaymerge.cell;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.fieldeditors.GroupsEditor;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class MergedGroupsCell extends ThreeWayMergeCell {
    public MergedGroupsCell(String text, int rowIndex, GuiPreferences preferences, StateManager stateManager) {
        super(text, rowIndex);
        getStyleClass().add(MergedFieldCell.DEFAULT_STYLE_CLASS);
        GroupsEditor groupsEditor = createGroupsEditor(preferences, stateManager);
        groupsEditor.disableTagSorting();
        groupsEditor.getViewModel().textProperty().bindBidirectional(textProperty());
        HBox.setHgrow(groupsEditor, Priority.ALWAYS);
        getChildren().add(groupsEditor);
    }

    private static GroupsEditor createGroupsEditor(GuiPreferences preferences, StateManager stateManager) {
        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().orElseGet(BibDatabaseContext::new);
        SuggestionProvider<?> suggestionProvider = stateManager.activeTabProperty().get()
                                                               .map(LibraryTab::getSuggestionProviders)
                                                               .orElseGet(SuggestionProviders::new)
                                                               .getForField(StandardField.GROUPS);
        FieldCheckers fieldCheckers = new FieldCheckers(
                databaseContext,
                preferences.getFilePreferences(),
                Injector.instantiateModelOrService(JournalAbbreviationRepository.class),
                preferences.getEntryEditorPreferences().shouldAllowIntegerEditionBibtex(),
                preferences.getCitationKeyPatternPreferences().getUnwantedCharacters());

        return new GroupsEditor(
                StandardField.GROUPS,
                suggestionProvider,
                fieldCheckers,
                preferences.getBibEntryPreferences(),
                databaseContext,
                new JabRefUndoManager());
    }
}
