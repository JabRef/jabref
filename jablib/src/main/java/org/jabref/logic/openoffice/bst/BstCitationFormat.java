package org.jabref.logic.openoffice.bst;

/// Selects the notation for normal and in-text BST citations and their bibliography labels.
/// Requested bibliography entry bodies are rendered from the selected BST style.
public enum BstCitationFormat {
    /// Numeric markers: [1], [2], [1, 3], ...
    NUMERIC,

    /// Author-year markers: (Cooper et al., 2007), (Smith, 2016), ...
    AUTHOR_YEAR,

    /// Style-defined markers from `\bibitem[label]{key}`: [ABC20], [ABC20a], ...
    STYLE_DEFINED
}
