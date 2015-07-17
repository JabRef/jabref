/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.util;

/* Mp3dings - manage mp3 meta-information
 * Copyright (C) 2003 Moritz Ringler
 * $Id$
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Class with static methods for changing the case of strings and arrays of
 * strings.
 *
 * @author Moritz Ringler, Simon Harrer
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

        @Override
        public String changeCase(String input) {
            return input.toLowerCase();
        }
    }

    public static class UpperCaseChanger implements CaseChanger {

        @Override
        public String getName() {
            return "UPPER";
        }

        @Override
        public String changeCase(String input) {
            return input.toUpperCase();
        }
    }

    public static class UpperFirstCaseChanger implements CaseChanger {

        private final static Pattern UF_PATTERN = Pattern.compile("\\b\\w");


        @Override
        public String getName() {
            return "Upper first";
        }

        @Override
        public String changeCase(String input) {
            String s = input.toLowerCase();

            Matcher matcher = UpperFirstCaseChanger.UF_PATTERN.matcher(s);

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

        @Override
        public String changeCase(String input) {
            String s = input.toLowerCase();
            String[] words = s.split("\\s+");
            String[] result = new String[words.length];

            for (int i = 0; i < words.length; i++) {
                result[i] = StringUtil.nCase(words[i]);
            }

            return StringUtil.join(result, CaseChangers.SPACE_SEPARATOR);
        }
    }

    public static class TitleCaseChanger implements CaseChanger {

        private final static Set<String> notToCapitalize;

        static {
            Set<String> smallerWords = new HashSet<String>();

            smallerWords.addAll(Arrays.asList("a", "an", "the", "and", "but", "or", "for", "nor", "as", "at", "by", "for",
                    "from", "in", "into", "near", "of", "on", "onto", "to", "with"));

            // unmodifiable for thread safety
            notToCapitalize = Collections.unmodifiableSet(smallerWords);
        }


        @Override
        public String getName() {
            return "Title";
        }

        @Override
        public String changeCase(String input) {
            String s = input.toLowerCase();
            String[] words = s.split("\\s+");
            String[] result = new String[words.length];

            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                // first word is Always capitalized
                boolean alwaysCapitalizeFirstWord = i == 0;
                boolean alwaysCapitalizeLastWord = i == (words.length - 1);
                if (alwaysCapitalizeFirstWord || alwaysCapitalizeLastWord) {
                    result[i] = StringUtil.nCase(word);
                } else if (TitleCaseChanger.notToCapitalize.contains(word)) {
                    result[i] = word;
                } else {
                    result[i] = StringUtil.nCase(word);
                }
            }

            return StringUtil.join(result, CaseChangers.SPACE_SEPARATOR);
        }

    }


    public final static LowerCaseChanger LOWER = new LowerCaseChanger();
    public final static UpperCaseChanger UPPER = new UpperCaseChanger();
    public final static UpperFirstCaseChanger UPPER_FIRST = new UpperFirstCaseChanger();
    public final static UpperEachFirstCaseChanger UPPER_EACH_FIRST = new UpperEachFirstCaseChanger();
    public final static TitleCaseChanger TITLE = new TitleCaseChanger();

    public final static List<CaseChanger> ALL = Arrays.asList(CaseChangers.LOWER, CaseChangers.UPPER, CaseChangers.UPPER_FIRST, CaseChangers.UPPER_EACH_FIRST, CaseChangers.TITLE);
}
