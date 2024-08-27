package org.jabref.logic.openoffice.oocsltext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.openoffice.ReferenceMark;
import org.jabref.model.entry.BibEntry;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XReferenceMarksSupplier;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import io.github.thibaultmeyer.cuid.CUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSLReferenceMarkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CSLReferenceMarkManager.class);

    private final HashMap<String, CSLReferenceMark> marksByName;
    private final ArrayList<CSLReferenceMark> marksByID;
    private final IdentityHashMap<CSLReferenceMark, Integer> idsByMark;
    private final XTextDocument document;
    private final XMultiServiceFactory factory;
    private final HashMap<String, Integer> citationKeyToNumber;
    private int highestCitationNumber = 0;
    private final Map<String, Integer> citationOrder = new HashMap<>();

    public CSLReferenceMarkManager(XTextDocument document) {
        this.document = document;
        this.factory = UnoRuntime.queryInterface(XMultiServiceFactory.class, document);
        this.marksByName = new HashMap<>();
        this.marksByID = new ArrayList<>();
        this.idsByMark = new IdentityHashMap<>();
        this.citationKeyToNumber = new HashMap<>();
    }

    public CSLReferenceMark createReferenceMark(BibEntry entry) throws Exception {
        String citationKey = entry.getCitationKey().orElse(CUID.randomCUID2(8).toString());
        int citationNumber = getCitationNumber(citationKey);
        CSLReferenceMark referenceMark = CSLReferenceMark.of(citationKey, citationNumber, factory);
        addMark(referenceMark);
        return referenceMark;
    }

    public void addMark(CSLReferenceMark mark) {
        marksByName.put(mark.getName(), mark);
        idsByMark.put(mark, marksByID.size());
        marksByID.add(mark);
        updateCitationInfo(mark.getName());
    }

    public void readExistingMarks() throws WrappedTargetException, NoSuchElementException {
        XReferenceMarksSupplier supplier = UnoRuntime.queryInterface(XReferenceMarksSupplier.class, document);
        XNameAccess marks = supplier.getReferenceMarks();

        citationOrder.clear();
        int citationCounter = 0;

        for (String name : marks.getElementNames()) {
            Optional<ReferenceMark> referenceMark = ReferenceMark.of(name);
            if (!referenceMark.isEmpty()) {
                citationOrder.putIfAbsent(referenceMark.map(ReferenceMark::getCitationKey).get(), ++citationCounter);
                XNamed named = UnoRuntime.queryInterface(XNamed.class, marks.getByName(name));
                CSLReferenceMark mark = new CSLReferenceMark(named, referenceMark.get());
                addMark(mark);
            }
        }
    }

    private void updateCitationInfo(String name) {
        Optional<ReferenceMark> referenceMark = ReferenceMark.of(name);
        if (referenceMark.isPresent()) {
            int citationNumber = referenceMark.get().getCitationNumber();
            citationKeyToNumber.put(referenceMark.get().getCitationKey(), citationNumber);
            highestCitationNumber = Math.max(highestCitationNumber, citationNumber);
        } else {
            LOGGER.warn("Could not parse ReferenceMark name: {}", name);
        }
    }

    public boolean hasCitationForKey(String citationKey) {
        return citationKeyToNumber.containsKey(citationKey);
    }

    public int getCitationNumber(String citationKey) {
        return citationKeyToNumber.computeIfAbsent(citationKey, k -> ++highestCitationNumber);
    }
}
