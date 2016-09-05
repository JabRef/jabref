package net.sf.jabref.logic.pdf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

public class PdfCommentImporter {

    private List pdfPages;
    private PDPage page;

    private Log logger = LogFactory.getLog(PdfCommentImporter.class);

    public PdfCommentImporter() {

    }

    /**
     * Imports the comments from a pdf specified by its URI
     *
     * @param document a PDDocument to get the annotations from
     * @return a hashmap with the unique name as key and the notes content as value
     */
    public HashMap<String, String> importNotes(final PDDocument document) {

        HashMap<String, String> annotationsMap = new HashMap<>();

        pdfPages = document.getDocumentCatalog().getAllPages();
        for (int i = 0; i < pdfPages.size(); i++) {
            page = (PDPage) pdfPages.get(i);
            try {
                for (PDAnnotation annotation : page.getAnnotations()) {
                    annotationsMap.put(annotation.getAnnotationName(), annotation.getContents());
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return annotationsMap;
    }

    public List<PDDocument> importPdfFile(final List<BibEntry> entryList, final BibDatabaseContext bibDatabaseContext) throws IOException {

        final List<File> files = FileUtil.getListOfLinkedFiles(entryList,
                bibDatabaseContext.getFileDirectory(Globals.prefs.getFileDirectoryPreferences()));

        ArrayList<PDDocument> documents = new ArrayList<>();
        for(File linkedFile : files){
            if(linkedFile.getName().toLowerCase().endsWith(".pdf")){
                documents.add(PDDocument.load(linkedFile));
            }
        }
        return documents;
    }
}
