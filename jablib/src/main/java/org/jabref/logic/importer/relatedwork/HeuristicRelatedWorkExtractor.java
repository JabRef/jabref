package org.jabref.logic.importer.relatedwork;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

/**
 * Deterministic extractor for author–year style citations in "Related Work" sections.
 * Scans the whole section for patterns like (Vesce et al., 2016) and captures a context
 * span around each match to use as the descriptive snippet.
 */
public class HeuristicRelatedWorkExtractor implements RelatedWorkExtractor {

    // Headings like "1.3 Related work", "RELATED WORK", etc. (case-insensitive)
    private static final Pattern RELATED_WORK_HEADING =
            Pattern.compile("(?im)^(\\d+(?:\\.\\d+)*)?\\s*related\\s+work[s]?\\s*[:\\-]?");

    // Author–year patterns inside parentheses:
    // (Vesce et al., 2016)  (Vesce et al. 2016)  (Vesce, 2016)  (Vesce 2016)
    private static final Pattern AUTHOR_YEAR_PATTERN = Pattern.compile(
            "\\(([A-Z][A-Za-z\\-\\p{L}\\p{M}]+)"     // 1 = first Author token
                    + "(?:\\s+et\\s+al\\.)?"         // optional 'et al.'
                    + "(?:,)?\\s+"                   // optional comma + space
                    + "(\\d{4})\\)",                 // 2 = year
            Pattern.UNICODE_CASE);

    @Override
    public Map<String, String> extract(String fullText, List<BibEntry> bibliography) {
        String related = sliceRelatedWorkSection(fullText);
        Map<String, BibEntry> index = buildIndex(bibliography);
        Map<String, String> out = new LinkedHashMap<>();

        Matcher m = AUTHOR_YEAR_PATTERN.matcher(related);
        while (m.find()) {
            String citedSurname = normalizeSurname(m.group(1));
            String year = m.group(2);
            String citedKey = findKeyFor(citedSurname, year, index);
            if (citedKey == null || out.containsKey(citedKey)) {
                continue;
            }

            // Derive a descriptive snippet: expand to nearest sentence-ish boundaries.
            String snippet = expandToSentenceLikeSpan(related, m.start(), m.end());
            snippet = snippet.trim();
            if (snippet.length() > 300) {
                snippet = snippet.substring(0, 300) + "...";
            }
            out.put(citedKey, snippet);
        }

        return out;
    }

    private String sliceRelatedWorkSection(String text) {
        Matcher start = RELATED_WORK_HEADING.matcher(text);
        if (!start.find()) {
            return text; // fallback: whole text
        }
        int begin = start.end();

        // Cut at next numbered or ALL-CAPS heading
        Pattern nextSection = Pattern.compile("(?m)^(?:\\d+(?:\\.\\d+)*)\\s+[A-Z][A-Z\\s\\-]{3,}|^[A-Z][A-Z\\s\\-]{3,}$");
        Matcher end = nextSection.matcher(text);
        int stop = text.length();
        while (end.find()) {
            if (end.start() > begin) {
                stop = end.start();
                break;
            }
        }
        return text.substring(begin, stop);
    }

    private Map<String, BibEntry> buildIndex(List<BibEntry> bibs) {
        Map<String, BibEntry> idx = new HashMap<>();
        for (BibEntry b : bibs) {
            Optional<String> y = b.getField(StandardField.YEAR);
            Optional<String> a = b.getField(StandardField.AUTHOR);
            if (y.isEmpty() || a.isEmpty()) {
                continue;
            }
            String firstSurname = extractFirstSurname(a.get());
            if (!firstSurname.isEmpty()) {
                String k = normalizeSurname(firstSurname) + y.get();
                idx.put(k, b);
            }
        }
        return idx;
    }

    private String extractFirstSurname(String authorField) {
        // Split authors by ' and ' (BibTeX style)
        String firstAuthor = authorField.split("\\s+and\\s+")[0].trim();

        // Handle "Surname, Given" vs "Given Middle Surname"
        if (firstAuthor.contains(",")) {
            // e.g., "Vesce, A." → "Vesce"
            return firstAuthor.substring(0, firstAuthor.indexOf(',')).trim();
        }

        // Braced corporate authors: "{Company Name}" → take last token as a fallback
        if (firstAuthor.startsWith("{") && firstAuthor.endsWith("}")) {
            String inner = firstAuthor.substring(1, firstAuthor.length() - 1).trim();
            String[] parts = inner.split("\\s+");
            return parts.length == 0 ? "" : parts[parts.length - 1];
        }

        // "Given Middle Surname" → take the last token as surname
        String[] parts = firstAuthor.split("\\s+");
        return parts.length == 0 ? "" : parts[parts.length - 1];
    }

    private String normalizeSurname(String s) {
        String noBraces = s.replace("{", "").replace("}", "");
        String normalized = Normalizer.normalize(noBraces, Normalizer.Form.NFD)
                                      .replaceAll("\\p{M}+", "");
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String findKeyFor(String lowerSurname, String year, Map<String, BibEntry> index) {
        BibEntry entry = index.get(lowerSurname + year);
        // Return null when not found; caller guards against null.
        return (entry != null) ? entry.getCitationKey().orElse(null) : null;
    }

    /**
     * Return a context span that looks like a sentence around a match.
     * We expand left to the previous '.', '!' or '?' (or a line break),
     * and right to the next '.', '!' or '?' (or a line break).
     */
    private String expandToSentenceLikeSpan(String text, int matchStart, int matchEnd) {
        int left = matchStart;
        while (left > 0) {
            char c = text.charAt(left - 1);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                break;
            }
            left--;
        }
        int right = matchEnd;
        int len = text.length();
        while (right < len) {
            char c = text.charAt(right);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                right++; // include the boundary char
                break;
            }
            right++;
        }
        // Clamp in case no right boundary found
        if (right > len) {
            right = len;
        }
        return text.substring(left, right);
    }
}
