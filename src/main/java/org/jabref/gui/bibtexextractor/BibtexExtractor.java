package org.jabref.gui.bibtexextractor;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.FieldName;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BibtexExtractor {

    private final static String INITIALS_GROUP = "INITIALS";
    private final static String LASTNAME_GROUP = "LASTNAME";

    private ArrayList<String> urls = new ArrayList<>();
    private ArrayList<String> authors = new ArrayList<>();
    private String year = new String();

    private static final Pattern urlPattern = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)" +
            "(([\\w\\-]+\\.)+?([\\w\\-.~]+\\/?)*" +
            "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern yearPattern = Pattern.compile(
            "\\d{4}");

    private static final Pattern authorPattern1 = Pattern.compile(
            "(?<" + LASTNAME_GROUP + ">\\p{Lu}\\w+),?\\s(?<" + INITIALS_GROUP + ">(\\p{Lu}\\.\\s){1,2})" +
                    "\\s*(and|,|\\.)*");

    private static final Pattern authorPattern2 = Pattern.compile(
            "(?<" + INITIALS_GROUP + ">(\\p{Lu}\\.\\s){1,2})(?<" + LASTNAME_GROUP + ">\\p{Lu}\\w+)" +
            "\\s*(and|,|\\.)*");

    public BibEntry extract(String input){
        String inputWithoutUrls = findUrls(input);
        String inputWithoutAuthors = findAuthors(inputWithoutUrls);
        String inputWithoutYear = findYear(inputWithoutAuthors);
        return GenerateEntity(inputWithoutYear);
    }

    private BibEntry GenerateEntity(String input){
        BibEntry extractedEntity = new BibEntry(BiblatexEntryTypes.ARTICLE);
        extractedEntity.setField(FieldName.AUTHOR, String.join(" and ", authors));
        extractedEntity.setField(FieldName.COMMENT, input);
        extractedEntity.setField(FieldName.URL, String.join(", ", urls));
        extractedEntity.setField(FieldName.YEAR, year);
        return  extractedEntity;
    }

    private String findUrls(String input){
        Matcher matcher = urlPattern.matcher(input);
        while (matcher.find()) {
            urls.add(input.substring(matcher.start(1), matcher.end()));
        }
        return fixSpaces(matcher.replaceAll("[url_tag]"));
    }

    private String findYear(String input){
        Matcher matcher = yearPattern.matcher(input);
        while (matcher.find()){
            String yearCandidate = input.substring(matcher.start(), matcher.end());
            Integer intYearCandidate = Integer.parseInt(yearCandidate);
            if (intYearCandidate > 1700 && intYearCandidate <= Calendar.getInstance().get(Calendar.YEAR)){
                year = yearCandidate;
                return fixSpaces(input.replace(year, "[year_tag]"));
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
        return fixSpaces(matcher.replaceAll("[author_tag]"));
    }

    private String GenerateAuthor(String lastName, String initials){
        return lastName + ", " + initials;
    }

    private String fixSpaces(String input){
        return input.replaceAll("[,.!?;:]", "$0 ")
                    .replaceAll("\\p{Lt}", " $0")
                    .replaceAll("\\s+", " ").trim();
    }
}
