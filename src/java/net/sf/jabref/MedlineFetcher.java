package net.sf.jabref;

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
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertEntry;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class MedlineFetcher extends SidePaneComponent implements Runnable {

  SidePaneHeader header = new SidePaneHeader("Fetch Medline", GUIGlobals.fetchMedlineIcon, this);
  BasePanel panel;
  JTextField tf = new JTextField();
  JPanel pan = new JPanel();
  GridBagLayout gbl = new GridBagLayout();
  GridBagConstraints con = new GridBagConstraints();
  JButton ok = new JButton(new ImageIcon(GUIGlobals.fetchMedlineIcon));
  MedlineFetcher ths = this;

  public MedlineFetcher(BasePanel panel_, SidePaneManager p0) {
    super(p0);
    panel = panel_;
    tf.setMinimumSize(new Dimension(1,1));
    //add(hd, BorderLayout.NORTH);
    ok.setToolTipText(Globals.lang("Fetch Medline"));
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

  public void run() {
    String idList = tf.getText().replace(';', ',');
    if(idList==null || idList.trim().equals(""))//if user pressed cancel
      return;
    Pattern p = Pattern.compile("\\d+[,\\d+]*");
    Matcher m = p.matcher( idList );
    if ( m.matches() ) {
      panel.frame.output(Globals.lang("Fetching Medline..."));
      ArrayList bibs = fetchMedline(idList);
      if ((bibs != null) && (bibs.size() > 0)) {
        tf.setText("");
        NamedCompound ce = new NamedCompound(Globals.lang("Fetch Medline"));
        Iterator i = bibs.iterator();
        while (i.hasNext()) {
          try {
            BibtexEntry be = (BibtexEntry) i.next();
            String id = Util.createId(be.getType(), panel.database);
            be.setId(id);
            panel.database.insertEntry(be);
            ce.addEdit(new UndoableInsertEntry(panel.database, be, panel));
          }
          catch (KeyCollisionException ex) {
          }
        }
        ce.end();
        panel.output(Globals.lang("Medline entries fetched:")+" "+bibs.size());
        panel.undoManager.addEdit(ce);
        panel.markBaseChanged();
        panel.refreshTable();
      } else
        panel.output(Globals.lang("No Medline entries found."));
    } else {
      JOptionPane.showMessageDialog(panel.frame,"Sorry, I was expecting a semicolon or comma separated list of Medline IDs (numbers)!","Input Error",JOptionPane.ERROR_MESSAGE);
    }
  }



//==================================================
//
//==================================================
  public ArrayList fetchMedline(String id)
  {
    ArrayList bibItems=null;
    try {

      String baseUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&rettype=citation&id=" + id;

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
}
