import java.io.*;
import java.util.*;
import net.sf.jabref.*;
import net.sf.jabref.imports.ImportFormat;
import net.sf.jabref.imports.ImportFormatReader;

public class SimpleCsvImporter extends ImportFormat {

  public String getFormatName() {
    return "Simple CSV Importer";
  }

  public boolean isRecognizedFormat(InputStream stream) throws IOException {
    return true; // this is discouraged except for demonstration purposes
  }
  
  public List importEntries(InputStream stream) throws IOException {    
    ArrayList bibitems = new ArrayList();
    BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
      
    String line = in.readLine();
    while (line != null) {
      if (!"".equals(line.trim())) {
        String[] fields = line.split(";");
        BibtexEntry be = new BibtexEntry(Util.createNeutralId());
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
