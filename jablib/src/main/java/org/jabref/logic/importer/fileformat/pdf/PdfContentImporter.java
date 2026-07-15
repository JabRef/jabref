package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.PdfUtils;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.logic.util.strings.StringUtil.isNullOrEmpty;

/// Parses data of the first page of the PDF and creates a BibTeX entry.
///
/// Currently, Springer, and IEEE formats are supported.
///
/// In case one wants to have a list of {@link BibEntry} matching the bibliography of a PDF,
/// please see {@link RuleBasedBibliographyPdfImporter}.
///
/// If several PDF importers should be tried, use {@link PdfMergeMetadataImporter}.
public class PdfContentImporter extends PdfImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfContentImporter.class);

    // Lookarounds keep the pattern from matching inside longer digit runs such as postal codes or URL path segments
    private static final Pattern YEAR_EXTRACT_PATTERN = Pattern.compile("(?<!\\d)\\d{4}(?!\\d)");
    private static final int MINIMUM_PLAUSIBLE_YEAR = 1500;

    private static final int ARXIV_PREFIX_LENGTH = "arxiv:".length();

    // Author cross-check against the leading-page text (used by PdfMergeMetadataImporter, see crossCheckAuthor)
    private static final Pattern LEADING_AND_TRAILING_NON_LETTERS = Pattern.compile("^\\P{L}+|\\P{L}+$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NON_LETTERS = Pattern.compile("\\P{L}+");
    private static final Pattern SOFT_LINE_BREAK_HYPHEN = Pattern.compile("-\\r?\\n\\s*");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Set<String> NAME_LIST_LOWERCASE_WORDS = Set.of(
            "and", "van", "von", "der", "den", "de", "del", "dos", "da", "di", "la", "le", "ten", "ter", "y", "e");

    // input lines into several lines
    private String[] lines;

    // current index in lines
    private int lineIndex;

    private String curString;

    private String year;

    /// Removes all non-letter characters at the end
    ///
    /// EXCEPTION: a closing bracket is NOT removed
    ///
    ///
    /// TODO: Additionally replace multiple subsequent spaces by one space, which will cause a rename of this method
    ///
    private String removeNonLettersAtEnd(String input) {
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

    private String streamlineNames(String names) {
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

                if (!curName.isEmpty()) {
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
            // @formatter:off
            do {
                // @formatter:on
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
                    if (!"and".equalsIgnoreCase(splitNames[i])) {
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
                    }  // do nothing, just increment i at the end of this iteration
                }
                i++;
            } while (i < splitNames.length);
        }
        return res;
    }

    private String streamlineTitle(String title) {
        return removeNonLettersAtEnd(title);
    }

    @Override
    public ParserResult importDatabase(Path filePath, PDDocument document) throws IOException {
        List<BibEntry> result = new ArrayList<>(1);
        String firstPageContents = PdfUtils.getFirstPageContents(document);
        Optional<String> titleByFontSize = extractTitleFromDocument(document);
        Optional<BibEntry> entry = getEntryFromPDFContent(firstPageContents, OS.NEWLINE, titleByFontSize);
        entry.ifPresent(result::add);
        return new ParserResult(result);
    }

    private static Optional<String> extractTitleFromDocument(PDDocument document) throws IOException {
        TitleExtractorByFontSize stripper = new TitleExtractorByFontSize();
        return stripper.getTitle(document);
    }

    private static class TitleExtractorByFontSize extends PDFTextStripper {

        private final List<TextPosition> textPositionsList;

        public TitleExtractorByFontSize() {
            super();
            this.textPositionsList = new ArrayList<>();
        }

        public Optional<String> getTitle(PDDocument document) throws IOException {
            this.setStartPage(1);
            this.setEndPage(2);
            this.writeText(document, new StringWriter());
            return findLargestFontText(textPositionsList);
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) {
            textPositionsList.addAll(textPositions);
        }

        private boolean isFarAway(TextPosition previous, TextPosition current) {
            float XspaceThreshold = previous.getFontSizeInPt() * 3.0F;
            float YspaceThreshold = previous.getFontSizeInPt() * 3.0F;
            float Xgap = current.getXDirAdj() - (previous.getXDirAdj() + previous.getWidthDirAdj());
            float Ygap = current.getYDirAdj() - previous.getYDirAdj();
            // For cases like paper titles spanning two or more lines, both X and Y gaps must exceed thresholds,
            // so "&&" is used instead of "||".
            return Math.abs(Xgap) > XspaceThreshold && Math.abs(Ygap) > YspaceThreshold;
        }

        private boolean isUnwantedText(TextPosition previousTextPosition, TextPosition textPosition,
                                       Map<Float, TextPosition> lastPositionMap, float fontSize) {
            // This indicates that the text is at the start of the line, so it is needed.
            if (textPosition == null || previousTextPosition == null) {
                return false;
            }
            // We use the font size to identify titles. Blank characters don't have a font size, so we discard them.
            // The space will be added back in the final result, but not in this method.
            if (StringUtil.isBlank(textPosition.getUnicode())) {
                return true;
            }
            // Titles are generally not located in the bottom 10% of a page.
            if ((textPosition.getPageHeight() - textPosition.getYDirAdj()) < (textPosition.getPageHeight() * 0.1)) {
                return true;
            }
            // Characters in a title typically remain close together,
            // so a distant character is unlikely to be part of the title.
            return lastPositionMap.containsKey(fontSize) && isFarAway(lastPositionMap.get(fontSize), textPosition);
        }

        private Optional<String> findLargestFontText(List<TextPosition> textPositions) {
            Map<Float, StringBuilder> fontSizeTextMap = new TreeMap<>(Collections.reverseOrder());
            Map<Float, TextPosition> lastPositionMap = new TreeMap<>(Collections.reverseOrder());
            TextPosition previousTextPosition = null;
            for (TextPosition textPosition : textPositions) {
                float fontSize = textPosition.getFontSizeInPt();
                // Exclude unwanted text based on heuristics
                if (isUnwantedText(previousTextPosition, textPosition, lastPositionMap, fontSize)) {
                    continue;
                }
                fontSizeTextMap.putIfAbsent(fontSize, new StringBuilder());
                if (previousTextPosition != null && isThereSpace(previousTextPosition, textPosition)) {
                    fontSizeTextMap.get(fontSize).append(" ");
                }
                fontSizeTextMap.get(fontSize).append(textPosition.getUnicode());
                lastPositionMap.put(fontSize, textPosition);
                previousTextPosition = textPosition;
            }
            for (Map.Entry<Float, StringBuilder> entry : fontSizeTextMap.entrySet()) {
                String candidateText = entry.getValue().toString().trim();
                if (isLegalTitle(candidateText)) {
                    return Optional.of(candidateText);
                }
            }
            return fontSizeTextMap.values().stream().findFirst().map(StringBuilder::toString).map(String::trim);
        }

        private boolean isLegalTitle(String candidateText) {
            // The minimum title length typically observed in academic research is 4 characters.
            return candidateText.length() >= 4;
        }

        private boolean isThereSpace(TextPosition previous, TextPosition current) {
            float XspaceThreshold = 1F;
            float YspaceThreshold = previous.getFontSizeInPt();
            float Xgap = current.getXDirAdj() - (previous.getXDirAdj() + previous.getWidthDirAdj());
            float Ygap = current.getYDirAdj() - (previous.getYDirAdj() - previous.getHeightDir());
            return Math.abs(Xgap) > XspaceThreshold || Math.abs(Ygap) > YspaceThreshold;
        }
    }

    /// Parses the first page content of a PDF document and extracts bibliographic information such as title, author,
    /// abstract, keywords, and other relevant metadata. This method processes the content line-by-line and uses
    /// custom parsing logic to identify and assemble information blocks from academic papers.
    ///
    /// idea: split[] contains the different lines, blocks are separated by empty lines, treat each block
    /// or do special treatment at authors (which are not broken).
    /// Therefore, we do a line-based and not a block-based splitting i points to the current line
    /// curString (mostly) contains the current block,
    /// the different lines are joined into one and thereby separated by " "
    ///
    /// This method follows the structure typically found in academic paper PDFs:
    /// - First, it attempts to detect the title by font size, if available, or by text position.
    /// - Authors are then processed line-by-line until reaching the next section.
    /// - Abstract and keywords, if found, are extracted as they appear on the page.
    /// - Finally, conference details, DOI, and publication information are parsed from the lower blocks.
    ///
    /// The parsing logic also identifies and categorizes entries based on keywords such as "Abstract" or "Keywords"
    /// and specific terms that denote sections. Additionally, this method can handle
    /// publisher-specific formats like Springer or IEEE, extracting data like series, volume, and conference titles.
    ///
    /// @param firstpageContents The raw content of the PDF's first page, which may contain metadata and main content.
    /// @param lineSeparator     The line separator used to format and unify line breaks in the text content.
    /// @param titleByFontSize   An optional title string determined by font size; if provided, this overrides the default title parsing.
    /// @return An {@link Optional} containing a {@link BibEntry} with the parsed bibliographic data if extraction
    /// is successful. Otherwise, an empty {@link Optional}.
    @VisibleForTesting
    Optional<BibEntry> getEntryFromPDFContent(String firstpageContents, String lineSeparator, Optional<String> titleByFontSize) {
        String firstpageContentsUnifiedLineBreaks = StringUtil.unifyLineBreaks(firstpageContents, lineSeparator);

        lines = firstpageContentsUnifiedLineBreaks.split(lineSeparator);

        lineIndex = 0; // to prevent array index out of bounds exception on second run we need to reset i to zero

        proceedToNextNonEmptyLine();
        if (lineIndex >= lines.length) {
            // PDF could not be parsed or is empty
            // return empty list
            return Optional.empty();
        }

        // we start at the current line
        curString = lines[lineIndex];
        // i might get incremented later and curString modified, too
        lineIndex++;

        String author;
        String editor = null;
        String abstractT = null;
        String keywords = null;
        String title;
        String conference = null;
        String doi = null;
        String series = null;
        String volume = null;
        String number = null;
        String pages = null;
        String arXivId = null;
        // year is a class variable as the method extractYear() uses it;
        String publisher = null;

        EntryType type = StandardEntryType.InProceedings;
        if (curString.length() > 4) {
            arXivId = getArXivId(null);
            // special case: possibly conference as first line on the page
            extractYear();
            doi = getDoi(null);
            if (curString.contains("Conference")) {
                fillCurStringWithNonEmptyLines();
                conference = curString;
                curString = "";
            } else {
                // e.g. Copyright (c) 1998 by the Genetics Society of America
                // future work: get year using RegEx
                String lower = curString.toLowerCase(Locale.ROOT);
                if (lower.contains("copyright")) {
                    fillCurStringWithNonEmptyLines();
                    publisher = curString;
                    curString = "";
                }
            }
        }

        arXivId = getArXivId(arXivId);
        // start: title
        fillCurStringWithNonEmptyLines();
        title = streamlineTitle(curString);
        // i points to the next non-empty line
        curString = "";
        if (titleByFontSize.isPresent() && !isNullOrEmpty(titleByFontSize.get())) {
            title = titleByFontSize.get();
        }

        // after title: authors
        author = null;
        while ((lineIndex < lines.length) && !"".equals(lines[lineIndex])) {
            // author names are unlikely to be lines among different lines
            // treat them line by line
            curString = streamlineNames(lines[lineIndex]);
            if (author == null) {
                author = curString;
            } else {
                if (!"".equals(curString)) {
                    author = author.concat(" and ").concat(curString);
                }  // if lines[i] is "and" then "" is returned by streamlineNames -> do nothing
            }
            lineIndex++;
        }
        curString = "";
        lineIndex++;

        // then, abstract and keywords follow
        while (lineIndex < lines.length) {
            curString = lines[lineIndex];
            if ((curString.length() >= "Abstract".length()) && "Abstract".equalsIgnoreCase(curString.substring(0, "Abstract".length()))) {
                if (curString.length() == "Abstract".length()) {
                    // only word "abstract" found -- skip line
                    curString = "";
                } else {
                    curString = curString.substring("Abstract".length() + 1).trim().concat(System.lineSeparator());
                }
                lineIndex++;
                // fillCurStringWithNonEmptyLines() cannot be used as that uses " " as line separator
                // whereas we need linebreak as separator
                while ((lineIndex < lines.length) && !"".equals(lines[lineIndex])) {
                    curString = curString.concat(lines[lineIndex]).concat(System.lineSeparator());
                    lineIndex++;
                }
                abstractT = curString.trim();
                lineIndex++;
            } else if ((curString.length() >= "Keywords".length()) && "Keywords".equalsIgnoreCase(curString.substring(0, "Keywords".length()))) {
                if (curString.length() == "Keywords".length()) {
                    // only word "Keywords" found -- skip line
                    curString = "";
                } else {
                    curString = curString.substring("Keywords".length() + 1).trim();
                }
                lineIndex++;
                fillCurStringWithNonEmptyLines();
                keywords = removeNonLettersAtEnd(curString);
            } else {
                String lower = curString.toLowerCase(Locale.ROOT);

                int pos = lower.indexOf("technical");
                if (pos >= 0) {
                    type = StandardEntryType.TechReport;
                    pos = curString.trim().lastIndexOf(' ');
                    if (pos >= 0) {
                        // assumption: last character of curString is NOT ' '
                        //   otherwise pos+1 leads to an out-of-bounds exception
                        number = curString.substring(pos + 1);
                    }
                }

                lineIndex++;
                proceedToNextNonEmptyLine();
            }
        }

        lineIndex = lines.length - 1;

        // last block: DOI, detailed information
        // sometimes, this information is in the third last block etc...
        // therefore, read until the beginning of the file

        while (lineIndex >= 0) {
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

                int edslength = "(Eds.)".length();
                int posWithEditor = pos + edslength + 2; // +2 because of ":" after (Eds.) and the subsequent space
                if (posWithEditor > curString.length()) {
                    curString = curString.substring(posWithEditor - 2); // we don't have any spaces after Eds so we substract the 2
                } else {
                    curString = curString.substring(posWithEditor);
                }
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
                doi = getDoi(doi);
                arXivId = getArXivId(arXivId);

                if ((publisher == null) && curString.contains("IEEE")) {
                    // IEEE has the conference things at the end
                    publisher = "IEEE";

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

        BibEntry entry = new BibEntry(type);

        // TODO: institution parsing missing

        if (author != null) {
            entry.setField(StandardField.AUTHOR, author);
        }
        if (editor != null) {
            entry.setField(StandardField.EDITOR, editor);
        }
        if (abstractT != null) {
            entry.setField(StandardField.ABSTRACT, abstractT);
        }
        if (!Strings.isNullOrEmpty(keywords)) {
            entry.setField(StandardField.KEYWORDS, keywords);
        }
        if (title != null) {
            entry.setField(StandardField.TITLE, title);
        }
        if (conference != null) {
            entry.setField(StandardField.BOOKTITLE, conference);
        }
        if (doi != null) {
            entry.setField(StandardField.DOI, doi);
        }
        if (arXivId != null) {
            entry.setField(StandardField.EPRINT, arXivId);
            assert !arXivId.startsWith("arxiv");
            entry.setField(StandardField.EPRINTTYPE, "arXiv");

            // Quick workaround to avoid wrong year and number parsing
            number = null; // "Germany" in org.jabref.logic.importer.fileformat.PdfContentImporterTest.extractArXivFromPage
            year = null; // "2408" in org.jabref.logic.importer.fileformat.PdfContentImporterTest.extractArXivFromPage
        }
        if (series != null) {
            entry.setField(StandardField.SERIES, series);
        }
        if (volume != null) {
            entry.setField(StandardField.VOLUME, volume);
        }
        if (number != null) {
            entry.setField(StandardField.NUMBER, number);
        }
        if (pages != null) {
            entry.setField(StandardField.PAGES, pages);
        }
        if (year != null) {
            entry.setField(StandardField.YEAR, year);
        }
        if (publisher != null) {
            entry.setField(StandardField.PUBLISHER, publisher);
        }
        return Optional.of(entry);
    }

    private @Nullable String getDoi(@Nullable String currentDoi) {
        if (currentDoi != null) {
            return currentDoi;
        }

        return DOI.findInText(curString).map(DOI::asString).orElse(null);
    }

    private String getArXivId(String arXivId) {
        if (arXivId != null) {
            return arXivId;
        }

        String arXiv = curString.split(" ")[0];
        arXivId = ArXivIdentifier.parse(arXiv).map(ArXivIdentifier::asString).orElse(null);

        if (arXivId == null || curString.length() < arXivId.length() + ARXIV_PREFIX_LENGTH) {
            return arXivId;
        }

        proceedToNextNonEmptyLine();

        return arXivId;
    }

    /// Extract the year out of curString (if it is not yet defined)
    private void extractYear() {
        if (year != null) {
            return;
        }

        Matcher m = YEAR_EXTRACT_PATTERN.matcher(curString);
        while (m.find()) {
            int extractedYear = Integer.parseInt(m.group());
            // The upper bound tolerates in-press works dated slightly ahead of the current year
            if ((extractedYear >= MINIMUM_PLAUSIBLE_YEAR) && (extractedYear <= Year.now().getValue() + 2)) {
                year = m.group();
                return;
            }
        }
    }

    /// PDFTextStripper normally does NOT produce multiple empty lines
    /// (besides at strange PDFs). These strange PDFs are handled here:
    /// proceed to next non-empty line
    private void proceedToNextNonEmptyLine() {
        while ((lineIndex < lines.length) && lines[lineIndex].isBlank()) {
            lineIndex++;
        }
    }

    /// Fill curString with lines until "" is found
    /// No trailing space is added
    /// i is advanced to the next non-empty line (ignoring white space)
    ///
    /// Lines containing only white spaces are ignored,
    /// but NOT considered as ""
    ///
    /// Uses GLOBAL variables lines, curLine, i
    private void fillCurStringWithNonEmptyLines() {
        // ensure that curString does not end with " "
        curString = curString.trim();
        while ((lineIndex < lines.length) && !"".equals(lines[lineIndex])) {
            String curLine = lines[lineIndex].trim();
            if (!curLine.isEmpty()) {
                if (!curString.isEmpty()) {
                    // insert separating space if necessary
                    curString = curString.concat(" ");
                }
                curString = curString.concat(lines[lineIndex]);
            }
            lineIndex++;
        }

        proceedToNextNonEmptyLine();
    }

    /// resets curString
    /// curString now contains the last block (until "" reached)
    /// Trailing space is added
    ///
    /// invariant before/after: i points to line before the last handled block
    private void readLastBlock() {
        while ((lineIndex >= 0) && lines[lineIndex].isBlank()) {
            lineIndex--;
        }
        // i is now at the end of a block

        int end = lineIndex;

        // find beginning
        while ((lineIndex >= 0) && !"".equals(lines[lineIndex])) {
            lineIndex--;
        }
        // i is now the line before the beginning of the block
        // this fulfills the invariant

        curString = "";
        for (int j = lineIndex + 1; j <= end; j++) {
            curString = curString.concat(lines[j].trim());
            if (j != end) {
                curString = curString.concat(" ");
            }
        }
    }

    /// Extracts the plain text of a PDF's leading pages. Used by [PdfMergeMetadataImporter] to cross-check
    /// author candidates against what the document itself prints. Returns an empty string on failure.
    static String extractLeadingPagesText(PDDocument document) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(Math.min(2, document.getNumberOfPages()));
            return stripper.getText(document);
        } catch (IOException e) {
            LOGGER.debug("Could not extract text for the author plausibility check", e);
            return "";
        }
    }

    /// Office suites store the account name of whoever produced the file as "Author" in the document
    /// information dictionary. Via the XMP/docinfo candidate, this person — usually not an author of the
    /// work at all — would win the merge in [PdfMergeMetadataImporter] against author lists extracted from
    /// the document itself.
    ///
    /// Since scholarly works print their authors on the leading pages, an author list is trusted only if at
    /// least one of its family names occurs in the text of those pages. An unconfirmed merged value is
    /// replaced by the best-confirmed candidate value. If no candidate is confirmed, a single person coming
    /// from a non-bibliographic candidate (no citation key, no explicit entry type) is dropped entirely: a
    /// wrong author is worse than none. Entries with a citation key or explicit type are left untouched so
    /// that metadata previously written by JabRef survives re-import even when the PDF text does not
    /// contain the author (e.g. slides or reports).
    static void crossCheckAuthor(BibEntry entry, List<BibEntry> candidates, String documentText) {
        if (documentText.isBlank()) {
            return;
        }
        entry.getField(StandardField.AUTHOR).ifPresent(mergedAuthor -> {
            String normalizedText = normalizeForComparison(documentText);
            if (isAuthorConfirmedByText(mergedAuthor, normalizedText)) {
                return;
            }

            candidates.stream()
                      .flatMap(candidate -> candidate.getField(StandardField.AUTHOR).stream())
                      .filter(PdfContentImporter::looksLikeAuthorList)
                      .map(author -> new ScoredAuthor(author, countFamilyNamesInText(author, normalizedText)))
                      .filter(scored -> scored.confirmedNames() > 0)
                      // keeps the earlier (= higher-priority) candidate unless a later one is strictly better
                      .reduce((first, second) -> second.confirmedNames() > first.confirmedNames() ? second : first)
                      .ifPresentOrElse(
                              scored -> entry.setField(StandardField.AUTHOR, scored.value()),
                              () -> dropCreatorOnlyAuthor(entry, candidates, mergedAuthor));
        });
    }

    private static void dropCreatorOnlyAuthor(BibEntry entry, List<BibEntry> candidates, String mergedAuthor) {
        // The merge is first-wins, thus the merged value stems from the first candidate carrying an author
        boolean sourceLooksBibliographic = candidates.stream()
                                                     .filter(candidate -> candidate.hasField(StandardField.AUTHOR))
                                                     .findFirst()
                                                     .map(source -> source.getCitationKey().isPresent()
                                                             || !BibEntry.DEFAULT_TYPE.equals(source.getType()))
                                                     .orElse(true);
        if (!sourceLooksBibliographic && (AuthorList.parse(mergedAuthor).getNumberOfAuthors() == 1)) {
            entry.clearField(StandardField.AUTHOR);
        }
    }

    private record ScoredAuthor(String value, int confirmedNames) {
    }

    /// Keeps sentence fragments mis-parsed as author lists (e.g. by this importer's first-page parsing) from
    /// being promoted by the cross-check: their words trivially occur in the document text. Real name lists
    /// consist of capitalized words plus name particles and the "and" separator, while prose contains
    /// other lowercase words and non-letter tokens such as URLs.
    private static boolean looksLikeAuthorList(String authorField) {
        for (String word : WHITESPACE.split(authorField)) {
            String stripped = LEADING_AND_TRAILING_NON_LETTERS.matcher(word).replaceAll("");
            if (stripped.isEmpty()
                    || (!Character.isUpperCase(stripped.codePointAt(0)) && !NAME_LIST_LOWERCASE_WORDS.contains(stripped))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAuthorConfirmedByText(String authorField, String normalizedText) {
        if (countFamilyNamesInText(authorField, normalizedText) > 0) {
            return true;
        }
        // Fallback for author values AuthorList cannot split into proper persons (e.g. exotic separator
        // characters from broken XMP decoding): any word of the raw value found in the text confirms it.
        return Arrays.stream(NON_LETTERS.split(authorField))
                     .filter(word -> word.length() >= 2)
                     .map(PdfContentImporter::normalizeForComparison)
                     .anyMatch(word -> !word.isBlank() && containsWord(normalizedText, word));
    }

    private static int countFamilyNamesInText(String authorField, String normalizedText) {
        return (int) AuthorList.parse(authorField).getAuthors().stream()
                               .map(Author::getFamilyName)
                               .flatMap(Optional::stream)
                               .map(PdfContentImporter::normalizeForComparison)
                               .filter(familyName -> !familyName.isBlank() && containsWord(normalizedText, familyName))
                               .count();
    }

    /// Whole-word check: the occurrence must not be preceded or followed by another letter
    private static boolean containsWord(String normalizedText, String word) {
        int index = normalizedText.indexOf(word);
        while (index >= 0) {
            int end = index + word.length();
            if ((index == 0 || !Character.isLetter(normalizedText.codePointBefore(index)))
                    && (end >= normalizedText.length() || !Character.isLetter(normalizedText.codePointAt(end)))) {
                return true;
            }
            index = normalizedText.indexOf(word, index + 1);
        }
        return false;
    }

    /// Case-, diacritic- and hyphen-insensitive comparison form. Hyphens are removed on both sides because
    /// text extraction may break a name at the end of a justified line ("Breitenbü-\ncher"), where the
    /// hyphen is a soft line-break hyphen for one name but a genuine part of another (e.g. "Kylo-Ren").
    private static String normalizeForComparison(String text) {
        String dehyphenated = SOFT_LINE_BREAK_HYPHEN.matcher(text).replaceAll("").replace("-", "");
        return COMBINING_MARKS.matcher(Normalizer.normalize(dehyphenated, Normalizer.Form.NFKD))
                              .replaceAll("")
                              .toLowerCase(Locale.ROOT);
    }

    @Override
    public String getId() {
        return "pdfContent";
    }

    @Override
    public String getName() {
        return Localization.lang("PDF content");
    }

    @Override
    public String getDescription() {
        return Localization.lang("This importer parses data of the first page of the PDF and creates a BibTeX entry. Currently, Springer and IEEE formats are supported.");
    }
}
