package org.jabref.gui.preferences;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Label;

import org.jabref.gui.testutils.JavaFxExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(JavaFxExtension.class)
class PreferencesSearchHandlerTest {

    private Node autosaveNode;
    private PreferencesTab generalTab;
    private PreferencesTab tableTab;
    private PreferencesSearchHandler handler;

    @BeforeEach
    void setUp() {
        autosaveNode = new Label();
        generalTab = tab("General", new SearchableElement("Show welcome tab", new Label()),
                new SearchableElement("Autosave local libraries", autosaveNode),
                new SearchableElement("Autosave interval", new Label()));
        tableTab = tab("Table", new SearchableElement("Show column names", new Label()));
        handler = new PreferencesSearchHandler(List.of(generalTab, tableTab));
    }

    @Test
    void firstMatchIsTheFirstDeclaredMatchingElement() {
        handler.filterTabs("autosave");

        assertEquals(List.of(generalTab), handler.filteredPreferenceTabsProperty().get());
        assertEquals(autosaveNode, handler.firstMatch(generalTab).orElseThrow());
        assertTrue(handler.firstMatch(tableTab).isEmpty());
    }

    @Test
    void firstMatchIsForgottenOnANewQuery() {
        handler.filterTabs("autosave");
        handler.filterTabs("column");

        assertTrue(handler.firstMatch(generalTab).isEmpty());
        assertTrue(handler.firstMatch(tableTab).isPresent());
    }

    @Test
    void noFirstMatchWithoutQuery() {
        handler.filterTabs("autosave");
        handler.filterTabs("");

        assertTrue(handler.firstMatch(generalTab).isEmpty());
    }

    private static PreferencesTab tab(String title, SearchableElement... elements) {
        return new PreferencesTab() {
            @Override
            public Node getContent() {
                return new Label(title);
            }

            @Override
            public String getTabName() {
                return title;
            }

            @Override
            public List<SearchableElement> getSearchableElements() {
                return List.of(elements);
            }

            @Override
            public void setValues() {
            }

            @Override
            public void storeSettings() {
            }

            @Override
            public boolean validateSettings() {
                return true;
            }

            @Override
            public List<String> getRestartWarnings() {
                return List.of();
            }
        };
    }
}
