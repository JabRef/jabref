package net.sf.jabref.logic.importer.fileformat;

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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.jabref.logic.importer.Importer;
import net.sf.jabref.logic.importer.ParseException;
import net.sf.jabref.logic.importer.Parser;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.medline.Abstract;
import net.sf.jabref.logic.importer.fileformat.medline.AbstractText;
import net.sf.jabref.logic.importer.fileformat.medline.AffiliationInfo;
import net.sf.jabref.logic.importer.fileformat.medline.ArticleId;
import net.sf.jabref.logic.importer.fileformat.medline.ArticleIdList;
import net.sf.jabref.logic.importer.fileformat.medline.ArticleTitle;
import net.sf.jabref.logic.importer.fileformat.medline.Author;
import net.sf.jabref.logic.importer.fileformat.medline.AuthorList;
import net.sf.jabref.logic.importer.fileformat.medline.Book;
import net.sf.jabref.logic.importer.fileformat.medline.BookDocument;
import net.sf.jabref.logic.importer.fileformat.medline.BookTitle;
import net.sf.jabref.logic.importer.fileformat.medline.Chemical;
import net.sf.jabref.logic.importer.fileformat.medline.ContributionDate;
import net.sf.jabref.logic.importer.fileformat.medline.DateCompleted;
import net.sf.jabref.logic.importer.fileformat.medline.DateCreated;
import net.sf.jabref.logic.importer.fileformat.medline.DateRevised;
import net.sf.jabref.logic.importer.fileformat.medline.ELocationID;
import net.sf.jabref.logic.importer.fileformat.medline.GeneSymbolList;
import net.sf.jabref.logic.importer.fileformat.medline.GeneralNote;
import net.sf.jabref.logic.importer.fileformat.medline.ISSN;
import net.sf.jabref.logic.importer.fileformat.medline.Investigator;
import net.sf.jabref.logic.importer.fileformat.medline.InvestigatorList;
import net.sf.jabref.logic.importer.fileformat.medline.Journal;
import net.sf.jabref.logic.importer.fileformat.medline.JournalIssue;
import net.sf.jabref.logic.importer.fileformat.medline.Keyword;
import net.sf.jabref.logic.importer.fileformat.medline.KeywordList;
import net.sf.jabref.logic.importer.fileformat.medline.MedlineCitation;
import net.sf.jabref.logic.importer.fileformat.medline.MedlineJournalInfo;
import net.sf.jabref.logic.importer.fileformat.medline.MeshHeading;
import net.sf.jabref.logic.importer.fileformat.medline.MeshHeadingList;
import net.sf.jabref.logic.importer.fileformat.medline.OtherID;
import net.sf.jabref.logic.importer.fileformat.medline.Pagination;
import net.sf.jabref.logic.importer.fileformat.medline.PersonalNameSubject;
import net.sf.jabref.logic.importer.fileformat.medline.PersonalNameSubjectList;
import net.sf.jabref.logic.importer.fileformat.medline.PubDate;
import net.sf.jabref.logic.importer.fileformat.medline.PublicationType;
import net.sf.jabref.logic.importer.fileformat.medline.Publisher;
import net.sf.jabref.logic.importer.fileformat.medline.PubmedArticle;
import net.sf.jabref.logic.importer.fileformat.medline.PubmedArticleSet;
import net.sf.jabref.logic.importer.fileformat.medline.PubmedBookArticle;
import net.sf.jabref.logic.importer.fileformat.medline.PubmedBookArticleSet;
import net.sf.jabref.logic.importer.fileformat.medline.PubmedBookData;
import net.sf.jabref.logic.importer.fileformat.medline.QualifierName;
import net.sf.jabref.logic.importer.fileformat.medline.Section;
import net.sf.jabref.logic.importer.fileformat.medline.Sections;
import net.sf.jabref.logic.importer.fileformat.medline.Text;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.strings.StringUtil;

import com.google.common.base.Joiner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Importer for the Medline/Pubmed format.
 * <p>
 * check here for details on the format
 * https://www.nlm.nih.gov/bsd/licensee/elements_descriptions.html
 */
public class MedlineImporter extends Importer implements Parser {

    private static final Log LOGGER = LogFactory.getLog(MedlineImporter.class);
    private static final String KEYWORD_SEPARATOR = "; ";

    private static final Locale ENGLISH = Locale.ENGLISH;


