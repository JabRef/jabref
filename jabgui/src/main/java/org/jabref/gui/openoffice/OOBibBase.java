package org.jabref.gui.openoffice;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jabref.gui.DialogService;
import org.jabref.logic.JabRefException;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.action.EditInsert;
import org.jabref.logic.openoffice.action.EditMerge;
import org.jabref.logic.openoffice.action.EditSeparate;
import org.jabref.logic.openoffice.action.ExportCited;
import org.jabref.logic.openoffice.action.ManageCitations;
import org.jabref.logic.openoffice.action.Update;
import org.jabref.logic.openoffice.frontend.OOFrontend;
import org.jabref.logic.openoffice.frontend.RangeForOverlapCheck;
import org.jabref.logic.openoffice.oocsltext.BSTCitationOOAdapter;
import org.jabref.logic.openoffice.oocsltext.BstUpdateBibliography;
import org.jabref.logic.openoffice.oocsltext.CSLCitationOOAdapter;
import org.jabref.logic.openoffice.oocsltext.CSLCitationType;
import org.jabref.logic.openoffice.oocsltext.CSLUpdateBibliography;
import org.jabref.logic.openoffice.oocsltext.MissingStyleDefinedCitationLabelException;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.CitationEntry;
import org.jabref.model.openoffice.rangesort.FunctionalTextViewCursor;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoCrossRef;
import org.jabref.model.openoffice.uno.UnoCursor;
import org.jabref.model.openoffice.uno.UnoRedlines;
import org.jabref.model.openoffice.uno.UnoStyle;
import org.jabref.model.openoffice.uno.UnoUndo;
import org.jabref.model.openoffice.util.OOResult;
import org.jabref.model.openoffice.util.OOVoidResult;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.XComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Class for manipulating the Bibliography of the currently started document in OpenOffice.
public class OOBibBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(OOBibBase.class);

    private final DialogService dialogService;

    private final OOBibBaseConnect connection;
    private final XComponentContext componentContext;

    private final OpenOfficePreferences openOfficePreferences;
    private final BibEntryTypesManager bibEntryTypesManager;

    private CSLCitationOOAdapter cslCitationOOAdapter;
    private CSLUpdateBibliography cslUpdateBibliography;
    private BSTCitationOOAdapter bstCitationOOAdapter;
    private BstUpdateBibliography bstUpdateBibliography;

    public OOBibBase(Path loPath, DialogService dialogService, OpenOfficePreferences openOfficePreferences, BibEntryTypesManager bibEntryTypesManager)
            throws BootstrapException, CreationException, IOException, InterruptedException {

        this.dialogService = dialogService;
        this.connection = new OOBibBaseConnect(loPath, dialogService);
        this.componentContext = connection.getComponentContext();
        this.openOfficePreferences = openOfficePreferences;
        this.bibEntryTypesManager = bibEntryTypesManager;
    }

    /// Adapter lifecycle is tied to the currently selected document. Keep creation here so cite/export/
    /// bibliography actions only ever use adapters that were initialized as part of document selection.
    private void initializeCitationAdapter(XTextDocument doc) throws WrappedTargetException, NoSuchElementException {
        // Plain reassignment would be enough for most helpers, but CSLCitationOOAdapter registers a listener on
        // openOfficePreferences. Clear document-bound helpers first so the old CSL adapter can dispose that listener
        // before we replace the adapters for the newly selected document.
        clearCitationAdapters();
        cslCitationOOAdapter = new CSLCitationOOAdapter(doc, componentContext, openOfficePreferences, bibEntryTypesManager);
        cslUpdateBibliography = new CSLUpdateBibliography(openOfficePreferences);
        bstCitationOOAdapter = new BSTCitationOOAdapter(doc, componentContext, openOfficePreferences);
        bstUpdateBibliography = new BstUpdateBibliography();
    }

    /// Clear all document-bound helpers after connection loss or before switching to another document.
    private void clearCitationAdapters() {
        if (cslCitationOOAdapter != null) {
            cslCitationOOAdapter.dispose();
        }
        cslCitationOOAdapter = null;
        cslUpdateBibliography = null;
        bstCitationOOAdapter = null;
        bstUpdateBibliography = null;
    }

    /// `dispose` in SE is usually treated as a lifecycle hook which means that the concerned object is no longer to be used.
    ///
    /// It is arguable that this wrapper logically adds no extra functionality over `clearCitationAdapters` (the
    /// private helper), but it is to maintain a semantic split when called from [OpenOfficePanel] externally - meaning
    /// clear related resources without knowing "what".
    ///
    /// When called internally, we use the private helper as it does not come with the meaning mentioned above, as we
    /// are about to reuse the same `ooBase` object.
    public void dispose() {
        clearCitationAdapters();
    }

    public void guiActionSelectDocument(boolean autoSelectForSingle) throws WrappedTargetException, NoSuchElementException {
        testDialog(connection.selectDocument(autoSelectForSingle));

        if (isConnectedToDocument()) {
            XTextDocument doc = this.getXTextDocument().get();
            initializeCitationAdapter(doc);
            dialogService.notify(Localization.lang("Connected to document") + ": "
                    + this.getCurrentDocumentTitle().orElse(""));
        }
    }

    /// A simple test for document availability.
    ///
    /// See also `isDocumentConnectionMissing` for a test actually attempting to use the connection.
    public boolean isConnectedToDocument() {
        return this.connection.isConnectedToDocument();
    }

    /// @return true if we are connected to a document
    public boolean isDocumentConnectionMissing() {
        return this.connection.isDocumentConnectionMissing();
    }

    /// Either return an XTextDocument or return JabRefException.
    public OOResult<XTextDocument, OOError> getXTextDocument() {
        return this.connection.getXTextDocument();
    }

    /// The title of the current document, or Optional.empty()
    public Optional<String> getCurrentDocumentTitle() {
        return this.connection.getCurrentDocumentTitle();
    }

    OOResult<Optional<CitationStyle>, OOError> inferCslStyleFromDocument() {
        if (!isConnectedToDocument()) {
            return OOResult.ok(Optional.empty());
        }

        OOResult<XTextDocument, OOError> document = getXTextDocument();
        if (document.isError()) {
            return OOResult.error(document.getError());
        }

        return inferCslStyleFromDocument(document.get());
    }

    private OOResult<Optional<CitationStyle>, OOError> inferCslStyleFromDocument(XTextDocument doc) {
        try {
            Optional<CitationStyle> citationStyle = ZoteroDocumentPreferences.findCitationStyle(doc, CSLStyleLoader.getStyles());
            citationStyle.ifPresent(openOfficePreferences::setCurrentStyle);
            return OOResult.ok(citationStyle);
        } catch (WrappedTargetException e) {
            LOGGER.warn("Could not read Zotero document preferences", e);
            return OOResult.error(OOError.fromMisc(e));
        }
    }

    OOVoidResult<OOError> writeDocumentCslStyle(CitationStyle citationStyle) {
        if (!isConnectedToDocument()) {
            return OOVoidResult.ok();
        }

        OOResult<XTextDocument, OOError> document = getXTextDocument();
        if (document.isError()) {
            return document.asVoidResult();
        }

        return writeDocumentCslStyle(document.get(), citationStyle);
    }

    static OOVoidResult<OOError> writeDocumentCslStyle(XTextDocument doc, CitationStyle citationStyle) {
        try {
            boolean result = ZoteroDocumentPreferences.writeCitationStyle(doc, citationStyle);
            if (!result) {
                return OOVoidResult.error(new OOError(
                        Localization.lang("Problem modifying citation"),
                        Localization.lang("Could not update document preferences.")));
            }
            return OOVoidResult.ok();
        } catch (IllegalTypeException | NotRemoveableException | PropertyVetoException | WrappedTargetException e) {
            LOGGER.warn("Could not update document CSL preferences", e);
            return OOVoidResult.error(OOError.fromMisc(e));
        }
    }

    /* ******************************************************
     *
     *  Tools to collect and show precondition test results
     *
     * ******************************************************/

    void showDialog(OOError err) {
        err.showErrorDialog(dialogService);
    }

    OOVoidResult<OOError> collectResults(String errorTitle, List<OOVoidResult<OOError>> results) {
        String msg = results.stream()
                            .filter(OOVoidResult::isError)
                            .map(e -> e.getError().getLocalizedMessage())
                            .collect(Collectors.joining("\n\n"));
        if (msg.isEmpty()) {
            return OOVoidResult.ok();
        } else {
            return OOVoidResult.error(new OOError(errorTitle, msg));
        }
    }

    boolean testDialog(OOVoidResult<OOError> res) {
        return res.ifError(ex -> ex.showErrorDialog(dialogService)).isError();
    }

    boolean testDialog(String errorTitle, OOVoidResult<OOError> res) {
        return res.ifError(e -> showDialog(e.setTitle(errorTitle))).isError();
    }

    @SafeVarargs
    final boolean testDialog(String errorTitle, OOVoidResult<OOError>... results) {
        List<OOVoidResult<OOError>> resultList = Arrays.asList(results);
        return testDialog(collectResults(errorTitle, resultList));
    }

    /// Get the cursor positioned by the user for inserting text.
    OOResult<XTextCursor, OOError> getUserCursorForTextInsertion(XTextDocument doc, String errorTitle) {
        // Get the cursor positioned by the user.
        XTextCursor cursor = UnoCursor.getViewCursor(doc).orElse(null);

        // Check for crippled XTextViewCursor
        Objects.requireNonNull(cursor);
        try {
            cursor.getStart();
        } catch (com.sun.star.uno.RuntimeException ex) {
            String msg =
                    Localization.lang("Please move the cursor"
                            + " to the location for the new citation.") + "\n"
                            + Localization.lang("I cannot insert to the cursor's current location.");
            return OOResult.error(new OOError(errorTitle, msg, ex));
        }
        return OOResult.ok(cursor);
    }

    /// This may move the view cursor.
    OOResult<FunctionalTextViewCursor, OOError> getFunctionalTextViewCursor(XTextDocument doc, String errorTitle) {
        String messageOnFailureToObtain =
                Localization.lang("Please move the cursor into the document text.")
                        + "\n"
                        + Localization.lang("To get the visual positions of your citations"
                        + " I need to move the cursor around,"
                        + " but could not get it.");
        OOResult<FunctionalTextViewCursor, String> result = FunctionalTextViewCursor.get(doc);
        if (result.isError()) {
            LOGGER.warn(result.getError());
        }
        return result.mapError(detail -> new OOError(errorTitle, messageOnFailureToObtain));
    }

    private static OOVoidResult<OOError> checkRangeOverlaps(XTextDocument doc, OOFrontend frontend) {
        final String errorTitle = "Overlapping ranges";
        boolean requireSeparation = false;
        int maxReportedOverlaps = 10;
        return frontend.checkRangeOverlaps(doc,
                               new ArrayList<>(),
                               requireSeparation,
                               maxReportedOverlaps)
                       .mapError(OOError::from);
    }

    private static OOVoidResult<OOError> checkRangeOverlapsWithCursor(XTextDocument doc, OOFrontend frontend, OOStyle style) {
        final String errorTitle = "Ranges overlapping with cursor";

        List<RangeForOverlapCheck<CitationGroupId>> userRanges;
        userRanges = frontend.viewCursorRanges(doc);

        boolean requireSeparation = false;
        OOVoidResult<JabRefException> res;
        res = frontend.checkRangeOverlapsWithCursor(doc,
                userRanges,
                requireSeparation,
                style);

        if (res.isError()) {
            final String xtitle = Localization.lang("The cursor is in a protected area.");
            return OOVoidResult.error(
                    new OOError(xtitle, xtitle + "\n" + res.getError().getLocalizedMessage() + "\n"));
        }
        return res.mapError(OOError::from);
    }

    /* ******************************************************
     *
     * Tests for preconditions.
     *
     * ******************************************************/

    private OOVoidResult<OOError> checkCitationMarkersOutsidePendingDeletions(XTextDocument doc, OOStyle style, OOFrontend frontend) {
        String errorTitle = Localization.lang("Recording and/or Recorded changes");
        try {
            // Keep the common path cheap: only run the overlap scan while Writer is actively
            // recording changes.
            if (!UnoRedlines.getRecordChanges(doc)) {
                return OOVoidResult.ok();
            }

            List<XTextRange> citationRanges = frontend.getCitationRanges(doc, style)
                                                      .stream()
                                                      .map(RangeForOverlapCheck::getRange)
                                                      .toList();
            if (UnoRedlines.countDeletedRangesTouching(doc, citationRanges) == 0) {
                return OOVoidResult.ok();
            }
            String msg = Localization.lang("Citations inside deletions that have not been accepted yet may reappear.")
                    + "\n"
                    + Localization.lang("Use [Edit]/[Track Changes]/[Manage] to resolve them first.");
            return OOVoidResult.error(new OOError(errorTitle, msg));
        } catch (WrappedTargetException ex) {
            String msg = Localization.lang("Error while checking if Writer is recording changes or has recorded changes.");
            return OOVoidResult.error(new OOError(errorTitle, msg, ex));
        } catch (NoDocumentException ex) {
            return OOVoidResult.error(OOError.from(ex).setTitle(errorTitle));
        }
    }

    /// Run a marker-rewriting action with change recording suspended.
    ///
    /// This prevents duplicated citations when Writer's change recording is enabled: deleting the
    /// old marker would otherwise only mark it as deleted, leaving old and new marker side by side.
    private <T> T supplyWithTrackChangesSuspended(XTextDocument doc, Supplier<T> action) {
        List<T> holder = new ArrayList<>(1);
        try {
            UnoRedlines.withRecordChangesSuspended(doc, () -> holder.add(action.get()));
        } catch (UnoRedlines.TrackChangesRestoreException ex) {
            LOGGER.warn("Could not restore change recording", ex);
            dialogService.showWarningDialogAndWait(
                    Localization.lang("Track changes"),
                    Localization.lang("JabRef updated the document, but could not restore Track Changes."
                            + " Please verify [Edit]/[Track Changes]/[Record]."));
            return holder.getFirst();
        } catch (WrappedTargetException ex) {
            LOGGER.warn("Could not suspend change recording", ex);
            if (!holder.isEmpty()) {
                return holder.getFirst();
            }
            return action.get();
        }
        return holder.getFirst();
    }

    OOVoidResult<OOError> styleIsRequired(OOStyle style) {
        if (style == null) {
            return OOVoidResult.error(OOError.noValidStyleSelected());
        } else {
            return OOVoidResult.ok();
        }
    }

    OOResult<OOFrontend, OOError> getFrontend(XTextDocument doc) {
        return OOFrontend.create(doc).mapError(OOError::from);
    }

    OOVoidResult<OOError> databaseIsRequired(List<BibDatabase> databases,
                                             Supplier<OOError> fun) {
        if (databases == null || databases.isEmpty()) {
            return OOVoidResult.error(fun.get());
        } else {
            return OOVoidResult.ok();
        }
    }

    OOVoidResult<OOError> selectedBibEntryIsRequired(List<BibEntry> entries,
                                                     Supplier<OOError> fun) {
        if (entries == null || entries.isEmpty()) {
            return OOVoidResult.error(fun.get());
        } else {
            return OOVoidResult.ok();
        }
    }

    /*
     * Checks existence and also checks if it is not an internal name.
     */
    private OOVoidResult<OOError> checkStyleExistsInTheDocument(String familyName,
                                                                String styleName,
                                                                XTextDocument doc,
                                                                String labelInJstyleFile,
                                                                String pathToStyleFile)
            throws
            WrappedTargetException {

        Optional<String> internalName = UnoStyle.getInternalNameOfStyle(doc, familyName, styleName);

        if (internalName.isEmpty()) {
            String msg =
                    switch (familyName) {
                        case UnoStyle.PARAGRAPH_STYLES ->
                                Localization.lang("The %0 paragraph style '%1' is missing from the document",
                                        labelInJstyleFile,
                                        styleName);
                        case UnoStyle.CHARACTER_STYLES ->
                                Localization.lang("The %0 character style '%1' is missing from the document",
                                        labelInJstyleFile,
                                        styleName);
                        default ->
                                throw new IllegalArgumentException("Expected " + UnoStyle.CHARACTER_STYLES
                                        + " or " + UnoStyle.PARAGRAPH_STYLES
                                        + " for familyName");
                    }
                            + "\n"
                            + Localization.lang("Please create it in the document or change in the file:")
                            + "\n"
                            + pathToStyleFile;
            return OOVoidResult.error(new OOError("StyleIsNotKnown", msg));
        }

        if (!internalName.get().equals(styleName)) {
            String msg =
                    switch (familyName) {
                        case UnoStyle.PARAGRAPH_STYLES ->
                                Localization.lang("The %0 paragraph style '%1' is a display name for '%2'.",
                                        labelInJstyleFile,
                                        styleName,
                                        internalName.get());
                        case UnoStyle.CHARACTER_STYLES ->
                                Localization.lang("The %0 character style '%1' is a display name for '%2'.",
                                        labelInJstyleFile,
                                        styleName,
                                        internalName.get());
                        default ->
                                throw new IllegalArgumentException("Expected " + UnoStyle.CHARACTER_STYLES
                                        + " or " + UnoStyle.PARAGRAPH_STYLES
                                        + " for familyName");
                    }
                            + "\n"
                            + Localization.lang("Please use the latter in the style file below"
                            + " to avoid localization problems.")
                            + "\n"
                            + pathToStyleFile;
            return OOVoidResult.error(new OOError("StyleNameIsNotInternal", msg));
        }
        return OOVoidResult.ok();
    }

    public OOVoidResult<OOError> checkStylesExistInTheDocument(JStyle jStyle, XTextDocument doc) {
        String pathToStyleFile = jStyle.getPath();

        List<OOVoidResult<OOError>> results = new ArrayList<>();
        try {
            results.add(checkStyleExistsInTheDocument(UnoStyle.PARAGRAPH_STYLES,
                    jStyle.getReferenceHeaderParagraphFormat(),
                    doc,
                    "ReferenceHeaderParagraphFormat",
                    pathToStyleFile));
            results.add(checkStyleExistsInTheDocument(UnoStyle.PARAGRAPH_STYLES,
                    jStyle.getReferenceParagraphFormat(),
                    doc,
                    "ReferenceParagraphFormat",
                    pathToStyleFile));
            if (jStyle.isFormatCitations()) {
                results.add(checkStyleExistsInTheDocument(UnoStyle.CHARACTER_STYLES,
                        jStyle.getCitationCharacterFormat(),
                        doc,
                        "CitationCharacterFormat",
                        pathToStyleFile));
            }
        } catch (WrappedTargetException ex) {
            results.add(OOVoidResult.error(new OOError("Other error in checkStyleExistsInTheDocument",
                    ex.getMessage(),
                    ex)));
        }

        return collectResults("checkStyleExistsInTheDocument failed", results);
    }

    /* ******************************************************
     *
     * ManageCitationsDialogView
     *
     * ******************************************************/
    public Optional<List<CitationEntry>> guiActionGetCitationEntries() {
        final Optional<List<CitationEntry>> FAIL = Optional.empty();
        final String errorTitle = Localization.lang("Problem collecting citations");

        OOResult<XTextDocument, OOError> odoc = getXTextDocument();
        if (testDialog(errorTitle, odoc.asVoidResult())) {
            return FAIL;
        }
        XTextDocument doc = odoc.get();

        OOResult<List<CitationEntry>, JabRefException> result = ManageCitations.getCitationEntries(doc);
        if (testDialog(errorTitle, result.asVoidResult().mapError(OOError::from))) {
            return FAIL;
        }
        return Optional.of(result.get());
    }

    /// Apply editable parts of citationEntries to the document: store pageInfo.
    ///
    /// Does not change presentation.
    ///
    /// Note: we use no undo context here, because only DocumentConnection.setUserDefinedStringPropertyValue() is called, and Undo in LO will not undo that.
    ///
    /// GUI: "Manage citations" dialog "OK" button. Called from: ManageCitationsDialogViewModel.storeSettings
    ///
    ///
    /// Currently the only editable part is pageInfo.
    ///
    /// Since the only call to applyCitationEntries() only changes pageInfo w.r.t those returned by getCitationEntries(), we can do with the following restrictions:
    ///
    /// -  Missing pageInfo means no action.
    /// -  Missing CitationEntry means no action (no attempt to remove
    /// citation from the text).
    public void guiActionApplyCitationEntries(List<CitationEntry> citationEntries) {
        final String errorTitle = Localization.lang("Problem modifying citation");

        OOResult<XTextDocument, OOError> odoc = getXTextDocument();
        if (testDialog(errorTitle, odoc.asVoidResult())) {
            return;
        }
        XTextDocument doc = odoc.get();

        testDialog(errorTitle, ManageCitations.applyCitationEntries(doc, citationEntries).mapError(OOError::from));
    }

    /// Creates a citation group from `entries` at the cursor.
    ///
    /// Uses LO undo context "Insert citation".
    ///
    /// Note: Undo does not remove or reestablish custom properties.
    ///
    /// Consistency: for each entry in `entries`: looking it up in `syncOptions.get().databases` (if present) should yield `database`.
    ///
    /// @param entries            The entries to cite.
    /// @param bibDatabaseContext The database the entries belong to (all of them). Used when creating the citation mark.
    /// @param selectedDatabases  The databases selected for citation lookup in this action.
    /// @param style              The bibliography style we are using.
    /// @param citationType       Indicates whether it is an in-text citation, a citation in parenthesis or an invisible citation.
    /// @param pageInfo           A single page-info for these entries. Attributed to the last entry.
    /// @param syncOptions        Indicates whether in-text citations should be refreshed in the document. Optional.empty() indicates no refresh. Otherwise, provides options for refreshing the reference list.
    public void guiActionInsertEntry(List<BibEntry> entries,
                                     BibDatabaseContext bibDatabaseContext,
                                     List<BibDatabase> selectedDatabases,
                                     OOStyle style,
                                     CitationType citationType,
                                     String pageInfo,
                                     Optional<Update.SyncOptions> syncOptions) {
        final String errorTitle = "Could not insert citation";

        OOResult<XTextDocument, OOError> odoc = getXTextDocument();
        if (testDialog(errorTitle,
                odoc.asVoidResult(),
                styleIsRequired(style),
                selectedBibEntryIsRequired(entries, OOError::noEntriesSelectedForCitation))) {
            return;
        }
        XTextDocument doc = odoc.get();

        OOResult<OOFrontend, OOError> frontend = getFrontend(doc);
        if (testDialog(errorTitle, frontend.asVoidResult())) {
            return;
        }

        OOResult<XTextCursor, OOError> cursor = getUserCursorForTextInsertion(doc, errorTitle);
        if (testDialog(errorTitle, cursor.asVoidResult())) {
            return;
        }

        if (testDialog(errorTitle, checkRangeOverlapsWithCursor(doc, frontend.get(), style))) {
            return;
        }

        if (style instanceof JStyle jStyle) {
            OOVoidResult<OOError> pendingDeletionOverlap = syncOptions.isPresent()
                                                           ? checkCitationMarkersOutsidePendingDeletions(doc, style, frontend.get())
                                                           : OOVoidResult.ok();
            if (testDialog(errorTitle,
                    checkStylesExistInTheDocument(jStyle, doc),
                    pendingDeletionOverlap)) {
                return;
            }
        }

        /*
         * For sync we need a FunctionalTextViewCursor and an open database.
         */
        OOResult<FunctionalTextViewCursor, OOError> fcursor = null;
        if (syncOptions.isPresent()) {
            fcursor = getFunctionalTextViewCursor(doc, errorTitle);
            syncOptions.map(e -> e.setAlwaysAddCitedOnPages(openOfficePreferences.getAlwaysAddCitedOnPages()));
            if (testDialog(errorTitle, fcursor.asVoidResult()) || testDialog(databaseIsRequired(syncOptions.get().databases,
                    OOError::noDataBaseIsOpenForSyncingAfterCitation))) {
                return;
            }
        }

        try {

            UnoUndo.enterUndoContext(doc, "Insert citation");
            OOVoidResult<OOError> result = OOVoidResult.ok();
            if (style instanceof CitationStyle citationStyle) {
                // Handle insertion of CSL Style citations
                result = insertCSLCitation(entries,
                        doc,
                        citationType,
                        citationStyle,
                        bibDatabaseContext,
                        selectedDatabases,
                        cursor,
                        syncOptions);
            } else if (style instanceof BstStyle bstStyle) {
                // Handle insertion of BST citations
                result = insertBstCitation(entries, doc, bstStyle, bibDatabaseContext, cursor);
            } else if (style instanceof JStyle jStyle) {
                // Handle insertion of JStyle citations
                result = insertJStyleCitation(entries,
                        doc,
                        citationType,
                        jStyle,
                        frontend,
                        cursor,
                        bibDatabaseContext,
                        syncOptions,
                        pageInfo,
                        fcursor);
            }
            testDialog(errorTitle, result);
        } finally {
            UnoUndo.leaveUndoContext(doc);
        }
    }

    /// Helper method for guiActionInsertEntry. Handles CSL citation insertion
    /// Throws CreationException, com.sun.star.uno.Exception
    /// Caught by guiActionInsertEntry
    ///
    /// @param entries             The entries to cite.
    /// @param currentEntryContext The database the entries belong to. Used when creating the citation mark.
    /// @param selectedDatabases   The databases selected for resolving existing CSL citations during this action.
    /// @param citationType        Indicates whether it is an in-text citation, a citation in parenthesis or an invisible citation.
    /// @param citationStyle       Indicates style, name and path of citation
    /// @param syncOptions         Indicates whether in-text citations should be refreshed in the document. Optional.empty() indicates no refresh. Otherwise, provides options for refreshing the reference list.
    public OOVoidResult<OOError> insertCSLCitation(List<BibEntry> entries,
                                                   XTextDocument doc,
                                                   CitationType citationType,
                                                   CitationStyle citationStyle,
                                                   BibDatabaseContext currentEntryContext,
                                                   List<BibDatabase> selectedDatabases,
                                                   OOResult<XTextCursor, OOError> cursor,
                                                   Optional<Update.SyncOptions> syncOptions) {

        boolean convertReferenceMarks;
        try {
            convertReferenceMarks = cslCitationOOAdapter.needsReferenceMarkConversion();
            if (convertReferenceMarks) {
                dialogService.showWarningDialogAndWait(
                        Localization.lang("Reference mark format"),
                        Localization.lang("Converting references to selected format"));
            }
        } catch (com.sun.star.uno.Exception e) {
            return OOVoidResult.error(OOError.fromMisc(e));
        }

        OOVoidResult<OOError> documentPreferencesResult = writeDocumentCslStyle(doc, citationStyle);
        if (documentPreferencesResult.isError()) {
            return documentPreferencesResult;
        }

        try {
            // Lock document controllers - disable refresh during the process (avoids document flicker during writing)
            // MUST always be paired with an unlockControllers() call
            doc.lockControllers();

            OOVoidResult<OOError> insertResult = supplyWithTrackChangesSuspended(doc, () -> {
                try {
                    if (convertReferenceMarks) {
                        cslCitationOOAdapter.convertReferenceMarksToPreference(selectedDatabases);
                    }

                    if (citationType == CitationType.AUTHORYEAR_PAR) {
                        // If current citation style is not the same as passed-in citation type, then change it to the new citation style.
                        // If current citation type is not "NORMAL", then change it to "NORMAL".
                        // Placing this at the beginning reduces the number of updates needed by 1 (in the positive case).
                        cslCitationOOAdapter.prepareCitationInsertion(citationStyle, CSLCitationType.NORMAL, currentEntryContext, selectedDatabases);
                        // "Cite" button
                        cslCitationOOAdapter.insertCitation(cursor.get(), citationStyle, entries, currentEntryContext);
                    } else if (citationType == CitationType.AUTHORYEAR_INTEXT) {
                        cslCitationOOAdapter.prepareCitationInsertion(citationStyle, CSLCitationType.IN_TEXT, currentEntryContext, selectedDatabases);
                        // "Cite in-text" button
                        cslCitationOOAdapter.insertInTextCitation(cursor.get(), citationStyle, entries, currentEntryContext);
                    } else if (citationType == CitationType.INVISIBLE_CIT) {
                        cslCitationOOAdapter.prepareCitationInsertion(citationStyle, CSLCitationType.EMPTY, currentEntryContext, selectedDatabases);
                        // "Insert empty citation"
                        cslCitationOOAdapter.insertEmptyCitation(cursor.get(), citationStyle, entries, currentEntryContext);
                    }
                    return OOVoidResult.ok();
                } catch (CreationException | com.sun.star.uno.Exception e) {
                    return OOVoidResult.error(OOError.fromMisc(e));
                }
            });
            if (insertResult.isError()) {
                return insertResult;
            }

            // If "Automatically sync bibliography when inserting citations" is enabled
            if (citationStyle.hasBibliography()) {
                syncOptions.ifPresent(options -> guiActionUpdateDocument(options.databases, citationStyle));
            }
            return OOVoidResult.ok();
        } finally {
            // Release controller lock
            doc.unlockControllers();
        }
    }

    /// Helper method for guiActionInsertEntry
    /// Throws PropertyVetoException, WrappedTargetException, IllegalTypeException, NotRemoveableException, CreationException, NoDocumentException
    /// Exceptions caught by guiActionInsertEntry
    ///
    /// @param entries            The entries to cite.
    /// @param citationType       Indicates whether it is an in-text citation, a citation in parentheses or an invisible citation.
    /// @param jStyle             Indicates citation formating in JStyle
    /// @param bibDatabaseContext The database the entries belong to (all of them). Used when creating the citation mark.
    /// @param syncOptions        Indicates whether in-text citations should be refreshed in the document. Optional.empty() indicates no refresh. Otherwise, provides options for refreshing the reference list.
    /// @param pageInfo           A single page-info for these entries. Attributed to the last entry.
    public OOVoidResult<OOError> insertJStyleCitation(List<BibEntry> entries,
                                                      XTextDocument doc,
                                                      CitationType citationType,
                                                      JStyle jStyle,
                                                      OOResult<OOFrontend, OOError> frontend,
                                                      OOResult<XTextCursor, OOError> cursor,
                                                      BibDatabaseContext bibDatabaseContext,
                                                      Optional<Update.SyncOptions> syncOptions,
                                                      String pageInfo,
                                                      OOResult<FunctionalTextViewCursor, OOError> fcursor) {
        OOVoidResult<OOError> insertResult = EditInsert.insertCitationGroup(doc,
                componentContext,
                frontend.get(),
                cursor.get(),
                entries,
                bibDatabaseContext.getDatabase(),
                jStyle,
                citationType,
                pageInfo,
                openOfficePreferences.getAddSpaceBefore(),
                openOfficePreferences.getAddSpaceAfter()).mapError(OOError::from);

        if (insertResult.isError()) {
            return insertResult;
        }

        if (syncOptions.isPresent()) {
            return supplyWithTrackChangesSuspended(doc,
                    () -> Update.resyncDocument(doc, jStyle, fcursor.get(), syncOptions.get())
                                .asVoidResult().mapError(OOError::from));
        }
        return OOVoidResult.ok();
    }

    /// Helper method for guiActionInsertEntry - handles BST citation insertion.
    private OOVoidResult<OOError> insertBstCitation(List<BibEntry> entries,
                                                    XTextDocument doc,
                                                    BstStyle bstStyle,
                                                    BibDatabaseContext bibDatabaseContext,
                                                    OOResult<XTextCursor, OOError> cursor) {
        try {
            doc.lockControllers();
            return supplyWithTrackChangesSuspended(doc, () -> {
                try {
                    bstCitationOOAdapter.insertCitation(cursor.get(), entries, bibDatabaseContext);
                    return OOVoidResult.ok();
                } catch (MissingStyleDefinedCitationLabelException e) {
                    return OOVoidResult.error(OOError.bstStyleDoesNotDefineCitationFormat());
                } catch (CreationException | com.sun.star.uno.Exception e) {
                    return OOVoidResult.error(OOError.fromMisc(e));
                }
            });
        } finally {
            doc.unlockControllers();
        }
    }

    /// GUI action "Merge citations"
    public void guiActionMergeCitationGroups(List<BibDatabase> databases, OOStyle style) {
        final String errorTitle = Localization.lang("Problem combining cite markers");

        if (style instanceof JStyle jStyle) {
            OOResult<XTextDocument, OOError> odoc = getXTextDocument();
            if (testDialog(errorTitle,
                    odoc.asVoidResult(),
                    styleIsRequired(jStyle),
                    databaseIsRequired(databases, OOError::noDataBaseIsOpen))) {
                return;
            }
            XTextDocument doc = odoc.get();

            OOResult<FunctionalTextViewCursor, OOError> fcursor = getFunctionalTextViewCursor(doc, errorTitle);

            if (testDialog(errorTitle,
                    fcursor.asVoidResult(),
                    checkStylesExistInTheDocument(jStyle, doc))) {
                return;
            }

            try {
                UnoUndo.enterUndoContext(doc, "Merge citations");

                OOResult<OOFrontend, OOError> ofrontend = getFrontend(doc);
                if (testDialog(errorTitle, ofrontend.asVoidResult())) {
                    return;
                }
                OOFrontend frontend = ofrontend.get();
                if (testDialog(errorTitle, checkCitationMarkersOutsidePendingDeletions(doc, jStyle, frontend))) {
                    return;
                }

                OOResult<Boolean, JabRefException> mergeResult = supplyWithTrackChangesSuspended(doc,
                        () -> EditMerge.mergeCitationGroups(doc, componentContext, frontend, jStyle));
                if (testDialog(errorTitle, mergeResult.asVoidResult().mapError(OOError::from))) {
                    return;
                }

                if (mergeResult.get()) {
                    OOResult<List<String>, JabRefException> syncResult = supplyWithTrackChangesSuspended(doc, () -> {
                        UnoCrossRef.refresh(doc);
                        Update.SyncOptions syncOptions = new Update.SyncOptions(databases);
                        return Update.resyncDocument(doc, jStyle, fcursor.get(), syncOptions);
                    });
                    testDialog(errorTitle, syncResult.asVoidResult().mapError(OOError::from));
                }
            } finally {
                UnoUndo.leaveUndoContext(doc);
                fcursor.get().restore(doc);
            }
        }
    } // MergeCitationGroups

    /// GUI action "Separate citations".
    ///
    /// Do the opposite of MergeCitationGroups. Combined markers are split, with a space inserted between.
    public void guiActionSeparateCitations(List<BibDatabase> databases, OOStyle style) {
        final String errorTitle = Localization.lang("Problem during separating cite markers");

        if (style instanceof JStyle jStyle) {
            OOResult<XTextDocument, OOError> odoc = getXTextDocument();
            if (testDialog(errorTitle,
                    odoc.asVoidResult(),
                    styleIsRequired(jStyle),
                    databaseIsRequired(databases, OOError::noDataBaseIsOpen))) {
                return;
            }

            XTextDocument doc = odoc.get();

            OOResult<FunctionalTextViewCursor, OOError> fcursor = getFunctionalTextViewCursor(doc, errorTitle);

            if (testDialog(errorTitle,
                    fcursor.asVoidResult(),
                    checkStylesExistInTheDocument(jStyle, doc))) {
                return;
            }

            try {
                UnoUndo.enterUndoContext(doc, "Separate citations");

                OOResult<OOFrontend, OOError> ofrontend = getFrontend(doc);
                if (testDialog(errorTitle, ofrontend.asVoidResult())) {
                    return;
                }
                OOFrontend frontend = ofrontend.get();
                if (testDialog(errorTitle, checkCitationMarkersOutsidePendingDeletions(doc, jStyle, frontend))) {
                    return;
                }

                OOResult<Boolean, JabRefException> separateResult = supplyWithTrackChangesSuspended(doc,
                        () -> EditSeparate.separateCitations(doc, componentContext, frontend, databases, jStyle));
                if (testDialog(errorTitle, separateResult.asVoidResult().mapError(OOError::from))) {
                    return;
                }

                if (separateResult.get()) {
                    OOResult<List<String>, JabRefException> syncResult = supplyWithTrackChangesSuspended(doc, () -> {
                        UnoCrossRef.refresh(doc);
                        Update.SyncOptions syncOptions = new Update.SyncOptions(databases);
                        return Update.resyncDocument(doc, jStyle, fcursor.get(), syncOptions);
                    });
                    testDialog(errorTitle, syncResult.asVoidResult().mapError(OOError::from));
                }
            } finally {
                UnoUndo.leaveUndoContext(doc);
                fcursor.get().restore(doc);
            }
        }
    }

    /// GUI action for "Export cited"
    ///
    /// Does not refresh the bibliography.
    ///
    /// @param returnPartialResult If there are some unresolved keys, shall we return an otherwise nonempty result, or Optional.empty()?
    public Optional<BibDatabase> exportCitedHelper(List<BibDatabase> databases, OOStyle style, boolean returnPartialResult) {
        final Optional<BibDatabase> FAIL = Optional.empty();
        final String errorTitle = Localization.lang("Unable to generate new library");

        OOResult<XTextDocument, OOError> odoc = getXTextDocument();
        if (testDialog(errorTitle,
                odoc.asVoidResult(),
                databaseIsRequired(databases, OOError::noDataBaseIsOpenForExport))) {
            return FAIL;
        }
        XTextDocument doc = odoc.get();

        ExportCited.GenerateDatabaseResult result = null;
        try {
            UnoUndo.enterUndoContext(doc, "Changes during \"Export cited\"");
            OOResult<ExportCited.GenerateDatabaseResult, JabRefException> generateResult;
            if (style instanceof CitationStyle) {
                generateResult = exportCitedForCSL(databases);
            } else if (style instanceof BstStyle) {
                generateResult = exportCitedForBST(databases);
            } else {
                generateResult = ExportCited.generateDatabase(doc, databases);
            }
            if (testDialog(errorTitle, generateResult.asVoidResult().mapError(OOError::from))) {
                return FAIL;
            }
            result = generateResult.get();
        } finally {
            // There should be no changes, thus no Undo entry should appear
            // in LibreOffice.
            UnoUndo.leaveUndoContext(doc);
        }

        if (!result.newDatabase.hasEntries()) {
            dialogService.showErrorDialogAndWait(
                    errorTitle,
                    Localization.lang("Your OpenOffice/LibreOffice document references"
                            + " no citation keys"
                            + " which could also be found in your current library."));
            return FAIL;
        }

        List<String> unresolvedKeys = result.unresolvedKeys;
        if (!unresolvedKeys.isEmpty()) {
            dialogService.showErrorDialogAndWait(
                    errorTitle,
                    Localization.lang("Your OpenOffice/LibreOffice document references"
                                    + " at least %0 citation keys"
                                    + " which could not be found in your current library."
                                    + " Some of these are %1.",
                            String.valueOf(unresolvedKeys.size()),
                            String.join(", ", unresolvedKeys)));
            if (returnPartialResult) {
                return Optional.of(result.newDatabase);
            } else {
                return FAIL;
            }
        }
        return Optional.of(result.newDatabase);
    }

    private OOResult<ExportCited.GenerateDatabaseResult, JabRefException> exportCitedForCSL(List<BibDatabase> databases) {
        assert cslCitationOOAdapter != null;

        try {
            return OOResult.ok(ExportCited.generateDatabaseFromCitationKeys(cslCitationOOAdapter.getCitedCitationKeys(), databases));
        } catch (WrappedTargetException | NoSuchElementException e) {
            return OOResult.error(new JabRefException(e.getMessage(), e));
        }
    }

    private OOResult<ExportCited.GenerateDatabaseResult, JabRefException> exportCitedForBST(List<BibDatabase> databases) {
        assert bstCitationOOAdapter != null;

        try {
            return OOResult.ok(ExportCited.generateDatabaseFromIdentifiers(bstCitationOOAdapter.getCitedIdentifiers(), databases));
        } catch (WrappedTargetException | NoSuchElementException e) {
            return OOResult.error(new JabRefException(e.getMessage(), e));
        }
    }

    /// GUI action, refreshes citation markers and bibliography.
    ///
    /// @param databases Must have at least one.
    /// @param style     Style.
    public void guiActionUpdateDocument(List<BibDatabase> databases, OOStyle style) {
        final String errorTitle = Localization.lang("Unable to synchronize bibliography");

        OOResult<XTextDocument, OOError> odoc = getXTextDocument();
        if (testDialog(errorTitle,
                odoc.asVoidResult(),
                styleIsRequired(style))) {
            return;
        }

        XTextDocument doc = odoc.get();

        OOResult<FunctionalTextViewCursor, OOError> fcursor = getFunctionalTextViewCursor(doc, errorTitle);

        if (style instanceof JStyle jStyle) {
            if (testDialog(errorTitle,
                    fcursor.asVoidResult(),
                    checkStylesExistInTheDocument(jStyle, doc))) {
                return;
            }

            OOResult<OOFrontend, OOError> ofrontend = getFrontend(doc);
            if (testDialog(errorTitle, ofrontend.asVoidResult())) {
                return;
            }
            OOFrontend frontend = ofrontend.get();

            if (testDialog(errorTitle,
                    checkRangeOverlaps(doc, frontend),
                    checkCitationMarkersOutsidePendingDeletions(doc, jStyle, frontend))) {
                return;
            }

            testDialog(errorTitle, supplyWithTrackChangesSuspended(doc,
                    () -> updateJStyleBibliography(databases, jStyle, doc, frontend, fcursor, errorTitle)));
        } else if (style instanceof CitationStyle citationStyle) {
            if (!citationStyle.hasBibliography()) {
                return;
            }
            OOResult<OOFrontend, OOError> ofrontend = getFrontend(doc);
            if (testDialog(errorTitle,
                    fcursor.asVoidResult(),
                    ofrontend.asVoidResult())) {
                return;
            }
            testDialog(errorTitle,
                    checkCitationMarkersOutsidePendingDeletions(doc, citationStyle, ofrontend.get()),
                    supplyWithTrackChangesSuspended(doc,
                            () -> updateCSLBibliography(databases, citationStyle, doc, fcursor, errorTitle)));
        } else if (style instanceof BstStyle bstStyle) {
            OOResult<OOFrontend, OOError> ofrontend = getFrontend(doc);
            if (testDialog(errorTitle,
                    fcursor.asVoidResult(),
                    ofrontend.asVoidResult())) {
                return;
            }
            testDialog(errorTitle,
                    checkCitationMarkersOutsidePendingDeletions(doc, bstStyle, ofrontend.get()),
                    supplyWithTrackChangesSuspended(doc,
                            () -> updateBstBibliography(databases, bstStyle, doc, fcursor, errorTitle)));
        }
    }

    /// Helper method for guiActionUpdateDocument, refreshes a JStyle bibliography.
    ///
    /// @param databases        Must have at least one.
    /// @param jStyle           Indicates citation formating in JStyle.
    /// @param doc              Text document.
    /// @param frontend,fcursor Used to synchronize document.
    /// @param errorTitle       Error message for user.
    private OOVoidResult<OOError> updateJStyleBibliography(List<BibDatabase> databases, JStyle jStyle, XTextDocument doc, OOFrontend frontend,
                                                           OOResult<FunctionalTextViewCursor, OOError> fcursor, String errorTitle) {
        OOResult<List<String>, JabRefException> syncResult;
        try {
            UnoUndo.enterUndoContext(doc, "Refresh bibliography");

            Update.SyncOptions syncOptions = new Update.SyncOptions(databases);
            syncOptions
                    .setUpdateBibliography(true)
                    .setAlwaysAddCitedOnPages(openOfficePreferences.getAlwaysAddCitedOnPages());

            syncResult = Update.synchronizeDocument(doc, frontend, jStyle, fcursor.get(), syncOptions);
        } finally {
            UnoUndo.leaveUndoContext(doc);
            fcursor.get().restore(doc);
        }
        if (syncResult.isError()) {
            return OOVoidResult.error(OOError.from(syncResult.getError()).setTitle(errorTitle));
        }
        List<String> unresolvedKeys = syncResult.get();
        if (!unresolvedKeys.isEmpty()) {
            String msg = Localization.lang(
                    "Your OpenOffice/LibreOffice document references the citation key '%0',"
                            + " which could not be found in your current library.",
                    unresolvedKeys.getFirst());
            dialogService.showErrorDialogAndWait(errorTitle, msg);
        }
        return OOVoidResult.ok();
    }

    /// Helper method for guiActionUpdateDocument - refreshes a BST bibliography.
    ///
    /// @param databases  Must have at least one.
    /// @param bstStyle   BST style to update the bibliography with.
    /// @param doc        Text document.
    /// @param fcursor    Used to synchronize document.
    /// @param errorTitle Error message for user.
    private OOVoidResult<OOError> updateBstBibliography(List<BibDatabase> databases, BstStyle bstStyle,
                                                        XTextDocument doc,
                                                        OOResult<FunctionalTextViewCursor, OOError> fcursor,
                                                        String errorTitle) {
        try {
            UnoUndo.enterUndoContext(doc, "Create BST bibliography");

            try {
                bstCitationOOAdapter.refreshCitationState();
            } catch (WrappedTargetException | NoSuchElementException exception) {
                LOGGER.error("Could not refresh BST citation state", exception);
                return OOVoidResult.error(OOError.fromMisc(exception).setTitle(errorTitle));
            }

            List<BibEntry> citedEntries = databases.stream()
                                                   .flatMap(db -> db.getEntries().stream())
                                                   .filter(bstCitationOOAdapter::isCitedEntry)
                                                   .collect(Collectors.toCollection(ArrayList::new)); // has to be a mutable list as it undergoes sorting

            if (citedEntries.isEmpty()) {
                dialogService.showInformationDialogAndWait(
                        Localization.lang("Bibliography"),
                        Localization.lang("No cited entries found in the document."));
                return OOVoidResult.ok();
            }

            BibDatabase bibDatabase = new BibDatabase(citedEntries);
            BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(bibDatabase);

            doc.lockControllers();
            try {
                bstUpdateBibliography.rebuildBstBibliography(
                        doc, bstCitationOOAdapter, bstStyle, citedEntries, bibDatabaseContext);
            } catch (MissingStyleDefinedCitationLabelException e) {
                return OOVoidResult.error(OOError.bstStyleDoesNotDefineCitationFormat());
            } catch (IOException | InterruptedException | com.sun.star.uno.Exception | CreationException e) {
                LOGGER.error("Could not update BST bibliography", e);
                return OOVoidResult.error(OOError.fromMisc(e).setTitle(errorTitle));
            } catch (NoDocumentException e) {
                LOGGER.error("Could not update BST bibliography", e);
                return OOVoidResult.error(OOError.from(e).setTitle(errorTitle));
            } finally {
                doc.unlockControllers();
            }
        } finally {
            UnoUndo.leaveUndoContext(doc);
            fcursor.get().restore(doc);
        }
        return OOVoidResult.ok();
    }

    /// Helper method for guiActionUpdateDocument, refreshes a CSL bibliography.
    ///
    /// @param databases     Must have at least one.
    /// @param citationStyle Citation style to update bibliography with.
    /// @param doc           Text document.
    /// @param fcursor       Used to synchronize document.
    /// @param errorTitle    Error message for user.
    private OOVoidResult<OOError> updateCSLBibliography(List<BibDatabase> databases, CitationStyle citationStyle, XTextDocument doc,
                                                        OOResult<FunctionalTextViewCursor, OOError> fcursor, String errorTitle) {
        OOVoidResult<OOError> documentPreferencesResult = writeDocumentCslStyle(doc, citationStyle);
        if (documentPreferencesResult.isError()) {
            return documentPreferencesResult;
        }

        return updateCSLBibliography(dialogService, databases, citationStyle, doc, fcursor, errorTitle, cslCitationOOAdapter, cslUpdateBibliography);
    }

    static OOVoidResult<OOError> updateCSLBibliography(DialogService dialogService,
                                                       List<BibDatabase> databases,
                                                       CitationStyle citationStyle,
                                                       XTextDocument doc,
                                                       OOResult<FunctionalTextViewCursor, OOError> fcursor,
                                                       String errorTitle,
                                                       CSLCitationOOAdapter cslCitationOOAdapter,
                                                       CSLUpdateBibliography cslUpdateBibliography) {
        try {
            UnoUndo.enterUndoContext(doc, "Create CSL bibliography");

            // Collect entries from the selected databases, depending on whether the OpenOffice panel's "currently selected library only" preference is enabled.
            List<BibEntry> entries = databases.stream()
                                              .flatMap(database -> database.getEntries().stream())
                                              .toList();

            cslCitationOOAdapter.linkZoteroCitations(new BibDatabaseContext(new BibDatabase(entries)));
            try {
                cslCitationOOAdapter.refreshCitationState();
            } catch (WrappedTargetException | NoSuchElementException exception) {
                LOGGER.error("Could not refresh CSL citation state", exception);
                return OOVoidResult.error(OOError.fromMisc(exception).setTitle(errorTitle));
            }

            List<BibEntry> citedEntries = entries.stream()
                                                 .filter(cslCitationOOAdapter::isCitedEntry)
                                                 .collect(Collectors.toCollection(ArrayList::new)); // has to be a mutable list as it undergoes sorting

            // If no entries are cited, show a message and return
            if (citedEntries.isEmpty()) {
                dialogService.showInformationDialogAndWait(
                        Localization.lang("Bibliography"),
                        Localization.lang("No cited entries found in the document.")
                );
                return OOVoidResult.ok();
            }

            // Lock document controllers - disable refresh during the process (avoids document flicker during writing)
            // MUST always be paired with an unlockControllers() call
            doc.lockControllers();
            try {
                cslUpdateBibliography.rebuildCSLBibliography(doc, cslCitationOOAdapter, citedEntries, databases, citationStyle);
            } catch (CreationException | com.sun.star.uno.Exception e) {
                LOGGER.error("Could not update CSL bibliography", e);
                return OOVoidResult.error(OOError.fromMisc(e).setTitle(errorTitle));
            } catch (NoDocumentException e) {
                LOGGER.error("Could not update CSL bibliography", e);
                return OOVoidResult.error(OOError.from(e).setTitle(errorTitle));
            } finally {
                doc.unlockControllers();
            }
        } finally {
            UnoUndo.leaveUndoContext(doc);
            fcursor.get().restore(doc);
        }
        return OOVoidResult.ok();
    }
}
