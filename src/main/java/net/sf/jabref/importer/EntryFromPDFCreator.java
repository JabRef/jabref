package net.sf.jabref.importer;

import java.io.File;
import java.util.Optional;

import net.sf.jabref.JabRefGUI;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypes;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.pdfimport.PdfImporter;
import net.sf.jabref.pdfimport.PdfImporter.ImportPdfFilesResult;

/**
 * Uses XMPUtils to get one BibEntry for a PDF-File.
 * Also imports the non-XMP Data (PDDocument-Information) using XMPUtil.getBibtexEntryFromDocumentInformation.
 * If data from more than one entry is read by XMPUtil then this entys are merged into one.
 * @author Dan
 * @version 12.11.2008 | 22:12:48
 *
 */
public class EntryFromPDFCreator extends EntryFromFileCreator {

    public EntryFromPDFCreator() {
        super(EntryFromPDFCreator.getPDFExternalFileType());
    }

    private static ExternalFileType getPDFExternalFileType() {
        Optional<ExternalFileType> pdfFileType = ExternalFileTypes.getInstance().getExternalFileTypeByExt("pdf");
        if (!pdfFileType.isPresent()) {
            return new ExternalFileType("PDF", "pdf", "application/pdf", "evince", "pdfSmall",
                    IconTheme.JabRefIcon.PDF_FILE.getSmallIcon());
        }
        return pdfFileType.get();
    }

    /**
     * Accepts all Files having as suffix ".PDF" (in ignore case mode).
     */
    @Override
    public boolean accept(File f) {
        return (f != null) && f.getName().toUpperCase().endsWith(".PDF");
    }

    @Override
    protected Optional<BibEntry> createBibtexEntry(File pdfFile) {

        if (!accept(pdfFile)) {
            return Optional.empty();
        }

        PdfImporter pi = new PdfImporter(JabRefGUI.getMainFrame(), JabRefGUI.getMainFrame().getCurrentBasePanel(),
                JabRefGUI.getMainFrame().getCurrentBasePanel().getMainTable(), -1);

        String[] fileNames = {pdfFile.toString()};
        ImportPdfFilesResult res = pi.importPdfFiles(fileNames);
        if (res.getEntries().size() == 1) {
            return Optional.of(res.getEntries().get(0));
        } else {
            return Optional.empty();
        }

        /*addEntryDataFromPDDocumentInformation(pdfFile, entry);
        addEntryDataFromXMP(pdfFile, entry);

        if (entry.getField("title") == null) {
        	entry.setField("title", pdfFile.getName());
        }

        return entry;*/
    }

    @Override
    public String getFormatName() {
        return "PDF";
    }

}
