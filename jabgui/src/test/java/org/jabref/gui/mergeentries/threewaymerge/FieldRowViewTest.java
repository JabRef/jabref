package org.jabref.gui.mergeentries.threewaymerge;

import java.util.List;

import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.fieldeditors.GroupsEditor;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.mergeentries.threewaymerge.fieldsmerger.FieldMergerFactory;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class FieldRowViewTest extends JavaFxTest {

    private final BibEntry leftEntry = new BibEntry().withField(StandardField.GROUPS, "Alpha, Beta");
    private final BibEntry rightEntry = new BibEntry().withField(StandardField.GROUPS, "Zulu, Gamma");
    private FieldRowView groupsRow;

    @Override
    public void start(Stage stage) {
        Injector.setModelOrService(DialogService.class, mock(DialogService.class));
        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));
        Injector.setModelOrService(KeyBindingRepository.class, new KeyBindingRepository());
        Injector.setModelOrService(JournalAbbreviationRepository.class, mock(JournalAbbreviationRepository.class));

        GuiPreferences preferences = mock(GuiPreferences.class, RETURNS_DEEP_STUBS);
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());

        groupsRow = new FieldRowView(StandardField.GROUPS, leftEntry, rightEntry, new BibEntry(), new FieldMergerFactory(','), preferences, stateManager, 0);

        stage.setScene(new Scene(new HBox(groupsRow.getMergedValueCell())));
        stage.show();
    }

    @AfterAll
    static void tearDown() {
        Injector.forgetAll();
    }

    @Test
    void selectingLeftValueShowsLeftGroups() {
        interact(groupsRow::selectLeftValue);

        assertEquals(List.of(new Keyword("Alpha"), new Keyword("Beta")), mergedGroups());
    }

    @Test
    void selectingRightValueShowsRightGroupsInTheirOrder() {
        interact(groupsRow::selectLeftValue);
        awaitEvents();

        interact(groupsRow::selectRightValue);
        awaitEvents();

        assertEquals(List.of(new Keyword("Zulu"), new Keyword("Gamma")), mergedGroups());
        assertEquals("Zulu, Gamma", groupsRow.getMergedValue());
    }

    @Test
    void removingGroupUpdatesMergedValue() {
        interact(groupsRow::selectLeftValue);

        interact(() -> mergedGroupsEditor().getViewModel().groupListProperty().remove(new Keyword("Beta")));

        assertEquals("Alpha", groupsRow.getMergedValue());
    }

    private GroupsEditor mergedGroupsEditor() {
        return (GroupsEditor) groupsRow.getMergedValueCell().getChildren().getFirst();
    }

    private List<Keyword> mergedGroups() {
        return List.copyOf(mergedGroupsEditor().getViewModel().groupListProperty());
    }
}
