package org.jabref.logic.importer.plaincitation;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.xmp.XmpUtilReader;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferencesBlockFromPdfFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferencesBlockFromPdfFinder.class);
    private static final Pattern REFERENCES = Pattern.compile("References", Pattern.CASE_INSENSITIVE);

    public static String getReferencesPagesText(Path filePath) throws IOException {
        try (PDDocument document = new XmpUtilReader().loadWithAutomaticDecryption(filePath)) {
            return getReferencesPagesText(document);
        }
    }

    /**
     * Extracts the text from all pages containing references. It simply goes from the last page backwards until there is probably no reference anymore.
     */
    private static String getReferencesPagesText(PDDocument document) throws IOException {
        int lastPage = document.getNumberOfPages();
        String result = prependToResult("", document, new PDFTextStripper(), lastPage);

        // Same matcher uses as in {@link containsWordReferences}
        Matcher matcher = ReferencesBlockFromPdfFinder.REFERENCES.matcher(result);
        if (!matcher.find()) {
            // Ensure that not too much is returned
            LOGGER.warn("Could not found 'References'. Returning last page only.");
            return getPageContents(document, new PDFTextStripper(), lastPage);
        }

        int end = matcher.end();
        return result.substring(end);
    }

    private static String prependToResult(String currentText, PDDocument document, PDFTextStripper stripper, int pageNumber) throws IOException {
        String pageContents = getPageContents(document, stripper, pageNumber);
        String result = pageContents + currentText;
        if (!ReferencesBlockFromPdfFinder.containsWordReferences(pageContents) && (pageNumber > 0)) {
            return prependToResult(result, document, stripper, pageNumber - 1);
        }
        return result;
    }

    private static String getPageContents(PDDocument document, PDFTextStripper stripper, int lastPage) throws IOException {
        stripper.setStartPage(lastPage);
        stripper.setEndPage(lastPage);
        StringWriter writer = new StringWriter();
        stripper.writeText(document, writer);
        return writer.toString();
    }

    private static boolean containsWordReferences(String result) {
        Matcher matcher = ReferencesBlockFromPdfFinder.REFERENCES.matcher(result);
        return matcher.find();
    }
}
