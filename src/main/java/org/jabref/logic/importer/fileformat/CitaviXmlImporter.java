package org.jabref.logic.importer.fileformat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import org.w3c.dom.Element;

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
        try (BufferedReader reader = getReaderFromZip(filePath)) {
            List<BibEntry> bibEntry = new ArrayList();
            mapper = mapperRoot(reader);

            while (mapper.hasNext()) {
                mapper.next();
                if (mapper.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    String elementName = mapper.getName().getLocalPart();
                    if ("Person".equals(elementName)) {
//                         this.refIdWithAuthors.add(mapper.getAttributeValue(null, "id"));
                        bibEntry.addAll(parseElements(mapper, elementName));
                    }
                    else if ("Periodical".equals(elementName)) {
//                         this.refIdWithEditors.add(mapper.getAttributeValue(null, "id"));
                        bibEntry.addAll(parseElements(mapper, elementName));
                    }
                    else if ("Library".equals(elementName)) {
//                         this.refIdWithPublishers.add(mapper.getAttributeValue(null, "id"));
                        bibEntry.addAll(parseElements(mapper, elementName));
                    }
                    else if ("Keyword".equals(elementName)) {
//                         this.refIdWithKeywords.add(mapper.getAttributeValue(null, "id"));
                        bibEntry.addAll(parseElements(mapper, elementName));
                    }
                    else if ("UniqueFullName".equals(elementName)) {
//                         this.refIdWithAuthors.add(mapper.getAttributeValue(null, "id"));
                        bibEntry.addAll(parseElements(mapper, elementName));
                    }
                    else if ("Editor".equals(elementName)) {
//                         this.refIdWithEditors.add(mapper.getAttributeValue(null, "id"));
                        bibEntry.addAll(parseElements(mapper, elementName));
                    }
                    else if ("Publisher".equals(elementName)) {
//                         this.refIdWithPublishers.add(mapper.getAttributeValue(null, "id"));
                        bibEntry.addAll(parseElements(mapper, elementName));
                    }
                }
            }

            return new ParserResult(bibEntry);
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }
    }

    private List<BibEntry> parseElements(XMLStreamReader reader, String startElement) {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        List<BibEntry> bibItems = new ArrayList();
        Map<Field, String> fields = new HashMap<>();

        try{
            while (reader.hasNext()) {
                reader.next();
                if (reader.getEventType() == XMLStreamConstants.END_ELEMENT && reader.getName().getLocalPart().equals(startElement)) {
                    break;
                }

                if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    if (reader.getEventType() == XMLStreamConstants.CHARACTERS) {
                        String elementName = reader.getName().getLocalPart();
                        if ("Title".equals(elementName)) {
                            fields.put(StandardField.TITLE, reader.getElementText());
                        } else if ("Name".equals(elementName)) {
                            fields.put(StandardField.TITLE, reader.getElementText());
                        } else if ("LastName".equals(elementName)) {
                            fields.put(StandardField.AUTHOR, reader.getElementText());
                        } else if ("FirstName".equals(elementName)) {
                            fields.put(StandardField.AUTHOR, reader.getElementText());
                        } else if ("MiddleName".equals(elementName)) {
                            fields.put(StandardField.AUTHOR, reader.getElementText());
                        } else if ("Year".equals(elementName)) {
                            fields.put(StandardField.YEAR, reader.getElementText());
                        } else if ("Volume".equals(elementName)) {
                            fields.put(StandardField.VOLUME, reader.getElementText());
                        } else if ("ISBN".equals(elementName)) {
                            fields.put(StandardField.ISBN, reader.getElementText());
                        } else if ("Abstract".equals(elementName)) {
                            fields.put(StandardField.ABSTRACT, reader.getElementText());
                        } else if ("DOI".equals(elementName)) {
                            fields.put(StandardField.DOI, reader.getElementText());
                        } else if ("KnowledgeItem".equals(elementName)) {
                            fields.put(StandardField.COMMENT, reader.getElementText());
                        } else if ("SeriesTitle".equals(elementName)) {
                            fields.put(new UnknownField("references"), reader.getElementText());
                        } else if ("Reference".equals(elementName)) {
                            fields.put(new UnknownField("references"), reader.getElementText());
                        } else if ("OneToN".equals(elementName)) {
                            fields.put(new UnknownField("references"), reader.getElementText());
                        } else if ("PageRangeNumber".equals(elementName)) {
                            fields.put(StandardField.PAGES, reader.getElementText());
                        }
                    }
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
        }

        entry.setField(fields);
        bibItems.add(entry);

        return bibItems;
    }

    private EntryType getType(XMLStreamReader reader, String startElement) {
        EntryType entryType = StandardEntryType.Article;

        try{
            while (reader.hasNext()) {
                reader.next();
                if (reader.getEventType() == XMLStreamConstants.END_ELEMENT && reader.getName().getLocalPart().equals(startElement)) {
                    break;
                }

                if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    if (reader.getEventType() == XMLStreamConstants.CHARACTERS) {
                        String elementName = reader.getName().getLocalPart();
                        if ("ReferenceType".equals(elementName) && "ReferenceType".contains("Book")) {
                            entryType = StandardEntryType.Book;
                        }
                        else if ("RefereneceType".equals(elementName) && "ReferenceType".contains("Article")) {
                            entryType = StandardEntryType.Article;
                        }
                    }
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
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

        while (mapper.getEventType() != XMLStreamConstants.START_ELEMENT) {
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

        BufferedInputStream bufferedInputStream = new BufferedInputStream(checkForUtf8BOMAndDiscardIfAny(stream));

        // Citavi XML files sometimes contains BOM markers. We just discard them.
        // Solution inspired by https://stackoverflow.com/a/37445972/873282
        return new BufferedReader(new InputStreamReader(bufferedInputStream));
    }

    private static InputStream checkForUtf8BOMAndDiscardIfAny(InputStream inputStream) throws IOException {
        byte[] bom = new byte[3];
        PushbackInputStream pushbackInputStream = new PushbackInputStream(new BufferedInputStream(inputStream), bom.length);

        if (pushbackInputStream.read(bom) != -1) {
            if (!(bom[0] == 0xEF) && !(bom[1] == 0xBB) && !(bom[2] == 0xBF)) {
                pushbackInputStream.unread(bom);
            }
        }

        return pushbackInputStream;
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
