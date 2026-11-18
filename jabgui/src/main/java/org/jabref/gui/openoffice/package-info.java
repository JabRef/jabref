/// JavaFX controls and dialogs for the Writer integration.
/// [OpenOfficePanel] presents document actions, prepares the selected JStyle, and controls capability-based availability;
/// [OpenOfficeCitationController] connects those actions to [org.jabref.logic.openoffice.engine.CitationEngine].
/// [StyleSelectDialogViewModel] manages style selection; [CslStyleResolver] resolves document metadata for manual CSL inference.
/// [CiteSpecialDialogView] limits citation choices and page information to the selected backend's capabilities.
/// Rendering, document storage, and Zotero strategy selection belong to the logic layer, not this package.
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/overview.html">Writer integration architecture</a>
package org.jabref.gui.openoffice;
