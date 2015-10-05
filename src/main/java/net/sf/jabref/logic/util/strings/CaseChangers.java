/*  Copyright (C) 2003-2015 JabRef contributors and Moritz Ringler, Simon Harrer, Oscar Gustafsson
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Class with static methods for changing the case of strings and arrays of strings.
 */
public class CaseChangers {

    private static final String SPACE_SEPARATOR = " ";


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
            String[] words = input.split("\\s+");
            String[] result = new String[words.length];

            for (int i = 0; i < words.length; i++) {
                if (words[i].startsWith("{")) {
                    result[i] = words[i];
                } else {
                    result[i] = words[i].toLowerCase();
                }
            }

            return StringUtil.join(result, CaseChangers.SPACE_SEPARATOR);
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
            String[] words = input.split("\\s+");
            String[] result = new String[words.length];

            for (int i = 0; i < words.length; i++) {
                if (words[i].startsWith("{")) {
                    result[i] = words[i];
                } else {
                    result[i] = words[i].toUpperCase();
                }
            }

            return StringUtil.join(result, CaseChangers.SPACE_SEPARATOR);
        }
    }

    public static class UpperFirstCaseChanger implements CaseChanger {

        private static final Pattern UF_PATTERN = Pattern.compile("\\b\\w");


        @Override
        public String getName() {
            return "Upper first";
        }

        /**
         * Converts the first character of the first word of the given string to a upper case (and the remaining characters of the first word to lower case), but does not change anything if word starts with "{"
         */
        @Override
        public String changeCase(String input) {
            String lowerCase = CaseChangers.LOWER.changeCase(input);

            Matcher matcher = UpperFirstCaseChanger.UF_PATTERN.matcher(lowerCase);

            if (matcher.find()) {
                return matcher.replaceFirst(matcher.group(0).toUpperCase());
            } else {
                return input;
            }
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
            String[] words = input.split("\\s+");
            String[] result = new String[words.length];

            for (int i = 0; i < words.length; i++) {
                if (words[i].startsWith("{")) {
                    result[i] = words[i];
                } else {
                    result[i] = StringUtil.capitalizeFirst(words[i].toLowerCase());
                }
            }

            return StringUtil.join(result, CaseChangers.SPACE_SEPARATOR);
        }
    }

    public static class TitleCaseChanger implements CaseChanger {

        private static final Set<String> notToCapitalize;


        static {
            Set<String> smallerWords = new HashSet<>();

            // Articles
            smallerWords.addAll(Arrays.asList("a", "an", "the"));
            // Prepositions
            smallerWords.addAll(Arrays.asList("above", "about", "across", "against", "along", "among", "around", "at", "before", "behind", "below", "beneath", "beside", "between", "beyond", "by", "down", "during", "except", "for", "from", "in", "inside", "into", "like", "near", "of", "off", "on", "onto", "since", "to", "toward", "through", "under", "until", "up", "upon", "with", "within", "without"));
            // Conjunctions
            smallerWords.addAll(Arrays.asList("and", "but", "for", "nor", "or", "so", "yet"));

            // unmodifiable for thread safety
            notToCapitalize = Collections.unmodifiableSet(smallerWords);
        }

        @Override
        public String getName() {
            return "Title";
        }

        /**
         * Converts all words to upper case, but converts articles, prepositions, and conjunctions to lower case
         * Does not change words starting with "{"
         */
        @Override
        public String changeCase(String input) {
            String[] words = input.split("\\s+");
            String[] result = new String[words.length];

            for (int i = 0; i < words.length; i++) {
                if (words[i].startsWith("{")) {
                    result[i] = words[i];
                } else {
                    String word = words[i].toLowerCase();
                    // first word is Always capitalized
                    boolean alwaysCapitalizeFirstWord = i == 0;
                    boolean alwaysCapitalizeLastWord = i == (words.length - 1);
                    if (alwaysCapitalizeFirstWord || alwaysCapitalizeLastWord) {
                        result[i] = StringUtil.capitalizeFirst(word);
                    } else if (TitleCaseChanger.notToCapitalize.contains(word)) {
                        result[i] = word;
                    } else {
                        result[i] = StringUtil.capitalizeFirst(word);
                    }
                }
            }

            return StringUtil.join(result, CaseChangers.SPACE_SEPARATOR);
        }

    }


    public static final LowerCaseChanger LOWER = new LowerCaseChanger();
    public static final UpperCaseChanger UPPER = new UpperCaseChanger();
    public static final UpperFirstCaseChanger UPPER_FIRST = new UpperFirstCaseChanger();
    public static final UpperEachFirstCaseChanger UPPER_EACH_FIRST = new UpperEachFirstCaseChanger();
    public static final TitleCaseChanger TITLE = new TitleCaseChanger();

    public static final List<CaseChanger> ALL = Arrays.asList(CaseChangers.LOWER, CaseChangers.UPPER, CaseChangers.UPPER_FIRST, CaseChangers.UPPER_EACH_FIRST, CaseChangers.TITLE);
}
