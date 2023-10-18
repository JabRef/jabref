package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ISIDOREFetcher implements IdBasedParserFetcher {
    private String URL;

    public ISIDOREFetcher() {
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        identifier = identifier.trim();
        // this allows the user to input only the six-digit code at the end.
        if (identifier.length() == 6) {
            identifier = "https://isidore.science/document/10670/1." + identifier;
        }
        // allows the user to put in the eight digits including the "1."
        if (identifier.length() == 8) {
            identifier = "https://isidore.science/document/10670/" + identifier;
        }
        // Throw an error if this is not the starting link
        if (!identifier.startsWith("https://isidore.science/document/10670/1.") || !(identifier.length() == 47)) {
            throw new FetcherException("Could not construct url for ISIDORE");
        }
        this.URL = identifier;
        // change the link to be the correct link for the api.
        identifier = identifier.replace("/document/", "/resource/content?uri=");
        identifier = identifier.replace("https://isidore.science/", "https://api.isidore.science/");
        return new URL(identifier);
    }

    @Override
    public Parser getParser() {
        return xmlData -> {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(xmlData);

                // Assuming the root element represents an entry
                Element entryElement = document.getDocumentElement();

                if (entryElement == null) {
                    return Collections.emptyList();
                }

                return Collections.singletonList(xmlItemToBibEntry(document));
            } catch (Exception e) {
                // Handle parsing exceptions
                try {
                    throw new FetcherException("Issue with parsing link");
                } catch (FetcherException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    private BibEntry xmlItemToBibEntry(Document document) {
        Element itemElement = document.getDocumentElement();
        return new BibEntry(getType(itemElement.getElementsByTagName("types").item(0).getChildNodes()))
                .withField(StandardField.TITLE, itemElement.getElementsByTagName("title").item(0).getTextContent())
                .withField(StandardField.AUTHOR, getAuthor(itemElement.getElementsByTagName("enrichedCreators").item(0)))
                .withField(StandardField.YEAR, itemElement.getElementsByTagName("date").item(0).getChildNodes().item(1).getTextContent().substring(0, 4))
                .withField(StandardField.JOURNAL, getJournal(itemElement.getElementsByTagName("dc:source")))
                .withField(StandardField.PUBLISHER, getPublishers(itemElement.getElementsByTagName("publishers").item(0)))
                .withField(StandardField.ABSTRACT, getAbstract(itemElement.getElementsByTagName("abstract")))
                .withField(StandardField.DOI, getDOI(itemElement.getElementsByTagName("ore").item(0).getChildNodes()))
                .withField(StandardField.URL, this.URL);
    }

    private String getDOI(NodeList list) {
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getTextContent().contains("DOI:")) {
                return list.item(i).getTextContent().replace("DOI: ", "");
            }
            if (list.item(i).getTextContent().contains("doi:")) {
                return list.item(i).getTextContent().replace("info:doi:", "");
            }
        }
        return "";
    }

    private String getAbstract(NodeList list) {
        // If there is only one abstract, return the abstract
        if (list.getLength() == 1) {
            return list.item(0).getTextContent();
        }

        // If there are multiple abstracts, return the one with the english tag
        for (int i = 0; i < list.getLength(); i++) {
            Element abstractElement = (Element) list.item(i);
            String langAttribute = abstractElement.getAttribute("xml:lang");
            if (langAttribute.equals("en")) {
                return abstractElement.getTextContent();
            }
        }
        return "";
    }

    // Get the type of the document, ISIDORE only seems to have select types, also their types are different to
    // those used by Jabaref.
    private EntryType getType(NodeList list) {
        for (int i = 0; i < list.getLength(); i++) {
            String type = list.item(i).getTextContent();
            if (type.contains("article") || type.contains("Article")) {
                return StandardEntryType.Article;
            }
            if (type.contains("thesis") || type.contains("Thesis")) {
                return StandardEntryType.Thesis;
            }
            if (type.contains("book") || type.contains("Book")) {
                return StandardEntryType.Book;
            }
        }
        return StandardEntryType.Misc;
    }

    // Gets all the authors, separated with the word "and"
    // For some reason the author field sometimes has extra numbers and letters.
    private String getAuthor(Node itemElement) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i < itemElement.getChildNodes().getLength(); i += 2) {
            String next = removeMultipleSpaces(removeNumbers(itemElement.getChildNodes().item(i).getTextContent()));
            next = next.replace("\n", "");
            if (!next.isBlank()) {
                stringBuilder.append(next);
                stringBuilder.append(" and ");
            }
        }
        return removeMultipleSpaces(stringBuilder.substring(0, stringBuilder.length() - 5)).trim();
    }

    private String removeMultipleSpaces(String in) {
        int previous = 0;
        while (spaces(in) != previous) {
            previous = spaces(in);
            in = in.replaceAll("  ", " ");
        }
        return in;
    }

    // count the number of spaces in a string
    private int spaces(String s) {
        int x = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') {
                x++;
            }
        }
        return x;
    }

    // Remove numbers from a string and everything after the number, (helps with the author field).
    private String removeNumbers(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (Character.isDigit(string.charAt(i))) {
                return string.substring(0, i);
            }
        }
        return string;
    }

    private String getPublishers(Node itemElement) {
        if (itemElement == null) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < itemElement.getChildNodes().getLength(); i++) {
            stringBuilder.append(itemElement.getChildNodes().item(i).getTextContent().trim());
            if (!itemElement.getChildNodes().item(i).getTextContent().isBlank()) {
                stringBuilder.append(", ");
            }
        }
        return stringBuilder.substring(0, stringBuilder.length() - 2);
    }

    private String getJournal(NodeList list) {
        // If there is no journal, return an empty string.
        if (list.getLength() == 0) {
            return "";
        }
        String reference = list.item(list.getLength() - 1).getTextContent();
        for (int i = 0; i < reference.length(); i++) {
            if (reference.charAt(i) == ',') {
                return reference.substring(0, i);
            }
        }
        return "";
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        IdBasedParserFetcher.super.doPostCleanup(entry);
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        return IdBasedParserFetcher.super.performSearchById(identifier);
    }

    @Override
    public String getName() {
        return "ISIDORE";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return IdBasedParserFetcher.super.getHelpPage();
    }

    @Override
    public URLDownload getUrlDownload(URL url) {
        return IdBasedParserFetcher.super.getUrlDownload(url);
    }
}
