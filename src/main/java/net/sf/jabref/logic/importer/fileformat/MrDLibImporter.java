/**
 *
 */
package net.sf.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
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

    private final String NAME = "MrDLibImporter";
    private final String DESCRIPTION = "Takes valid xml documents. Parses from MrDLib API a BibEntriy";
    private static final Log LOGGER = LogFactory.getLog(MrDLibImporter.class);


    public MrDLibImporter() {
    }

    /*
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
            };

            try {
                InputStream stream = new ByteArrayInputStream(recommendationsAsString.toString().getBytes());
                saxParser.parse(stream, handler);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return false;
            }
        } catch (ParserConfigurationException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        } catch (SAXException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    private String convertToString(BufferedReader input) throws IOException {
        String line;
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    /*
     * @see net.sf.jabref.logic.importer.Importer#importDatabase(java.io.BufferedReader)
     */
    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        // The Bibdatabase that gets returned in the ParserResult.
        BibDatabase bibDatabase = new BibDatabase();
        // The document to parse
        String recommendations = convertToString(input);
        // The list ob BibEntries with its associated rank
        ArrayList<RankedBibEntry> rankedBibEntries = new ArrayList<>();
        // The sorted BibEntries gets stored here later
        List<BibEntry> bibEntries = new ArrayList<>();
        //Parsing the response with a SAX parser
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {

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

                    if (qName.equalsIgnoreCase("related_article")) {
                        currentEntry = new BibEntry();
                    }
                    if (qName.equalsIgnoreCase("authors")) {
                        authors = true;
                    }
                    if (qName.equalsIgnoreCase("published_in")) {
                        published_in = true;
                    }
                    if (qName.equalsIgnoreCase("title")) {
                        title = true;
                    }
                    if (qName.equalsIgnoreCase("year")) {
                        year = true;
                    }
                    if (qName.equalsIgnoreCase("type")) {
                        type = true;
                    }
                    //-----------
                    if (qName.equalsIgnoreCase("related_article")) {
                        htmlSnippetSingle = null;
                        htmlSnippetSingleRank = -1;
                    }
                    if (qName.equalsIgnoreCase("snippet")
                            && attributes.getValue(0).equalsIgnoreCase("html_fully_formatted")) {
                        snippet = true;
                    }
                    if (qName.equalsIgnoreCase("suggested_rank")) {
                        rank = true;
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
                        currentEntry.setField("author", new String(ch, start, length));
                        authors = false;
                    }
                    if (published_in) {
                        currentEntry.setField("journal", new String(ch, start, length));
                        published_in = false;
                    }
                    if (title) {
                        currentEntry.setField("title", new String(ch, start, length));
                        title = false;
                    }
                    if (year) {
                        currentEntry.setField("year", new String(ch, start, length));
                        year = false;
                    }
                    // -------------
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

            try {
                InputStream stream = new ByteArrayInputStream(recommendations.getBytes());
                saxParser.parse(stream, handler);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            rankedBibEntries.sort(new Comparator<RankedBibEntry>() {

                @Override
                public int compare(RankedBibEntry o1, RankedBibEntry o2) {
                    return o1.rank.compareTo(o2.rank);
                }
            });


            bibEntries = rankedBibEntries.stream().map(e -> e.entry).collect(Collectors.toList());
        } catch (ParserConfigurationException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (SAXException e) {
            LOGGER.error(e.getMessage(), e);
        }
        for (BibEntry bibentry : bibEntries) {
            bibDatabase.insertEntry(bibentry);
        }

        ParserResult parserResult = new ParserResult(bibDatabase);
        return parserResult;
    }

    /*
     * @see net.sf.jabref.logic.importer.Importer#getName()
     */
    @Override
    public String getName() {
        return NAME;
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
        return DESCRIPTION;
    }


    private class RankedBibEntry {

        public BibEntry entry;
        public Integer rank;


        public RankedBibEntry(BibEntry entry, Integer rank) {
            this.rank = rank;
            this.entry = entry;
        }
    }

}
