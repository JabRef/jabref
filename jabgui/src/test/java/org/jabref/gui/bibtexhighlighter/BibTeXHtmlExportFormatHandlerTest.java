package org.jabref.gui.bibtexhighlighter;

import java.util.List;

import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.util.OptionalObjectProperty;

import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter;
import jfx.incubator.scene.control.richtext.TextPos;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class BibTeXHtmlExportFormatHandlerTest extends JavaFxTest {
    private static final String SOURCE = """
            @Article{Agrawal2015,
              author       = {Agrawal, Shikha and Agrawal, Jitendra},
              date         = {2015},
              journaltitle = {Procedia Computer Science},
              title        = {Survey on Anomaly Detection using Data Mining Techniques},
            }""";

    private Clipboard clipboard;
    private BibTeXCodeArea source;

    @Override
    public void start(Stage stage) {
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH)).thenReturn(OptionalObjectProperty.empty());
        source = new BibTeXCodeArea(stateManager, new BibTeXSyntaxHighlighter());
        source.setText(SOURCE);
        clipboard = Clipboard.getSystemClipboard();
        stage.setScene(new Scene(new VBox(source), 600, 400));
        stage.show();
        source.applyCss();
        source.layout();
    }

    @AfterEach
    void tearDown() {
        interact(clipboard::clear);
    }

    // [utest->req~entry-editor.source-tab.copy-syntax-highlighting~1]
    @Test
    void copyKeepsIndentationAndLineBreaksInMonospacePre() {
        interact(() -> {
            source.selectAll();
            source.copy();
            Element pre = Jsoup.parse(clipboard.getHtml()).selectFirst("pre");
            assertEquals(SOURCE, pre.wholeText());
            assertTrue(pre.attr("style").contains("monospace"));
            assertEquals(SOURCE, clipboard.getString());
        });
    }

    @Test
    void copyOfWhitespaceOnlyKeepsWhitespace() {
        interact(() -> {
            source.select(TextPos.ofLeading(1, 0), TextPos.ofLeading(1, 2));
            source.copy();
            assertEquals("  ", Jsoup.parse(clipboard.getHtml()).selectFirst("pre").wholeText());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"#0033B3", "#EECC44"})
    void partialCitationKeyKeepsThemeColor(String color) {
        interact(() -> {
            source.setStyle("-test-key-color: " + color + ";");
            source.getScene().getStylesheets().add(getClass().getResource("bibtex-clipboard.css").toExternalForm());
            source.applyCss();
            source.layout();
            source.select(TextPos.ofLeading(0, 11), TextPos.ofLeading(0, 17));
            source.copy();
            List<String> coloredText = Jsoup.parse(clipboard.getHtml()).select("pre span").stream()
                                            .filter(span -> span.attr("style").contains(color))
                                            .map(Element::wholeText)
                                            .toList();
            assertEquals(List.of("rawal2"), coloredText);
        });
    }

    @Test
    void copyEscapesHtml() {
        interact(() -> {
            source.setText("@Article{Müller,\n  title = {Tom & Jerry <cartoon> 日本語},\n}");
            source.selectAll();
            source.copy();
            var html = Jsoup.parse(clipboard.getHtml());
            assertEquals("@Article{Müller,\n  title = {Tom & Jerry <cartoon> 日本語},\n}", html.selectFirst("pre").wholeText());
            assertEquals(0, html.select("cartoon").size());
        });
    }
}
