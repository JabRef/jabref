package org.jabref.logic.journals.ltwa;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
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
            MVMap<String, List<LtwaEntry>> prefixMap = store.openMap("Prefixes");
            MVMap<String, List<LtwaEntry>> suffixMap = store.openMap("Suffixes");

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

    public LtwaRepository() {
    }

    /**
     * Abbreviates a given title using the ISO4 rules.
     *
     * @param title The title to be abbreviated
     * @return The abbreviated title
     */
    public Optional<String> abbreviate(String title) {
        return Optional.ofNullable(title)
                       .flatMap(NormalizeUtils::toNFKC)
                       .flatMap(normalizedTitle -> {
                           CharStream charStream = CharStreams.fromString(normalizedTitle);
                           LtwaLexer lexer = new LtwaLexer(charStream);
                           CommonTokenStream tokens = new CommonTokenStream(lexer);
                           LtwaParser parser = new LtwaParser(tokens);
                           LtwaParser.TitleContext titleContext = parser.title();
                           AbbreviationListener listener = new AbbreviationListener(normalizedTitle, prefix, suffix);
                           ParseTreeWalker walker = new ParseTreeWalker();
                           walker.walk(listener, titleContext);
                           return listener.getResult();
                       });
    }

    /**
     * Listener to apply abbreviation rules to the parsed title
     */
    private static class AbbreviationListener extends LtwaBaseListener {
        private final StringBuilder result = new StringBuilder();
        private final String originalTitle;
        private final PrefixTree<LtwaEntry> prefix;
        private final PrefixTree<LtwaEntry> suffix;
        private boolean isFirstElement = true;
        private boolean addSpace = false;
        private int lastPartPosition = -1;
        private int abbreviatedTitlePosition = 0;
        private boolean error = false;

        public AbbreviationListener(String originalTitle, PrefixTree<LtwaEntry> prefix, PrefixTree<LtwaEntry> suffix) {
            this.originalTitle = originalTitle;
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override
        public void exitArticleElement(LtwaParser.ArticleElementContext ctx) {
            // Skip articles
        }

        @Override
        public void exitStopwordElement(LtwaParser.StopwordElementContext ctx) {
            if (isFirstElement) {
                appendWithSpace(ctx.getText());
                isFirstElement = false;
            }
        }

        @Override
        public void exitSymbolsElement(LtwaParser.SymbolsElementContext ctx) {
            String val = ctx.getText().replace("...", "").replace(",", "").replace(".", ",");
            if ("&".equals(val) || "+".equals(val)) {
                return;
            }
            result.append(val);
        }

        @Override
        public void exitOrdinalElement(LtwaParser.OrdinalElementContext ctx) {
            addSpace = true;
            if (lastPartPosition != -1) {
                result.delete(lastPartPosition, result.length());
            }
            appendWithSpace(ctx.getText());
        }

        @Override
        public void exitHyphenElement(LtwaParser.HyphenElementContext ctx) {
            result.append(ctx.getText());
            addSpace = false;
        }

        @Override
        public void exitPartElement(LtwaParser.PartElementContext ctx) {
            lastPartPosition = result.length();
            addAbbreviation(ctx.getStart().getStartIndex(), ctx.getText());
        }

        @Override
        public void exitAbbreviationElement(LtwaParser.AbbreviationElementContext ctx) {
            appendWithSpace(ctx.getText());
        }

        @Override
        public void exitWordElement(LtwaParser.WordElementContext ctx) {
            addAbbreviation(ctx.getStart().getStartIndex(), ctx.getText());
        }

        private void addAbbreviation(int position, String initialText) {
            if (position < abbreviatedTitlePosition) {
                return;
            }

            String remainingTitle = originalTitle.substring(position);
            Optional<String> normalizedOpt = NormalizeUtils.normalize(originalTitle.substring(position))
                                                           .map(String::toLowerCase);

            if (normalizedOpt.isEmpty()) {
                error = true;
                appendWithSpace(initialText);
                return;
            }

            String normalizedRemaining = normalizedOpt.get();

            List<LtwaEntry> matchingEntries = Stream.concat(
                                                            prefix.search(normalizedRemaining).stream(),
                                                            suffix.search(reverse(normalizedRemaining)).stream())
                                                    .filter(e -> matches(normalizedRemaining, e))
                                                    .toList();

            if (matchingEntries.isEmpty()) {
                appendWithSpace(initialText);
                return;
            }

            Optional<LtwaEntry> bestEntryOpt = matchingEntries.stream()
                                                              .max(Comparator
                                                                      .<LtwaEntry>comparingInt(e -> e.word().endsWith("-") ? 1 : 0)
                                                                      .thenComparingInt(e -> e.word().length())
                                                                      .thenComparingInt(e -> e.abbreviation() != null ? 1 : 0)
                                                                      .thenComparingInt(e -> e.languages().contains("eng") ? 1 : 0));

            LtwaEntry entry = bestEntryOpt.get();
            if (entry.abbreviation() == null) {
                appendWithSpace(initialText);
                return;
            }

            abbreviatedTitlePosition += entry.word().length();
            Optional<String> matchedOpt = restoreCapitalizationAndDiacritics(entry.abbreviation(), remainingTitle);
            if (matchedOpt.isPresent()) {
                appendWithSpace(matchedOpt.get());
            } else {
                error = true;
            }
        }

        @Override
        public void exitSingleWordTitleFull(LtwaParser.SingleWordTitleFullContext ctx) {
            result.append(ctx.singleWordTitle().getText());
            addSpace = false;
        }

        @Override
        public void exitStopwordPlusTitleFull(LtwaParser.StopwordPlusTitleFullContext ctx) {
            String stopword = ctx.stopwordPlusAny().STOPWORD().getText();
            String second = ctx.stopwordPlusAny().getChild(1).getText();
            result.append(stopword).append(" ").append(second);
            addSpace = false;
        }

        @Override
        public void exitAnyPlusSymbolsFull(LtwaParser.AnyPlusSymbolsFullContext ctx) {
            String first = ctx.anyPlusSymbols().getChild(0).getText();
            String symbol = ctx.anyPlusSymbols().SYMBOLS().getText();
            result.append(first).append(symbol);
            addSpace = false;
        }

        @Override
        public void exitEveryRule(ParserRuleContext ctx) {
            isFirstElement = ctx.getParent() instanceof LtwaParser.TitleElementContext && isFirstElement;
            if (!(ctx instanceof LtwaParser.PartContext ||
                    ctx instanceof LtwaParser.PartElementContext ||
                    ctx instanceof LtwaParser.OrdinalContext)) {
                lastPartPosition = -1;
            }
            abbreviatedTitlePosition = Math.max(abbreviatedTitlePosition, ctx.getStart().getStartIndex());
        }

        private void appendWithSpace(String text) {
            if (addSpace && !result.isEmpty() && result.charAt(result.length() - 1) != ' ') {
                result.append(" ");
            }
            result.append(text);
            addSpace = true;
        }

        public Optional<String> getResult() {
            return error ? Optional.empty() : Optional.of(result.toString());
        }
    }

    private static Optional<String> restoreCapitalizationAndDiacritics(String abbreviation, String original) {
        int abbrCodePointCount = abbreviation.codePointCount(0, abbreviation.length());
        int origCodePointCount = original.codePointCount(0, original.length());

        if (abbrCodePointCount > origCodePointCount) {
            abbreviation = abbreviation.substring(0, abbreviation.offsetByCodePoints(0, origCodePointCount));
        }

        return NormalizeUtils.toNFKC(abbreviation)
                             .map(normalized -> {
                                 int[] normalizedAbbrCodePoints = normalized.codePoints().toArray();
                                 int[] origCodePoints = original.codePoints().toArray();
                                 int[] resultCodePoints = Arrays.copyOf(normalizedAbbrCodePoints,
                                         Math.min(normalizedAbbrCodePoints.length, origCodePoints.length));
                                 IntStream.range(0, resultCodePoints.length)
                                          .forEach(i -> {
                                              String normalizedAbbrChar = new String(Character.toChars(normalizedAbbrCodePoints[i]));
                                              String origChar = new String(Character.toChars(origCodePoints[i]));

                                              NormalizeUtils.toNFKC(origChar)
                                                            .filter(normalizedOrigChar -> !normalizedOrigChar.isEmpty() &&
                                                                    normalizedAbbrChar.equalsIgnoreCase(normalizedOrigChar))
                                                            .ifPresent(_ -> resultCodePoints[i] = origCodePoints[i]);
                                          });

                                 return new String(resultCodePoints, 0, resultCodePoints.length);
                             });
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
