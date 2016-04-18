package net.sf.jabref.importer.fileformat;

import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fetcher.DOItoBibTeXFetcher;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.logic.xmp.EncryptedPdfsNotSupportedException;
import net.sf.jabref.logic.xmp.XMPUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.EntryType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

import com.google.common.base.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PdfContentImporter parses data of the first page of the PDF and creates a BibTeX entry.
 * <p>
 * Currently, Springer and IEEE formats are supported.
 * <p>
 * Integrating XMP support is future work
 */
public class PdfContentImporter extends ImportFormat {
    private static final Log LOGGER = LogFactory.getLog(PdfContentImporter.class);

    private static final Pattern YEAR_EXTRACT_PATTERN = Pattern.compile("\\d{4}");
    // we can store the DOItoBibTeXFetcher as single reference as the fetcher doesn't hold internal state
    private static final DOItoBibTeXFetcher DOI_TO_BIBTEX_FETCHER = new DOItoBibTeXFetcher();

    // input lines into several lines
    private String[] lines;

    // current index in lines
    private int i;

    private String curString;

    private String year;

    /**
     * Removes all non-letter characters at the end
     * <p>
     * EXCEPTION: a closing bracket is NOT removed
     * </p>
     * <p>
     * TODO: Additionally replace multiple subsequent spaces by one space, which will cause a rename of this method
     * </p>
     */
    private static String removeNonLettersAtEnd(String input) {
        String result = input.trim();
        if (result.isEmpty()) {
            return result;
        }
        char lastC = result.charAt(result.length() - 1);
        while (!Character.isLetter(lastC) && (lastC != ')')) {
            // if there is an asterix, a dot or something else at the end: remove it
            result = result.substring(0, result.length() - 1);
            if (result.isEmpty()) {
                break;
            } else {
                lastC = result.charAt(result.length() - 1);
            }
        }
        return result;
    }

