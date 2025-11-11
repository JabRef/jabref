package org.jabref.gui.mergeentries.threewaymerge;

import org.jabref.gui.mergeentries.threewaymerge.toolbar.ThreeWayMergeToolbar.DiffView;

public record ShowDiffConfig(
        DiffView diffView,
        DiffMethod diffHighlightingMethod) {
}
