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
 * Handles single and multi-citation parentheticals, including diacritics and all-caps acronyms.
 */
public class HeuristicRelatedWorkExtractor implements RelatedWorkExtractor {

    // Headings like "1.4 Related work", "RELATED WORK", etc. (case-insensitive)
    private static final Pattern RELATED_WORK_HEADING =
            Pattern.compile("(?im)^(\\d+(?:\\.\\d+)*)?\\s*related\\s+work[s]?\\s*[:\\-]?$");

    // Any parenthetical block; author-year pairs are mined inside it.
    private static final Pattern PAREN_BLOCK = Pattern.compile("\\(([^)]+)\\)");

    // Unicode-aware author–year inside a parenthetical.
    // Allows all-caps acronyms like "CIA" and Unicode surnames like "Šimić".
    // \p{Lu} = uppercase letter, \p{L} = any letter, \p{M} = combining mark.
    private static final Pattern AUTHOR_YEAR_INNER = Pattern.compile(
            "(?U)"                               // enable Unicode character classes
                    + "(\\p{Lu}[\\p{L}\\p{M}'\\-]*)"       // 1: first author token (can be acronym or surname)
                    + "(?:\\s+(?:et\\s+al\\.)|\\s*(?:&|and)\\s+\\p{Lu}[\\p{L}\\p{M}'\\-]+)?"
                    + "\\s*,?\\s*"
                    + "(\\d{4})([a-z]?)"                  // 2: year, 3: optional trailing letter
    );

    /**
     * Extract a mapping from cited entry key to a short contextual snippet.
     *
     * <p>The returned map uses the cited entry's citation key (for example, {@code Smith2021})
     * as the key, and a sentence-like snippet taken from around the in-text citation as the value.</p>
     *
     * @param fullText the full (plain) text of the paper or section to scan
     * @param bibliography candidate entries that may be cited; used to resolve author/year to a citation key
     * @return a {@code Map} from citation key to snippet; never {@code null}, possibly empty
     */
    @Override
    public Map<String, String> extract(String fullText, List<BibEntry> bibliography) {
        String related = sliceRelatedWorkSection(fullText);
        Map<String, BibEntry> index = buildIndex(bibliography);
        Map<String, String> out = new LinkedHashMap<>();

        Matcher paren = PAREN_BLOCK.matcher(related);
        while (paren.find()) {
            String inner = paren.group(1);
            Matcher cite = AUTHOR_YEAR_INNER.matcher(inner);

            while (cite.find()) {
                String citedToken = normalizeSurname(cite.group(1)); // e.g., "cia" or "nash"
                String yearDigits = cite.group(2);                   // ignore group(3) letter
                String citedKey = findKeyFor(citedToken, yearDigits, index);
                if (citedKey == null || out.containsKey(citedKey)) {
                    continue;
                }

                String snippet = expandToSentenceLikeSpan(related, paren.start(), paren.end());
                snippet = pruneTrailingCitationTail(snippet).trim();

                if (!snippet.endsWith(".")) {
                    snippet = snippet + ".";
                }
                if (snippet.length() > 300) {
                    snippet = snippet.substring(0, 300) + "...";
                }

                out.put(citedKey, snippet);
            }
        }

        return out;
    }

