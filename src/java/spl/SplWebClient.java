package spl;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.sciplore.xml.XmlApplication;
import org.sciplore.xml.XmlDocument;
import org.sciplore.xml.XmlDocuments;

import javax.swing.*;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

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
    private static WebResource WEBRESOURCE = CLIENT.resource( "http://api.mr-dlib.org/rest/" );
    private static WebResource INTERNETRESOURCE = CLIENT.resource( "http://www.google.com" );
    //private static WebResource WEBRESOURCE = CLIENT.resource( "http://localhost:8080/rest/" );

    public static XmlDocuments metadata;

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
            byte[] data = Tools.zip(file);
            FormDataMultiPart formDataMultiPart = new FormDataMultiPart();            
            formDataMultiPart.field("gzippedpdf", data,  MediaType.APPLICATION_OCTET_STREAM_TYPE);
            ClientResponse response = WEBRESOURCE.path("service/metadata").type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class, formDataMultiPart);
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
                    XmlDocuments documents = JAXB.unmarshal(is, XmlDocuments.class);
                    SplWebClient.metadata = documents;
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
                    XmlApplication app = JAXB.unmarshal(is, XmlApplication.class);
                    if(app != null){
                        if(app.getVersion() != null && !app.getVersion().equalsIgnoreCase(Tools.WEBSERVICE_VERSION_SHORT)){
                            return true;
                        }
                    }
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
