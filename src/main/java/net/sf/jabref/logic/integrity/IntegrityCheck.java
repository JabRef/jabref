/*
Copyright (C) 2004 R. Nagel

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/

// created by : r.nagel 27.10.2004
//
// function : check all bibtex items and report errors, inconsistencies,
//            warnings, hints and ....
//
//     todo : find equal authors: e.g.: D. Knuth = Donald Knuth = Donald E. Knuth
//            and try to give all items an identically look
//
// modified :

package net.sf.jabref.logic.integrity;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IntegrityCheck {

    public static final Checker AUTHOR_NAME_CHECKER = new AuthorNameChecker();
    public static final Checker YEAR_CHECKER = new YearChecker();
    public static final Checker TITLE_CHECKER = new TitleChecker();
    public static final Checker BRACKET_CHECKER = new BracketChecker();

    public List<IntegrityMessage> checkBibtexDatabase(BibtexDatabase base) {
        List<IntegrityMessage> result = new ArrayList<>();

        if (base == null) {
            return result;
        }

        for (BibtexEntry entry : base.getEntries()) {
            result.addAll(checkBibtexEntry(entry));
        }

        return result;
    }

    public List<IntegrityMessage> checkBibtexEntry(BibtexEntry entry) {
        List<IntegrityMessage> result = new ArrayList<>();

        if (entry == null) {
            return result;
        }

        Object data = entry.getField("author");
        if (data != null) {
            AUTHOR_NAME_CHECKER.check(data.toString(), "author", entry, result);
        }

        data = entry.getField("editor");
        if (data != null) {
            AUTHOR_NAME_CHECKER.check(data.toString(), "editor", entry, result);
        }

        data = entry.getField("title");
        if (data != null) {
            TITLE_CHECKER.check(data.toString(), "title", entry, result);
            BRACKET_CHECKER.check(data.toString(), "title", entry, result);
        }

        data = entry.getField("year");
        if (data != null) {
            YEAR_CHECKER.check(data.toString(), "year", entry, result);
        }

        return result;
    }

    public static interface Checker {

        void check(String value, String fieldName, BibtexEntry entry, List<IntegrityMessage> collector);
    }

    private static class AuthorNameChecker implements Checker {

        public void check(String value, String fieldName, BibtexEntry entry, List<IntegrityMessage> collector) {
            String valueTrimmedAndLowerCase = value.trim().toLowerCase();
            if(valueTrimmedAndLowerCase.startsWith("and ") || valueTrimmedAndLowerCase.startsWith(",")) {
                collector.add(new IntegrityMessage(Localization.lang("should start with a name"), entry, fieldName));
            } else if(valueTrimmedAndLowerCase.endsWith(" and")  || valueTrimmedAndLowerCase.endsWith(",")) {
                collector.add(new IntegrityMessage(Localization.lang("should end with a name"), entry, fieldName));
            }
        }

    }

    private static class BracketChecker implements Checker {

        public void check(String value, String fieldName, BibtexEntry entry, List<IntegrityMessage> collector) {
            // metaphor: integer-based stack (push + / pop -)
            int counter = 0;
            for (char a : value.trim().toCharArray()) {
                if (a == '{') {
                    counter++;
                } else if (a == '}') {
                    if (counter == 0) {
                        collector.add(new IntegrityMessage(Localization.lang("unexpected closing curly braket"), entry, fieldName));
                    } else {
                        counter--;
                    }
                }
            }

            if (counter > 0) {
                collector.add(new IntegrityMessage(Localization.lang("unexpected opening curly braket"), entry, fieldName));
            } else if (counter < 0) {
                collector.add(new IntegrityMessage(Localization.lang("unexpected closing curly braket"), entry, fieldName));
            }
        }

    }

    private static class TitleChecker implements Checker {

        private static final Pattern INSIDE_CURLY_BRAKETS = Pattern.compile("\\{[^}]*\\}");
        private static final Predicate<String> HAS_CAPITAL_LETTERS = Pattern.compile("[\\p{Lu}\\p{Lt}]").asPredicate();

        public void check(String value, String fieldName, BibtexEntry entry, List<IntegrityMessage> collector) {
            /*
             * Algorithm:
             * - remove trailing whitespaces
             * - ignore first letter as this can always be written in caps
             * - remove everything that is in brackets
             * - check if at least one capital letter is in the title
             */
            String valueTrimmed = value.trim();
            String valueIgnoringFirstLetter = valueTrimmed.startsWith("{") ? valueTrimmed : valueTrimmed.substring(1);
            String valueOnlySpacesWithinCurlyBraces = INSIDE_CURLY_BRAKETS.matcher(valueIgnoringFirstLetter).replaceAll("");
            boolean hasCapitalLettersThatBibtexWillConvertToSmallerOnes = HAS_CAPITAL_LETTERS.test(valueOnlySpacesWithinCurlyBraces);

            if (hasCapitalLettersThatBibtexWillConvertToSmallerOnes) {
                collector.add(new IntegrityMessage(Localization.lang("large capitals are not masked using curly brackets {}"), entry, fieldName));
            }
        }
    }

    private static class YearChecker implements Checker {

        private static final Predicate<String> CONTAINS_FOUR_DIGIT = Pattern.compile("([^0-9]|^)[0-9]{4}([^0-9]|$)").asPredicate();

        /**
         * Checks, if the number String contains a four digit year
         */
        public void check(String value, String fieldName, BibtexEntry entry, List<IntegrityMessage> collector) {
            if (!CONTAINS_FOUR_DIGIT.test(value.trim())) {
                collector.add(new IntegrityMessage(Localization.lang("should contain a four digit number"), entry, fieldName));
            }
        }
    }

}
