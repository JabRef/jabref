//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.02.06 at 09:03:29 PM CET 
//


package org.jabref.logic.importer.fileformat.medline;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for pub.status.int.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="pub.status.int">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *     &lt;enumeration value="pmc"/>
 *     &lt;enumeration value="pmcr"/>
 *     &lt;enumeration value="pubmed"/>
 *     &lt;enumeration value="pubmedr"/>
 *     &lt;enumeration value="premedline"/>
 *     &lt;enumeration value="medline"/>
 *     &lt;enumeration value="medliner"/>
 *     &lt;enumeration value="entrez"/>
 *     &lt;enumeration value="pmc-release"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "pub.status.int")
@XmlEnum
public enum PubStatusInt {

    @XmlEnumValue("pmc")
    PMC("pmc"),
    @XmlEnumValue("pmcr")
    PMCR("pmcr"),
    @XmlEnumValue("pubmed")
    PUBMED("pubmed"),
    @XmlEnumValue("pubmedr")
    PUBMEDR("pubmedr"),
    @XmlEnumValue("premedline")
    PREMEDLINE("premedline"),
    @XmlEnumValue("medline")
    MEDLINE("medline"),
    @XmlEnumValue("medliner")
    MEDLINER("medliner"),
    @XmlEnumValue("entrez")
    ENTREZ("entrez"),
    @XmlEnumValue("pmc-release")
    PMC_RELEASE("pmc-release");
    private final String value;

    PubStatusInt(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static PubStatusInt fromValue(String v) {
        for (PubStatusInt c: PubStatusInt.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
