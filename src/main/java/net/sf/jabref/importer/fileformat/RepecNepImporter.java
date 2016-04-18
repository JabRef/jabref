/* Copyright (C) 2005 Andreas Rudert
   Copyright (C) 2015 JabRef contributors

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
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.IdGenerator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Imports a New Economics Papers-Message from the REPEC-NEP Service.
 *
 * <p><a href="http://www.repec.org">RePEc (Research Papers in Economics)</a>
 * is a collaborative effort of over 100 volunteers in 49 countries
 * to enhance the dissemination of research in economics. The heart of
 * the project is a decentralized database of working papers, journal
 * articles and software components. All RePEc material is freely available.</p>
 * At the time of writing RePEc holds over 300.000 items.</p>
 *
 * <p><a href="http://nep.repec.org">NEP (New Economic Papers)</a> is an announcement
 * service which filters information on new additions to RePEc into edited
 * reports. The goal is to provide subscribers with up-to-date information
 * to the research literature.</p>
 *
 * <p>This importer is capable of importing NEP messages into JabRef.</p>
 *
 * <p>There is no officially defined message format for NEP. NEP messages are assumed to have
 * (and almost always have) the form given by the following semi-formal grammar:
 * <pre>
 * NEPMessage:
 *       MessageSection NEPMessage
 *       MessageSection
 *
 * MessageSection:
 *       OverviewMessageSection
 *       OtherMessageSection
 *
 * # we skip the overview
 * OverviewMessageSection:
 *       'In this issue we have: ' SectionSeparator OtherStuff
 *
 * OtherMessageSection:
 *       SectionSeparator  OtherMessageSectionContent
 *
 * # we skip other stuff and read only full working paper references
 * OtherMessageSectionContent:
 *       WorkingPaper EmptyLine OtherMessageSectionContent
 *       OtherStuff EmptyLine OtherMessageSectionContent
 *       ''
 *
 * OtherStuff:
 *       NonEmptyLine OtherStuff
 *       NonEmptyLine
 *
 * NonEmptyLine:
 *       a non-empty String that does not start with a number followed by a '.'
 *
 * # working papers are recognized by a number followed by a '.'
 * # in a non-overview section
 * WorkingPaper:
 *       Number'.' WhiteSpace TitleString EmptyLine Authors EmptyLine Abstract AdditionalFields
 *       Number'.' WhiteSpace TitleString AdditionalFields Abstract AdditionalFields
 *
 * TitleString:
 *       a String that may span several lines and should be joined
 *
 * # there must be at least one author
 * Authors:
 *       Author '\n' Authors
 *       Author '\n'
 *
 * # optionally, an institution is given for an author
 * Author:
 *       AuthorName
 *       AuthorName '(' Institution ')'
 *
 * # there are no rules about the name, it may be firstname lastname or lastname, firstname or anything else
 * AuthorName:
 *       a non-empty String without '(' or ')' characters, not spanning more that one line
 *
 * Institution:
 *       a non-empty String that may span several lines
 *
 * Abstract:
 *       a (possibly empty) String that may span several lines
 *
 * AdditionalFields:
 *       AdditionalField '\n' AdditionalFields
 *       EmptyLine AdditionalFields
 *       ''
 *
 * AdditionalField:
 *       'Keywords:' KeywordList
 *       'URL:' non-empty String
 *       'Date:' DateString
 *       'JEL:' JelClassificationList
 *       'By': Authors
 *
 * KeywordList:
 *        Keyword ',' KeywordList
 *        Keyword ';' KeywordList
 *        Keyword
 *
 * Keyword:
 *        non-empty String that does not contain ',' (may contain whitespace)
 *
 * # if no date is given, the current year as given by the system clock is assumed
 * DateString:
 *        'yyyy-MM-dd'
 *        'yyyy-MM'
 *        'yyyy'
 *
 * JelClassificationList:
 *        JelClassification JelClassificationList
 *        JelClassification
 *
 * # the JEL Classifications are set into a new BIBTEX-field 'jel'
 * # they will appear if you add it as a field to one of the BIBTex Entry sections
 * JelClassification:
 *        one of the allowed classes, see http://ideas.repec.org/j/
 *
 * SectionSeparator:
 *       '\n-----------------------------'
 * </pre>
 * </p>
 *
 * @see <a href="http://nep.repec.org">NEP</a>
 * @author andreas_sf at rudert-home dot de
 */
public class RepecNepImporter extends ImportFormat {

    private static final Log LOGGER = LogFactory.getLog(RepecNepImporter.class);

    private static final Collection<String> RECOGNIZED_FIELDS = Arrays.asList("Keywords", "JEL", "Date", "URL", "By");

    private int line;
    private String lastLine = "";
    private String preLine = "";
    private boolean inOverviewSection;


