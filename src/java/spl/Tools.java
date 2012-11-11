package spl;



import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Christoph Arbeit
 * Date: 09.09.2010
 * Time: 10:43:01
 * To change this template use File | Settings | File Templates.
 */
public class Tools {

    public static int WEBSERVICE_APP_ID = 9;
    public static String WEBSERVICE_VERSION_SHORT = "0.1";

    public static byte[] zip(File file){
        try{
            FileInputStream fileInputStream = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream out = new GZIPOutputStream(bos);

            byte[] buf = new byte[1024];
            int len;
            while ((len = fileInputStream.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            fileInputStream.close();
            out.finish();
            out.close();
            bos.close();
            return bos.toByteArray();
        }catch(IOException e){
            //Todo logging
            return null;
        }
    }

    public static String getStackTraceAsString(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.print(" [ ");
        pw.print(exception.getClass().getName());
        pw.print(" ] ");
        pw.print(exception.getMessage());
        exception.printStackTrace(pw);
        return sw.toString();
    }

    public static void centerRelativeToWindow (java.awt.Dialog diag, java.awt.Container win) {
          int x;
          int y;

          Point topLeft = win.getLocationOnScreen();
          Dimension parentSize = win.getSize();

          Dimension mySize = diag.getSize();

          if (parentSize.width > mySize.width)
            x = ((parentSize.width - mySize.width)/2) + topLeft.x;
          else
            x = topLeft.x;

          if (parentSize.height > mySize.height)
            y = ((parentSize.height - mySize.height)/2) + topLeft.y;
          else
            y = topLeft.y;

          diag.setLocation (x, y);
    }

    public static String getLink(String link, URL mindmapUrl){
        if(link == null || link.isEmpty()){
            return null;
        }
        if(!Tools.isAbsolutePath(link)){
            try{
                if(link.startsWith("\\\\")){
                    link = link.replace("\\\\", "file://");
                    link = link.replace('\\', '/').replaceAll(" ","%20");
                    URL url = new URL(link);
                    File file = new File(url.toURI());
                    return file.getPath();
                }
                else if(mindmapUrl != null){
                    URL url = new URL(mindmapUrl, link);
                    File file = new File(url.toURI());
                    return file.getPath();
                }
            } catch(MalformedURLException e){
                return link;
            } catch (URISyntaxException e) {
                return link;
            }catch(IllegalArgumentException e){
                return link;
            }
        }
        else{
            return link;
        }
        return link;
    }

    public static boolean isAbsolutePath(String path) {
        // On Windows, we cannot just ask if the file name starts with file
        // separator.
        // If path contains ":" at the second position, then it is not relative,
        // I guess.
        // However, if it starts with separator, then it is absolute too.

        // Possible problems: Not tested on Macintosh, but should work.
        // Koh, 1.4.2004: Resolved problem: I tested on Mac OS X 10.3.3 and
        // worked.

        String osNameStart = System.getProperty("os.name").substring(0, 3);
        String fileSeparator = System.getProperty("file.separator");
        if (osNameStart.equals("Win")) {
            //Todo SciPlore
            return ((path.length() > 1) && path.substring(1, 2).equals(":"))
                    || (path.startsWith(fileSeparator) && !path.startsWith("\\\\"));
        } else if (osNameStart.equals("Mac")) {
            //Koh:Panther (or Java 1.4.2) may change file path rule
            return path.startsWith(fileSeparator);
        } else {
            return path.startsWith(fileSeparator);
        }
    }
}
