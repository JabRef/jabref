/// Field formatters used by [org.jabref.logic.layout.Layout] templates.
/// [org.jabref.logic.layout.LayoutEntry] resolves formatter names to implementations of
/// [org.jabref.logic.layout.LayoutFormatter]. [HTMLChars] and [OOPreFormatter] share the package-private
/// `LatexMarkupConverter` traversal while retaining HTML and Writer output rules.
/// [OOPreFormatter] supplies the HTML-like markup used by generic layouts and JStyle formatting.
/// Layout parsing and evaluation belong to [org.jabref.logic.layout]; Writer document operations
/// belong to [org.jabref.logic.openoffice.engine].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/code-reorganization.html#citation-model-and-jstyle-processing">JStyle processing</a>
package org.jabref.logic.layout.format;
