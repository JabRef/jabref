package org.jabref.gui.bibtexhighlighter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.gui.StateManager;
import org.jabref.gui.search.Highlighter;
import org.jabref.gui.search.SearchType;
import org.jabref.model.entry.field.Field;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.util.Range;

import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter;
import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter.BibTeXHighlightRegion;
import jfx.incubator.scene.control.richtext.SyntaxDecorator;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import org.jspecify.annotations.NullMarked;

/// [SyntaxDecorator] that applies BibTeX syntax highlighting and search-term highlighting
/// to the `CodeArea` used in [org.jabref.gui.entryeditor.SourceTab].
///
/// Highlighting itself is delegated to the Veneer [BibTeXSyntaxHighlighter], which
/// computes highlight regions over the full source text. Since `CodeArea` builds one
/// [RichParagraph] per line, this class:
///
/// * reassembles the model's lines into a single string and caches the computed
///   [BibTeXHighlightRegion]s so the (relatively expensive) lexing only happens when
///   the text has actually changed;
/// * maps each region back onto the requested line using cached line-start offsets;
/// * overlays any active search-query matches as translucent highlights on top of the
///   syntax styling.
///
/// Instances are stateful (they hold the last computed text/region cache) and are intended to
/// be long-lived, one per `CodeArea`, rather than recreated per paragraph.
@NullMarked
public class BibTeXHighlighter implements SyntaxDecorator {

    private final StateManager stateManager;
    private final BibTeXSyntaxHighlighter syntaxHighlighter;

    private Supplier<Map<Field, Range>> fieldPositionsProvider = Map::of;

    private String cachedFullText = "";
    private List<BibTeXHighlightRegion> cachedRegions = List.of();
    private int[] lineStarts = {0};
    private volatile boolean cacheDirty = true;

    /// Creates a new highlighter.
    ///
    /// @param stateManager      used to retrieve the active search query for term highlighting
    /// @param syntaxHighlighter the syntax highlighter used to compute highlight regions for BibTeX source code
    public BibTeXHighlighter(StateManager stateManager, BibTeXSyntaxHighlighter syntaxHighlighter) {
        this.stateManager = stateManager;
        this.syntaxHighlighter = syntaxHighlighter;
    }

    /// Sets the provider for BibTeX field ranges within the source text.
    ///
    /// @param fieldPositionsProvider supplier providing a mapping of fields to their global ranges
    public void setFieldPositionsProvider(Supplier<Map<Field, Range>> fieldPositionsProvider) {
        this.fieldPositionsProvider = fieldPositionsProvider;
    }

    /// Builds the styled paragraph for a single line of the model, applying both BibTeX syntax
    /// highlighting and (if a search query is active) search-match highlighting.
    ///
    /// @param model the code text model backing the `CodeArea`
    /// @param index the paragraph (line) index to render
    /// @return the styled [RichParagraph] for the requested line
    @Override
    public RichParagraph createRichParagraph(CodeTextModel model, int index) {
        refreshCacheIfNeeded(model);

        // VFlow can request a cell from the previous model state while the source is being replaced.
        if (index >= lineStarts.length) {
            return RichParagraph.builder().build();
        }

        String text = model.getPlainText(index);
        RichParagraph.Builder builder = RichParagraph.builder();

        List<Range> matches = getSearchMatches(text, lineStarts[index]);

        addSyntaxSegments(builder, text, lineStarts[index], matches);
        addSearchHighlights(builder, text, matches);

        return builder.build();
    }

    /// Recomputes the highlight-region cache if the model's full text has changed since the
    /// last call or if the cache was invalidated by [#handleChange].
    ///
    /// @param model the code text model backing the `CodeArea`
    private void refreshCacheIfNeeded(CodeTextModel model) {
        if (!cacheDirty) {
            return;
        }

        int count = model.size();
        int[] starts = new int[count];
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            starts[i] = sb.length();
            sb.append(model.getPlainText(i));
        }

        String fullText = sb.toString();
        if (fullText.equals(cachedFullText)) {
            cacheDirty = false;
            return;
        }

