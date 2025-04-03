package org.jabref.logic.journals.ltwa;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Lexer {
    private static final String PERIOD = ".";
    private static final Set<String> PARTS = Set.of("series", "serie", "part", "section", "série", "supplemento",
            "chapter", "parte");
    private static final Set<String> PARTS_ABBREVIATIONS = Set.of("ser", "sect", "sec");
    private static final Pattern ARTICLE = Pattern.compile("""
            an?|the # English
            |der|die|das|des|dem|den # German
            |el|la|los|las|un|una|unos|unas # Spanish
            |le|la|les|l['’]|un|une|des|du|de la|d['’]|au|aux|dell['’]|nell['’] # French
            """, Pattern.COMMENTS);

    private static final Set<String> COMMON_ABBREVIATIONS = Set.of("St", "Mr", "Ms", "Mrs", "Mx", "Dr", "Prof",
            "vs");
    private static final Pattern IS_ORDINAL = Pattern.compile("[A-Z]|[IVXivx]+");
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "but", "or", "for", "nor", "so", "yet", "though",
            "when", "whenever", "where", "whereas", "wherever", "while", "about", "afore",
            "after", "ago", "along", "amid", "among", "amongst", "apropos", "as", "at",
            "atop", "by", "ca", "circa", "from", "hence", "in", "into",
            "like", "of", "off", "on", "onto", "ontop", "out", "over", "per", "since",
            "than", "til", "till", "to", "unlike", "until", "unto", "up", "upon", "upside",
            "versus", "via", "vis-a-vis", "vs", "with", "within", "für", "und", "aus",
            "zu", "zur", "im", "de", "et", "y", "del", "en", "di", "e", "da", "delle",
            "della", "sue", "el", "do", "og", "i", "voor", "van", "dell'", "dell", "ed",
            "för", "tot", "vir", "o", "its", "sul");

    private final String input;
    private int position; // Position in char units
    private int startWord;
    private String currentWord;
    private List<Token> tokens = new ArrayList<>();

    public Lexer(String input) {
        this.input = NormalizeUtils.toNFKC(input);
        this.position = 0;
        this.startWord = 0;
        this.currentWord = null;
        next();
    }

    private void skipSpace() {
        while (position < input.length() && isBoundary()) {
            position += Character.charCount(input.codePointAt(position));
        }
    }

    private void next() {
        skipSpace();
        int begin = position;

        while (position < input.length() && !isBoundary()) {
            position += Character.charCount(input.codePointAt(position));
        }

        if (begin != position) {
            if (position < input.length()) {
                int cp = input.codePointAt(position);
                if (isPunctuation(cp)) {
                    position += Character.charCount(cp);
                }
            }

            currentWord = input.substring(begin, position);
            startWord = begin;
        } else {
            currentWord = null;
        }
    }

    private boolean isBoundary() {
        if (position >= input.length()) {
            return true;
        }

        int cp = input.codePointAt(position);
        return Character.isSpaceChar(cp)
                || isPunctuation(cp) && (position + 2 > input.length() - 1
                        || !Character.isSpaceChar(input.codePointAt(position + 2)));
    }

    private boolean isPunctuation(int cp) {
        return cp == '-' || cp == '\'';
    }

    private boolean isPartOfWord() {
        if (position >= input.length()) {
            return false;
        }

        int cp = input.codePointAt(position);
        return Character.isLetterOrDigit(cp);
    }

    public List<Token> tokenize() {
        if (currentWord == null) {
            return tokens;
        }

        while (currentWord != null) {
            String word = currentWord;
            String lowerWord = currentWord.toLowerCase();
            StringBuilder endSymbols = new StringBuilder();

            while (!lowerWord.isEmpty()) {
                int lastCP = lowerWord.codePointBefore(lowerWord.length());
                if (Character.isLetterOrDigit(lastCP)) {
                    break;
                }
                int cpSize = Character.charCount(lastCP);
                endSymbols.insert(0, lowerWord.substring(lowerWord.length() - cpSize));
                lowerWord = lowerWord.substring(0, lowerWord.length() - cpSize);
                word = word.substring(0, word.length() - cpSize);
            }

            if (endSymbols.length() > 0 && endSymbols.codePointAt(0) == '\'') {
                word = word + "'";
                lowerWord = lowerWord + "'";
                endSymbols.deleteCharAt(0);
            }

            if (!word.isEmpty()) {
                boolean endsWithPeriod = endSymbols.length() > 0 && endSymbols.codePointAt(0) == '.';

                if (endsWithPeriod &&
                        (COMMON_ABBREVIATIONS.contains(word)
                                || word.contains(PERIOD)
                                || (word.codePointCount(0, word.length()) == 1 &&
                                        word.equals(word.toUpperCase())))) {
                    endSymbols.deleteCharAt(0);
                    tokens.add(new Token(Token.Type.ABBREVIATION, word + PERIOD, startWord));
                } else if (COMMON_ABBREVIATIONS.contains(word)) {
                    tokens.add(new Token(Token.Type.ABBREVIATION, word, startWord));
                } else if (endsWithPeriod && PARTS_ABBREVIATIONS.contains(lowerWord)) {
                    endSymbols.deleteCharAt(0);
                    tokens.add(new Token(Token.Type.PART, word + PERIOD, startWord));
                } else if (PARTS.contains(lowerWord)) {
                    tokens.add(new Token(Token.Type.PART, word, startWord));
                } else if (IS_ORDINAL.matcher(word).matches() && !isPartOfWord()) {
                    tokens.add(new Token(Token.Type.ORDINAL, word, startWord));
                } else if (ARTICLE.matcher(lowerWord).matches()) {
                    // dell'.* is a possible article in Italian, which means article
                    // don't have to be a space-separated word
                    tokens.add(new Token(Token.Type.ARTICLE, word, startWord));
                } else if (STOPWORDS.contains(lowerWord) && !isPartOfWord()) {
                    tokens.add(new Token(Token.Type.STOPWORD, word, startWord));
                } else {
                    tokens.add(new Token(Token.Type.WORD, word, startWord));
                }
            }

            if (endSymbols.length() > 0 && endSymbols.codePointAt(0) == '-') {
                tokens.add(new Token(Token.Type.HYPHEN, "-", position - endSymbols.toString().length()));
                endSymbols.deleteCharAt(0);
            }

            if (endSymbols.length() > 0) {
                tokens.add(new Token(Token.Type.SYMBOLS, endSymbols.toString(),
                        position - endSymbols.toString().length()));
            }

            next();
        }

        tokens.add(new Token(Token.Type.EOS, "\0", -1));
        return tokens;
    }
}
