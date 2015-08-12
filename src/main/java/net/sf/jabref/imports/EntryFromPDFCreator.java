/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.imports;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import spl.PdfImporter;
import spl.PdfImporter.ImportPdfFilesResult;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.util.EncryptionNotSupportedException;
import net.sf.jabref.util.XMPUtil;

/**
 * Uses XMPUtils to get one BibtexEntry for a PDF-File. 
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
        ExternalFileType pdfFileType = JabRefPreferences.getInstance().getExternalFileTypeByExt("pdf");
        if (pdfFileType == null) {
            return new ExternalFileType("PDF", "pdf", "application/pdf", "evince", "pdfSmall");
        }
        return pdfFileType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.jabref.imports.EntryFromFileCreator#accept(java.io.File)
     * 
     * Accepts all Files having as suffix ".PDF" (in ignore case mode).
     */
    @Override
    public boolean accept(File f) {
        return f != null && f.getName().toUpperCase().endsWith(".PDF");
    }

    @Override
    protected BibtexEntry createBibtexEntry(File pdfFile) {

        if (!accept(pdfFile)) {
            return null;
        }

        PdfImporter pi = new PdfImporter(JabRef.jrf, JabRef.jrf.basePanel(), JabRef.jrf.basePanel().mainTable, -1);
        String[] fileNames = {pdfFile.toString()};
        ImportPdfFilesResult res = pi.importPdfFiles(fileNames, JabRef.jrf);
        assert res.entries.size() == 1;
        return res.entries.get(0);

        /*addEntryDataFromPDDocumentInformation(pdfFile, entry);
        addEntyDataFromXMP(pdfFile, entry);

        if (entry.getField("title") == null) {
        	entry.setField("title", pdfFile.getName());
        }

        return entry;*/
    }

    /** Adds entry data read from the PDDocument information of the file.
     * @param pdfFile
     * @param entry
     */
    private void addEntryDataFromPDDocumentInformation(File pdfFile, BibtexEntry entry) {
        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFile.getAbsoluteFile());
            PDDocumentInformation pdfDocInfo = document
                    .getDocumentInformation();

            if (pdfDocInfo != null) {
                BibtexEntry entryDI = XMPUtil.getBibtexEntryFromDocumentInformation(document
                        .getDocumentInformation());
                if (entryDI != null) {
                    addEntryDataToEntry(entry, entryDI);
                    Calendar creationDate = pdfDocInfo.getCreationDate();
                    if (creationDate != null) {
                        String date = new SimpleDateFormat("yyyy.MM.dd")
                                .format(creationDate.getTime());
                        appendToField(entry, "timestamp", date);
                    }

                    if (pdfDocInfo.getCustomMetadataValue("bibtex/bibtexkey") != null) {
                        entry.setId(pdfDocInfo
                                .getCustomMetadataValue("bibtex/bibtexkey"));
                    }
                }
            }
        } catch (IOException e) {
            // no canceling here, just no data added.
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    // no canceling here, just no data added.
                }
            }
        }
    }

    /**
     * Adds all data Found in all the entrys of this XMP file to the given
     * entry. This was implemented without having much knowledge of the XMP
     * format.
     * 
     * @param aFile
     * @param entry
     */
    private void addEntyDataFromXMP(File aFile, BibtexEntry entry) {
        try {
            List<BibtexEntry> entrys = XMPUtil.readXMP(aFile.getAbsoluteFile());
            addEntrysToEntry(entry, entrys);
        } catch (EncryptionNotSupportedException e) {
            // no canceling here, just no data added.
        } catch (IOException e) {
            // no canceling here, just no data added.
        }
    }

    @Override
    public String getFormatName() {
        return "PDF";
    }

}
