package org.jabref.gui.bibtexhighlighter;

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
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class BibTeXRtfExportFormatHandlerTest extends JavaFxTest {
    private static final String SOURCE = """
            @Article{Agrawal2015,
              author       = {Agrawal, Shikha and Agrawal, Jitendra},
              date         = {2015},
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
    void copyUsesMonospaceFontAndKeepsLinesAndIndentation() {
        interact(() -> {
            source.selectAll();
            source.copy();
            String rtf = clipboard.getRtf();
            assertTrue(rtf.contains("\\f1\\fmodern Courier New;"));
            assertTrue(rtf.substring(rtf.indexOf("\\fi0")).contains("\\f1"));
            assertFalse(rtf.substring(rtf.indexOf("\\fi0")).contains("\\f0"));
            assertTrue(rtf.contains("@Article\\{Agrawal2015,"));
            assertTrue(rtf.contains("  author       = \\{Agrawal, Shikha and Agrawal, Jitendra\\},"));
            assertTrue(rtf.contains("  date         = \\{2015\\},"));
            assertEquals(3, rtf.split("\\\\par", -1).length - 1);
        });
    }

    @Test
    void partialCitationKeyKeepsThemeColor() {
        interact(() -> {
            source.setStyle("-color-success-emphasis: #EECC44;");
            source.getScene().getStylesheets().add(getClass().getResource("/org/jabref/gui/theme/internal/jabref-base.css").toExternalForm());
            source.applyCss();
            source.layout();
            source.select(TextPos.ofLeading(0, 11), TextPos.ofLeading(0, 17));
            source.copy();
            String rtf = clipboard.getRtf();
            assertTrue(rtf.contains("\\red238\\green204\\blue68;"));
            assertTrue(rtf.contains("\\cf2 rawal2"));
        });
    }

    @Test
    void copyEscapesRtfAndNonAsciiCharacters() {
        interact(() -> {
            source.setText("@Misc{Müller,\n  note = {\\LaTeX{} 日本語},\n}");
            source.selectAll();
            source.copy();
            String rtf = clipboard.getRtf();
            assertTrue(rtf.contains("M\\u252?ller"));
            assertTrue(rtf.contains("\\{\\\\LaTeX\\{\\} \\u26085?\\u26412?\\u-30050?\\}"));
        });
    }
}
