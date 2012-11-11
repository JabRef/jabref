package spl;

import java.util.List;

import org.sciplore.beans.Abstract;
import org.sciplore.beans.Author;
import org.sciplore.beans.Authors;
import org.sciplore.beans.Document;
import org.sciplore.formatter.Bean;
import org.sciplore.formatter.SimpleTypeElementBean;

/**
 * Created by IntelliJ IDEA.
 * User: Christoph Arbeit
 * Date: 10.09.2010
 * Time: 20:02:51
 * To change this template use File | Settings | File Templates.
 */
public class DocumentWrapper {

    Document xmlDocument;

    public DocumentWrapper(Document xmlDocument) {
        this.xmlDocument = xmlDocument;
    }

    public Document getXmlDocument() {
        return xmlDocument;
    }

    public void setXmlDocument(Document xmlDocument) {
        this.xmlDocument = xmlDocument;
    }

    public String getTitle(){
        if(this.hasTitle()){
            return xmlDocument.getTitle().getValue();
        }
        else{
            return "";
        }
    }

    public boolean hasTitle(){
        return (xmlDocument.getTitle() != null && xmlDocument.getTitle().getValue() != null && !xmlDocument.getTitle().getValue().isEmpty());
    }

    public String getAbstract(){
        if(this.hasAbstract()){
            return ((Abstract)xmlDocument.getAbstract()).getValue();
        }
        else{
            return "";
        }
    }

    public boolean hasAbstract(){
        return (xmlDocument.getAbstract() != null && ((Abstract)xmlDocument.getAbstract()).getValue() != null && !((Abstract)xmlDocument.getAbstract()).getValue().isEmpty());
    }

   public String getAuthors(String seperator){
        if(this.hasAuthors()){
        	List<Bean> authors = xmlDocument.getAuthors().getCollection();
            authors = this.sortAuthors(authors);
            String value = "";
            int i = 1;
            for(Bean author : authors){
                if(i < authors.size()){
                    value = value + getNameComplete((Author)author);
                    value = value + " " + seperator + " ";
                }
                else{
                    value = value + getNameComplete((Author)author);
                }
                i++;
            }
            return value;
        }
        else{
            return "";
        }
    }

    public boolean hasAuthors(){
        return (xmlDocument.getAuthors() != null && xmlDocument.getAuthors().getCollection() != null && !xmlDocument.getAuthors().getCollection().isEmpty());
    }

    /* public String getKeyWords(){
        if(this.hasKeyWords()){
            List<XmlKeyword> keywords = xmlDocument.getKeywords().getKeywords();
            String value = "";
            int i = 1;
            for(XmlKeyword keyword : keywords){
                if(i < keywords.size()){
                    value = value + keyword.getValue();
                    value = value + ", ";
                }
                else{
                    value = value + keyword.getValue();
                }
                i++;
            }
            return value;
        }
        else{
            return "";
        }
    }

    public boolean hasKeyWords(){
        return (xmlDocument.getKeywords() != null && xmlDocument.getKeywords().getKeywords() != null && !xmlDocument.getKeywords().getKeywords().isEmpty());
    }
*/

    public String getDoi(){
        if(this.hasDoi()){
            return this.getSimpleTypeValue(xmlDocument.getDoi());
        }
        else{
            return "";
        }
    }

    public boolean hasDoi(){
        return (xmlDocument.getDoi() != null && this.getSimpleTypeValue(xmlDocument.getDoi()) != null && !this.getSimpleTypeValue(xmlDocument.getDoi()).isEmpty());
    }
/*
    public String getPages(){
        if(this.hasPages()){
            return xmlDocument.getPages().getValue();
        }
        else{
            return "";
        }
    }

    public boolean hasPages(){
        return (xmlDocument.getPages() != null && xmlDocument.getPages().getValue() != null && !xmlDocument.getPages().getValue().isEmpty());
    }

    public String getVolume(){
        if(this.hasVolume()){
            return xmlDocument.getVolume().getValue();
        }
        else{
            return "";
        }
    }

    public boolean hasVolume(){
        return (xmlDocument.getVolume() != null && xmlDocument.getVolume().getValue() != null && !xmlDocument.getVolume().getValue().isEmpty());
    }

    public String getNumber(){
        if(this.hasNumber()){
            return xmlDocument.getNumber().getValue();
        }
        else{
            return "";
        }
    }

    public boolean hasNumber(){
        return (xmlDocument.getNumber() != null && xmlDocument.getNumber().getValue() != null && !xmlDocument.getNumber().getValue().isEmpty());
    }
*/
    
