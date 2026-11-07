package org.jabref.logic.openoffice.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.JabRefItemDataProvider;
import org.jabref.logic.citationstyle.JabRefLocaleProvider;
import org.jabref.logic.openoffice.oocsltext.CSLFormatUtils;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationLookupResult;
import org.jabref.model.openoffice.style.CitationType;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.DefaultAbbreviationProvider;
import de.undercouch.citeproc.ListItemDataProvider;
import de.undercouch.citeproc.csl.CSLItemData;
import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A document-wide citeproc pass. All resolved keys are registered before any cluster is rendered,
/// so numeric ranges and bibliography ordering use citeproc's real numbers, not digit replacement.
/// Citeproc may revise earlier clusters; its returned indices are applied before producing markers.
@NullMarked
public final class CslRenderer implements CitationRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CslRenderer.class);

    private final CitationStyle style;
    private final BibEntryTypesManager entryTypesManager;

    public CslRenderer(CitationStyle style, BibEntryTypesManager entryTypesManager) {
        this.style = style;
        this.entryTypesManager = entryTypesManager;
    }

    @Override
    public StyleCapabilities capabilities() {
        return new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT, CitationType.INVISIBLE_CIT),
                style.hasBibliography(), false, false);
    }

    @Override
    public RenderResult render(RenderRequest request) throws RenderException {
        RenderData data = new RenderData(request);
        try {
            Optional<CSL> processor = data.resolvedKeys().isEmpty() ? Optional.empty() : Optional.of(createProcessor(data));
            BibDatabaseContext alphaContext = new BibDatabaseContext(new BibDatabase(style.isAlphanumericStyle() ? data.flattenedEntries() : List.of()));
            Map<Integer, String> renderedClusters = new HashMap<>();
            Map<CitationGroupId, List<Fragment>> fragments = new LinkedHashMap<>();
            for (CitationGroup group : request.groupsInDocumentOrder()) {
                CitationType type = capabilities().coerceForInsert(group.citationType);
                List<Fragment> groupFragments = new ArrayList<>();
                if (type == CitationType.AUTHORYEAR_INTEXT) {
                    for (Citation citation : group.citationsInStorageOrder) {
                        groupFragments.add(inText(citation.citationKey, data, processor, alphaContext, renderedClusters));
                    }
                } else if (type != CitationType.INVISIBLE_CIT) {
                    List<String> run = new ArrayList<>();
                    for (Citation citation : group.citationsInStorageOrder) {
                        if (data.lookup(citation.citationKey).isPresent()) {
                            run.add(citation.citationKey);
                        } else {
                            appendRun(run, groupFragments, processor, alphaContext, renderedClusters);
                            groupFragments.add(Fragment.literal(RenderData.unresolved(citation.citationKey)));
                        }
                    }
                    appendRun(run, groupFragments, processor, alphaContext, renderedClusters);
                }
                fragments.put(group.groupId, groupFragments);
            }
            Map<CitationGroupId, OOText> markers = new LinkedHashMap<>();
            for (CitationGroup group : request.groupsInDocumentOrder()) {
                String delimiter = capabilities().coerceForInsert(group.citationType) == CitationType.AUTHORYEAR_INTEXT ? ", " : "; ";
                StringJoiner text = new StringJoiner(delimiter);
                List<Fragment> groupFragments = Optional.ofNullable(fragments.get(group.groupId))
                                                        .orElseThrow(() -> new RenderException("CSL did not prepare a citation group"));
                for (Fragment fragment : groupFragments) {
                    text.add(fragment.text(renderedClusters));
                }
                markers.put(group.groupId, text.length() == 0 ? OOText.fromString("") : OOFormat.setLocaleNone(OOText.fromString(text.toString())));
            }
            List<OOText> bibliography = new ArrayList<>();
            if (request.options().includeBibliography() && style.hasBibliography()) {
                bibliography.addAll(BibliographyText.start(request.options()));
                if (processor.isPresent()) {
                    for (String entry : processor.orElseThrow().makeBibliography().getEntries()) {
                        bibliography.add(BibliographyText.entry(CSLFormatUtils.transformHTML(entry), request.options()));
                    }
                }
            }
            return new RenderResult(markers, bibliography, data.unresolvedKeys());
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            LOGGER.debug("CSL rendering failed", e);
            throw new RenderException("Cannot render CSL citations", e);
        }
    }

    private CSL createProcessor(RenderData data) throws IOException, RenderException {
        List<CSLItemData> items = new ArrayList<>();
        JabRefItemDataProvider provider = new JabRefItemDataProvider();
        for (String key : data.resolvedKeys()) {
            CitationLookupResult source = data.resolved(key);
            BibEntry copy = new BibEntry(source.entry).withCitationKey(key);
            provider.setData(List.of(copy), new BibDatabaseContext(source.database), entryTypesManager);
            items.add(Optional.ofNullable(provider.retrieveItem(key)).orElseThrow(() -> new RenderException("Cannot convert a resolved citation to CSL data: " + key)));
        }
        CSL processor = new CSL(new ListItemDataProvider(items), new JabRefLocaleProvider(), new DefaultAbbreviationProvider(), style.getSource(), "en-US");
        processor.setOutputFormat("html");
        processor.registerCitationItems(data.resolvedKeys(), style.isNumericStyle() && !style.hasBibliographySortOrder());
        return processor;
    }

    private void appendRun(List<String> keys, List<Fragment> fragments, Optional<CSL> processor,
                           BibDatabaseContext alphaContext, Map<Integer, String> renderedClusters) throws RenderException {
        if (keys.isEmpty()) {
            return;
        }
        if (style.isAlphanumericStyle()) {
            List<BibEntry> entries = new ArrayList<>();
            for (String key : keys) {
                entries.add(alphaContext.getDatabase().getEntryByCitationKey(key).orElseThrow(() -> new RenderException("Alphanumeric citation is unresolved: " + key)));
            }
            fragments.add(Fragment.literal(CSLFormatUtils.transformHTML(CSLFormatUtils.generateAlphanumericCitation(entries, alphaContext))));
        } else {
            fragments.add(cluster(processor, keys, "", false, renderedClusters));
        }
        keys.clear();
    }

    private Fragment inText(String key, RenderData data, Optional<CSL> processor,
                            BibDatabaseContext alphaContext, Map<Integer, String> renderedClusters) throws RenderException {
        if (data.lookup(key).isEmpty()) {
            return Fragment.literal(RenderData.unresolved(key));
        }
        if (style.isAlphanumericStyle()) {
            BibEntry entry = alphaContext.getDatabase().getEntryByCitationKey(key).orElseThrow(() -> new RenderException("Alphanumeric citation is unresolved: " + key));
            return Fragment.literal(CSLFormatUtils.transformHTML(CSLFormatUtils.generateAlphanumericInTextCitation(entry, alphaContext)));
        }
        CitationLookupResult source = data.resolved(key);
        String prefix = style.isNumericStyle()
                        ? StringEscapeUtils.escapeHtml4(CSLFormatUtils.generateAuthorPrefix(source.entry, new BibDatabaseContext(source.database)))
                        : "";
        return cluster(processor, List.of(key), prefix, !style.isNumericStyle(), renderedClusters);
    }

    private static Fragment cluster(Optional<CSL> processor, List<String> keys, String prefix,
                                    boolean narrative, Map<Integer, String> renderedClusters) throws RenderException {
        CSL csl = processor.orElseThrow(() -> new RenderException("No CSL processor for a resolved citation"));
        List<de.undercouch.citeproc.output.Citation> updates = csl.makeCitation(keys);
        if (updates.isEmpty()) {
            throw new RenderException("Citeproc returned no citation text");
        }
        for (de.undercouch.citeproc.output.Citation update : updates) {
            renderedClusters.put(update.getIndex(), update.getText());
        }
        return new Fragment(Optional.of(updates.getLast().getIndex()), "", prefix, narrative);
    }

    @NullMarked
    private record Fragment(Optional<Integer> citationIndex, String literal, String prefix, boolean narrative) {
        static Fragment literal(String text) {
            return new Fragment(Optional.empty(), text, "", false);
        }

        String text(Map<Integer, String> renderedClusters) throws RenderException {
            if (citationIndex.isEmpty()) {
                return literal;
            }
            String html = Optional.ofNullable(renderedClusters.get(citationIndex.orElseThrow()))
                                  .orElseThrow(() -> new RenderException("Citeproc omitted a rendered citation cluster"));
            String text = CSLFormatUtils.transformHTML(html);
            return prefix + (narrative ? CSLFormatUtils.changeToInText(text) : text);
        }
    }
}
