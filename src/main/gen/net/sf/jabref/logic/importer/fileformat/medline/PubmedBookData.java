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
 *         &lt;element ref="{}History" minOccurs="0"/>
 *         &lt;element ref="{}PublicationStatus"/>
 *         &lt;element ref="{}ArticleIdList"/>
 *         &lt;element ref="{}ObjectList" minOccurs="0"/>
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
    "history",
    "publicationStatus",
    "articleIdList",
    "objectList"
})
@XmlRootElement(name = "PubmedBookData")
public class PubmedBookData {

    @XmlElement(name = "History")
    protected History history;
    @XmlElement(name = "PublicationStatus", required = true)
    protected String publicationStatus;
    @XmlElement(name = "ArticleIdList", required = true)
    protected ArticleIdList articleIdList;
    @XmlElement(name = "ObjectList")
    protected ObjectList objectList;

    /**
     * Gets the value of the history property.
     * 
     * @return
     *     possible object is
     *     {@link History }
     *     
     */
    public History getHistory() {
        return history;
    }

    /**
     * Sets the value of the history property.
     * 
     * @param value
     *     allowed object is
     *     {@link History }
     *     
     */
    public void setHistory(History value) {
        this.history = value;
    }

    /**
     * Gets the value of the publicationStatus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPublicationStatus() {
        return publicationStatus;
    }

    /**
     * Sets the value of the publicationStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPublicationStatus(String value) {
        this.publicationStatus = value;
    }

    /**
     * Gets the value of the articleIdList property.
     * 
     * @return
     *     possible object is
     *     {@link ArticleIdList }
     *     
     */
    public ArticleIdList getArticleIdList() {
        return articleIdList;
    }

    /**
     * Sets the value of the articleIdList property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArticleIdList }
     *     
     */
    public void setArticleIdList(ArticleIdList value) {
        this.articleIdList = value;
    }

    /**
     * Gets the value of the objectList property.
     * 
     * @return
     *     possible object is
     *     {@link ObjectList }
     *     
     */
    public ObjectList getObjectList() {
        return objectList;
    }

    /**
     * Sets the value of the objectList property.
     * 
     * @param value
     *     allowed object is
     *     {@link ObjectList }
     *     
     */
    public void setObjectList(ObjectList value) {
        this.objectList = value;
    }

}
