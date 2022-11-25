package org.jabref.gui.mergeentries.newmergedialog;

import static org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.DiffHighlighter.DiffMethod;
import static org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar.DiffView;

public record ShowDiffConfig(
        DiffView diffView,
        DiffMethod diffHighlightingMethod) {
}
