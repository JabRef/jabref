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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.sf.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.IdGenerator;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

class MedlineHandler extends DefaultHandler {

    private static final UnicodeToLatexFormatter UNICODE_CONVERTER = new UnicodeToLatexFormatter();
    private final List<BibEntry> bibitems = new ArrayList<>();
    private boolean inTitle;
    private boolean inYear;
    private boolean inJournal;
    private boolean inMonth;
    private boolean inVolume;
    private boolean inAuthorList;
    private boolean inAuthor;
    private boolean inLastName;
    private boolean inSuffix;
    private boolean inInitials;
    private boolean inMedlinePgn;
    private boolean inIssue;
    private boolean inPubDate;
    private boolean inUrl;
    private boolean inForename;
    private boolean inAbstractText;
    private boolean inMedlineDate;
    private boolean inPubMedID;
    private boolean inDescriptorName;
    private boolean inDoi;
    private boolean inPii;
    private boolean inPmc;
    private boolean inAffiliation;
    private boolean inMeshHeader;
    private boolean inQualifierName;
    private boolean inLanguage;
    private boolean inPst;
    private String title = "";
    private String journal = "";
    private String author = "";
    private String lastName = "";
    private String suffix = "";
    private String year = "";
    private String forename = "";
    private String abstractText = "";
    private String affiliation = "";
    private String month = "";
    private String volume = "";
    private String lastname = "";
    private String initials = "";
    private String number = "";
    private String page = "";
    private String medlineDate = "";
    private final String series = "";
    private final String editor = "";
    private final String booktitle = "";
    private final String type = "article";
    private final String key = "";
    private final String address = "";
    private String pubmedid = "";
    private String doi = "";
    private String pii = "";
    private String pmc = "";
    private String majorTopic = "";
    private String minorTopics = "";
    private String language = "";
    private String pst = "";
    private final List<String> authors = new ArrayList<>();
    private final Set<String> descriptors = new TreeSet<>(); // To gather keywords

    private static final String KEYWORD_SEPARATOR = "; ";


    public List<BibEntry> getItems() {
        return bibitems;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
        // Get the number of attribute
        if ("PubmedArticle".equals(localName)) {
            // Do nothing
        } else if ("ArticleTitle".equals(localName)) {
            inTitle = true;
            title = "";
        } else if ("PubDate".equals(localName)) {
            inPubDate = true;
        } else if ("Year".equals(localName) && inPubDate) {
            inYear = true;
        } else if ("MedlineDate".equals(localName) && inPubDate) {
            inMedlineDate = true;
        } // medline date does not have 4 digit dates instead it has multiyear etc
        else if ("MedlineTA".equals(localName)) {
            inJournal = true;
            journal = "";
        } //journal name
        else if ("Month".equals(localName) && inPubDate) {
            inMonth = true;
        } else if ("Volume".equals(localName)) {
            inVolume = true;
        } else if ("Language".equals(localName)) {
            inLanguage = true;
        } else if ("PublicationStatus".equals(localName)) {
            inPst = true;
        } else if ("AuthorList".equals(localName)) {
            inAuthorList = true;
            authors.clear();
        } else if ("MeshHeading".equals(localName)) {
            inMeshHeader = true;
            majorTopic = "";
            minorTopics = "";
        } else if ("DescriptorName".equals(localName)) {
            inDescriptorName = true;
        } else if ("QualifierName".equals(localName)) {
            inQualifierName = true;
        } else if ("Author".equals(localName)) {
            inAuthor = true;
            author = "";
        } else if ("CollectiveName".equals(localName)) {
            inForename = true;
            forename = "";
        } // Morten A. 20040513.
        else if ("PMID".equals(localName)) {
            // Set PMID only once, because there can be <CommentIn> tags later on that
            // contain IDs of different articles.
            if (pubmedid.isEmpty()) {
                inPubMedID = true;
                pubmedid = "";
            }
        } else if ("LastName".equals(localName)) {
            inLastName = true;
            lastName = "";
        } else if ("ForeName".equals(localName) || "FirstName".equals(localName)) {
            inForename = true;
            forename = "";
        } else if ("Suffix".equals(localName)) {
            inSuffix = true;
            suffix = "";
        } else if ("Issue".equals(localName)) {
            inIssue = true;
        } else if ("MedlinePgn".equals(localName)) {
            inMedlinePgn = true;
        } //pagenumber
        else if ("URL".equals(localName)) {
            inUrl = true;
        } else if ("Initials".equals(localName)) {
            inInitials = true;
        } else if ("AbstractText".equals(localName)) {
            inAbstractText = true;
        } else if ("ArticleId".equals(localName)) {
            for (int i = 0; i < atts.getLength(); i++) {
                String value = atts.getValue(i);
                if ("doi".equals(value)) {
                    inDoi = true;
                } else if ("pii".equals(value)) {
                    inPii = true;
                } else if ("pmc".equals(value)) {
                    inPmc = true;
                }

            }
        } else if ("Affiliation".equals(localName)) {
            inAffiliation = true;
        }

    }

