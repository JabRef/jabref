package net.sf.jabref;

import java.util.*;

/**
 * Class for holding the information about customizable entry editor tabs.
 */
public final class EntryEditorTabList {

    private List list = null;
    private List names = null;

    public EntryEditorTabList() {
	init();
    }

    private void init() {
	list = new ArrayList();
	names = new ArrayList();
	int i=0;
	String name=null;
	String[] fields=null;
	while ((name=Globals.prefs.get(Globals.prefs.CUSTOM_TAB_NAME+i)) != null) {

	    fields = Globals.prefs.get(Globals.prefs.CUSTOM_TAB_FIELDS+i).split(";");
	    List entry = Arrays.asList(fields);
	    names.add(name);
	    list.add(entry);
	    i++;
	}
	
    }

    public int getTabCount() {
	return list.size();
    }

    public String getTabName(int tab) {
	return (String)names.get(tab);
    }

    public List getTabFields(int tab) {
	return (List)list.get(tab);
    }
}
