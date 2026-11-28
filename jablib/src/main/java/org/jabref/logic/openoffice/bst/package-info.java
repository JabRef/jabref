/// Loads Writer BST styles and converts their bibliography output.
/// [BstStyleLoader] supplies [BstStyle] instances; [BstCitationFormat] selects the citation notation.
/// [BstEntryRenderer] prepares entry previews, while [PandocLatexConverter] and [BSTFormatUtils]
/// convert and normalize LaTeX output. The BST interpreter lives in [org.jabref.logic.bst].
/// Document-wide rendering and actions belong to [org.jabref.logic.openoffice.engine].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/code-reorganization.html">Writer package ownership</a>
@NullMarked
package org.jabref.logic.openoffice.bst;

import org.jspecify.annotations.NullMarked;
