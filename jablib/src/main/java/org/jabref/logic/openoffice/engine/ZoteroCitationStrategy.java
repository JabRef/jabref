package org.jabref.logic.openoffice.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.JabRefException;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.CitationIds;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.ZoteroCitationLinker;
import org.jabref.logic.openoffice.ZoteroCitationLinker.LinkPlan;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.logic.openoffice.backend.ReferenceMarkTextWriter;
import org.jabref.logic.openoffice.backend.WriterReferenceMark;
import org.jabref.logic.openoffice.backend.WriterReferenceMark.BoundaryPolicy;
import org.jabref.logic.openoffice.backend.WriterReferenceMarks;
import org.jabref.logic.openoffice.frontend.RangeForOverlapCheck;
import org.jabref.logic.openoffice.rangesort.RangeSort;
import org.jabref.logic.openoffice.rangesort.RangeSortEntry;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.DocumentAnnotation;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.OOTextWriter;
import org.jabref.logic.openoffice.uno.UnoCast;
import org.jabref.logic.openoffice.uno.UnoProperties;
import org.jabref.logic.openoffice.uno.UnoRedlines;
import org.jabref.logic.openoffice.uno.UnoTextRange;
import org.jabref.logic.openoffice.uno.UnoTextSection;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOResult;

import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextSection;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Separate Zotero storage strategy with action-local citation state and explicit CSL styles.
/// Only Zotero names are parsed; legacy JabRef marks are neither read nor converted. The existing
/// linker plans URI/duplicate matching in memory. Unchanged names are written in place; linked
/// names are recreated with fully rendered text only after rendering succeeds. Sessions, undo and controller locking belong to
/// the caller. No adapter, preference listener or rendered-text property is retained.
@NullMarked
public final class ZoteroCitationStrategy implements AutoCloseable {

    static final String BIBLIOGRAPHY_PREFIX = "ZOTERO_BIBL ";
    private static final Logger LOGGER = LoggerFactory.getLogger(ZoteroCitationStrategy.class);

    private final XTextDocument document;
    private final XComponentContext context;
    private final OpenOfficePreferences preferences;
    private final BibEntryTypesManager entryTypes;
    private final WriterReferenceMarks ranges;
    private boolean closed;

    public ZoteroCitationStrategy(XTextDocument document, XComponentContext context, OpenOfficePreferences preferences, BibEntryTypesManager entryTypes) {
        this(document, context, preferences, entryTypes, new WriterReferenceMarks());
    }

    ZoteroCitationStrategy(XTextDocument document, XComponentContext context, OpenOfficePreferences preferences,
                           BibEntryTypesManager entryTypes, WriterReferenceMarks ranges) {
        this.document = document;
        this.context = context;
        this.preferences = preferences;
        this.entryTypes = entryTypes;
        this.ranges = ranges;
    }

