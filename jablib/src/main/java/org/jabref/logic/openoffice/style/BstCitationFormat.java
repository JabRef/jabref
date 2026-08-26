package org.jabref.logic.openoffice.style;

/// Controls the in-text citation marker format used by the BST citation adapter.
/// The bibliography is always rendered by the BST engine regardless of this setting.
public enum BstCitationFormat {
    /// Numeric markers by first appearance: [1], [2], [1, 3], ...
    NUMERIC,

    /// Numeric markers whose numbers follow the BST bibliography sort order.
    NUMERIC_STYLE_ORDER,

    /// Author-year markers: (Cooper et al., 2007), (Smith, 2016), ...
    AUTHOR_YEAR,

    /// Style-defined markers from `\bibitem[label]{key}`: [ABC20], [ABC20a], ...
    STYLE_DEFINED
}
