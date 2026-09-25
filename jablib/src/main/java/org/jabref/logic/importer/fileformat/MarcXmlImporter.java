package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.strings.StringUtil;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Reads MARCXML records and collections, including records inside SRU responses.
// [impl->req~import.bibliographic.xml-formats~1]
@NullMarked
public class MarcXmlImporter extends Importer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarcXmlImporter.class);
    private static final String MARC_NAMESPACE = "http://www.loc.gov/MARC21/slim";

    @Override
    public String getId() {
        return "marcxml";
    }

    @Override
    public String getName() {
        return "MARC21 XML";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Importer for the MARC21 XML format.");
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.XML;
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return isRecognizedFormat((Reader) input);
    }

    @Override
    public boolean isRecognizedFormat(Reader input) throws IOException {
        try {
            return scan(input).recognized();
        } catch (XMLStreamException e) {
            LOGGER.debug("Could not recognize MARCXML", e);
            return false;
        }
    }

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        try {
            ScanResult scanResult = scan(input);
            if (!scanResult.recognized()) {
                return new ParserResult();
            }
            return new ParserResult(new MarcXmlParser().parseEntries(
                    new ByteArrayInputStream(scanResult.sruXml().getBytes(StandardCharsets.UTF_8))));
        } catch (ParseException | XMLStreamException e) {
            LOGGER.debug("Could not parse MARCXML", e);
            return ParserResult.fromError(e);
        }
    }

    private ScanResult scan(Reader input) throws XMLStreamException {
        XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        inputFactory.setXMLResolver((_, _, _, _) -> {
            throw new XMLStreamException("External entities are not allowed in MARCXML");
        });

        XMLStreamReader reader = inputFactory.createXMLStreamReader(input);
        StringWriter output = new StringWriter();
        XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
        try {
            writer.writeStartElement("searchRetrieveResponse");
            writer.writeStartElement("records");
            List<String> path = new ArrayList<>();
            boolean recognized = false;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.DTD) {
                    throw new XMLStreamException("Document types are not allowed in MARCXML");
                }
                if (event == XMLStreamConstants.START_ELEMENT) {
                    path.add(reader.getLocalName());
                    if (isRecordStart(reader, path)) {
                        writer.writeStartElement("record");
                        writer.writeStartElement("recordData");
                        recognized |= writeRecord(reader, writer);
                        writer.writeEndElement();
                        writer.writeEndElement();
                        path.removeLast();
                    } else if (path.size() == 1 && "collection".equals(path.getFirst())
                            && MARC_NAMESPACE.equals(reader.getNamespaceURI())) {
                        recognized = true;
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    path.removeLast();
                }
            }
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            return new ScanResult(recognized, output.toString());
        } finally {
            reader.close();
            writer.close();
        }
    }

    private boolean isRecordStart(XMLStreamReader reader, List<String> path) {
        String namespace = reader.getNamespaceURI();
        if (!"record".equals(reader.getLocalName()) || !(MARC_NAMESPACE.equals(namespace) || StringUtil.isBlank(namespace))) {
            return false;
        }
        if (path.size() == 1 || path.size() == 2 && "collection".equals(path.getFirst())) {
            return true;
        }
        return "searchRetrieveResponse".equals(path.getFirst()) && path.size() >= 2
                && "recordData".equals(path.get(path.size() - 2));
    }

    private boolean writeRecord(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("record");
        boolean recognized = false;
        boolean inDatafield = false;
        boolean inSubfield = false;
        int depth = 1;
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.DTD) {
                throw new XMLStreamException("Document types are not allowed in MARCXML");
            }
            if (event == XMLStreamConstants.START_ELEMENT) {
                depth++;
                if (depth == 2) {
                    recognized |= switch (reader.getLocalName()) {
                        case "leader", "controlfield", "datafield" -> true;
                        default -> false;
                    };
                    if ("datafield".equals(reader.getLocalName())) {
                        writer.writeStartElement("datafield");
                        writeAttribute(reader, writer, "tag");
                        writeAttribute(reader, writer, "ind1");
                        writeAttribute(reader, writer, "ind2");
                        inDatafield = true;
                    }
                } else if (depth == 3 && inDatafield && "subfield".equals(reader.getLocalName())) {
                    writer.writeStartElement("subfield");
                    writeAttribute(reader, writer, "code");
                    inSubfield = true;
                }
            } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                if (inSubfield && depth == 3) {
                    writer.writeCharacters(reader.getText());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (depth == 3 && inSubfield) {
                    writer.writeEndElement();
                    inSubfield = false;
                } else if (depth == 2 && inDatafield) {
                    writer.writeEndElement();
                    inDatafield = false;
                }
                depth--;
                if (depth == 0) {
                    writer.writeEndElement();
                    return recognized;
                }
            }
        }
        throw new XMLStreamException("Incomplete MARCXML record");
    }

    private void writeAttribute(XMLStreamReader reader, XMLStreamWriter writer, String name) throws XMLStreamException {
        Optional<String> value = Optional.ofNullable(reader.getAttributeValue(null, name));
        if (value.isPresent()) {
            writer.writeAttribute(name, value.orElseThrow());
        }
    }

    private record ScanResult(boolean recognized, String sruXml) {
    }
}
