package org.jabref.logic.importer.fileformat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitaviXmlImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitaviXmlImporter.class);
    private static final byte UUID_LENGTH = 36;
    private static final byte UUID_SEMICOLON_OFFSET_INDEX = 37;
    private static final EnumSet<QuotationTypeMapping> QUOTATION_TYPES = EnumSet.allOf(QuotationTypeMapping.class);
    private final HtmlToLatexFormatter htmlToLatexFormatter = new HtmlToLatexFormatter();
    private final NormalizePagesFormatter pagesFormatter = new NormalizePagesFormatter();

    private final XMLInputFactory xmlInputFactory;
    private List<String> refIdWithAuthors = new ArrayList<>();
    private List<String> refIdWithEditors = new ArrayList<>();
    private List<String> refIdWithKeywords = new ArrayList<>();
    private List<String> refIdWithPublishers = new ArrayList<>();

    private XMLStreamReader mapper = null;

    public CitaviXmlImporter() {
        xmlInputFactory = XMLInputFactory.newFactory();
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, true);
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
        return Localization.lang("Importer for the Citavi XML format.");
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        return false;
    }

    @Override
    public boolean isRecognizedFormat(Path filePath) throws IOException {
        try (BufferedReader reader = getReaderFromZip(filePath)) {
            String str;
            int i = 0;
            while ((str = reader.readLine()) != null) {
                if (str.toLowerCase(Locale.ROOT).contains("citaviexchangedata")) {
                    return true;
                }
                i++;
            }
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(Path filePath) throws IOException {
        List<BibEntry> bibEntries = new ArrayList<>();

        try (BufferedReader reader = getReaderFromZip(filePath)) {
            mapper = mapperRoot(reader);

            while (mapper.hasNext()) {
                mapper.next();
                if (mapper.getEventType() == XMLStreamReader.START_ELEMENT) {
                    bibEntries.add(parseElements(mapper));
                }
            }

            return new ParserResult(bibEntries);
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }
    }

    private BibEntry parseElements(XMLStreamReader reader) {
        BibEntry entry = new BibEntry();

        if (reader.getEventType() == XMLStreamReader.START_ELEMENT) {
            parseData(reader);
        }
        if (reader.getEventType() == XMLStreamReader.CHARACTERS) {
            String elementName = reader.getName().getLocalPart();
            entry.setType(getType(reader));
            if ("Title".equals(elementName)) {
                entry.setField(StandardField.TITLE, reader.getText());
            }
            if ("Name".equals(elementName)) {
                entry.setField(StandardField.TITLE, reader.getText());
            }
            if ("LastName".equals(elementName)) {
                entry.setField(StandardField.AUTHOR, reader.getText());
            }
            if ("FirstName".equals(elementName)) {
                entry.setField(StandardField.AUTHOR, reader.getText());
            }
            if ("MiddleName".equals(elementName)) {
                entry.setField(StandardField.AUTHOR, reader.getText());
            }
            if ("Year".equals(elementName)) {
                entry.setField(StandardField.YEAR, reader.getText());
            }
            if ("Volume".equals(elementName)) {
                entry.setField(StandardField.VOLUME, reader.getLocalName());
            }
            if ("ISBN".equals(elementName)) {
                entry.setField(StandardField.ISBN, reader.getLocalName());
            }
            if ("Abstract".equals(elementName)) {
                entry.setField(StandardField.ABSTRACT, reader.getText());
            }
            if ("DOI".equals(elementName)) {
                entry.setField(StandardField.DOI, reader.getText());
            }
            if ("KnowledgeItem".equals(elementName)) {
                entry.setField(StandardField.COMMENT, reader.getText());
            }
            if ("SeriesTitle".equals(elementName)) {
                entry.setField(new UnknownField("references"), reader.getText());
            }
            if ("Reference".equals(elementName)) {
                entry.setField(new UnknownField("references"), reader.getText());
            }
            if ("OneToN".equals(elementName)) {
                entry.setField(new UnknownField("references"), reader.getText());
            }
            if ("PageRangeNumber".equals(elementName)) {
                entry.setField(StandardField.PAGES, reader.getText());
            }
        }

        return entry;
    }

    private void parseData(XMLStreamReader reader) {

        if (reader.getEventType() == XMLStreamReader.START_ELEMENT && reader.hasText()) {
            String elementName = reader.getName().getLocalPart();
            if ("Person".equals(elementName)) {
                this.refIdWithAuthors.add(reader.getAttributeValue(null, "id"));
            }
            if ("Periodical".equals(elementName)) {
                this.refIdWithEditors.add(reader.getAttributeValue(null, "id"));
            }
            if ("Library".equals(elementName)) {
                this.refIdWithPublishers.add(reader.getAttributeValue(null, "id"));
            }
            if ("Keyword".equals(elementName)) {
                this.refIdWithKeywords.add(reader.getAttributeValue(null, "id"));
            }
            if ("UniqueFullName".equals(elementName)) {
                this.refIdWithAuthors.add(reader.getAttributeValue(null, "id"));
            }
            if ("Editor".equals(elementName)) {
                this.refIdWithEditors.add(reader.getAttributeValue(null, "id"));
            }
            if ("Publisher".equals(elementName)) {
                this.refIdWithPublishers.add(reader.getAttributeValue(null, "id"));
            }
        }
    }

    private EntryType getType(XMLStreamReader reader) {
        EntryType entryType = StandardEntryType.Article;

        if (reader.getEventType() == XMLStreamReader.START_ELEMENT) {
            if (reader.getEventType() == XMLStreamReader.CHARACTERS) {
                String elementName = reader.getName().getLocalPart();
                if ("ReferenceType".equals(elementName) && "ReferenceType".contains("Book")) {
                    entryType = StandardEntryType.Book;
                }
                if ("RefereneceType".equals(elementName) && "ReferenceType".contains("Article")) {
                    entryType = StandardEntryType.Article;
                }
            }
        }

        return entryType;
    }

    private String getAuthorName(String data) {
        if (refIdWithAuthors.isEmpty()) {
            return null;
        }

        return this.refIdWithAuthors.get(refIdWithAuthors.indexOf(data));
    }

    private String getEditorName(String data) {
        if (refIdWithEditors.isEmpty()) {
            return null;
        }
        return this.refIdWithEditors.get(refIdWithEditors.indexOf(data));
    }

    private String getKeywords(String data) {
        if (refIdWithKeywords.isEmpty()) {
            return null;
        }
        return this.refIdWithKeywords.get(refIdWithKeywords.indexOf(data));
    }

    private String getPublisher(String data) {
        if (refIdWithPublishers.isEmpty()) {
            return null;
        }
        return this.refIdWithPublishers.get(refIdWithPublishers.indexOf(data));
    }

    String cleanUpText(String text) {
        String result = removeSpacesBeforeLineBreak(text);
        result = result.replaceAll("(?<!\\\\)\\{", "\\\\{");
        result = result.replaceAll("(?<!\\\\)}", "\\\\}");
        return result;
    }

    private String removeSpacesBeforeLineBreak(String string) {
        return string.replaceAll(" +\r\n", "\r\n")
              .replaceAll(" +\n", "\n");
    }

    private XMLStreamReader mapperRoot(BufferedReader reader) throws XMLStreamException {
        Objects.requireNonNull(reader);

        mapper = xmlInputFactory.createXMLStreamReader(reader);

        while (mapper.getEventType() != XMLEvent.START_ELEMENT) {
            mapper.next();
        }

        return mapper;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        throw new UnsupportedOperationException("CitaviXmlImporter does not support importDatabase(BufferedReader reader). "
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

        BufferedInputStream bufferedInputStream = new BufferedInputStream(stream);
        Charset charset = getCharset(bufferedInputStream);
        InputStreamReader reader = new InputStreamReader(bufferedInputStream, charset);

        // clean up the temp files
        Files.delete(newFile);

        return new BufferedReader(reader);
    }

    private String clean(String input) {
        String result = StringUtil.unifyLineBreaks(input, " ")
                         .trim()
                         .replaceAll(" +", " ");
        return htmlToLatexFormatter.format(result);
    }

    enum QuotationTypeMapping {
        IMAGE_QUOTATION(0, "Image quotation"),
        DIRECT_QUOTATION(1, "Direct quotation"),
        INDIRECT_QUOTATION(2, "Indirect quotation"),
        SUMMARY(3, "Summary"),
        COMMENT(4, "Comment"),
        HIGHLIGHT(5, "Highlight"),
        HIGHLIGHT_RED(6, "Highlight in red");

        final int citaviType;
        final String name;

        QuotationTypeMapping(int citaviType, String name) {
            this.name = name;
            this.citaviType = citaviType;
        }

        String getName() {
            return name;
        }

        int getCitaviIndexType() {
            return citaviType;
        }
    }
}
