package org.jabref.logic.exporter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;

// relevant StAX imports
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

/**
 * TemplateExporter for exporting in MODS XML format.
 */
class ModsExporter extends Exporter {

    private static final String MODS_NAMESPACE_URI = "http://www.loc.gov/mods/v3";
    private static final String MINUS = "-";
    private static final String DOUBLE_MINUS = "--";
    private static final String MODS_SCHEMA_LOCATION = "http://www.loc.gov/standards/mods/v3/mods-3-6.xsd";

    public ModsExporter() {
        super("mods", "MODS", StandardFileType.XML);
    }

    @Override
    public void export(final BibDatabaseContext databaseContext, final Path file, List<BibEntry> entries) throws SaveException {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(entries);
        if (entries.isEmpty()) { // Only export if entries exist
            return;
        }

        try {
            // create writer -- do this in separate method and return writer
            XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
            FileOutputStream fOutputStream = new FileOutputStream(file.toFile());
            //XMLStreamWriter writer = outputFactory.createXMLStreamWriter(fOutputStream);
            IndentingXMLStreamWriter writer = new IndentingXMLStreamWriter(outputFactory.createXMLStreamWriter(fOutputStream));
            writer.setIndentStep("    ");
            writer.writeDTD("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");

            writer.writeStartElement("mods", "modsCollection", MODS_NAMESPACE_URI);
            writer.writeNamespace("mods", MODS_NAMESPACE_URI);
            writer.writeNamespace("ns2", "http://www.w3.org/1999/xlink");
            writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            writer.writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", MODS_SCHEMA_LOCATION);

            for (BibEntry bibEntry : entries) {

                //ModsDefinition mods = new ModsDefinition();

                bibEntry.getCitationKey().ifPresent(citeKey -> {
                    try {
                        addIdentifier(writer, new UnknownField("citekey"), citeKey);
                    } catch (
                            XMLStreamException e) {
                        throw new RuntimeException(e); // cant throw SaveException?
                    }
                });
                if (!bibEntry.getCitationKey().isPresent()) {
                    writer.writeStartElement("mods", "mods", MODS_NAMESPACE_URI);
                }

                Map<Field, String> fieldMap = new TreeMap<>(Comparator.comparing(Field::getName));
                fieldMap.putAll(bibEntry.getFieldMap());
                addGenre(writer, bibEntry.getType());

              /*  OriginInfoDefinition originInfo = new OriginInfoDefinition();
                PartDefinition partDefinition = new PartDefinition();
                RelatedItemDefinition relatedItem = new RelatedItemDefinition();*/


                for (Map.Entry<Field, String> entry : fieldMap.entrySet()) {
                    Field field = entry.getKey();
                    String value = entry.getValue();

                    if (StandardField.AUTHOR.equals(field)) {
                        handleAuthors(writer, value);
                    } else if (new UnknownField("affiliation").equals(field)) {
                        addAffiliation(writer, value);
                    } else if (StandardField.ABSTRACT.equals(field)) {
                        addAbstract(writer, value);
                    } else if (StandardField.TITLE.equals(field)) {
                        addTitle(writer, value); // this might have to be called within that second for loop.... i think this goes inside of relateditem lol
                    } else if (StandardField.LANGUAGE.equals(field)) {
                        addLanguage(writer, value);
                    } else if (StandardField.LOCATION.equals(field)) {
                        addLocation(writer, value);
                    } else if (StandardField.URL.equals(field)) {
                        addUrl(writer, value);
                    } else if (StandardField.NOTE.equals(field)) {
                        addNote(writer, value);
                    } else if (StandardField.KEYWORDS.equals(field)) {
                        addKeyWords(writer, value);
                    } else if (StandardField.URI.equals(field)) {
                        addIdentifier(writer, StandardField.URI, value);
                    } else if (StandardField.ISBN.equals(field)) {
                        addIdentifier(writer, StandardField.ISBN, value);
                    } else if (StandardField.ISSN.equals(field)) {
                        addIdentifier(writer, StandardField.ISSN, value);
                    } else if (StandardField.DOI.equals(field)) {
                        addIdentifier(writer, StandardField.DOI, value);
                    } else if (StandardField.PMID.equals(field)) {
                        addIdentifier(writer, StandardField.PMID, value);
                    }

                }

                // does not handle the case of empty origin information <originInfo/>
                writer.writeStartElement("mods", "originInfo", MODS_NAMESPACE_URI);
                for (Map.Entry<Field, String> entry : fieldMap.entrySet()) {
                    Field field = entry.getKey();
                    String value = entry.getValue();

                    addOriginInformation(writer,field, value); // might have to split this up?? the order is janky
                }
                writer.writeEndElement();

                // for related items
                writer.writeStartElement("mods", "relatedItem",MODS_NAMESPACE_URI);
                writer.writeAttribute("type", "host");
                writer.writeStartElement("mods","part", MODS_NAMESPACE_URI);
                for (Map.Entry<Field, String> entry : fieldMap.entrySet()) {
                    Field field = entry.getKey();
                    String value = entry.getValue();
                    if (StandardField.JOURNAL.equals(field)) {
                        addJournal(writer, value);
                    } else if (StandardField.PAGES.equals(field)) {
                        addPages(writer, value); // STILL NEED TO CHANGE
                    } else if (StandardField.VOLUME.equals(field)) {
                        addDetail(writer, StandardField.VOLUME, value);
                    } else if (StandardField.ISSUE.equals(field)) {
                        addDetail(writer, StandardField.ISSUE, value);
                    }
                }
                // idea: store within data structure?
                writer.writeEndElement(); // end part -- this is an issue because pages and detail should only be within part
                writer.writeEndElement(); // end relatedItem

                writer.writeStartElement("mods","typeOfResource",MODS_NAMESPACE_URI);
                writer.writeCharacters("text");
                writer.writeEndElement(); // end typeOfResource

            }

            // addRelatedAndOriginInfoToModsGroup(writer);
            // end element and close
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();

        } catch (
                XMLStreamException |
                FileNotFoundException ex) {
            throw new SaveException(ex);
        }
    }


