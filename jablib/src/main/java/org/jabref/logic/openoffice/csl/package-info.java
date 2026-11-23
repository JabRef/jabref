/// Adapts CSL output to Writer-ready text.
/// [CSLFormatUtils] normalizes markup and provides bounded in-text formatting fallbacks.
/// CSL style loading and citeproc integration shared by JabRef live in [org.jabref.logic.citationstyle].
/// Document-wide rendering and actions belong to [org.jabref.logic.openoffice.engine].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/overview.html#rendering-numbering-and-style-changes">Writer rendering</a>
@NullMarked
package org.jabref.logic.openoffice.csl;

import org.jspecify.annotations.NullMarked;
