/**
 *
 */
package net.sf.jabref.gui.entryeditor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
 * @author Stefan Feyer
 *
 */
public class EntryEditorTabRelatedArticles extends JEditorPane implements Runnable {

    private String request;
    private String htmlContent = "";
    private final String htmlContentLoading;
    private final String[] htmlSnippets = new String[10];
    private String response;


    public EntryEditorTabRelatedArticles(String request) {
        this.setContentType("text/html");
        this.request = request;
        this.setEditable(false);
        htmlContentLoading = "<html><head><title>Hallo </title></head><body bgcolor='#ffffff'><font size=5>"
                + "Loading Recommendations for " + request + "</font></body></html>";
        this.setText(htmlContentLoading);
    }

    @Override
    public void run() {
        try {
            SSLContext sc = SSLContext.getInstance("TLSv1");
            System.setProperty("https.protocols", "TLSv1");
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
            HostnameVerifier allHostsValid = new HostnameVerifier() {

                @Override
                public boolean verify(String hostname, SSLSession session) {
                    // TODO Auto-generated method stub
                    return true;
                }
            };
            System.out.println("Hostname verifyer complete");
            Client client = ClientBuilder.newBuilder().sslContext(sc).hostnameVerifier(allHostsValid).build();
            System.out.println("client builded");
            WebTarget mdlServer = client
                    .target("https://api-dev.mr-dlib.org/v1/documents/gesis-smarth-0000003284/related_documents/");
            System.out.println("Target set");
            response = mdlServer.request(MediaType.APPLICATION_XML).get(String.class);
            System.out.println("tried to get the response");
            System.out.println(response);
            client.close();
            //            String[] responsear = response.split("((?=<snippet)|(?=<snippet))|((?<=</snippet>)|(?<=/snippet>))");
            String[] responsear = response.split("(<snippet format=\"html_fully_formatted\">)|</snippet>");

            for (int i = 1; i < (responsear.length); i += 2) {
                htmlContent = htmlContent + responsear[i] + "<br>";
                System.out.println(htmlContent);
            }
            htmlContent = htmlContent.replaceAll("&gt;", ">");
            htmlContent = htmlContent.replaceAll("&lt;", "<");
            System.out.println(htmlContent);
            this.setText("<html><head><title></title></head><body bgcolor='#ffffff'><font size=5>"
                    + "<a href=\"http://www.feyer.de\">feyer</a>" + "<a href=\"http://www.feyer2.de\">feyer2</a>"
                    + "</font></body></html>");
            this.addHyperlinkListener(new HyperlinkListener() {

                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        System.out.println(e.getURL());
                        try {
                            //new Windows().openFileWithApplication(e.getURL().toString(), ExternalFileTypes.getInstance()
                            //.getExternalFileTypeByExt("html").get().getOpenWithApplication());
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

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {

                boolean snippet = false;


                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {

                    //                    System.out.println("Start Element :" + qName);
                    //                    System.out.println("uri: " + uri);
                    //                    System.out.println("localName: " + localName);

                    if (qName.equalsIgnoreCase("snippet")) {
                        if (attributes.getValue(0).equalsIgnoreCase("html_fully_formatted")) {
                            System.out.println("dasda: " + qName + attributes.getValue(0));
                            snippet = true;
                        }

                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {

                    //                    System.out.println("End Element :" + qName);

                }

                @Override
                public void characters(char ch[], int start, int length) throws SAXException {
                    //                    System.out.println("start: " + start);
                    //                    System.out.println("length: " + length);

                    if (snippet) {
                        System.out.println("snippet: " + new String(ch, start, length));
                        htmlContent += htmlContent;
                        snippet = false;
                    }

                }

            };
            response = response.replaceAll("&", "&amp;");
            //            response = response.replaceAll("&lt;", "yyyyy");
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