    private String join(Object[] sa, String delim) {
        StringBuilder sb = new StringBuilder();
        sb.append(sa[0]);
        for (int i = 1; i < sa.length; i++)
        {
            sb.append(delim);
            sb.append(sa[i]);
        }
        return sb.toString();
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if ("PubmedArticle".equals(localName)) {
            //bibitems.add( new Bibitem(null, makeBibtexString(), Globals.nextKey(),"-1" )	 );
            // check if year ="" then give medline date instead
            if ("".equals(year) && !"".equals(medlineDate)) {
                    // multi-year date format
                    //System.out.println(MedlineDate);
                    year = medlineDate.substring(0, 4);
                    //Matcher m = Pattern.compile("\\b[0-9]{4}\\b").matcher(MedlineDate);
                    //if(m.matches())
                    //year = m.group();
            }

            // Build a string from the collected keywords:
            StringBuilder sb = new StringBuilder();
            for (Iterator<String> iterator = descriptors.iterator(); iterator.hasNext();) {
                String s = iterator.next();
                sb.append(s);
                if (iterator.hasNext()) {
                    sb.append(MedlineHandler.KEYWORD_SEPARATOR);
                }
            }
            String keywords = sb.toString();

            BibEntry b = new BibEntry(IdGenerator.next(), "article"); // id assumes an existing database so don't create one here
            if (!"".equals(author)) {
                b.setField("author",
                        MedlineHandler.UNICODE_CONVERTER.format(StringUtil.expandAuthorInitials(author)));
                // b.setField("author",Util.replaceSpecialCharacters(ImportFormatReader.expandAuthorInitials(author)));
                author = "";
            }
            if (!"".equals(title)) {
                b.setField("title", MedlineHandler.UNICODE_CONVERTER.format(title));
            }
            // if (!title.equals("")) b.setField("title",Util.replaceSpecialCharacters(title));
            if (!"".equals(journal)) {
                b.setField("journal", journal);
            }
            if (!"".equals(year)) {
                b.setField("year", year);
            }
            // PENDING jeffrey.kuhn@yale.edu 2005-05-27 : added call to fixPageRange
            if (!"".equals(page)) {
                b.setField("pages", fixPageRange(page));
            }
            if (!"".equals(volume)) {
                b.setField("volume", volume);
            }
            if (!"".equals(language)) {
                b.setField("language", language);
            }
            if (!"".equals(pst)) {
                b.setField("medline-pst", pst);
            }
            if (!"".equals(abstractText)) {
                b.setField("abstract", abstractText.replace("%", "\\%"));
            }
            if (!"".equals(keywords)) {
                b.setField("keywords", keywords);
            }
            if (!"".equals(month)) {
                b.setField("month", month);
            }
            //if (!url.equals("")) b.setField("url",url);
            if (!"".equals(number)) {
                b.setField("number", number);
            }

            if (!"".equals(doi)) {
                b.setField("doi", doi);
                b.setField("url", "http://dx.doi.org/" + doi);
            }
            if (!"".equals(pii)) {
                b.setField("pii", pii);
            }
            if (!"".equals(pmc)) {
                b.setField("pmc", pmc);
            }
            if (!"".equals(affiliation)) {
                b.setField("institution", affiliation.replace("#", "\\#"));
            }

            // PENDING jeffrey.kuhn@yale.edu 2005-05-27 : added "pmid" bibtex field
            // Older references do not have doi entries, but every
            // medline entry has a unique pubmed ID (aka primary ID).
            // Add a bibtex field for the pubmed ID for future use.
            if (!"".equals(pubmedid)) {
                b.setField("pmid", pubmedid);
            }

            bibitems.add(b);

            abstractText = "";
            author = "";
            title = "";
            journal = "";
            keywords = "";
            doi = "";
            pii = "";
            pmc = "";
            year = "";
            forename = "";
            lastName = "";
            suffix = "";
            abstractText = "";
            affiliation = "";
            pubmedid = "";
            majorTopic = "";
            minorTopics = "";
            month = "";
            volume = "";
            language = "";
            pst = "";
            lastname = "";
            suffix = "";
            initials = "";
            number = "";
            page = "";
            String medlineID = "";
            String url = "";
            medlineDate = "";
            descriptors.clear();
        }

        else if ("ArticleTitle".equals(localName)) {
            inTitle = false;
        }
        else if ("PubDate".equals(localName)) {
            inPubDate = false;
        }
        else if ("Year".equals(localName)) {
            inYear = false;
        }
        else if ("PMID".equals(localName)) {
            inPubMedID = false;
        }
        else if ("MedlineDate".equals(localName)) {
            inMedlineDate = false;
        }
        else if ("MedlineTA".equals(localName)) {
            inJournal = false;
        } //journal name
        else if ("Month".equals(localName)) {
            inMonth = false;
        }
        else if ("Volume".equals(localName)) {
            inVolume = false;
        }
        else if ("Language".equals(localName)) {
            inLanguage = false;
        }
        else if ("PublicationStatus".equals(localName)) {
            inPst = false;
        }
        else if ("AuthorList".equals(localName)) {
            author = join(authors.toArray(), " and ");
            inAuthorList = false;
        }
        else if ("Author".equals(localName)) {
            // forename sometimes has initials with " " in middle: is pattern [A-Z] [A-Z]
            // when above is the case replace it with initials
            if ((forename.length() == 3) && (forename.charAt(1) == ' ')) {
                forename = initials;
            }

            // Put together name with last name first, and enter suffix in between if present:
            if (lastname.indexOf(' ') > 0) {
                author = "{" + lastname + "}";
            } else {
                author = lastname;
            }

            if (!suffix.isEmpty()) {
                author = author + ", " + suffix;
            }
            if (!forename.isEmpty()) {
                author = author + ", " + forename;
            }

            //author = initials + " " + lastname;
            authors.add(author);
            inAuthor = false;
            forename = "";
            initials = "";
            lastname = "";
            suffix = "";
        }
        else if ("DescriptorName".equals(localName)) {
            inDescriptorName = false;
        } else if ("QualifierName".equals(localName)) {
            inQualifierName = false;
        } else if ("MeshHeading".equals(localName)) {
            inMeshHeader = false;
            if ("".equals(minorTopics)) {
                descriptors.add(majorTopic);
            } else {
                descriptors.add(majorTopic + ", " + minorTopics);
            }
        }
        else if ("LastName".equals(localName)) {
            inLastName = false;
        }
        else if ("Suffix".equals(localName)) {
            inSuffix = false;
        }
        else if ("ForeName".equals(localName) || "FirstName".equals(localName)) {
            inForename = false;
        }
        else if ("Issue".equals(localName)) {
            inIssue = false;
        }
        else if ("MedlinePgn".equals(localName)) {
            inMedlinePgn = false;
        }//pagenumber
        else if ("URL".equals(localName)) {
            inUrl = false;
        }
        else if ("Initials".equals(localName)) {
            //initials= '.' + initials + '.';
            inInitials = false;
        }
        else if ("AbstractText".equals(localName)) {
            inAbstractText = false;
        }
        else if ("Affiliation".equals(localName)) {
            inAffiliation = false;
        }
        else if ("ArticleId".equals(localName)) {
            if (inDoi) {
                inDoi = false;
            } else if (inPii) {
                inPii = false;
            } else if (inPmc) {
                inPmc = false;
            }
        }
    }

