/**
 *
 */
package net.sf.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.jabref.logic.importer.Importer;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 *
 */
public class MrDLibImporter extends Importer {

    private static final Log LOGGER = LogFactory.getLog(MrDLibImporter.class);

    /**
     *
     * @see net.sf.jabref.logic.importer.Importer#isRecognizedFormat(java.io.BufferedReader)
     */
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        String recommendationsAsString = convertToString(input);
        // check for valid format
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {
                // No Processing here. Just check for valid xml.
                // Later here will be the check against the XML schema.
            };

            try (InputStream stream = new ByteArrayInputStream(recommendationsAsString.toString().getBytes())) {
                saxParser.parse(stream, handler);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return false;
            }
        } catch (ParserConfigurationException | SAXException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    /*
     * @see net.sf.jabref.logic.importer.Importer#importDatabase(java.io.BufferedReader)
     */
    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        parse(input);
        return parserResult;
    }

    /*
     * @see net.sf.jabref.logic.importer.Importer#getName()
     */
    @Override
    public String getName() {
        return "MrDLibImporter";
    }

    /*
     * @see net.sf.jabref.logic.importer.Importer#getExtensions()
     */
    @Override
    public FileExtensions getExtensions() {
        return FileExtensions.XML;
    }

    /*
     * @see net.sf.jabref.logic.importer.Importer#getDescription()
     */
    @Override
    public String getDescription() {
        return "Takes valid xml documents. Parses from MrDLib API a BibEntriy";
    }

    /**
     * The SaxParser needs this String. So I convert it here.
     * @param Takes a BufferedReader with a reference to the XML document delivered by mdl server.
     * @return Returns an String containing the XML file.
     * @throws IOException
     */
    private String convertToString(BufferedReader input) throws IOException {
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            while ((line = input.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return stringBuilder.toString();
    }

    /**
     * Small pair-class to ensure the right order of the recommendations.
     */
    private class RankedBibEntry {

        public BibEntry entry;
        public Integer rank;

        public RankedBibEntry(BibEntry entry, Integer rank) {
            this.rank = rank;
            this.entry = entry;
        }
    }

    public ParserResult parserResult;



    private void parse(BufferedReader input) throws IOException {
        // The Bibdatabase that gets returned in the ParserResult.
        BibDatabase bibDatabase = new BibDatabase();
        // The document to parse
        String recommendations = convertToString(input);
        // The sorted BibEntries gets stored here later
        List<BibEntry> bibEntries = new ArrayList<>();
        //Parsing the response with a SAX parser
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            MrDlibImporterHandler handler = new MrDlibImporterHandler();
            try (InputStream stream = new ByteArrayInputStream(recommendations.getBytes())) {
                saxParser.parse(stream, handler);
            } catch (SAXException e) {
                LOGGER.error(e.getMessage(), e);
            }
            List<RankedBibEntry> rankedBibEntries = handler.getRankedBibEntries();
            rankedBibEntries.sort((RankedBibEntry rankedBibEntry1,
                    RankedBibEntry rankedBibEntry2) -> rankedBibEntry1.rank.compareTo(rankedBibEntry2.rank));
            bibEntries = rankedBibEntries.stream().map(e -> e.entry).collect(Collectors.toList());
        } catch (ParserConfigurationException | SAXException e) {
            LOGGER.error(e.getMessage(), e);
        }

        for (BibEntry bibentry : bibEntries) {
            bibDatabase.insertEntry(bibentry);
        }

        parserResult = new ParserResult(bibDatabase);
    }

    public ParserResult getParserResult() {
        return parserResult;
    }

    private class MrDlibImporterHandler extends DefaultHandler {

        // The list ob BibEntries with its associated rank
        private final List<RankedBibEntry> rankedBibEntries = new ArrayList<>();

        public List<RankedBibEntry> getRankedBibEntries() {
            return rankedBibEntries;
        }

        boolean authors;
        boolean published_in;
        boolean title;
        boolean year;
        boolean snippet;
        boolean rank;
        boolean type;
        String htmlSnippetSingle;
        int htmlSnippetSingleRank = -1;
        BibEntry currentEntry;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {

            switch (qName.toLowerCase()) {
            case "related_article":
                currentEntry = new BibEntry();
                htmlSnippetSingle = null;
                htmlSnippetSingleRank = -1;
                break;
            case "authors":
                authors = true;
                break;
            case "published_in":
                published_in = true;
                break;
            case "title":
                title = true;
                break;
            case "year":
                year = true;
                break;
            case "type":
                type = true;
                break;
            case "suggested_rank":
                rank = true;
                break;
            default:
                break;
            }
            if (qName.equalsIgnoreCase("snippet")
                    && attributes.getValue(0).equalsIgnoreCase("html_fully_formatted")) {
                snippet = true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equalsIgnoreCase("related_article")) {
                rankedBibEntries.add(new RankedBibEntry(currentEntry, htmlSnippetSingleRank));
                currentEntry = new BibEntry();
            }
        }

        @Override
        public void characters(char ch[], int start, int length) throws SAXException {

            if (authors) {
                currentEntry.setField(FieldName.AUTHOR, new String(ch, start, length));
                authors = false;
            }
            if (published_in) {
                currentEntry.setField(FieldName.JOURNAL, new String(ch, start, length));
                published_in = false;
            }
            if (title) {
                currentEntry.setField(FieldName.TITLE, new String(ch, start, length));
                title = false;
            }
            if (year) {
                currentEntry.setField(FieldName.YEAR, new String(ch, start, length));
                year = false;
            }
            if (rank) {
                htmlSnippetSingleRank = Integer.parseInt(new String(ch, start, length));
                rank = false;
            }
            if (snippet) {
                currentEntry.setField("html_representation", new String(ch, start, length));
                snippet = false;
            }

        }

    };

}
