package spl;

import org.sciplore.xml.XmlAuthor;
import org.sciplore.xml.XmlDocument;
import org.sciplore.xml.XmlDocuments;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Christoph Arbeit
 * Date: 09.09.2010
 * Time: 10:56:50
 * To change this template use File | Settings | File Templates.
 */
public class DocumentsWrapper {

    XmlDocuments xmlDocuments;

    public DocumentsWrapper(XmlDocuments documents) {
        this.xmlDocuments = documents;
    }

    public XmlDocuments getXmlDocuments() {
        return xmlDocuments;
    }

    public void setXmlDocuments(XmlDocuments xmlDocuments) {
        this.xmlDocuments = xmlDocuments;
    }

    public List<Vector> getDocuments(){
        List<Vector> documents = new ArrayList<Vector>();
        List<XmlDocument> xmlDocuments = this.xmlDocuments.getDocuments();
        for(XmlDocument xmlDocument : xmlDocuments){
            Vector<String> vector = new Vector<String>();
            if(xmlDocument.getTitle() != null){
                vector.add(xmlDocument.getTitle().getValue());
            }
            else{
                vector.add("");
            }
            if(xmlDocument.getAuthors() != null){
                List<XmlAuthor> authors = xmlDocument.getAuthors().getAuthors();
                String value = "";
                int i = 1;
                for(XmlAuthor author : authors){
                    if(i < authors.size()){
                        value = value + author.getNameComplete();
                        value = value + ", ";
                    }
                    else{
                        value = value + author.getNameComplete();
                    }
                    i++;
                }
                vector.add(value);
            }
            else{
                vector.add("");
            }
            if(xmlDocument.getPublishdate() != null && xmlDocument.getPublishdate().getYear() != null && !xmlDocument.getPublishdate().getYear().equalsIgnoreCase("null")){
                vector.add(xmlDocument.getPublishdate().getYear());
            }
            else{
                vector.add("");
            }
            documents.add(vector);
        }
        return documents;
    }
}
