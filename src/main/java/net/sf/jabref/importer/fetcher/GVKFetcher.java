package gvkPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.OutputPrinter;
import net.sf.jabref.Util;
import net.sf.jabref.gui.ImportInspectionDialog;
import net.sf.jabref.imports.EntryFetcher;
import net.sf.jabref.imports.ImportInspector;

import java.net.URLEncoder;

/**
 * Fetch or search from GVK http://gso.gbv.de/sru/DB=2.1/
 */
public class GVKFetcher implements EntryFetcher {
	
	HashMap <String,String> searchKeys = new HashMap<String,String>();
	
	public GVKFetcher()
	{
        searchKeys.put("all", "pica.all%3D");
        searchKeys.put("tit", "pica.tit%3D");
        searchKeys.put("per", "pica.per%3D");
        searchKeys.put("thm", "pica.thm%3D");
        searchKeys.put("slw", "pica.slw%3D");
        searchKeys.put("txt", "pica.txt%3D");
        searchKeys.put("num", "pica.num%3D");
        searchKeys.put("kon", "pica.kon%3D");
        searchKeys.put("ppn", "pica.ppn%3D");
        searchKeys.put("bkl", "pica.bkl%3D");
        searchKeys.put("erj", "pica.erj%3D");
	}


    boolean shouldContinue;

    //OutputPrinter frame;

    ImportInspector dialog;

    /**
     * Necessary for JabRef
     */

    public void stopFetching() 
    {
        shouldContinue = false;
    }
    
    /**
     * Get the name of the help page for this fetcher.
     * 
     * If given, a question mark is displayed in the side pane which leads to
     * the help page.
     * 
     * @return The name of the help file or null if this fetcher does not have
     *         any help.
     */

    public String getHelpPage() 
    {
        //return GUIGlobals.medlineHelp;
    	return null;
    }
    
    /**
     * Get the appropriate icon URL for this fetcher.
     * 
     * @return The icon URL
     */
    public URL getIcon() {
        return GUIGlobals.getIconUrl("www");
    }

    /**
     * Get the name of the key binding for this fetcher, if any.
     * 
     * @return The name of the key binding or null, if no keybinding should be
     *         created.
     */
    public String getKeyName() {
        // return "Fetch GVK";
    	return null;
    }

    /**
     * If this fetcher requires additional options, a panel for setting up these
     * should be returned in a JPanel by this method. This JPanel will be added
     * to the side pane component automatically.
     * 
     * @return Options panel for this fetcher or null if this fetcher does not
     *         have any options.
     */
    
    public JPanel getOptionsPanel() 
    { 
    	return null; 
    }

    
    /**
     * The title for this fetcher, displayed in the menu and in the side pane.
     * 
     * @return The title
     */
    public String getTitle() 
    {
        return Globals.menuTitle("Search GVK");
    }

    /**
     * Handle a query entered by the user.
     * 
     * The method is expected to block the caller until all entries have been
     * reported to the inspector.
     * 
     * @param query
     *            The query text.
     * @param inspector
     *            The dialog to add imported entries to.
     * @param status
     *            An OutputPrinter passed to the fetcher for reporting about the
     *            status of the fetching.
     * 
     * @return True if the query was completed successfully, false if an error
     *         occurred.
     */
    
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter frame) {

    	String gvkQuery = "";
        boolean shouldContinue = true;

        query = query.trim(); 
     
        String[] qterms = query.split("\\s");
        
        // Null abfangen!
        if (qterms.length == 0)
        {
        	return false;
        }
        
        // Jeden einzelnen Suchbegriff URL-Encodieren
    	for (int x=0; x<qterms.length; x++)
        {
    		try
    		{
    			qterms[x] = URLEncoder.encode(qterms[x], "UTF-8");
    		}
    		catch (UnsupportedEncodingException e) { System.out.println("Unsupported encoding");};
        }	
        
        if (searchKeys.containsKey(qterms[0]))
        {
        	gvkQuery = processComplexQuery(qterms);
        }
        else
        {
        	gvkQuery = "pica.all%3D";
        	gvkQuery = gvkQuery.concat(qterms[0]);
        		
        	for (int x=1; x<qterms.length; x++)
            {
            	gvkQuery = gvkQuery.concat("%20");
            	gvkQuery = gvkQuery.concat(qterms[x]);
            }	
        }

        List<BibtexEntry> bibs = fetchGVK(gvkQuery);
        
        for (BibtexEntry entry : bibs)
        {
            dialog.addEntry(entry);
        }
        
        if (bibs.size() == 0) 
        {
        	frame.showMessage(Globals.lang("No references found"));
        }

        return true;
    }
     
    private String processComplexQuery(String[] s)
    {
    	String result = "";
    	boolean lastWasKey = false;
    	
    	for (int x=0; x<s.length; x++)
        {
    		if (searchKeys.containsKey(s[x]))
    		{
    			if (!(x == 0))
    			{
    				result = result.concat("%20and%20" + searchKeys.get(s[x]));
    			}
    			else
    			{
    				result = searchKeys.get(s[x]);
    			}
    			lastWasKey = true;
    		}
    		else
    		{
    			if (!lastWasKey) 
    			{
    				result = result.concat("%20");
    			}
    			String encoded = s[x];
    			encoded = encoded.replaceAll(",","%2C");
    			encoded = encoded.replaceAll("\\?","%3F");
    			
    			result = result.concat(encoded);
    			lastWasKey = false;
    		}
        }
    	return(result);
    }
    
    private List<BibtexEntry> fetchGVK(String query) 
    {
    	List<BibtexEntry> result= null;
    	
        String urlPrefix = "http://sru.gbv.de/gvk?version=1.1&operation=searchRetrieve&query=";
        String urlQuery = query;
        String urlSuffix = "&maximumRecords=50&recordSchema=picaxml&sortKeys=Year%2C%2C1";
	//Systemmeldung zum Debugging (JabRef über bash starten)
        //System.out.println(urlPrefix+query+urlSuffix);
        
        String searchstring = (urlPrefix + urlQuery + urlSuffix);
	//Systemmeldung zum Debugging (JabRef über bash starten)
	//System.out.println(searchstring);
       try 
        {
    	   	URI uri = null;
			try 
			{
				uri = new URI(searchstring);
			} 
			catch (URISyntaxException e) 
			{
				System.out.println("URI malformed error");
				return (new ArrayList<BibtexEntry>());
			}
            // URL url = new URL(urlPrefix + urlQuery + urlSuffix);
    	   	URL url = uri.toURL();
            InputStream is = url.openStream(); 
            DocumentBuilder dbuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document content = dbuild.parse(is);   		
            result= (new GVKParser()).parseEntries(content);
            is.close();
        }  
 
       catch (IOException e)       
       {
    	   System.out.println("GVK plugin: An I/O exception occurred:");
    	   System.out.println(e);
        	
    	   return new ArrayList<BibtexEntry>();   
       } 
       catch (ParserConfigurationException e)
       {
    	   System.out.println("GVK plugin: An internal parser error occurred:");
    	   System.out.println(e);
        	
    	   return new ArrayList<BibtexEntry>();   
       }
       catch (SAXException e)
       {
    	   System.out.println("An internal parser error occurred:");
    	   System.out.println(e);
        	
    	   return new ArrayList<BibtexEntry>();   
       }

        return result;
    }
}    			        
