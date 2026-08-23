package org.jabref.gui.fieldeditors;

import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.undo.UndoManager;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@NullMarked
class TagsEditorTest extends ApplicationTest {

    private final ListProperty<Keyword> tags = new SimpleListProperty<>(FXCollections.observableArrayList());
    private TestTagsEditor editor;

    @Override
    public void start(Stage stage) {
        Injector.setModelOrService(DialogService.class, mock(DialogService.class));
        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));
        Injector.setModelOrService(KeyBindingRepository.class, new KeyBindingRepository());

        editor = new TestTagsEditor();
        editor.initialize(tags);

        stage.setScene(new Scene(editor));
        stage.show();
    }

    @AfterAll
    static void tearDown() {
        Injector.forgetAll();
    }

    @Test
    void removingSeveralTagsFromBoundListDoesNotMutateItDuringSynchronization() {
        interact(() -> tags.setAll(List.of(new Keyword("Alpha"), new Keyword("Beta"), new Keyword("Gamma"))));

        interact(() -> tags.setAll(List.of(new Keyword("Gamma"))));

        assertEquals(List.of(new Keyword("Gamma")), editor.getTags());
    }

    @Test
    void rapidTagUpdatesAreSortedUsingTheLatestList() {
        interact(() -> tags.setAll(List.of(new Keyword("Alpha"), new Keyword("Beta"), new Keyword("Gamma"))));
        interact((Runnable) tags::get);

        interact(() -> {
            tags.setAll(List.of(new Keyword("Alpha"), new Keyword("Beta")));
            tags.setAll(List.of(new Keyword("Zulu"), new Keyword("Beta")));
            tags.setAll(List.of(new Keyword("Gamma"), new Keyword("Alpha")));
        });
        interact((Runnable) tags::get);
        interact((Runnable) tags::get);

        assertEquals(List.of(new Keyword("Alpha"), new Keyword("Gamma")), editor.getTags());
    }

    private static class TestTagsEditor extends TagsEditor {

        TestTagsEditor() {
            super(StandardField.GROUPS,
                    mock(SuggestionProvider.class),
                    mock(FieldCheckers.class),
                    mock(UndoManager.class));
        }

        void initialize(ListProperty<Keyword> tagListProperty) {
            setupTagsField(new StringConverter<>() {
                @Override
                public String toString(Keyword keyword) {
                    return keyword.get();
                }

                @Override
                public Keyword fromString(String string) {
                    return new Keyword(string);
                }
            }, ',', tagListProperty, _ -> List.of());
        }

        List<Keyword> getTags() {
            return List.copyOf(tagsField.getTags());
        }

        @Override
        public void bindToEntry(BibEntry entry) {
        }
    }
}
