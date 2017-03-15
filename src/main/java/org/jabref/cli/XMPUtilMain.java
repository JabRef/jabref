package org.jabref.cli;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.xml.transform.TransformerException;

import org.jabref.Globals;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.LatexFieldFormatter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.xmp.XMPPreferences;
import org.jabref.logic.xmp.XMPUtil;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import org.apache.jempbox.impl.XMLUtil;
import org.apache.jempbox.xmp.XMPMetadata;

public class XMPUtilMain {

    private XMPUtilMain() {
    }

    /**
     * Command-line tool for working with XMP-data.
     *
     * Read or write XMP-metadata from or to pdf file.
     *
     * Usage:
     * <dl>
     * <dd>Read from PDF and print as bibtex:</dd>
     * <dt>xmpUtil PDF</dt>
     * <dd>Read from PDF and print raw XMP:</dd>
     * <dt>xmpUtil -x PDF</dt>
     * <dd>Write the entry in BIB given by KEY to the PDF:</dd>
     * <dt>xmpUtil KEY BIB PDF</dt>
     * <dd>Write all entries in BIB to the PDF:</dd>
     * <dt>xmpUtil BIB PDF</dt>
     * </dl>
     *
     * @param args
     *            Command line strings passed to utility.
     * @throws IOException
     *             If any of the given files could not be read or written.
     * @throws TransformerException
     *             If the given BibEntry is malformed.
     */
    public static void main(String[] args) throws IOException, TransformerException {

        // Don't forget to initialize the preferences
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }

        XMPPreferences xmpPreferences = Globals.prefs.getXMPPreferences();
        ImportFormatPreferences importFormatPreferences = Globals.prefs.getImportFormatPreferences();

        switch (args.length) {
        case 0:
            usage();
            break;
        case 1:

            if (args[0].endsWith(".pdf")) {
                // Read from pdf and write as BibTex
                List<BibEntry> l = XMPUtil.readXMP(new File(args[0]), xmpPreferences);

                BibEntryWriter bibtexEntryWriter = new BibEntryWriter(
                        new LatexFieldFormatter(Globals.prefs.getLatexFieldFormatterPreferences()), false);

                for (BibEntry entry : l) {
                    StringWriter sw = new StringWriter();
                    bibtexEntryWriter.write(entry, sw, BibDatabaseMode.BIBTEX);
                    System.out.println(sw.getBuffer());
                }

            } else if (args[0].endsWith(".bib")) {
                // Read from BIB and write as XMP
                try (FileReader fr = new FileReader(args[0])) {
                    ParserResult result = new BibtexParser(importFormatPreferences).parse(fr);
                    Collection<BibEntry> entries = result.getDatabase().getEntries();

                    if (entries.isEmpty()) {
                        System.err.println("Could not find BibEntry in " + args[0]);
                    } else {
                        System.out.println(XMPUtil.toXMP(entries, result.getDatabase(), xmpPreferences));
                    }
                }
            } else {
                usage();
            }
            break;
        case 2:
            if ("-x".equals(args[0]) && args[1].endsWith(".pdf")) {
                // Read from pdf and write as BibTex
                Optional<XMPMetadata> meta = XMPUtil.readRawXMP(new File(args[1]));

                if (meta.isPresent()) {
                    XMLUtil.save(meta.get().getXMPDocument(), System.out, StandardCharsets.UTF_8.name());
                } else {
                    System.err.println("The given pdf does not contain any XMP-metadata.");
                }
                break;
            }

            if (args[0].endsWith(".bib") && args[1].endsWith(".pdf")) {
                ParserResult result = new BibtexParser(importFormatPreferences).parse(new FileReader(args[0]));

                Collection<BibEntry> entries = result.getDatabase().getEntries();

                if (entries.isEmpty()) {
                    System.err.println("Could not find BibEntry in " + args[0]);
                } else {
                    XMPUtil.writeXMP(new File(args[1]), entries, result.getDatabase(), false, xmpPreferences);
                    System.out.println("XMP written.");
                }
                break;
            }

            usage();
            break;
        case 3:
            if (!args[1].endsWith(".bib") && !args[2].endsWith(".pdf")) {
                usage();
                break;
            }

            ParserResult result = new BibtexParser(importFormatPreferences).parse(new FileReader(args[1]));

            Optional<BibEntry> bibEntry = result.getDatabase().getEntryByKey(args[0]);

            if (bibEntry.isPresent()) {
                XMPUtil.writeXMP(new File(args[2]), bibEntry.get(), result.getDatabase(), xmpPreferences);

                System.out.println("XMP written.");
            } else {
                System.err.println("Could not find BibEntry " + args[0] + " in " + args[0]);
            }
            break;

        default:
            usage();
        }
    }

    /**
     * Print usage information for the command line tool xmpUtil.
     *
     * @see XMPUtilMain#main(String[])
     */
    private static void usage() {
        System.out.println("Read or write XMP-metadata from or to pdf file.");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("Read from PDF and print as bibtex:");
        System.out.println("  xmpUtil <pdf>");
        System.out.println("Read from PDF and print raw XMP:");
        System.out.println("  xmpUtil -x <pdf>");
        System.out
        .println("Write the entry in <bib> given by <key> to the PDF:");
        System.out.println("  xmpUtil <key> <bib> <pdf>");
        System.out.println("Write all entries in <bib> to the PDF:");
        System.out.println("  xmpUtil <bib> <pdf>");
        System.out.println("");
        System.out
        .println("To report bugs visit https://issues.jabref.org");
    }

}