    public String getYear(){
        if(this.hasYear()){
            return this.getSimpleTypeValue(xmlDocument.getYear());
        }
        else{
            return "";
        }
    }

    public boolean hasYear(){
        return (this.getSimpleTypeValue(xmlDocument.getYear()) != null && !this.getSimpleTypeValue(xmlDocument.getYear()).isEmpty() && !this.getSimpleTypeValue(xmlDocument.getYear()).equalsIgnoreCase("null"));
    }
    
/*
     public String getMonth(){
        if(this.hasMonth()){
            return xmlDocument.getPublishdate().getMonth();
        }
        else{
            return "";
        }
    }

    public boolean hasMonth(){
        return (xmlDocument.getPublishdate() != null && xmlDocument.getPublishdate().getMonth() != null && !xmlDocument.getPublishdate().getMonth().isEmpty());
    }

    public String getDay(){
        if(this.hasDay()){
            return xmlDocument.getPublishdate().getDay();
        }
        else{
            return "";
        }
    }

    public boolean hasDay(){
        return (xmlDocument.getPublishdate() != null && xmlDocument.getPublishdate().getDay() != null && !xmlDocument.getPublishdate().getDay().isEmpty());
    }
    
	/*
    public String getVenue() {
        if(this.hasVenue()){
            return xmlDocument.getVenue().getValue();
        }
        else{
            return "";
        }
    }

    public boolean hasVenue(){
        return (xmlDocument.getVenue() != null && xmlDocument.getVenue().getValue() != null && !xmlDocument.getVenue().getValue().isEmpty());
    }*/
    
    private String getNameComplete(Author author){
    	if(author == null) return "";
    	String result = "";
    	if(getSimpleTypeValue(author.getName_First()) != null)
    		result = result + getSimpleTypeValue(author.getName_First()).trim() + " ";
    	if(getSimpleTypeValue(author.getName_Middle()) != null)
    		result = result + getSimpleTypeValue(author.getName_Middle()).trim() + " ";
    	if(getSimpleTypeValue(author.getName_Last_Prefix()) != null)
    		result = result + getSimpleTypeValue(author.getName_Last_Prefix()).trim() + " ";
    	if(getSimpleTypeValue(author.getName_Last()) != null)
    		result = result + getSimpleTypeValue(author.getName_Last()).trim() + " ";
    	if(getSimpleTypeValue(author.getName_Last_Suffix()) != null)
    		result = result + getSimpleTypeValue(author.getName_Last_Suffix()).trim() + " ";
    	return result.trim();
    }
    
    private String getSimpleTypeValue(Bean bean){
    	if(bean == null || !(bean instanceof SimpleTypeElementBean)) return null;
    	SimpleTypeElementBean simpleTypeElementBean = (SimpleTypeElementBean)bean;
    	if(simpleTypeElementBean.getValue() == null || simpleTypeElementBean.getValue().equalsIgnoreCase("null") || simpleTypeElementBean.getValue().length() <= 0) return null;
    	return simpleTypeElementBean.getValue();
    }
    
    private List<Bean> sortAuthors(List<Bean> authors){
   	 boolean unsorted = true;
        Bean temp;
        
        while (unsorted){
       	 unsorted = false;
       	 for (int i = 0; i < authors.size() - 1; i++){
       		 int rank = 99;
       		 int otherRank = 99;
       		 if(((Author)authors.get(i)).getRank() != null && !((Author)authors.get(i)).getRank().equalsIgnoreCase("null")){
       			 rank = Integer.parseInt(((Author)authors.get(i)).getRank());
       		 }        		 
       		 if(((Author)authors.get(i + 1)).getRank() != null && !((Author)authors.get(i + 1)).getRank().equalsIgnoreCase("null")){
       			 otherRank = Integer.parseInt(((Author)authors.get(i + 1)).getRank());
       		 }       		 
       		 
       	 	 if (rank > otherRank) {                      
                 temp       = authors.get(i);
                 authors.set(i, authors.get(i + 1));
                 authors.set(i + 1, temp);                 
                 unsorted = true;
              }          
        	} 	 
       } 
   	
   	return authors;
   }
}