    private void addRelatedAndOriginInfoToModsGroup(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("mods", "relatedItem",MODS_NAMESPACE_URI);
        writer.writeAttribute("type", "host");

        writer.writeStartElement("mods","part", MODS_NAMESPACE_URI);

        // inside here, we need to call the other stuff..

        writer.writeEndElement(); // end part
        writer.writeEndElement(); // end relatedItem

        writer.writeStartElement("mods","typeOfResource",MODS_NAMESPACE_URI);
        writer.writeCharacters("text");
        writer.writeEndElement(); // end typeOfResource
    }

    private void addGenre(XMLStreamWriter writer, EntryType entryType) throws XMLStreamException {
        writer.writeStartElement( "mods", "genre", MODS_NAMESPACE_URI);
        writer.writeCharacters(entryType.getName());
        writer.writeEndElement();
    }

    private void addAbstract(XMLStreamWriter writer, String value) throws XMLStreamException {
        writer.writeStartElement("mods", "abstract",MODS_NAMESPACE_URI);
        writer.writeCharacters(value);
        writer.writeEndElement(); // end abstract
    }

    private void addTitle(XMLStreamWriter writer, String value) throws XMLStreamException {
        writer.writeStartElement("mods", "titleInfo",MODS_NAMESPACE_URI);
        writer.writeStartElement("mods", "title",MODS_NAMESPACE_URI);
        writer.writeCharacters(value);
        writer.writeEndElement(); // end title
        writer.writeEndElement(); // end titleInfo
    }

    private void addAffiliation(XMLStreamWriter writer, String value) throws XMLStreamException {
        writer.writeStartElement("mods", "name",MODS_NAMESPACE_URI);
        writer.writeStartElement("mods", "affiliation",MODS_NAMESPACE_URI);
        writer.writeCharacters(value);
        writer.writeEndElement(); // end affiliation
        writer.writeEndElement(); // end name
    }

