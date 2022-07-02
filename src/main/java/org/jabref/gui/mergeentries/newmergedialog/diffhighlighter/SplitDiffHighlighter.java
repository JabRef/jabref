package org.jabref.gui.mergeentries.newmergedialog.diffhighlighter;

import com.github.difflib.patch.DeltaType;
import org.fxmisc.richtext.StyleClassedTextArea;

/**
 * A diff highlighter in which changes of type {@link DeltaType#CHANGE} are split between source and target
 * text view. They are represented by an addition in the target text view and deletion in the source text view.
 * Normal addition and deletion are kept as they are.
 */
public final class SplitDiffHighlighter extends DiffHighlighter {

    public SplitDiffHighlighter(StyleClassedTextArea sourceTextview, StyleClassedTextArea targetTextview, DiffMethod diffMethod) {
        super(sourceTextview, targetTextview, diffMethod);
    }

    public SplitDiffHighlighter(StyleClassedTextArea sourceTextview, StyleClassedTextArea targetTextview) {
        this(sourceTextview, targetTextview, DiffMethod.WORDS);
    }

    @Override
    public void highlight() {
    }
}
