package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.FileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.FieldName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Imports a New Economics Papers-Message from the REPEC-NEP Service.
 * <p>
 * <p><a href="http://www.repec.org">RePEc (Research Papers in Economics)</a>
 * is a collaborative effort of over 100 volunteers in 49 countries
 * to enhance the dissemination of research in economics. The heart of
 * the project is a decentralized database of working papers, journal
 * articles and software components. All RePEc material is freely available.</p>
 * At the time of writing RePEc holds over 300.000 items.</p>
 * <p>
 * <p><a href="http://nep.repec.org">NEP (New Economic Papers)</a> is an announcement
 * service which filters information on new additions to RePEc into edited
 * reports. The goal is to provide subscribers with up-to-date information
 * to the research literature.</p>
 * <p>
 * <p>This importer is capable of importing NEP messages into JabRef.</p>
 * <p>
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
 * @author andreas_sf at rudert-home dot de
 * @see <a href="http://nep.repec.org">NEP</a>
 */
public class RepecNepImporter extends Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepecNepImporter.class);

    private static final Collection<String> RECOGNIZED_FIELDS = Arrays.asList("Keywords", "JEL", "Date", "URL", "By");
    private final ImportFormatPreferences importFormatPreferences;
    private int line;
    private String lastLine = "";
    private String preLine = "";
    private boolean inOverviewSection;


    public RepecNepImporter(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return "REPEC New Economic Papers (NEP)";
    }

    @Override
    public String getId() {
        return "repecnep";
    }

    @Override
    public FileType getFileType() {
        return FileType.REPEC;
    }

    @Override
    public String getDescription() {
        return "Imports a New Economics Papers-Message from the REPEC-NEP Service.";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        // read the first couple of lines
        // NEP message usually contain the String 'NEP: New Economics Papers'
        // or, they are from nep.repec.org
        StringBuilder startOfMessage = new StringBuilder();
        String tmpLine = reader.readLine();
        for (int i = 0; (i < 25) && (tmpLine != null); i++) {
            startOfMessage.append(tmpLine);
            tmpLine = reader.readLine();
        }
        return startOfMessage.toString().contains("NEP: New Economics Papers") || startOfMessage.toString().contains(
                "nep.repec.org");
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
     * <p>
     * <p>Reads multiple lines until either
     * <ul>
     * <li>an empty line</li>
     * <li>the end of file</li>
     * <li>the next working paper or</li>
     * <li>a keyword</li>
     * </ul>
     * is found. Whitespace at start or end of lines is trimmed except for one blank character.</p>
     *
     * @return result
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
        be.setField(FieldName.TITLE, readMultipleLines(in));
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
            be.setField(FieldName.AUTHOR, String.join(" and ", authors));
        }
        if (institutions.length() > 0) {
            be.setField(FieldName.INSTITUTION, institutions.toString());
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
            be.setField(FieldName.ABSTRACT, theabstract);
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
                be.addKeywords(Arrays.asList(keywords),
                        importFormatPreferences.getKeywordSeparator());
                // parse JEL field
            } else if ("JEL".equals(keyword)) {
                be.setField("jel", readMultipleLines(in));

            } else if (keyword.startsWith("Date")) {
                // parse date field
                String content = readMultipleLines(in);
                Date.parse(content).ifPresent(be::setDate);
                // parse URL field
            } else if (keyword.startsWith("URL")) {
                String content;
                if (multilineUrlFieldAllowed) {
                    content = readMultipleLines(in);
                } else {
                    content = this.lastLine;
                    readLine(in);
                }
                be.setField(FieldName.URL, content);

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

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);

        List<BibEntry> bibitems = new ArrayList<>();
        String paperNoStr = null;
        this.line = 0;
        try {
            readLine(reader); // skip header and editor information
            while (this.lastLine != null) {

                if (this.lastLine.startsWith("-----------------------------")) {
                    this.inOverviewSection = this.preLine.startsWith("In this issue we have");
                }
                if (isStartOfWorkingPaper()) {
                    BibEntry be = new BibEntry();
                    be.setType("techreport");
                    paperNoStr = this.lastLine.substring(0, this.lastLine.indexOf('.'));
                    parseTitleString(be, reader);
                    if (startsWithKeyword(RepecNepImporter.RECOGNIZED_FIELDS)) {
                        parseAdditionalFields(be, false, reader);
                    } else {
                        readLine(reader); // skip empty line
                        parseAuthors(be, reader);
                        readLine(reader); // skip empty line
                    }
                    if (!startsWithKeyword(RepecNepImporter.RECOGNIZED_FIELDS)) {
                        parseAbstract(be, reader);
                    }
                    parseAdditionalFields(be, true, reader);

                    bibitems.add(be);
                    paperNoStr = null;

                } else {
                    this.preLine = this.lastLine;
                    readLine(reader);
                }
            }

        } catch (Exception e) {
            String message = "Error in REPEC-NEP import on line " + this.line;
            if (paperNoStr != null) {
                message += ", paper no. " + paperNoStr + ": ";
            }
            message += e.getLocalizedMessage();
            LOGGER.error(message, e);
            return ParserResult.fromErrorMessage(message);
        }

        return new ParserResult(bibitems);
    }
}
