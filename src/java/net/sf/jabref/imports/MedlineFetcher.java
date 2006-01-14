package net.sf.jabref.imports;

import java.util.ArrayList;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertEntry;
import java.io.*;
import net.sf.jabref.HelpAction;
import net.sf.jabref.gui.ImportInspectionDialog;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class MedlineFetcher extends SidePaneComponent implements Runnable,
        ImportInspectionDialog.CallBack {

    /**@class SearchResult
     *        nested class.
     */
    public class SearchResult {
	public int count;
	public int retmax;
	public int retstart;
	public String ids = "";
    public ArrayList idList = new ArrayList();
	public SearchResult()
	    {
		count = 0;
		retmax = 0;
		retstart = 0;
	    }

	public void addID(String id)
        {

        idList.add(id);
		if(!ids.equals(""))
		    ids += ","+id;
		else
		    ids = id;
	    }
    }
    final int PACING = 20;
    final int MAX_TO_FETCH = 10;
    boolean keepOn = true;
    String idList;
    JTextField tf = new JTextField();
    JPanel pan = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    MedlineFetcher ths = this;
    AuthorDialog authorDialog;
    JFrame jFrame; // invisible dialog holder
    JButton go = new JButton(Globals.lang("Fetch")),
	helpBut = new JButton(new ImageIcon(GUIGlobals.helpIconFile));
    HelpAction help;
    
    public MedlineFetcher(SidePaneManager p0) {
	super(p0, GUIGlobals.fetchMedlineIcon, Globals.lang("Fetch Medline"));

	help = new HelpAction(Globals.helpDiag, GUIGlobals.medlineHelp, "Help");
	helpBut.addActionListener(help);
	helpBut.setMargin(new Insets(0,0,0,0));        
	//tf.setMinimumSize(new Dimension(1,1));
	//add(hd, BorderLayout.NORTH);
	//ok.setToolTipText(Globals.lang("Fetch Medline"));
        JPanel main = new JPanel();
    	main.setLayout(gbl);
	con.fill = GridBagConstraints.BOTH;
	//con.insets = new Insets(0, 0, 2,  0);
	con.gridwidth = GridBagConstraints.REMAINDER;
	con.weightx = 1;
	con.weighty = 1;
	con.fill = GridBagConstraints.BOTH;
	gbl.setConstraints(tf, con);
	main.add(tf);
	con.weighty = 0;
	con.gridwidth = 1;
	gbl.setConstraints(go, con);
	main.add(go);
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(helpBut, con);
	main.add(helpBut);
	ActionListener listener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    (new Thread(ths)).start(); // Run fetch in thread.
		}
	    };
        main.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
	add(main, BorderLayout.CENTER);
	go.addActionListener(listener);
	tf.addActionListener(listener);
        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent event) {
                if (!event.isTemporary() && (tf.getText().length()>0)) {
                    tf.selectAll();
                }
            }
        });
    }
    
    public JTextField getTextField() {
        return tf;
    }
    
    public void fetchById() {
	//if(idList==null || idList.trim().equals(""))//if user pressed cancel
	//  return;
	Pattern p = Pattern.compile("\\d+[,\\d+]*");
	//System.out.println(""+p+"\t"+idList);
	Matcher m = p.matcher( idList );
	if ( m.matches() ) {
	    panel.frame().output(Globals.lang("Fetching Medline by ID..."));
	    
	    ArrayList bibs = fetchMedline(idList);
	    if ((bibs != null) && (bibs.size() > 0)) {
		//if (panel.prefs().getBoolean("useOwner")) {
		//    Util.setDefaultOwner(bibs, panel.prefs().get("defaultOwner"));
		//}
		tf.setText("");
		/*NamedCompound ce = new NamedCompound("fetch Medline");
		Iterator i = bibs.iterator();
		while (i.hasNext()) {
		    try {
			BibtexEntry be = (BibtexEntry) i.next();
			String id = Util.createId(be.getType(), panel.database());
			be.setId(id);
			entries.add(be);
			//panel.database().insertEntry(be);
			//ce.addEdit(new UndoableInsertEntry(panel.database(), be, panel));
		    }
		    catch (KeyCollisionException ex) {
		    }
		    }*/
		//ce.end();

        panel.frame().addImportedEntries(panel, bibs, null, false, this);

        /*
		int importedEntries = panel.frame().addBibEntries(bibs, null, false);
        if (importedEntries == 0) {
            return; // Nothing to refresh!
        }
        panel.markBaseChanged();
		panel.refreshTable();
        if (bibs.size() > 0) {
            BibtexEntry[] entries = (BibtexEntry[])bibs.toArray(new BibtexEntry[0]);
            panel.selectEntries(entries, 0);
            if (entries.length == 1)
                panel.showEntry(entries[0]);
            //else
            //    panel.updateViewToSelected();
        }*/

		//panel.undoManager.addEdit(ce);
	    } else
		panel.output(Globals.lang("No Medline entries found."));
	} else {
	    JOptionPane.showMessageDialog(panel.frame(),Globals.lang("Please enter a semicolon or comma separated list of Medline IDs (numbers)."),Globals.lang("Input error"),JOptionPane.ERROR_MESSAGE);
	}
    }
    
    

