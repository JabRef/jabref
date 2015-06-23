import java.io.*;
import java.util.*;
import net.sf.jabref.*;
import net.sf.jabref.IdGenerator;
import net.sf.jabref.imports.ImportFormat;
import net.sf.jabref.imports.ImportFormatReader;

public class SimpleCsvImporter extends ImportFormat {

  @Override
public String getFormatName() {
    return "Simple CSV Importer";
  }

  @Override
public boolean isRecognizedFormat(InputStream stream) throws IOException {
    return true; // this is discouraged except for demonstration purposes
  }

    @Override
    public List<BibtexEntry> importEntries(InputStream in, OutputPrinter status) throws IOException {
        // MUST BE IMPLEMENTED
        return null;
    }

    public List<BibtexEntry> importEntries(InputStream stream) throws IOException {
    ArrayList<BibtexEntry> bibitems = new ArrayList<BibtexEntry>();
    BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
      
    String line = in.readLine();
    while (line != null) {
      if (!"".equals(line.trim())) {
        String[] fields = line.split(";");
        BibtexEntry be = new BibtexEntry(IdGenerator.next());
        be.setType(BibtexEntryType.getType("techreport"));
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
