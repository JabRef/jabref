package org.jabref.gui.bibtexextractor;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryType;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.FieldName;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BibtexExtractor {

    private final static String authorTag = "[author_tag]";
    private final static String urlTag = "[url_tag]";
    private final static String yearTag = "[year_tag]";
    private final static String pagesTag = "[pages_tag]";
    private final static String titleTag = "[title_tag]";
    private final static String journalTag = "[journal_tag]";

    private final static String INITIALS_GROUP = "INITIALS";
    private final static String LASTNAME_GROUP = "LASTNAME";

    private ArrayList<String> urls = new ArrayList<>();
    private ArrayList<String> authors = new ArrayList<>();
    private String year = new String();
    private String pages = new String();
    private String title = new String();
    private boolean isArticle = true;
    private String journalOrPublisher = new String();

    private static final Pattern urlPattern = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)" +
            "(([\\w\\-]+\\.)+?([\\w\\-.~]+\\/?)*" +
            "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern yearPattern = Pattern.compile(
            "\\d{4}",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern authorPattern1 = Pattern.compile(
            "(?<" + LASTNAME_GROUP + ">\\p{Lu}\\w+),?\\s(?<" + INITIALS_GROUP + ">(\\p{Lu}\\.\\s){1,2})" +
            "\\s*(and|,|\\.)*",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern authorPattern2 = Pattern.compile(
            "(?<" + INITIALS_GROUP + ">(\\p{Lu}\\.\\s){1,2})(?<" + LASTNAME_GROUP + ">\\p{Lu}\\w+)" +
            "\\s*(and|,|\\.)*",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern pagesPattern = Pattern.compile(
            "(p.)?\\s?\\d+(-\\d+)?",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);


    public BibEntry extract(String input){
        String inputWithoutUrls = findUrls(input);
        String inputWithoutAuthors = findAuthors(inputWithoutUrls);
        String inputWithoutYear = findYear(inputWithoutAuthors);
        String inputWithoutPages = findPages(inputWithoutYear);
        String nonparsed = findParts(inputWithoutPages);
        return GenerateEntity(nonparsed);
    }

    private BibEntry GenerateEntity(String input){
        BiblatexEntryType type = isArticle ? BiblatexEntryTypes.ARTICLE : BiblatexEntryTypes.BOOK;
        BibEntry extractedEntity = new BibEntry(type);
        extractedEntity.setField(FieldName.AUTHOR, String.join(" and ", authors));
        extractedEntity.setField(FieldName.URL, String.join(", ", urls));
        extractedEntity.setField(FieldName.YEAR, year);
        extractedEntity.setField(FieldName.PAGES, pages);
        extractedEntity.setField(FieldName.TITLE, title);
        if (isArticle){
            extractedEntity.setField(FieldName.JOURNAL, journalOrPublisher);
        }
        else {
            extractedEntity.setField(FieldName.PUBLISHER, journalOrPublisher);
        }
        extractedEntity.setField(FieldName.COMMENT, input);
        return  extractedEntity;
    }

    private String findUrls(String input){
        Matcher matcher = urlPattern.matcher(input);
        while (matcher.find()) {
            urls.add(input.substring(matcher.start(1), matcher.end()));
        }
        return fixSpaces(matcher.replaceAll(urlTag));
    }

    private String findYear(String input){
        Matcher matcher = yearPattern.matcher(input);
        while (matcher.find()){
            String yearCandidate = input.substring(matcher.start(), matcher.end());
            Integer intYearCandidate = Integer.parseInt(yearCandidate);
            if (intYearCandidate > 1700 && intYearCandidate <= Calendar.getInstance().get(Calendar.YEAR)){
                year = yearCandidate;
                return fixSpaces(input.replace(year, yearTag));
            }
        }
        return input;
    }

    private String findAuthors(String input){
        String currentInput = findAuthorsByPattern(input, authorPattern1);
        return findAuthorsByPattern(currentInput, authorPattern2);
    }

    private String findAuthorsByPattern(String input, Pattern pattern){
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            authors.add(GenerateAuthor(matcher.group(LASTNAME_GROUP), matcher.group(INITIALS_GROUP)));
        }
        return fixSpaces(matcher.replaceAll(authorTag));
    }

    private String GenerateAuthor(String lastName, String initials){
        return lastName + ", " + initials;
    }

    private String findPages(String input){
        Matcher matcher = pagesPattern.matcher(input);
        if (matcher.find()){
            pages = input.substring(matcher.start(1), matcher.end());
        }
        return fixSpaces(matcher.replaceFirst(pagesTag));
    }

    private String fixSpaces(String input){
        return input.replaceAll("[,.!?;:]", "$0 ")
                    .replaceAll("\\p{Lt}", " $0")
                    .replaceAll("\\s+", " ").trim();
    }

    private String findParts(String input)
    {
        ArrayList<String> lastParts = new ArrayList<>();
        String line = input;
        int afterAuthorsIndex = input.lastIndexOf(authorTag);
        if (afterAuthorsIndex == -1){
            return input;
        }
        else {
            afterAuthorsIndex += authorTag.length();
        }
        int delimiterIndex = input.lastIndexOf("//");
        if (delimiterIndex != -1){
            lastParts.add(input.substring(afterAuthorsIndex, delimiterIndex)
                         .replace(yearTag, "")
                         .replace(pagesTag, ""));
            lastParts.addAll(Arrays.asList(input.substring(delimiterIndex + 2).split(",|\\.")));
        }

        else {
            lastParts.addAll(Arrays.asList(input.substring(afterAuthorsIndex).split(",|\\.")));
        }
        int nonDigitParts = 0;
        for (String part: lastParts) {
            if (part.matches(".*\\d.*")){
                break;
            }
            nonDigitParts++;
        }
        if (nonDigitParts > 0){
            title = lastParts.get(0);
            line.replace(title, titleTag);
        }
        if (nonDigitParts > 1){
            journalOrPublisher = lastParts.get(1);
            line.replace(journalOrPublisher, journalTag);
        }
        if (nonDigitParts > 2){
            isArticle = false;
        }
        return fixSpaces(line);
    }
}
