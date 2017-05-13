import java.io.*;
import java.util.*;

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fileformat.ImportFormat;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

// Make sure this is up to date with src/test/resources/net/sf/jabref/importer/fileformat/SimpleCSVImporter.java
// Different names are needed to not cause any confusion(?)

public class CustomImportExample extends ImportFormat {

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