    public OOResult<List<String>, JabRefException> insert(List<BibEntry> entries, List<BibDatabase> databases,
                                                          OOStyle style, CitationType type, String pageInfo,
                                                          XTextCursor cursor, boolean rebuildBib) {
        try {
            CitationStyle cslStyle = requireStyle(style);
            CitationType zoteroType = requireType(type);
            if (!pageInfo.isEmpty()) {
                throw new StrategyException(Reason.PAGE_INFO_NOT_SUPPORTED, "Zotero mode does not support page information", Localization.lang("Problem modifying citation"));
            }
            if (entries.isEmpty()) {
                throw new StrategyException(Reason.INVALID_ENTRY, "No entries selected for Zotero citation", Localization.lang("No entries selected for citation"));
            }
            List<String> keys = new ArrayList<>();
            for (BibEntry entry : entries) {
                keys.add(entry.getCitationKey().filter(key -> !key.isBlank()).orElseThrow(() -> new StrategyException(Reason.INVALID_ENTRY,
                        "A Zotero citation requires stable citation keys", Localization.lang("This operation requires all selected entries to have citation keys defined."))));
            }
            List<BibDatabase> selected = new ArrayList<>(databases);
            selected.add(new BibDatabase(entries.stream().map(BibEntry::new).toList()));
            requireSupportedModifiers();
            List<StoredCitation> citations = read();
            validate(citations, Optional.of(cursor));

            LinkPlan links = planLinks(citations, selected);
            citations = prepareLinks(citations, links);
            Map<String, ConversionMetadata> metadataUpdates = prepareMetadataLinks(citations);
            Map<String, String> uris = links.zoteroUriByCitationKey();
            int nextItemId = 1;
            for (StoredCitation citation : citations) {
                int maximum = ZoteroReferenceMark.getMaxItemId(citation.mark().name());
                if (maximum > Integer.MAX_VALUE - entries.size()) {
                    throw new StrategyException(Reason.INVALID_ENTRY, "Zotero citation item ids are exhausted", Localization.lang("Could not insert citation"));
                }
                nextItemId = Math.max(nextItemId, maximum + 1);
            }
            CitationGroup temporary = group("pending", keys, type);
            RenderData insertionData = new RenderData(new RenderRequest(List.of(temporary), selected, options(false)));
            BibDatabase insertionDatabase = new BibDatabase(insertionData.flattenedEntries());
            List<BibEntry> orderedEntries = new ArrayList<>();
            for (String key : keys) {
                orderedEntries.add(insertionDatabase.getEntryByCitationKey(key).orElseThrow(() -> new RenderException("Cannot resolve selected Zotero citation " + key)));
            }
            ZoteroReferenceMark mark = ZoteroReferenceMark.buildReferenceMark(orderedEntries, keys,
                    nextItemId, zoteroType, new BibDatabaseContext(insertionDatabase), entryTypes, uris);
            XTextCursor position = cursor.getText().createTextCursorByRange(cursor.getEnd());
            position.collapseToEnd();
            StoredCitation inserted = new StoredCitation(mark, group(mark.name(), keys, type), position, Optional.empty(), mark.name());
            citations.add(inserted);
            List<StoredCitation> ordered = order(citations);
            RenderResult rendered = render(ordered, selected, cslStyle, rebuildBib);
            WriterReferenceMark range = ranges.create(document, context, mark.name(), cursor,
                    preferences.getAddSpaceBefore() && !hasAdjacentSpace(cursor, false),
                    preferences.getAddSpaceAfter() && !hasAdjacentSpace(cursor, true), BoundaryPolicy.TEMPORARY);
            List<StoredCitation> stored = ordered.stream().map(citation -> citation == inserted
                                                                           ? new StoredCitation(mark, inserted.group(), inserted.anchor(), Optional.of(range), mark.name())
                                                                           : citation).toList();
            apply(stored, rendered, cslStyle, rebuildBib, metadataUpdates);
            return OOResult.ok(rendered.unresolvedKeys());
        } catch (StrategyException exception) {
            return OOResult.error(exception);
        } catch (com.sun.star.uno.Exception | CreationException | NoDocumentException | RenderException
                 | MarkCodecException | IllegalArgumentException | IllegalStateException | com.sun.star.uno.RuntimeException exception) {
            return OOResult.error(failure(Localization.lang("Could not insert citation"), exception));
        }
    }

    public OOResult<List<String>, JabRefException> refresh(List<BibDatabase> databases, OOStyle style) {
        try {
            CitationStyle cslStyle = requireStyle(style);
            requireSupportedModifiers();
            List<StoredCitation> original = read();
            validate(original, Optional.empty());

            List<StoredCitation> citations = order(prepareLinks(original, planLinks(original, databases)));
            Map<String, ConversionMetadata> metadataUpdates = prepareMetadataLinks(citations);
            RenderResult rendered = render(citations, databases, cslStyle, true);
            apply(citations, rendered, cslStyle, true, metadataUpdates);
            return OOResult.ok(rendered.unresolvedKeys());
        } catch (StrategyException exception) {
            return OOResult.error(exception);
        } catch (com.sun.star.uno.Exception | CreationException | NoDocumentException | RenderException
                 | MarkCodecException | IllegalArgumentException | IllegalStateException | com.sun.star.uno.RuntimeException exception) {
            return OOResult.error(failure(Localization.lang("Could not update bibliography"), exception));
        }
    }

