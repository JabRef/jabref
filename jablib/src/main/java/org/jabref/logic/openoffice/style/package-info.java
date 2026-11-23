/// Common contract for selectable Writer citation styles.
/// [OOStyle] is implemented by shared CSL styles in [org.jabref.logic.citationstyle],
/// [org.jabref.logic.openoffice.bst.BstStyle], and [org.jabref.logic.openoffice.jstyle.JStyle].
/// Family-specific loading and formatting belong to those packages; rendering and document actions
/// belong to [org.jabref.logic.openoffice.engine].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/code-reorganization.html">Writer package ownership</a>
@NullMarked
package org.jabref.logic.openoffice.style;

import org.jspecify.annotations.NullMarked;
