package net.sf.jabref.logic.integrity;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.bibtex.FieldProperties;
import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IntegrityCheck {

    private final BibDatabaseContext bibDatabaseContext;

    public IntegrityCheck(BibDatabaseContext bibDatabaseContext) {
        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);
    }

    public List<IntegrityMessage> checkBibtexDatabase() {
        List<IntegrityMessage> result = new ArrayList<>();

        for (BibEntry entry : bibDatabaseContext.getDatabase().getEntries()) {
            result.addAll(checkBibtexEntry(entry));
        }

        return result;
    }

    private List<IntegrityMessage> checkBibtexEntry(BibEntry entry) {
        List<IntegrityMessage> result = new ArrayList<>();

        if (entry == null) {
            return result;
        }

        result.addAll(new AuthorNameChecker().check(entry));

        if (!bibDatabaseContext.isBiblatexMode()) {
            result.addAll(new TitleChecker().check(entry));
        }

        result.addAll(new BracketChecker("title").check(entry));
        result.addAll(new YearChecker().check(entry));
        result.addAll(new PagesChecker().check(entry));
        result.addAll(new UrlChecker().check(entry));
        result.addAll(new FileChecker(bibDatabaseContext).check(entry));
        result.addAll(new TypeChecker().check(entry));
        result.addAll(new AbbreviationChecker("journal").check(entry));
        result.addAll(new AbbreviationChecker("booktitle").check(entry));

        return result;
    }


    @FunctionalInterface
    public interface Checker {
        List<IntegrityMessage> check(BibEntry entry);
    }

    private static class TypeChecker implements Checker {

        @Override
        public List<IntegrityMessage> check(BibEntry entry) {
            Optional<String> value = entry.getFieldOptional("pages");
            if (!value.isPresent()) {
                return Collections.emptyList();
            }

            if ("proceedings".equalsIgnoreCase(entry.getType())) {
                return Collections.singletonList(new IntegrityMessage(Localization.lang("wrong entry type as proceedings has page numbers"), entry, "pages"));
            }

            return Collections.emptyList();
        }
    }

    private static class AbbreviationChecker implements Checker {

        private final String field;

        private AbbreviationChecker(String field) {
            this.field = field;
        }

        @Override
        public List<IntegrityMessage> check(BibEntry entry) {
            Optional<String> value = entry.getFieldOptional(field);
            if (!value.isPresent()) {
                return Collections.emptyList();
            }

            if (value.get().contains(".")) {
                return Collections.singletonList(new IntegrityMessage(Localization.lang("abbreviation detected"), entry, field));
            }

            return Collections.emptyList();
        }
    }

    private static class FileChecker implements Checker {

        private final BibDatabaseContext context;

        private FileChecker(BibDatabaseContext context) {
            this.context = context;
        }

        @Override
        public List<IntegrityMessage> check(BibEntry entry) {
            Optional<String> value = entry.getFieldOptional("file");
            if (!value.isPresent()) {
                return Collections.emptyList();
            }

            List<ParsedFileField> parsedFileFields = FileField.parse(value.get()).stream()
                    .filter(p -> !(p.getLink().startsWith("http://") || p.getLink().startsWith("https://")))
                    .collect(Collectors.toList());

            for (ParsedFileField p : parsedFileFields) {
                Optional<File> file = FileUtil.expandFilename(context, p.getLink());
                if ((!file.isPresent()) || !file.get().exists()) {
                    return Collections.singletonList(
                            new IntegrityMessage(Localization.lang("link should refer to a correct file path"), entry,
                                    "file"));
                }
            }

            return Collections.emptyList();
        }
    }

    private static class UrlChecker implements Checker {

        @Override
        public List<IntegrityMessage> check(BibEntry entry) {
            Optional<String> value = entry.getFieldOptional("url");
            if (!value.isPresent()) {
                return Collections.emptyList();
            }

            if (!value.get().contains("://")) {
                return Collections.singletonList(new IntegrityMessage(Localization.lang("should contain a protocol") + ": http[s]://, file://, ftp://, ...", entry, "url"));
            }

            return Collections.emptyList();
        }
    }

    private static class AuthorNameChecker implements Checker {

        @Override
        public List<IntegrityMessage> check(BibEntry entry) {
            List<IntegrityMessage> result = new ArrayList<>();
            for (String field : entry.getFieldNames()) {
                if (InternalBibtexFields.getFieldExtras(field).contains(FieldProperties.PERSON_NAMES)) {
                    Optional<String> value = entry.getFieldOptional(field);
                    if (!value.isPresent()) {
                        return Collections.emptyList();
                    }

                    String valueTrimmedAndLowerCase = value.get().trim().toLowerCase();
                    if (valueTrimmedAndLowerCase.startsWith("and ") || valueTrimmedAndLowerCase.startsWith(",")) {
                        result.add(new IntegrityMessage(Localization.lang("should start with a name"), entry, field));
                    } else if (valueTrimmedAndLowerCase.endsWith(" and") || valueTrimmedAndLowerCase.endsWith(",")) {
                        result.add(new IntegrityMessage(Localization.lang("should end with a name"), entry, field));
                    }
                }
            }
            return result;
        }
    }

    private static class BracketChecker implements Checker {

        private final String field;

        private BracketChecker(String field) {
            this.field = field;
        }

        @Override
        public List<IntegrityMessage> check(BibEntry entry) {
            Optional<String> value = entry.getFieldOptional(field);
            if (!value.isPresent()) {
                return Collections.emptyList();
            }

            // metaphor: integer-based stack (push + / pop -)
            int counter = 0;
            for (char a : value.get().trim().toCharArray()) {
                if (a == '{') {
                    counter++;
                } else if (a == '}') {
                    if (counter == 0) {
                        return Collections.singletonList(new IntegrityMessage(Localization.lang("unexpected closing curly braket"), entry, field));
                    } else {
                        counter--;
                    }
                }
            }

            if (counter > 0) {
                return Collections.singletonList(new IntegrityMessage(Localization.lang("unexpected opening curly braket"), entry, field));
            }

            return Collections.emptyList();
        }

    }

    private static class TitleChecker implements Checker {

        private static final Pattern INSIDE_CURLY_BRAKETS = Pattern.compile("\\{[^}\\{]*\\}");
        private static final Predicate<String> HAS_CAPITAL_LETTERS = Pattern.compile("[\\p{Lu}\\p{Lt}]").asPredicate();

        @Override
        public List<IntegrityMessage> check(BibEntry entry) {
            Optional<String> value = entry.getFieldOptional("title");
            if (!value.isPresent()) {
                return Collections.emptyList();
            }


            /*
             * Algorithm:
             * - remove trailing whitespaces
             * - ignore first letter as this can always be written in caps
             * - remove everything that is in brackets
             * - check if at least one capital letter is in the title
             */
            String valueTrimmed = value.get().trim();
            String valueIgnoringFirstLetter = valueTrimmed.startsWith("{") ? valueTrimmed : valueTrimmed.substring(1);
            String valueOnlySpacesWithinCurlyBraces = valueIgnoringFirstLetter;
            while (true) {
                Matcher matcher = INSIDE_CURLY_BRAKETS.matcher(valueOnlySpacesWithinCurlyBraces);
                if (!matcher.find()) {
                    break;
                }
                valueOnlySpacesWithinCurlyBraces = matcher.replaceAll("");
            }

            boolean hasCapitalLettersThatBibtexWillConvertToSmallerOnes = HAS_CAPITAL_LETTERS.test(valueOnlySpacesWithinCurlyBraces);

            if (hasCapitalLettersThatBibtexWillConvertToSmallerOnes) {
                return Collections.singletonList(new IntegrityMessage(Localization.lang("large capitals are not masked using curly brackets {}"), entry, "title"));
            }

            return Collections.emptyList();
        }
    }

    private static class YearChecker implements Checker {

        private static final Predicate<String> CONTAINS_FOUR_DIGIT = Pattern.compile("([^0-9]|^)[0-9]{4}([^0-9]|$)").asPredicate();

        /**
         * Checks, if the number String contains a four digit year
         */
        @Override
        public List<IntegrityMessage> check(BibEntry entry) {
            Optional<String> value = entry.getFieldOptional("year");
            if (!value.isPresent()) {
                return Collections.emptyList();
            }

            if (!CONTAINS_FOUR_DIGIT.test(value.get().trim())) {
                return Collections.singletonList(new IntegrityMessage(Localization.lang("should contain a four digit number"), entry, "year"));
            }

            return Collections.emptyList();
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
        public List<IntegrityMessage> check(BibEntry entry) {
            Optional<String> value = entry.getFieldOptional("pages");
            if (!value.isPresent()) {
                return Collections.emptyList();
            }

            if (!VALID_PAGE_NUMBER.test(value.get().trim())) {
                return Collections.singletonList(new IntegrityMessage(Localization.lang("should contain a valid page number range"), entry, "pages"));
            }

            return Collections.emptyList();
        }
    }

}
