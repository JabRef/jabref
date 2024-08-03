package org.jabref.logic.openoffice.oocsltext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

import org.jabref.model.entry.BibEntry;

import com.sun.star.container.XNamed;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;

public class MarkManager {
    private final HashMap<String, ReferenceMark> marksByName;
    private final ArrayList<ReferenceMark> marksByID;
    private final IdentityHashMap<ReferenceMark, Integer> idsByMark;
    private final XTextDocument document;
    private final XMultiServiceFactory factory;

    public MarkManager(XTextDocument document) throws Exception {
        this.document = document;
        this.factory = UnoRuntime.queryInterface(XMultiServiceFactory.class, document);
        this.marksByName = new HashMap<>();
        this.marksByID = new ArrayList<>();
        this.idsByMark = new IdentityHashMap<>();
    }

    public void renameMark(String oldName, String newName) {
        ReferenceMark mark = marksByName.remove(oldName);
        if (mark != null) {
            marksByName.put(newName, mark);
        }
    }

    public int getIDForMark(ReferenceMark mark) {
        return idsByMark.getOrDefault(mark, -1);
    }

    public ReferenceMark getMarkForID(int id) {
        return (id >= 0 && id < marksByID.size()) ? marksByID.get(id) : null;
    }

    public ReferenceMark getMark(Object mark, String fieldType) {
        if (mark == null) {
            return null;
        }

        XNamed named = UnoRuntime.queryInterface(XNamed.class, mark);
        String name = named.getName();

        ReferenceMark referenceMark = marksByName.get(name);
        if (referenceMark != null) {
            return referenceMark;
        }

        for (String prefix : CSLCitationOOAdapter.PREFIXES) {
            if (name.contains(prefix)) {
                try {
                    referenceMark = new ReferenceMark(document, named, name);
                    marksByName.put(name, referenceMark);
                    idsByMark.put(referenceMark, marksByID.size());
                    marksByID.add(referenceMark);
                    return referenceMark;
                } catch (IllegalArgumentException e) {
                    // Ignore and continue
                }
            }
        }

        return null;
    }

    public ReferenceMark createReferenceMark(BibEntry entry, String fieldType) throws Exception {
        String name = CSLCitationOOAdapter.PREFIXES[0] + entry.getCitationKey().orElse("") + " RND" + CSLCitationOOAdapter.getRandomString(CSLCitationOOAdapter.REFMARK_ADD_CHARS);
        Object mark = factory.createInstance("com.sun.star.text.ReferenceMark");
        XNamed named = UnoRuntime.queryInterface(XNamed.class, mark);
        named.setName(name);

        ReferenceMark referenceMark = new ReferenceMark(document, named, name);
        marksByName.put(name, referenceMark);
        idsByMark.put(referenceMark, marksByID.size());
        marksByID.add(referenceMark);

        return referenceMark;
    }
}
