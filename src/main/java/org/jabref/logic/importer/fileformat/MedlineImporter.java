package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
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
import org.jabref.logic.importer.fileformat.medline.Abstract;
import org.jabref.logic.importer.fileformat.medline.AffiliationInfo;
import org.jabref.logic.importer.fileformat.medline.ArticleId;
import org.jabref.logic.importer.fileformat.medline.ArticleIdList;
import org.jabref.logic.importer.fileformat.medline.ArticleTitle;
import org.jabref.logic.importer.fileformat.medline.AuthorList;
import org.jabref.logic.importer.fileformat.medline.Book;
import org.jabref.logic.importer.fileformat.medline.BookDocument;
import org.jabref.logic.importer.fileformat.medline.BookTitle;
import org.jabref.logic.importer.fileformat.medline.Chemical;
import org.jabref.logic.importer.fileformat.medline.ContributionDate;
import org.jabref.logic.importer.fileformat.medline.DateCompleted;
import org.jabref.logic.importer.fileformat.medline.DateCreated;
import org.jabref.logic.importer.fileformat.medline.DateRevised;
import org.jabref.logic.importer.fileformat.medline.ELocationID;
import org.jabref.logic.importer.fileformat.medline.GeneSymbolList;
import org.jabref.logic.importer.fileformat.medline.GeneralNote;
import org.jabref.logic.importer.fileformat.medline.ISSN;
import org.jabref.logic.importer.fileformat.medline.Investigator;
import org.jabref.logic.importer.fileformat.medline.InvestigatorList;
import org.jabref.logic.importer.fileformat.medline.Journal;
import org.jabref.logic.importer.fileformat.medline.JournalIssue;
import org.jabref.logic.importer.fileformat.medline.MedlineCitation;
import org.jabref.logic.importer.fileformat.medline.MedlineJournalInfo;
import org.jabref.logic.importer.fileformat.medline.MeshHeadingRec;
import org.jabref.logic.importer.fileformat.medline.OtherIDRec;
import org.jabref.logic.importer.fileformat.medline.Pagination;
import org.jabref.logic.importer.fileformat.medline.PersonalNameSubjectRec;
import org.jabref.logic.importer.fileformat.medline.PublicationType;
import org.jabref.logic.importer.fileformat.medline.Publisher;
import org.jabref.logic.importer.fileformat.medline.PubmedArticle;
import org.jabref.logic.importer.fileformat.medline.PubmedBookArticle;
import org.jabref.logic.importer.fileformat.medline.PubmedBookData;
import org.jabref.logic.importer.fileformat.medline.Section;
import org.jabref.logic.importer.fileformat.medline.Sections;
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
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
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
    private Unmarshaller unmarshaller;

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

            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(input);

            while (reader.hasNext()) {
                reader.next();
                if (isStartXMLEvent(reader)) {
                    String elementName = reader.getName().getLocalPart();
                    switch (elementName) {
                        case "PubmedArticle" -> {
                            // Case 3: PubmedArticle
                            parseArticleNew(reader, bibItems, elementName);
                        }
                        // Case 1: PubmedArticleSet

                        // Case 2: PubmedBookArticleSet

                        // Case 4: PubmedBookArticle
                    }
                }
            }