    @Override
    public String getName() {
        return "Medline";
    }

    @Override
    public FileExtensions getExtensions() {
        return FileExtensions.MEDLINE;
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
            JAXBContext context = JAXBContext.newInstance("net.sf.jabref.logic.importer.fileformat.medline");
            XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);

            //go to the root element
            while (!xmlStreamReader.isStartElement()) {
                xmlStreamReader.next();
            }

            Unmarshaller unmarshaller = context.createUnmarshaller();
            Object unmarshalledObject = unmarshaller.unmarshal(xmlStreamReader);

            //check whether we have an article set, an article, a book article or a book article set
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
            return ParserResult.fromErrorMessage(e.getLocalizedMessage());
        }
        return new ParserResult(bibItems);
    }

    private void parseBookArticle(PubmedBookArticle currentArticle, List<BibEntry> bibItems) {
        Map<String, String> fields = new HashMap<>();
        if (currentArticle.getBookDocument() != null) {
            BookDocument bookDocument = currentArticle.getBookDocument();
            fields.put(FieldName.PMID, bookDocument.getPMID().getContent());
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
                fields.put("sections", join(result, "; "));
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
                fields.put("pubtype", join(result, ", "));
            }
            if (bookDocument.getArticleTitle() != null) {
                ArticleTitle articleTitle = bookDocument.getArticleTitle();
                ArrayList<String> titles = new ArrayList<>();
                for (Serializable content : articleTitle.getContent()) {
                    if (content instanceof String) {
                        titles.add((String) content);
                    }
                }
                fields.put("article", join(titles, ", "));
            }
            if (bookDocument.getBook() != null) {
                addBookInformation(fields, bookDocument.getBook());
            }
        }

        if (currentArticle.getPubmedBookData() != null) {
            PubmedBookData bookData = currentArticle.getPubmedBookData();
            putIfValueNotNull(fields, "pubstatus", bookData.getPublicationStatus());
        }

        BibEntry entry = new BibEntry(IdGenerator.next(), "article");
        entry.setField(fields);

        bibItems.add(entry);
    }

    private void addBookInformation(Map<String, String> fields, Book book) {
        if (book.getPublisher() != null) {
            Publisher publisher = book.getPublisher();
            putIfValueNotNull(fields, "publocation", publisher.getPublisherLocation());
            putStringFromSerializableList(fields, FieldName.PUBLISHER, publisher.getPublisherName().getContent());
        }
        if (book.getBookTitle() != null) {
            BookTitle title = book.getBookTitle();
            putStringFromSerializableList(fields, FieldName.TITLE, title.getContent());
        }
        if (book.getPubDate() != null) {
            addPubDate(fields, book.getPubDate());
        }
        if (book.getAuthorList() != null) {
            List<AuthorList> authorLists = book.getAuthorList();
            //authorLists size should be one
            if (authorLists.size() == 1) {
                for (AuthorList authorList : authorLists) {
                    handleAuthors(fields, authorList);
                }
            } else {
                LOGGER.info(String.format("Size of authorlist was %s", authorLists.size()));
            }
        }

        putIfValueNotNull(fields, FieldName.VOLUME, book.getVolume());
        putIfValueNotNull(fields, FieldName.EDITION, book.getEdition());
        putIfValueNotNull(fields, "medium", book.getMedium());
        putIfValueNotNull(fields, "reportnumber", book.getReportNumber());

        if (book.getELocationID() != null) {
            for (ELocationID id : book.getELocationID()) {
                addElocationID(fields, id);
            }
        }
        if (book.getIsbn() != null) {
            fields.put(FieldName.ISBN, join(book.getIsbn(), ", "));
        }
    }

    private void putStringFromSerializableList(Map<String, String> fields, String medlineKey,
            List<Serializable> contentList) {
        StringBuilder result = new StringBuilder();
        for (Serializable content : contentList) {
            if (content instanceof String) {
                result.append((String) content);
            }
        }
        if (result.length() > 0) {
            fields.put(medlineKey, result.toString());
        }
    }

    private void addContributionDate(Map<String, String> fields, ContributionDate contributionDate) {
        if ((contributionDate.getDay() != null) && (contributionDate.getMonth() != null)
                && (contributionDate.getYear() != null)) {
            String result = convertToDateFormat(contributionDate.getYear(), contributionDate.getMonth(),
                    contributionDate.getDay());
            fields.put("contribution", result);
        }
    }

    private String convertToDateFormat(String year, String month, String day) {
        return String.format("%s-%s-%s", year, month, day);
    }

    private void parseArticle(PubmedArticle article, List<BibEntry> bibItems) {
        Map<String, String> fields = new HashMap<>();

        if (article.getPubmedData() != null) {
            if (article.getMedlineCitation().getDateRevised() != null) {
                DateRevised dateRevised = article.getMedlineCitation().getDateRevised();
                addDateRevised(fields, dateRevised);
                putIfValueNotNull(fields, "pubstatus", article.getPubmedData().getPublicationStatus());
                if (article.getPubmedData().getArticleIdList() != null) {
                    ArticleIdList articleIdList = article.getPubmedData().getArticleIdList();
                    addArticleIdList(fields, articleIdList);
                }
            }
        }
        if (article.getMedlineCitation() != null) {
            MedlineCitation medlineCitation = article.getMedlineCitation();

            fields.put("status", medlineCitation.getStatus());
            DateCreated dateCreated = medlineCitation.getDateCreated();
            fields.put("created",
                    convertToDateFormat(dateCreated.getYear(), dateCreated.getMonth(), dateCreated.getDay()));
            fields.put("pubmodel", medlineCitation.getArticle().getPubModel());

            if (medlineCitation.getDateCompleted() != null) {
                DateCompleted dateCompleted = medlineCitation.getDateCompleted();
                fields.put("completed",
                        convertToDateFormat(dateCompleted.getYear(), dateCompleted.getMonth(), dateCompleted.getDay()));
            }

            fields.put(FieldName.PMID, medlineCitation.getPMID().getContent());
            fields.put(FieldName.OWNER, medlineCitation.getOwner());

            addArticleInformation(fields, medlineCitation.getArticle().getContent());

            MedlineJournalInfo medlineJournalInfo = medlineCitation.getMedlineJournalInfo();
            putIfValueNotNull(fields, "country", medlineJournalInfo.getCountry());
            putIfValueNotNull(fields, "journal-abbreviation", medlineJournalInfo.getMedlineTA());
            putIfValueNotNull(fields, "nlm-id", medlineJournalInfo.getNlmUniqueID());
            putIfValueNotNull(fields, "issn-linking", medlineJournalInfo.getISSNLinking());
            if (medlineCitation.getChemicalList() != null) {
                if (medlineCitation.getChemicalList().getChemical() != null) {
                    addChemicals(fields, medlineCitation.getChemicalList().getChemical());
                }
            }
            if (medlineCitation.getCitationSubset() != null) {
                fields.put("citation-subset", join(medlineCitation.getCitationSubset(), ", "));
            }
            if (medlineCitation.getGeneSymbolList() != null) {
                addGeneSymbols(fields, medlineCitation.getGeneSymbolList());
            }
            if (medlineCitation.getMeshHeadingList() != null) {
                addMeashHeading(fields, medlineCitation.getMeshHeadingList());
            }
            putIfValueNotNull(fields, "references", medlineCitation.getNumberOfReferences());
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
                fields.put("space-flight-mission", join(medlineCitation.getSpaceFlightMission(), ", "));
            }
            if (medlineCitation.getInvestigatorList() != null) {
                addInvestigators(fields, medlineCitation.getInvestigatorList());
            }
            if (medlineCitation.getGeneralNote() != null) {
                addNotes(fields, medlineCitation.getGeneralNote());
            }
        }

        BibEntry entry = new BibEntry(IdGenerator.next(), "article");
        entry.setField(fields);

        bibItems.add(entry);
    }

    private void addArticleIdList(Map<String, String> fields, ArticleIdList articleIdList) {
        for (ArticleId id : articleIdList.getArticleId()) {
            if (id.getIdType() != null) {
                if ("pubmed".equals(id.getIdType())) {
                    fields.put("pmid", id.getContent());
                } else {
                    fields.put(id.getIdType(), id.getContent());
                }
            }
        }
    }

    private void addNotes(Map<String, String> fields, List<GeneralNote> generalNote) {
        List<String> notes = new ArrayList<>();
        for (GeneralNote note : generalNote) {
            if (note != null) {
                notes.add(note.getContent());
            }
        }
        fields.put(FieldName.NOTE, join(notes, ", "));
    }

    private void addInvestigators(Map<String, String> fields, InvestigatorList investigatorList) {
        List<String> investigatorNames = new ArrayList<>();
        List<String> affiliationInfos = new ArrayList<>();
        String name = "";
        // add the investigators like the authors
        if (investigatorList.getInvestigator() != null) {
            List<Investigator> investigators = investigatorList.getInvestigator();
            for (Investigator investigator : investigators) {
                name = investigator.getLastName();
                if (investigator.getForeName() != null) {
                    name += ", " + investigator.getForeName();
                }
                investigatorNames.add(name);

                //now add the affiliation info
                if (investigator.getAffiliationInfo() != null) {
                    for (AffiliationInfo info : investigator.getAffiliationInfo()) {
                        for (Serializable affiliation : info.getAffiliation().getContent()) {
                            if (affiliation instanceof String) {
                                affiliationInfos.add((String) affiliation);
                            }
                        }
                    }
                    fields.put("affiliation", join(affiliationInfos, ", "));
                }
            }
            fields.put("investigator", join(investigatorNames, " and "));
        }
    }

    private void addKeyWords(Map<String, String> fields, List<KeywordList> allKeywordLists) {
        List<String> keywordStrings = new ArrayList<>();
        //add keywords to the list
        for (KeywordList keywordList : allKeywordLists) {
            for (Keyword keyword : keywordList.getKeyword()) {
                for (Serializable content : keyword.getContent()) {
                    if (content instanceof String) {
                        keywordStrings.add((String) content);
                    }
                }
            }
        }
        //Check whether MeshHeadingList exist or not
        if (fields.get(FieldName.KEYWORDS) == null) {
            fields.put(FieldName.KEYWORDS, join(keywordStrings, KEYWORD_SEPARATOR));
        } else {
            if (keywordStrings.size() > 0) {
                //if it exists, combine the MeshHeading with the keywords
                String result = join(keywordStrings, "; ");
                result = fields.get(FieldName.KEYWORDS) + KEYWORD_SEPARATOR + result;
                fields.put(FieldName.KEYWORDS, result);
            }
        }
    }

    private void addOtherId(Map<String, String> fields, List<OtherID> otherID) {
        for (OtherID id : otherID) {
            if ((id.getSource() != null) && (id.getContent() != null)) {
                fields.put(id.getSource(), id.getContent());
            }
        }
    }

    private void addPersonalNames(Map<String, String> fields, PersonalNameSubjectList personalNameSubjectList) {
        if (fields.get(FieldName.AUTHOR) == null) {
            //if no authors appear, then add the personal names as authors
            List<String> personalNames = new ArrayList<>();
            if (personalNameSubjectList.getPersonalNameSubject() != null) {
                List<PersonalNameSubject> personalNameSubject = personalNameSubjectList.getPersonalNameSubject();
                for (PersonalNameSubject personalName : personalNameSubject) {
                    String name = personalName.getLastName();
                    if (personalName.getForeName() != null) {
                        name += ", " + personalName.getForeName().toString();
                    }
                    personalNames.add(name);
                }
                fields.put(FieldName.AUTHOR, join(personalNames, " and "));
            }
        }
    }

    private void addMeashHeading(Map<String, String> fields, MeshHeadingList meshHeadingList) {
        ArrayList<String> keywords = new ArrayList<>();
        for (MeshHeading keyword : meshHeadingList.getMeshHeading()) {
            String result = keyword.getDescriptorName().getContent();
            if (keyword.getQualifierName() != null) {
                for (QualifierName qualifier : keyword.getQualifierName()) {
                    result += ", " + qualifier.getContent();
                }
            }
            keywords.add(result);
        }
        fields.put(FieldName.KEYWORDS, join(keywords, KEYWORD_SEPARATOR));
    }

    private void addGeneSymbols(Map<String, String> fields, GeneSymbolList geneSymbolList) {
        List<String> geneSymbols = geneSymbolList.getGeneSymbol();
        fields.put("gene-symbols", join(geneSymbols, ", "));
    }

    private void addChemicals(Map<String, String> fields, List<Chemical> chemicals) {
        List<String> chemicalNames = new ArrayList<>();
        for (Chemical chemical : chemicals) {
            if (chemical != null) {
                chemicalNames.add(chemical.getNameOfSubstance().getContent());
            }
        }
        fields.put("chemicals", join(chemicalNames, ", "));
    }

    private void addArticleInformation(Map<String, String> fields, List<Object> content) {
        for (Object object : content) {
            if (object instanceof Journal) {
                Journal journal = (Journal) object;
                putIfValueNotNull(fields, FieldName.JOURNAL, journal.getTitle());

                ISSN issn = journal.getISSN();
                if (issn != null) {
                    putIfValueNotNull(fields, FieldName.ISSN, issn.getContent());
                }

                JournalIssue journalIssue = journal.getJournalIssue();
                putIfValueNotNull(fields, FieldName.VOLUME, journalIssue.getVolume());
                putIfValueNotNull(fields, FieldName.ISSUE, journalIssue.getIssue());

                addPubDate(fields, journalIssue.getPubDate());
            } else if (object instanceof ArticleTitle) {
                ArticleTitle articleTitle = (ArticleTitle) object;
                fields.put(FieldName.TITLE, StringUtil.stripBrackets(articleTitle.getContent().toString()));
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


    private void addElocationID(Map<String, String> fields, ELocationID eLocationID) {
        if (FieldName.DOI.equals(eLocationID.getEIdType())) {
            fields.put(FieldName.DOI, eLocationID.getContent());
        }
        if ("pii".equals(eLocationID.getEIdType())) {
            fields.put("pii", eLocationID.getContent());
        }
    }

    private void addPubDate(Map<String, String> fields, PubDate pubDate) {
        if (pubDate.getYear() == null) {
            //if year of the pubdate is null, the medlineDate shouldn't be null
            fields.put(FieldName.YEAR, extractYear(pubDate.getMedlineDate()));
        } else {
            fields.put(FieldName.YEAR, pubDate.getYear());
            if (pubDate.getMonth() != null) {
                fields.put(FieldName.MONTH, pubDate.getMonth());
            } else if (pubDate.getSeason() != null) {
                fields.put("season", pubDate.getSeason());
            }
        }
    }

    private void addAbstract(Map<String, String> fields, Abstract abs) {
        putIfValueNotNull(fields, "copyright", abs.getCopyrightInformation());
        List<String> abstractText = new ArrayList<>();
        for (AbstractText text : abs.getAbstractText()) {
            for (Serializable textContent : text.getContent()) {
                if (textContent instanceof String) {
                    abstractText.add((String) textContent);
                }
            }
        }
        fields.put(FieldName.ABSTRACT, join(abstractText, " "));
    }

    private void addPagination(Map<String, String> fields, Pagination pagination) {
        String startPage = "";
        String endPage = "";
        for (JAXBElement<String> element : pagination.getContent()) {
            if ("MedlinePgn".equals(element.getName().getLocalPart())) {
                putIfValueNotNull(fields, FieldName.PAGES, fixPageRange(element.getValue()));
            } else if ("StartPage".equals(element.getName().getLocalPart())) {
                //it could happen, that the article has only a start page
                startPage = element.getValue() + endPage;
                putIfValueNotNull(fields, FieldName.PAGES, startPage);
            } else if ("EndPage".equals(element.getName().getLocalPart())) {
                endPage = element.getValue();
                //but it should not happen, that a endpage appears without startpage
                fields.put(FieldName.PAGES, fixPageRange(startPage + "-" + endPage));
            }
        }
    }

    private String extractYear(String medlineDate) {
        //The year of the medlineDate should be the first 4 digits
        return medlineDate.substring(0, 4);
    }

    private void handleAuthors(Map<String, String> fields, AuthorList authors) {
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
        fields.put(FieldName.AUTHOR, join(authorNames, " and "));
    }

    private static String join(List<String> list, String string) {
        return Joiner.on(string).join(list);
    }

    private void addDateRevised(Map<String, String> fields, DateRevised dateRevised) {
        if ((dateRevised.getDay() != null) && (dateRevised.getMonth() != null) && (dateRevised.getYear() != null)) {
            fields.put("revised",
                    convertToDateFormat(dateRevised.getYear(), dateRevised.getMonth(), dateRevised.getDay()));
        }
    }

    private void putIfValueNotNull(Map<String, String> fields, String medlineKey, String value) {
        if (value != null) {
            fields.put(medlineKey, value);
        }
    }

    /**
     * Convert medline page ranges from short form to full form.
     * Medline reports page ranges in a shorthand format.
     * The last page is reported using only the digits which
     * differ from the first page.
     * i.e. 12345-51 refers to the actual range 12345-12351
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
