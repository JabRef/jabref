/**
 *
 */
package net.sf.jabref.gui.entryeditor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.jabref.gui.desktop.JabRefDesktop;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *Works only 5 times. TODO: Check why
 *
 */
public class EntryEditorTabRelatedArticles extends JEditorPane implements Runnable {

    //The title of the selected entry, transfered to server
    private String request;

    //The content displayed in the tab
    private String htmlContent = "";

    //Default content while getting the server content from the server
    private String htmlContentLoading = "";

    //Stores the single recommendations in html format
    private final String[] htmlSnippets = new String[10];

    //The response from the server
    private String response;


    //Constructor initialised with the default content.
    public EntryEditorTabRelatedArticles(String request) {
        this.setContentType("text/html");
        this.request = request;
        this.setEditable(false);
        setDefaultContent();
    }

    /*
     * Sets the default content. Like a "loading screen".
     * With a gif as animation (gif is only a placeholder and can be adjusted at any time).
     */
    private void setDefaultContent() {
        //What is the best way to include that gif?
        //AuxCommandLineTest.class.getResource("paper.aux");
        //File f = Paths.get(url.toUri()).toFile();
        //concrete example
        //File auxFile = Paths.get(AuxCommandLineTest.class.getResource("paper.aux").toURI()).toFile();
        URL url = getClass().getResource("loading_animation.gif");
        System.out.println(url);
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        htmlContentLoading = "<html><head><title>Hallo </title></head><body bgcolor='#ffffff'><font size=8>"
                + "Loading Recommendations for " + request + "<img width=\"100\" height=\"100\" src=\""
                + url
                + "\"></img>"
                + "</font></body></html>";
        System.out.println("----html Content: " + htmlContentLoading);
        this.setText(htmlContentLoading);
    }



    /*
     *Makes a REST request in an own Thread. Parses the resopns and formats it into the panel.
     *At the very end, the panel is updated (setText()) with the 6 recommendations
     */
    @Override
    public void run() {
        System.out.println("----- Thread Started -----");
        try {
            SSLContext sc = SSLContext.getInstance("TLSv1");
            System.setProperty("https.protocols", "TLSv1");
            //Manages certificates for SSL communication
            TrustManager[] trustAllCerts = {new X509TrustManager() {

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    // TODO Auto-generated method stub
                    return new X509Certificate[0];
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    // TODO Auto-generated method stub

                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    // TODO Auto-generated method stub

                }
            }};
            System.out.println("TrustManager setup complete");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            //Returns true, accepts all certificates. To change.
            HostnameVerifier allHostsValid = new HostnameVerifier() {

                @Override
                public boolean verify(String hostname, SSLSession session) {
                    // TODO Auto-generated method stub
                    return true;
                }
            };

            //Makes a request to the RESTful MDL-API. Example document.
            //Servers-side implementations in implementation.
            Client client = ClientBuilder.newBuilder().sslContext(sc).hostnameVerifier(allHostsValid).build();
            WebTarget mdlServer = client
                    .target("https://api-dev.mr-dlib.org/v1/documents/gesis-smarth-0000003299/related_documents/");
            response = mdlServer.request(MediaType.APPLICATION_XML).get(String.class);

            //Conversion. Server delivers false format, conversion here, TODO to fix
            response = response.replaceAll("&gt;", ">");
            response = response.replaceAll("&lt;", "<");
            System.out.println("response formatted: " + response);
            client.close();

            //Makes links clickable with a hyperlink listener, opening the browser with a JABREF desktop
            this.addHyperlinkListener(new HyperlinkListener() {

                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        System.out.println(e.getURL());
                        try {
                            new JabRefDesktop().openBrowser(e.getURL().toString());
                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                }
            });
            System.out.println("-------------------------");

        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException");
            e.printStackTrace();
            System.out.println("");
        } catch (KeyManagementException e) {
            System.out.println("KeyManagementException");
            System.out.println("");
            e.printStackTrace();
        } catch (ProcessingException e) {
            System.out.println("ProcessingException");
        }

        //Parsing the response with a SAX parser
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {

                boolean snippet = false;


                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {
                    if (qName.equalsIgnoreCase("snippet")) {
                        if (attributes.getValue(0).equalsIgnoreCase("html_fully_formatted")) {
                            //System.out.println("qualified name and atrribute value: " + qName + attributes.getValue(0));
                            snippet = true;
                        }

                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {

                }

                @Override
                public void characters(char ch[], int start, int length) throws SAXException {

                    if (snippet) {
                        System.out.println("snippet: " + new String(ch, start, length));
                        htmlContent = htmlContent + "<li>" + new String(ch, start, length) + "</li>";
                        snippet = false;
                    }

                }

            };
            try {
                InputStream stream = new ByteArrayInputStream(response.getBytes());
                saxParser.parse(stream, handler);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //Updates the content with the 6 recommentations
        this.setText(
                "<html><head><title></title></head><body bgcolor='#ffffff'><ul>" + htmlContent + "</ul></body></html>");

    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

}
