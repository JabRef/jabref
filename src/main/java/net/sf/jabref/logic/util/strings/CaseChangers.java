/*  Copyright (C) 2003-2015 JabRef contributors and Moritz Ringler, Simon Harrer
    Copyright (C) 2015 Ocar Gustafsson, Oliver Kopp

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.util.strings;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class with static methods for changing the case of strings and arrays of strings.
 * <p>
 * This class must detect the words in the title and whether letters are protected for case changes via enclosing them with '{' and '}' brakets.
 * Hence, for each letter to be changed, it needs to be known wether it is protected or not.
 * This can be done by starting at the letter position and moving forward and backword to see if there is a '{' and '}, respectively.
 */
public class CaseChangers {

    public static final Set<String> SMALLER_WORDS;

    static {
        Set<String> smallerWords = new HashSet<>();
        // NOTE: before JabRef 2.80, it was SKIP_WORDS = {"a", "an", "the", "for", "on", "of"}; in net.sf.jabref.logic.labelPattern.LabelPatternUtil.SKIP_WORDS 

        // Articles
        smallerWords.addAll(Arrays.asList("a", "an", "the"));
        // Prepositions
        smallerWords.addAll(Arrays.asList("above", "about", "across", "against", "along", "among", "around", "at", "before", "behind", "below", "beneath", "beside", "between", "beyond", "by", "down", "during", "except", "for", "from", "in", "inside", "into", "like", "near", "of", "off", "on", "onto", "since", "to", "toward", "through", "under", "until", "up", "upon", "with", "within", "without"));
        // Conjunctions
        smallerWords.addAll(Arrays.asList("and", "but", "for", "nor", "or", "so", "yet"));

        // unmodifiable for thread safety
        SMALLER_WORDS = Collections.unmodifiableSet(smallerWords);
    }

    /**
     * Represents a word in a title of a bibtex entry.
     * <p>
     * A word can have protected chars (enclosed in '{' '}') and may be a small (a, an, the, ...) word.
     */
    private static final class Word {

        private char[] chars;
        private boolean[] protectedChars;

        public Word(char[] chars, boolean[] protectedChars) {
            this.chars = Objects.requireNonNull(chars);
            this.protectedChars = Objects.requireNonNull(protectedChars);

            if (this.chars.length != this.protectedChars.length) {
                throw new IllegalArgumentException("the chars and the protectedChars array must be of same length");
            }
        }

        /**
         * Only change letters of the word that are unprotected to upper case.
         */
        public void toUpperCase() {
            for (int i = 0; i < chars.length; i++) {
                if (protectedChars[i]) {
                    continue;
                }

                chars[i] = Character.toUpperCase(chars[i]);
            }
        }

        /**
         * Only change letters of the word that are unprotected to lower case.
         */
        public void toLowerCase() {
            for (int i = 0; i < chars.length; i++) {
                if (protectedChars[i]) {
                    continue;
                }

                chars[i] = Character.toLowerCase(chars[i]);
            }
        }


        public void toUpperFirst() {
            for (int i = 0; i < chars.length; i++) {
                if (protectedChars[i]) {
                    continue;
                }

                if (i == 0) {
                    chars[i] = Character.toUpperCase(chars[i]);
                } else {
                    chars[i] = Character.toLowerCase(chars[i]);
                }
            }
        }

        public boolean isSmallerWord() {
            // "word:" is still a small "word"
            return SMALLER_WORDS.contains(this.toString().replaceAll("[:]", "").toLowerCase());
        }

        public boolean isLargerWord() {
            return !isSmallerWord();
        }

        @Override
        public String toString() {
            return new String(chars);
        }

        public boolean endsWithColon() {
            return this.toString().endsWith(":");
        }
    }

    /**
     * Parses a title to a list of words.
     */
    private static final class TitleParser {

        private StringBuffer buffer;
        private int wordStart;

        public List<Word> parse(String title) {
            List<Word> words = new LinkedList<>();

            boolean[] isProtected = determineProtectedChars(title);

            reset();

            int index = 0;
            for (char c : title.toCharArray()) {
                if (!Character.isWhitespace(c)) {
                    if (wordStart == -1) {
                        wordStart = index;
                    }

                    buffer.append(c);
                } else {
                    createWord(isProtected).ifPresent(words::add);
                }

                index++;
            }
            createWord(isProtected).ifPresent(words::add);

            return words;
        }

        private Optional<Word> createWord(boolean[] isProtected) {
            if (buffer.length() <= 0) {
                return Optional.empty();
            }

            char[] chars = buffer.toString().toCharArray();
            boolean[] protectedChars = new boolean[chars.length];

            System.arraycopy(isProtected, wordStart, protectedChars, 0, chars.length);

            reset();

            return Optional.of(new Word(chars, protectedChars));
        }

