// This is the example used in the custom import format help page and should reside in test
// Make sure this is up to date with the help page and /src/main/nonpackagejava (which is used for testing custom import)
// Skip the package line when updating the help page

package net.sf.jabref.importer.fileformat;

import java.io.*;
import java.util.*;

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fileformat.ImportFormat;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

public class SimpleCSVImporter extends ImportFormat {

    @Override
    public String getFormatName() {
        return "Simple CSV Importer";
    }

    @Override
    public boolean isRecognizedFormat(InputStream stream) throws IOException {
        return true; // this is discouraged except for demonstration purposes
    }

    @Override
    public List<BibEntry> importEntries(InputStream stream, OutputPrinter printer) throws IOException {
        List<BibEntry> bibitems = new ArrayList<>();
        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));

        String line = in.readLine();
        while (line != null) {
            if (!line.trim().isEmpty()) {
                String[] fields = line.split(";");
                BibEntry be = new BibEntry();
                be.setType(BibtexEntryTypes.TECHREPORT);
                be.setField("year", fields[0]);
                be.setField("author", fields[1]);
                be.setField("title", fields[2]);
                bibitems.add(be);
                line = in.readLine();
            }
        }
        return bibitems;
    }
}
