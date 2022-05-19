package org.jabref.logic.importer.fileformat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData;
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
        return "Citavi XML";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.CITAVI;
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
        Objects.requireNonNull(reader);
        return false;
    }

    @Override
    public boolean isRecognizedFormat(Path filePath) throws IOException {
        BufferedReader reader = getReaderFromZip(filePath);

        String str;
        int i = 0;
        while (((str = reader.readLine()) != null) && (i < 50)) {
            if (str.toLowerCase(Locale.ENGLISH).contains("<ProjectSettings>")) {
                return true;
            }

            i++;
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(Path filePath) throws IOException {
        BufferedReader reader = getReaderFromZip(filePath);

        Objects.requireNonNull(reader);

        try {
            Object unmarshalledObject = unmarshallRoot(reader);

            if (unmarshalledObject instanceof CitaviExchangeData) {
                // Check whether we have an article set, an article, a book article or a book article set
                CitaviExchangeData data = (CitaviExchangeData) unmarshalledObject;
                List<BibEntry> bibEntries = parseData(data);

                return new ParserResult(bibEntries);
            } else {
                return ParserResult.fromErrorMessage("File does not start with xml tag.");
            }
        } catch (JAXBException | XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }
    }

    private List<BibEntry> parseData(CitaviExchangeData data) {
        List<BibEntry> bibEntries = new ArrayList<>();

        // todo parse logic

        return bibEntries;
    }

    private void initUnmarshaller() throws JAXBException {
        if (unmarshaller == null) {
            // Lazy init because this is expensive
            JAXBContext context = JAXBContext.newInstance("org.jabref.logic.importer.fileformat.citavi");
            unmarshaller = context.createUnmarshaller();
        }
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

    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        throw new UnsupportedOperationException("CitaviXmlImporter does not support importDatabase(BufferedReader reader)."
                + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
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

    private BufferedReader getReaderFromZip(Path filePath) throws IOException {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(filePath.toFile()));
        ZipEntry zipEntry = zis.getNextEntry();

        Path newFile = Files.createTempFile("citavicontent", ".xml");

        while (zipEntry != null) {
            Files.copy(zis, newFile, StandardCopyOption.REPLACE_EXISTING);

            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();

        InputStream stream = Files.newInputStream(newFile, StandardOpenOption.READ);

        // check and delete the utf-8 BOM bytes
        InputStream newStream = checkForUtf8BOMAndDiscardIfAny(stream);

        return new BufferedReader(new InputStreamReader(newStream, StandardCharsets.UTF_8));
    }

    private static InputStream checkForUtf8BOMAndDiscardIfAny(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(new BufferedInputStream(inputStream), 3);
        byte[] bom = new byte[3];
        if (pushbackInputStream.read(bom) != -1) {
            if (!(bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF)) {
                pushbackInputStream.unread(bom);
            }
        }
        return pushbackInputStream;
    }
}
