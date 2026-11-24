/// Loads JStyle definitions and formats their citations and bibliography entries.
/// Start with [JStyleLoader] and [JStyle]. [JStyleCitationProcessor] coordinates family-specific
/// numbering, author formatting, and markers; [JStyleBibliographyFormatter] formats bibliography entries.
/// Keeping these helpers with [JStyle] preserves their package-level access without exposing internals.
/// The shared style contract lives in [org.jabref.logic.openoffice.style]; document-wide renderer
/// adapters and actions belong to [org.jabref.logic.openoffice.engine].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/code-reorganization.html#citation-model-and-jstyle-processing">JStyle processing</a>
@NullMarked
package org.jabref.logic.openoffice.jstyle;

import org.jspecify.annotations.NullMarked;
