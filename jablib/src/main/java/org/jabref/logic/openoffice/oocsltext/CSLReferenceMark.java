package org.jabref.logic.openoffice.oocsltext;

import java.util.List;
import java.util.Map;

import org.jabref.logic.openoffice.JabRefReferenceMark;
import org.jabref.logic.openoffice.OpenOfficeReferenceMarkFormat;
import org.jabref.logic.openoffice.ReferenceMark;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.sun.star.container.XNamed;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XTextContent;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

/// Class to model a reference mark. See {@link CSLReferenceMarkManager} for the usage and management of all reference marks.
public class CSLReferenceMark {
    private ReferenceMark referenceMark;
    private XTextContent textContent;
    private final List<String> citationKeys;
    private List<Integer> citationNumbers;
    private final CSLCitationType citationType;

    public CSLReferenceMark(XNamed named, ReferenceMark referenceMark) {
        this.referenceMark = referenceMark;
        this.textContent = UnoRuntime.queryInterface(XTextContent.class, named);
        this.citationKeys = referenceMark.getCitationKeys();
        this.citationNumbers = referenceMark.getCitationNumbers();
        this.citationType = referenceMark.getCitationType();
    }

    public static CSLReferenceMark of(List<BibEntry> entries,
                                      List<String> citationKeys,
                                      List<Integer> citationNumbers,
                                      CSLCitationType citationType,
                                      int firstZoteroItemId,
                                      XMultiServiceFactory factory,
                                      BibDatabaseContext bibDatabaseContext,
                                      BibEntryTypesManager entryTypesManager,
                                      OpenOfficeReferenceMarkFormat referenceMarkFormat,
                                      Map<String, String> zoteroUriByCitationKey) throws Exception {
        ReferenceMark referenceMark = buildReferenceMark(
                entries,
                citationKeys,
                citationNumbers,
                citationType,
                firstZoteroItemId,
                bibDatabaseContext,
                entryTypesManager,
                referenceMarkFormat,
                zoteroUriByCitationKey);
        XNamed named = UnoRuntime.queryInterface(XNamed.class, factory.createInstance("com.sun.star.text.ReferenceMark"));
        named.setName(referenceMark.getName());
        return new CSLReferenceMark(named, referenceMark);
    }

    static ReferenceMark buildReferenceMark(List<BibEntry> entries,
                                            List<String> citationKeys,
                                            List<Integer> citationNumbers,
                                            CSLCitationType citationType,
                                            int firstZoteroItemId,
                                            BibDatabaseContext bibDatabaseContext,
                                            BibEntryTypesManager entryTypesManager,
                                            OpenOfficeReferenceMarkFormat referenceMarkFormat,
                                            Map<String, String> zoteroUriByCitationKey) {
        return switch (referenceMarkFormat) {
            case JABREF_ONLY ->
                    JabRefReferenceMark.buildReferenceMark(
                            citationKeys,
                            citationNumbers,
                            citationType);
            case ZOTERO_COMPATIBLE ->
                    ZoteroReferenceMark.buildReferenceMark(
                            entries,
                            citationKeys,
                            citationNumbers,
                            firstZoteroItemId,
                            citationType,
                            bibDatabaseContext,
                            entryTypesManager,
                            zoteroUriByCitationKey);
        };
    }

    public List<String> getCitationKeys() {
        return citationKeys;
    }

    public List<Integer> getCitationNumbers() {
        return citationNumbers;
    }

    public CSLCitationType getCitationType() {
        return citationType;
    }

    public void setCitationNumbers(List<Integer> numbers) {
        this.citationNumbers = numbers;
    }

    public XTextContent getTextContent() {
        return textContent;
    }

    public String getName() {
        return referenceMark.getName();
    }

    public String getUniqueId() {
        return referenceMark.getUniqueId();
    }

    public void updateTextContent(XTextContent newTextContent) {
        this.textContent = newTextContent;
    }

    public void updateName(String newName) {
        this.referenceMark = ReferenceMark.of(newName, this.citationKeys, this.citationNumbers, this.referenceMark.getUniqueId(), this.citationType);
    }
}
