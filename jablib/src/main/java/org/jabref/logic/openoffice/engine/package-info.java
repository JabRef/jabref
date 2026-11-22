/// Writer citation rendering and document-action coordination.
/// Start with [CitationEngine] for the document-bound API; [UpdatePipeline] prepares and applies
/// canonical renderer output, while [DocumentWriteSession] controls the write phase.
/// [CitationGroupEditor] prepares group replacements before mutation and applies structural edits inside the caller's session.
/// [RendererFactory] selects [JStyleRenderer], [CslRenderer], or [BstRenderer] through [CitationRenderer].
/// [JabRefMarkCodec] defines canonical identities; their document storage lives in
/// [org.jabref.logic.openoffice.engine.storage].
/// JStyle and BST always share the canonical path; CSL uses it with Zotero compatibility disabled.
/// [ZoteroCitationStrategy] keeps native CSL-only storage, ordering, validation, and writes separate,
/// while sharing [CslRenderer], the pure citation model, and the engine's write-session infrastructure.
/// [CitationFormatConverter] converts canonical and native marks on the next CSL insertion when formats differ.
/// It also migrates supported legacy `JR_cite` marks to canonical marks on insertion outside native mode.
/// Among conversion paths, only legacy-only migration permits missing-entry placeholders;
/// canonical/native conversion requires resolved sources.
/// `CID_n` marks remain unmanaged, and mixed managed formats are unsupported.
/// Conversion changes citation marks, not bibliography formats; normal synchronization separately handles target bibliography output.
/// [ConversionMetadata] persists citation sidecars in document properties; its lookup index is rebuilt per action, not cached persistently.
/// [CitedEntryExporter] resolves ordered citation keys into a library without accessing Writer.
/// `CitationEngine.importCited()` delegates embedded Zotero entry collection to
/// [org.jabref.logic.openoffice.CitedEntryCollector] without document writes or format conversion.
/// Duplicate handling and target-library insertion belong to the GUI's existing import pipeline.
/// Low-level document access helpers live in [org.jabref.logic.openoffice.uno].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/overview.html">Writer integration overview</a>
/// @see <a href="https://devdocs.jabref.org/decisions/0084-canonical-writer-citations.html">Canonical Writer citation decision</a>
@NullMarked
package org.jabref.logic.openoffice.engine;

import org.jspecify.annotations.NullMarked;
