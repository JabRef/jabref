package org.jabref.cli;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.jabref.Globals;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.LatexFieldFormatter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.ParserResult;

public class XmpUtilMain {

    private static XmpPreferences xmpPreferences;
    private static ImportFormatPreferences importFormatPreferences;

    private XmpUtilMain() {
    }

    /**
     * Reads metadata from pdf and print all included bib entries to the console.
     *
     * @param filename Filename of the pdf file (.pdf)
     */
    private static void readPdfAndPrintBib(String filename) throws IOException {
        if (filename.endsWith(".pdf")) {
            List<BibEntry> entryList = XmpUtilReader.readXmp(filename, xmpPreferences);

            BibEntryWriter bibtexEntryWriter = new BibEntryWriter(
                    new LatexFieldFormatter(Globals.prefs.getLatexFieldFormatterPreferences()), false);

            for (BibEntry entry : entryList) {
                StringWriter writer = new StringWriter();
                bibtexEntryWriter.write(entry, writer, BibDatabaseMode.BIBTEX);
                System.out.println(writer.getBuffer());
            }
        } else {
            System.err.println("Insert a file path (.pdf)");
        }
    }

    /**
     * Writes all entries included in the bib file to the metadata section of the pdf file.
     *
     * @param bibFile Filename of the bib file (.bib)
     * @param pdfFile Filename of the pdf file (.pdf)
     */
    private static void writeBibFileToPdfMetadata(String bibFile, String pdfFile) throws FileNotFoundException, IOException, TransformerException {
        if (bibFile.endsWith(".bib") && pdfFile.endsWith(".pdf")) {
            try (FileReader reader = new FileReader(bibFile)) {
                ParserResult result = new BibtexParser(importFormatPreferences, Globals.getFileUpdateMonitor()).parse(reader);
                XmpUtilWriter.writeXmp(Paths.get(pdfFile), result.getDatabase().getEntries(), result.getDatabase(), xmpPreferences);
                System.out.println("Metadata sucessfully written to Pdf.");
            }
        } else {
            System.err.println("Insert correct file paths (.bib and .pdf)");
        }
    }

    /**
     * Print usage information for the console tool xmpUtil.
     */
    private static void printMenu() {
        System.out.println("---------------------Menu-----------------------");
        System.out.println("(0) Exit");
        System.out.println("(1) Read metadata from PDF and print as bibtex");
        System.out.println("(2) Write entries in bib file to Pdf metadata");
        System.out.println("    To report bugs visit https://issues.jabref.org");
        System.out.println("-------------------------------------------------");
        System.out.print("Choose an option: ");
    }

    /**
     * The tool is implemented as a console application with a read-evaluate-print cycle.
     *
     * @param args
     */
    public static void main(String[] args) {

        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }

        xmpPreferences = Globals.prefs.getXMPPreferences();
        importFormatPreferences = Globals.prefs.getImportFormatPreferences();

        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

        int option = -1;
        while (option != 0) {
            try {
                XmpUtilMain.printMenu();
                option = Integer.parseInt(consoleReader.readLine());

                if (option == 0) {
                    break;
                } else if (option == 1) {
                    System.out.print("Insert your filename (.pdf): ");
                    String filename = consoleReader.readLine().trim();
                    XmpUtilMain.readPdfAndPrintBib(filename);
                } else if (option == 2) {
                    System.out.print("Insert your filename (.bib): ");
                    String bibFile = consoleReader.readLine().trim();
                    System.out.print("Insert your filename (.pdf): ");
                    String pdfFile = consoleReader.readLine().trim();
                    XmpUtilMain.writeBibFileToPdfMetadata(bibFile, pdfFile);
                }
            } catch (IOException | TransformerException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
