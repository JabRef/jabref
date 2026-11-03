/// Writer citation rendering and document-action coordination.
/// Start with [CitationEngine] for the document-bound API; [UpdatePipeline] prepares and applies
/// canonical renderer output, while [DocumentSession] controls the write phase.
/// [CitationGroupEditor] owns structural insertion, merging, and separation inside the caller's session.
/// [RendererFactory] selects [JStyleRenderer], [CslRenderer], or [BstRenderer] through [CitationRenderer].
/// [MarkCodec] defines canonical identities; their document storage lives in
/// [org.jabref.logic.openoffice.engine.storage].
/// [ZoteroCitationStrategy] keeps CSL-only Zotero storage separate from canonical JabRef marks.
/// [CitedEntryExporter] resolves ordered citation keys into a library without accessing Writer.
/// Low-level document access helpers live in [org.jabref.logic.openoffice.uno].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/overview.html">Writer integration overview</a>
/// @see <a href="https://devdocs.jabref.org/decisions/0080-canonical-writer-citations.html">Canonical Writer citation decision</a>
@NullMarked
package org.jabref.logic.openoffice.engine;

import org.jspecify.annotations.NullMarked;