        cachedFullText = fullText;
        cachedRegions = syntaxHighlighter.computeHighlightRegions(fullText);
        lineStarts = starts;
        cacheDirty = false;
    }

    /// Adds the syntax-highlighted segments for the given line to the builder, translating each
    /// cached [BibTeXHighlightRegion] (expressed in offsets over the full document) into
    /// offsets local to this line, and splitting segments if they overlap with search matches.
    ///
    /// @param builder   the paragraph builder to append segments to
    /// @param text      the plain text of the current line
    /// @param lineStart the offset of this line's first character within the full cached text
    /// @param matches   list of active search match ranges within this line
    private void addSyntaxSegments(RichParagraph.Builder builder, String text, int lineStart, List<Range> matches) {
        int lineEnd = lineStart + text.length();
        int cursor = 0;

        for (BibTeXHighlightRegion region : cachedRegions) {
            if (region.end() <= lineStart) {
                continue;
            }
            if (region.start() >= lineEnd) {
                break;
            }

            int localStart = Math.max(region.start() - lineStart, 0);
            int localEnd = Math.min(region.end() - lineStart, text.length());

            if (localStart > cursor) {
                addSegmentWithSearchCheck(builder, text.substring(cursor, localStart), cursor, null, matches);
            }
            if (localEnd > localStart) {
                String styleClass = BibTeXStyleClass.valueOf(region.category().name()).getStyleClass();
                addSegmentWithSearchCheck(builder, text.substring(localStart, localEnd), localStart, styleClass, matches);
                cursor = localEnd;
            }
        }

        if (cursor < text.length()) {
            addSegmentWithSearchCheck(builder, text.substring(cursor), cursor, null, matches);
        }
    }

    /// Helper to add a text segment, splitting it if it intersects with search matches to apply
    /// the `search-highlight-text` CSS class dynamically.
    private void addSegmentWithSearchCheck(RichParagraph.Builder builder, String segmentText, int segmentStart, @org.jspecify.annotations.Nullable String baseStyleClass, List<Range> matches) {
        int segmentEnd = segmentStart + segmentText.length();
        int cursor = segmentStart;

        for (Range match : matches) {
            int matchStart = match.start() - 1;
            int matchEnd = match.end();

            if (matchEnd <= cursor || matchStart >= segmentEnd) {
                continue;
            }

            // Match overlaps with this segment
            if (matchStart > cursor) {
                // Segment part BEFORE match
                String sub = segmentText.substring(cursor - segmentStart, matchStart - segmentStart);
                appendSegment(builder, sub, baseStyleClass, false);
            }

            // Segment part INSIDE match
            int overlapStart = Math.max(cursor, matchStart);
            int overlapEnd = Math.min(segmentEnd, matchEnd);
            String subMatch = segmentText.substring(overlapStart - segmentStart, overlapEnd - segmentStart);
            appendSegment(builder, subMatch, baseStyleClass, true);

            cursor = overlapEnd;
        }

        // Remaining segment part AFTER matches
        if (cursor < segmentEnd) {
            String sub = segmentText.substring(cursor - segmentStart);
            appendSegment(builder, sub, baseStyleClass, false);
        }
    }

    private void appendSegment(RichParagraph.Builder builder, String text, @org.jspecify.annotations.Nullable String baseStyleClass, boolean isSearchMatch) {
        if (text.isEmpty()) {
            return;
        }

        if (isSearchMatch) {
            if (baseStyleClass != null) {
                builder.addWithStyleNames(text, baseStyleClass, "search-highlight-text");
            } else {
                builder.addWithStyleNames(text, "search-highlight-text");
            }
        } else {
            if (baseStyleClass != null) {
                builder.addWithStyleNames(text, baseStyleClass);
            } else {
                builder.addSegment(text);
            }
        }
    }

    /// Finds search matches in the current line.
    private List<Range> getSearchMatches(String text, int lineStart) {
        Optional<SearchQuery> searchQuery = stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).get();
        if (searchQuery.isEmpty()) {
            return List.of();
        }

        Map<Optional<Field>, List<String>> termsMap = Highlighter.groupTermsByField(searchQuery.get());
        Map<Field, Range> fieldPositions = fieldPositionsProvider.get();

        List<Range> matches = new ArrayList<>();
        termsMap.forEach((fieldOpt, terms) ->
                Highlighter.buildSearchPattern(terms).ifPresent(pattern -> {
                    List<Range> localMatches = Highlighter.findMatchPositions(text, pattern);
                    for (Range localMatch : localMatches) {
                        int globalStart = lineStart + localMatch.start() - 1;
                        int globalEnd = lineStart + localMatch.end() - 1;

                        if (fieldOpt.isEmpty()) {
                            matches.add(localMatch);
                        } else {
                            Field field = fieldOpt.get();
                            Range fieldRange = fieldPositions.get(field);
                            if (fieldRange != null && globalStart >= fieldRange.start() && globalEnd <= fieldRange.end()) {
                                matches.add(localMatch);
                            }
                        }
                    }
                }));
        return matches;
    }

    /// Adds background highlight rectangles for matches in the current line.
    private void addSearchHighlights(RichParagraph.Builder builder, String text, List<Range> matches) {
        for (Range match : matches) {
            int start = match.start() - 1;
            int length = match.end() - match.start() + 1;
            if (start >= 0 && start + length <= text.length()) {
                builder.addHighlight(start, length, "search-highlight");
            }
        }
    }

    /// Marks the cache as dirty so that subsequent rendering calls will rebuild
    /// syntax highlight regions for the updated text model.
    @Override
    public void handleChange(CodeTextModel m, TextPos start, TextPos end, int charsTop, int linesAdded, int charsBottom) {
        cacheDirty = true;
    }
}