        private void reset() {
            wordStart = -1;
            buffer = new StringBuffer();
        }

        private static boolean[] determineProtectedChars(String title) {
            boolean[] isProtected = new boolean[title.length()];
            char[] chars = title.toCharArray();

            int brakets = 0;
            for (int i = 0; i < title.length(); i++) {
                if (chars[i] == '{') {
                    brakets++;
                } else if (chars[i] == '}') {
                    brakets--;
                } else {
                    isProtected[i] = brakets > 0;
                }
            }

            return isProtected;
        }

    }

    /**
     * Represents a title of a bibtex entry.
     */
    private static final class Title {

        private final List<Word> words = new LinkedList<>();

        public Title(String title) {
            this.words.addAll(new TitleParser().parse(title));
        }

        public List<Word> getWords() {
            return words;
        }

        public Optional<Word> getFirstWord() {
            if (getWords().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(getWords().get(0));
        }

        public Optional<Word> getLastWord() {
            if (getWords().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(getWords().get(getWords().size() - 1));
        }

        public String toString() {
            return words.stream().map(Word::toString).collect(Collectors.joining(" "));
        }

    }

    public interface CaseChanger {

        String getName();

        String changeCase(String input);
    }

    public static class LowerCaseChanger implements CaseChanger {

        @Override
        public String getName() {
            return "lower";
        }

        /**
         * Converts all characters of the string to lower case, but does not change words starting with "{"
         */
        @Override
        public String changeCase(String input) {
            Title title = new Title(input);

            title.getWords().stream().forEach(Word::toLowerCase);

            return title.toString();
        }
    }

    public static class UpperCaseChanger implements CaseChanger {

        @Override
        public String getName() {
            return "UPPER";
        }

        /**
         * Converts all characters of the given string to upper case, but does not change words starting with "{"
         */
        @Override
        public String changeCase(String input) {
            Title title = new Title(input);

            title.getWords().stream().forEach(Word::toUpperCase);

            return title.toString();
        }
    }

    public static class UpperFirstCaseChanger implements CaseChanger {

        @Override
        public String getName() {
            return "Upper first";
        }

        /**
         * Converts the first character of the first word of the given string to a upper case (and the remaining characters of the first word to lower case), but does not change anything if word starts with "{"
         */
        @Override
        public String changeCase(String input) {
            Title title = new Title(LOWER.changeCase(input));

            title.getWords().stream().findFirst().ifPresent(Word::toUpperFirst);

            return title.toString();
        }
    }

    public static class UpperEachFirstCaseChanger implements CaseChanger {

        @Override
        public String getName() {
            return "Upper Each First";
        }

        /**
         * Converts the first character of each word of the given string to a upper case (and all others to lower case), but does not change words starting with "{"
         */
        @Override
        public String changeCase(String input) {
            Title title = new Title(input);

            title.getWords().stream().forEach(Word::toUpperFirst);

            return title.toString();
        }
    }

    public static class TitleCaseChanger implements CaseChanger {

        @Override
        public String getName() {
            return "Title";
        }

        /**
         * Converts all words to upper case, but converts articles, prepositions, and conjunctions to lower case
         * Capitalizes first and last word
         * Does not change words starting with "{"
         */
        @Override
        public String changeCase(String input) {
            Title title = new Title(input);

            title.getWords().stream().filter(Word::isSmallerWord).forEach(Word::toLowerCase);
            title.getWords().stream().filter(Word::isLargerWord).forEach(Word::toUpperFirst);

            title.getFirstWord().ifPresent(Word::toUpperFirst);
            title.getLastWord().ifPresent(Word::toUpperFirst);

            for (int i = 0; i < title.getWords().size() - 2; i++) {
                if (title.getWords().get(i).endsWithColon()) {
                    title.getWords().get(i + 1).toUpperFirst();
                }
            }

            return title.toString();
        }
    }

    public static final LowerCaseChanger LOWER = new LowerCaseChanger();
    public static final UpperCaseChanger UPPER = new UpperCaseChanger();
    public static final UpperFirstCaseChanger UPPER_FIRST = new UpperFirstCaseChanger();
    public static final UpperEachFirstCaseChanger UPPER_EACH_FIRST = new UpperEachFirstCaseChanger();
    public static final TitleCaseChanger TITLE = new TitleCaseChanger();

    public static final List<CaseChanger> ALL = Arrays.asList(CaseChangers.LOWER, CaseChangers.UPPER, CaseChangers.UPPER_FIRST, CaseChangers.UPPER_EACH_FIRST, CaseChangers.TITLE);
}
