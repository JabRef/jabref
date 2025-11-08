package org.jabref.logic.importer.relatedwork;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic locator for the "Related Work" (or "Literature Review") section in a paper's full plain text.
 * <p>
 * Strategy:
 * 1) Split text into lines.
 * 2) Find the first line that looks like a top-level section header for related work.
 * 3) Capture until the next top-level header (or end of text).
 * <p>
 * This does NOT parse PDFs. Feed it already-extracted plain text (from a PDF, HTML, arXiv, etc.).
 */
public final class RelatedWorkSectionLocator {

    // Matches typical top-level section headers (optionally numbered).
    // Examples: "2 Related Work", "3.1 Literature Review", "RELATED WORK", "Background and Related Work"
    private static final Pattern TOP_LEVEL_HEADER = Pattern.compile(
            "^(?<num>(\\d+\\s*|\\d+(?:\\.\\d+)*\\s+)?)(?<title>[A-Za-z].{0,80})$"
    );

    // Titles we consider as "related work" headers (lowercased, spaces normalized).
    private static final List<Pattern> RELATED_TITLES = List.of(
            // exact forms
            Pattern.compile("\\brelated\\s+work\\b"),
            Pattern.compile("\\bliterature\\s+review\\b"),
            // combined/variant forms
            Pattern.compile("\\bbackground\\s+(and|&)\\s+related\\s+work\\b"),
            Pattern.compile("\\bprior\\s+work\\b"),
            Pattern.compile("\\bprevious\\s+work\\b"),
            Pattern.compile("\\brelated\\s+(studies|research)\\b"),
            // long variants often seen
            Pattern.compile("\\bstate\\s+of\\s+the\\s+art\\b")
    );

    // A simple guard to identify *any* top-level header (used as end boundary)
    // Be permissive but avoid false positives like sentence lines.
    private static final Pattern ANY_TOP_LEVEL_HEADER = Pattern.compile(
            "^(\\d+\\s*|\\d+(?:\\.\\d+)*\\s+)?[A-Z][A-Za-z0-9 \\-/,&()]{2,}$"
    );

    public Optional<String> locate(String fullPlainText) {
        Objects.requireNonNull(fullPlainText, "fullPlainText");

        String[] lines = fullPlainText.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        int startLine = findRelatedHeaderLine(lines).orElse(-1);
        if (startLine < 0) {
            return Optional.empty();
        }
        int endLine = findNextTopLevelHeader(lines, startLine + 1).orElse(lines.length);
        String block = joinKeepingParagraphs(lines, startLine + 1, endLine); // exclude header line itself
        String trimmed = block.strip();
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }

    private Optional<Integer> findRelatedHeaderLine(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i].strip();
            if (raw.isEmpty()) {
                continue;
            }
            Matcher m = TOP_LEVEL_HEADER.matcher(raw);
            if (!m.matches()) {
                continue;
            }
            String title = normalizeTitle(m.group("title"));
            if (isRelatedTitle(title)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private Optional<Integer> findNextTopLevelHeader(String[] lines, int fromExclusive) {
        for (int i = fromExclusive; i < lines.length; i++) {
            String raw = lines[i].strip();
            if (raw.isEmpty()) {
                continue;
            }
            if (ANY_TOP_LEVEL_HEADER.matcher(raw).matches() && !looksLikeFigureOrTableCaption(raw)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private static boolean looksLikeFigureOrTableCaption(String s) {
        String lc = s.toLowerCase(Locale.ROOT);
        return lc.startsWith("figure ") || lc.startsWith("fig. ") || lc.startsWith("table ");
    }

    private static String normalizeTitle(String title) {
        String t = title.toLowerCase(Locale.ROOT).trim();
        t = t.replaceAll("\\s+", " ");
        t = t.replaceAll("[.:–—-]+$", "");
        return t;
    }

    private static boolean isRelatedTitle(String normalizedLowerTitle) {
        for (Pattern p : RELATED_TITLES) {
            if (p.matcher(normalizedLowerTitle).find()) {
                return true;
            }
        }
        return false;
    }

    private static String joinKeepingParagraphs(String[] lines, int startInclusive, int endExclusive) {
        List<String> kept = new ArrayList<>(Math.max(0, endExclusive - startInclusive));
        for (int i = startInclusive; i < endExclusive; i++) {
            kept.add(lines[i]);
        }
        // Keep line breaks as-is; the extractor can handle sentence splitting downstream.
        return String.join("\n", kept);
    }
}
