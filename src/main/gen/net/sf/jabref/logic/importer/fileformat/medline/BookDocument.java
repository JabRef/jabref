//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.12.13 at 11:48:11 AM BRST 
//


package net.sf.jabref.logic.importer.fileformat.medline;

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
 *         &lt;element ref="{}PMID"/>
 *         &lt;element ref="{}ArticleIdList"/>
 *         &lt;element ref="{}Book"/>
 *         &lt;element ref="{}LocationLabel" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}ArticleTitle" minOccurs="0"/>
 *         &lt;element ref="{}VernacularTitle" minOccurs="0"/>
 *         &lt;element ref="{}Pagination" minOccurs="0"/>
 *         &lt;element ref="{}Language" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}AuthorList" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}PublicationType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}Abstract" minOccurs="0"/>
 *         &lt;element ref="{}Sections" minOccurs="0"/>
 *         &lt;element ref="{}KeywordList" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}ContributionDate" minOccurs="0"/>
 *         &lt;element ref="{}DateRevised" minOccurs="0"/>
 *         &lt;element ref="{}CitationString" minOccurs="0"/>
 *         &lt;element ref="{}GrantList" minOccurs="0"/>
 *         &lt;element ref="{}ItemList" maxOccurs="unbounded" minOccurs="0"/>
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
    "pmid",
    "articleIdList",
    "book",
    "locationLabel",
    "articleTitle",
    "vernacularTitle",
    "pagination",
    "language",
    "authorList",
    "publicationType",
    "_abstract",
    "sections",
    "keywordList",
    "contributionDate",
    "dateRevised",
    "citationString",
    "grantList",
    "itemList"
})
@XmlRootElement(name = "BookDocument")
public class BookDocument {

    @XmlElement(name = "PMID", required = true)
    protected PMID pmid;
    @XmlElement(name = "ArticleIdList", required = true)
    protected ArticleIdList articleIdList;
    @XmlElement(name = "Book", required = true)
    protected Book book;
    @XmlElement(name = "LocationLabel")
    protected List<LocationLabel> locationLabel;
    @XmlElement(name = "ArticleTitle")
    protected ArticleTitle articleTitle;
    @XmlElement(name = "VernacularTitle")
    protected Text vernacularTitle;
    @XmlElement(name = "Pagination")
    protected Pagination pagination;
    @XmlElement(name = "Language")
    protected List<String> language;
    @XmlElement(name = "AuthorList")
    protected List<AuthorList> authorList;
    @XmlElement(name = "PublicationType")
    protected List<PublicationType> publicationType;
    @XmlElement(name = "Abstract")
    protected Abstract _abstract;
    @XmlElement(name = "Sections")
    protected Sections sections;
    @XmlElement(name = "KeywordList")
    protected List<KeywordList> keywordList;
    @XmlElement(name = "ContributionDate")
    protected ContributionDate contributionDate;
    @XmlElement(name = "DateRevised")
    protected DateRevised dateRevised;
    @XmlElement(name = "CitationString")
    protected Text citationString;
    @XmlElement(name = "GrantList")
    protected GrantList grantList;
    @XmlElement(name = "ItemList")
    protected List<ItemList> itemList;

    /**
     * Gets the value of the pmid property.
     * 
     * @return
     *     possible object is
     *     {@link PMID }
     *     
     */
    public PMID getPMID() {
        return pmid;
    }