    /**
     * Return the name of this import format.
     */
    @Override
    public String getFormatName() {
        return "REPEC New Economic Papers (NEP)";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "repecnep";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getExtensions()
     */
    @Override
    public String getExtensions() {
        return ".txt";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getDescription()
     */
    @Override
    public String getDescription() {
        return
        "Imports a New Economics Papers-Message (see http://nep.repec.org)\n"
                + "from the REPEC-NEP Service (see http://www.repec.org).\n"
                + "To import papers either save a NEP message as a text file and then import or\n"
                + "copy&paste the papers you want to import and make sure, one of the first lines\n"
                + "contains the line \"nep.repec.org\".";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#isRecognizedFormat(java.io.InputStream)
     */
    @Override
    public boolean isRecognizedFormat(InputStream stream) throws IOException {
        // read the first couple of lines
        // NEP message usually contain the String 'NEP: New Economics Papers'
        // or, they are from nep.repec.org
        try (BufferedReader inBR = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream))) {
            StringBuilder startOfMessage = new StringBuilder();
            String tmpLine = inBR.readLine();
            for (int i = 0; (i < 25) && (tmpLine != null); i++) {
                startOfMessage.append(tmpLine);
                tmpLine = inBR.readLine();
            }
            return startOfMessage.toString().contains("NEP: New Economics Papers")
                    || startOfMessage.toString().contains("nep.repec.org");
        }
    }

    private boolean startsWithKeyword(Collection<String> keywords) {
        boolean result = this.lastLine.indexOf(':') >= 1;
        if (result) {
            String possibleKeyword = this.lastLine.substring(0, this.lastLine.indexOf(':'));
            result = keywords.contains(possibleKeyword);
        }
        return result;
    }

    private void readLine(BufferedReader in) throws IOException {
        this.line++;
        this.preLine = this.lastLine;
        this.lastLine = in.readLine();
    }

    /**
     * Read multiple lines.
     *
     * <p>Reads multiple lines until either
     * <ul>
     *   <li>an empty line</li>
     *   <li>the end of file</li>
     *   <li>the next working paper or</li>
     *   <li>a keyword</li>
     * </ul>
     * is found. Whitespace at start or end of lines is trimmed except for one blank character.</p>
     *
     * @return  result
     */
    private String readMultipleLines(BufferedReader in) throws IOException {
        StringBuilder result = new StringBuilder(this.lastLine.trim());
        readLine(in);
        while ((this.lastLine != null) && !"".equals(this.lastLine.trim()) && !startsWithKeyword(RepecNepImporter.RECOGNIZED_FIELDS) && !isStartOfWorkingPaper()) {
            result.append(this.lastLine.isEmpty() ? this.lastLine.trim() : " " + this.lastLine.trim());
            readLine(in);
        }
        return result.toString();
    }

    /**
     * Implements grammar rule "TitleString".
     *
     * @param be
     * @throws IOException
     */
    private void parseTitleString(BibEntry be, BufferedReader in) throws IOException {
        // skip article number
        this.lastLine = this.lastLine.substring(this.lastLine.indexOf('.') + 1, this.lastLine.length());
        be.setField("title", readMultipleLines(in));
    }

    /**
     * Implements grammar rule "Authors"
     *
     * @param be
     * @throws IOException
     */
    private void parseAuthors(BibEntry be, BufferedReader in) throws IOException {
        // read authors and institutions
        List<String> authors = new ArrayList<>();
        StringBuilder institutions = new StringBuilder();
        while ((this.lastLine != null) && !"".equals(this.lastLine) && !startsWithKeyword(RepecNepImporter.RECOGNIZED_FIELDS)) {

            // read single author
            String author;
            StringBuilder institution = new StringBuilder();
            boolean institutionDone;
            if (this.lastLine.indexOf('(') >= 0) {
                author = this.lastLine.substring(0, this.lastLine.indexOf('(')).trim();
                institutionDone = this.lastLine.indexOf(')') >= 1;
                institution
                        .append(this.lastLine.substring(this.lastLine.indexOf('(') + 1,
                                institutionDone && (this.lastLine
                                        .indexOf(')') > (this.lastLine.indexOf('(') + 1)) ? this.lastLine
                                                .indexOf(')') : this.lastLine.length())
                                .trim());
            } else {
                author = this.lastLine.substring(0, this.lastLine.length()).trim();
                institutionDone = true;
            }

            readLine(in);
            while (!institutionDone && (this.lastLine != null)) {
                institutionDone = this.lastLine.indexOf(')') >= 1;
                institution.append(this.lastLine
                        .substring(0, institutionDone ? this.lastLine.indexOf(')') : this.lastLine.length()).trim());
                readLine(in);
            }

            authors.add(author);

            if (institution.length() > 0) {
                institutions.append(
                        (institutions.length() == 0) ? institution.toString() : " and " + institution.toString());
            }
        }

        if (!authors.isEmpty()) {
            be.setField("author", String.join(" and ", authors));
        }
        if (institutions.length() > 0) {
            be.setField("institution", institutions.toString());
        }
    }

    /**
     * Implements grammar rule "Abstract".
     *
     * @param be
     * @throws IOException
     */
    private void parseAbstract(BibEntry be, BufferedReader in) throws IOException {
        String theabstract = readMultipleLines(in);

        if (!"".equals(theabstract)) {
            be.setField("abstract", theabstract);
        }
    }

    /**
     * Implements grammar rule "AdditionalFields".
     *
     * @param be
     * @throws IOException
     */
    private void parseAdditionalFields(BibEntry be, boolean multilineUrlFieldAllowed, BufferedReader in)
            throws IOException {

        // one empty line is possible before fields start
        if ((this.lastLine != null) && "".equals(this.lastLine.trim())) {
            readLine(in);
        }

        // read other fields
        while ((this.lastLine != null) && !isStartOfWorkingPaper() && (startsWithKeyword(RepecNepImporter.RECOGNIZED_FIELDS) || "".equals(this.lastLine))) {

            // if multiple lines for a field are allowed and field consists of multiple lines, join them
            String keyword = "".equals(this.lastLine) ? "" : this.lastLine.substring(0, this.lastLine.indexOf(':')).trim();
            // skip keyword
            this.lastLine = "".equals(this.lastLine) ? "" : this.lastLine.substring(this.lastLine.indexOf(':') + 1, this.lastLine.length()).trim();

            // parse keywords field
            if ("Keywords".equals(keyword)) {
                String content = readMultipleLines(in);
                String[] keywords = content.split("[,;]");
                be.addKeywords(Arrays.asList(keywords));
                // parse JEL field
            } else if ("JEL".equals(keyword)) {
                be.setField("jel", readMultipleLines(in));

                // parse date field
            } else if (keyword.startsWith("Date")) {
                Date date = null;
                String content = readMultipleLines(in);
                String[] recognizedDateFormats = new String[] {"yyyy-MM-dd", "yyyy-MM", "yyyy"};
                int i = 0;
                for (; (i < recognizedDateFormats.length) && (date == null); i++) {
                    try {
                        date = new SimpleDateFormat(recognizedDateFormats[i]).parse(content);
                    } catch (ParseException e) {
                        // wrong format
                    }
                }

                Calendar cal = new GregorianCalendar();
                cal.setTime(date == null ? new Date() : date);
                be.setField("year", String.valueOf(cal.get(Calendar.YEAR)));
                if ((date != null) && recognizedDateFormats[i - 1].contains("MM")) {
                    be.setField("month", String.valueOf(cal.get(Calendar.MONTH) + 1));
                }
                if ((date != null) && recognizedDateFormats[i - 1].contains("dd")) {
                    be.setField("day", String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
                }

                // parse URL field
            } else if (keyword.startsWith("URL")) {
                String content;
                if (multilineUrlFieldAllowed) {
                    content = readMultipleLines(in);
                } else {
                    content = this.lastLine;
                    readLine(in);
                }
                be.setField("url", content);

                // authors field
            } else if (keyword.startsWith("By")) {
                // parse authors
                parseAuthors(be, in);
            } else {
                readLine(in);
            }
        }
    }

    /**
     * if line starts with a string of the form 'x. ' and we are not in the overview
     * section, we have a working paper entry we are interested in
     */
    private boolean isStartOfWorkingPaper() {
        return this.lastLine.matches("\\d+\\.\\s.*") && !this.inOverviewSection && "".equals(this.preLine.trim());
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#importEntries(java.io.InputStream)
     */
    @Override
    public List<BibEntry> importEntries(InputStream stream, OutputPrinter status) throws IOException {
        List<BibEntry> bibitems = new ArrayList<>();
        String paperNoStr = null;
        this.line = 0;

        try (BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream))) {
            readLine(in); // skip header and editor information
            while (this.lastLine != null) {

                if (this.lastLine.startsWith("-----------------------------")) {
                    this.inOverviewSection = this.preLine.startsWith("In this issue we have");
                }
                if (isStartOfWorkingPaper()) {
                    BibEntry be = new BibEntry(IdGenerator.next());
                    be.setType("techreport");
                    paperNoStr = this.lastLine.substring(0, this.lastLine.indexOf('.'));
                    parseTitleString(be, in);
                    if (startsWithKeyword(RepecNepImporter.RECOGNIZED_FIELDS)) {
                        parseAdditionalFields(be, false, in);
                    } else {
                        readLine(in); // skip empty line
                        parseAuthors(be, in);
                        readLine(in); // skip empty line
                    }
                    if (!startsWithKeyword(RepecNepImporter.RECOGNIZED_FIELDS)) {
                        parseAbstract(be, in);
                    }
                    parseAdditionalFields(be, true, in);

                    bibitems.add(be);
                    paperNoStr = null;

                } else {
                    this.preLine = this.lastLine;
                    readLine(in);
                }
            }

        } catch (Exception e) {
            String message = "Error in REPEC-NEP import on line " + this.line;
            if (paperNoStr != null) {
                message += ", paper no. " + paperNoStr + ": ";
            }
            message += e.getMessage();
            LOGGER.error(message, e);
            IOException toThrow;
            if (e instanceof IOException) {
                toThrow = (IOException) e;
            } else {
                toThrow = new IOException(message);
            }
            throw toThrow;
        }

        return bibitems;
    }
}
