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

/// BST-dependent labels and bodies come from one execution over the cited set, preserving set-wide
/// disambiguation. Requested bibliography bodies use one Pandoc process rather than one per entry.
/// Author–year citations without a bibliography require neither BST execution nor Pandoc.
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
        return new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT), true, false, false);
    }

    @Override
    public RenderResult render(RenderRequest request) throws RenderException {
        CitationKeyResolution data = new CitationKeyResolution(request);
        try {
            Map<String, BibItem> byKey = new LinkedHashMap<>();
            Map<String, Integer> numbers = Map.of();
            if (request.options().includeBibliography() || request.options().bstCitationFormat() != BstCitationFormat.AUTHOR_YEAR) {
                List<BibEntry> entries = data.flattenedEntries();
                ConvertToBibtexCleanup cleanup = new ConvertToBibtexCleanup();
                entries.forEach(cleanup::cleanup);
                List<BibItem> items = entries.isEmpty() ? List.of() : BstCitationFormatter.parseBibliography(new BstVM(style.getSource()).render(entries, new BibDatabase(entries)));
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
                numbers = CitationNumbering.bibliographyOrder(data.resolvedKeys(), order);
            }
            Map<String, String> labels = new LinkedHashMap<>();
            if (request.options().bstCitationFormat() == BstCitationFormat.STYLE_DEFINED) {
                byKey.values().forEach(item -> item.label().ifPresent(label -> labels.put(item.key(), label)));
            }
            Map<CitationGroupId, OOText> markers = new LinkedHashMap<>();
            for (CitationGroup group : request.groupsInDocumentOrder()) {
                List<String> keys = group.citationsInStorageOrder.stream().map(Citation::getCitationKey).toList();
                String text = group.citationType == CitationType.AUTHORYEAR_INTEXT
                              ? inText(keys, request.options().bstCitationFormat(), numbers, labels, data)
                              : switch (request.options().bstCitationFormat()) {
                                  case NUMERIC ->
                                          numeric(keys, numbers, data);
                                  case AUTHOR_YEAR ->
                                          authorYear(keys, data);
                                  case STYLE_DEFINED ->
                                          styleDefined(keys, labels, data);
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

    // [impl->req~openoffice.narrative-citations~1]
    private static String inText(List<String> keys, BstCitationFormat format, Map<String, Integer> numbers,
                                 Map<String, String> labels, CitationKeyResolution data) throws RenderException {
        StringJoiner text = new StringJoiner(", ");
        for (String key : keys) {
            if (data.lookup(key).isEmpty()) {
                text.add(CitationKeyResolution.unresolved(key));
                continue;
            }
            CitationLookupResult source = data.resolved(key);
            text.add(switch (format) {
                case AUTHOR_YEAR ->
                        StringEscapeUtils.escapeHtml4(BstCitationFormatter.authorYearInTextEntry(source.entry, source.database));
                case NUMERIC, STYLE_DEFINED -> {
                    String author = StringEscapeUtils.escapeHtml4(BstCitationFormatter.formatShortAuthorList(source.entry));
                    String marker = format == BstCitationFormat.NUMERIC
                                    ? numeric(List.of(key), numbers, data)
                                    : BstCitationFormatter.buildStyleDefinedCitationText(List.of(key), labels);
                    yield author + " " + marker;
                }
            });
        }
        return text.toString();
    }

    private static String numeric(List<String> keys, Map<String, Integer> numbers, CitationKeyResolution data) {
        return BstCitationFormatter.buildNumericCitationText(keys, numbers, keys.stream().filter(key -> data.lookup(key).isEmpty()).toList());
    }

    private static String authorYear(List<String> keys, CitationKeyResolution data) throws RenderException {
        StringJoiner text = new StringJoiner("; ", "(", ")");
        for (String key : keys) {
            if (data.lookup(key).isEmpty()) {
                text.add(CitationKeyResolution.unresolved(key));
            } else {
                CitationLookupResult source = data.resolved(key);
                text.add(StringEscapeUtils.escapeHtml4(BstCitationFormatter.authorYearEntry(source.entry, source.database)));
            }
        }
        return text.toString();
    }

    private static String styleDefined(List<String> keys, Map<String, String> labels, CitationKeyResolution data) throws MissingCitationLabelException {
        return BstCitationFormatter.buildStyleDefinedCitationText(keys, labels, data.unresolvedKeys());
    }

    private static String label(BibItem item) throws MissingCitationLabelException {
        return item.label().orElseThrow(() -> new MissingCitationLabelException(item.key()));
    }
}
