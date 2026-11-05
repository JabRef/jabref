package org.jabref.gui.bibtexhighlighter;

import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.util.OptionalObjectProperty;

import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter;
import jfx.incubator.scene.control.richtext.TextPos;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class BibTeXRtfExportFormatHandlerTest extends JavaFxTest {
    private static final String SOURCE = """
            @Article{Agrawal2015,
              author       = {Agrawal, Shikha and Agrawal, Jitendra},
              date         = {2015},
            }""";

    private static final String HEADER = "{\\rtf1\\ansi\\ansicpg1252\\uc1\\sl0\\sb0\\sa0\\deff0"
            + "{\\fonttbl\\f0\\fswiss Helvetica;\\f1\\fmodern Courier New;}\r\n";

    private Clipboard clipboard;
    private BibTeXCodeArea source;

    @Override
    public void start(Stage stage) {
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH)).thenReturn(OptionalObjectProperty.empty());
        source = new BibTeXCodeArea(stateManager, new BibTeXSyntaxHighlighter());
        source.setFont(Font.font("Monospaced", 13));
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

    // [utest->req~entry-editor.source-tab.copy-layout~1]
    @Test
    void copyUsesMonospaceFontAndKeepsLinesAndIndentation() {
        interact(() -> {
            source.selectAll();
            source.copy();
            assertEquals(HEADER + "{\\colortbl ;\\red0\\green0\\blue0;}\r\n"
                    + "\\fi0\\ql \\f1\\fs26\\cf0 @Article\\{Agrawal2015,\\cf0 \\par\r\n"
                    + "\\fi0\\ql \\cf0   author       = \\{Agrawal, Shikha and Agrawal, Jitendra\\},\\cf0 \\par\r\n"
                    + "\\fi0\\ql \\cf0   date         = \\{2015\\},\\cf0 \\par\r\n"
                    + "\\fi0\\ql \\cf0 \\}\\cf0 \r\n}\r\n", clipboard.getRtf());
        });
    }

    // [utest->req~entry-editor.source-tab.copy-syntax-colors~1]
    @Test
    void partialCitationKeyKeepsThemeColor() {
        interact(() -> {
            source.setStyle("-color-success-emphasis: #EECC44;");
            source.getScene().getStylesheets().add(getClass().getResource("/org/jabref/gui/theme/internal/jabref-base.css").toExternalForm());
            source.applyCss();
            source.layout();
            source.select(TextPos.ofLeading(0, 11), TextPos.ofLeading(0, 17));
            source.copy();
            assertEquals(HEADER + "{\\colortbl ;\\red0\\green0\\blue0;\\red238\\green204\\blue68;}\r\n"
                    + "\\fi0\\ql \\f1\\cf2 rawal2\\cf0 \r\n}\r\n", clipboard.getRtf());
        });
    }

    @Test
    void copyEscapesRtfAndNonAsciiCharacters() {
        interact(() -> {
            source.setText("@Misc{Müller,\n  note = {\\LaTeX{} 日本語},\n}");
            source.selectAll();
            source.copy();
            assertEquals(HEADER + "{\\colortbl ;\\red0\\green0\\blue0;}\r\n"
                    + "\\fi0\\ql \\f1\\fs26\\cf0 @Misc\\{M\\u252?ller,\\cf0 \\par\r\n"
                    + "\\fi0\\ql \\cf0   note = \\{\\\\LaTeX\\{\\} \\u26085?\\u26412?\\u-30050?\\},\\cf0 \\par\r\n"
                    + "\\fi0\\ql \\cf0 \\}\\cf0 \r\n}\r\n", clipboard.getRtf());
        });
    }
}
