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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.medline.Abstract;
import org.jabref.logic.importer.fileformat.medline.AbstractText;
import org.jabref.logic.importer.fileformat.medline.AffiliationInfo;
import org.jabref.logic.importer.fileformat.medline.ArticleId;
import org.jabref.logic.importer.fileformat.medline.ArticleIdList;
import org.jabref.logic.importer.fileformat.medline.ArticleTitle;
import org.jabref.logic.importer.fileformat.medline.Author;
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
import org.jabref.logic.importer.fileformat.medline.Keyword;
import org.jabref.logic.importer.fileformat.medline.KeywordList;
import org.jabref.logic.importer.fileformat.medline.MedlineCitation;
import org.jabref.logic.importer.fileformat.medline.MedlineJournalInfo;
import org.jabref.logic.importer.fileformat.medline.MeshHeading;
import org.jabref.logic.importer.fileformat.medline.MeshHeadingList;
import org.jabref.logic.importer.fileformat.medline.OtherID;
import org.jabref.logic.importer.fileformat.medline.Pagination;
import org.jabref.logic.importer.fileformat.medline.PersonalNameSubject;
import org.jabref.logic.importer.fileformat.medline.PersonalNameSubjectList;
import org.jabref.logic.importer.fileformat.medline.PubDate;
import org.jabref.logic.importer.fileformat.medline.PublicationType;
import org.jabref.logic.importer.fileformat.medline.Publisher;
import org.jabref.logic.importer.fileformat.medline.PubmedArticle;
import org.jabref.logic.importer.fileformat.medline.PubmedArticleSet;
import org.jabref.logic.importer.fileformat.medline.PubmedBookArticle;
import org.jabref.logic.importer.fileformat.medline.PubmedBookArticleSet;
import org.jabref.logic.importer.fileformat.medline.PubmedBookData;
import org.jabref.logic.importer.fileformat.medline.QualifierName;
import org.jabref.logic.importer.fileformat.medline.Section;
import org.jabref.logic.importer.fileformat.medline.Sections;
import org.jabref.logic.importer.fileformat.medline.Text;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
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
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);

        List<BibEntry> bibItems = new ArrayList<>();

        try {
            Object unmarshalledObject = unmarshallRoot(reader);

            // check whether we have an article set, an article, a book article or a book article set
            if (unmarshalledObject instanceof PubmedArticleSet) {
                PubmedArticleSet articleSet = (PubmedArticleSet) unmarshalledObject;
                for (Object article : articleSet.getPubmedArticleOrPubmedBookArticle()) {
                    if (article instanceof PubmedArticle) {
                        PubmedArticle currentArticle = (PubmedArticle) article;
                        parseArticle(currentArticle, bibItems);
                    }
                    if (article instanceof PubmedBookArticle) {
                        PubmedBookArticle currentArticle = (PubmedBookArticle) article;
                        parseBookArticle(currentArticle, bibItems);
                    }
                }
            } else if (unmarshalledObject instanceof PubmedArticle) {
                PubmedArticle article = (PubmedArticle) unmarshalledObject;
                parseArticle(article, bibItems);
            } else if (unmarshalledObject instanceof PubmedBookArticle) {
                PubmedBookArticle currentArticle = (PubmedBookArticle) unmarshalledObject;
                parseBookArticle(currentArticle, bibItems);
            } else {
                PubmedBookArticleSet bookArticleSet = (PubmedBookArticleSet) unmarshalledObject;
                for (PubmedBookArticle bookArticle : bookArticleSet.getPubmedBookArticle()) {
                    parseBookArticle(bookArticle, bibItems);
                }
            }
        } catch (JAXBException | XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }
        return new ParserResult(bibItems);
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
                addAbstract(fields, abs);
            }
            if (bookDocument.getPagination() != null) {
                Pagination pagination = bookDocument.getPagination();
                addPagination(fields, pagination);
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
                addKeyWords(fields, bookDocument.getKeywordList());
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
            addPubDate(fields, book.getPubDate());
        }
        if (book.getAuthorList() != null) {
            List<AuthorList> authorLists = book.getAuthorList();
            // authorLists size should be one
            if (authorLists.size() == 1) {
                for (AuthorList authorList : authorLists) {
                    handleAuthors(fields, authorList);
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
                addElocationID(fields, id);
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
                addMeashHeading(fields, medlineCitation.getMeshHeadingList());
            }
            putIfValueNotNull(fields, new UnknownField("references"), medlineCitation.getNumberOfReferences());
            if (medlineCitation.getPersonalNameSubjectList() != null) {
                addPersonalNames(fields, medlineCitation.getPersonalNameSubjectList());
            }
            if (medlineCitation.getOtherID() != null) {
                addOtherId(fields, medlineCitation.getOtherID());
            }
            if (medlineCitation.getKeywordList() != null) {
                addKeyWords(fields, medlineCitation.getKeywordList());
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
                    fields.put(FieldFactory.parseField(id.getIdType()), id.getContent());
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

    private void addKeyWords(Map<Field, String> fields, List<KeywordList> allKeywordLists) {
        List<String> keywordStrings = new ArrayList<>();
        // add keywords to the list
        for (KeywordList keywordList : allKeywordLists) {
            for (Keyword keyword : keywordList.getKeyword()) {
                for (Serializable content : keyword.getContent()) {
                    if (content instanceof String) {
                        keywordStrings.add((String) content);
                    }
                }
            }
        }
        // Check whether MeshHeadingList exist or not
        if (fields.get(StandardField.KEYWORDS) == null) {
            fields.put(StandardField.KEYWORDS, join(keywordStrings, KEYWORD_SEPARATOR));
        } else {
            if (keywordStrings.size() > 0) {
                // if it exists, combine the MeshHeading with the keywords
                String result = join(keywordStrings, "; ");
                result = fields.get(StandardField.KEYWORDS) + KEYWORD_SEPARATOR + result;
                fields.put(StandardField.KEYWORDS, result);
            }
        }
    }

    private void addOtherId(Map<Field, String> fields, List<OtherID> otherID) {
        for (OtherID id : otherID) {
            if ((id.getSource() != null) && (id.getContent() != null)) {
                fields.put(FieldFactory.parseField(id.getSource()), id.getContent());
            }
        }
    }

    private void addPersonalNames(Map<Field, String> fields, PersonalNameSubjectList personalNameSubjectList) {
        if (fields.get(StandardField.AUTHOR) == null) {
            // if no authors appear, then add the personal names as authors
            List<String> personalNames = new ArrayList<>();
            if (personalNameSubjectList.getPersonalNameSubject() != null) {
                List<PersonalNameSubject> personalNameSubject = personalNameSubjectList.getPersonalNameSubject();
                for (PersonalNameSubject personalName : personalNameSubject) {
                    String name = personalName.getLastName();
                    if (personalName.getForeName() != null) {
                        name += ", " + personalName.getForeName();
                    }
                    personalNames.add(name);
                }
                fields.put(StandardField.AUTHOR, join(personalNames, " and "));
            }
        }
    }

    private void addMeashHeading(Map<Field, String> fields, MeshHeadingList meshHeadingList) {
        ArrayList<String> keywords = new ArrayList<>();
        for (MeshHeading keyword : meshHeadingList.getMeshHeading()) {
            StringBuilder result = new StringBuilder(keyword.getDescriptorName().getContent());
            if (keyword.getQualifierName() != null) {
                for (QualifierName qualifier : keyword.getQualifierName()) {
                    result.append(", ").append(qualifier.getContent());
                }
            }
            keywords.add(result.toString());
        }
        fields.put(StandardField.KEYWORDS, join(keywords, KEYWORD_SEPARATOR));
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

                addPubDate(fields, journalIssue.getPubDate());
            } else if (object instanceof ArticleTitle) {
                ArticleTitle articleTitle = (ArticleTitle) object;
                fields.put(StandardField.TITLE, StringUtil.stripBrackets(articleTitle.getContent().toString()));
            } else if (object instanceof Pagination) {
                Pagination pagination = (Pagination) object;
                addPagination(fields, pagination);
            } else if (object instanceof ELocationID) {
                ELocationID eLocationID = (ELocationID) object;
                addElocationID(fields, eLocationID);
            } else if (object instanceof Abstract) {
                Abstract abs = (Abstract) object;
                addAbstract(fields, abs);
            } else if (object instanceof AuthorList) {
                AuthorList authors = (AuthorList) object;
                handleAuthors(fields, authors);
            }
        }
    }

    private void addElocationID(Map<Field, String> fields, ELocationID eLocationID) {
        if (eLocationID.getEIdType().equals("doi")) {
            fields.put(StandardField.DOI, eLocationID.getContent());
        }
        if (eLocationID.getEIdType().equals("pii")) {
            fields.put(new UnknownField("pii"), eLocationID.getContent());
        }
    }

    private void addPubDate(Map<Field, String> fields, PubDate pubDate) {
        if (pubDate.getYear() == null) {
            // if year of the pubdate is null, the medlineDate shouldn't be null
            fields.put(StandardField.YEAR, extractYear(pubDate.getMedlineDate()));
        } else {
            fields.put(StandardField.YEAR, pubDate.getYear());
            if (pubDate.getMonth() != null) {
                Optional<Month> month = Month.parse(pubDate.getMonth());
                if (month.isPresent()) {
                    fields.put(StandardField.MONTH, month.get().getJabRefFormat());
                }
            } else if (pubDate.getSeason() != null) {
                fields.put(new UnknownField("season"), pubDate.getSeason());
            }
        }
    }

    private void addAbstract(Map<Field, String> fields, Abstract abs) {
        putIfValueNotNull(fields, new UnknownField("copyright"), abs.getCopyrightInformation());
        List<String> abstractText = new ArrayList<>();
        for (AbstractText text : abs.getAbstractText()) {
            for (Serializable textContent : text.getContent()) {
                if (textContent instanceof String) {
                    abstractText.add((String) textContent);
                }
            }
        }
        fields.put(StandardField.ABSTRACT, join(abstractText, " "));
    }

    private void addPagination(Map<Field, String> fields, Pagination pagination) {
        String startPage = "";
        String endPage = "";
        for (JAXBElement<String> element : pagination.getContent()) {
            if ("MedlinePgn".equals(element.getName().getLocalPart())) {
                putIfValueNotNull(fields, StandardField.PAGES, fixPageRange(element.getValue()));
            } else if ("StartPage".equals(element.getName().getLocalPart())) {
                // it could happen, that the article has only a start page
                startPage = element.getValue() + endPage;
                putIfValueNotNull(fields, StandardField.PAGES, startPage);
            } else if ("EndPage".equals(element.getName().getLocalPart())) {
                endPage = element.getValue();
                // but it should not happen, that a endpage appears without startpage
                fields.put(StandardField.PAGES, fixPageRange(startPage + "-" + endPage));
            }
        }
    }

    private String extractYear(String medlineDate) {
        // The year of the medlineDate should be the first 4 digits
        return medlineDate.substring(0, 4);
    }

    private void handleAuthors(Map<Field, String> fields, AuthorList authors) {
        List<String> authorNames = new ArrayList<>();
        for (Author author : authors.getAuthor()) {
            if (author.getCollectiveName() != null) {
                Text collectiveNames = author.getCollectiveName();
                for (Serializable content : collectiveNames.getContent()) {
                    if (content instanceof String) {
                        authorNames.add((String) content);
                    }
                }
            } else {
                String authorName = author.getLastName();
                if (author.getForeName() != null) {
                    authorName += ", " + author.getForeName();
                }
                authorNames.add(authorName);
            }
        }
        fields.put(StandardField.AUTHOR, join(authorNames, " and "));
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
