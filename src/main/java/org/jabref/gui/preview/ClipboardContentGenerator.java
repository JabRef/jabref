package org.jabref.gui.preview;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.input.ClipboardContent;

import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.os.OS;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.annotations.VisibleForTesting;

public class ClipboardContentGenerator {

    private PreviewPreferences previewPreferences;
    private final LayoutFormatterPreferences layoutFormatterPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;

    public ClipboardContentGenerator(PreviewPreferences previewPreferences,
                                     LayoutFormatterPreferences layoutFormatterPreferences,
                                     JournalAbbreviationRepository abbreviationRepository) {
        this.previewPreferences = previewPreferences;
        this.layoutFormatterPreferences = layoutFormatterPreferences;
        this.abbreviationRepository = abbreviationRepository;
    }

    public ClipboardContent generateCitation(List<BibEntry> selectedEntries, CitationStyleOutputFormat outputFormat, BibDatabaseContext bibDatabaseContext) throws IOException {
        List<String> citations = generateCitations(selectedEntries, outputFormat, bibDatabaseContext);
        PreviewLayout previewLayout = previewPreferences.getSelectedPreviewLayout();

        // if it is not a citation style take care of the preview
        if (!(previewLayout instanceof CitationStylePreviewLayout)) {
            return processPreview(citations);
        } else {
            // if it's generated by a citation style take care of each output format
            ClipboardContent content;
            return switch (outputFormat) {
                case HTML -> processHtml(citations);
                case TEXT -> processText(citations);
            };
        }
    }

    private List<String> generateCitations(List<BibEntry> selectedEntries, CitationStyleOutputFormat outputFormat, BibDatabaseContext bibDatabaseContext) throws IOException {
        // This worker stored the style as filename. The CSLAdapter and the CitationStyleCache store the source of the
        // style. Therefore, we extract the style source from the file.
        String styleSource = null;
        PreviewLayout previewLayout = previewPreferences.getSelectedPreviewLayout();

        if (previewLayout instanceof CitationStylePreviewLayout citationStyleLayout) {
            styleSource = citationStyleLayout.getText();
        }

        if (styleSource != null) {
            return CitationStyleGenerator.generateBibliographies(
                    selectedEntries,
                    styleSource,
                    outputFormat,
                    bibDatabaseContext,
                    Injector.instantiateModelOrService(BibEntryTypesManager.class));
        } else {
            return generateTextBasedPreviewLayoutCitations(selectedEntries, bibDatabaseContext);
        }
    }

    /**
     * Generates a plain text string out of the preview (based on {@link org.jabref.logic.layout.TextBasedPreviewLayout} or {@link org.jabref.logic.bst.BstPreviewLayout})
     * and copies it additionally to the html to the clipboard (WYSIWYG Editors use the HTML, plain text editors the text)
     */
    @VisibleForTesting
    static ClipboardContent processPreview(List<String> citations) {
        ClipboardContent content = new ClipboardContent();
        content.putHtml(String.join(CitationStyleOutputFormat.HTML.getLineSeparator(), citations));
        content.putString(String.join(CitationStyleOutputFormat.HTML.getLineSeparator(), citations));
        return content;
    }

    /**
     * Joins every citation with a newline and returns it.
     */
    @VisibleForTesting
    static ClipboardContent processText(List<String> citations) {
        ClipboardContent content = new ClipboardContent();
        content.putString(String.join(CitationStyleOutputFormat.TEXT.getLineSeparator(), citations));
        return content;
    }

    /**
     * Inserts each citation into a HTML body and copies it to the clipboard.
     * The given preview is based on {@link org.jabref.logic.citationstyle.CitationStylePreviewLayout}.
     */
    @VisibleForTesting
    static ClipboardContent processHtml(List<String> citations) {
        String result = "<!DOCTYPE html>" + OS.NEWLINE +
                "<html>" + OS.NEWLINE +
                "   <head>" + OS.NEWLINE +
                "      <meta charset=\"utf-8\">" + OS.NEWLINE +
                "   </head>" + OS.NEWLINE +
                "   <body>" + OS.NEWLINE + OS.NEWLINE;

        result += String.join(CitationStyleOutputFormat.HTML.getLineSeparator(), citations);
        result += OS.NEWLINE +
                "   </body>" + OS.NEWLINE +
                "</html>" + OS.NEWLINE;

        ClipboardContent content = new ClipboardContent();
        content.putString(result);
        content.putHtml(result);
        return content;
    }

    private List<String> generateTextBasedPreviewLayoutCitations(List<BibEntry> selectedEntries, BibDatabaseContext bibDatabaseContext) throws IOException {
        TextBasedPreviewLayout customPreviewLayout = previewPreferences.getCustomPreviewLayout();
        StringReader customLayoutReader = new StringReader(customPreviewLayout.getText().replace("__NEWLINE__", "\n"));
        Layout layout = new LayoutHelper(customLayoutReader, layoutFormatterPreferences, abbreviationRepository).getLayoutFromText();
        List<String> citations = new ArrayList<>(selectedEntries.size());
        for (BibEntry entry : selectedEntries) {
            citations.add(layout.doLayout(entry, bibDatabaseContext.getDatabase()));
        }
        return citations;
    }
}
