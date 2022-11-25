package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.formatter.bibtexfields.HtmlToUnicodeFormatter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

public class CollectionOfComputerScienceBibliographiesParser implements Parser {

    final static Pattern REGEX_FOR_LINKS = Pattern.compile("<item>[\\s\\S]*?<link>([\\s\\S]*?)<\\/link>[\\s\\S]*?<\\/item>");
    final static Pattern REGEX_FOR_BIBTEX = Pattern.compile("<pre class=\"bibtex\">([\\s\\S]*?)<\\/pre>");

    final BibtexParser bibtexParser;
    final HtmlToUnicodeFormatter htmlToUnicodeFormatter;

    public CollectionOfComputerScienceBibliographiesParser(ImportFormatPreferences importFormatPreferences) {
        this.bibtexParser = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
        this.htmlToUnicodeFormatter = new HtmlToUnicodeFormatter();
    }

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
        try {
            List<String> links = matchRegexFromInputStreamHtml(inputStream, REGEX_FOR_LINKS);
            String bibtexDataString = parseBibtexStringsFromLinks(links)
                    .stream()
                    .collect(Collectors.joining());

            return bibtexParser.parseEntries(bibtexDataString);
        } catch (IOException e) {
            throw new ParseException(e);
        }
    }

    private List<String> matchRegexFromInputStreamHtml(InputStream inputStream, Pattern pattern) {
        try (Scanner scanner = new Scanner(inputStream)) {
            return scanner.findAll(pattern)
                          .map(match -> htmlToUnicodeFormatter.format(match.group(1)))
                          .collect(Collectors.toList());
        }
    }

    private List<String> parseBibtexStringsFromLinks(List<String> links) throws IOException {
        List<String> bibtexStringsFromAllLinks = new ArrayList<>();
        for (String link : links) {
            try (InputStream inputStream = new URLDownload(link).asInputStream()) {
                List<String> bibtexStringsFromLink = matchRegexFromInputStreamHtml(inputStream, REGEX_FOR_BIBTEX);
                bibtexStringsFromAllLinks.addAll(bibtexStringsFromLink);
            }
        }

        return bibtexStringsFromAllLinks;
    }
}

