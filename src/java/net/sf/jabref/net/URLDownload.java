/*
 * Created on 1-Dec-2004
 *
 */
package net.sf.jabref.net;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.swing.ProgressMonitorInputStream;

/**
 * @author Erik Putrycz erik.putrycz-at-nrc-cnrc.gc.ca
 */

public class URLDownload {  
    
    private URL source;
    private File dest;
    private Component parent;

    public URLDownload(Component _parent, URL _source, File _dest) {
        source = _source;
        dest = _dest;
        parent = _parent;
    }
    
    public void download() throws IOException {
        InputStream input = source.openStream();
        FileOutputStream output =  new FileOutputStream(dest);
     
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
        ProgressMonitorInputStream _in = new ProgressMonitorInputStream(parent, "Downloading " + source.toString(), in);
        byte[] buffer = new byte[256];
        synchronized(in)
          {
            synchronized(out)
              {
                while(true)
                  {
                    int bytesRead = _in.read(buffer);
                    if(bytesRead == -1) break;
                    out.write(buffer, 0, bytesRead);
                  }
              }
          }
      }    
}
