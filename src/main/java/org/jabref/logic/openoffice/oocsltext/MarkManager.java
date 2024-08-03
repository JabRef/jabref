package org.jabref.logic.openoffice.oocsltext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

import org.jabref.model.entry.BibEntry;

import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XReferenceMarksSupplier;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import org.tinylog.Logger;

public class MarkManager {
    private final HashMap<String, ReferenceMark> marksByName;
    private final ArrayList<ReferenceMark> marksByID;
    private final IdentityHashMap<ReferenceMark, Integer> idsByMark;
    private final XTextDocument document;
    private final XMultiServiceFactory factory;
    private int lastUsedCitationNumber = 0;
    private HashMap<String, Integer> citationKeyToNumber;
    private int highestCitationNumber = 0;

    public MarkManager(XTextDocument document) throws Exception {
        this.document = document;
        this.factory = UnoRuntime.queryInterface(XMultiServiceFactory.class, document);
        this.marksByName = new HashMap<>();
        this.marksByID = new ArrayList<>();
        this.idsByMark = new IdentityHashMap<>();
        this.citationKeyToNumber = new HashMap<>();
    }

    public void readExistingMarks() throws Exception {
        XReferenceMarksSupplier supplier = UnoRuntime.queryInterface(XReferenceMarksSupplier.class, document);
        XNameAccess marks = supplier.getReferenceMarks();
        for (String name : marks.getElementNames()) {
            if (name.startsWith(CSLCitationOOAdapter.PREFIXES[0])) {
                XNamed named = UnoRuntime.queryInterface(XNamed.class, marks.getByName(name));
                ReferenceMark mark = new ReferenceMark(document, named, name);
                addMark(mark);
            }
        }
    }

    public void addMark(ReferenceMark mark) {
        marksByName.put(mark.getName(), mark);
        idsByMark.put(mark, marksByID.size());
        marksByID.add(mark);
        updateCitationInfo(mark.getName());
    }

    private void updateCitationInfo(String name) {
        String[] parts = name.split(" ");
        if (parts.length >= 3) {
            String citationKey = parts[1];
            try {
                int citationNumber = Integer.parseInt(parts[parts.length - 1]);
                citationKeyToNumber.put(citationKey, citationNumber);
                highestCitationNumber = Math.max(highestCitationNumber, citationNumber);
            } catch (NumberFormatException e) {
                Logger.warn("WHat", e);
                // Ignore if we can't parse the number
            }
        }
    }

    public int getCitationNumber(String citationKey) {
        return citationKeyToNumber.computeIfAbsent(citationKey, k -> {
            highestCitationNumber++;
            return highestCitationNumber;
        });
    }

    public ReferenceMark createReferenceMark(BibEntry entry, String fieldType) throws Exception {
        String citationKey = entry.getCitationKey().orElse("");
        int citationNumber = getCitationNumber(citationKey);

        String name = CSLCitationOOAdapter.PREFIXES[0] + citationKey + " RND" + citationNumber;
        Object mark = factory.createInstance("com.sun.star.text.ReferenceMark");
        XNamed named = UnoRuntime.queryInterface(XNamed.class, mark);
        named.setName(name);

        ReferenceMark referenceMark = new ReferenceMark(document, named, name);
        addMark(referenceMark);

        return referenceMark;
    }

    public int getHighestCitationNumber() {
        return highestCitationNumber;
    }

    public void setHighestCitationNumber(int number) {
        this.highestCitationNumber = number;
    }

    public ReferenceMark getMarkByName(String name) {
        return marksByName.get(name);
    }

    public ReferenceMark getMarkByID(int id) {
        return marksByID.get(id);
    }

    public int getIDForMark(ReferenceMark mark) {
        return idsByMark.get(mark);
    }
}
