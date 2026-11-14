/// Canonical Writer citation storage and write-in-place operations.
/// [MarkStore] reads reference-mark identities and page information indexed by citation storage order.
/// [MarkWriter] applies formatted renderer output inside existing marks and keeps its change baseline in memory.
/// [DocumentStateStore] records the successfully applied style independently of Zotero preferences.
/// [UndoableDocumentProperties] records custom-property deltas for page information and format conversion.
/// Rendering and action coordination live in [org.jabref.logic.openoffice.engine]; document ordering
/// lives in [org.jabref.logic.openoffice.frontend]. Low-level document operations are in
/// [org.jabref.logic.openoffice.uno].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/overview.html#what-is-stored-in-a-document">Canonical document storage</a>
/// @see <a href="https://devdocs.jabref.org/decisions/0080-canonical-writer-citations.html">Canonical Writer citation decision</a>
@NullMarked
package org.jabref.logic.openoffice.engine.storage;

import org.jspecify.annotations.NullMarked;