    /// Export is read-only. Native Zotero items not yet linked to a selected library remain unresolved,
    /// as in the previous export path; insert and refresh perform the URI/duplicate linking step.
    public OOResult<CitedEntryExporter.ExportResult, JabRefException> exportCited(List<BibDatabase> databases) {
        try {
            requireOpen();
            List<String> keys = order(read()).stream().flatMap(citation -> citation.mark().citationKeys().stream()).distinct().toList();
            return OOResult.ok(CitedEntryExporter.export(keys, databases));
        } catch (StrategyException exception) {
            return OOResult.error(exception);
        } catch (NoDocumentException | WrappedTargetException | IllegalArgumentException
                 | IllegalStateException | com.sun.star.uno.RuntimeException exception) {
            return OOResult.error(failure(Localization.lang("Problem collecting citations"), exception));
        }
    }

    // [impl->req~openoffice.zotero-modifier-safe-update~1]
    void requireSupportedModifiers() throws NoDocumentException, StrategyException {
        requireOpen();
        for (String name : ranges.names(document)) {
            if (!ZoteroReferenceMark.isZoteroReferenceMarkName(name)) {
                continue;
            }
            try {
                ZoteroCitationModifiers.requireSupportedModifiers(name);
            } catch (RenderException exception) {
                LOGGER.debug("Cannot update native Zotero citation modifiers", exception);
                throw new StrategyException(Reason.UNSUPPORTED_MODIFIERS, "Unsupported native Zotero citation modifiers",
                        Localization.lang("Cannot update Zotero citations with unsupported modifiers. No citations were changed. Update them in Zotero instead."), exception);
            }
        }
    }

    private void validate(List<StoredCitation> citations, Optional<XTextCursor> insertion)
            throws NoDocumentException, com.sun.star.uno.Exception, StrategyException {
        List<RangeForOverlapCheck<String>> citationRanges = citations.stream()
                                                                     .map(citation -> protectedRange(citation.anchor(), citation.originalName(), RangeForOverlapCheck.REFERENCE_MARK_KIND)).toList();
        validateRanges(citationRanges, insertion, Set.of(BIBLIOGRAPHY_PREFIX));
    }

    void validateRanges(List<RangeForOverlapCheck<String>> citationRanges, Optional<XTextCursor> insertion, Set<String> editedBibliographyPrefixes)
            throws NoDocumentException, com.sun.star.uno.Exception, StrategyException {
        List<RangeForOverlapCheck<String>> managed = new ArrayList<>(citationRanges);
        managed.addAll(footnoteRanges(citationRanges));
        List<RangeForOverlapCheck<String>> unmanaged = new ArrayList<>();
        Set<String> managedNames = citationRanges.stream().map(range -> range.idWithinKind).collect(Collectors.toSet());
        for (String name : ranges.names(document)) {
            if (managedNames.contains(name)) {
                continue;
            }
            WriterReferenceMark range = ranges.find(document, name, BoundaryPolicy.TEMPORARY)
                                              .orElseThrow(() -> new NoDocumentException("Missing protected reference mark"));
            XTextRange anchor = range.getMarkRange(document).orElseThrow(() -> new NoDocumentException("Missing protected reference-mark anchor"));
            unmanaged.add(protectedRange(anchor, name, RangeForOverlapCheck.REFERENCE_MARK_KIND));
        }
        unmanaged.addAll(footnoteRanges(unmanaged));
        for (String name : UnoTextSection.getNameAccess(document).getElementNames()) {
            if (name.startsWith(BIBLIOGRAPHY_PREFIX) || BibliographySection.NAME.equals(name)) {
                XTextRange anchor = UnoTextSection.getAnchor(document, name)
                                                  .orElseThrow(() -> new NoDocumentException("Missing protected bibliography section"));
                RangeForOverlapCheck<String> range = protectedRange(anchor, name, RangeForOverlapCheck.BIBLIOGRAPHY_MARK_KIND);
                if (editedBibliographyPrefixes.stream().anyMatch(name::startsWith)) {
                    managed.add(range);
                } else {
                    unmanaged.add(range);
                }
            }
            XTextSection section = UnoTextSection.getByName(document, name).orElseThrow(() -> new NoDocumentException("Missing text section"));
            Optional<XPropertySet> properties = UnoCast.cast(XPropertySet.class, section);
            if (properties.isPresent() && UnoProperties.getValueAsObject(properties.orElseThrow(), "IsProtected").filter(Boolean.TRUE::equals).isPresent()) {
                unmanaged.add(protectedRange(section.getAnchor(), name, RangeForOverlapCheck.BIBLIOGRAPHY_MARK_KIND));
            }
        }
        // Comparing every pair also catches containment hidden behind an intervening shorter range.
        for (int i = 0; i < managed.size(); i++) {
            for (int j = i + 1; j < managed.size(); j++) {
                rejectOverlap(managed.get(i), managed.get(j));
            }
            for (RangeForOverlapCheck<String> foreign : unmanaged) {
                rejectOverlap(managed.get(i), foreign);
            }
        }
        if (insertion.isPresent()) {
            XTextCursor cursor = insertion.orElseThrow();
            List<RangeForOverlapCheck<String>> protectedRanges = new ArrayList<>(managed);
            protectedRanges.addAll(unmanaged);
            for (RangeForOverlapCheck<String> range : protectedRanges) {
                if (overlaps(cursor, range.range, range.kind == RangeForOverlapCheck.BIBLIOGRAPHY_MARK_KIND)) {
                    throw new StrategyException(Reason.PROTECTED_RANGE, "Zotero insertion intersects protected document content", Localization.lang("Found overlapping ranges"));
                }
            }
        }
        List<XTextRange> edited = new ArrayList<>(managed.stream().map(RangeForOverlapCheck::getRange).toList());
        insertion.ifPresent(edited::add);
        // Pending deletions survive after recording is disabled, including while the caller's session is suspended.
        for (XTextRange deletion : UnoRedlines.getDeletedRanges(document)) {
            for (XTextRange range : edited) {
                if (overlaps(deletion, range, true)) {
                    throw new StrategyException(Reason.PENDING_DELETIONS, "Zotero edits touch an unaccepted deletion",
                            Localization.lang("Citations inside deletions that have not been accepted yet may reappear.") + "\n"
                                    + Localization.lang("Use [Edit]/[Track Changes]/[Manage] to resolve them first."));
                }
            }
        }
    }

