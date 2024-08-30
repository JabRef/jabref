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
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.importer.fileformat.medline.ArticleId;
import org.jabref.logic.importer.fileformat.medline.OtherId;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
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
            while (((str = reader.readLine()) != null)) {
                if (str.toLowerCase(Locale.ROOT).contains("CitaviExchangeData")){
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
            Object mapper = mapperRoot(reader);

            if (mapper instanceof XMLStreamReader data) {
                while (data.hasNext()) {
                    data.next();
                    if (data.getEventType() == XMLEvent.START_ELEMENT) {
                        bibEntries.addAll(parseDataList(data));
                    }
                    if (data.getEventType() == XMLEvent.END_ELEMENT) {
                        break;
                    }
                }
            } else {
                return ParserResult.fromErrorMessage("File does not start with xml tag.");
            }
        } catch(XMLStreamException e) {
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
                    if ("Person".equals(elementName)) {
                        this.refIdWithAuthors.add(reader.getText());
                        bibEntries.add(parseData(reader));
                    }
                    if ("Keyword".equals(elementName)) {
                        this.refIdWithKeywords.add(reader.getText());
                        bibEntries.add(parseData(reader));
                    }
                    if ("Periodicals".equals(elementName) || "Publisher".equals(elementName)) {
                        this.refIdWithPublishers.add(reader.getText());
                        bibEntries.add(parseData(reader));
                    }
                    if ("Libraries".equals(elementName)) {
                        this.refIdWithEditors.add(reader.getText());
                        bibEntries.add(parseData(reader));
                    }
                    if ("KnowledgeItem".equals(elementName) ||
                    "Reference".equals(elementName)) {
                        this.refIdWithEditors.add(reader.getText());
                        bibEntries.add(parseData(reader));
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

    private BibEntry parseData(XMLStreamReader reader) {
        BibEntry entry = new BibEntry();

        String elementName = reader.getName().getLocalPart();
        if ("Name".equals(elementName)) {
            entry.setField(StandardField.JOURNAL, reader.getText());
        }
        if ("LastName".equals(elementName)) {
            entry.setField(StandardField.AUTHOR, reader.getText());
        }
        if ("Author".equals(elementName)) {
            entry.setField(StandardField.AUTHOR, reader.getText());
        }
        if ("FirstName".equals(elementName)) {
            entry.setField(StandardField.AUTHOR, reader.getText());
        }
        if ("UniqueFullName".equals(elementName)) {
            entry.setField(StandardField.AUTHOR, getAuthorName(reader.getText()));
        }
        if ("Editor".equals(elementName)) {
            entry.setField(StandardField.EDITOR, reader.getText());
        }
        if ("Publisher".equals(elementName)) {
            entry.setField(StandardField.PUBLISHER, reader.getText());
        }
        if ("Title".equals(elementName)) {
            entry.setField(StandardField.TITLE,         reader.getText());
        }
        if ("Year".equals(elementName)) {
            entry.setField(StandardField.YEAR, reader.getText());
        }
        if ("Volume".equals(elementName)) {
            entry.setField(StandardField.VOLUME, reader.getText());
        }
        if ("ISBN".equals(elementName)) {
            entry.setField(StandardField.ISBN, reader.getText());
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

        String pages = getPages(reader);
        // Cleans also unicode minus signs
        pages = pagesFormatter.format(pages);
        entry.setField(StandardField.PAGES, pages);
        return entry;
    }

    private EntryType getType(XMLStreamReader reader) {
        EntryType entryType = StandardEntryType.Article;

        try {
            while (reader.hasNext()) {
                reader.next();
                if (reader.getEventType() == XMLEvent.START_ELEMENT) {
                    String elementName = reader.getName().getLocalPart();
                    switch (elementName) {
                        case "artwork", "generic", "musicalbum", "audioorvideodocument", "movie" -> {
                            entryType = StandardEntryType.Misc;
                        }
                        case "electronic article" -> {
                            entryType = IEEETranEntryType.Electronic;
                        }
                        case "book section" -> {
                            entryType = StandardEntryType.InBook;
                        }
                        case "book", "bookedited", "audiobook" -> {
                            entryType = StandardEntryType.Book;
                        }
                        case "report" -> {
                            entryType = StandardEntryType.Report;
                        }
                    }
                }
                if (reader.getEventType() == XMLEvent.END_ELEMENT) {
                    break;
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
        }
        return entryType;
    }

    private String getPages(XMLStreamReader reader) {
        String pages = "";

        try {
            while (reader.hasNext()) {
                reader.next();
                if (reader.getEventType() == XMLEvent.START_ELEMENT) {
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
