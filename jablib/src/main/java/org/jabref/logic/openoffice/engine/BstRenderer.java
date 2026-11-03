package org.jabref.logic.openoffice.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import org.jabref.logic.bst.BstVM;
import org.jabref.logic.bst.BstVMException;
import org.jabref.logic.cleanup.ConvertToBibtexCleanup;
import org.jabref.logic.openoffice.bst.BSTFormatUtils;
import org.jabref.logic.openoffice.bst.PandocLatexConverter;
import org.jabref.logic.openoffice.engine.BstCitationFormatter.BibItem;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationLookupResult;
import org.jabref.model.openoffice.style.CitationType;

import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Labels and entry bodies come from one BST execution over the whole cited set. Bibliography
/// bodies are converted in one pandoc process, rather than rerendering each entry in isolation,
/// which would lose set-wide disambiguation and start hundreds of external processes.
@NullMarked
public final class BstRenderer implements CitationRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BstRenderer.class);

    private final BstStyle style;
    private final PandocLatexConverter pandoc;
    private final Optional<String> pandocPath;

    public BstRenderer(BstStyle style, String pandocPath) {
        this(style, new PandocLatexConverter(pandocPath), Optional.of(pandocPath));
    }

    public BstRenderer(BstStyle style, PandocLatexConverter pandoc) {
        this(style, pandoc, Optional.empty());
    }

    private BstRenderer(BstStyle style, PandocLatexConverter pandoc, Optional<String> pandocPath) {
        this.style = style;
        this.pandoc = pandoc;
        this.pandocPath = pandocPath;
    }

    @Override
    public StyleCapabilities capabilities() {
        return new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR), true, false, false);
    }

    @Override
    public RenderResult render(RenderRequest request) throws RenderException {
        RenderData data = new RenderData(request);
        try {
            List<BibEntry> entries = data.flattenedEntries();
            ConvertToBibtexCleanup cleanup = new ConvertToBibtexCleanup();
            entries.forEach(cleanup::cleanup);
            List<BibItem> items = entries.isEmpty() ? List.of() : BstCitationFormatter.parseBibliography(new BstVM(style.getSource()).render(entries, new BibDatabase(entries)));
            Map<String, BibItem> byKey = new LinkedHashMap<>();
            items.forEach(item -> byKey.putIfAbsent(item.key(), item));
            for (String key : data.resolvedKeys()) {
                if (!byKey.containsKey(key)) {
                    throw new RenderException("The BST style omitted cited entry " + key);
                }
                if (request.options().bstCitationFormat() == BstCitationFormat.STYLE_DEFINED) {
                    label(byKey.get(key));
                }
            }
            List<String> order = style.hasSortCommand() || request.options().bstCitationFormat() != BstCitationFormat.NUMERIC
                                 ? items.stream().map(BibItem::key).toList() : List.of();
            Map<String, Integer> numbers = CitationNumbering.bibliographyOrder(data.resolvedKeys(), order);
            Map<CitationGroupId, OOText> markers = new LinkedHashMap<>();
            for (CitationGroup group : request.groupsInDocumentOrder()) {
                String text = switch (request.options().bstCitationFormat()) {
                    case NUMERIC ->
                            numeric(group, numbers, data);
                    case AUTHOR_YEAR ->
                            authorYear(group, data);
                    case STYLE_DEFINED ->
                            styleDefined(group, byKey, data);
                };
                markers.put(group.groupId, OOFormat.setLocaleNone(OOText.fromString(text)));
            }
            List<OOText> bibliography = new ArrayList<>();
            if (request.options().includeBibliography()) {
                bibliography.addAll(BibliographyText.start(request.options()));
                List<BibItem> orderedItems = numbers.keySet().stream().map(byKey::get).toList();
                List<String> bodies = convertBodies(orderedItems);
                for (int i = 0; i < orderedItems.size(); i++) {
                    BibItem item = orderedItems.get(i);
                    String prefix = switch (request.options().bstCitationFormat()) {
                        case NUMERIC ->
                                "[" + numbers.get(item.key()) + "] ";
                        case STYLE_DEFINED ->
                                "[" + StringEscapeUtils.escapeHtml4(label(item)) + "] ";
                        case AUTHOR_YEAR ->
                                "";
                    };
                    bibliography.add(BibliographyText.entry(prefix + bodies.get(i), request.options()));
                }
            }
            return new RenderResult(markers, bibliography, data.unresolvedKeys());
        } catch (BstVMException | IllegalArgumentException | IllegalStateException e) {
            LOGGER.debug("BST rendering failed", e);
            throw new RenderException("Cannot render BST citations or bibliography", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("BST conversion interrupted", e);
            throw new RenderException("BST bibliography conversion was interrupted", e);
        }
    }

    private List<String> convertBodies(List<BibItem> items) throws InterruptedException, RenderException {
        if (items.isEmpty()) {
            return List.of();
        }
        String separator = "JabRefBibliographyBoundary";
        String source = String.join("\n", items.stream().map(BibItem::latex).toList());
        while (source.contains(separator)) {
            separator += "X";
        }
        StringJoiner latex = new StringJoiner("\n\n");
        for (int i = 0; i < items.size(); i++) {
            latex.add(separator + i).add(BSTFormatUtils.normalizeLegacyForPandoc(items.get(i).latex()));
        }
        String html;
        try {
            html = pandoc.latexToHtml(latex.toString());
        } catch (IOException exception) {
            LOGGER.debug("Pandoc bibliography conversion failed", exception);
            throw new PandocRenderException(pandocPath, exception);
        }
        List<String> result = new ArrayList<>();
        int position = 0;
        for (int i = 0; i < items.size(); i++) {
            String boundary = "<p>" + separator + i + "</p>";
            int start = html.indexOf(boundary, position);
            if (start < 0) {
                throw new RenderException("Pandoc did not preserve a bibliography entry boundary");
            }
            int bodyStart = start + boundary.length();
            int end = i + 1 == items.size() ? html.length() : html.indexOf("<p>" + separator + (i + 1) + "</p>", bodyStart);
            if (end < 0) {
                throw new RenderException("Pandoc did not preserve a bibliography entry boundary");
            }
            result.add(BSTFormatUtils.convertPandocHtmlToOOText(html.substring(bodyStart, end)));
            position = end;
        }
        return result;
    }

    private static String numeric(CitationGroup group, Map<String, Integer> numbers, RenderData data) {
        List<String> keys = group.citationsInStorageOrder.stream().map(Citation::getCitationKey).toList();
        return BstCitationFormatter.buildNumericCitationText(keys, numbers, keys.stream().filter(key -> data.lookup(key).isEmpty()).toList());
    }

    private static String authorYear(CitationGroup group, RenderData data) throws RenderException {
        StringJoiner text = new StringJoiner("; ", "(", ")");
        for (Citation citation : group.citationsInStorageOrder) {
            if (data.lookup(citation.citationKey).isEmpty()) {
                text.add(RenderData.unresolved(citation.citationKey));
            } else {
                CitationLookupResult source = data.resolved(citation.citationKey);
                text.add(StringEscapeUtils.escapeHtml4(BstCitationFormatter.authorYearEntry(source.entry, source.database)));
            }
        }
        return text.toString();
    }

    private static String styleDefined(CitationGroup group, Map<String, BibItem> items, RenderData data) throws MissingCitationLabelException {
        Map<String, String> labels = new LinkedHashMap<>();
        items.values().forEach(item -> item.label().ifPresent(label -> labels.put(item.key(), label)));
        return BstCitationFormatter.buildStyleDefinedCitationText(group.citationsInStorageOrder.stream().map(Citation::getCitationKey).toList(), labels, data.unresolvedKeys());
    }

    private static String label(BibItem item) throws MissingCitationLabelException {
        return item.label().orElseThrow(() -> new MissingCitationLabelException(item.key()));
    }
}
