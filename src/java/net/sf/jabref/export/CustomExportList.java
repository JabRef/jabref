package net.sf.jabref.export;

import java.util.TreeSet;
import java.util.Comparator;
import java.util.TreeMap;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.BasicEventList;

/**
 * This class handles user defined custom export formats. They are initially
 * read from Preferences, and kept alphabetically (sorted by name). Formats can
 * be added or removed. When modified, the sort() method must be called to make
 * sure the formats stay properly sorted. When the method store() is called,
 * export formats are written to Preferences.
 */

public class CustomExportList {

    private EventList<String[]> list;
    private SortedList<String[]> sorted;
    private TreeMap<String, ExportFormat> formats = new TreeMap<String, ExportFormat>();
	private Object[] array;

    public CustomExportList(Comparator<String[]> comp) {
	    list = new BasicEventList<String[]>();
        sorted = new SortedList<String[]>(list, comp);
    }

	public TreeMap<String, ExportFormat> getCustomExportFormats() {
        formats.clear();
        readPrefs();
        return formats;
	}

    public int size() {
        return list.size();
    }

    public EventList<String[]> getSortedList() {
        return sorted;
    }

	private void readPrefs() {
        formats.clear();
        list.clear();
        int i = 0;
		String[] s;
		while ((s = Globals.prefs.getStringArray("customExportFormat" + i)) != null) {
            ExportFormat format = createFormat(s);
            if (format != null) {
                formats.put(format.getConsoleName(), format);
    			list.add(s);
            } else {
                System.out.println(Globals.lang("Error initializing custom export format from string '%0'",
                    Globals.prefs.get("customExportFormat" + i)));
            }
			i++;
		}
	}

    private ExportFormat createFormat(String[] s) {
        if (s.length < 3)
            return null;
		String lfFileName;
		if (s[1].endsWith(".layout"))
			lfFileName = s[1].substring(0, s[1].length() - 7);
		else
			lfFileName = s[1];
		ExportFormat format = new ExportFormat(s[0], s[0], lfFileName, null,
			s[2]);
		format.setCustomExport(true);
		return format;
	}

	public String[] getElementAt(int pos) {
		return (String[]) (array[pos]);
	}

	public void addFormat(String[] s) {
		list.add(s);
		ExportFormat format = createFormat(s);
		formats.put(format.getConsoleName(), format);
	}

	public void remove(String[] toRemove) {

        ExportFormat format = createFormat(toRemove);
        formats.remove(format.getConsoleName());
        list.remove(toRemove);
        
	}

	public void store() {

		if (list.size() == 0)
			purge(0);
		else {
			for (int i = 0; i < list.size(); i++) {
				// System.out.println(i+"..");
				Globals.prefs.putStringArray("customExportFormat" + i,
					list.get(i));
			}
			purge(list.size());
		}
	}

	private void purge(int from) {
		int i = from;
		while (Globals.prefs.getStringArray("customExportFormat" + i) != null) {
			Globals.prefs.remove("customExportFormat" + i);
			i++;
		}
	}

}