//            Object unmarshalledObject = unmarshallRoot(reader);
//
//            // check whether we have an article set, an article, a book article or a book article set
//            if (unmarshalledObject instanceof PubmedArticleSet) {
//                PubmedArticleSet articleSet = (PubmedArticleSet) unmarshalledObject;
//                for (Object article : articleSet.getPubmedArticleOrPubmedBookArticle()) {
//                    if (article instanceof PubmedArticle) {
//                        PubmedArticle currentArticle = (PubmedArticle) article;
//                        parseArticle(currentArticle, bibItems);
//                    }
//                    if (article instanceof PubmedBookArticle) {
//                        PubmedBookArticle currentArticle = (PubmedBookArticle) article;
//                        parseBookArticle(currentArticle, bibItems);
//                    }
//                }
//            } else if (unmarshalledObject instanceof PubmedArticle) {
//                PubmedArticle article = (PubmedArticle) unmarshalledObject;
//                parseArticle(article, bibItems);
//            } else if (unmarshalledObject instanceof PubmedBookArticle) {
//                PubmedBookArticle currentArticle = (PubmedBookArticle) unmarshalledObject;
//                parseBookArticle(currentArticle, bibItems);
//            } else {
//                PubmedBookArticleSet bookArticleSet = (PubmedBookArticleSet) unmarshalledObject;
//                for (PubmedBookArticle bookArticle : bookArticleSet.getPubmedBookArticle()) {
//                    parseBookArticle(bookArticle, bibItems);
//                }
//            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }

        return new ParserResult(bibItems);
    }

    private void parseArticleNew(XMLStreamReader reader, List<BibEntry> bibItems, String parentElement) throws XMLStreamException {
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
                        //
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(parentElement)) {
                break;
            }
        }

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(fields);

        bibItems.add(entry);
    }

    private void parseMedlineCitation(XMLStreamReader reader, Map<Field, String> fields, String parentElement) throws XMLStreamException {
        // multiple occurrences of the following fields can be present
        List<String> citationSubsets = new ArrayList<>();
        List<MeshHeadingRec> meshHeadingList = new ArrayList<>();
        List<PersonalNameSubjectRec> personalNameSubjectList = new ArrayList<>();
        List<OtherIDRec> otherIDList = new ArrayList<>();
        List<String> keywordList = new ArrayList<>();
        List<String> spaceFlightMissionList = new ArrayList<>();

        String status = reader.getAttributeValue(null, "Status");
        String owner = reader.getAttributeValue(null, "Owner");
        fields.put(new UnknownField("status"), status);
        fields.put(StandardField.OWNER, owner);

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "DateCreated", "DateCompleted" -> {
                        parseDate(reader, elementName, fields);
                    }
                    case "Article" -> {
                        parseArticleInformation(reader, fields);
                    }
                    case "PMID" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            fields.put(StandardField.PMID, reader.getText());
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
                    case "GeneSymbol" -> {
                        parseGeneSymbolList(reader, fields, elementName);
                    }
                    case "MeshHeading" -> {
                        parseMeshHeading(reader, meshHeadingList, elementName);
                    }
                    case "NumberofReferences" -> {
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
                            otherIDList.add(new OtherIDRec(otherIdSource, content));
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
                    case "InvestigatorList" -> {
                        // TODO
                    }
                    case "GeneralNote" -> {
                        // TODO
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(parentElement)) {
                break;
            }
        }

        // populate multiple occurrence fields
        fields.put(new UnknownField("citation-subset"), join(citationSubsets, ", "));
        addMeshHeading(fields, meshHeadingList);
        addPersonalNames(fields, personalNameSubjectList);
        addOtherId(fields, otherIDList);
        addKeywords(fields, keywordList);
        fields.put(new UnknownField("space-flight-mission"), join(spaceFlightMissionList, ", "));
    }

    private void parsePersonalNameSubject(XMLStreamReader reader, List<PersonalNameSubjectRec> personalNameSubjectList, String startElement)
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

        personalNameSubjectList.add(new PersonalNameSubjectRec(lastName, foreName));
    }

    private void parseMeshHeading(XMLStreamReader reader, List<MeshHeadingRec> meshHeadingList, String startElement) throws XMLStreamException {
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

        meshHeadingList.add(new MeshHeadingRec(descriptorName, qualifierNames));
    }

    private void parseGeneSymbolList(XMLStreamReader reader, Map<Field, String> fields, String startElement) throws XMLStreamException {
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

        fields.put(new UnknownField("gene-symbols"), join(geneSymbols, ", "));
    }

    private void parseChemicalList(XMLStreamReader reader, Map<Field, String> fields, String startElement) throws XMLStreamException {
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

    private void parseMedlineJournalInfo(XMLStreamReader reader, Map<Field, String> fields, String startElement) throws XMLStreamException {
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
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            fields.put(StandardField.TITLE, StringUtil.stripBrackets(reader.getText()));
                        }
                    }
                    case "Pagination" -> {
                        addPagination(reader, fields);
                    }
                    case "ELocationID" -> {
                        String eidType = reader.getAttributeValue(null, "EIdType");
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            if (eidType.equals("doi")) {
                                fields.put(StandardField.DOI, reader.getText());
                            }
                            if (eidType.equals("pii")) {
                                fields.put(new UnknownField("pii"), reader.getText());
                            }
                        }
                    }
                    case "Abstract" -> {
                        addAbstract(reader, fields);
                    }
                    case "AuthorList" -> {
                        handleAuthorList(reader, fields);
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("Article")) {
                break;
            }
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
                        addPubDate(reader, fields);
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("Journal")) {
                break;
            }
        }
    }

    private void parseDate(XMLStreamReader reader, String parentElement, Map<Field, String> fields) throws XMLStreamException {
        Optional<String> year = Optional.empty();
        Optional<String> month = Optional.empty();
        Optional<String> day = Optional.empty();

        // mapping from date XML element to field name
        Map<String, String> dateFieldMap = Map.of(
                "DateCreated", "created",
                "DateCompleted", "completed"
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

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(parentElement)) {
                break;
            }
        }

        Optional<Date> date = Date.parse(year, month, day);
        date.ifPresent(dateValue ->
                fields.put(new UnknownField(dateFieldMap.get(parentElement)), dateValue.getNormalized()));
    }

    private Object unmarshallRoot(BufferedReader reader) throws JAXBException, XMLStreamException {
        initUmarshaller();

        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);

        // go to the root element
        while (!xmlStreamReader.isStartElement()) {
            xmlStreamReader.next();
        }

        return unmarshaller.unmarshal(xmlStreamReader);
    }

    private void initUmarshaller() throws JAXBException {
        if (unmarshaller == null) {
            // Lazy init because this is expensive
            JAXBContext context = JAXBContext.newInstance("org.jabref.logic.importer.fileformat.medline");
            unmarshaller = context.createUnmarshaller();
        }
    }

    private void parseBookArticle(PubmedBookArticle currentArticle, List<BibEntry> bibItems) {
        Map<Field, String> fields = new HashMap<>();
        if (currentArticle.getBookDocument() != null) {
            BookDocument bookDocument = currentArticle.getBookDocument();
            fields.put(StandardField.PMID, bookDocument.getPMID().getContent());
            if (bookDocument.getDateRevised() != null) {
                DateRevised dateRevised = bookDocument.getDateRevised();
                addDateRevised(fields, dateRevised);
            }
            if (bookDocument.getAbstract() != null) {
                Abstract abs = bookDocument.getAbstract();
                // addAbstract(fields, abs);
            }
            if (bookDocument.getPagination() != null) {
                Pagination pagination = bookDocument.getPagination();
                // addPagination(fields, pagination);
            }
            if (bookDocument.getSections() != null) {
                ArrayList<String> result = new ArrayList<>();
                Sections sections = bookDocument.getSections();
                for (Section section : sections.getSection()) {
                    for (Serializable content : section.getSectionTitle().getContent()) {
                        if (content instanceof String) {
                            result.add((String) content);
                        }
                    }
                }
                fields.put(new UnknownField("sections"), join(result, "; "));
            }
            if (bookDocument.getKeywordList() != null) {
//                addKeywords(fields, bookDocument.getKeywordList());
            }
            if (bookDocument.getContributionDate() != null) {
                addContributionDate(fields, bookDocument.getContributionDate());
            }
            if (bookDocument.getPublicationType() != null) {
                List<String> result = new ArrayList<>();
                for (PublicationType type : bookDocument.getPublicationType()) {
                    if (type.getContent() != null) {
                        result.add(type.getContent());
                    }
                }
                fields.put(new UnknownField("pubtype"), join(result, ", "));
            }
            if (bookDocument.getArticleTitle() != null) {
                ArticleTitle articleTitle = bookDocument.getArticleTitle();
                ArrayList<String> titles = new ArrayList<>();
                for (Serializable content : articleTitle.getContent()) {
                    if (content instanceof String) {
                        titles.add((String) content);
                    }
                }
                fields.put(new UnknownField("article"), join(titles, ", "));
            }
            if (bookDocument.getBook() != null) {
                addBookInformation(fields, bookDocument.getBook());
            }
        }

        if (currentArticle.getPubmedBookData() != null) {
            PubmedBookData bookData = currentArticle.getPubmedBookData();
            putIfValueNotNull(fields, StandardField.PUBSTATE, bookData.getPublicationStatus());
        }

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(fields);

        bibItems.add(entry);
    }

    private void addBookInformation(Map<Field, String> fields, Book book) {
        if (book.getPublisher() != null) {
            Publisher publisher = book.getPublisher();
            putIfValueNotNull(fields, new UnknownField("publocation"), publisher.getPublisherLocation());
            putStringFromSerializableList(fields, StandardField.PUBLISHER, publisher.getPublisherName().getContent());
        }
        if (book.getBookTitle() != null) {
            BookTitle title = book.getBookTitle();
            putStringFromSerializableList(fields, StandardField.TITLE, title.getContent());
        }
        if (book.getPubDate() != null) {
            // addPubDate(fields, book.getPubDate());
        }
        if (book.getAuthorList() != null) {
            List<AuthorList> authorLists = book.getAuthorList();
            // authorLists size should be one
            if (authorLists.size() == 1) {
                for (AuthorList authorList : authorLists) {
                    // handleAuthorList(fields, authorList);
                }
            } else {
                LOGGER.info(String.format("Size of authorlist was %s", authorLists.size()));
            }
        }

        putIfValueNotNull(fields, StandardField.VOLUME, book.getVolume());
        putIfValueNotNull(fields, StandardField.EDITION, book.getEdition());
        putIfValueNotNull(fields, new UnknownField("medium"), book.getMedium());
        putIfValueNotNull(fields, new UnknownField("reportnumber"), book.getReportNumber());

        if (book.getELocationID() != null) {
            for (ELocationID id : book.getELocationID()) {
//                addElocationID(fields, id);
            }
        }
        if (book.getIsbn() != null) {
            fields.put(StandardField.ISBN, join(book.getIsbn(), ", "));
        }
    }

    private void putStringFromSerializableList(Map<Field, String> fields, Field field, List<Serializable> contentList) {
        StringBuilder result = new StringBuilder();
        for (Serializable content : contentList) {
            if (content instanceof String) {
                result.append((String) content);
            }
        }
        if (result.length() > 0) {
            fields.put(field, result.toString());
        }
    }

    private void addContributionDate(Map<Field, String> fields, ContributionDate contributionDate) {
        if ((contributionDate.getDay() != null) && (contributionDate.getMonth() != null)
                && (contributionDate.getYear() != null)) {
            String result = convertToDateFormat(contributionDate.getYear(), contributionDate.getMonth(),
                    contributionDate.getDay());
            fields.put(new UnknownField("contribution"), result);
        }
    }

    private String convertToDateFormat(String year, String month, String day) {
        return String.format("%s-%s-%s", year, month, day);
    }

    private void parseArticle(PubmedArticle article, List<BibEntry> bibItems) {
        Map<Field, String> fields = new HashMap<>();

        if (article.getPubmedData() != null) {
            if (article.getMedlineCitation().getDateRevised() != null) {
                DateRevised dateRevised = article.getMedlineCitation().getDateRevised();
                addDateRevised(fields, dateRevised);
                putIfValueNotNull(fields, StandardField.PUBSTATE, article.getPubmedData().getPublicationStatus());
                if (article.getPubmedData().getArticleIdList() != null) {
                    ArticleIdList articleIdList = article.getPubmedData().getArticleIdList();
                    addArticleIdList(fields, articleIdList);
                }
            }
        }
        if (article.getMedlineCitation() != null) {
            MedlineCitation medlineCitation = article.getMedlineCitation();

            fields.put(new UnknownField("status"), medlineCitation.getStatus());
            DateCreated dateCreated = medlineCitation.getDateCreated();
            if (medlineCitation.getDateCreated() != null) {
                fields.put(new UnknownField("created"),
                        convertToDateFormat(dateCreated.getYear(), dateCreated.getMonth(), dateCreated.getDay()));
            }
            fields.put(new UnknownField("pubmodel"), medlineCitation.getArticle().getPubModel());

            if (medlineCitation.getDateCompleted() != null) {
                DateCompleted dateCompleted = medlineCitation.getDateCompleted();
                fields.put(new UnknownField("completed"),
                        convertToDateFormat(dateCompleted.getYear(), dateCompleted.getMonth(), dateCompleted.getDay()));
            }

            fields.put(StandardField.PMID, medlineCitation.getPMID().getContent());
            fields.put(StandardField.OWNER, medlineCitation.getOwner());

            addArticleInformation(fields, medlineCitation.getArticle().getContent());

            MedlineJournalInfo medlineJournalInfo = medlineCitation.getMedlineJournalInfo();
            putIfValueNotNull(fields, new UnknownField("country"), medlineJournalInfo.getCountry());
            putIfValueNotNull(fields, new UnknownField("journal-abbreviation"), medlineJournalInfo.getMedlineTA());
            putIfValueNotNull(fields, new UnknownField("nlm-id"), medlineJournalInfo.getNlmUniqueID());
            putIfValueNotNull(fields, new UnknownField("issn-linking"), medlineJournalInfo.getISSNLinking());
            if (medlineCitation.getChemicalList() != null) {
                if (medlineCitation.getChemicalList().getChemical() != null) {
                    addChemicals(fields, medlineCitation.getChemicalList().getChemical());
                }
            }
            if (medlineCitation.getCitationSubset() != null) {
                fields.put(new UnknownField("citation-subset"), join(medlineCitation.getCitationSubset(), ", "));
            }
            if (medlineCitation.getGeneSymbolList() != null) {
                addGeneSymbols(fields, medlineCitation.getGeneSymbolList());
            }
            if (medlineCitation.getMeshHeadingList() != null) {
                // addMeshHeading(fields, medlineCitation.getMeshHeadingList());
            }
            putIfValueNotNull(fields, new UnknownField("references"), medlineCitation.getNumberOfReferences());
            if (medlineCitation.getPersonalNameSubjectList() != null) {
//                addPersonalNames(fields, medlineCitation.getPersonalNameSubjectList());
            }
            if (medlineCitation.getOtherID() != null) {
//                addOtherId(fields, medlineCitation.getOtherID());
            }
            if (medlineCitation.getKeywordList() != null) {
//                addKeywords(fields, medlineCitation.getKeywordList());
            }
            if (medlineCitation.getSpaceFlightMission() != null) {
                fields.put(new UnknownField("space-flight-mission"), join(medlineCitation.getSpaceFlightMission(), ", "));
            }
            if (medlineCitation.getInvestigatorList() != null) {
                addInvestigators(fields, medlineCitation.getInvestigatorList());
            }
            if (medlineCitation.getGeneralNote() != null) {
                addNotes(fields, medlineCitation.getGeneralNote());
            }
        }

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(fields);

        bibItems.add(entry);
    }

    private void addArticleIdList(Map<Field, String> fields, ArticleIdList articleIdList) {
        for (ArticleId id : articleIdList.getArticleId()) {
            if (id.getIdType() != null) {
                if ("pubmed".equals(id.getIdType())) {
                    fields.put(StandardField.PMID, id.getContent());
                } else {
                    fields.put(FieldFactory.parseField(StandardEntryType.Article, id.getIdType()), id.getContent());
                }
            }
        }
    }

    private void addNotes(Map<Field, String> fields, List<GeneralNote> generalNote) {
        List<String> notes = new ArrayList<>();
        for (GeneralNote note : generalNote) {
            if (note != null) {
                notes.add(note.getContent());
            }
        }
        fields.put(StandardField.NOTE, join(notes, ", "));
    }

    private void addInvestigators(Map<Field, String> fields, InvestigatorList investigatorList) {
        List<String> investigatorNames = new ArrayList<>();
        List<String> affiliationInfos = new ArrayList<>();
        String name;
        // add the investigators like the authors
        if (investigatorList.getInvestigator() != null) {
            List<Investigator> investigators = investigatorList.getInvestigator();
            for (Investigator investigator : investigators) {
                name = investigator.getLastName();
                if (investigator.getForeName() != null) {
                    name += ", " + investigator.getForeName();
                }
                investigatorNames.add(name);

                // now add the affiliation info
                if (investigator.getAffiliationInfo() != null) {
                    for (AffiliationInfo info : investigator.getAffiliationInfo()) {
                        for (Serializable affiliation : info.getAffiliation().getContent()) {
                            if (affiliation instanceof String) {
                                affiliationInfos.add((String) affiliation);
                            }
                        }
                    }
                    fields.put(new UnknownField("affiliation"), join(affiliationInfos, ", "));
                }
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

    private void addOtherId(Map<Field, String> fields, List<OtherIDRec> otherIDList) {
        for (OtherIDRec id : otherIDList) {
            if (!id.source().isBlank() && !id.content().isBlank()) {
                fields.put(FieldFactory.parseField(StandardEntryType.Article, id.source()), id.content());
            }
        }
    }

    private void addPersonalNames(Map<Field, String> fields, List<PersonalNameSubjectRec> personalNameSubjectList) {
        if (fields.get(StandardField.AUTHOR) == null) {
            // if no authors appear, then add the personal names as authors
            List<String> personalNames = new ArrayList<>();

            if (!personalNameSubjectList.isEmpty()) {
                for (PersonalNameSubjectRec personalNameSubject : personalNameSubjectList) {
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

    private void addMeshHeading(Map<Field, String> fields, List<MeshHeadingRec> meshHeadingList) {
        List<String> keywords = new ArrayList<>();

        if (!meshHeadingList.isEmpty()) {
            for (MeshHeadingRec meshHeading : meshHeadingList) {
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

    private void addGeneSymbols(Map<Field, String> fields, GeneSymbolList geneSymbolList) {
        List<String> geneSymbols = geneSymbolList.getGeneSymbol();
        fields.put(new UnknownField("gene-symbols"), join(geneSymbols, ", "));
    }

    private void addChemicals(Map<Field, String> fields, List<Chemical> chemicals) {
        List<String> chemicalNames = new ArrayList<>();
        for (Chemical chemical : chemicals) {
            if (chemical != null) {
                chemicalNames.add(chemical.getNameOfSubstance().getContent());
            }
        }
        fields.put(new UnknownField("chemicals"), join(chemicalNames, ", "));
    }

    private void addArticleInformation(Map<Field, String> fields, List<Object> content) {
        for (Object object : content) {
            if (object instanceof Journal) {
                Journal journal = (Journal) object;
                putIfValueNotNull(fields, StandardField.JOURNAL, journal.getTitle());

                ISSN issn = journal.getISSN();
                if (issn != null) {
                    putIfValueNotNull(fields, StandardField.ISSN, issn.getContent());
                }

                JournalIssue journalIssue = journal.getJournalIssue();
                putIfValueNotNull(fields, StandardField.VOLUME, journalIssue.getVolume());
                putIfValueNotNull(fields, StandardField.ISSUE, journalIssue.getIssue());

                // addPubDate(fields, journalIssue.getPubDate());
            } else if (object instanceof ArticleTitle) {
                ArticleTitle articleTitle = (ArticleTitle) object;
                fields.put(StandardField.TITLE, StringUtil.stripBrackets(articleTitle.getContent().toString()));
            } else if (object instanceof Pagination) {
                Pagination pagination = (Pagination) object;
                // addPagination(fields, pagination);
            } else if (object instanceof ELocationID) {
                ELocationID eLocationID = (ELocationID) object;
//                addElocationID(fields, eLocationID);
            } else if (object instanceof Abstract) {
                Abstract abs = (Abstract) object;
                // addAbstract(fields, abs);
            } else if (object instanceof AuthorList) {
                AuthorList authors = (AuthorList) object;
//                handleAuthorList(fields, authors);
            }
        }
    }

    private void addPubDate(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
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

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("PubDate")) {
                break;
            }
        }
    }

    private void addAbstract(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        List<String> abstractText = new ArrayList<>();

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
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            abstractText.add(reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("Abstract")) {
                break;
            }
        }

        fields.put(StandardField.ABSTRACT, join(abstractText, " "));
    }

    private void addPagination(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
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

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("Pagination")) {
                break;
            }
        }
    }

    private String extractYear(String medlineDate) {
        // The year of the medlineDate should be the first 4 digits
        return medlineDate.substring(0, 4);
    }

    private void handleAuthorList(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
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

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("AuthorList")) {
                break;
            }
        }

        fields.put(StandardField.AUTHOR, join(authorNames, " and "));
    }

    private void parseAuthor(XMLStreamReader reader, List<String> authorNames) throws XMLStreamException {
        String authorName = "";
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
                            authorName = reader.getText();
                        }
                    }
                    case "ForeName" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            authorName += ", " + reader.getText();
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
        if (!authorName.isBlank()) {
            authorNames.add(authorName);
        }
    }

    private void addDateRevised(Map<Field, String> fields, DateRevised dateRevised) {
        if ((dateRevised.getDay() != null) && (dateRevised.getMonth() != null) && (dateRevised.getYear() != null)) {
            fields.put(new UnknownField("revised"),
                    convertToDateFormat(dateRevised.getYear(), dateRevised.getMonth(), dateRevised.getDay()));
        }
    }

    private void putIfValueNotNull(Map<Field, String> fields, Field field, String value) {
        if (value != null) {
            fields.put(field, value);
        }
    }

    /**
     * Convert medline page ranges from short form to full form. Medline reports page ranges in a shorthand format. The last page is reported using only the digits which differ from the first page. i.e. 12345-51 refers to the actual range 12345-12351
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
