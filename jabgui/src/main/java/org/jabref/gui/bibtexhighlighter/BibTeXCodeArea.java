package org.jabref.gui.bibtexhighlighter;

import java.util.Map;
import java.util.function.Supplier;

import javafx.util.Subscription;

import org.jabref.gui.StateManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.util.Range;

import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter;
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.SyntaxDecorator;
import org.jspecify.annotations.NullMarked;

@NullMarked
/// A [CodeArea] displaying BibTeX source with syntax highlighting.
///
/// Shared by all places showing the BibTeX source of an entry, so that they look the same.
public class BibTeXCodeArea extends CodeArea {

    private final BibTeXHighlighter highlighter;
    private Subscription colorSchemeSubscription = Subscription.EMPTY;

    public BibTeXCodeArea(StateManager stateManager, BibTeXSyntaxHighlighter syntaxHighlighter) {
        highlighter = new BibTeXHighlighter(stateManager, syntaxHighlighter);
        setSyntaxDecorator(highlighter);
        setWrapText(true);
        getStyleClass().add("bibtex-code-area");

        /// The color scheme of a dialog is set when it is shown, after the text was already rendered
        sceneProperty().subscribe(scene -> {
            colorSchemeSubscription.unsubscribe();
            colorSchemeSubscription = scene == null
                                      ? Subscription.EMPTY
                                      : scene.getPreferences().colorSchemeProperty().subscribe(_ -> refreshHighlighting());
        });
    }

    /// See [BibTeXHighlighter#setFieldPositionsProvider(Supplier)].
    public void setFieldPositionsProvider(Supplier<Map<Field, Range>> fieldPositionsProvider) {
        highlighter.setFieldPositionsProvider(fieldPositionsProvider);
    }

    /// Renders the text again, so that it picks up the current colors of the theme.
    public void refreshHighlighting() {
        SyntaxDecorator decorator = getSyntaxDecorator();
        setSyntaxDecorator(null);
        setSyntaxDecorator(decorator);
    }
}
