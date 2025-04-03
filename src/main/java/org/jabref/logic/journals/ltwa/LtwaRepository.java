package org.jabref.logic.journals.ltwa;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A repository for LTWA (List of Title Word Abbreviations) entries.
 * Provides methods for retrieving and applying abbreviations based on LTWA rules.
 */
@SuppressWarnings("checkstyle:RegexpMultiline")
public class LtwaRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(LtwaRepository.class);
    private static final Pattern INFLECTION = Pattern.compile("[ieasn'’]{1,3}");
    private static final Pattern BOUNDARY = Pattern.compile("[\\s\\u2013\\u2014_.,:;!|=+*\\\\/\"()&#%@$?]");

    private final PrefixTree<LtwaEntry> prefix = new PrefixTree<>();
    private final PrefixTree<LtwaEntry> suffix = new PrefixTree<>();

    /**
     * Creates a new LtwaRepository from an MV store file.
     *
     * @param ltwaListFile Path to the LTWA MVStore file
     */
    public LtwaRepository(Path ltwaListFile) {
        try (var store = new MVStore.Builder().readOnly().fileName(ltwaListFile.toAbsolutePath().toString()).open()) {
            MVMap<String, LtwaEntry> prefixMap = store.openMap("Prefixes");
            MVMap<String, LtwaEntry> suffixMap = store.openMap("Suffixes");

            for (String key : prefixMap.keySet()) {
                var value = prefixMap.get(key);
                if (value != null) {
                    prefix.insert(key, value);
                }
            }

            for (String key : suffixMap.keySet()) {
                var value = suffixMap.get(key);
                if (value != null) {
                    suffix.insert(key, value);
                }
            }

            LOGGER.debug("Loaded LTWA repository with {} prefixes and {} suffixes", prefixMap.size(), suffixMap.size());
        }
    }

    public LtwaRepository() { }

    /**
     * Abbreviates a given title using the ISO4 rules.
     *
     * @param title The title to be abbreviated
     * @return The abbreviated title
     */
    public String abbreviate(String title) {
        var lexer = new Lexer(title);
        var tokens = lexer.tokenize();

        List<Token> processed = new LinkedList<>();

        for (int i = 0; i < tokens.size(); i++) {
            var token = tokens.get(i);
            if (token.type() == Token.Type.ARTICLE) {
                continue;
            } else if (token.type() == Token.Type.STOPWORD && i != 0) {
                continue;
            } else if (token.type() == Token.Type.SYMBOLS) {
                var val = token.value().replace("...", "").replace(",", "").replace(".", ",");
                if (val.equals("&") || val.equals("+")) {
                    continue;
                }
                token = new Token(Token.Type.SYMBOLS, val, token.position());
            } else if (token.type() == Token.Type.ORDINAL && !processed.isEmpty()) {
                var lastToken = processed.getLast();
                if (lastToken.type() == Token.Type.PART) {
                    processed.removeLast();
                }
            }

            if (token.type() != Token.Type.EOS && !token.value().isEmpty()) {
                processed.add(token);
            }
        }

        if (processed.size() == 1) {
            return processed.getFirst().value();
        }

        if (processed.size() == 2) {
            var first = processed.get(0);
            var second = processed.get(1);
            if (first.type() == Token.Type.STOPWORD) {
                return first.value() + " " + second.value();
            }
            if (second.type() == Token.Type.SYMBOLS) {
                return first.value() + second.value();
            }
        }

        boolean space = true;
        StringBuilder result = new StringBuilder();
        String normalized = NormalizeUtils.normalize(title).toLowerCase();
        int position = 0;

        for (var token : processed) {
            String abbreviation = token.value();
            int length;

            if (token.type() == Token.Type.WORD || token.type() == Token.Type.PART) {
                if (token.position() < position) {
                    continue;
                }

                List<LtwaEntry> entries = new ArrayList<>();

                var remainingTitle = title.substring(token.position());
                var remainingNormalizedTitle = normalized.substring(token.position());
                entries.addAll(prefix.search(remainingNormalizedTitle));
                entries.addAll(suffix.search(reverse(remainingNormalizedTitle)));
                var optionalEntry = entries.stream()
                                           .filter(e -> matches(remainingNormalizedTitle, e))
                                           .max((a, b) -> {
                                               // Suffix first
                                               boolean isSuffixA = a.word().endsWith("-");
                                               boolean isSuffixB = b.word().endsWith("-");
                                               if (isSuffixA != isSuffixB) {
                                                   return isSuffixA ? 1 : -1;
                                               }

                                               // Longer first
                                               int lengthComparison = a.word().length() - b.word().length();
                                               if (lengthComparison != 0) {
                                                   return lengthComparison;
                                               }

                                               // Valid first
                                               return a.abbreviation() == null && b.abbreviation() == null ? 1
                                                       : a.abbreviation() == null ? -1 : 0;
                                           });

                if (optionalEntry.isEmpty()) {
                    length = token.value().length();
                } else {
                    var entry = optionalEntry.get();
                    abbreviation = entry.abbreviation() == null ? token.value() : restoreCapitalizationAndDiacritics(entry.abbreviation(), remainingTitle);
                    length = entry.word().length();
                }

                position = token.position() + length;
            } else if (token.type() == Token.Type.SYMBOLS || token.type() == Token.Type.HYPHEN) {
                space = false;
            }

            result.append(space && !result.isEmpty() ? " " : "").append(abbreviation);

            // If the last token is a hyphen, the next token should not have a space
            space = token.type() != Token.Type.HYPHEN;
        }

        return result.toString();
    }

    private static String restoreCapitalizationAndDiacritics(String abbreviation, String original) {
        int abbrCodePointCount = abbreviation.codePointCount(0, abbreviation.length());
        int origCodePointCount = original.codePointCount(0, original.length());

        if (abbrCodePointCount > origCodePointCount) {
            abbreviation = abbreviation.substring(0,
                    abbreviation.offsetByCodePoints(0, origCodePointCount));
        }

        String normalizedAbbreviation = NormalizeUtils.toNFKC(abbreviation);

        int[] normalizedAbbrCodePoints = normalizedAbbreviation.codePoints().toArray();
        int[] origCodePoints = original.codePoints().toArray();

        int[] resultCodePoints = Arrays.copyOf(normalizedAbbrCodePoints,
                Math.min(normalizedAbbrCodePoints.length, origCodePoints.length));

        for (int i = 0; i < resultCodePoints.length; i++) {
            String normalizedAbbrChar = new String(Character.toChars(normalizedAbbrCodePoints[i]));
            String origChar = new String(Character.toChars(origCodePoints[i]));
            String normalizedOrigChar = NormalizeUtils.toNFKC(origChar);

            if (!normalizedOrigChar.isEmpty() &&
                    normalizedAbbrChar.equalsIgnoreCase(normalizedOrigChar)) {
                resultCodePoints[i] = origCodePoints[i];
            }
        }

        return new String(resultCodePoints, 0, resultCodePoints.length);
    }

    private static boolean matches(String title, LtwaEntry entry) {
        var word = entry.word();
        int margin = (word.startsWith("-") ? 1 : 0) + (word.endsWith("-") ? 1 : 0);
        if (title.length() < word.length() - margin) {
            return false;
        }

        if (word.startsWith("-")) {
            word = reverse(word);
            title = reverse(title);
        }

        int wordPosition = 0;
        int titlePosition = 0;
        int wordCp;
        int titleCp;
        while (wordPosition < word.length() && titlePosition < title.length()) {
            wordCp = word.codePointAt(wordPosition);
            titleCp = title.codePointAt(titlePosition);
            if (wordCp == '-' && wordPosition == word.length() - 1) {
                return true;
            }
            if (Character.toLowerCase(wordCp) != Character.toLowerCase(titleCp)) {
                var match = INFLECTION.matcher(title.substring(titlePosition));
                if (match.lookingAt()) {
                    titlePosition += match.end();

                    match = BOUNDARY.matcher(title.substring(titlePosition));
                    if (match.lookingAt()) {
                        titlePosition += match.end();
                        wordPosition += match.end();
                        continue;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            wordPosition += Character.charCount(wordCp);
            titlePosition += Character.charCount(titleCp);
        }

        var match = INFLECTION.matcher(title.substring(titlePosition));
        if (match.lookingAt()) {
            titlePosition += match.end();
        }

        if (titlePosition >= title.length()) {
            return true;
        }

        return BOUNDARY.matcher(title.substring(titlePosition)).lookingAt();
    }

    private static String reverse(String str) {
        return new StringBuilder(str).reverse().toString();
    }
}
