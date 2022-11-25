package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

/**
 * @implNote implemented by reverse-engineering <a href="https://github.com/SeerLabs/CiteSeerX/blob/4df28a98083be2829ec4c56ebbac09eb7772d379/src/java/edu/psu/citeseerx/domain/BiblioTransformer.java#L155-L249">the implementation by CiteSeerX</a>
 */
public class CoinsParser implements Parser {

    private final Pattern DOI = Pattern.compile("%3Fdoi%3D([^&]+)");
    private final Pattern TITLE = Pattern.compile("&amp;rft.atitle=([^&]+)");
    private final Pattern JOURNAL = Pattern.compile("&amp;rft.jtitle=([^&]+)");
    private final Pattern YEAR = Pattern.compile("&amp;rft.date=([^&]+)");
    private final Pattern VOLUME = Pattern.compile("&amp;rft.volume=([^&]+)");
    private final Pattern PAGES = Pattern.compile("&amp;rft.pages=([^&]+)");
    private final Pattern ISSUE = Pattern.compile("&amp;rft.issue=([^&]+)");
    private final Pattern TYPE = Pattern.compile("&amp;rft.genre=([^&]+)");
    private final Pattern AUTHOR = Pattern.compile("&amp;rft.au=([^&]+)");

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
        String data = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));
        BibEntry entry = new BibEntry();

        appendData(data, entry, DOI, StandardField.DOI);
        appendData(data, entry, TITLE, StandardField.TITLE);
        appendData(data, entry, JOURNAL, StandardField.JOURNALTITLE);
        appendData(data, entry, YEAR, StandardField.YEAR);
        appendData(data, entry, VOLUME, StandardField.VOLUME);
        appendData(data, entry, PAGES, StandardField.PAGES);
        appendData(data, entry, ISSUE, StandardField.ISSUE);

        Matcher matcherType = TYPE.matcher(data);
        if (matcherType.find()) {
            switch (matcherType.group(1)) {
                case "article":
                    entry.setType(StandardEntryType.Article);
                    break;
                case "unknown":
                default:
                    entry.setType(StandardEntryType.Misc);
                    break;
            }
        }

        List<String> authors = new ArrayList<>();
        Matcher matcherAuthors = AUTHOR.matcher(data);
        while (matcherAuthors.find()) {
            String author = matcherAuthors.group(1);
            authors.add(author);
        }
        entry.setField(StandardField.AUTHOR, authors.stream().collect(Collectors.joining(" and ")));

        return Collections.singletonList(entry);
    }

    private void appendData(String data, BibEntry entry, Pattern pattern, Field field) {
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            entry.setField(field, matcher.group(1));
        }
    }
}