    /**
     * Try to isolate the "Related work" section; fallback to full text.
     */
    private String sliceRelatedWorkSection(String text) {
        Matcher start = RELATED_WORK_HEADING.matcher(text);
        int begin = -1;
        while (start.find()) {
            begin = start.end();
            break;
        }
        if (begin < 0) {
            return text; // fallback: whole text
        }

        // Next likely section heading AFTER begin (numbered or ALL-CAPS)
        Pattern nextSection = Pattern.compile(
                "(?m)^(?:\\d+(?:\\.\\d+)*)\\s+[A-Z][A-Z\\s\\-]{3,}$|^[A-Z][A-Z\\s\\-]{3,}$");
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
            if (y.isEmpty()) {
                Optional<String> date = b.getField(StandardField.DATE);
                if (date.isPresent()) {
                    Matcher m = Pattern.compile("(\\d{4})").matcher(date.get());
                    if (m.find()) {
                        y = Optional.of(m.group(1));
                    }
                }
            }
            Optional<String> a = b.getField(StandardField.AUTHOR);
            if (y.isEmpty() || a.isEmpty()) {
                continue;
            }
            String yearDigits = y.get().replaceAll("[^0-9]", "");
            if (yearDigits.isEmpty()) {
                continue;
            }

            String firstAuthor = firstAuthorRaw(a.get());
            String firstSurname = extractFirstSurnameFromRaw(firstAuthor);
            if (!firstSurname.isEmpty()) {
                idx.put(normalizeSurname(firstSurname) + yearDigits, b);
            }

            // Also index acronym for corporate/multi-word first author without comma.
            String acronym = maybeAcronym(firstAuthor);
            if (!acronym.isEmpty()) {
                idx.put(acronym + yearDigits, b);
            }
        }
        return idx;
    }

    /**
     * Get the raw first author string (before surname extraction).
     */
    private String firstAuthorRaw(String authorField) {
        return authorField.split("\\s+and\\s+")[0].trim();
    }

    /**
     * Extract the first author surname from a raw first-author token.
     */
    private String extractFirstSurnameFromRaw(String firstAuthor) {
        if (firstAuthor.contains(",")) {
            return firstAuthor.substring(0, firstAuthor.indexOf(',')).trim();
        }
        if (firstAuthor.startsWith("{") && firstAuthor.endsWith("}")) {
            String inner = firstAuthor.substring(1, firstAuthor.length() - 1).trim();
            String[] parts = inner.split("\\s+");
            return parts.length == 0 ? "" : parts[parts.length - 1];
        }
        String[] parts = firstAuthor.split("\\s+");
        return parts.length == 0 ? "" : parts[parts.length - 1];
    }

    private String maybeAcronym(String firstAuthor) {
        if (firstAuthor.contains(",")) {
            return ""; // likely "Surname, Given" → skip acronym
        }
        String unbraced = firstAuthor;
        if (unbraced.startsWith("{") && unbraced.endsWith("}")) {
            unbraced = unbraced.substring(1, unbraced.length() - 1);
        }
        String[] parts = unbraced.trim().split("\\s+");
        if (parts.length < 2) {
            return ""; // single token → not helpful
        }
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            char c = p.charAt(0);
            if (Character.isLetter(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    /**
     * Normalize token: remove braces, strip diacritics, lowercase.
     */
    private String normalizeSurname(String s) {
        String noBraces = s.replace("{", "").replace("}", "");
        String normalized = Normalizer.normalize(noBraces, Normalizer.Form.NFD)
                                      .replaceAll("\\p{M}+", "");
        return normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * Lookup by normalized token (surname or acronym) + 4-digit year.
     */
    private String findKeyFor(String lowerToken, String yearDigits, Map<String, BibEntry> index) {
        BibEntry entry = index.get(lowerToken + yearDigits);
        return (entry != null) ? entry.getCitationKey().orElse(null) : null; // null signals "not found"
    }

    /**
     * Expand to a sentence-like span around the parenthetical match.
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
        if (right > len) {
            right = len;
        }
        return text.substring(left, right);
    }

    /**
     * Heuristically remove trailing citation trains at the end of a snippet
     */
    private String pruneTrailingCitationTail(String s) {
        int lastParen = s.lastIndexOf(')');
        if (lastParen > -1 && lastParen >= s.length() - 3) {
            String head = s.substring(0, lastParen + 1).trim();
            if (head.endsWith(").")) {
                return head;
            }
            if (head.endsWith(")")) {
                return head + ".";
            }
            return head;
        }
        return s;
    }
}