    private static String streamlineNames(String names) {
        // TODO: replace with NormalizeNamesFormatter?!
        String res;
        // supported formats:
        //   Matthias Schrepfer1, Johannes Wolf1, Jan Mendling1, and Hajo A. Reijers2
        if (names.contains(",")) {
            String[] splitNames = names.split(",");
            res = "";
            boolean isFirst = true;
            for (String splitName : splitNames) {
                String curName = removeNonLettersAtEnd(splitName);
                if (curName.indexOf("and") == 0) {
                    // skip possible ands between names
                    curName = curName.substring(3).trim();
                } else {
                    int posAnd = curName.indexOf(" and ");
                    if (posAnd >= 0) {
                        String nameBefore = curName.substring(0, posAnd);
                        // cannot be first name as "," is contained in the string
                        res = res.concat(" and ").concat(removeNonLettersAtEnd(nameBefore));
                        curName = curName.substring(posAnd + 5);
                    }
                }

                if (!"".equals(curName)) {
                    if ("et al.".equalsIgnoreCase(curName)) {
                        curName = "others";
                    }
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        res = res.concat(" and ");
                    }
                    res = res.concat(curName);
                }
            }
        } else {
            // assumption: names separated by space

            String[] splitNames = names.split(" ");
            if (splitNames.length == 0) {
                // empty names... something was really wrong...
                return "";
            }

            boolean workedOnFirstOrMiddle = false;
            boolean isFirst = true;
            int i = 0;
            res = "";
            do {
                if (workedOnFirstOrMiddle) {
                    // last item was a first or a middle name
                    // we have to check whether we are on a middle name
                    // if not, just add the item as last name and add an "and"
                    if (splitNames[i].contains(".")) {
                        // we found a middle name
                        res = res.concat(splitNames[i]).concat(" ");
                    } else {
                        // last name found
                        res = res.concat(removeNonLettersAtEnd(splitNames[i]));

                        if (!splitNames[i].isEmpty() && Character.isLowerCase(splitNames[i].charAt(0))) {
                            // it is probably be "van", "vom", ...
                            // we just rely on the fact that these things are written in lower case letters
                            // do NOT finish name
                            res = res.concat(" ");
                        } else {
                            // finish this name
                            workedOnFirstOrMiddle = false;
                        }
                    }
                } else {
                    if ("and".equalsIgnoreCase(splitNames[i])) {
                        // do nothing, just increment i at the end of this iteration
                    } else {
                        if (isFirst) {
                            isFirst = false;
                        } else {
                            res = res.concat(" and ");
                        }
                        if ("et".equalsIgnoreCase(splitNames[i]) && (splitNames.length > (i + 1))
                                && "al.".equalsIgnoreCase(splitNames[i + 1])) {
                            res = res.concat("others");
                            break;
                        } else {
                            res = res.concat(splitNames[i]).concat(" ");
                            workedOnFirstOrMiddle = true;
                        }
                    }
                }
                i++;
            } while (i < splitNames.length);
        }
        return res;
    }

    private static String streamlineTitle(String title) {
        return removeNonLettersAtEnd(title);
    }

    @Override
    public boolean isRecognizedFormat(InputStream in) throws IOException {
        return false;
    }

    @Override
    public List<BibEntry> importEntries(InputStream in, OutputPrinter status) throws IOException {
        final ArrayList<BibEntry> result = new ArrayList<>(1);

        try (PDDocument document = XMPUtil.loadWithAutomaticDecryption(in)) {
            String firstPageContents = getFirstPageContents(document);

            Optional<DOI> doi = DOI.findInText(firstPageContents);
            if (doi.isPresent()) {
                ImportInspector inspector = new ImportInspector() {

                    @Override
                    public void toFront() {
                        // Do nothing
                    }

                    @Override
                    public void setProgress(int current, int max) {
                        // Do nothing
                    }

                    @Override
                    public void addEntry(BibEntry entry) {
                        // add the entry to the result object
                        result.add(entry);
                    }
                };

                DOI_TO_BIBTEX_FETCHER.processQuery(doi.get().getDOI(), inspector, status);
                if (!result.isEmpty()) {
                    return result;
                }
            }

            // idea: split[] contains the different lines
            // blocks are separated by empty lines
            // treat each block
            //   or do special treatment at authors (which are not broken)
            //   therefore, we do a line-based and not a block-based splitting
            // i points to the current line
            // curString (mostly) contains the current block
            //   the different lines are joined into one and thereby separated by " "
            lines = firstPageContents.split(System.lineSeparator());

            proceedToNextNonEmptyLine();
            if (i >= lines.length) {
                // PDF could not be parsed or is empty
                // return empty list
                return result;
            }

            // we start at the current line
            curString = lines[i];
            // i might get incremented later and curString modified, too
            i = i + 1;

            String author;
            String editor = null;
            String abstractT = null;
            String keywords = null;
            String title;
            String conference = null;
            String DOI = null;
            String series = null;
            String volume = null;
            String number = null;
            String pages = null;
            // year is a class variable as the method extractYear() uses it;
            String publisher = null;

            EntryType type = BibtexEntryTypes.INPROCEEDINGS;
            if (curString.length() > 4) {
                // special case: possibly conference as first line on the page
                extractYear();
                if (curString.contains("Conference")) {
                    fillCurStringWithNonEmptyLines();
                    conference = curString;
                    curString = "";
                } else {
                    // e.g. Copyright (c) 1998 by the Genetics Society of America
                    // future work: get year using RegEx
                    String lower = curString.toLowerCase();
                    if (lower.contains("copyright")) {
                        fillCurStringWithNonEmptyLines();
                        publisher = curString;
                        curString = "";
                    }
                }
            }

            // start: title
            fillCurStringWithNonEmptyLines();
            title = streamlineTitle(curString);
            curString = "";
            //i points to the next non-empty line

            // after title: authors
            author = null;
            while ((i < lines.length) && !"".equals(lines[i])) {
                // author names are unlikely to be lines among different lines
                // treat them line by line
                curString = streamlineNames(lines[i]);
                if (author == null) {
                    author = curString;
                } else {
                    if ("".equals(curString)) {
                        // if lines[i] is "and" then "" is returned by streamlineNames -> do nothing
                    } else {
                        author = author.concat(" and ").concat(curString);
                    }
                }
                i++;
            }
            curString = "";
            i++;

            // then, abstract and keywords follow
            while (i < lines.length) {
                curString = lines[i];
                if ((curString.length() >= "Abstract".length()) && "Abstract".equalsIgnoreCase(curString.substring(0, "Abstract".length()))) {
                    if (curString.length() == "Abstract".length()) {
                        // only word "abstract" found -- skip line
                        curString = "";
                    } else {
                        curString = curString.substring("Abstract".length() + 1).trim().concat(System.lineSeparator());
                    }
                    i++;
                    // fillCurStringWithNonEmptyLines() cannot be used as that uses " " as line separator
                    // whereas we need linebreak as separator
                    while ((i < lines.length) && !"".equals(lines[i])) {
                        curString = curString.concat(lines[i]).concat(System.lineSeparator());
                        i++;
                    }
                    abstractT = curString.trim();
                    i++;
                } else if ((curString.length() >= "Keywords".length()) && "Keywords".equalsIgnoreCase(curString.substring(0, "Keywords".length()))) {
                    if (curString.length() == "Keywords".length()) {
                        // only word "Keywords" found -- skip line
                        curString = "";
                    } else {
                        curString = curString.substring("Keywords".length() + 1).trim();
                    }
                    i++;
                    fillCurStringWithNonEmptyLines();
                    keywords = removeNonLettersAtEnd(curString);
                } else {
                    String lower = curString.toLowerCase();

                    int pos = lower.indexOf("technical");
                    if (pos >= 0) {
                        type = BibtexEntryTypes.TECHREPORT;
                        pos = curString.trim().lastIndexOf(' ');
                        if (pos >= 0) {
                            // assumption: last character of curString is NOT ' '
                            //   otherwise pos+1 leads to an out-of-bounds exception
                            number = curString.substring(pos + 1);
                        }
                    }

                    i++;
                    proceedToNextNonEmptyLine();
                }
            }

            i = lines.length - 1;

            // last block: DOI, detailed information
            // sometimes, this information is in the third last block etc...
            // therefore, read until the beginning of the file

            while (i >= 0) {
                readLastBlock();
                // i now points to the block before or is -1
                // curString contains the last block, separated by " "

                extractYear();

                int pos = curString.indexOf("(Eds.)");
                if ((pos >= 0) && (publisher == null)) {
                    // looks like a Springer last line
                    // e.g: A. Persson and J. Stirna (Eds.): PoEM 2009, LNBIP 39, pp. 161-175, 2009.
                    publisher = "Springer";
                    editor = streamlineNames(curString.substring(0, pos - 1));
                    curString = curString.substring(pos + "(Eds.)".length() + 2); //+2 because of ":" after (Eds.) and the subsequent space
                    String[] springerSplit = curString.split(", ");
                    if (springerSplit.length >= 4) {
                        conference = springerSplit[0];

                        String seriesData = springerSplit[1];
                        int lastSpace = seriesData.lastIndexOf(' ');
                        series = seriesData.substring(0, lastSpace);
                        volume = seriesData.substring(lastSpace + 1);

                        pages = springerSplit[2].substring(4);

                        if (springerSplit[3].length() >= 4) {
                            year = springerSplit[3].substring(0, 4);
                        }
                    }
                } else {
                    if (DOI == null) {
                        pos = curString.indexOf("DOI");
                        if (pos < 0) {
                            pos = curString.indexOf("doi");
                        }
                        if (pos >= 0) {
                            pos += 3;
                            char delimiter = curString.charAt(pos);
                            if ((delimiter == ':') || (delimiter == ' ')) {
                                pos++;
                            }
                            int nextSpace = curString.indexOf(' ', pos);
                            if (nextSpace > 0) {
                                DOI = curString.substring(pos, nextSpace);
                            } else {
                                DOI = curString.substring(pos);
                            }
                        }
                    }

                    if ((publisher == null) && curString.contains("IEEE")) {
                        // IEEE has the conference things at the end
                        publisher = "IEEE";

                        // year is extracted by extractYear
                        // otherwise, we could it determine as follows:
                        // String yearStr = curString.substring(curString.length()-4);
                        // if (isYear(yearStr)) {
                        //	year = yearStr;
                        // }

                        if (conference == null) {
                            pos = curString.indexOf('$');
                            if (pos > 0) {
                                // we found the price
                                // before the price, the ISSN is stated
                                // skip that
                                pos -= 2;
                                while ((pos >= 0) && (curString.charAt(pos) != ' ')) {
                                    pos--;
                                }
                                if (pos > 0) {
                                    conference = curString.substring(0, pos);
                                }
                            }
                        }
                    }
                }
            }

            BibEntry entry = new BibEntry();
            entry.setType(type);

            // TODO: institution parsing missing

            if (author != null) {
                entry.setField("author", author);
            }
            if (editor != null) {
                entry.setField("editor", editor);
            }
            if (abstractT != null) {
                entry.setField("abstract", abstractT);
            }
            if (!Strings.isNullOrEmpty(keywords)) {
                entry.setField("keywords", keywords);
            }
            if (title != null) {
                entry.setField("title", title);
            }
            if (conference != null) {
                entry.setField("booktitle", conference);
            }
            if (DOI != null) {
                entry.setField("doi", DOI);
            }
            if (series != null) {
                entry.setField("series", series);
            }
            if (volume != null) {
                entry.setField("volume", volume);
            }
            if (number != null) {
                entry.setField("number", number);
            }
            if (pages != null) {
                entry.setField("pages", pages);
            }
            if (year != null) {
                entry.setField("year", year);
            }
            if (publisher != null) {
                entry.setField("publisher", publisher);
            }

            result.add(entry);
        } catch (EncryptedPdfsNotSupportedException e) {
            LOGGER.info("Decryption not supported");
            return Collections.EMPTY_LIST;
        }
        return result;
    }

    private String getFirstPageContents(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();

        stripper.setStartPage(1);
        stripper.setEndPage(1);
        stripper.setSortByPosition(true);
        stripper.setParagraphEnd(System.lineSeparator());
        StringWriter writer = new StringWriter();
        stripper.writeText(document, writer);

        return writer.toString();
    }

    /**
     * Extract the year out of curString (if it is not yet defined)
     */
    private void extractYear() {
        if (year != null) {
            return;
        }

        Matcher m = YEAR_EXTRACT_PATTERN.matcher(curString);
        if (m.find()) {
            year = curString.substring(m.start(), m.end());
        }

    }

    /**
     * PDFTextStripper normally does NOT produce multiple empty lines
     * (besides at strange PDFs). These strange PDFs are handled here:
     * proceed to next non-empty line
     */
    private void proceedToNextNonEmptyLine() {
        while ((i < lines.length) && "".equals(lines[i].trim())) {
            i++;
        }
    }

    /**
     * Fill curString with lines until "" is found
     * No trailing space is added
     * i is advanced to the next non-empty line (ignoring white space)
     * <p>
     * Lines containing only white spaces are ignored,
     * but NOT considered as ""
     * <p>
     * Uses GLOBAL variables lines, curLine, i
     */
    private void fillCurStringWithNonEmptyLines() {
        // ensure that curString does not end with " "
        curString = curString.trim();
        while ((i < lines.length) && !"".equals(lines[i])) {
            String curLine = lines[i].trim();
            if (!"".equals(curLine)) {
                if (!curString.isEmpty()) {
                    // insert separating space if necessary
                    curString = curString.concat(" ");
                }
                curString = curString.concat(lines[i]);
            }
            i++;
        }

        proceedToNextNonEmptyLine();
    }

    /**
     * resets curString
     * curString now contains the last block (until "" reached)
     * Trailing space is added
     * <p>
     * invariant before/after: i points to line before the last handled block
     */
    private void readLastBlock() {
        while ((i >= 0) && "".equals(lines[i].trim())) {
            i--;
        }
        // i is now at the end of a block

        int end = i;

        // find beginning
        while ((i >= 0) && !"".equals(lines[i])) {
            i--;
        }
        // i is now the line before the beginning of the block
        // this fulfills the invariant

        curString = "";
        for (int j = i + 1; j <= end; j++) {
            curString = curString.concat(lines[j].trim());
            if (j != end) {
                curString = curString.concat(" ");
            }
        }
    }

    @Override
    public String getFormatName() {
        return "PDFcontent";
    }

}
