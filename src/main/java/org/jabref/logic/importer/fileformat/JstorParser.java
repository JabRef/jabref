package org.jabref.logic.importer.fileformat;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 *
 **/
public class JstorParser extends BibtexParser{

    private static final Logger LOGGER = LoggerFactory.getLogger(JstorParser.class);
    private static final String HOST = "https://www.jstor.org/citation/text";

    public JstorParser(ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileMonitor) {
        super(importFormatPreferences, fileMonitor);
    }

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
        var reader = new BufferedReader(new InputStreamReader(inputStream));
        var lines = reader.lines().reduce("", String::concat);
        Document doc = Jsoup.parse(lines);
        var elements = doc.body().getElementsByClass("cite-this-item");
        List<BibEntry> result = new ArrayList<>();
        elements.forEach(element -> {
            var id = element.attr("href");
            id = id.replace("citation/info/", "");

            var url = HOST + id;
            LOGGER.info(url);
            try(InputStream stream = new URL(url).openStream()) {
                var parsed = super.parse(new InputStreamReader(stream));
                var entries = parsed.getDatabase().getEntries();
                LOGGER.info(entries.toString());
                result.addAll(entries);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return result;
    }
}
