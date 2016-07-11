/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.medline.Abstract;
import net.sf.jabref.importer.fileformat.medline.AbstractText;
import net.sf.jabref.importer.fileformat.medline.AffiliationInfo;
import net.sf.jabref.importer.fileformat.medline.ArticleTitle;
import net.sf.jabref.importer.fileformat.medline.Author;
import net.sf.jabref.importer.fileformat.medline.AuthorList;
import net.sf.jabref.importer.fileformat.medline.Chemical;
import net.sf.jabref.importer.fileformat.medline.DateCompleted;
import net.sf.jabref.importer.fileformat.medline.DateCreated;
import net.sf.jabref.importer.fileformat.medline.DateRevised;
import net.sf.jabref.importer.fileformat.medline.ELocationID;
import net.sf.jabref.importer.fileformat.medline.GeneSymbolList;
import net.sf.jabref.importer.fileformat.medline.GeneralNote;
import net.sf.jabref.importer.fileformat.medline.ISSN;
import net.sf.jabref.importer.fileformat.medline.Investigator;
import net.sf.jabref.importer.fileformat.medline.InvestigatorList;
import net.sf.jabref.importer.fileformat.medline.Journal;
import net.sf.jabref.importer.fileformat.medline.JournalIssue;
import net.sf.jabref.importer.fileformat.medline.Keyword;
import net.sf.jabref.importer.fileformat.medline.KeywordList;
import net.sf.jabref.importer.fileformat.medline.MedlineCitation;
import net.sf.jabref.importer.fileformat.medline.MedlineJournalInfo;
import net.sf.jabref.importer.fileformat.medline.MeshHeading;
import net.sf.jabref.importer.fileformat.medline.MeshHeadingList;
import net.sf.jabref.importer.fileformat.medline.OtherID;
import net.sf.jabref.importer.fileformat.medline.Pagination;
import net.sf.jabref.importer.fileformat.medline.PersonalNameSubject;
import net.sf.jabref.importer.fileformat.medline.PersonalNameSubjectList;
import net.sf.jabref.importer.fileformat.medline.PubDate;
import net.sf.jabref.importer.fileformat.medline.PubmedArticle;
import net.sf.jabref.importer.fileformat.medline.PubmedArticleSet;
import net.sf.jabref.importer.fileformat.medline.QualifierName;
import net.sf.jabref.importer.fileformat.medline.Text;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.IdGenerator;

import com.google.common.base.Joiner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Importer for the Medline/Pubmed format.
 *
 * check here for details on the format
 * https://www.nlm.nih.gov/bsd/licensee/elements_descriptions.html
 */
public class MedlineImporter extends ImportFormat {

    private static final Log LOGGER = LogFactory.getLog(MedlineImporter.class);


    @Override
    public String getFormatName() {
        return "Medline";
    }

    @Override
    public List<String> getExtensions() {
        return Arrays.asList(".nbib", ".xml");
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

            if (str.toLowerCase().contains("<pubmedarticle>")) {
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
            JAXBContext context = JAXBContext.newInstance("generated");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Object unmarshalledObject = unmarshaller.unmarshal(reader);

            //check whether we have an article set or just an article
            if (unmarshalledObject instanceof PubmedArticleSet) {
                PubmedArticleSet articleSet = (PubmedArticleSet) unmarshalledObject;
                for (Object article : articleSet.getPubmedArticleOrPubmedBookArticle()) {
                    if (article instanceof PubmedArticle) {
                        PubmedArticle currentArticle = (PubmedArticle) article;
                        parseArticle(currentArticle, bibItems);
                    }
                }
            } else {
                PubmedArticle article = (PubmedArticle) unmarshalledObject;
                parseArticle(article, bibItems);
            }
        } catch (JAXBException e1) {
            LOGGER.debug("could not parse document", e1);
            return ParserResult.fromErrorMessage(e1.getLocalizedMessage());
        }

        return new ParserResult(bibItems);
    }

