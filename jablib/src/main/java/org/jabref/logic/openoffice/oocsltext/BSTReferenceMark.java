package org.jabref.logic.openoffice.oocsltext;

import java.util.List;

import org.jabref.logic.openoffice.JabRefReferenceMark;
import org.jabref.logic.openoffice.ReferenceMark;

import com.sun.star.container.XNamed;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XTextContent;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

/// Class to model a reference mark for BST integration. See [BSTReferenceMarkManager] for usage/management.
public class BSTReferenceMark {
    private JabRefReferenceMark referenceMark;
    private XTextContent textContent;
    private final List<String> citationKeys;
    private List<Integer> citationNumbers;
    private CSLCitationType citationType;

    public BSTReferenceMark(XNamed named, JabRefReferenceMark referenceMark) {
        this.referenceMark = referenceMark;
        this.textContent = UnoRuntime.queryInterface(XTextContent.class, named);
        this.citationKeys = referenceMark.getCitationKeys();
        this.citationNumbers = referenceMark.getCitationNumbers();
        this.citationType = referenceMark.getCitationType();
    }

    public static BSTReferenceMark of(List<String> citationKeys, List<Integer> citationNumbers, CSLCitationType citationType, XMultiServiceFactory factory) throws Exception {
        String uniqueId = ReferenceMark.generateRandomCUID(8);
        JabRefReferenceMark referenceMark = new JabRefReferenceMark(
                JabRefReferenceMark.buildReferenceMarkName(citationKeys, citationNumbers, uniqueId, citationType),
                citationKeys,
                citationNumbers,
                uniqueId,
                citationType);
        XNamed named = UnoRuntime.queryInterface(XNamed.class, factory.createInstance("com.sun.star.text.ReferenceMark"));
        named.setName(referenceMark.getName());
        return new BSTReferenceMark(named, referenceMark);
    }

    public List<String> getCitationKeys() {
        return citationKeys;
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

    public void updateTextContent(XTextContent newTextContent) {
        this.textContent = newTextContent;
    }

    public void updateName(String newName) {
        this.referenceMark = new JabRefReferenceMark(newName, this.citationKeys, this.citationNumbers, this.referenceMark.getUniqueId(), this.citationType);
    }
}
