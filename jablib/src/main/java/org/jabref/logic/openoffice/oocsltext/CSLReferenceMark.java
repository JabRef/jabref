package org.jabref.logic.openoffice.oocsltext;

import java.util.List;

import org.jabref.logic.openoffice.ReferenceMark;

import com.sun.star.container.XNamed;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XTextContent;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import io.github.thibaultmeyer.cuid.CUID;

/**
 * Class to model a reference mark. See {@link CSLReferenceMarkManager} for the usage and management of all reference marks.
 */
public class CSLReferenceMark {
    private ReferenceMark referenceMark;
    private XTextContent textContent;
    private final List<String> citationKeys;
    private List<Integer> citationNumbers;

    public CSLReferenceMark(XNamed named, ReferenceMark referenceMark) {
        this.referenceMark = referenceMark;
        this.textContent = UnoRuntime.queryInterface(XTextContent.class, named);
        this.citationKeys = referenceMark.getCitationKeys();
        this.citationNumbers = referenceMark.getCitationNumbers();
    }

    public static CSLReferenceMark of(List<String> citationKeys, List<Integer> citationNumbers, XMultiServiceFactory factory) throws Exception {
        String uniqueId = CUID.randomCUID2(8).toString();
        String name = buildReferenceName(citationKeys, citationNumbers, uniqueId);
        XNamed named = UnoRuntime.queryInterface(XNamed.class, factory.createInstance("com.sun.star.text.ReferenceMark"));
        named.setName(name);
        ReferenceMark referenceMark = new ReferenceMark(name, citationKeys, citationNumbers, uniqueId);
        return new CSLReferenceMark(named, referenceMark);
    }

    private static String buildReferenceName(List<String> citationKeys, List<Integer> citationNumbers, String uniqueId) {
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < citationKeys.size(); i++) {
            if (i > 0) {
                nameBuilder.append(", ");
            }
            nameBuilder.append(ReferenceMark.PREFIXES[0]).append(citationKeys.get(i))
                       .append(" ").append(ReferenceMark.PREFIXES[1]).append(citationNumbers.get(i));
        }
        nameBuilder.append(" ").append(uniqueId);
        return nameBuilder.toString();
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
        this.referenceMark = new ReferenceMark(newName, this.citationKeys, this.citationNumbers, this.referenceMark.getUniqueId());
    }
}
