package net.sf.jabref.imports;

import java.util.ArrayList;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertEntry;
import java.io.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class MedlineAuthorFetcher extends SidePaneComponent implements Runnable {

    SidePaneHeader header = new SidePaneHeader("Query author(s)", GUIGlobals.fetchMedlineIcon, this);
    BasePanel panel;
    JTextField tf = new JTextField();

    JPanel pan = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JButton ok = new JButton(new ImageIcon(GUIGlobals.fetchMedlineIcon));
    AuthorDialog authorDialog;
    MedlineAuthorFetcher ths = this;
    JFrame jFrame; // invisible dialog holder

    public MedlineAuthorFetcher(BasePanel panel_, SidePaneManager p0) {
	super(p0);
	panel = panel_;
	jFrame=new JFrame();
	tf.setMinimumSize(new Dimension(1,1));
	//add(hd, BorderLayout.NORTH);
	//ok.setToolTipText(Globals.lang("Fetch Medline"));
	setLayout(gbl);
	con.fill = GridBagConstraints.BOTH;
	con.insets = new Insets(0, 0, 2,  0);
	con.gridwidth = GridBagConstraints.REMAINDER;
	con.weightx = 1;
	con.weighty = 1;
	gbl.setConstraints(header, con);
	add(header);
	con.gridwidth = 1;
	con.insets = new Insets(0, 0, 0,  0);
	//    pan.setLayout(gbl);
	con.fill = GridBagConstraints.HORIZONTAL;
	gbl.setConstraints(tf, con);
	add(tf);
		con.weightx = 0;
	gbl.setConstraints(ok, con);
	//add(ok);
	ActionListener listener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    (new Thread(ths)).start(); // Run fetch in thread.
		}
	    };

	ok.addActionListener(listener);
	tf.addActionListener(listener);
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

    public void run() {

	String idList = tf.getText().replace(';', ',');

	if(idList==null || idList.trim().equals(""))//if user pressed cancel
	    return;
	Pattern p = Pattern.compile(".+[,.+]*");
	Matcher m = p.matcher( idList );
	if ( m.matches() ) {
	    panel.frame().output(Globals.lang("Fetching Medline..."));

	    // my stuff
	    //---------------------------
	    idList=setupTerm(idList); // fix the syntax
	    String[] idListArray=getIds(idList); // get the ids from entrez
	    int idMax=idListArray.length; // check length

	    String[] titles=new String[idMax]; // prepare an array of titles for the dialog
	    titles=getTitles(idListArray);
	    // get a list of which ids the user wants.
	    authorDialog=new AuthorDialog(jFrame, titles);
	    // prompt the user to select articles
	    boolean[] picks=authorDialog.showDialog();

	    idList="";
	    for (int i=0; i<idListArray.length;i++){
		if (picks[i]){
		    idList+=idListArray[i]+",";
		    //System.out.println(idListArray[i]);
		}
	    }
	    //System.out.println(idList);
		 //----------------------------
		 // end my stuff

	    ArrayList bibs = fetchMedline(idList);
	    if ((bibs != null) && (bibs.size() > 0)) {
		tf.setText("");
		NamedCompound ce = new NamedCompound("fetch Medline");
		Iterator i = bibs.iterator();
		while (i.hasNext()) {
		    try {
			BibtexEntry be = (BibtexEntry) i.next();
			String id = Util.createId(be.getType(), panel.database());
			be.setId(id);
			panel.database().insertEntry(be);
			ce.addEdit(new UndoableInsertEntry(panel.database(), be, panel));
		    }
		    catch (KeyCollisionException ex) {
		    }
		}
		ce.end();
		panel.output(Globals.lang("Medline entries fetched")+": "+bibs.size());
		panel.undoManager.addEdit(ce);
		panel.markBaseChanged();
		panel.refreshTable();
	    } else
		panel.output(Globals.lang("No Medline entries found."));

	} else {
	    JOptionPane.showMessageDialog(panel.frame(), Globals.lang("Please enter a semicolon or comma separated list of Medline IDs (numbers)."),Globals.lang("Input error"),JOptionPane.ERROR_MESSAGE);
	}
    }


    //==================================================
    //
    //==================================================
    public ArrayList fetchMedline(String id)
    {
	ArrayList bibItems=null;
	try {

	    String baseUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi"
	    	+"?db=pubmed&retmode=xml&rettype=citation&id=" + id;

	    URL url = new URL( baseUrl );
	    HttpURLConnection data = (HttpURLConnection)url.openConnection();

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
	    // When you're done, report the results stored by your handler object
	    bibItems = handler.getItems();

	}
	catch(javax.xml.parsers.ParserConfigurationException e1){}
	catch(org.xml.sax.SAXException e2){}
	catch(java.io.IOException e3){}
	return bibItems;
    }

    // loop through the array
    public String[] getCitations(String[] ids){
	String[] xmls = new String[ids.length];
	for (int i=0; i<ids.length; i++){
	    xmls[i]=getOneCitation(ids[i]);
	}
	return xmls;
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

    public String[] getTitles(String[] idArrayList){
	String[] titles = new String[idArrayList.length];
	String temp;
	for (int i=0; i<idArrayList.length; i++){
	    temp=getOneCitation(idArrayList[i]);
	    titles[i]=getVitalData(temp);
	}
	return titles;
    }

    // parse out the titles from the xml
    public String getVitalData(String sb){
	StringBuffer result=new StringBuffer();
	Pattern articleTitle=Pattern.compile("<ArticleTitle>(.+)</ArticleTitle>");
	Pattern authorName=Pattern.compile("<Author>(.+)</Author>");
	Matcher matcher;
	matcher=articleTitle.matcher(sb);
	if (matcher.find())
	    result.append("Title: "+matcher.group(1));
	//matcher=authorName.matcher(sb);
	//while (matcher.find())
	//   result.append("\tAuthor: "+matcher.group(1));
	return result.toString();
    }

    // this gets the initial list of ids
    public String[] getIds(String term){
	String baseUrl="http://eutils.ncbi.nlm.nih.gov/entrez/eutils";
	String medlineUrl = baseUrl+"/esearch.fcgi?retmax=20&usehistory=y&db=pubmed&term=";
	Pattern idPattern=Pattern.compile("<Id>(\\d+)</Id>");
	Pattern countPattern=Pattern.compile("<Count>(\\d+)<\\/Count>");
	Matcher matcher;
	boolean doCount=true;
	int count=0;
	String[] id=new String[10]; // doesn't matter
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
		if (doCount){
		    // get the count
		    matcher=countPattern.matcher(inLine);
		    if (matcher.find()){
			count=Integer.parseInt(matcher.group(1));
			id=new String[count];
			count=0;
			doCount=false;
		    }
		}
		else {
		    // get the ids
		    matcher=idPattern.matcher(inLine);
		    if (matcher.find()){
			id[count++]=matcher.group(1);
		    }
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
	return id;
    }




}
