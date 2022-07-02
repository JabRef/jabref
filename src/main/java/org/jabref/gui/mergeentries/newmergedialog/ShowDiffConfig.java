package org.jabref.gui.mergeentries.newmergedialog;

import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.DiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar;

public record ShowDiffConfig(ThreeWayMergeToolbar.DiffView diffView, DiffHighlighter.DiffMethod diffMethod) {
}
