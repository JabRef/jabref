package org.jabref.gui.mergeentries.newmergedialog;

import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar.DiffView;

public record ShowDiffConfig(
        DiffView diffView,
        DiffMethod diffHighlightingMethod) {
}
