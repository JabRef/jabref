package org.jabref.gui.bibtexextractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

public class BibtexExtractor {

    private static final String AUTHOR_TAG = "[author_tag]";
    private static final String URL_TAG = "[url_tag]";
    private static final String YEAR_TAG = "[year_tag]";
    private static final String PAGES_TAG = "[pages_tag]";

    private static final String INITIALS_GROUP = "INITIALS";
    private static final String LASTNAME_GROUP = "LASTNAME";

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)" +
                    "(([\\w\\-]+\\.)+?([\\w\\-.~]+\\/?)*" +
                    "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern YEAR_PATTERN = Pattern.compile(
            "\\d{4}",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern AUTHOR_PATTERN = Pattern.compile(
            "(?<" + LASTNAME_GROUP + ">\\p{Lu}\\w+),?\\s(?<" + INITIALS_GROUP + ">(\\p{Lu}\\.\\s){1,2})" +
                    "\\s*(and|,|\\.)*",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern AUTHOR_PATTERN_2 = Pattern.compile(
            "(?<" + INITIALS_GROUP + ">(\\p{Lu}\\.\\s){1,2})(?<" + LASTNAME_GROUP + ">\\p{Lu}\\w+)" +
                    "\\s*(and|,|\\.)*",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern PAGES_PATTERN = Pattern.compile(
            "(p.)?\\s?\\d+(-\\d+)?",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private final List<String> urls = new ArrayList<>();
    private final List<String> authors = new ArrayList<>();
    private String year = "";
    private String pages = "";
    private String title = "";
    private boolean isArticle = true;
    private String journalOrPublisher = "";

    public BibEntry extract(String input) {
        String inputWithoutUrls = findUrls(input);
        String inputWithoutAuthors = findAuthors(inputWithoutUrls);
        String inputWithoutYear = findYear(inputWithoutAuthors);
        String inputWithoutPages = findPages(inputWithoutYear);
        String nonParsed = findParts(inputWithoutPages);
        return generateEntity(nonParsed);
    }

    private BibEntry generateEntity(String input) {
        EntryType type = isArticle ? StandardEntryType.Article : StandardEntryType.Book;
        BibEntry extractedEntity = new BibEntry(type);
        extractedEntity.setField(StandardField.AUTHOR, String.join(" and ", authors));
        extractedEntity.setField(StandardField.URL, String.join(", ", urls));
        extractedEntity.setField(StandardField.YEAR, year);
        extractedEntity.setField(StandardField.PAGES, pages);
        extractedEntity.setField(StandardField.TITLE, title);
        if (isArticle) {
            extractedEntity.setField(StandardField.JOURNAL, journalOrPublisher);
        } else {
            extractedEntity.setField(StandardField.PUBLISHER, journalOrPublisher);
        }
        extractedEntity.setField(StandardField.COMMENT, input);
        return extractedEntity;
    }

    private String findUrls(String input) {
        Matcher matcher = URL_PATTERN.matcher(input);
        while (matcher.find()) {
            urls.add(input.substring(matcher.start(1), matcher.end()));
        }
        return fixSpaces(matcher.replaceAll(URL_TAG));
    }

    private String findYear(String input) {
        Matcher matcher = YEAR_PATTERN.matcher(input);
        while (matcher.find()) {
            String yearCandidate = input.substring(matcher.start(), matcher.end());
            int intYearCandidate = Integer.parseInt(yearCandidate);
            if ((intYearCandidate > 1700) && (intYearCandidate <= Calendar.getInstance().get(Calendar.YEAR))) {
                year = yearCandidate;
                return fixSpaces(input.replace(year, YEAR_TAG));
            }
        }
        return input;
    }

    private String findAuthors(String input) {
        String currentInput = findAuthorsByPattern(input, AUTHOR_PATTERN);
        return findAuthorsByPattern(currentInput, AUTHOR_PATTERN_2);
    }

    private String findAuthorsByPattern(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            authors.add(GenerateAuthor(matcher.group(LASTNAME_GROUP), matcher.group(INITIALS_GROUP)));
        }
        return fixSpaces(matcher.replaceAll(AUTHOR_TAG));
    }

    private String GenerateAuthor(String lastName, String initials) {
        return lastName + ", " + initials;
    }

    private String findPages(String input) {
        Matcher matcher = PAGES_PATTERN.matcher(input);
        if (matcher.find()) {
            pages = input.substring(matcher.start(), matcher.end());
        }
        return fixSpaces(matcher.replaceFirst(PAGES_TAG));
    }

    private String fixSpaces(String input) {
        return input.replaceAll("[,.!?;:]", "$0 ")
                    .replaceAll("\\p{Lt}", " $0")
                    .replaceAll("\\s+", " ").trim();
    }

    private String findParts(String input) {
        ArrayList<String> lastParts = new ArrayList<>();
        int afterAuthorsIndex = input.lastIndexOf(AUTHOR_TAG);
        if (afterAuthorsIndex == -1) {
            return input;
        } else {
            afterAuthorsIndex += AUTHOR_TAG.length();
        }
        int delimiterIndex = input.lastIndexOf("//");
        if (delimiterIndex != -1) {
            lastParts.add(input.substring(afterAuthorsIndex, delimiterIndex)
                               .replace(YEAR_TAG, "")
                               .replace(PAGES_TAG, ""));
            lastParts.addAll(Arrays.asList(input.substring(delimiterIndex + 2).split(",|\\.")));
        } else {
            lastParts.addAll(Arrays.asList(input.substring(afterAuthorsIndex).split(",|\\.")));
        }
        int nonDigitParts = 0;
        for (String part : lastParts) {
            if (part.matches(".*\\d.*")) {
                break;
            }
            nonDigitParts++;
        }
        if (nonDigitParts > 0) {
            title = lastParts.get(0);
        }
        if (nonDigitParts > 1) {
            journalOrPublisher = lastParts.get(1);
        }
        if (nonDigitParts > 2) {
            isArticle = false;
        }
        return fixSpaces(input);
    }
}
