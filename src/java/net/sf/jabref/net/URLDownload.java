/*
 * Created on 1-Dec-2004
 *
 */
package net.sf.jabref.net;

import java.awt.Component;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.CookieHandler;
import java.net.CookieManager;

import javax.swing.ProgressMonitorInputStream;

/**
 * @author Erik Putrycz erik.putrycz-at-nrc-cnrc.gc.ca
 */

public class URLDownload {  
    
    private URL source;
    private URLConnection con = null;
    private File dest;
    private Component parent;
    private String mimeType = null;

    private CookieHandler cm;

    public URLDownload(Component _parent, URL _source, File _dest) {
        source = _source;
        dest = _dest;
        parent = _parent;

        try {
            // This should set up JabRef to receive cookies properly
            if ((cm = CookieHandler.getDefault()) == null) {
                cm = new CookieManager();
                CookieHandler.setDefault(cm);
            }
        } catch (SecurityException e) {
            // Setting or getting the system default cookie handler is forbidden
            // In this case cookie handling is not possible.
        }
    }

    public String getMimeType() {
        return mimeType;
    }

    public void openConnectionOnly() throws IOException {
        con = source.openConnection();
        con.setRequestProperty("User-Agent", "Jabref");
        mimeType = con.getContentType();
    }

    public void download() throws IOException {

        if (con == null) {
            con = source.openConnection();
            con.setRequestProperty("User-Agent", "Jabref");
            mimeType = con.getContentType();
        }

    	InputStream input = new BufferedInputStream(con.getInputStream());
        OutputStream output =  new BufferedOutputStream(new FileOutputStream(dest));
     
        try
          {
            copy(input, output);
          }
        catch (IOException e)
          {
            e.printStackTrace();
          }
        finally
          {
            try
              {
                input.close();
                output.close();
              }
            catch (Exception e)
              {
              }
          }        
    }

    public void copy(InputStream in, OutputStream out) throws IOException
      {
        InputStream _in = new ProgressMonitorInputStream(parent, "Downloading " + source.toString(), in);
        byte[] buffer = new byte[512];
        while(true)
        {
            int bytesRead = _in.read(buffer);
            if(bytesRead == -1) break;
            out.write(buffer, 0, bytesRead);
        }        
      }   
}
