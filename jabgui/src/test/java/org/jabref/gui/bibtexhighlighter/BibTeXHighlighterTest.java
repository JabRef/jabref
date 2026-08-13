package org.jabref.gui.bibtexhighlighter;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;

import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.search.SearchPreferences;

import com.airhacks.afterburner.injection.Injector;
import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BibTeXHighlighterTest {

    private StateManager stateManager;
    private BibTeXSyntaxHighlighter syntaxHighlighter;
    private BibTeXHighlighter highlighter;
    private CodeTextModel model;

    @BeforeEach
    void setUp() {
        SearchPreferences searchPreferences = mock(SearchPreferences.class);
        when(searchPreferences.shouldUsePostgresSearch()).thenReturn(false);

        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getSearchPreferences()).thenReturn(searchPreferences);

        Injector.setModelOrService(GuiPreferences.class, preferences);

        stateManager = mock(StateManager.class);
        syntaxHighlighter = mock(BibTeXSyntaxHighlighter.class);
        model = mock(CodeTextModel.class);

        when(stateManager.searchQueryProperty()).thenReturn(new SimpleStringProperty(""));
        highlighter = new BibTeXHighlighter(stateManager, syntaxHighlighter);
    }

    @AfterEach
    void tearDown() {
        Injector.forgetAll();
    }

    @Test
    void createRichParagraphCachesHighlightRegionsWhenTextUnchanged() {
        when(model.size()).thenReturn(2);
        when(model.getPlainText(0)).thenReturn("line1");
        when(model.getPlainText(1)).thenReturn("line2");
        when(syntaxHighlighter.computeHighlightRegions("line1\nline2")).thenReturn(List.of());

        highlighter.createRichParagraph(model, 0);
        highlighter.createRichParagraph(model, 1);

        verify(syntaxHighlighter, times(1)).computeHighlightRegions("line1\nline2");
    }

    @Test
    void createRichParagraphRecomputesRegionsWhenTextChanges() {
        when(model.size()).thenReturn(1);
        when(model.getPlainText(0)).thenReturn("line1");
        when(syntaxHighlighter.computeHighlightRegions("line1")).thenReturn(List.of());

        highlighter.createRichParagraph(model, 0);

        when(model.size()).thenReturn(2);
        when(model.getPlainText(0)).thenReturn("line1");
        when(model.getPlainText(1)).thenReturn("line2");
        when(syntaxHighlighter.computeHighlightRegions("line1\nline2")).thenReturn(List.of());

        highlighter.handleChange(model, null, null, 0, 1, 0);

        highlighter.createRichParagraph(model, 0);

        verify(syntaxHighlighter, times(1)).computeHighlightRegions("line1");
        verify(syntaxHighlighter, times(1)).computeHighlightRegions("line1\nline2");
    }
}
