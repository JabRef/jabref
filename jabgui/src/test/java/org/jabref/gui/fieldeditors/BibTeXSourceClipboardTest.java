package org.jabref.gui.fieldeditors;

import java.util.List;

import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.bibtexhighlighter.BibTeXHighlighter;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.injection.Injector;
import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter;
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.TextPos;
import org.jsoup.Jsoup;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class BibTeXSourceClipboardTest extends JavaFxTest {
    private static final String SOURCE = """
            @Article{Agrawal2015,
              author       = {Agrawal, Shikha and Agrawal, Jitendra},
              date         = {2015},
              journaltitle = {Procedia Computer Science},
              title        = {Survey on Anomaly Detection using Data Mining Techniques},
              doi          = {10.1016/j.procs.2015.08.220},
              issn         = {1877-0509},
              pages        = {708-713},
              volume       = {60},
              journal      = {Procedia Computer Science},
              publisher    = {Elsevier BV},
            }
            """;

    private Clipboard clipboard;
    private MarkdownEditor markdown;
    private CodeArea source;

    @Override
    public void start(Stage stage) {
        GuiPreferences preferences = mock(GuiPreferences.class, RETURNS_DEEP_STUBS);
        Injector.setModelOrService(GuiPreferences.class, preferences);
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH)).thenReturn(OptionalObjectProperty.empty());
        source = new CodeArea();
        source.getStyleClass().add("bibtex-code-area");
        source.setSyntaxDecorator(new BibTeXHighlighter(stateManager, new BibTeXSyntaxHighlighter()));
        source.setText(SOURCE);
        FieldCheckers checkers = mock(FieldCheckers.class);
        when(checkers.getForField(StandardField.ABSTRACT)).thenReturn(List.of());
        markdown = new MarkdownEditor(StandardField.ABSTRACT, mock(SuggestionProvider.class), checkers, preferences,
                mock(UndoManager.class), mock(UndoAction.class), mock(RedoAction.class), new BibDatabaseContext());
        clipboard = Clipboard.getSystemClipboard();
        new ClipBoardManager(stateManager, clipboard, new java.awt.datatransfer.Clipboard("test"));
        stage.setScene(new Scene(new VBox(source, markdown), 600, 400));
        stage.show();
        source.applyCss();
        source.layout();
    }

    @AfterEach
    void tearDown() {
        interact(clipboard::clear);
        Injector.forgetAll();
    }

    // [utest->req~entry-editor.source-tab.copy-syntax-highlighting~1]
    @ParameterizedTest
    @ValueSource(strings = {"#0033b3", "#eecc44"})
    void partialCitationKeyRetainsThemeColor(String color) {
        interact(() -> {
            source.setStyle("-test-key-color: " + color + ";");
            source.getScene().getStylesheets().add(getClass().getResource("bibtex-clipboard.css").toExternalForm());
            source.applyCss();
            source.layout();
            source.select(TextPos.ofLeading(0, 17), TextPos.ofLeading(0, 11));
            BibTeXHighlighter.copySource(source);
            assertEquals("rawal2", clipboard.getString());
            var html = Jsoup.parse(clipboard.getHtml());
            assertEquals("rawal2", html.body().text());
            assertEquals(List.of("rawal2"), html.select("[style]").stream()
                                               .filter(element -> element.attr("style").contains(color))
                                               .map(org.jsoup.nodes.Element::text)
                                               .toList());
        });
    }

    @Test
    void sourcePasteIntoMarkdownPreservesPlainText() {
        interact(() -> {
            source.selectAll();
            BibTeXHighlighter.copySource(source);
            assertTrue(clipboard.hasHtml());
            assertEquals(source.getText(), clipboard.getContent(ClipBoardManager.BIBTEX_SOURCE));
            markdown.getTextInput().paste();
            assertEquals(source.getText(), markdown.getTextInput().getText());
        });
    }

    @Test
    void externalHtmlStillConvertsToMarkdown() {
        interact(() -> {
            ClipboardContent content = new ClipboardContent();
            content.putString("Bold");
            content.putHtml("<strong>Bold</strong>");
            clipboard.setContent(content);
            markdown.getTextInput().paste();
            assertEquals("**Bold**", markdown.getTextInput().getText().strip());
        });
    }

    @Test
    void staleSourceMarkerDoesNotPreventExternalHtmlConversion() {
        interact(() -> {
            ClipboardContent content = new ClipboardContent();
            content.putString("Bold");
            content.putHtml("<strong>Bold</strong>");
            content.put(ClipBoardManager.BIBTEX_SOURCE, SOURCE);
            clipboard.setContent(content);
            markdown.getTextInput().paste();
            assertEquals("**Bold**", markdown.getTextInput().getText().strip());
        });
    }

    @Test
    void multilineUnicodeSourceRetainsTextInHtmlAndMarkdown() {
        interact(() -> {
            source.setText("@Article{Müller,\n  title = {Résumé & 日本語},\n}");
            source.selectAll();
            BibTeXHighlighter.copySource(source);
            assertEquals(source.getText(), clipboard.getString());
            assertEquals("@Article{Müller, title = {Résumé & 日本語}, }", Jsoup.parse(clipboard.getHtml()).body().text());
            markdown.getTextInput().paste();
            assertEquals(source.getText(), markdown.getTextInput().getText());
        });
    }

    @Test
    void sourceCopyEscapesHtml() {
        interact(() -> {
            source.setText("@Article{Agrawal2015,\n  title = {Tom & Jerry <cartoon>},\n}");
            source.select(TextPos.ofLeading(1, 11), TextPos.ofLeading(1, 32));
            BibTeXHighlighter.copySource(source);
            assertEquals("Tom & Jerry <cartoon>", Jsoup.parse(clipboard.getHtml()).body().text());
            assertEquals(0, Jsoup.parse(clipboard.getHtml()).select("cartoon").size());
        });
    }

    @Test
    void emptySelectionLeavesClipboardUnchanged() {
        interact(() -> {
            ClipboardContent content = new ClipboardContent();
            content.putString("previous copy");
            clipboard.setContent(content);
            source.select(TextPos.ZERO);
            BibTeXHighlighter.copySource(source);
            assertEquals("previous copy", clipboard.getString());
            assertFalse(clipboard.hasHtml());
        });
    }

    @Test
    void whitespaceSelectionIsCopiedExactly() {
        interact(() -> {
            source.select(TextPos.ofLeading(1, 0), TextPos.ofLeading(1, 2));
            BibTeXHighlighter.copySource(source);
            assertEquals("  ", clipboard.getString());
            assertTrue(clipboard.hasHtml());
        });
    }
}
