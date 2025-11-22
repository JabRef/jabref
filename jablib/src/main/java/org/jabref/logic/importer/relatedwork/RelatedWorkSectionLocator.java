package org.jabref.logic.importer.relatedwork;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the "Related Work" (or variants) section and returns its text span.
 */
public final class RelatedWorkSectionLocator {

    /**
     * Immutable span describing the located section.
     */
    public static final class SectionSpan {
        public final int headerStart;
        public final int headerEnd;
        public final int startOffset; // body start (after header line break)
        public final int endOffset;   // body end (before next header or EOF)
        public final String headerText;

        public SectionSpan(int headerStart, int headerEnd, int startOffset, int endOffset, String headerText) {
            this.headerStart = headerStart;
            this.headerEnd = headerEnd;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.headerText = headerText;
        }
    }

    // Header patterns (case-insensitive), allowing optional numbering like "2", "2.1", or Roman numerals.
    private static final Pattern HEADER_PATTERN = Pattern.compile(
            "^(?:\\s*(?:\\d+(?:\\.\\d+)*|[IVXLCDM]+)\\.?\\s+)?"
                    + "(?:RELATED\\s+WORKS?"
                    + "|BACKGROUND\\s+AND\\s+RELATED\\s+WORK"
                    + "|LITERATURE\\s+REVIEW"
                    + "|STATE\\s+OF\\s+THE\\s+ART)"
                    + "\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE
    );

    // Fragments to help detect a *generic* next header in a line-by-line scan.
    private static final Pattern OPT_NUMBERING = Pattern.compile("^\\s*(?:\\d+(?:\\.\\d+)*|[IVXLCDM]+)\\.?\\s+");
    private static final Pattern ALL_CAPS_BODY = Pattern.compile("^[A-Z][A-Z \\-]{2,}$");
    private static final Pattern TITLE_CASE_WORD = Pattern.compile("[A-Z][\\p{L}\\p{M}\\-]+");

    public RelatedWorkSectionLocator() {
    }

    /**
     * Instance entry point (delegates to static).
     */
    public Optional<SectionSpan> locate(String text) {
        return locateStatic(text);
    }

    /**
     * Static entry point for convenience in callers/tests.
     */
    public static Optional<SectionSpan> locateStatic(String text) {
        if (text == null || text.isEmpty()) {
            return Optional.empty();
        }

        Matcher headerMatcher = HEADER_PATTERN.matcher(text);
        if (!headerMatcher.find()) {
            return Optional.empty();
        }

        int headerStart = headerMatcher.start();
        int headerEnd = headerMatcher.end();

        // Body starts right after the header line break(s)
        int startOffset = headerEnd;
        if (startOffset < text.length() && text.charAt(startOffset) == '\r') {
            startOffset++;
        }
        if (startOffset < text.length() && text.charAt(startOffset) == '\n') {
            startOffset++;
        }

        // Determine end by scanning subsequent lines until a "generic header" is found.
        int endOffset = findNextHeaderBoundary(text, startOffset);

        // Trim trailing whitespace from the section body.
        while (endOffset > startOffset && Character.isWhitespace(text.charAt(endOffset - 1))) {
            endOffset--;
        }

        String headerText = extractHeaderLine(text, headerStart, headerEnd);
        return Optional.of(new SectionSpan(headerStart, headerEnd, startOffset, endOffset, headerText));
    }

    private static int findNextHeaderBoundary(String text, int startFrom) {
        int pos = startFrom;
        final int n = text.length();

        while (pos < n) {
            int lineEnd = indexOfNewline(text, pos);
            String line = text.substring(pos, lineEnd);

            if (looksLikeHeader(line)) {
                return pos; // cut section before this header
            }
            pos = (lineEnd < n) ? lineEnd + 1 : n; // move to next line (skip '\n')
        }
        return n; // no later header; section runs to EOF
    }

    private static boolean looksLikeHeader(String rawLine) {
        String line = rawLine.strip();
        if (line.isEmpty()) {
            return false;
        }

        // Remove optional numbering prefix for header-shape checks
        String core = stripNumbering(line);

        // 1) ALL CAPS headers (e.g., "3 METHODS", "RESULTS")
        if (ALL_CAPS_BODY.matcher(core).matches()) {
            return true;
        }

        // 2) Title-Case short headers: 1–6 words, each capitalized
        //    Avoid sentences by requiring few words and no trailing punctuation.
        if (core.length() <= 80 && !endsWithPunctuation(core)) {
            String[] parts = core.split("\\s+");
            if (parts.length >= 1 && parts.length <= 6) {
                int titleLike = 0;
                for (String p : parts) {
                    if (TITLE_CASE_WORD.matcher(p).matches()) {
                        titleLike++;
                    }
                }
                if (titleLike == parts.length) {
                    return true;
                }
            }
        }

        return false;
    }

    private static String stripNumbering(String s) {
        Matcher m = OPT_NUMBERING.matcher(s);
        return m.find() ? s.substring(m.end()).stripLeading() : s;
    }

    private static boolean endsWithPunctuation(String s) {
        if (s.isEmpty()) {
            return false;
        }
        char c = s.charAt(s.length() - 1);
        return c == '.' || c == ':' || c == ';' || c == '!' || c == '?';
    }

    private static int indexOfNewline(String text, int from) {
        int idx = text.indexOf('\n', from);
        return (idx == -1) ? text.length() : idx;
    }

    private static String extractHeaderLine(String text, int headerStart, int headerEnd) {
        int lineStart = text.lastIndexOf('\n', Math.max(0, headerStart - 1));
        lineStart = (lineStart == -1) ? 0 : lineStart + 1;
        int lineEnd = text.indexOf('\n', headerEnd);
        if (lineEnd == -1) {
            lineEnd = text.length();
        }
        return text.substring(lineStart, lineEnd).trim();
    }
}
