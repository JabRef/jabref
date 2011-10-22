package spl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;

import org.sciplore.beans.Author;
import org.sciplore.beans.Document;
import org.sciplore.deserialize.creator.AuthorBeanCreator;
import org.sciplore.deserialize.creator.AuthorsBeanCreator;
import org.sciplore.deserialize.creator.DefaultStringCreator;
import org.sciplore.deserialize.creator.DocumentBeanCreator;
import org.sciplore.deserialize.creator.DocumentsBeanCreator;
import org.sciplore.deserialize.creator.ObjectCreator;
import org.sciplore.deserialize.creator.TitleBeanCreator;
import org.sciplore.deserialize.creator.YearBeanCreator;
import org.sciplore.deserialize.reader.ObjectCreatorMapper;
import org.sciplore.deserialize.reader.XmlResourceReader;
import org.sciplore.formatter.Bean;
import org.sciplore.formatter.SimpleTypeElementBean;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataMultiPart;

/**
 * Created by IntelliJ IDEA.
 * User: Christoph Arbeit
 * Date: 09.09.2010
 * Time: 10:35:20
 * To change this template use File | Settings | File Templates.
 */
public class SplWebClient {

    private static Client CLIENT = Client.create();
    static{
        CLIENT.setConnectTimeout(1000);
        CLIENT.setReadTimeout(70000);
    }
    private static WebResource WEBRESOURCE = CLIENT.resource( "http://api.mr-dlib.org/" );
    private static WebResource INTERNETRESOURCE = CLIENT.resource( "http://www.google.com" );
    //private static WebResource WEBRESOURCE = CLIENT.resource( "http://localhost:8080/rest/" );

    public static Document metadata;

    public static WebServiceStatus getMetaData(File file){
        try{
            if(isWebServiceAvailable() == false){
                if(isInternetAvailable()){
                    return  WebServiceStatus.WEBSERVICE_DOWN;
                }
                else{
                    return  WebServiceStatus.NO_INTERNET;
                }
            }
            if(isWebServiceOutDated()){
                return  WebServiceStatus.OUTDATED;
            }
            if(isMetaDataServiceAvailable() == false){
                return  WebServiceStatus.UNAVAILABLE;
            }
            FileInputStream fin = new FileInputStream(file);      
            byte[] data = new byte[(int)file.length()];          
            fin.read(data);           
            
            FormDataMultiPart formDataMultiPart = new FormDataMultiPart();            
            formDataMultiPart.field("file", data,  MediaType.APPLICATION_OCTET_STREAM_TYPE);            
            formDataMultiPart.field("source", "jabref",  MediaType.TEXT_PLAIN_TYPE);
            formDataMultiPart.field("filename", file.getName(), MediaType.TEXT_PLAIN_TYPE);
           
           
            ClientResponse response = WEBRESOURCE.path("documents").type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class, formDataMultiPart);
            //System.out.println(response.getEntity(String.class));
            if(response.getClientResponseStatus() == ClientResponse.Status.OK && response.hasEntity()){
                String entity = response.getEntity(String.class);
                byte[] bytes = new byte[0];
                try {
                    bytes = entity.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return null;
                }
                InputStream is = new ByteArrayInputStream(bytes);
                if(is != null){
                	ObjectCreatorMapper resourceMapper = new ObjectCreatorMapper();
                	ObjectCreator stringCreator = new DefaultStringCreator();
                	// initialize Mapper    
	            	  resourceMapper.addCreator("documents", new DocumentsBeanCreator());
	            	  resourceMapper.addCreator("authors", new AuthorsBeanCreator());
	            	  resourceMapper.addCreator("document", new DocumentBeanCreator());
	            	  resourceMapper.addCreator("title", new TitleBeanCreator());
	            	  resourceMapper.addCreator("year", new YearBeanCreator());
	            	  resourceMapper.addCreator("author", new AuthorBeanCreator());
	            	  
	            	  resourceMapper.addCreator("name_first", stringCreator);
	            	  resourceMapper.addCreator("name_middle", stringCreator);
	            	  resourceMapper.addCreator("name_last", stringCreator);
	            	  resourceMapper.addCreator("name_last_prefix", stringCreator);
	            	  resourceMapper.addCreator("name_last_suffix", stringCreator);
	            	  
	            	  // initialize xml reader
	            	  XmlResourceReader<?> reader = new XmlResourceReader(resourceMapper);
	            	  
	            	  // parse given file -> create object tree
	            	  Document docs =  (Document)reader.parse(is);
	            	  for(Bean author : docs.getAuthors().getCollection()){
	            		  Author temp = (Author)author;
	            		  System.out.println(((SimpleTypeElementBean)temp.getName_Last()).getValue() + " " + temp.getRank());
	            	  }
                   // XmlDocuments documents = JAXB.unmarshal(is, XmlDocuments.class);
                    SplWebClient.metadata = docs;
                    return WebServiceStatus.OK;
                }
                else{
                    return WebServiceStatus.NO_METADATA;
                }
            }
            if(response.getClientResponseStatus() == ClientResponse.Status.SERVICE_UNAVAILABLE){
                return  WebServiceStatus.UNAVAILABLE;
            }
        }catch(Exception e){
            System.out.println(Tools.getStackTraceAsString(e));
            //Todo logging
        }
        return WebServiceStatus.NO_METADATA;
    }

    public static boolean isWebServiceOutDated(){
        try{
            ClientResponse response =  WEBRESOURCE.path("service/versioncheck/" + Tools.WEBSERVICE_APP_ID + "/current").get(ClientResponse.class);
            if(response.getClientResponseStatus() == ClientResponse.Status.OK && response.hasEntity()){
                String entity = response.getEntity(String.class);
                byte[] bytes = entity.getBytes();
                InputStream is = new ByteArrayInputStream(bytes);
                if(is != null){
                    /*XmlApplication app = JAXB.unmarshal(is, XmlApplication.class);
                    if(app != null){
                        if(app.getVersion() != null && !app.getVersion().equalsIgnoreCase(Tools.WEBSERVICE_VERSION_SHORT)){
                            return true;
                        }
                    }*/
                }
            }
        }catch(Exception e){
            //Todo logging
        }
        return false;
    }

    public static boolean isMetaDataServiceAvailable(){
        try{
            ClientResponse response =  WEBRESOURCE.path("service/metadata/available").get(ClientResponse.class);
            if(response.getClientResponseStatus() == ClientResponse.Status.OK && response.hasEntity()){
                String entity = response.getEntity(String.class);
                if(entity != null && entity.equalsIgnoreCase("false")){
                    return false;
                }
            }
        }catch(Exception e){
            //Todo logging
        }
        return true;
    }

    public static boolean isWebServiceAvailable(){
        try{
            ClientResponse response =  WEBRESOURCE.path("service/metadata/available").get(ClientResponse.class);
        }catch(Exception e){
            return false;
        }
        return true;
    }

    public static boolean isInternetAvailable(){
        try{
            ClientResponse response =  INTERNETRESOURCE.get(ClientResponse.class);
        }catch(Exception e){
            return false;
        }
        return true;
    }

    public enum WebServiceStatus {
        OK,
        NO_METADATA,
        UNAVAILABLE,
        OUTDATED,
        WEBSERVICE_DOWN,
        NO_INTERNET
    }
}