    private static RangeForOverlapCheck<String> protectedRange(XTextRange range, String name, int kind) {
        return new RangeForOverlapCheck<>(range, name, kind, name);
    }

    private List<RangeForOverlapCheck<String>> footnoteRanges(List<RangeForOverlapCheck<String>> citations)
            throws NoDocumentException, com.sun.star.lang.IllegalArgumentException {
        List<RangeForOverlapCheck<String>> result = new ArrayList<>();
        for (RangeForOverlapCheck<String> citation : citations) {
            Optional<XTextRange> footnote = UnoTextRange.getFootnoteMarkRange(citation.range);
            if (footnote.isEmpty()) {
                continue;
            }
            XTextRange anchor = footnote.orElseThrow();
            boolean duplicate = false;
            for (RangeForOverlapCheck<String> previous : result) {
                if (sameRange(anchor, previous.range)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                result.add(protectedRange(anchor, citation.idWithinKind, RangeForOverlapCheck.FOOTNOTE_MARK_KIND));
            }
        }
        return result;
    }

    private void rejectOverlap(RangeForOverlapCheck<String> left, RangeForOverlapCheck<String> right)
            throws NoDocumentException, com.sun.star.lang.IllegalArgumentException, StrategyException {
        if (left.kind == RangeForOverlapCheck.FOOTNOTE_MARK_KIND && right.kind == RangeForOverlapCheck.FOOTNOTE_MARK_KIND
                && sameRange(left.range, right.range)) {
            return;
        }
        if (overlaps(left.range, right.range, false)) {
            throw new StrategyException(Reason.OVERLAPPING_RANGES, "Zotero protected ranges overlap", Localization.lang("Found overlapping ranges"));
        }
    }

    private boolean sameRange(XTextRange left, XTextRange right) throws NoDocumentException, com.sun.star.lang.IllegalArgumentException {
        if (!UnoRuntime.areSame(left.getText(), right.getText())) {
            return false;
        }
        XTextRangeCompare compare = UnoCast.cast(XTextRangeCompare.class, left.getText())
                                           .orElseThrow(() -> new NoDocumentException("Cannot compare protected Zotero ranges"));
        return compare.compareRegionStarts(left, right) == 0 && compare.compareRegionEnds(left, right) == 0;
    }

    private boolean overlaps(XTextRange left, XTextRange right, boolean touching) throws NoDocumentException, com.sun.star.lang.IllegalArgumentException {
        if (!UnoRuntime.areSame(left.getText(), right.getText())) {
            return false;
        }
        XTextRangeCompare compare = UnoCast.cast(XTextRangeCompare.class, left.getText())
                                           .orElseThrow(() -> new NoDocumentException("Cannot compare protected Zotero ranges"));
        if (compare.compareRegionStarts(left, right) == 0 && compare.compareRegionEnds(left, right) == 0) {
            return true;
        }
        int leftEndToRightStart = compare.compareRegionStarts(left.getEnd(), right.getStart());
        int rightEndToLeftStart = compare.compareRegionStarts(right.getEnd(), left.getStart());
        return touching ? leftEndToRightStart <= 0 && rightEndToLeftStart <= 0 : leftEndToRightStart < 0 && rightEndToLeftStart < 0;
    }

    LinkPlan planLinks(List<StoredCitation> citations, List<BibDatabase> databases) {
        List<BibEntry> entries = databases.stream()
                                          .flatMap(database -> database.getEntries().stream().map(entry -> database.resolveForStrings(entry, false)))
                                          .distinct().toList();
        return ZoteroCitationLinker.planLinks(citations.stream().map(StoredCitation::originalName).toList(),
                new BibDatabaseContext(new BibDatabase(entries)), entryTypes);
    }

    static List<StoredCitation> prepareLinks(List<StoredCitation> citations, LinkPlan links) throws RenderException {
        List<StoredCitation> planned = new ArrayList<>();
        for (StoredCitation citation : citations) {
            ZoteroReferenceMark mark = ZoteroReferenceMark.parse(links.updatedName(citation.originalName()))
                                                          .orElseThrow(() -> new RenderException("Cannot parse planned Zotero URI link"));
            CitationGroup group = new CitationGroup(citation.group().groupId, citation.group().citationType,
                    mark.citationKeys().stream().map(Citation::new).toList(), Optional.empty());
            planned.add(new StoredCitation(mark, group, citation.anchor(), citation.range(), citation.originalName()));
        }
        return planned;
    }

    List<StoredCitation> read() throws NoDocumentException, WrappedTargetException {
        List<StoredCitation> result = new ArrayList<>();
        for (String name : ranges.names(document)) {
            if (!ZoteroReferenceMark.isZoteroReferenceMarkName(name)) {
                continue;
            }
            Optional<ZoteroReferenceMark> parsed = ZoteroReferenceMark.parse(name);
            if (parsed.isEmpty()) {
                LOGGER.warn("Ignoring an invalid Zotero citation mark");
                continue;
            }
            ZoteroReferenceMark mark = parsed.orElseThrow();
            WriterReferenceMark range = ranges.find(document, name, BoundaryPolicy.TEMPORARY).orElseThrow(() -> new NoDocumentException("Missing Zotero citation range"));
            XTextRange anchor = range.getMarkRange(document).orElseThrow(() -> new NoDocumentException("Missing Zotero citation anchor"));
            String content = range.getRawCursor(document).orElseThrow(() -> new NoDocumentException("Missing Zotero citation cursor")).getString();
            // Zotero's JSON model has normal and suppress-author types only. Preserve empty citations
            // through their empty content, including the invisible boundary characters Writer needs.
            CitationType type = content.codePoints().allMatch(character -> character == 0x200B)
                                ? CitationType.INVISIBLE_CIT
                                : mark.citationType() == CitationType.AUTHORYEAR_INTEXT ? CitationType.AUTHORYEAR_INTEXT : CitationType.AUTHORYEAR_PAR;
            result.add(new StoredCitation(mark, group(name, mark.citationKeys(), type), anchor, Optional.of(range), name));
        }
        return result;
    }

    private static CitationGroup group(String id, List<String> keys, CitationType type) {
        return new CitationGroup(new CitationGroupId(id), type, keys.stream().map(Citation::new).toList(), Optional.empty());
    }

    private static List<StoredCitation> order(List<StoredCitation> citations) {
        Map<XText, List<RangeSortEntry<StoredCitation>>> partitions = new LinkedHashMap<>();
        for (StoredCitation citation : citations) {
            partitions.computeIfAbsent(citation.anchor().getText(), _ -> new ArrayList<>()).add(new RangeSortEntry<>(citation.anchor(), 0, citation));
        }
        Map<XText, List<RangeSortEntry<StoredCitation>>> mapped = new LinkedHashMap<>();
        for (List<RangeSortEntry<StoredCitation>> partition : partitions.values()) {
            RangeSort.sortWithinPartition(partition);
            for (int index = 0; index < partition.size(); index++) {
                RangeSortEntry<StoredCitation> entry = partition.get(index);
                XTextRange range = UnoTextRange.getFootnoteMarkRange(entry.getRange()).orElse(entry.getRange());
                mapped.computeIfAbsent(range.getText(), _ -> new ArrayList<>()).add(new RangeSortEntry<>(range, index, entry.getContent()));
            }
        }
        for (List<RangeSortEntry<StoredCitation>> partition : mapped.values()) {
            RangeSort.sortWithinPartition(partition);
        }
        return mapped.values().stream().flatMap(List::stream).map(RangeSortEntry::getContent).toList();
    }

    private RenderResult render(List<StoredCitation> citations, List<BibDatabase> databases, CitationStyle style, boolean bibliography) throws RenderException {
        return new CslRenderer(style, entryTypes).render(new RenderRequest(citations.stream().map(StoredCitation::group).toList(), databases, options(bibliography)));
    }

    private RenderOptions options(boolean bibliography) {
        return new RenderOptions(BstCitationFormat.NUMERIC, bibliography, "", "", preferences.getCslBibliographyBodyFormat(), false);
    }

    private Map<String, ConversionMetadata> prepareMetadataLinks(List<StoredCitation> citations) throws com.sun.star.uno.Exception {
        Map<String, ConversionMetadata> updates = new LinkedHashMap<>();
        for (StoredCitation citation : citations) {
            if (!citation.originalName().equals(citation.mark().name())) {
                ConversionMetadata.prepareRelink(document, citation.originalName(), citation.mark().name())
                                  .ifPresent(metadata -> updates.put(citation.originalName(), metadata));
            }
        }
        return updates;
    }

    private void apply(List<StoredCitation> citations, RenderResult rendered, CitationStyle style, boolean bibliography, Map<String, ConversionMetadata> metadataUpdates)
            throws NoDocumentException, CreationException, com.sun.star.uno.Exception, StrategyException, MarkCodecException {
        for (StoredCitation citation : citations) {
            if (citation.range().isEmpty() || !rendered.markers().containsKey(citation.group().groupId)) {
                throw new CreationException("Missing Zotero citation range or rendered text");
            }
        }
        for (StoredCitation citation : citations) {
            WriterReferenceMark range = citation.range().orElseThrow(() -> new CreationException("Missing Zotero citation range"));
            OOText marker = Optional.ofNullable(rendered.markers().get(citation.group().groupId))
                                    .orElseThrow(() -> new CreationException("Missing rendered Zotero citation text"));
            if (!citation.originalName().equals(citation.mark().name())) {
                range = recreateLinkedMark(citation);
            }
            write(range, marker);
        }
        if (bibliography && style.hasBibliography()) {
            rebuildBibliography(rendered.bibliography());
        }
        for (ConversionMetadata metadata : metadataUpdates.values()) {
            metadata.store(document);
        }
        if (!ZoteroDocumentPreferences.writeCitationStyle(document, style)) {
            throw new StrategyException(Reason.DOCUMENT_FAILURE, "Cannot persist Zotero document preferences", Localization.lang("Could not update bibliography"));
        }
    }

    private WriterReferenceMark recreateLinkedMark(StoredCitation citation) throws NoDocumentException, CreationException, com.sun.star.uno.Exception {
        XTextContent content = WriterReferenceMarks.getAsTextContent(document, citation.originalName())
                                                   .orElseThrow(() -> new NoDocumentException("Zotero mark disappeared before applying its planned link"));
        XTextRange anchor = Optional.ofNullable(content.getAnchor()).orElseThrow(() -> new NoDocumentException("Missing Zotero link anchor"));
        XTextCursor position = anchor.getText().createTextCursorByRange(anchor);
        OOTextWriter.removeEscapementFormatting(position);
        anchor.getText().removeTextContent(content);
        position.collapseToStart();
        return ranges.create(document, context, citation.mark().name(), position, false, false, BoundaryPolicy.TEMPORARY);
    }

    private void write(WriterReferenceMark range, OOText marker) throws NoDocumentException, CreationException, com.sun.star.uno.Exception {
        ReferenceMarkTextWriter.write(document, context, range, marker);
    }

    void rebuildBibliography(List<OOText> bibliography) throws NoDocumentException, CreationException, WrappedTargetException {
        List<String> names = new ArrayList<>();
        for (String name : UnoTextSection.getNameAccess(document).getElementNames()) {
            if (name.startsWith(BIBLIOGRAPHY_PREFIX)) {
                names.add(name);
            }
        }
        if (names.isEmpty()) {
            XTextCursor end = document.getText().createTextCursor();
            end.gotoEnd(false);
            String name = BIBLIOGRAPHY_PREFIX + "{\"uncited\":[],\"omitted\":[],\"custom\":[]} CSL_BIBLIOGRAPHY RND"
                    + CitationIds.newId(10);
            names.add(UnoTextSection.create(new DocumentAnnotation(document, name, end, false)).getName());
        }
        OOText text = OOText.fromString(String.join("", bibliography.stream().map(entry -> entry.toString()).toList()));
        for (String name : names) {
            XTextRange range = UnoTextSection.getAnchor(document, name).orElseThrow(() -> new CreationException("Missing Zotero bibliography section"));
            XTextCursor cursor = range.getText().createTextCursorByRange(range);
            cursor.setString("");
            OOTextWriter.write(document, cursor, text);
        }
    }

    private CitationStyle requireStyle(OOStyle style) throws StrategyException {
        requireOpen();
        if (style instanceof CitationStyle cslStyle) {
            return cslStyle;
        }
        throw new StrategyException(Reason.UNSUPPORTED_STYLE, "Zotero compatibility mode requires a CSL style", Localization.lang("Cannot generate citation based on selected citation style."));
    }

    static CitationType requireType(CitationType type) throws StrategyException {
        return switch (type) {
            case AUTHORYEAR_PAR,
                 AUTHORYEAR_INTEXT,
                 INVISIBLE_CIT ->
                    type;
            default ->
                    throw new StrategyException(Reason.UNSUPPORTED_TYPE, "Zotero mode does not support this citation type", Localization.lang("Cannot generate citation based on selected citation style."));
        };
    }

    private void requireOpen() throws StrategyException {
        if (closed) {
            throw new StrategyException(Reason.CLOSED, "Zotero citation strategy is closed", Localization.lang("Not connected to document"));
        }
    }

    private static boolean hasAdjacentSpace(XTextCursor cursor, boolean after) {
        XTextCursor adjacent = cursor.getText().createTextCursorByRange(cursor.getEnd());
        boolean moved = after ? adjacent.goRight((short) 1, true) : adjacent.goLeft((short) 1, true);
        return moved && !adjacent.getString().isEmpty() && adjacent.getString().isBlank();
    }

    private static StrategyException failure(String localizedMessage, Throwable cause) {
        LOGGER.debug("Zotero citation operation failed", cause);
        return new StrategyException(Reason.DOCUMENT_FAILURE, "Zotero citation operation failed", localizedMessage, cause);
    }

    @Override
    public void close() {
        closed = true;
    }

    @NullMarked
    public enum Reason {
        CLOSED,
        DOCUMENT_FAILURE,
        INVALID_ENTRY,
        OVERLAPPING_RANGES,
        PAGE_INFO_NOT_SUPPORTED,
        PENDING_DELETIONS,
        PROTECTED_RANGE,
        UNSUPPORTED_MODIFIERS,
        UNSUPPORTED_STYLE,
        UNSUPPORTED_TYPE
    }

    @NullMarked
    public static final class StrategyException extends JabRefException {
        private final Reason reason;

        private StrategyException(Reason reason, String message, String localizedMessage) {
            super(message, localizedMessage);
            this.reason = reason;
        }

        private StrategyException(Reason reason, String message, String localizedMessage, Throwable cause) {
            super(message, localizedMessage, cause);
            this.reason = reason;
        }

        public Reason reason() {
            return reason;
        }
    }

    @NullMarked
    record StoredCitation(ZoteroReferenceMark mark, CitationGroup group, XTextRange anchor, Optional<WriterReferenceMark> range, String originalName) {
    }
}
