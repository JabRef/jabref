package org.jabref.logic.openoffice.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.jabref.logic.JabRefException;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.CitationIds;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.backend.WriterReferenceMarks;
import org.jabref.logic.openoffice.engine.ZoteroCitationStrategy.StrategyException;
import org.jabref.logic.openoffice.engine.storage.MarkWriter;
import org.jabref.logic.openoffice.frontend.DocumentCitations;
import org.jabref.logic.openoffice.frontend.DocumentOrder;
import org.jabref.logic.openoffice.frontend.RangeForOverlapCheck;
import org.jabref.logic.openoffice.rangesort.FunctionalTextViewCursor;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.CitationEntry;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.style.CitedReference;
import org.jabref.model.openoffice.util.OOResult;
import org.jabref.model.openoffice.util.OOVoidResult;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Document-bound entry point for citation actions. Only formatted write baselines survive an action;
/// groups, resolution, numbering and rendering are reconstructed from the document each time.
// [impl->adr~canonical-writer-citations~1]
@NullMarked
public final class CitationEngine implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationEngine.class);
    private static final StyleCapabilities NONE = new StyleCapabilities(Set.of(), false, false, false);
    private final XTextDocument document;
    private final XComponentContext context;
    private final OpenOfficePreferences preferences;
    private final BibEntryTypesManager entryTypes;
    private final MarkWriter writer;
    private final DocumentSession session;
    private final ZoteroCitationStrategy zotero;
    private boolean closed;

    public CitationEngine(XTextDocument document, XComponentContext context, OpenOfficePreferences preferences, BibEntryTypesManager entryTypes) {
        this.document = document;
        this.context = context;
        this.preferences = preferences;
        this.entryTypes = entryTypes;
        this.writer = new MarkWriter(document, context);
        this.session = new DocumentSession(document);
        this.zotero = new ZoteroCitationStrategy(document, context, preferences, entryTypes);
    }

    private RendererFactory renderers() {
        return new RendererFactory(entryTypes, preferences.getPandocPath());
    }

    private UpdatePipeline pipeline() {
        return new UpdatePipeline(document, preferences, writer, renderers());
    }

    public StyleCapabilities capabilities(OOStyle style) {
        if (closed || (preferences.getZoteroCompatibilityMode() && !(style instanceof CitationStyle))) {
            return NONE;
        }
        try {
            StyleCapabilities capabilities = renderers().create(style).capabilities();
            return preferences.getZoteroCompatibilityMode()
                   ? new StyleCapabilities(capabilities.supportedTypes(), capabilities.hasBibliography(), false, false)
                   : capabilities;
        } catch (RenderException exception) {
            LOGGER.debug("Cannot determine citation style capabilities", exception);
            return NONE;
        }
    }

    /// Fresh, read-only mismatch detection for the next CSL insertion. Preferences alone do not
    /// convert anything; neither refresh nor export invokes this insertion transaction.
    // [impl->req~openoffice.next-citation-format-conversion~1]
    public OOResult<Boolean, JabRefException> requiresFormatConversion(OOStyle style) {
        if (closed) {
            return OOResult.error(unavailable());
        }
        if (!(style instanceof CitationStyle)) {
            return OOResult.ok(false);
        }
        try {
            return OOResult.ok(CitationFormatConverter.required(WriterReferenceMarks.getListOfNames(document), preferences.getZoteroCompatibilityMode()));
        } catch (NoDocumentException | com.sun.star.uno.RuntimeException exception) {
            return OOResult.error(failure(Localization.lang("Problem collecting citations"), exception));
        }
    }

    // [impl->req~openoffice.legacy-marks-unmanaged~1]
    public OOResult<List<String>, JabRefException> insert(List<BibEntry> entries, List<BibDatabase> databases,
                                                          OOStyle style, CitationType requestedType, String pageInfo,
                                                          XTextCursor cursor, boolean rebuildBibliography) {
        if (closed) {
            return OOResult.error(unavailable());
        }
        OOResult<Boolean, JabRefException> conversion = requiresFormatConversion(style);
        if (conversion.isError()) {
            return OOResult.error(conversion.getError());
        }
        if (conversion.get() && style instanceof CitationStyle cslStyle) {
            return insertWithConversion(entries, databases, cslStyle, requestedType, pageInfo, cursor, rebuildBibliography);
        }
        if (preferences.getZoteroCompatibilityMode()) {
            return writeNativeSession(Localization.lang("Insert citation"),
                    () -> zotero.insert(entries, databases, style, requestedType, pageInfo, cursor, rebuildBibliography));
        }
        try {
            DocumentCitations documentCitations = new DocumentCitations(document);
            OOVoidResult<JabRefException> checked = documentCitations.checkCitationEdits(document);
            if (checked.isError()) {
                return OOResult.error(checked.getError());
            }
            OOVoidResult<JabRefException> protectedPosition = documentCitations.checkRangeOverlapsWithCursor(document,
                    List.of(new RangeForOverlapCheck<>(cursor, new CitationGroupId("cursor"), RangeForOverlapCheck.CURSOR_MARK_KIND, "cursor")), false);
            if (protectedPosition.isError()) {
                return OOResult.error(protectedPosition.getError());
            }
            List<String> keys = new ArrayList<>();
            for (BibEntry entry : entries) {
                keys.add(entry.getCitationKey().orElseThrow(() -> new RenderException(Localization.lang("This operation requires all selected entries to have citation keys defined."))));
            }
            CitationType type = capabilities(style).coerceForInsert(requestedType);
            JabRefMarkData data = new JabRefMarkData(keys, CitationIds.newId(), type);
            List<Citation> citations = new ArrayList<>();
            for (String key : keys) {
                citations.add(new Citation(key));
            }
            if (!pageInfo.isEmpty()) {
                citations.getLast().setPageInfo(Optional.of(OOText.fromString(pageInfo)));
            }
            CitationGroup pending = new CitationGroup(new CitationGroupId(data.uniqueId()), type,
                    citations, Optional.of(MarkCodec.encode(data)));
            OOResult<FunctionalTextViewCursor, String> view = FunctionalTextViewCursor.get(document);
            List<CitationGroup> ordered = new DocumentOrder().withInsertion(document, documentCitations.citationGroups, documentCitations.markStore,
                    pending, cursor.getEnd(), view.isError() ? Optional.empty() : Optional.of(view.get()));
            UpdatePipeline pipeline = pipeline();
            RenderResult rendered = pipeline.render(style, ordered, databases, rebuildBibliography);
            return writeSession(Localization.lang("Insert citation"), () -> {
                try {
                    CitationGroup inserted = new CitationGroupEditor(document, context, writer).insert(documentCitations, keys,
                            citations.stream().map(Citation::getPageInfo).toList(), type, cursor, data.uniqueId(),
                            preferences.getAddSpaceBefore(), preferences.getAddSpaceAfter());
                    List<CitationGroup> stored = ordered.stream().map(group -> group == pending ? inserted : group).toList();
                    Map<CitationGroupId, OOText> markers = new LinkedHashMap<>(rendered.markers());
                    markers.put(inserted.groupId, markers.remove(pending.groupId));
                    pipeline.apply(style, documentCitations.markStore, stored,
                            new RenderResult(markers, rendered.bibliography(), rendered.unresolvedKeys(), rendered.refreshCitedOnPages()), rebuildBibliography);
                    return OOResult.ok(rendered.unresolvedKeys());
                } catch (com.sun.star.uno.Exception | CreationException | NoDocumentException | RenderException exception) {
                    writer.clear();
                    return OOResult.error(failure(Localization.lang("Could not insert citation"), exception));
                }
            });
        } catch (NoDocumentException | WrappedTargetException | RenderException | MarkCodecException exception) {
            return OOResult.error(failure(Localization.lang("Could not insert citation"), exception));
        }
    }

    private OOResult<List<String>, JabRefException> insertWithConversion(List<BibEntry> entries, List<BibDatabase> databases,
                                                                         CitationStyle style, CitationType type, String pageInfo,
                                                                         XTextCursor cursor, boolean rebuildBibliography) {
        try {
            session.requireRollbackSupport();
            CitationFormatConverter.PreparedInsertion prepared = new CitationFormatConverter(document, context, preferences, entryTypes, zotero)
                    .prepare(entries, databases, style, type, pageInfo, cursor, rebuildBibliography);
            return writeSession(Localization.lang("Insert citation"), () -> {
                try {
                    prepared.apply(writer, cursor);
                    return OOResult.ok(prepared.unresolvedKeys());
                } catch (com.sun.star.uno.Exception | CreationException | NoDocumentException | MarkCodecException exception) {
                    writer.clear();
                    return OOResult.error(failure(Localization.lang("Could not insert citation"), exception));
                }
            }, true);
        } catch (JabRefException exception) {
            return OOResult.error(exception);
        } catch (com.sun.star.uno.Exception | NoDocumentException | RenderException | MarkCodecException | IOException
                 | IllegalArgumentException | IllegalStateException | com.sun.star.uno.RuntimeException exception) {
            return OOResult.error(failure(Localization.lang("Could not insert citation"), exception));
        }
    }

    // [impl->req~openoffice.cross-family-citations~1,req~openoffice.legacy-marks-unmanaged~1]
    public OOResult<List<String>, JabRefException> refresh(List<BibDatabase> databases, OOStyle style) {
        if (closed) {
            return OOResult.error(unavailable());
        }
        if (preferences.getZoteroCompatibilityMode()) {
            return writeNativeSession(Localization.lang("Refresh bibliography"), () -> zotero.refresh(databases, style));
        }
        try {
            DocumentCitations documentCitations = new DocumentCitations(document);
            OOVoidResult<JabRefException> checked = documentCitations.checkCitationEdits(document);
            if (checked.isError()) {
                return OOResult.error(checked.getError());
            }
            documentCitations.imposeGlobalOrder(document);
            List<CitationGroup> groups = documentCitations.citationGroups.getCitationGroupsInGlobalOrder();
            UpdatePipeline pipeline = pipeline();
            RenderResult result = pipeline.render(style, groups, databases, true);
            return writeSession(Localization.lang("Refresh bibliography"), () -> {
                try {
                    pipeline.apply(style, documentCitations.markStore, groups, result, true);
                    return OOResult.ok(result.unresolvedKeys());
                } catch (com.sun.star.uno.Exception | CreationException | NoDocumentException | RenderException exception) {
                    writer.clear();
                    return OOResult.error(failure(Localization.lang("Could not update bibliography"), exception));
                }
            });
        } catch (NoDocumentException | WrappedTargetException | RenderException exception) {
            return OOResult.error(failure(Localization.lang("Could not update bibliography"), exception));
        }
    }

    public OOResult<List<String>, JabRefException> merge(List<BibDatabase> databases, OOStyle style) {
        return editGroups(databases, style, GroupEdit.MERGE);
    }

    public OOResult<List<String>, JabRefException> separate(List<BibDatabase> databases, OOStyle style) {
        return editGroups(databases, style, GroupEdit.SEPARATE);
    }

    // [impl->req~openoffice.single-step-group-undo~1]
    private OOResult<List<String>, JabRefException> editGroups(List<BibDatabase> databases, OOStyle style, GroupEdit edit) {
        if (!capabilities(style).supportsGroupEditing()) {
            return OOResult.error(new UnsupportedOperation("The selected style does not support citation group editing"));
        }
        try {
            DocumentCitations documentCitations = new DocumentCitations(document);
            OOVoidResult<JabRefException> checked = documentCitations.checkCitationEdits(document);
            if (checked.isError()) {
                return OOResult.error(checked.getError());
            }
            documentCitations.imposeGlobalOrder(document);
            List<CitationGroup> originalOrder = documentCitations.citationGroups.getCitationGroupsInGlobalOrder();
            CitationGroupEditor editor = new CitationGroupEditor(document, context, writer);
            CitationGroupEditor.PreparedEdit prepared = edit == GroupEdit.MERGE
                                                        ? editor.prepareMerge(documentCitations, originalOrder)
                                                        : editor.prepareSeparate(documentCitations, originalOrder);
            if (!prepared.hasChanges()) {
                return OOResult.ok(List.of());
            }
            UpdatePipeline pipeline = pipeline();
            RenderResult rendered = pipeline.render(style, prepared.groupsInDocumentOrder(), databases, true);
            List<OOText> preparedMarkers = new ArrayList<>();
            for (CitationGroup group : prepared.groupsInDocumentOrder()) {
                preparedMarkers.add(Optional.ofNullable(rendered.markers().get(group.groupId))
                                            .orElseThrow(() -> new RenderException("Renderer omitted a prepared citation group")));
            }
            return writeSession(edit == GroupEdit.MERGE ? Localization.lang("Merge citations") : Localization.lang("Separate citations"), () -> {
                try {
                    List<CitationGroup> stored = editor.apply(documentCitations, prepared);
                    Map<CitationGroupId, OOText> markers = new LinkedHashMap<>();
                    for (int index = 0; index < stored.size(); index++) {
                        markers.put(stored.get(index).groupId, preparedMarkers.get(index));
                    }
                    pipeline.apply(style, documentCitations.markStore, stored,
                            new RenderResult(markers, rendered.bibliography(), rendered.unresolvedKeys(), rendered.refreshCitedOnPages()), true);
                    return OOResult.ok(rendered.unresolvedKeys());
                } catch (com.sun.star.uno.Exception | CreationException | NoDocumentException | RenderException exception) {
                    writer.clear();
                    return OOResult.error(failure(Localization.lang("Problem modifying citation"), exception));
                }
            });
        } catch (NoDocumentException | WrappedTargetException | CreationException | RenderException exception) {
            return OOResult.error(failure(Localization.lang("Problem modifying citation"), exception));
        }
    }

    // [impl->req~openoffice.legacy-marks-unmanaged~1]
    public OOResult<CitedEntryExporter.ExportResult, JabRefException> exportCited(List<BibDatabase> databases) {
        if (closed) {
            return OOResult.error(unavailable());
        }
        if (preferences.getZoteroCompatibilityMode()) {
            return zotero.exportCited(databases);
        }
        try {
            DocumentCitations documentCitations = new DocumentCitations(document);
            documentCitations.imposeGlobalOrder(document);
            List<String> citationKeys = documentCitations.citationGroups.getCitedReferencesSortedInOrderOfAppearance()
                                                                        .values()
                                                                        .stream()
                                                                        .map(CitedReference::getCitationKey)
                                                                        .toList();
            return OOResult.ok(CitedEntryExporter.export(citationKeys, databases));
        } catch (NoDocumentException | WrappedTargetException e) {
            LOGGER.debug("Could not collect cited entries for export", e);
            return OOResult.error(new JabRefException(e.getMessage(), e));
        }
    }

    public OOResult<List<CitationEntry>, JabRefException> getCitationEntries(OOStyle style) {
        if (!capabilities(style).supportsPageInfo()) {
            return OOResult.error(new UnsupportedOperation("The selected style does not support editing page information"));
        }
        try {
            return OOResult.ok(new DocumentCitations(document).getCitationEntries(document));
        } catch (NoDocumentException | WrappedTargetException exception) {
            return OOResult.error(failure(Localization.lang("Problem collecting citations"), exception));
        }
    }

    public OOVoidResult<JabRefException> applyCitationEntries(List<CitationEntry> entries, OOStyle style) {
        if (!capabilities(style).supportsPageInfo()) {
            return OOVoidResult.error(new UnsupportedOperation("The selected style does not support editing page information"));
        }
        try {
            new DocumentCitations(document).applyCitationEntries(document, entries);
            writer.clear();
            return OOVoidResult.ok();
        } catch (com.sun.star.uno.Exception | NoDocumentException exception) {
            return OOVoidResult.error(failure(Localization.lang("Problem modifying citation"), exception));
        }
    }

    private OOResult<List<String>, JabRefException> writeNativeSession(String title, Supplier<OOResult<List<String>, JabRefException>> action) {
        // Even an empty Writer undo context can evict older undo actions when the history is full.
        try {
            zotero.requireSupportedModifiers();
        } catch (StrategyException exception) {
            return OOResult.error(exception);
        } catch (NoDocumentException | com.sun.star.uno.RuntimeException exception) {
            return OOResult.error(failure(Localization.lang("Problem collecting citations"), exception));
        }
        return writeSession(title, action);
    }

    private <T> OOResult<T, JabRefException> writeSession(String title, Supplier<OOResult<T, JabRefException>> action) {
        return writeSession(title, action, false);
    }

    private <T> OOResult<T, JabRefException> writeSession(String title, Supplier<OOResult<T, JabRefException>> action, boolean rollbackOnFailure) {
        writer.beginUpdate();
        try {
            return rollbackOnFailure ? session.writeWithRollback(title, action) : session.write(title, action);
        } finally {
            writer.endUpdate();
        }
    }

    @Override
    public void close() {
        writer.close();
        zotero.close();
        closed = true;
    }

    private static JabRefException unavailable() {
        return new JabRefException("Citation document connection is closed", Localization.lang("Not connected to any Writer document. Please make sure a document is open, and use the 'Select Writer document' button to connect to it."));
    }

    private static JabRefException failure(String localizedMessage, Exception exception) {
        LOGGER.warn("Writer citation operation failed", exception);
        String details = exception instanceof RenderException renderException
                         ? renderException.localizedMessage().orElse(localizedMessage) : localizedMessage;
        return new JabRefException("Writer citation operation failed", details, exception);
    }

    @NullMarked
    public static final class UnsupportedOperation extends JabRefException {
        private UnsupportedOperation(String message) {
            super(message, Localization.lang("Problem modifying citation"));
        }
    }

    @NullMarked
    private enum GroupEdit {
        MERGE,
        SEPARATE
    }
}
