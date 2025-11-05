package org.jabref.logic.importer.plaincitation;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.util.PdfUtils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferencesBlockFromPdfFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferencesBlockFromPdfFinder.class);
    private static final Pattern REFERENCES = Pattern.compile("References", Pattern.CASE_INSENSITIVE);

    /**
     * Extracts the text from all pages containing references. It simply goes from the last page backwards until there is probably no reference anymore.
     */
    public static String getReferencesPagesText(PDDocument document) throws IOException {
        int lastPage = document.getNumberOfPages();
        String result = prependToResult("", document, lastPage);

        // Same matcher uses as in {@link containsWordReferences}
        Matcher matcher = ReferencesBlockFromPdfFinder.REFERENCES.matcher(result);
        if (!matcher.find()) {
            // Ensure that not too much is returned
            LOGGER.warn("Could not found 'References'. Returning last page only.");
            return PdfUtils.getPageContents(document, lastPage);
        }

        int end = matcher.end();
        return result.substring(end);
    }

    private static String prependToResult(String currentText, PDDocument document, int pageNumber) throws IOException {
        String pageContents = PdfUtils.getPageContents(document, pageNumber);
        String result = pageContents + currentText;
        if (!ReferencesBlockFromPdfFinder.containsWordReferences(pageContents) && (pageNumber > 0)) {
            return prependToResult(result, document, pageNumber - 1);
        }
        return result;
    }

    private static boolean containsWordReferences(String result) {
        Matcher matcher = ReferencesBlockFromPdfFinder.REFERENCES.matcher(result);
        return matcher.find();
    }
}
