package org.jabref.gui.bibtexhighlighter;

import java.util.List;
import java.util.stream.IntStream;

import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.search.SearchType;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.util.OptionalObjectProperty;

import com.airhacks.afterburner.injection.Injector;
import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

        when(stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH)).thenReturn(OptionalObjectProperty.empty());
        highlighter = new BibTeXHighlighter(stateManager, syntaxHighlighter);
    }

    @AfterEach
    void tearDown() {
        Injector.forgetAll();
    }

    @Test
    void highlightsWholeFieldNamesAfterEmoji() {
        String firstLine = "@Article{🍯🍫🌚,";
        String secondLine = "  author = {Example},";
        String thirdLine = "  title = {Title},";
        when(model.size()).thenReturn(3);
        when(model.getPlainText(0)).thenReturn(firstLine);
        when(model.getPlainText(1)).thenReturn(secondLine);
        when(model.getPlainText(2)).thenReturn(thirdLine);
        highlighter = new BibTeXHighlighter(stateManager, new BibTeXSyntaxHighlighter());

        RichParagraph author = highlighter.createRichParagraph(model, 1);
        RichParagraph title = highlighter.createRichParagraph(model, 2);

        assertEquals(List.of("  ", "author", " = ", "{Example}", ","),
                IntStream.range(0, author.getSegmentCount()).mapToObj(index -> author.getSegment(index).getText()).toList());
        assertEquals(List.of("  ", "title", " = ", "{Title}", ","),
                IntStream.range(0, title.getSegmentCount()).mapToObj(index -> title.getSegment(index).getText()).toList());
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

    @Test
    void createRichParagraphIgnoresStaleParagraphAfterClearingTheModel() {
        when(model.size()).thenReturn(5);
        for (int index = 0; index < 5; index++) {
            when(model.getPlainText(index)).thenReturn("line" + index);
        }
        when(syntaxHighlighter.computeHighlightRegions("line0\nline1\nline2\nline3\nline4")).thenReturn(List.of());

        highlighter.createRichParagraph(model, 4);

        when(model.size()).thenReturn(1);
        when(model.getPlainText(0)).thenReturn("");
        when(model.getPlainText(4)).thenThrow(new AssertionError("Stale paragraph access should be guarded"));
        when(syntaxHighlighter.computeHighlightRegions("")).thenReturn(List.of());
        highlighter.handleChange(model, null, null, 0, 0, 0);

        assertDoesNotThrow(() -> highlighter.createRichParagraph(model, 4));
    }
}
