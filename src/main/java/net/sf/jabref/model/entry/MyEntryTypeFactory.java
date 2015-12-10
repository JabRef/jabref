package net.sf.jabref.model.entry;

import net.sf.jabref.JabRefPreferences;

public class MyEntryTypeFactory {

    public MyEntryTypeFactory(JabRefPreferences preferences) {

    }

    public EntryType getTypeFor(MyEntryClass myClass) {
        // if biblatex mode
        if (myClass == MyStandardEntryClass.ARTICLE) {
            return BibLatexEntryTypes.ARTICLE;
        }

        // be more intelligent than that...
        return null;
    }
}
