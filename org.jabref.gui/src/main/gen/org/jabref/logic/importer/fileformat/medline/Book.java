//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.02.06 at 09:03:29 PM CET 
//


package org.jabref.logic.importer.fileformat.medline;

import java.util.ArrayList;
import java.util.List;
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
 *         &lt;element ref="{}Publisher"/>
 *         &lt;element ref="{}BookTitle"/>
 *         &lt;element ref="{}PubDate"/>
 *         &lt;element ref="{}BeginningDate" minOccurs="0"/>
 *         &lt;element ref="{}EndingDate" minOccurs="0"/>
 *         &lt;element ref="{}AuthorList" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}Volume" minOccurs="0"/>
 *         &lt;element ref="{}VolumeTitle" minOccurs="0"/>
 *         &lt;element ref="{}Edition" minOccurs="0"/>
 *         &lt;element ref="{}CollectionTitle" minOccurs="0"/>
 *         &lt;element ref="{}Isbn" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}ELocationID" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}Medium" minOccurs="0"/>
 *         &lt;element ref="{}ReportNumber" minOccurs="0"/>
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
    "publisher",
    "bookTitle",
    "pubDate",
    "beginningDate",
    "endingDate",
    "authorList",
    "volume",
    "volumeTitle",
    "edition",
    "collectionTitle",
    "isbn",
    "eLocationID",
    "medium",
    "reportNumber"
})
@XmlRootElement(name = "Book")
public class Book {

    @XmlElement(name = "Publisher", required = true)
    protected Publisher publisher;
    @XmlElement(name = "BookTitle", required = true)
    protected BookTitle bookTitle;
    @XmlElement(name = "PubDate", required = true)
    protected PubDate pubDate;
    @XmlElement(name = "BeginningDate")
    protected BeginningDate beginningDate;
    @XmlElement(name = "EndingDate")
    protected EndingDate endingDate;
    @XmlElement(name = "AuthorList")
    protected List<AuthorList> authorList;
    @XmlElement(name = "Volume")
    protected String volume;
    @XmlElement(name = "VolumeTitle")
    protected Text volumeTitle;
    @XmlElement(name = "Edition")
    protected String edition;
    @XmlElement(name = "CollectionTitle")
    protected CollectionTitle collectionTitle;
    @XmlElement(name = "Isbn")
    protected List<String> isbn;
    @XmlElement(name = "ELocationID")
    protected List<ELocationID> eLocationID;
    @XmlElement(name = "Medium")
    protected String medium;
    @XmlElement(name = "ReportNumber")
    protected String reportNumber;

    /**
     * Gets the value of the publisher property.
     * 
     * @return
     *     possible object is
     *     {@link Publisher }
     *     
     */
    public Publisher getPublisher() {
        return publisher;
    }

    /**
     * Sets the value of the publisher property.
     * 
     * @param value
     *     allowed object is
     *     {@link Publisher }
     *     
     */
    public void setPublisher(Publisher value) {
        this.publisher = value;
    }

    /**
     * Gets the value of the bookTitle property.
     * 
     * @return
     *     possible object is
     *     {@link BookTitle }
     *     
     */
    public BookTitle getBookTitle() {
        return bookTitle;
    }

    /**
     * Sets the value of the bookTitle property.
     * 
     * @param value
     *     allowed object is
     *     {@link BookTitle }
     *     
     */
    public void setBookTitle(BookTitle value) {
        this.bookTitle = value;
    }

    /**
     * Gets the value of the pubDate property.
     * 
     * @return
     *     possible object is
     *     {@link PubDate }
     *     
     */
    public PubDate getPubDate() {
        return pubDate;
    }

    /**
     * Sets the value of the pubDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link PubDate }
     *     
     */
    public void setPubDate(PubDate value) {
        this.pubDate = value;
    }

    /**
     * Gets the value of the beginningDate property.
     * 
     * @return
     *     possible object is
     *     {@link BeginningDate }
     *     
     */
    public BeginningDate getBeginningDate() {
        return beginningDate;
    }

    /**
     * Sets the value of the beginningDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link BeginningDate }
     *     
     */
    public void setBeginningDate(BeginningDate value) {
        this.beginningDate = value;
    }

    /**
     * Gets the value of the endingDate property.
     * 
     * @return
     *     possible object is
     *     {@link EndingDate }
     *     
     */
    public EndingDate getEndingDate() {
        return endingDate;
    }

    /**
     * Sets the value of the endingDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link EndingDate }
     *     
     */
    public void setEndingDate(EndingDate value) {
        this.endingDate = value;
    }

    /**
     * Gets the value of the authorList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the authorList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuthorList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AuthorList }
     * 
     * 
     */
    public List<AuthorList> getAuthorList() {
        if (authorList == null) {
            authorList = new ArrayList<AuthorList>();
        }
        return this.authorList;
    }

    /**
     * Gets the value of the volume property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVolume() {
        return volume;
    }

    /**
     * Sets the value of the volume property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVolume(String value) {
        this.volume = value;
    }

    /**
     * Gets the value of the volumeTitle property.
     * 
     * @return
     *     possible object is
     *     {@link Text }
     *     
     */
    public Text getVolumeTitle() {
        return volumeTitle;
    }

    /**
     * Sets the value of the volumeTitle property.
     * 
     * @param value
     *     allowed object is
     *     {@link Text }
     *     
     */
    public void setVolumeTitle(Text value) {
        this.volumeTitle = value;
    }

    /**
     * Gets the value of the edition property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEdition() {
        return edition;
    }

    /**
     * Sets the value of the edition property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEdition(String value) {
        this.edition = value;
    }

    /**
     * Gets the value of the collectionTitle property.
     * 
     * @return
     *     possible object is
     *     {@link CollectionTitle }
     *     
     */
    public CollectionTitle getCollectionTitle() {
        return collectionTitle;
    }

    /**
     * Sets the value of the collectionTitle property.
     * 
     * @param value
     *     allowed object is
     *     {@link CollectionTitle }
     *     
     */
    public void setCollectionTitle(CollectionTitle value) {
        this.collectionTitle = value;
    }

    /**
     * Gets the value of the isbn property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the isbn property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIsbn().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getIsbn() {
        if (isbn == null) {
            isbn = new ArrayList<String>();
        }
        return this.isbn;
    }

    /**
     * Gets the value of the eLocationID property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the eLocationID property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getELocationID().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ELocationID }
     * 
     * 
     */
    public List<ELocationID> getELocationID() {
        if (eLocationID == null) {
            eLocationID = new ArrayList<ELocationID>();
        }
        return this.eLocationID;
    }

    /**
     * Gets the value of the medium property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMedium() {
        return medium;
    }

    /**
     * Sets the value of the medium property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMedium(String value) {
        this.medium = value;
    }

    /**
     * Gets the value of the reportNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReportNumber() {
        return reportNumber;
    }

    /**
     * Sets the value of the reportNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReportNumber(String value) {
        this.reportNumber = value;
    }

}
