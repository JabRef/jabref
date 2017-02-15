package net.sf.jabref.preferences;

import java.util.List;
import java.util.TreeSet;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.importer.Importer;
import net.sf.jabref.logic.importer.fileformat.CustomImporter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Collection of user defined custom import formats.
 *
 * <p>The collection can be stored and retrieved from Preferences. It is sorted by the default
 * order of {@link Importer}.</p>
 */
public class CustomImportList extends TreeSet<CustomImporter> {

    private final JabRefPreferences prefs;

    private static final Log LOGGER = LogFactory.getLog(CustomImportList.class);


    public CustomImportList(JabRefPreferences prefs) {
        super();
        this.prefs = prefs;
        readPrefs();
    }

    private void readPrefs() {
        int i = 0;
        List<String> s;
        while (!((s = prefs.getStringList(JabRefPreferences.CUSTOM_IMPORT_FORMAT + i)).isEmpty())) {
            try {
                if (s.size() == 2) {
                    // New format: basePath, className
                    super.add(new CustomImporter(s.get(0), s.get(1)));
                } else {
                    // Old format: name, cliId, className, basePath
                    super.add(new CustomImporter(s.get(3), s.get(2)));
                }
            } catch (Exception e) {
                LOGGER.warn("Could not load " + s.get(0) + " from preferences. Will ignore.", e);
            }
            i++;
        }
    }

    private void addImporter(CustomImporter customImporter) {
        super.add(customImporter);
    }

    /**
     * Adds an importer.
     *
     * <p>If an old one equal to the new one was contained, the old
     * one is replaced.</p>
     *
     * @param customImporter new (version of an) importer
     * @return  if the importer was contained
     */
    public boolean replaceImporter(CustomImporter customImporter) {
        boolean wasContained = this.remove(customImporter);
        this.addImporter(customImporter);
        return wasContained;
    }

    public void store() {
        purgeAll();
        CustomImporter[] importers = this.toArray(new CustomImporter[this.size()]);
        for (int i = 0; i < importers.length; i++) {
            Globals.prefs.putStringList(JabRefPreferences.CUSTOM_IMPORT_FORMAT + i, importers[i].getAsStringList());
        }
    }

    private void purgeAll() {
        for (int i = 0; !(Globals.prefs.getStringList(JabRefPreferences.CUSTOM_IMPORT_FORMAT + i).isEmpty()); i++) {
            Globals.prefs.remove(JabRefPreferences.CUSTOM_IMPORT_FORMAT + i);
        }
    }
}
