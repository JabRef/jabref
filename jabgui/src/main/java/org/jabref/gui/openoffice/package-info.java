/// JavaFX controls and dialogs for the Writer integration.
/// [OpenOfficePanel] presents document actions, prepares the selected JStyle, and controls capability-based availability;
/// [OpenOfficeCitationController] connects those actions to [org.jabref.logic.openoffice.engine.CitationEngine].
/// Cited-entry import collects embedded Zotero data read-only, then uses the captured library tab's
/// [org.jabref.gui.externalfiles.ImportHandler] for duplicate handling and library insertion.
/// [StyleSelectDialogViewModel] manages style selection and delegates external-style mutations to
/// [org.jabref.logic.openoffice.jstyle.JStyleLoader], [org.jabref.logic.openoffice.bst.BstStyleLoader], and
/// [org.jabref.logic.citationstyle.CSLStyleLoader]. The loaders alone persist their families' external paths.
/// [StyleSelectDialogView] loads each non-null family selection's preview on opening and on selection changes,
/// regardless of the active tab. Preview layouts live in [org.jabref.logic.preview]; JStyle templates use
/// [org.jabref.logic.layout]. These previews do not read or update Writer citations.
/// [CslStyleResolver] resolves manual CSL inference against the current bundled and external styles:
/// JabRef's applied-style metadata takes precedence, with Zotero preferences used only when that metadata is absent.
/// Startup and document connection retain the globally selected style; neither infers a style from the document.
/// [CiteSpecialDialogView] limits citation choices and page information to the selected style and mode's capabilities.
/// Rendering, document storage, and Zotero strategy selection belong to the logic layer, not this package.
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/overview.html">Writer integration architecture</a>
package org.jabref.gui.openoffice;
