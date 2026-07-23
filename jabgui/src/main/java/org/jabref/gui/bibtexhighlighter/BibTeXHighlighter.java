package org.jabref.gui.bibtexhighlighter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.scene.paint.Color;

import org.jabref.gui.StateManager;
import org.jabref.gui.search.Highlighter;
import org.jabref.logic.util.strings.StringUtil;
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

/**
 * {@link SyntaxDecorator} that applies BibTeX syntax highlighting and search-term highlighting
 * to the {@code CodeArea} used in {@link org.jabref.gui.entryeditor.SourceTab}.
 * <p>
 * Highlighting itself is delegated to the Veneer {@link BibTeXSyntaxHighlighter}, which
 * computes highlight regions over the full source text. Since {@code CodeArea} builds one
 * {@link RichParagraph} per line, this class:
 * <ul>
 *     <li>reassembles the model's lines into a single string and caches the computed
 *     {@link BibTeXHighlightRegion}s so the (relatively expensive) lexing only happens when
 *     the text has actually changed;</li>
 *     <li>maps each region back onto the requested line using cached line-start offsets;</li>
 *     <li>overlays any active search-query matches as translucent highlights on top of the
 *     syntax styling.</li>
 * </ul>
 * <p>
 * Instances are stateful (they hold the last computed text/region cache) and are intended to
 * be long-lived, one per {@code CodeArea}, rather than recreated per paragraph.
 */
@NullMarked
public class BibTeXHighlighter implements SyntaxDecorator {

    private final StateManager stateManager;
    private final BibTeXSyntaxHighlighter syntaxHighlighter;

    private String cachedFullText = "";
    private List<BibTeXHighlightRegion> cachedRegions = List.of();
    private int[] lineStarts = {0};

    /**
     * Creates a new highlighter.
     *
     * @param stateManager      used to read the currently active global search query so matches can
     *                          be highlighted alongside syntax highlighting
     * @param syntaxHighlighter the syntax highlighter used to compute highlight regions for BibTeX source code
     */
    public BibTeXHighlighter(StateManager stateManager, BibTeXSyntaxHighlighter syntaxHighlighter) {
        this.stateManager = stateManager;
        this.syntaxHighlighter = syntaxHighlighter;
    }

    /**
     * Builds the styled paragraph for a single line of the model, applying both BibTeX syntax
     * highlighting and (if a search query is active) search-match highlighting.
     *
     * @param model the code text model backing the {@code CodeArea}
     * @param index the paragraph (line) index to render
     * @return the styled {@link RichParagraph} for the requested line
     */
    @Override
    public RichParagraph createRichParagraph(CodeTextModel model, int index) {
        refreshCacheIfNeeded(model);

        String text = model.getPlainText(index);
        RichParagraph.Builder builder = RichParagraph.builder();

        addSyntaxSegments(builder, text, lineStarts[index]);
        addSearchHighlights(builder, text);

        return builder.build();
    }

    /**
     * Recomputes the highlight-region cache if the model's full text has changed since the
     * last call. Reassembles the model's lines (joined by {@code \n}) and, if the resulting text
     * differs from what was cached, recomputes both the highlight regions and the per-line start
     * offsets used to translate global offsets into line-local ones.
     *
     * @param model the code text model backing the {@code CodeArea}
     */
    private void refreshCacheIfNeeded(CodeTextModel model) {
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
            return;
        }

        cachedFullText = fullText;
        cachedRegions = syntaxHighlighter.computeHighlightRegions(fullText);
        lineStarts = starts;
    }

    /**
     * Adds the syntax-highlighted segments for the given line to the builder, translating each
     * cached {@link BibTeXHighlightRegion} (expressed in offsets over the full document) into
     * offsets local to this line, and skipping regions that don't intersect the line.
     *
     * @param builder   the paragraph builder to append segments to
     * @param text      the plain text of the current line
     * @param lineStart the offset of this line's first character within the full cached text
     */
    private void addSyntaxSegments(RichParagraph.Builder builder, String text, int lineStart) {
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
                builder.addSegment(text.substring(cursor, localStart));
            }
            if (localEnd > localStart) {
                String styleClass = BibTeXStyleClass.valueOf(region.category().name()).getStyleClass();
                builder.addWithStyleNames(text.substring(localStart, localEnd), styleClass);
                cursor = localEnd;
            }
        }

        if (cursor < text.length()) {
            builder.addSegment(text.substring(cursor));
        }
    }

    /**
     * Overlays highlights for any matches of the currently active global search query within
     * the given line. Does nothing if no search query is active.
     *
     * @param builder the paragraph builder to add highlights to
     * @param text    the plain text of the current line
     */
    private void addSearchHighlights(RichParagraph.Builder builder, String text) {
        String query = stateManager.searchQueryProperty().get();
        if (StringUtil.isBlank(query)) {
            return;
        }

        SearchQuery searchQuery = new SearchQuery(query);
        Map<Optional<Field>, List<String>> termsMap = Highlighter.groupTermsByField(searchQuery);

        List<Range> matches = new ArrayList<>();
        termsMap.forEach((field, terms) ->
                Highlighter.buildSearchPattern(terms).ifPresent(pattern ->
                        matches.addAll(Highlighter.findMatchPositions(text, pattern))));

        for (Range match : matches) {
            int start = match.start() - 1;
            int length = match.end() - match.start() + 1;
            if (start >= 0 && start + length <= text.length()) {
                builder.addHighlight(start, length, Color.ORANGE);
            }
        }
    }

    /**
     * No-op. This decorator recomputes styling per-paragraph in {@link #createRichParagraph}
     * rather than incrementally reacting to edits, so there is nothing to do on model changes.
     */
    @Override
    public void handleChange(CodeTextModel m, TextPos start, TextPos end, int charsTop, int linesAdded, int charsBottom) {}
}