    private void parseArticle(PubmedArticle article, List<BibEntry> bibItems) {
        HashMap<String, String> fields = new HashMap<>();

        if (article.getPubmedData() != null) {
            addDateRevised(fields, article);
            putIfNotNull(fields, "pubstatus", article.getPubmedData().getPublicationStatus());
        }
        if (article.getMedlineCitation() != null) {
            MedlineCitation medlineCitation = article.getMedlineCitation();

            fields.put("status", medlineCitation.getStatus());
            DateCreated dateCreated = medlineCitation.getDateCreated();
            fields.put("created", dateCreated.getDay() + "/" + dateCreated.getMonth() + "/" + dateCreated.getYear());
            fields.put("pubmodel", medlineCitation.getArticle().getPubModel());

            if (medlineCitation.getDateCompleted() != null) {
                DateCompleted dateCompleted = medlineCitation.getDateCompleted();
                fields.put("completed",
                        dateCompleted.getDay() + "/" + dateCompleted.getMonth() + "/" + dateCompleted.getYear());
            }

            fields.put("pmid", medlineCitation.getPMID().getContent());
            fields.put("owner", medlineCitation.getOwner());

            addArticleInformation(fields, medlineCitation.getArticle().getContent());

            MedlineJournalInfo medlineJournalInfo = medlineCitation.getMedlineJournalInfo();
            putIfNotNull(fields, "country", medlineJournalInfo.getCountry());
            putIfNotNull(fields, "journal-abbreviation", medlineJournalInfo.getMedlineTA());
            putIfNotNull(fields, "nlm-id", medlineJournalInfo.getNlmUniqueID());
            putIfNotNull(fields, "issn-linking", medlineJournalInfo.getISSNLinking());
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
            putIfNotNull(fields, "references", medlineCitation.getNumberOfReferences());
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

        BibEntry entry = new BibEntry(IdGenerator.next(), "article"); // id assumes an existing database so don't create one here
        entry.setField(fields);

        bibItems.add(entry);
    }

    private void addNotes(HashMap<String, String> fields, List<GeneralNote> generalNote) {
        List<String> notes = new ArrayList<>();
        for (GeneralNote note : generalNote) {
            if (note != null) {
                notes.add(note.getContent());
            }
        }
        fields.put("note", join(notes, ", "));
    }

    private void addInvestigators(HashMap<String, String> fields, InvestigatorList investigatorList) {
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

    private void addKeyWords(HashMap<String, String> fields, List<KeywordList> allKeywordLists) {
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
        if (fields.get("keywords") == null) {
            fields.put("keywords", join(keywordStrings, "; "));
        } else {
            if (keywordStrings.size() > 0) {
                //if it exists, combine the MeshHeading with the keywords
                String result = join(keywordStrings, "; ");
                result = fields.get("keywords") + "; " + result;
                fields.put("keywords", result);
            }
        }
    }

    private void addOtherId(HashMap<String, String> fields, List<OtherID> otherID) {
        for (OtherID id : otherID) {
            if ((id.getSource() != null) && (id.getContent() != null)) {
                fields.put(id.getSource(), id.getContent());
            }
        }
    }

    private void addPersonalNames(HashMap<String, String> fields, PersonalNameSubjectList personalNameSubjectList) {
        if (fields.get("author") == null) {
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
                fields.put("author", join(personalNames, " and "));
            }
        }
    }

    private void addMeashHeading(HashMap<String, String> fields, MeshHeadingList meshHeadingList) {
        ArrayList<String> keywords = new ArrayList<>();
        String result = "";
        for (MeshHeading keyword : meshHeadingList.getMeshHeading()) {
            result = keyword.getDescriptorName().getContent();
            if (keyword.getQualifierName() != null) {
                for (QualifierName qualifier : keyword.getQualifierName()) {
                    result += ", " + qualifier.getContent();
                }
            }
            keywords.add(result);
        }
        fields.put("keywords", join(keywords, "; "));
    }

    private void addGeneSymbols(HashMap<String, String> fields, GeneSymbolList geneSymbolList) {
        List<String> geneSymbols = geneSymbolList.getGeneSymbol();
        fields.put("gene-symbols", join(geneSymbols, ", "));
    }

    private void addChemicals(HashMap<String, String> fields, List<Chemical> chemicals) {
        List<String> chemicalNames = new ArrayList<>();
        for (Chemical chemical : chemicals) {
            if (chemical != null) {
                chemicalNames.add(chemical.getNameOfSubstance().getContent());
            }
        }
        fields.put("chemicals", join(chemicalNames, ", "));
    }

    private void addArticleInformation(HashMap<String, String> fields, List<Object> content) {
        for (Object object : content) {
            if (object instanceof Journal) {
                Journal journal = (Journal) object;
                putIfNotNull(fields, "journal", journal.getTitle());

                ISSN issn = journal.getISSN();
                putIfNotNull(fields, "issn", issn.getContent());

                JournalIssue journalIssue = journal.getJournalIssue();
                putIfNotNull(fields, "volume", journalIssue.getVolume());
                putIfNotNull(fields, "issue", journalIssue.getIssue());

                PubDate pubDate = journalIssue.getPubDate();
                if (pubDate.getYear() == null) {
                    //if year of the pubdate is null, the medlineDate shouldn't be null
                    fields.put("year", extractYear(pubDate.getMedlineDate()));
                } else {
                    fields.put("year", pubDate.getYear());
                    if (pubDate.getMonth() != null) {
                        fields.put("month", pubDate.getMonth());
                    } else if (pubDate.getSeason() != null) {
                        fields.put("season", pubDate.getSeason());
                    }
                }
            } else if (object instanceof ArticleTitle) {
                ArticleTitle articleTitle = (ArticleTitle) object;
                fields.put("title", removeSurroundingBrackets(articleTitle.getContent().toString()));
            } else if (object instanceof Pagination) {
                Pagination pagination = (Pagination) object;
                String startPage = "";
                String endPage = "";
                for (JAXBElement<String> element : pagination.getContent()) {
                    if ("MedlinePgn".equals(element.getName().getLocalPart())) {
                        putIfNotNull(fields, "pages", fixPageRange(element.getValue()));
                    } else if ("StartPage".equals(element.getName().getLocalPart())) {
                        //it could happen, that the article has only a start page
                        startPage = element.getValue() + endPage;
                        putIfNotNull(fields, "pages", startPage);
                    } else if ("EndPage".equals(element.getName().getLocalPart())) {
                        endPage = element.getValue();
                        //but it should not happen, that a endpage appears without startpage
                        fields.put("pages", fixPageRange(startPage + "-" + endPage));
                    }
                }
            } else if (object instanceof ELocationID) {
                ELocationID eLocationID = (ELocationID) object;
                if ("doi".equals(eLocationID.getEIdType())) {
                    fields.put("doi", eLocationID.getContent());
                }
                if ("pii".equals(eLocationID.getEIdType())) {
                    fields.put("pii", eLocationID.getContent());
                }
            } else if (object instanceof Abstract) {
                Abstract abs = (Abstract) object;
                putIfNotNull(fields, "copyright", abs.getCopyrightInformation());
                List<String> abstractText = new ArrayList<>();
                for (AbstractText text : abs.getAbstractText()) {
                    for (Serializable textContent : text.getContent()) {
                        if (textContent instanceof String) {
                            abstractText.add((String) textContent);
                        }
                    }
                }
                fields.put("abstract", join(abstractText, " "));
            } else if (object instanceof AuthorList) {
                AuthorList authors = (AuthorList) object;
                handleAuthors(fields, authors);
            }
        }
    }

    private String extractYear(String medlineDate) {
        //The year of the medlineDate should be the first 4 digits
        return medlineDate.substring(0, 4);
    }

    private String removeSurroundingBrackets(String title) {
        if (title.startsWith("[") && title.endsWith("]")) {
            return title.replace("[", "").replace("]", "");
        }
        return title;
    }

    private void handleAuthors(HashMap<String, String> fields, AuthorList authors) {
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
        fields.put("author", join(authorNames, " and "));
    }

    private String join(List<String> list, String string) {
        return Joiner.on(string).join(list);
    }

    private void addDateRevised(HashMap<String, String> fields, PubmedArticle article) {
        if (article.getMedlineCitation().getDateRevised() != null) {
            DateRevised dateRevised = article.getMedlineCitation().getDateRevised();
            if ((dateRevised.getDay() != null) && (dateRevised.getMonth() != null) && (dateRevised.getYear() != null)) {
                fields.put("revised",
                        dateRevised.getDay() + "/" + dateRevised.getMonth() + "/" + dateRevised.getYear());
            }
        }
    }

    private void putIfNotNull(HashMap<String, String> fields, String medlineKey, String value) {
        if (value != null) {
            fields.put(medlineKey, value);
        }
    }

    // PENDING jeffrey.kuhn@yale.edu 2005-05-27 : added fixPageRange method
    //   Convert medline page ranges from short form to full form.
    //   Medline reports page ranges in a shorthand format.
    //   The last page is reported using only the digits which
    //   differ from the first page.
    //      i.e. 12345-51 refers to the actual range 12345-12351
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

}