//==================================================
//
//==================================================
  public static ArrayList fetchMedline(String id)
  {
    ArrayList bibItems=null;
    try {

      String baseUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&rettype=citation&id=" + id;

      URL url = new URL( baseUrl );
      HttpURLConnection data = (HttpURLConnection)url.openConnection();


       /* Reader un = new InputStreamReader(data.getInputStream());
        int c;
        while ((c=un.read()) != -1) {
          System.out.print((char)c);
        }*/
        
        
        // Obtain a factory object for creating SAX parsers
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        // Configure the factory object to specify attributes of the parsers it creates
        parserFactory.setValidating(true);
        parserFactory.setNamespaceAware(true);

        // Now create a SAXParser object
        SAXParser parser = parserFactory.newSAXParser();   //May throw exceptions
        MedlineHandler handler = new MedlineHandler();
        // Start the parser. It reads the file and calls methods of the handler.

        parser.parse( data.getInputStream(), handler);
	/*FileOutputStream out = new FileOutputStream(new File("/home/alver/ut.txt"));
	System.out.println("#####");
	InputStream is = data.getInputStream();
	int c;
	while ((c = is.read()) != -1) {
	    out.write((char)c);
	}
	System.out.println("#####");
	out.close();*/
        // When you're done, report the results stored by your handler object
        bibItems = handler.getItems();

    }
    catch(javax.xml.parsers.ParserConfigurationException e1){}
    catch(org.xml.sax.SAXException e2){}
    catch(java.io.IOException e3){}
    return bibItems;
}

   public void run() {

	idList = tf.getText().replace(';', ',');

	//if(idList==null || idList.trim().equals(""))//if user pressed cancel
	//    return;
        Pattern p1 = Pattern.compile("\\d+[,\\d+]*"),
            p2 = Pattern.compile(".+[,.+]*");

         Matcher m1 = p1.matcher( idList ),
             m2 = p2.matcher( idList );
         if ( m1.matches() ) {
	     panel.frame().output(Globals.lang("Fetching Medline by id ..."));
	     idList = tf.getText().replace(';', ',');
	     fetchById();
	     //System.out.println("Fetch by id");
         }
         else if ( m2.matches() ) {
	    panel.frame().output(Globals.lang("Fetching Medline by term ..."));

	    // my stuff
	    //---------------------------
	    String searchTerm = setupTerm(idList); // fix the syntax
	    SearchResult result = getIds(searchTerm ,0,1); // get the ids from entrez
	    // prompt the user to number articles to retrieve
            if (result.count == 0) {
                JOptionPane.showMessageDialog(panel.frame(), Globals.lang("No references found"));
                return;
            }
	    String question = 
		Globals.lang("References found")+": "
		+ Integer.toString(result.count)+"  "
		+ Globals.lang("Number of references to fetch?");
	    String strCount = 
		JOptionPane.showInputDialog(question, 
					    Integer.toString(result.count));
	    
	    // for strCount ...
	    if(strCount.equals(""))
		    return;
	    int count;
        try {
            count = Integer.parseInt(strCount);
        } catch (NumberFormatException ex) {
            panel.output("");
            return;
        }

        ImportInspectionDialog diag = new ImportInspectionDialog(panel.frame(), panel,
                GUIGlobals.DEFAULT_INSPECTION_FIELDS, Globals.lang("Fetch Medline"), false);
        Util.placeDialog(diag, panel.frame());
         diag.setDefaultSelected(false); // Make sure new entries are not selected by default.

             // diag.setProgress(0, count);
        diag.setVisible(true);
        keepOn = true;
         diag.addCallBack(new ImportInspectionDialog.CallBack() {
             public void done(int entriesImported) {
                 if (entriesImported > 0) {
                 panel.output(Globals.lang("Medline entries fetched")+": "+entriesImported);
                 panel.markBaseChanged();
             } else
                 panel.output(Globals.lang("No Medline entries found."));
            }

             public void cancelled() {
                 panel.output(Globals.lang("%0 import cancelled.", "Medline"));
             }


             public void stopFetching() {
                // Make sure the fetch loop exits at next iteration.
                keepOn = false;
             }
         });
	    for (int jj = 0; jj < count; jj+=PACING) {
            if (!keepOn)
                break;
		    // get the ids from entrez
		    result = getIds(searchTerm,jj,PACING);

            /*String[] test = getTitles((String[])result.idList.toArray(new String[0]));
            for (int pelle=0; pelle<test.length; pelle++) {
                System.out.println(": "+test[pelle]);
            } */

            final ArrayList bibs = fetchMedline(result.ids);
            if (!keepOn)
                break;
            diag.addEntries(bibs);
            diag.setProgress(jj+PACING, count);
	    }
         diag.entryListComplete();
	 }
   }
    public String setupTerm(String in){
        Pattern part1=Pattern.compile(", ");
        Pattern part2=Pattern.compile(",");
        Pattern part3=Pattern.compile(" ");
        Matcher matcher;
        matcher=part1.matcher(in);
        in=matcher.replaceAll("\\+AND\\+");
        matcher=part2.matcher(in);
        in=matcher.replaceAll("\\+AND\\+");
        matcher=part3.matcher(in);
        in=matcher.replaceAll("+");

        return in;
    }

    // this gets the initial list of ids
    public SearchResult getIds(String term, int start,int pacing){
        String baseUrl="http://eutils.ncbi.nlm.nih.gov/entrez/eutils";
        String medlineUrl = baseUrl
	    +"/esearch.fcgi?db=pubmed&retmax="
	    +Integer.toString(pacing)
	    +"&retstart="+Integer.toString(start)
	    +"&term=";
        Pattern idPattern=Pattern.compile("<Id>(\\d+)</Id>");
        Pattern countPattern=Pattern.compile("<Count>(\\d+)<\\/Count>");
	Pattern retMaxPattern=Pattern.compile("<RetMax>(\\d+)<\\/RetMax>");
	Pattern retStartPattern=Pattern.compile("<RetStart>(\\d+)<\\/RetStart>");
        Matcher idMatcher;
        Matcher countMatcher;
        Matcher retMaxMatcher;
        Matcher retStartMatcher;
	boolean doCount = true;
	SearchResult result = new SearchResult();
	//System.out.println(medlineUrl+term);
        try{
            URL ncbi = new URL(medlineUrl+term);
            // get the ids
            HttpURLConnection ncbiCon=(HttpURLConnection)ncbi.openConnection();
            BufferedReader in =
                new BufferedReader
                (new InputStreamReader
                 ( ncbi.openStream()));
            String inLine;
            while ((inLine=in.readLine())!=null){

		// get the count
		idMatcher=idPattern.matcher(inLine);
		if (idMatcher.find()){
		    result.addID(idMatcher.group(1));
		}
		retMaxMatcher=retMaxPattern.matcher(inLine);
		if (idMatcher.find()){
		    result.retmax=Integer.parseInt(retMaxMatcher.group(1));
		}
		retStartMatcher=retStartPattern.matcher(inLine);
		if (retStartMatcher.find()){
		    result.retstart=Integer.parseInt(retStartMatcher.group(1));
		}
		countMatcher=countPattern.matcher(inLine);
		if (doCount && countMatcher.find()){
		    result.count=Integer.parseInt(countMatcher.group(1));
		    doCount = false;
		}
            }

        }
        catch (MalformedURLException e) {     // new URL() failed
            System.out.println("bad url");
            e.printStackTrace();
        }
        catch (IOException e) {               // openConnection() failed
            System.out.println("connection failed");
            e.printStackTrace();

        }
        return result;
    }

    public String[] getTitles(String[] idArrayList) {
      String[] titles = new String[Math.min(MAX_TO_FETCH, idArrayList.length)];
        String temp;
        for (int i=0; i<Math.min(MAX_TO_FETCH, idArrayList.length); i++){
            temp=getOneCitation(idArrayList[i]);
            titles[i]=getVitalData(temp);
        }
        return titles;
    }

        // get the xml for an entry
    public String getOneCitation(String id){
	String baseUrl="http://eutils.ncbi.nlm.nih.gov/entrez/eutils";
	String retrieveUrl = baseUrl+"/efetch.fcgi?db=pubmed&retmode=xml&rettype=citation&id=";
	StringBuffer sb=new StringBuffer();
	try{
	    URL ncbi = new URL(retrieveUrl+id);
	    HttpURLConnection ncbiCon=(HttpURLConnection)ncbi.openConnection();
	    BufferedReader in =
		new BufferedReader
		(new InputStreamReader
		 ( ncbi.openStream()));
	    String inLine;
	    while ((inLine=in.readLine())!=null){

		sb.append(inLine);
	    }

	}
	catch (MalformedURLException e) {     // new URL() failed
	    System.out.println("bad url");
	    e.printStackTrace();
	}
	catch (IOException e) {               // openConnection() failed
	    System.out.println("connection failed");
	    e.printStackTrace();

	}
	return sb.toString();
    }

        // parse out the titles from the xml
    public String getVitalData(String sb){
	StringBuffer result=new StringBuffer();
	Pattern articleTitle=Pattern.compile("<ArticleTitle>(.+)</ArticleTitle>");
	Pattern authorName=Pattern.compile("<Author>(.+)</Author>");
	Matcher matcher;
	matcher=articleTitle.matcher(sb);
	if (matcher.find())
        result.append("Title: ").append(matcher.group(1));

	//matcher=authorName.matcher(sb);
	//while (matcher.find())
	//   result.append("\tAuthor: "+matcher.group(1));
	return result.toString();
    }

    // This method is called by the dialog when the user has selected the
    // wanted entries, and clicked Ok. The callback object can update status
    // line etc.
    public void done(int entriesImported) {
        panel.output(Globals.lang("Medline entries fetched")+": "+entriesImported);
    }

    public void cancelled() {
        panel.output(Globals.lang("%0 import cancelled.", "Medline"));
    }


    // This method is called by the dialog when the user has cancelled or
    // signalled a stop. It is expected that any long-running fetch operations
    // will stop after this method is called.
    public void stopFetching() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
