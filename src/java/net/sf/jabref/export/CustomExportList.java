package net.sf.jabref.export;

import java.util.TreeSet;
import java.util.Comparator;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

/**
* This class handles user defined custom export formats. They are initially read from Preferences,
* and kept alphabetically (sorted by name). Formats can be added or removed. When modified, the
* sort() method must be called to make sure the formats stay properly sorted.
* When the method store() is called, export formats are written to Preferences.
*/

public class CustomExportList extends TreeSet {

  private Object[] array;
  JabRefPreferences prefs;
//  ExportComparator comp = new ExportComparator();

  public CustomExportList(JabRefPreferences prefs_, Comparator comp) {
    super(comp);
    //super(new ExportComparator());
    prefs = prefs_;
    readPrefs();
    sort();
  }



  private void readPrefs() {
    int i=0;
    String[] s = null;
    while ((s = prefs.getStringArray("customExportFormat"+i)) != null) {
      super.add(s);
      i++;
    }
  }

  public String[] getElementAt(int pos) {
    return (String[])(array[pos]);
  }

  public void addFormat(String[] s) {
    super.add(s);
    sort();
  }

  public void remove(int pos) {
    super.remove(array[pos]);
    sort();
  }

  public void sort() {
    array = toArray();
  }

  public void store() {

    if (array.length == 0)
      purge(0);
    else {
      for (int i=0; i<array.length; i++) {
        //System.out.println(i+"..");
        Globals.prefs.putStringArray("customExportFormat"+i, (String[])(array[i]));
      }
      purge(array.length);
    }
  }

  private void purge(int from) {
    String[] s = null;
    int i = from;
    while ((s = Globals.prefs.getStringArray("customExportFormat"+i)) != null) {
      Globals.prefs.remove("customExportFormat"+i);
      i++;
    }
  }

}
