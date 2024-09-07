package org.jabref.logic.importer.fileformat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
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

import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLInputFactory;
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
            Objects.requireNonNull(reader);
            Object mapper = mapperRoot(reader);

            if (mapper instanceof XMLStreamReader data) {
                while (data.hasNext()) {
                    data.next();
                    if (data.getEventType() == XMLEvent.START_ELEMENT) {
                        String elementName = data.getName().getLocalPart();
                        if (!elementName.isEmpty() && elementName != null) {
                            bibEntries.addAll(parseDataList(data));
                        }
                    }
                    if (data.getEventType() == XMLEvent.END_ELEMENT) {
                        break;
                    }
                }
            } else {
                return ParserResult.fromErrorMessage("File does not start with xml tag.");
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }

        return new ParserResult(bibEntries);
    }

    private List<BibEntry> parseDataList(XMLStreamReader reader) {
        List<BibEntry> bibEntries = new ArrayList<>();

        try {
            while (reader.hasNext()) {
                reader.next();
                if (reader.getEventType() == XMLEvent.START_ELEMENT) {
                    String elementName = reader.getName().getLocalPart();
                    if ("Persons".equals(elementName)) {
                        this.refIdWithAuthors.add(reader.getText());
                        bibEntries.addAll(parseData(reader));
                    }
                    if ("Keywords".equals(elementName)) {
                        this.refIdWithKeywords.add(reader.getText());
                        bibEntries.addAll(parseData(reader));
                    }
                    if ("Periodicals".equals(elementName) || "Publishers".equals(elementName) || "SeriesTitles".equals(elementName)) {
                        this.refIdWithPublishers.add(reader.getText());
                        bibEntries.addAll(parseData(reader));
                    }
                    if ("Libraries".equals(elementName) || "Locations".equals(elementName)) {
                        this.refIdWithEditors.add(reader.getText());
                        bibEntries.addAll(parseData(reader));
                    }
                    if ("KnowledgeItems".equals(elementName) ||
                    "References".equals(elementName)) {
                        this.refIdWithEditors.add(reader.getText());
                        bibEntries.addAll(parseData(reader));
                    }
                    if ("Categories".equals(elementName) || "CategoryCategories".equals(elementName) || "ReferencePublishers".equals(elementName) || "ReferenceOrganizations".equals(elementName) || "ReferenceKeywords".equals(elementName) || "ReferenceGroup".equals(elementName) || "ReferenceEditors".equals(elementName) || "ReferenceCollaborators".equals(elementName) || "ReferenceCategories".equals(elementName) || "ReferenceAuthors".equals(elementName) || "ReferenceReferences".equals(elementName)) {
                        bibEntries.addAll(parseData(reader));
                    }
                }
                if (reader.getEventType() == XMLEvent.END_ELEMENT) {
                    break;
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
        }

        return bibEntries;
    }

    private BibEntry parseElements(XMLStreamReader reader) {
        List<BibEntry> bibEntries = new ArrayList<>();
        BibEntry entry = new BibEntry();

        try {
            while (reader.hasNext()) {
                reader.next();
                entry.setType(getType(reader));
                if (reader.getEventType() == XMLEvent.CHARACTERS) {
                    String elementName = reader.getName().getLocalPart();
                    if ("Title".equals(elementName)) {
                        entry.setField(StandardField.TITLE, reader.getText());
                        bibEntries.add(entry);
                    }
                    if ("Name".equals(elementName)) {
                        entry.setField(StandardField.TITLE, reader.getText());
                        bibEntries.add(entry);
                    }
                    if ("LastName".equals(elementName)) {
                        entry.setField(StandardField.AUTHOR, reader.getText());
                        bibEntries.add(entry);
                    }
                    if ("FirstName".equals(elementName)) {
                        entry.setField(StandardField.AUTHOR, reader.getText());
                        bibEntries.add(entry);
                    }
                    if ("MiddleName".equals(elementName)) {
                        entry.setField(StandardField.AUTHOR, reader.getText());
                        bibEntries.add(entry);
                    }
                    if ("Year".equals(elementName)) {
                        entry.setField(StandardField.YEAR, reader.getText());
                        bibEntries.add(entry);
                    }
                    if ("Volume".equals(elementName)) {
                        entry.setField(StandardField.VOLUME, reader.getText());
                        bibEntries.add(entry);
                    }
                    if ("ISBN".equals(elementName)) {
                        entry.setField(StandardField.ISBN, reader.getText());
                        bibEntries.add(entry);
                    }
                    if ("Abstract".equals(elementName)) {
                        entry.setField(StandardField.ABSTRACT, reader.getText());
                        bibEntries.add(entry);
                    }
                    if ("DOI".equals(elementName)) {
                        entry.setField(StandardField.DOI, reader.getText());
                        bibEntries.add(entry);
                    }
                    if ("KnowledgeItem".equals(elementName)) {
                        entry.setField(StandardField.COMMENT, reader.getText());
                        bibEntries.add(entry);
                    }
                    if ("SeriesTitle".equals(elementName)) {
                        entry.setField(new UnknownField("references"), reader.getText());
                        bibEntries.add(entry);
                    }
                    if ("Reference".equals(elementName)) {
                        entry.setField(new UnknownField("references"), reader.getText());
                        bibEntries.add(entry);
                    }
                    if ("OneToN".equals(elementName)) {
                        entry.setField(new UnknownField("references"), reader.getText());
                        bibEntries.add(entry);
                    }

                    String pages = getPages(reader);
                    // Cleans also unicode minus signs
                    pages = pagesFormatter.format(pages);
                    entry.setField(StandardField.PAGES, pages);
                    bibEntries.add(entry);
                }
                if (reader.getEventType() == XMLEvent.END_ELEMENT) {
                    break;
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
        }
        return entry;
    }

    private List<BibEntry> parseData(XMLStreamReader reader) {
        List<BibEntry> entry = new ArrayList<>();

        try {
            while (reader.hasNext()) {
                reader.next();
                if (reader.getEventType() == XMLEvent.START_ELEMENT) {
                    String elementName = reader.getName().getLocalPart();
                    if ("Person".equals(elementName)) {
                        entry.add(parseElements(reader));
                    }
                    if ("Periodical".equals(elementName)) {
                        entry.add(parseElements(reader));
                    }
                    if ("Library".equals(elementName)) {
                        entry.add(parseElements(reader));
                    }
                    if ("Keyword".equals(elementName)) {
                        entry.add(parseElements(reader));
                    }
                    if ("UniqueFullName".equals(elementName)) {
                        entry.add(parseElements(reader));
                    }
                    if ("Editor".equals(elementName)) {
                        entry.add(parseElements(reader));
                    }
                    if ("Publisher".equals(elementName)) {
                        entry.add(parseElements(reader));
                    }
                }
                if (reader.getEventType() == XMLEvent.END_ELEMENT) {
                    break;
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
        }
        return entry;
    }

    private EntryType getType(XMLStreamReader reader) {
        EntryType entryType = StandardEntryType.Misc;

        if ("book".equals(reader.getName().getLocalPart())) {
            entryType = StandardEntryType.Book;
        }
        if ("article".equals(reader.getName().getLocalPart())) {
            entryType = StandardEntryType.Article;
        }

        return entryType;
    }

    private String getPages(XMLStreamReader reader) {
        String pages = "";

        try {
            while (reader.hasNext()) {
                reader.next();
                if (reader.getEventType() == XMLEvent.CHARACTERS) {
                    String elementName = reader.getName().getLocalPart();

                    if ("PageRangeNumber".equals(elementName)) {
                        pages = reader.getText();
                    }
                }
                if (reader.getEventType() == XMLEvent.END_ELEMENT) {
                    break;
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
        }
        return pages;
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

    private Object mapperRoot(BufferedReader reader) throws XMLStreamException {
        Objects.requireNonNull(reader);
        XMLStreamReader xmlStreamReader;

        try {
            xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);
        } catch (Exception e) {
            LOGGER.debug("could not read document", e);
            return ParserResult.fromError(e);
        }
        return xmlStreamReader;
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
        Path newFile = Files.createTempFile("citavicontent", ".xml");
        newFile.toFile().deleteOnExit();

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(filePath))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                Files.copy(zis, newFile, StandardCopyOption.REPLACE_EXISTING);
                zipEntry = zis.getNextEntry();
            }
        }

        // check and delete the utf-8 BOM bytes
        InputStream newStream = new BufferedInputStream(new BOMInputStream(Files.newInputStream(newFile, StandardOpenOption.READ), false, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_32BE, ByteOrderMark.UTF_32LE));

        BufferedInputStream bufferedInputStream = new BufferedInputStream(newStream);
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