    @Override
    public void characters(char[] data, int start, int length) {

        // if stack is not ready, data is not content of recognized element
        boolean inURL = false;
        boolean inMedlineID = false;
        if (inTitle) {
            title += new String(data, start, length);
        }
        else if (inYear) {
            year += new String(data, start, length);
        }
        else if (inJournal) {
            journal += new String(data, start, length);
        }
        else if (inMonth) {
            month += new String(data, start, length);
        }
        else if (inVolume) {
            volume += new String(data, start, length);
        }
        else if (inLanguage) {
            language += new String(data, start, length).toLowerCase();
        }
        else if (inPst) {
            pst += new String(data, start, length);
        }
        else if (inLastName) {
            lastname += new String(data, start, length);
        }
        else if (inSuffix) {
            suffix += new String(data, start, length);
        }
        else if (inInitials) {
            initials += new String(data, start, length);
        }
        else if (inIssue) {
            number += new String(data, start, length);
        }
        else if (inMedlinePgn) {
            page += new String(data, start, length);
        }
        else if (inMedlineID) {
            String medlineID = new String(data, start, length);
        }
        else if (inURL) {
            String url = new String(data, start, length);
        }
        else if (inPubMedID) {
            pubmedid = new String(data, start, length);
        }
        else if (inQualifierName) {
            if (!"".equals(minorTopics)) {
                minorTopics = minorTopics + "/";
            }
            minorTopics = minorTopics + new String(data, start, length);
        }
        else if (inDescriptorName) {
            majorTopic = new String(data, start, length);
        }

        //keywords += new String(data,start,length) + ", ";
        else if (inForename) {
            forename += new String(data, start, length);
            //System.out.println("IN FORENAME: " + forename);
        }
        else if (inAbstractText) {
            abstractText += new String(data, start, length);
        }
        else if (inMedlineDate) {
            medlineDate += new String(data, start, length);
        }
        else if (inDoi) {
            doi = new String(data, start, length);
        }
        else if (inPii) {
            pii = new String(data, start, length);
        }
        else if (inPmc) {
            pmc = new String(data, start, length);
        }
        else if (inAffiliation) {
            affiliation = new String(data, start, length);
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
        String first = pageRange.substring(0, minusPos).trim();
        String last = pageRange.substring(minusPos + 1).trim();
        int llast = last.length();
        int lfirst = first.length();
        if (llast < lfirst) {
            last = first.substring(0, lfirst - llast) + last;
        }
        return first + "--" + last;
    }
}
