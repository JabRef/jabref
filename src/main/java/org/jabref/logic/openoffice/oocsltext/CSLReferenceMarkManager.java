package org.jabref.logic.openoffice.oocsltext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.BibEntry;

import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XReferenceMarksSupplier;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;

public class CSLReferenceMarkManager {
    private final HashMap<String, CSLReferenceMark> marksByName;
    private final ArrayList<CSLReferenceMark> marksByID;
    private final IdentityHashMap<CSLReferenceMark, Integer> idsByMark;
    private final XTextDocument document;
    private final XMultiServiceFactory factory;
    private HashMap<String, Integer> citationKeyToNumber;
    private int highestCitationNumber = 0;

    public CSLReferenceMarkManager(XTextDocument document) throws Exception {
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
                CSLReferenceMark mark = new CSLReferenceMark(document, named, name);
                addMark(mark);
                updateCitationInfo(name);
            }
        }
    }

    private void updateCitationInfo(String name) {
        Pattern pattern = Pattern.compile("JABREF_(.+) RND(\\d+)"); // Format: JABREF_{citationKey} RND{citationNumber}
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            String citationKey = matcher.group(1);
            int citationNumber = Integer.parseInt(matcher.group(2));
            citationKeyToNumber.put(citationKey, citationNumber);
            highestCitationNumber = Math.max(highestCitationNumber, citationNumber);
        }
    }

    public void addMark(CSLReferenceMark mark) {
        marksByName.put(mark.getName(), mark);
        idsByMark.put(mark, marksByID.size());
        marksByID.add(mark);
        updateCitationInfo(mark.getName());
    }

    public int getCitationNumber(String citationKey) {
        return citationKeyToNumber.computeIfAbsent(citationKey, k -> {
            highestCitationNumber++;
            return highestCitationNumber;
        });
    }

    public CSLReferenceMark createReferenceMark(BibEntry entry, String fieldType) throws Exception {
        String citationKey = entry.getCitationKey().orElse("");
        int citationNumber = getCitationNumber(citationKey);

        String name = CSLCitationOOAdapter.PREFIXES[0] + citationKey + " RND" + citationNumber;
        Object mark = factory.createInstance("com.sun.star.text.CSLReferenceMark");
        XNamed named = UnoRuntime.queryInterface(XNamed.class, mark);
        named.setName(name);

        CSLReferenceMark referenceMark = new CSLReferenceMark(document, named, name);
        addMark(referenceMark);

        return referenceMark;
    }
}
