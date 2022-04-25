package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.endnote.Record;
import org.jabref.logic.importer.fileformat.endnote.Xml;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importer for the citavi XML format.
 */

public class CitaviXmlImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitaviXmlImporter.class);
    private final ImportFormatPreferences preferences;
    private Unmarshaller unmarshaller;

    public CitaviXmlImporter(ImportFormatPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getName() {
        return "Citavi";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.CIVTAVI;
    }

    @Override
    public String getId() {
        return "citavi";
    }

    @Override
    public String getDescription() {
        return "Importer for the Citavi XML format.";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        String str;
        int i = 0;
        while (((str = reader.readLine()) != null) && (i < 50)) {
            if (str.toLowerCase(Locale.ENGLISH).contains("<records>")) {
                return true;
            }

            i++;
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);

        try {
            Object unmarshalledObject = unmarshallRoot(reader);

            if (unmarshalledObject instanceof Xml) {
                // Check whether we have an article set, an article, a book article or a book article set
                Xml root = (Xml) unmarshalledObject;
                List<BibEntry> bibEntries = root
                        .getRecords().getRecord()
                        .stream()
                        .map(this::parseRecord)
                        .collect(Collectors.toList());

                return new ParserResult(bibEntries);
            } else {
                return ParserResult.fromErrorMessage("File does not start with xml tag.");
            }
        } catch (JAXBException | XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }
    }

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) {
        try {
            return importDatabase(
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))).getDatabase().getEntries();
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return Collections.emptyList();
    }

    private BibEntry parseRecord(Record record) {
        BibEntry entry = new BibEntry();
        // todo add parse logic
        return entry;
    }

    private Object unmarshallRoot(BufferedReader reader) throws XMLStreamException, JAXBException {
        initUnmarshaller();

        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);

        // Go to the root element
        while (!xmlStreamReader.isStartElement()) {
            xmlStreamReader.next();
        }

        return unmarshaller.unmarshal(xmlStreamReader);
    }

    private void initUnmarshaller() throws JAXBException {
        if (unmarshaller == null) {
            // Lazy init because this is expensive
            JAXBContext context = JAXBContext.newInstance("org.jabref.logic.importer.fileformat.citavi");
            unmarshaller = context.createUnmarshaller();
        }
    }
}