    private void addLocation(XMLStreamWriter writer, String value) throws XMLStreamException {
        writer.writeStartElement("mods","location", MODS_NAMESPACE_URI);
        String[] locations = value.split(", ");
        for (String location : locations) {
            writer.writeStartElement("mods","physicalLocation",MODS_NAMESPACE_URI);
            writer.writeCharacters(location);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void addNote(XMLStreamWriter writer, String value) throws XMLStreamException {
        String[] notes = value.split(", ");
        for (String note : notes) {
            writer.writeStartElement("mods","note", MODS_NAMESPACE_URI);
            writer.writeCharacters(note);
            writer.writeEndElement();
        }
    }

    private void addUrl(XMLStreamWriter writer, String value) throws XMLStreamException {
        String[] urls = value.split(", ");
        writer.writeStartElement("mods", "location", MODS_NAMESPACE_URI);
        for (String url : urls) {
            writer.writeStartElement("mods","url", MODS_NAMESPACE_URI);
            writer.writeCharacters(url);
            writer.writeEndElement();
        }
        writer.writeEndElement();


    }

    private void addJournal(XMLStreamWriter writer, String value) throws XMLStreamException { // this may also need to be called within second for loop?
        // Start RelatedItemDefinition
/*        writer.writeStartElement("mods", "relatedItem", MODS_NAMESPACE_URI);
        writer.writeAttribute("type", "host");*/

        // Start TitleInfoDefinition
        writer.writeStartElement("mods", "titleInfo",MODS_NAMESPACE_URI);

        // Write title element
        writer.writeStartElement("mods", "title",MODS_NAMESPACE_URI);
        writer.writeCharacters(value);
        writer.writeEndElement(); // End title element

        // End TitleInfoDefinition
        writer.writeEndElement(); // End titleInfo element

        // End RelatedItemDefinition
    /*    writer.writeEndElement(); // End relatedItem element*/
    }

    private void addLanguage(XMLStreamWriter writer, String value) throws XMLStreamException {
        writer.writeStartElement("mods", "language",MODS_NAMESPACE_URI);
        writer.writeStartElement("mods", "languageTerm",MODS_NAMESPACE_URI);
        writer.writeCharacters(value);
        writer.writeEndElement(); // end languageTerm
        writer.writeEndElement(); // end language
    }

    private void addPages(XMLStreamWriter writer, String value) throws XMLStreamException {
        if (value.contains(DOUBLE_MINUS)) {
            addStartAndEndPage(writer, value, DOUBLE_MINUS);
        } else if (value.contains(MINUS)) {
            addStartAndEndPage(writer, value, MINUS);
        } else {
            BigInteger total = new BigInteger(value);
            writer.writeStartElement("mods", "extent",MODS_NAMESPACE_URI);
            writer.writeCharacters(total.toString());
            writer.writeEndElement();
        }
    }

    private void addKeyWords(XMLStreamWriter writer, String value) throws XMLStreamException {
        String[] keywords = value.split(", ");

        for (String keyword : keywords) {
            writer.writeStartElement("mods", "subject",MODS_NAMESPACE_URI);
            writer.writeStartElement("mods", "topic",MODS_NAMESPACE_URI);
            writer.writeCharacters(keyword);
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }

    private void handleAuthors(XMLStreamWriter writer, String value) throws XMLStreamException {
        String[] authors = value.split("and");
        for (String author : authors) {
            writer.writeStartElement("mods", "name",MODS_NAMESPACE_URI);
            writer.writeAttribute("type", "personal");

            if (author.contains(",")) {
                // if author contains ","  then this indicates that the author has a forename and family name
                int commaIndex = author.indexOf(',');
                String familyName = author.substring(0, commaIndex);
                writer.writeStartElement("mods","namePart", MODS_NAMESPACE_URI);
                writer.writeAttribute("type", "family");
                writer.writeCharacters(familyName);
                writer.writeEndElement();

                // now take care of the forenames
                String forename = author.substring(commaIndex + 1);
                String[] forenames = forename.split(" ");
                for (String given : forenames) {
                    if (!given.isEmpty()) {
                        writer.writeStartElement("mods", "namePart", MODS_NAMESPACE_URI);
                        writer.writeAttribute("type", "given");
                        writer.writeCharacters(given);
                        writer.writeEndElement();
                    }
                }
                writer.writeEndElement();
            } else {
                // no "," indicates that there should only be a family name
                writer.writeStartElement("mods", "namePart",MODS_NAMESPACE_URI);
                writer.writeAttribute("type", "family");
                writer.writeCharacters(author);
                writer.writeEndElement();
                writer.writeEndElement();
            }
        }
    }

    private void addIdentifier(XMLStreamWriter writer, Field field, String value) throws XMLStreamException {

        if (new UnknownField("citekey").equals(field)) {
            writer.writeStartElement("mods", "mods", MODS_NAMESPACE_URI);
            writer.writeAttribute("ID", value);
        }
        writer.writeStartElement("mods", "identifier",MODS_NAMESPACE_URI);
        writer.writeAttribute("type", field.getName());
        writer.writeCharacters(value);
        writer.writeEndElement(); // end identifier
    }

    // THIS NEEDS TO CHANGE
    private void addStartAndEndPage(XMLStreamWriter writer, String value, String minus) throws XMLStreamException {
        int minusIndex = value.indexOf(minus);
        String startPage = value.substring(0, minusIndex);
        String endPage = "";
        if (MINUS.equals(minus)) {
            endPage = value.substring(minusIndex + 1);
        } else if (DOUBLE_MINUS.equals(minus)) {
            endPage = value.substring(minusIndex + 2);
        }

        writer.writeStartElement("mods", "extent",MODS_NAMESPACE_URI);
        writer.writeStartElement("mods", "start",MODS_NAMESPACE_URI);
        writer.writeCharacters(startPage);
        writer.writeEndElement();
        writer.writeStartElement("mods", "end",MODS_NAMESPACE_URI);
        writer.writeCharacters(endPage);
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private void addDetail(XMLStreamWriter writer, Field field, String value) throws XMLStreamException {
        writer.writeStartElement("mods","detail",MODS_NAMESPACE_URI);
        writer.writeAttribute("type", field.getName());
        writer.writeStartElement("mods","number", MODS_NAMESPACE_URI);
        writer.writeCharacters(value);
        writer.writeEndElement(); // end number
        writer.writeEndElement(); // end detail
    }

    private void addOriginInformation(XMLStreamWriter writer, Field field, String value) throws XMLStreamException {

        if (field.equals(StandardField.YEAR)) {
            addDate(writer, "dateIssued", value);
        } else if (field.equals(new UnknownField("created"))) {
            addDate(writer,"dateCreated", value);
        } else if (field.equals(StandardField.MODIFICATIONDATE)) {
            addDate(writer,"dateModified", value);
        } else if (field.equals(StandardField.CREATIONDATE)) {
            addDate(writer,"dateCaptured", value);
        } else if (StandardField.PUBLISHER.equals(field)) {
            writer.writeStartElement("mods", "publisher",MODS_NAMESPACE_URI);
            writer.writeAttribute("xsi",MODS_NAMESPACE_URI, "type","mods:stringPlusLanguagePlusSupplied");
            writer.writeCharacters(value);
            writer.writeEndElement();
        } else if (field.equals(new UnknownField("issuance"))) {
            writer.writeStartElement("mods", "issuance", MODS_NAMESPACE_URI);
            writer.writeCharacters(value);
            writer.writeEndElement();
        } else if (field.equals(StandardField.ADDRESS)) {
            writer.writeStartElement("mods", "place",MODS_NAMESPACE_URI);
            String[] places = value.split(", ");
            for (String place : places) {
                writer.writeStartElement("mods", "placeTerm",MODS_NAMESPACE_URI);
                writer.writeAttribute("type", "text");
                writer.writeCharacters(place);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        } else if (field.equals(StandardField.EDITION)) {
            writer.writeStartElement("mods", "edition",MODS_NAMESPACE_URI);
            writer.writeCharacters(value);
            writer.writeEndElement();
        }
    }

    private void addDate(XMLStreamWriter writer, String dateName, String value) throws XMLStreamException {
        writer.writeStartElement("mods", dateName, MODS_NAMESPACE_URI);
        writer.writeAttribute("keyDate", "yes");
        writer.writeCharacters(value);
        writer.writeEndElement(); // close date element
    }
}
