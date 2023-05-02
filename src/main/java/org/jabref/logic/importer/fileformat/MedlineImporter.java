package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.medline.ArticleId;
import org.jabref.logic.importer.fileformat.medline.Investigator;
import org.jabref.logic.importer.fileformat.medline.MeshHeading;
import org.jabref.logic.importer.fileformat.medline.OtherId;
import org.jabref.logic.importer.fileformat.medline.PersonalNameSubject;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importer for the Medline/Pubmed format.
 * <p>
 * check here for details on the format https://www.nlm.nih.gov/bsd/licensee/elements_descriptions.html
 */
public class MedlineImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(MedlineImporter.class);
    private static final String KEYWORD_SEPARATOR = "; ";

    private static final Locale ENGLISH = Locale.ENGLISH;

    private static String join(List<String> list, String string) {
        return Joiner.on(string).join(list);
    }

    @Override
    public String getName() {
        return "Medline/PubMed";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.MEDLINE;
    }

    @Override
    public String getId() {
        return "medline";
    }

    @Override
    public String getDescription() {
        return "Importer for the Medline format.";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        String str;
        int i = 0;
        while (((str = reader.readLine()) != null) && (i < 50)) {
            if (str.toLowerCase(ENGLISH).contains("<pubmedarticle>")
                    || str.toLowerCase(ENGLISH).contains("<pubmedbookarticle>")) {
                return true;
            }

            i++;
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        Objects.requireNonNull(input);

        List<BibEntry> bibItems = new ArrayList<>();

        try {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

            // prevent xxe (https://rules.sonarsource.com/java/RSPEC-2755)
            xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            // required for reading Unicode characters such as &#xf6;
            xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);

            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(input);

            while (reader.hasNext()) {
                reader.next();
                if (isStartXMLEvent(reader)) {
                    String elementName = reader.getName().getLocalPart();
                    switch (elementName) {
                        case "PubmedArticle" -> {
                            parseArticle(reader, bibItems, elementName);
                        }
                        case "PubmedBookArticle" -> {
                            parseBookArticle(reader, bibItems, elementName);
                        }
                    }
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }

        return new ParserResult(bibItems);
    }

    private void parseBookArticle(XMLStreamReader reader, List<BibEntry> bibItems, String startElement)
            throws XMLStreamException {
        Map<Field, String> fields = new HashMap<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "BookDocument" -> {
                        parseBookDocument(reader, fields, elementName);
                    }
                    case "PublicationStatus" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.PUBSTATE, reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(fields);

        bibItems.add(entry);
    }

    private void parseBookDocument(XMLStreamReader reader, Map<Field, String> fields, String startElement)
            throws XMLStreamException {
        // multiple occurrences of the following fields can be present
        List<String> sectionTitleList = new ArrayList<>();
        List<String> keywordList = new ArrayList<>();
        List<String> publicationTypeList = new ArrayList<>();
        List<String> articleTitleList = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "PMID" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            fields.put(StandardField.PMID, reader.getText());
                        }
                    }
                    case "DateRevised", "ContributionDate" -> {
                        parseDate(reader, fields, elementName);
                    }
                    case "Abstract" -> {
                        addAbstract(reader, fields, elementName);
                    }
                    case "Pagination" -> {
                        addPagination(reader, fields, elementName);
                    }
                    case "Section" -> {
                        parseSections(reader, sectionTitleList);
                    }
                    case "Keyword" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            keywordList.add(reader.getText());
                        }
                    }
                    case "PublicationType" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            publicationTypeList.add(reader.getText());
                        }
                    }
                    case "ArticleTitle" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            articleTitleList.add(reader.getText());
                        }
                    }
                    case "Book" -> {
                        parseBookInformation(reader, fields, elementName);
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        // populate multiple occurrence fields
        if (!sectionTitleList.isEmpty()) {
            fields.put(new UnknownField("sections"), join(sectionTitleList, "; "));
        }
        addKeywords(fields, keywordList);
        if (!publicationTypeList.isEmpty()) {
            fields.put(new UnknownField("pubtype"), join(publicationTypeList, ", "));
        }
        if (!articleTitleList.isEmpty()) {
            fields.put(new UnknownField("article"), join(articleTitleList, ", "));
        }
    }

    private void parseBookInformation(XMLStreamReader reader, Map<Field, String> fields, String startElement)
            throws XMLStreamException {
        List<String> isbnList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "PublisherName" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.PUBLISHER, reader.getText());
                        }
                    }
                    case "PublisherLocation" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, new UnknownField("publocation"), reader.getText());
                        }
                    }
                    case "BookTitle" -> {
                        handleTextElement(reader, titleList, elementName);
                    }
                    case "PubDate" -> {
                        addPubDate(reader, fields, elementName);
                    }
                    case "AuthorList" -> {
                        handleAuthorList(reader, fields, elementName);
                    }
                    case "Volume" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.VOLUME, reader.getText());
                        }
                    }
                    case "Edition" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.EDITION, reader.getText());
                        }
                    }
                    case "Medium" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, new UnknownField("medium"), reader.getText());
                        }
                    }
                    case "ReportNumber" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, new UnknownField("reportnumber"), reader.getText());
                        }
                    }
                    case "ELocationID" -> {
                        String eidType = reader.getAttributeValue(null, "EIdType");
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            handleElocationId(fields, reader, eidType);
                        }
                    }
                    case "Isbn" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            isbnList.add(reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        if (!isbnList.isEmpty()) {
            fields.put(StandardField.ISBN, join(isbnList, ", "));
        }

        if (!titleList.isEmpty()) {
            putIfValueNotNull(fields, StandardField.TITLE, join(titleList, " "));
        }
    }

    private void handleElocationId(Map<Field, String> fields, XMLStreamReader reader, String eidType) {
        if (eidType.equals("doi")) {
            fields.put(StandardField.DOI, reader.getText());
        }
        if (eidType.equals("pii")) {
            fields.put(new UnknownField("pii"), reader.getText());
        }
    }

    private void parseSections(XMLStreamReader reader, List<String> sectionTitleList) throws XMLStreamException {
        int sectionLevel = 0;

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "SectionTitle" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader) && sectionLevel == 0) {
                            // we only collect SectionTitles from root level Sections
                            sectionTitleList.add(reader.getText());
                        }
                    }
                    case "Section" -> {
                        sectionLevel++;
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("Section")) {
                if (sectionLevel == 0) {
                    break;
                } else {
                    sectionLevel--;
                }
            }
        }
    }

    private void parseArticle(XMLStreamReader reader, List<BibEntry> bibItems, String startElement)
            throws XMLStreamException {
        Map<Field, String> fields = new HashMap<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "MedlineCitation" -> {
                        parseMedlineCitation(reader, fields, elementName);
                    }
                    case "PubmedData" -> {
                        parsePubmedData(reader, fields, elementName);
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(fields);

        bibItems.add(entry);
    }

    private void parsePubmedData(XMLStreamReader reader, Map<Field, String> fields, String startElement)
            throws XMLStreamException {
        String publicationStatus = "";
        List<ArticleId> articleIdList = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "PublicationStatus" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            publicationStatus = reader.getText();
                        }
                    }
                    case "ArticleId" -> {
                        String idType = reader.getAttributeValue(null, "IdType");
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            articleIdList.add(new ArticleId(idType, reader.getText()));
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        if (fields.get(new UnknownField("revised")) != null) {
            putIfValueNotNull(fields, StandardField.PUBSTATE, publicationStatus);
            if (!articleIdList.isEmpty()) {
                addArticleIdList(fields, articleIdList);
            }
        }
    }

    private void parseMedlineCitation(XMLStreamReader reader, Map<Field, String> fields, String startElement)
            throws XMLStreamException {
        // multiple occurrences of the following fields can be present
        List<String> citationSubsets = new ArrayList<>();
        List<MeshHeading> meshHeadingList = new ArrayList<>();
        List<PersonalNameSubject> personalNameSubjectList = new ArrayList<>();
        List<OtherId> otherIdList = new ArrayList<>();
        List<String> keywordList = new ArrayList<>();
        List<String> spaceFlightMissionList = new ArrayList<>();
        List<Investigator> investigatorList = new ArrayList<>();
        List<String> generalNoteList = new ArrayList<>();

        String status = reader.getAttributeValue(null, "Status");
        String owner = reader.getAttributeValue(null, "Owner");
        int latestVersion = 0;
        fields.put(new UnknownField("status"), status);
        fields.put(StandardField.OWNER, owner);

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "DateCreated", "DateCompleted", "DateRevised" -> {
                        parseDate(reader, fields, elementName);
                    }
                    case "Article" -> {
                        parseArticleInformation(reader, fields);
                    }
                    case "PMID" -> {
                        String versionStr = reader.getAttributeValue(null, "Version");
                        reader.next();
                        if (versionStr != null) {
                            int version = Integer.parseInt(versionStr);
                            if (isCharacterXMLEvent(reader) && version > latestVersion) {
                                latestVersion = version;
                                fields.put(StandardField.PMID, reader.getText());
                            }
                        }
                    }
                    case "MedlineJournalInfo" -> {
                        parseMedlineJournalInfo(reader, fields, elementName);
                    }
                    case "ChemicalList" -> {
                        parseChemicalList(reader, fields, elementName);
                    }
                    case "CitationSubset" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            citationSubsets.add(reader.getText());
                        }
                    }
                    case "GeneSymbolList" -> {
                        parseGeneSymbolList(reader, fields, elementName);
                    }
                    case "MeshHeading" -> {
                        parseMeshHeading(reader, meshHeadingList, elementName);
                    }
                    case "NumberOfReferences" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, new UnknownField("references"), reader.getText());
                        }
                    }
                    case "PersonalNameSubject" -> {
                        parsePersonalNameSubject(reader, personalNameSubjectList, elementName);
                    }
                    case "OtherID" -> {
                        String otherIdSource = reader.getAttributeValue(null, "Source");
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            String content = reader.getText();
                            otherIdList.add(new OtherId(otherIdSource, content));
                        }
                    }
                    case "Keyword" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            keywordList.add(reader.getText());
                        }
                    }
                    case "SpaceFlightMission" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            spaceFlightMissionList.add(reader.getText());
                        }
                    }
                    case "Investigator" -> {
                        parseInvestigator(reader, investigatorList, elementName);
                    }
                    case "GeneralNote" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            generalNoteList.add(reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        // populate multiple occurrence fields
        if (!citationSubsets.isEmpty()) {
            fields.put(new UnknownField("citation-subset"), join(citationSubsets, ", "));
        }
        addMeshHeading(fields, meshHeadingList);
        addPersonalNames(fields, personalNameSubjectList);
        addOtherId(fields, otherIdList);
        addKeywords(fields, keywordList);
        if (!spaceFlightMissionList.isEmpty()) {
            fields.put(new UnknownField("space-flight-mission"), join(spaceFlightMissionList, ", "));
        }
        addInvestigators(fields, investigatorList);
        addNotes(fields, generalNoteList);
    }

    private void parseInvestigator(XMLStreamReader reader, List<Investigator> investigatorList, String startElement)
            throws XMLStreamException {
        String lastName = "";
        String foreName = "";
        List<String> affiliationList = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "LastName" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            lastName = reader.getText();
                        }
                    }
                    case "ForeName" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            foreName = reader.getText();
                        }
                    }
                    case "Affiliation" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            affiliationList.add(reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        investigatorList.add(new Investigator(lastName, foreName, affiliationList));
    }

    private void parsePersonalNameSubject(XMLStreamReader reader, List<PersonalNameSubject> personalNameSubjectList, String startElement)
            throws XMLStreamException {
        String lastName = "";
        String foreName = "";

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "LastName" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            lastName = reader.getText();
                        }
                    }
                    case "ForeName" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            foreName = reader.getText();
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        personalNameSubjectList.add(new PersonalNameSubject(lastName, foreName));
    }

    private void parseMeshHeading(XMLStreamReader reader, List<MeshHeading> meshHeadingList, String startElement)
            throws XMLStreamException {
        String descriptorName = "";
        List<String> qualifierNames = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "DescriptorName" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            descriptorName = reader.getText();
                        }
                    }
                    case "QualifierName" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            qualifierNames.add(reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        meshHeadingList.add(new MeshHeading(descriptorName, qualifierNames));
    }

    private void parseGeneSymbolList(XMLStreamReader reader, Map<Field, String> fields, String startElement)
            throws XMLStreamException {
        List<String> geneSymbols = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                if (elementName.equals("GeneSymbol")) {
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        geneSymbols.add(reader.getText());
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        if (!geneSymbols.isEmpty()) {
            fields.put(new UnknownField("gene-symbols"), join(geneSymbols, ", "));
        }
    }

    private void parseChemicalList(XMLStreamReader reader, Map<Field, String> fields, String startElement)
            throws XMLStreamException {
        List<String> chemicalNames = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                if (elementName.equals("NameOfSubstance")) {
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        chemicalNames.add(reader.getText());
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        fields.put(new UnknownField("chemicals"), join(chemicalNames, ", "));
    }

    private void parseMedlineJournalInfo(XMLStreamReader reader, Map<Field, String> fields, String startElement)
            throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "Country" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, new UnknownField("country"), reader.getText());
                        }
                    }
                    case "MedlineTA" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, new UnknownField("journal-abbreviation"), reader.getText());
                        }
                    }
                    case "NlmUniqueID" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, new UnknownField("nlm-id"), reader.getText());
                        }
                    }
                    case "ISSNLinking" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, new UnknownField("issn-linking"), reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }
    }

    private void parseArticleInformation(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        List<String> titleList = new ArrayList<>();
        String pubmodel = reader.getAttributeValue(null, "PubModel");
        fields.put(new UnknownField("pubmodel"), pubmodel);

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "Journal" -> {
                        parseJournal(reader, fields);
                    }
                    case "ArticleTitle" -> {
                        handleTextElement(reader, titleList, elementName);
                    }
                    case "Pagination" -> {
                        addPagination(reader, fields, elementName);
                    }
                    case "ELocationID" -> {
                        String eidType = reader.getAttributeValue(null, "EIdType");
                        String validYN = reader.getAttributeValue(null, "ValidYN");
                        reader.next();
                        if (isCharacterXMLEvent(reader) && "Y".equals(validYN)) {
                            handleElocationId(fields, reader, eidType);
                        }
                    }
                    case "Abstract" -> {
                        addAbstract(reader, fields, elementName);
                    }
                    case "AuthorList" -> {
                        handleAuthorList(reader, fields, elementName);
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("Article")) {
                break;
            }
        }

        if (!titleList.isEmpty()) {
            fields.put(StandardField.TITLE, StringUtil.stripBrackets(join(titleList, " ")));
        }
    }

    private void parseJournal(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "Title" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.JOURNAL, reader.getText());
                        }
                    }
                    case "ISSN" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.ISSN, reader.getText());
                        }
                    }
                    case "Volume" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.VOLUME, reader.getText());
                        }
                    }
                    case "Issue" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.ISSUE, reader.getText());
                        }
                    }
                    case "PubDate" -> {
                        addPubDate(reader, fields, elementName);
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("Journal")) {
                break;
            }
        }
    }

    private void parseDate(XMLStreamReader reader, Map<Field, String> fields, String startElement)
            throws XMLStreamException {
        Optional<String> year = Optional.empty();
        Optional<String> month = Optional.empty();
        Optional<String> day = Optional.empty();

        // mapping from date XML element to field name
        Map<String, String> dateFieldMap = Map.of(
                "DateCreated", "created",
                "DateCompleted", "completed",
                "DateRevised", "revised",
                "ContributionDate", "contribution",
                "PubDate", ""
        );

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "Year" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            year = Optional.of(reader.getText());
                        }
                    }
                    case "Month" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            month = Optional.of(reader.getText());
                        }
                    }
                    case "Day" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            day = Optional.of(reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        Optional<Date> date = Date.parse(year, month, day);
        date.ifPresent(dateValue ->
                fields.put(new UnknownField(dateFieldMap.get(startElement)), dateValue.getNormalized()));
    }

    private void addArticleIdList(Map<Field, String> fields, List<ArticleId> articleIdList) {
        for (ArticleId id : articleIdList) {
            if (!id.idType().isBlank()) {
                if ("pubmed".equals(id.idType())) {
                    fields.computeIfAbsent(StandardField.PMID, k -> id.content());
                } else {
                    fields.computeIfAbsent(FieldFactory.parseField(StandardEntryType.Article, id.idType()), k -> id.content());
                }
            }
        }
    }

    private void addNotes(Map<Field, String> fields, List<String> generalNoteList) {
        List<String> notes = new ArrayList<>();

        for (String note : generalNoteList) {
            if (!note.isBlank()) {
                notes.add(note);
            }
        }

        if (!notes.isEmpty()) {
            fields.put(StandardField.NOTE, join(notes, ", "));
        }
    }

    private void addInvestigators(Map<Field, String> fields, List<Investigator> investigatorList) {
        List<String> investigatorNames = new ArrayList<>();
        List<String> affiliationInfos = new ArrayList<>();

        // add the investigators like the authors
        if (!investigatorList.isEmpty()) {
            for (Investigator investigator : investigatorList) {
                StringBuilder result = new StringBuilder(investigator.lastName());
                if (!investigator.foreName().isBlank()) {
                    result.append(", ").append(investigator.foreName());
                }
                investigatorNames.add(result.toString());

                // now add the affiliation info
                if (!investigator.affiliationList().isEmpty()) {
                    affiliationInfos.addAll(investigator.affiliationList());
                }
            }

            if (!affiliationInfos.isEmpty()) {
                fields.put(new UnknownField("affiliation"), join(affiliationInfos, ", "));
            }

            fields.put(new UnknownField("investigator"), join(investigatorNames, " and "));
        }
    }

    private void addKeywords(Map<Field, String> fields, List<String> keywordList) {
        // Check whether MeshHeadingList exists or not
        if (fields.get(StandardField.KEYWORDS) == null) {
            fields.put(StandardField.KEYWORDS, join(keywordList, KEYWORD_SEPARATOR));
        } else {
            if (!keywordList.isEmpty()) {
                // if it exists, combine the MeshHeading with the keywords
                String result = join(keywordList, "; ");
                result = fields.get(StandardField.KEYWORDS) + KEYWORD_SEPARATOR + result;
                fields.put(StandardField.KEYWORDS, result);
            }
        }
    }

    private void addOtherId(Map<Field, String> fields, List<OtherId> otherIdList) {
        for (OtherId id : otherIdList) {
            if (!id.source().isBlank() && !id.content().isBlank()) {
                fields.put(FieldFactory.parseField(StandardEntryType.Article, id.source()), id.content());
            }
        }
    }

    private void addPersonalNames(Map<Field, String> fields, List<PersonalNameSubject> personalNameSubjectList) {
        if (fields.get(StandardField.AUTHOR) == null) {
            // if no authors appear, then add the personal names as authors
            List<String> personalNames = new ArrayList<>();

            if (!personalNameSubjectList.isEmpty()) {
                for (PersonalNameSubject personalNameSubject : personalNameSubjectList) {
                    StringBuilder result = new StringBuilder(personalNameSubject.lastName());
                    if (!personalNameSubject.foreName().isBlank()) {
                        result.append(", ").append(personalNameSubject.foreName());
                    }
                    personalNames.add(result.toString());
                }

                fields.put(StandardField.AUTHOR, join(personalNames, " and "));
            }
        }
    }

    private void addMeshHeading(Map<Field, String> fields, List<MeshHeading> meshHeadingList) {
        List<String> keywords = new ArrayList<>();

        if (!meshHeadingList.isEmpty()) {
            for (MeshHeading meshHeading : meshHeadingList) {
                StringBuilder result = new StringBuilder(meshHeading.descriptorName());
                if (meshHeading.qualifierNames() != null) {
                    for (String qualifierName : meshHeading.qualifierNames()) {
                        result.append(", ").append(qualifierName);
                    }
                }
                keywords.add(result.toString());
            }

            fields.put(StandardField.KEYWORDS, join(keywords, KEYWORD_SEPARATOR));
        }
    }

    private void addPubDate(XMLStreamReader reader, Map<Field, String> fields, String startElement) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "MedlineDate" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            fields.put(StandardField.YEAR, extractYear(reader.getText()));
                        }
                    }
                    case "Year" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            fields.put(StandardField.YEAR, reader.getText());
                        }
                    }
                    case "Month" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            Optional<Month> month = Month.parse(reader.getText());
                            month.ifPresent(monthValue -> fields.put(StandardField.MONTH, monthValue.getJabRefFormat()));
                        }
                    }
                    case "Season" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            fields.put(new UnknownField("season"), reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }
    }

    private void addAbstract(XMLStreamReader reader, Map<Field, String> fields, String startElement)
            throws XMLStreamException {
        List<String> abstractTextList = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "CopyrightInformation" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, new UnknownField("copyright"), reader.getText());
                        }
                    }
                    case "AbstractText" -> {
                        handleTextElement(reader, abstractTextList, elementName);
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        if (!abstractTextList.isEmpty()) {
            fields.put(StandardField.ABSTRACT, join(abstractTextList, " "));
        }
    }

    /**
     * Handles text entities that can have inner tags such as {@literal <}i{@literal >}, {@literal <}b{@literal >} etc.
     * We ignore the tags and return only the characters present in the enclosing parent element.
     *
     */
    private void handleTextElement(XMLStreamReader reader, List<String> textList, String startElement)
            throws XMLStreamException {
        StringBuilder result = new StringBuilder();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "sup", "sub" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            result.append("(").append(reader.getText()).append(")");
                        }
                    }
                }
            } else if (isCharacterXMLEvent(reader)) {
                result.append(reader.getText().trim()).append(" ");
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        textList.add(result.toString().trim());
    }

    private void addPagination(XMLStreamReader reader, Map<Field, String> fields, String startElement)
            throws XMLStreamException {
        String startPage = "";
        String endPage = "";

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "MedlinePgn" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.PAGES, fixPageRange(reader.getText()));
                        }
                    }
                    case "StartPage" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            // it could happen, that the article has only a start page
                            startPage = reader.getText() + endPage;
                            putIfValueNotNull(fields, StandardField.PAGES, startPage);
                        }
                    }
                    case "EndPage" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            endPage = reader.getText();
                            // but it should not happen, that a endpage appears without startpage
                            fields.put(StandardField.PAGES, fixPageRange(startPage + "-" + endPage));
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }
    }

    private String extractYear(String medlineDate) {
        // The year of the medlineDate should be the first 4 digits
        return medlineDate.substring(0, 4);
    }

    private void handleAuthorList(XMLStreamReader reader, Map<Field, String> fields, String startElement) throws XMLStreamException {
        List<String> authorNames = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "Author" -> {
                        parseAuthor(reader, authorNames);
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        fields.put(StandardField.AUTHOR, join(authorNames, " and "));
    }

    private void parseAuthor(XMLStreamReader reader, List<String> authorNames) throws XMLStreamException {
        StringBuilder authorName = new StringBuilder();
        List<String> collectiveNames = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "CollectiveName" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            collectiveNames.add(reader.getText());
                        }
                    }
                    case "LastName" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            authorName = new StringBuilder(reader.getText());
                        }
                    }
                    case "ForeName" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            authorName.append(", ").append(reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("Author")) {
                break;
            }
        }

        if (collectiveNames.size() > 0) {
            authorNames.addAll(collectiveNames);
        }
        if (!authorName.toString().isBlank()) {
            authorNames.add(authorName.toString());
        }
    }

    private void putIfValueNotNull(Map<Field, String> fields, Field field, String value) {
        if (value != null) {
            fields.put(field, value);
        }
    }

    /**
     * Convert medline page ranges from short form to full form. Medline reports page ranges in a shorthand format.
     * The last page is reported using only the digits which differ from the first page. i.e. 12345-51 refers to the actual range 12345-12351
     */
    private String fixPageRange(String pageRange) {
        int minusPos = pageRange.indexOf('-');
        if (minusPos < 0) {
            return pageRange;
        }
        String startPage = pageRange.substring(0, minusPos).trim();
        String endPage = pageRange.substring(minusPos + 1).trim();
        int lengthOfEndPage = endPage.length();
        int lengthOfStartPage = startPage.length();
        if (lengthOfEndPage < lengthOfStartPage) {
            endPage = startPage.substring(0, lengthOfStartPage - lengthOfEndPage) + endPage;
        }
        return startPage + "--" + endPage;
    }

    private boolean isCharacterXMLEvent(XMLStreamReader reader) {
        return reader.getEventType() == XMLEvent.CHARACTERS;
    }

    private boolean isStartXMLEvent(XMLStreamReader reader) {
        return reader.getEventType() == XMLEvent.START_ELEMENT;
    }

    private boolean isEndXMLEvent(XMLStreamReader reader) {
        return reader.getEventType() == XMLEvent.END_ELEMENT;
    }

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
        try {
            return importDatabase(
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))).getDatabase().getEntries();
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return Collections.emptyList();
    }
}
