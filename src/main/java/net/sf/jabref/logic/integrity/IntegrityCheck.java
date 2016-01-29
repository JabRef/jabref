package net.sf.jabref.logic.integrity;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntegrityCheck {

    public static final Checker AUTHOR_NAME_CHECKER = new AuthorNameChecker();
    public static final Checker YEAR_CHECKER = new YearChecker();
    public static final Checker TITLE_CHECKER = new TitleChecker();
    public static final Checker BRACKET_CHECKER = new BracketChecker();
    public static final Checker PAGES_CHECKER = new PagesChecker();
    public static final Checker URL_CHECKER = new UrlChecker();
    private boolean isBiblatexMode;

    public List<IntegrityMessage> checkBibtexDatabase(BibDatabase database, boolean isBiblatexMode) {
        List<IntegrityMessage> result = new ArrayList<>();

        if (database == null) {
            return result;
        }

        this.isBiblatexMode = isBiblatexMode;

        for (BibEntry entry : database.getEntries()) {
            result.addAll(checkBibtexEntry(entry));
        }

        return result;
    }

    public List<IntegrityMessage> checkBibtexEntry(BibEntry entry) {
        List<IntegrityMessage> result = new ArrayList<>();

        if (entry == null) {
            return result;
        }

        entry.getFieldOptional("author").ifPresent(data -> AUTHOR_NAME_CHECKER.check(data, "author", entry, result));

        entry.getFieldOptional("editor").ifPresent(data -> AUTHOR_NAME_CHECKER.check(data, "editor", entry, result));

        entry.getFieldOptional("title").ifPresent(data -> {
            if(!isBiblatexMode) {
                TITLE_CHECKER.check(data, "title", entry, result);
            }
            BRACKET_CHECKER.check(data, "title", entry, result);
        });

        entry.getFieldOptional("year").ifPresent(data -> YEAR_CHECKER.check(data, "year", entry, result));

        entry.getFieldOptional("pages").ifPresent(data -> PAGES_CHECKER.check(data.toString(), "pages", entry, result));

        entry.getFieldOptional("url").ifPresent(data -> URL_CHECKER.check(data, "url", entry, result));

        return result;
    }

    public interface Checker {

        void check(String value, String fieldName, BibEntry entry, List<IntegrityMessage> collector);
    }

    private static class UrlChecker implements Checker {

        @Override
        public void check(String value, String fieldName, BibEntry entry, List<IntegrityMessage> collector) {
            if(!value.contains("://")) {
                collector.add(new IntegrityMessage(Localization.lang("should contain a protocol") + ": http[s]://, file://, ftp://, ...", entry, fieldName));
            }
        }
    }

    private static class AuthorNameChecker implements Checker {

        @Override
        public void check(String value, String fieldName, BibEntry entry, List<IntegrityMessage> collector) {
            String valueTrimmedAndLowerCase = value.trim().toLowerCase();
            if(valueTrimmedAndLowerCase.startsWith("and ") || valueTrimmedAndLowerCase.startsWith(",")) {
                collector.add(new IntegrityMessage(Localization.lang("should start with a name"), entry, fieldName));
            } else if(valueTrimmedAndLowerCase.endsWith(" and")  || valueTrimmedAndLowerCase.endsWith(",")) {
                collector.add(new IntegrityMessage(Localization.lang("should end with a name"), entry, fieldName));
            }
        }

    }

    private static class BracketChecker implements Checker {

        @Override
        public void check(String value, String fieldName, BibEntry entry, List<IntegrityMessage> collector) {
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
            }
        }

    }

    private static class TitleChecker implements Checker {

        private static final Pattern INSIDE_CURLY_BRAKETS = Pattern.compile("\\{[^}\\{]*\\}");
        private static final Predicate<String> HAS_CAPITAL_LETTERS = Pattern.compile("[\\p{Lu}\\p{Lt}]").asPredicate();

        @Override
        public void check(String value, String fieldName, BibEntry entry, List<IntegrityMessage> collector) {
            /*
             * Algorithm:
             * - remove trailing whitespaces
             * - ignore first letter as this can always be written in caps
             * - remove everything that is in brackets
             * - check if at least one capital letter is in the title
             */
            String valueTrimmed = value.trim();
            String valueIgnoringFirstLetter = valueTrimmed.startsWith("{") ? valueTrimmed : valueTrimmed.substring(1);
            String valueOnlySpacesWithinCurlyBraces = valueIgnoringFirstLetter;
            while(true) {
                Matcher matcher = INSIDE_CURLY_BRAKETS.matcher(valueOnlySpacesWithinCurlyBraces);
                if(!matcher.find()) {
                    break;
                }
                valueOnlySpacesWithinCurlyBraces = matcher.replaceAll("");
            }

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
        @Override
        public void check(String value, String fieldName, BibEntry entry, List<IntegrityMessage> collector) {
            if (!CONTAINS_FOUR_DIGIT.test(value.trim())) {
                collector.add(new IntegrityMessage(Localization.lang("should contain a four digit number"), entry, fieldName));
            }
        }
    }

    /**
     * From BibTex manual:
     * One or more page numbers or range of numbers, such as 42--111 or 7,41,73--97 or 43+
     * (the '+' in this last example indicates pages following that don't form a simple range).
     * To make it easier to maintain Scribe-compatible databases, the standard styles convert
     * a single dash (as in 7-33) to the double dash used in TEX to denote number ranges (as in 7--33).
     */
    private static class PagesChecker implements Checker {
        private static final String PAGES_EXP = ""
                + "\\A"                       // begin String
                + "\\d+"                      // number
                + "(?:"                       // non-capture group
                + "\\+|\\-{2}\\d+"            // + or --number (range)
                + ")?"                        // optional group
                + "(?:"                       // non-capture group
                + ","                         // comma
                + "\\d+(?:\\+|\\-{2}\\d+)?"   // repeat former pattern
                + ")*"                        // repeat group 0,*
                + "\\z";                      // end String

        private static final Predicate<String> VALID_PAGE_NUMBER = Pattern.compile(PAGES_EXP).asPredicate();

        /**
         * Checks, if the page numbers String conforms to the BibTex manual
         */
        @Override
        public void check(String value, String fieldName, BibEntry entry, List<IntegrityMessage> collector) {
            if (!VALID_PAGE_NUMBER.test(value.trim())) {
                collector.add(new IntegrityMessage(Localization.lang("should contain a valid page number range"), entry, fieldName));
            }
        }
    }

}
