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
 */
public class CaseChangers {

    /**
     * Represents a word in a title of a bibtex entry.
     *
     * A word can be mutable vs constant ({word}) and small (a, an, the, ...) vs large.
     */
    private static final class Word {

        private static final Set<String> SMALLER_WORDS;

        static {
            Set<String> smallerWords = new HashSet<>();

            // Articles
            smallerWords.addAll(Arrays.asList("a", "an", "the"));
            // Prepositions
            smallerWords.addAll(Arrays.asList("above", "about", "across", "against", "along", "among", "around", "at", "before", "behind", "below", "beneath", "beside", "between", "beyond", "by", "down", "during", "except", "for", "from", "in", "inside", "into", "like", "near", "of", "off", "on", "onto", "since", "to", "toward", "through", "under", "until", "up", "upon", "with", "within", "without"));
            // Conjunctions
            smallerWords.addAll(Arrays.asList("and", "but", "for", "nor", "or", "so", "yet"));

            // unmodifiable for thread safety
            SMALLER_WORDS = Collections.unmodifiableSet(smallerWords);
        }

        private String word;

        public Word(String word) {
            this.word = Objects.requireNonNull(word);
        }

        public boolean isConstant() {
            return word.startsWith("{") && word.endsWith("}");
        }

        public boolean isMutable() {
            return !isConstant();
        }

        public void toUpperCase() {
            this.word = this.word.toUpperCase();
        }

        public void toLowerCase() {
            this.word = this.word.toLowerCase();
        }

        public void toUpperFirst() {
            this.word = StringUtil.capitalizeFirst(this.word);
        }

        public boolean isSmallerWord() {
            return SMALLER_WORDS.contains(this.word.toLowerCase());
        }

        public boolean isLargerWord() {
            return !isSmallerWord();
        }

        @Override
        public String toString() {
            return word;
        }
    }

    /**
     * Represents a title of a bibtex entry.
     */
    private static final class Title {

        private final List<Word> words = new LinkedList<>();

        public Title(String title) {
            for (String word : Objects.requireNonNull(title).split("\\s+")) {
                words.add(new Word(word));
            }
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

            title.getWords().stream().filter(Word::isMutable).forEach(Word::toLowerCase);

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

            title.getWords().stream().filter(Word::isMutable).forEach(Word::toUpperCase);

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

            title.getWords().stream().findFirst().filter(Word::isMutable).ifPresent(Word::toUpperFirst);

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

            title.getWords().stream().filter(Word::isMutable).forEach(Word::toUpperFirst);

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

            title.getWords().stream().filter(Word::isMutable).filter(Word::isSmallerWord).forEach(Word::toLowerCase);
            title.getWords().stream().filter(Word::isMutable).filter(Word::isLargerWord).forEach(Word::toUpperFirst);

            title.getFirstWord().filter(Word::isMutable).ifPresent(Word::toUpperFirst);
            title.getLastWord().filter(Word::isMutable).ifPresent(Word::toUpperFirst);

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