    /**
     * Sets the value of the pmid property.
     * 
     * @param value
     *     allowed object is
     *     {@link PMID }
     *     
     */
    public void setPMID(PMID value) {
        this.pmid = value;
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
     * Gets the value of the book property.
     * 
     * @return
     *     possible object is
     *     {@link Book }
     *     
     */
    public Book getBook() {
        return book;
    }

    /**
     * Sets the value of the book property.
     * 
     * @param value
     *     allowed object is
     *     {@link Book }
     *     
     */
    public void setBook(Book value) {
        this.book = value;
    }

    /**
     * Gets the value of the locationLabel property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the locationLabel property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLocationLabel().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link LocationLabel }
     * 
     * 
     */
    public List<LocationLabel> getLocationLabel() {
        if (locationLabel == null) {
            locationLabel = new ArrayList<LocationLabel>();
        }
        return this.locationLabel;
    }

    /**
     * Gets the value of the articleTitle property.
     * 
     * @return
     *     possible object is
     *     {@link ArticleTitle }
     *     
     */
    public ArticleTitle getArticleTitle() {
        return articleTitle;
    }

    /**
     * Sets the value of the articleTitle property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArticleTitle }
     *     
     */
    public void setArticleTitle(ArticleTitle value) {
        this.articleTitle = value;
    }

    /**
     * Gets the value of the vernacularTitle property.
     * 
     * @return
     *     possible object is
     *     {@link Text }
     *     
     */
    public Text getVernacularTitle() {
        return vernacularTitle;
    }

    /**
     * Sets the value of the vernacularTitle property.
     * 
     * @param value
     *     allowed object is
     *     {@link Text }
     *     
     */
    public void setVernacularTitle(Text value) {
        this.vernacularTitle = value;
    }

    /**
     * Gets the value of the pagination property.
     * 
     * @return
     *     possible object is
     *     {@link Pagination }
     *     
     */
    public Pagination getPagination() {
        return pagination;
    }

    /**
     * Sets the value of the pagination property.
     * 
     * @param value
     *     allowed object is
     *     {@link Pagination }
     *     
     */
    public void setPagination(Pagination value) {
        this.pagination = value;
    }

    /**
     * Gets the value of the language property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the language property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLanguage().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getLanguage() {
        if (language == null) {
            language = new ArrayList<String>();
        }
        return this.language;
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
     * Gets the value of the publicationType property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the publicationType property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPublicationType().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PublicationType }
     * 
     * 
     */
    public List<PublicationType> getPublicationType() {
        if (publicationType == null) {
            publicationType = new ArrayList<PublicationType>();
        }
        return this.publicationType;
    }

    /**
     * Gets the value of the abstract property.
     * 
     * @return
     *     possible object is
     *     {@link Abstract }
     *     
     */
    public Abstract getAbstract() {
        return _abstract;
    }

    /**
     * Sets the value of the abstract property.
     * 
     * @param value
     *     allowed object is
     *     {@link Abstract }
     *     
     */
    public void setAbstract(Abstract value) {
        this._abstract = value;
    }

    /**
     * Gets the value of the sections property.
     * 
     * @return
     *     possible object is
     *     {@link Sections }
     *     
     */
    public Sections getSections() {
        return sections;
    }

    /**
     * Sets the value of the sections property.
     * 
     * @param value
     *     allowed object is
     *     {@link Sections }
     *     
     */
    public void setSections(Sections value) {
        this.sections = value;
    }

    /**
     * Gets the value of the keywordList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the keywordList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getKeywordList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link KeywordList }
     * 
     * 
     */
    public List<KeywordList> getKeywordList() {
        if (keywordList == null) {
            keywordList = new ArrayList<KeywordList>();
        }
        return this.keywordList;
    }

    /**
     * Gets the value of the contributionDate property.
     * 
     * @return
     *     possible object is
     *     {@link ContributionDate }
     *     
     */
    public ContributionDate getContributionDate() {
        return contributionDate;
    }

    /**
     * Sets the value of the contributionDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link ContributionDate }
     *     
     */
    public void setContributionDate(ContributionDate value) {
        this.contributionDate = value;
    }

    /**
     * Gets the value of the dateRevised property.
     * 
     * @return
     *     possible object is
     *     {@link DateRevised }
     *     
     */
    public DateRevised getDateRevised() {
        return dateRevised;
    }

    /**
     * Sets the value of the dateRevised property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateRevised }
     *     
     */
    public void setDateRevised(DateRevised value) {
        this.dateRevised = value;
    }

    /**
     * Gets the value of the citationString property.
     * 
     * @return
     *     possible object is
     *     {@link Text }
     *     
     */
    public Text getCitationString() {
        return citationString;
    }

    /**
     * Sets the value of the citationString property.
     * 
     * @param value
     *     allowed object is
     *     {@link Text }
     *     
     */
    public void setCitationString(Text value) {
        this.citationString = value;
    }

    /**
     * Gets the value of the grantList property.
     * 
     * @return
     *     possible object is
     *     {@link GrantList }
     *     
     */
    public GrantList getGrantList() {
        return grantList;
    }

    /**
     * Sets the value of the grantList property.
     * 
     * @param value
     *     allowed object is
     *     {@link GrantList }
     *     
     */
    public void setGrantList(GrantList value) {
        this.grantList = value;
    }

    /**
     * Gets the value of the itemList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the itemList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getItemList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ItemList }
     * 
     * 
     */
    public List<ItemList> getItemList() {
        if (itemList == null) {
            itemList = new ArrayList<ItemList>();
        }
        return this.itemList;
    }

}
