//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.12.13 at 11:48:11 AM BRST 
//


package net.sf.jabref.logic.importer.fileformat.medline;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}MedlineCitation"/>
 *         &lt;element ref="{}PubmedData" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "medlineCitation",
    "pubmedData"
})
@XmlRootElement(name = "PubmedArticle")
public class PubmedArticle {

    @XmlElement(name = "MedlineCitation", required = true)
    protected MedlineCitation medlineCitation;
    @XmlElement(name = "PubmedData")
    protected PubmedData pubmedData;

    /**
     * Gets the value of the medlineCitation property.
     * 
     * @return
     *     possible object is
     *     {@link MedlineCitation }
     *     
     */
    public MedlineCitation getMedlineCitation() {
        return medlineCitation;
    }

    /**
     * Sets the value of the medlineCitation property.
     * 
     * @param value
     *     allowed object is
     *     {@link MedlineCitation }
     *     
     */
    public void setMedlineCitation(MedlineCitation value) {
        this.medlineCitation = value;
    }

    /**
     * Gets the value of the pubmedData property.
     * 
     * @return
     *     possible object is
     *     {@link PubmedData }
     *     
     */
    public PubmedData getPubmedData() {
        return pubmedData;
    }

    /**
     * Sets the value of the pubmedData property.
     * 
     * @param value
     *     allowed object is
     *     {@link PubmedData }
     *     
     */
    public void setPubmedData(PubmedData value) {
        this.pubmedData = value;
    }

}
