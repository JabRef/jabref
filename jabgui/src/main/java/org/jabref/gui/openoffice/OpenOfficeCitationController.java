package org.jabref.gui.openoffice;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.gui.DialogService;
import org.jabref.logic.JabRefException;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.engine.CitationEngine;
import org.jabref.logic.openoffice.engine.CitedEntryExporter;
import org.jabref.logic.openoffice.engine.StyleCapabilities;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.UnoCursor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.CitationEntry;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOResult;
import org.jabref.model.openoffice.util.OOVoidResult;

import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Connects Writer actions to the citation engine and presents their results to the user.
/// Document mutation, style-family dispatch, and the Zotero strategy belong to the engine.
@NullMarked
public class OpenOfficeCitationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenOfficeCitationController.class);
    private static final StyleCapabilities NO_CAPABILITIES = new StyleCapabilities(Set.of(), false, false, false);

    private final DialogService dialogService;
    private final OpenOfficeDocumentConnection connection;
    private final XComponentContext componentContext;
    private final OpenOfficePreferences openOfficePreferences;
    private final BibEntryTypesManager bibEntryTypesManager;
    private Optional<CitationEngine> engine = Optional.empty();
    private Optional<XTextDocument> engineDocument = Optional.empty();

    public OpenOfficeCitationController(Path loPath, DialogService dialogService, OpenOfficePreferences openOfficePreferences, BibEntryTypesManager bibEntryTypesManager)
            throws BootstrapException, CreationException, IOException, InterruptedException {
        this(new OpenOfficeDocumentConnection(loPath, dialogService), dialogService, openOfficePreferences, bibEntryTypesManager);
    }

    OpenOfficeCitationController(OpenOfficeDocumentConnection connection, DialogService dialogService, OpenOfficePreferences openOfficePreferences, BibEntryTypesManager bibEntryTypesManager) {
        this.connection = connection;
        this.dialogService = dialogService;
        this.componentContext = connection.getComponentContext();
        this.openOfficePreferences = openOfficePreferences;
        this.bibEntryTypesManager = bibEntryTypesManager;
    }

    public void dispose() {
        engine.ifPresent(CitationEngine::close);
        engine = Optional.empty();
        engineDocument = Optional.empty();
    }

    public void selectDocument(boolean autoSelectForSingle) throws WrappedTargetException, NoSuchElementException {
        if (showErrorIfPresent(connection.selectDocument(autoSelectForSingle))) {
            return;
        }
        if (isConnectedToDocument()) {
            OOResult<XTextDocument, OOError> selected = getXTextDocument();
            if (!showErrorIfPresent(selected.asVoidResult())) {
                if (engineDocument.filter(previous -> UnoRuntime.areSame(previous, selected.get())).isEmpty()) {
                    dispose();
                }
                dialogService.notify(Localization.lang("Connected to document: %0", getCurrentDocumentTitle().orElse("")));
            }
        }
    }

    public boolean isConnectedToDocument() {
        return connection.isConnectedToDocument();
    }

    public boolean isDocumentConnectionMissing() {
        boolean missing = connection.isDocumentConnectionMissing();
        if (missing) {
            dispose();
        }
        return missing;
    }

    public OOResult<XTextDocument, OOError> getXTextDocument() {
        OOResult<XTextDocument, OOError> document = connection.getXTextDocument();
        if (document.isError()) {
            dispose();
        }
        return document;
    }

    public Optional<String> getCurrentDocumentTitle() {
        return connection.getCurrentDocumentTitle();
    }

    private OOResult<CitationEngine, OOError> getEngine() {
        OOResult<XTextDocument, OOError> document = getXTextDocument();
        if (document.isError()) {
            return OOResult.error(document.getError());
        }
        XTextDocument selected = document.get();
        if (engineDocument.filter(previous -> UnoRuntime.areSame(previous, selected)).isEmpty()) {
            dispose();
            engine = Optional.of(new CitationEngine(selected, componentContext, openOfficePreferences, bibEntryTypesManager));
            engineDocument = Optional.of(selected);
        }
        return OOResult.ok(engine.orElseThrow());
    }

    public StyleCapabilities capabilities(OOStyle style) {
        return getEngine().map(current -> current.capabilities(style)).getOptional().orElse(NO_CAPABILITIES);
    }

    OOResult<Optional<OOStyle>, OOError> restoreStyleFromDocument(OpenOfficeStyleCatalog styles) {
        OOResult<XTextDocument, OOError> document = getXTextDocument();
        if (document.isError()) {
            return OOResult.error(document.getError());
        }
        try {
            Optional<OOStyle> restored = styles.restore(document.get(), openOfficePreferences.getZoteroCompatibilityMode());
            restored.ifPresent(openOfficePreferences::setCurrentStyle);
            return OOResult.ok(restored);
        } catch (WrappedTargetException exception) {
            LOGGER.warn("Could not read the citation style stored in the Writer document", exception);
            return OOResult.error(OOError.fromMisc(exception));
        }
    }

    OOResult<Optional<CitationStyle>, OOError> inferCslStyleFromDocument(OpenOfficeStyleCatalog styles) {
        OOResult<XTextDocument, OOError> document = getXTextDocument();
        if (document.isError()) {
            return OOResult.error(document.getError());
        }
        try {
            Optional<CitationStyle> inferred = styles.inferCslStyle(document.get());
            inferred.ifPresent(openOfficePreferences::setCurrentStyle);
            return OOResult.ok(inferred);
        } catch (WrappedTargetException exception) {
            LOGGER.warn("Could not infer the CSL citation style from the Writer document", exception);
            return OOResult.error(OOError.fromMisc(exception));
        }
    }

    void showDialog(OOError error) {
        error.showErrorDialog(dialogService);
    }

    boolean showErrorIfPresent(OOVoidResult<OOError> result) {
        return result.ifError(this::showDialog).isError();
    }

    boolean showErrorIfPresent(String title, OOVoidResult<OOError> result) {
        return result.ifError(error -> showDialog(error.setTitle(title))).isError();
    }

    OOResult<XTextCursor, OOError> getUserCursorForTextInsertion(XTextDocument document, String title) {
        try {
            Optional<XTextCursor> cursor = UnoCursor.getViewCursor(document).map(viewCursor -> viewCursor);
            if (cursor.isPresent()) {
                cursor.orElseThrow().getStart();
                return OOResult.ok(cursor.orElseThrow());
            }
        } catch (com.sun.star.uno.RuntimeException exception) {
            LOGGER.debug("Could not obtain the Writer insertion cursor", exception);
        }
        return OOResult.error(new OOError(title,
                Localization.lang("Please move the cursor to the location for the new citation.") + "\n"
                        + Localization.lang("I cannot insert to the cursor's current location.")));
    }

    public Optional<List<CitationEntry>> getCitationEntries() {
        String title = Localization.lang("Problem collecting citations");
        OOResult<CitationEngine, OOError> current = getEngine();
        if (showErrorIfPresent(title, current.asVoidResult())) {
            return Optional.empty();
        }
        OOResult<List<CitationEntry>, JabRefException> result = current.get().getCitationEntries(openOfficePreferences.getCurrentStyle());
        if (showErrorIfPresent(title, result.asVoidResult().mapError(OOError::from))) {
            return Optional.empty();
        }
        return result.getOptional();
    }

    public void applyCitationEntries(List<CitationEntry> entries) {
        String title = Localization.lang("Problem modifying citation");
        OOResult<CitationEngine, OOError> current = getEngine();
        if (!showErrorIfPresent(title, current.asVoidResult())) {
            showErrorIfPresent(title, current.get().applyCitationEntries(entries, openOfficePreferences.getCurrentStyle()).mapError(OOError::from));
        }
    }

    public void insertCitation(List<BibEntry> entries, List<BibDatabase> selectedDatabases,
                               OOStyle style, CitationType type, @Nullable String pageInfo) {
        String title = Localization.lang("Could not insert citation");
        if (entries.isEmpty()) {
            showDialog(OOError.noEntriesSelectedForCitation().setTitle(title));
            return;
        }
        OOResult<CitationEngine, OOError> current = getEngine();
        OOResult<XTextDocument, OOError> document = getXTextDocument();
        if (showErrorIfPresent(title, current.asVoidResult()) || showErrorIfPresent(title, document.asVoidResult())) {
            return;
        }
        OOResult<XTextCursor, OOError> cursor = getUserCursorForTextInsertion(document.get(), title);
        if (showErrorIfPresent(title, cursor.asVoidResult())) {
            return;
        }
        OOResult<Boolean, JabRefException> conversion = current.get().requiresFormatConversion(style);
        if (showErrorIfPresent(title, conversion.asVoidResult().mapError(OOError::from))) {
            return;
        }
        if (conversion.get()) {
            dialogService.showWarningDialogAndWait(Localization.lang("Reference mark format"),
                    Localization.lang("Converting references to selected format"));
        }
        OOResult<List<String>, JabRefException> result = current.get().insert(entries, selectedDatabases, style, type,
                Optional.ofNullable(pageInfo).orElse(""), cursor.get(), openOfficePreferences.getSyncWhenCiting());
        if (showErrorIfPresent(title, result.asVoidResult().mapError(OOError::from))) {
            return;
        }
        // [impl->req~openoffice.insertion-unresolved-warning~1]
        if (!result.get().isEmpty()) {
            dialogService.showWarningDialogAndWait(Localization.lang("Unresolved citations"),
                    Localization.lang("The citation was inserted, but some existing citations could not be found in the selected libraries: %0. Open the corresponding libraries and refresh the document.",
                            String.join(", ", result.get())));
        }
    }

    public void mergeCitations(List<BibDatabase> databases, OOStyle style) {
        String title = Localization.lang("Problem combining cite markers");
        OOResult<CitationEngine, OOError> current = getEngine();
        if (!showErrorIfPresent(title, current.asVoidResult())) {
            showRefreshResult(title, current.get().merge(databases, style));
        }
    }

    public void separateCitations(List<BibDatabase> databases, OOStyle style) {
        String title = Localization.lang("Problem during separating cite markers");
        OOResult<CitationEngine, OOError> current = getEngine();
        if (!showErrorIfPresent(title, current.asVoidResult())) {
            showRefreshResult(title, current.get().separate(databases, style));
        }
    }

    public void refresh(List<BibDatabase> databases, OOStyle style) {
        String title = Localization.lang("Unable to synchronize bibliography");
        OOResult<CitationEngine, OOError> current = getEngine();
        if (!showErrorIfPresent(title, current.asVoidResult())) {
            showRefreshResult(title, current.get().refresh(databases, style));
        }
    }

    private void showRefreshResult(String title, OOResult<List<String>, JabRefException> result) {
        if (showErrorIfPresent(title, result.asVoidResult().mapError(OOError::from))) {
            return;
        }
        if (!result.get().isEmpty()) {
            showUnresolvedKeys(title, result.get());
        }
    }

    public Optional<BibDatabase> exportCited(List<BibDatabase> databases, boolean returnPartialResult) {
        String title = Localization.lang("Unable to generate new library");
        if (databases.isEmpty()) {
            showDialog(OOError.noDataBaseIsOpenForExport().setTitle(title));
            return Optional.empty();
        }
        OOResult<CitationEngine, OOError> current = getEngine();
        if (showErrorIfPresent(title, current.asVoidResult())) {
            return Optional.empty();
        }
        OOResult<CitedEntryExporter.ExportResult, JabRefException> exported = current.get().exportCited(databases);
        if (showErrorIfPresent(title, exported.asVoidResult().mapError(OOError::from))) {
            return Optional.empty();
        }
        CitedEntryExporter.ExportResult result = exported.get();
        if (!result.database().hasEntries()) {
            dialogService.showErrorDialogAndWait(title,
                    Localization.lang("Your OpenOffice/LibreOffice document references no citation keys which could also be found in your current library."));
            return Optional.empty();
        }
        if (!result.unresolvedKeys().isEmpty()) {
            showUnresolvedKeys(title, result.unresolvedKeys());
            if (!returnPartialResult) {
                return Optional.empty();
            }
        }
        return Optional.of(result.database());
    }

    private void showUnresolvedKeys(String title, List<String> keys) {
        dialogService.showErrorDialogAndWait(title,
                Localization.lang("Your OpenOffice/LibreOffice document references at least %0 citation keys which could not be found in your current library. Some of these are %1.",
                        String.valueOf(keys.size()), String.join(", ", keys)));
    }
}
