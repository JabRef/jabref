package net.sf.jabref.model.bibtexkeypattern;

import java.util.List;

import net.sf.jabref.preferences.JabRefPreferences;

public class DatabaseBibtexKeyPattern extends AbstractBibtexKeyPattern {

    private final JabRefPreferences prefs;


    public DatabaseBibtexKeyPattern(JabRefPreferences prefs) {
        this.prefs = prefs;
    }

    @Override
    public List<String> getLastLevelBibtexKeyPattern(String key) {
        return prefs.getKeyPattern().getValue(key);
    }

}
