/// Common contract for selectable Writer citation styles and family-neutral paragraph-style identifiers.
/// [OOStyle] is implemented by shared CSL styles in [org.jabref.logic.citationstyle],
/// [org.jabref.logic.openoffice.bst.BstStyle], and [org.jabref.logic.openoffice.jstyle.JStyle].
/// [WriterParagraphStyles] supplies built-in bibliography paragraph-style catalogs and default identifiers.
/// CSL-aware default selection belongs to [org.jabref.logic.openoffice.csl.CSLFormatUtils].
/// Family-specific loading and formatting belong to those packages; rendering and document actions
/// belong to [org.jabref.logic.openoffice.engine].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/code-reorganization.html">Writer package ownership</a>
@NullMarked
package org.jabref.logic.openoffice.style;

import org.jspecify.annotations.NullMarked;
