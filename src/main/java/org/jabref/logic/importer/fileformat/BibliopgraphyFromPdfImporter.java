package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.EncryptedPdfsNotSupportedException;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.common.annotations.VisibleForTesting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses the references from the "References" section from a PDF
 * <p>
 * Currently, IEEE two column format is supported.
 * <p>
 */
public class BibliopgraphyFromPdfImporter extends Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibliopgraphyFromPdfImporter.class);

    private static final Pattern REFERENCE_PATTERN = Pattern.compile("\\[(\\d+)\\](.*?)(?=\\[|$)", Pattern.DOTALL);
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;

    public BibliopgraphyFromPdfImporter(CitationKeyPatternPreferences citationKeyPatternPreferences) {
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return input.readLine().startsWith("%PDF");
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        throw new UnsupportedOperationException("BibliopgraphyFromPdfImporter does not support importDatabase(BufferedReader reader)."
                + "Instead use importDatabase(Path filePath).");
    }

    @Override
    public String getName() {
        return "Bibliography from PDF";
    }

    @Override
    public String getDescription() {
        return "Reads the references from the 'References' section of a PDF file.";
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.PDF;
    }

    @Override
    public ParserResult importDatabase(Path filePath) {
        List<BibEntry> result;

        try (PDDocument document = new XmpUtilReader().loadWithAutomaticDecryption(filePath)) {
            String contents = getLastPageContents(document);
            result = getEntriesFromPDFContent(contents);
        } catch (EncryptedPdfsNotSupportedException e) {
            return ParserResult.fromErrorMessage(Localization.lang("Decryption not supported."));
        } catch (IOException exception) {
            return ParserResult.fromError(exception);
        }

        ParserResult parserResult = new ParserResult(result);

        // Generate citation keys for result
        CitationKeyGenerator citationKeyGenerator = new CitationKeyGenerator(parserResult.getDatabaseContext(), citationKeyPatternPreferences);
        parserResult.getDatabase().getEntries().forEach(citationKeyGenerator::generateAndSetKey);

        return parserResult;
    }

    private record IntermediateData(String number, String reference) {
    }

    /**
     * In: "[1] ...\n...\n...[2]...\n...\n...\n[3]..."
     * Out: List<String> = ["[1] ...", "[2]...", "[3]..."]
     */
    private List<BibEntry> getEntriesFromPDFContent(String contents) {
        List<IntermediateData> referencesStrings = new ArrayList<>();
        Matcher matcher = REFERENCE_PATTERN.matcher(contents);
        while (matcher.find()) {
            String reference = matcher.group(2).replaceAll("\\r?\\n", " ").trim();
            referencesStrings.add(new IntermediateData(matcher.group(1), reference));
        }

        return referencesStrings.stream()
                                .map(data -> parseReference(data.number(), data.reference()))
                                .toList();
    }

    private String getLastPageContents(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();

        int lastPage = document.getNumberOfPages();
        stripper.setStartPage(lastPage);
        stripper.setEndPage(lastPage);
        StringWriter writer = new StringWriter();
        stripper.writeText(document, writer);

        return writer.toString();
    }

    /**
     * Example: <code>J. Knaster et al., “Overview of the IFMIF/EVEDA project”, Nucl. Fusion, vol. 57, p. 102016, 2017. doi:10.1088/ 1741-4326/aa6a6a</code>
     *
     * @param number     The number of the reference - used for logging only
     */
    @VisibleForTesting
    BibEntry parseReference(String number, String reference) {
        String originalReference = "[" + number + "] " + reference;
        BibEntry result = new BibEntry(StandardEntryType.Article);

        reference = reference.replace(".-", "-");

        // J. Knaster et al., “Overview of the IFMIF/EVEDA project”, Nucl. Fusion, vol. 57, p. 102016, 2017. doi:10.1088/ 1741-4326/aa6a6a
        // Y. Shimosaki et al., “Lattice design for 5 MeV – 125 mA CW RFQ operation in LIPAc”, in Proc. IPAC’19, Mel- bourne, Australia, May 2019, pp. 977-979. doi:10.18429/ JACoW-IPAC2019-MOPTS051
        int pos = reference.indexOf("doi:");
        if (pos >= 0) {
            String doi = reference.substring(pos + 4).trim();
            doi = doi.replace(" ", "");
            result.setField(StandardField.DOI, doi);
            reference = reference.substring(0, pos).trim();
        }

        // J. Knaster et al., “Overview of the IFMIF/EVEDA project”, Nucl. Fusion, vol. 57, p. 102016, 2017.
        // Y. Shimosaki et al., “Lattice design for 5 MeV – 125 mA CW RFQ operation in LIPAc”, in Proc. IPAC’19, Mel- bourne, Australia, May 2019, pp. 977-979
        Pattern yearAtEnd = Pattern.compile(", (\\d{4})\\.$");
        Matcher matcher = yearAtEnd.matcher(reference);
        if (matcher.find()) {
            result.setField(StandardField.YEAR, matcher.group(1));
            reference = reference.substring(0, matcher.start()).trim();
        }

        Pattern pages = Pattern.compile(", pp\\. (\\d+--?\\d+)\\.?(.*)");
        reference = updateEntryAndReferenceIfMatches(reference, pages, result, StandardField.PAGES);

        // J. Knaster et al., “Overview of the IFMIF/EVEDA project”, Nucl. Fusion, vol. 57, p. 102016
        // Y. Shimosaki et al., “Lattice design for 5 MeV – 125 mA CW RFQ operation in LIPAc”, in Proc. IPAC’19, Mel- bourne, Australia, May 2019
        Pattern page = Pattern.compile(", p\\. (\\d+)(.*)");
        reference = updateEntryAndReferenceIfMatches(reference, page, result, StandardField.PAGES);

        Pattern monthRangeAndYear = Pattern.compile(", ([A-Z][a-z]{2,7}\\.?)-[A-Z][a-z]{2,7}\\.? (\\d+)(.*)");
        matcher = monthRangeAndYear.matcher(reference);
        if (matcher.find()) {
            // strip out second month
            reference = reference.substring(0, matcher.start()) + ", " + matcher.group(1) + " " + matcher.group(2) + matcher.group(3);
        }

        // J. Knaster et al., “Overview of the IFMIF/EVEDA project”, Nucl. Fusion, vol. 57
        // Y. Shimosaki et al., “Lattice design for 5 MeV – 125 mA CW RFQ operation in LIPAc”, in Proc. IPAC’19, Mel- bourne, Australia, May 2019
        Pattern monthAndYear = Pattern.compile(", ([A-Z][a-z]{2,7}\\.? \\d+),? ?(.*)");
        matcher = monthAndYear.matcher(reference);
        if (matcher.find()) {
            Optional<Date> parsedDate = Date.parse(matcher.group(1));
            if (parsedDate.isPresent()) {
                Date date = parsedDate.get();
                date.getYear().ifPresent(year -> result.setField(StandardField.YEAR, year.toString()));
                date.getMonth().ifPresent(month -> result.setField(StandardField.MONTH, month.getJabRefFormat()));

                String prefix = reference.substring(0, matcher.start()).trim();
                String suffix = matcher.group(2);
                if (!suffix.isEmpty() && !".".equals(suffix)) {
                    suffix = ", " + suffix.replaceAll("^\\. ", "");
                } else {
                    suffix = "";
                }
                reference = prefix + suffix;
            }
        }

        // J. Knaster et al., “Overview of the IFMIF/EVEDA project”, Nucl. Fusion, vol. 57
        // Y. Shimosaki et al., “Lattice design for 5 MeV – 125 mA CW RFQ operation in LIPAc”, in Proc. IPAC’19, Mel- bourne, Australia
        Pattern volume = Pattern.compile(", vol\\. (\\d+)(.*)");
        reference = updateEntryAndReferenceIfMatches(reference, volume, result, StandardField.VOLUME);

        Pattern no = Pattern.compile(", no\\. (\\d+)(.*)");
        reference = updateEntryAndReferenceIfMatches(reference, no, result, StandardField.NUMBER);

        // J. Knaster et al., “Overview of the IFMIF/EVEDA project”, Nucl. Fusion
        // Y. Shimosaki et al., “Lattice design for 5 MeV – 125 mA CW RFQ operation in LIPAc”, in Proc. IPAC’19, Mel- bourne, Australia
        Pattern authorsAndTitleAtBeginning = Pattern.compile("^([^“]+), “(.*?)”, ");
        matcher = authorsAndTitleAtBeginning.matcher(reference);
        if (matcher.find()) {
            result.setField(StandardField.AUTHOR, matcher.group(1)
                                                         .replace("- ", "")
                                                         .replaceAll("et al\\.?", "and others"));
            result.setField(StandardField.TITLE, matcher.group(2)
                                                         .replace("- ", "")
                                                         .replaceAll("et al\\.?", "and others"));
            reference = reference.substring(matcher.end()).trim();
        } else {
            // No authors present
            // Example: “AF4.1.1 SRF Linac Engineering Design Report”, Internal note.
            Pattern title = Pattern.compile("“(.*?)”, (.*)");
            reference = updateEntryAndReferenceIfMatches(reference, title, result, StandardField.TITLE);
        }

        // Nucl. Fusion
        // in Proc. IPAC’19, Mel- bourne, Australia
        // presented at th 8th DITANET Topical Workshop on Beam Position Monitors, CERN, Geneva, Switzreland
        List<String> stringsToRemove = List.of("presented at", "to be presented at");
        // need to use "iterator()" instead of "stream().foreach", because "reference" is modified inside the loop
        Iterator<String> iterator = stringsToRemove.iterator();
        while (iterator.hasNext()) {
            String check = iterator.next();
            if (reference.startsWith(check)) {
                reference = reference.substring(check.length()).trim();
                result.setType(StandardEntryType.InProceedings);
            }
        }

        boolean startsWithInProc = reference.startsWith("in Proc.");
        boolean conainsWorkshop = reference.contains("Workshop");
        if (startsWithInProc || conainsWorkshop) {
            int beginIndex = startsWithInProc ? 3 : 0;
            result.setField(StandardField.BOOKTITLE, reference.substring(beginIndex).replace("- ", "").trim());
            result.setType(StandardEntryType.InProceedings);
            reference = "";
        }

        // Nucl. Fusion
        reference = reference.trim()
                             .replace("- ", "")
                             .replaceAll("\\.$", "");
        if (!reference.contains(",") && !reference.isEmpty()) {
            if (reference.endsWith(" Note") || reference.endsWith(" note")) {
                result.setField(StandardField.NOTE, reference);
                result.setType(StandardEntryType.TechReport);
            } else {
                result.setField(StandardField.JOURNAL, reference.replace("- ", ""));
            }
            reference = "";
        } else {
            String toAdd = reference;
            result.setType(StandardEntryType.InProceedings);
            if (result.hasField(StandardField.BOOKTITLE)) {
                String oldTitle = result.getField(StandardField.BOOKTITLE).get();
                result.setField(StandardField.BOOKTITLE, oldTitle + toAdd);
            } else {
                result.setField(StandardField.BOOKTITLE, toAdd);
            }
            reference = "";
            LOGGER.debug("InProceedings fallback used for current state of handled string {}", reference);
        }

        if (reference.isEmpty()) {
            result.setField(StandardField.COMMENT, originalReference);
        } else {
            result.setField(StandardField.COMMENT, "Unprocessed: " + reference + "\n\n" + originalReference);
        }
        return result;
    }

    /**
     * @param pattern A pattern matching two groups: The first one to take, the second one to leave at the end of the string
     */
    private static String updateEntryAndReferenceIfMatches(String reference, Pattern pattern, BibEntry result, Field field) {
        Matcher matcher;
        matcher = pattern.matcher(reference);
        if (matcher.find()) {
            result.setField(field, matcher.group(1).replace("- ", ""));
            String suffix = matcher.group(2);
            if (!suffix.isEmpty()) {
                suffix = " " + suffix;
            }
            reference = reference.substring(0, matcher.start()).trim() + suffix;
        }
        return reference;
    }
}
