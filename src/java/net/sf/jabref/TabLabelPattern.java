/*
 * Created on 09-Dec-2003
 */
package net.sf.jabref;

import java.util.Iterator;
import java.util.HashMap;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Font;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ImageIcon;
import javax.swing.BorderFactory;

import net.sf.jabref.labelPattern.LabelPattern;
import net.sf.jabref.labelPattern.LabelPatternUtil;

/**
 * This is the panel for the key pattern definition tab in 'Preferences'. So far
 * it is only possible to edit default entry types.
 * 
 * Labels and buttons does not yet draw from a resource file.
 *   
 * @author Ulrik Stervbo (ulriks AT ruc.dk)
 */
public class TabLabelPattern extends JPanel implements PrefsTab{
	
    private String def = Globals.lang("Default");
    private GridBagLayout gbl = new GridBagLayout();
    private GridBagConstraints con = new GridBagConstraints();
    private HashMap textFields = new HashMap();

	private JabRefPreferences _prefs;
	private LabelPattern _keypatterns;
	
	private JLabel lblEntryType, lblKeyPattern;
	private JLabel lblArticle, lblBook, lblBooklet, lblConference;
	private JLabel lblInbook, lblIncollection, lblInproceedings;
	private JLabel lblManual, lblMastersthesis, lblMisc, lblPhdthesis;
	private JLabel lblProceedings, lblTechreport, lblUnpublished;

	private JTextField txtArticle, txtBook, txtBooklet, txtConference;
	private JTextField txtInbook, txtIncollection, txtInproceedings;
	private JTextField txtManual, txtMastersthesis, txtMisc, txtPhdthesis;
	private JTextField txtProceedings, txtTechreport, txtUnpublished;

	private JButton btnArticleDefault, btnBookDefault, btnBookletDefault;
	private JButton btnConferenceDefault, btnInbookDefault;
	private JButton btnIncollectionDefault, btnInproceedingsDefault;
	private JButton btnManualDefault, btnMastersthesisDefault, btnMiscDefault;
	private JButton btnPhdthesisDefault, btnProceedingsDefault;
	private JButton btnTechreportDefault, btnUnpublishedDefault;
	private JButton btnDefaultAll;

    private HelpAction help;
	
	/**
	 * The constructor
	 */
	public TabLabelPattern(JabRefPreferences prefs, HelpDialog helpDiag) {
		_prefs = prefs;
		_keypatterns = _prefs.getKeyPattern();
		help = new HelpAction(helpDiag, GUIGlobals.labelPatternHelp,
				      "Help on key patterns");
		buildGUI();
		//fillTextfields();
	}

	/**
	 * Store changes to table preferences. This method is called when
	 * the user clicks Ok.
	 *
	 */
	public void storeSettings() {
	    // first we erase everything!
	    LabelPattern defKeyPattern = _keypatterns.getParent();
	    _keypatterns = new LabelPattern(defKeyPattern);
	    
	    // then we rebuild... 
	    Iterator i=textFields.keySet().iterator();
	    String defa = (String)LabelPatternUtil.DEFAULT_LABELPATTERN.get(0);
	    while (i.hasNext()) {
		String s = (String)i.next(),
		    text = ((JTextField)textFields.get(s)).getText();
		if (!defa.equals(text))
		    _keypatterns.addLabelPattern(s, text);
	    }

	    _prefs.putKeyPattern(_keypatterns);

		/*
		_keypatterns.addLabelPattern("article", 			txtArticle.getText());
		_keypatterns.addLabelPattern("book", 					txtBook.getText());
		_keypatterns.addLabelPattern("booklet", 			txtBooklet.getText());
		_keypatterns.addLabelPattern("conference", 		txtConference.getText());
		_keypatterns.addLabelPattern("inbook", 				txtInbook.getText());
		_keypatterns.addLabelPattern("incollection", 	txtIncollection.getText());
		_keypatterns.addLabelPattern("inproceedings",	txtInproceedings.getText());
		_keypatterns.addLabelPattern("manual", 				txtManual.getText());
		_keypatterns.addLabelPattern("mastersthesis",	txtMastersthesis.getText());
		_keypatterns.addLabelPattern("misc",					txtMisc.getText());
		_keypatterns.addLabelPattern("phdthesis", 		txtPhdthesis.getText());
		_keypatterns.addLabelPattern("proceedings", 	txtProceedings.getText());
		_keypatterns.addLabelPattern("techreport", 		txtTechreport.getText());
		_keypatterns.addLabelPattern("unpublished", 	txtUnpublished.getText());
		*/		

	}
	
    private  JTextField addEntryType(Container c, String name, int y) { 

	JLabel lab = new JLabel(Util.nCase(name));
	name = name.toLowerCase();
	con.gridx = 0;
	con.gridy = y;
	con.fill = GridBagConstraints.BOTH;
	con.weightx = 0;
	con.weighty = 0;
	con.anchor = GridBagConstraints.WEST;
	con.insets = new Insets( 0,5,0,5 );
	gbl.setConstraints( lab, con );
	c.add( lab );
	
	JTextField tf = new JTextField();//_keypatterns.getValue(name).get(0).toString());
	tf.setColumns( 15 );
	con.gridx = 1;
	con.fill = GridBagConstraints.HORIZONTAL;
	con.weightx = 1;
	con.weighty = 0;
	con.anchor = GridBagConstraints.CENTER;
	con.insets = new Insets( 0,5,0,5 );
	gbl.setConstraints( tf, con );
	c.add( tf );	
	
	JButton but = new JButton( def );
	con.gridx = 2;
	con.fill = GridBagConstraints.BOTH;
	con.weightx = 0;
	con.weighty = 0;
	con.anchor = GridBagConstraints.CENTER;
	con.insets = new Insets( 0,5,0,5 );
	gbl.setConstraints( but, con );
	but.setActionCommand(name);
        but.addActionListener(new buttonHandler());
	c.add( but );		

	return tf;
    }

    private void setValue(JTextField tf, String fieldName) {
	tf.setText(_keypatterns.getValue(fieldName).get(0).toString());
    }

	/**
	 * Method to build GUI
	 *
	 */
	private void buildGUI(){

	    JPanel pan = new JPanel();
	    JScrollPane sp = new JScrollPane(pan);	
	    sp.setBorder(BorderFactory.createEmptyBorder());
	    pan.setLayout(gbl);
	    setLayout(gbl);	    
	    // The header - can be removed
	    lblEntryType = new JLabel(Globals.lang("Entry type"));
	    Font f = new Font("plain", Font.BOLD, 12);
	    lblEntryType.setFont(f);
	    con.gridx = 0;
	    con.gridy = 0;
	    con.gridwidth = 1;
	    con.gridheight = 1;
	    con.fill = GridBagConstraints.VERTICAL;
	    con.anchor = GridBagConstraints.WEST;
	    con.insets = new Insets( 5,5,10,0 );
	    gbl.setConstraints( lblEntryType, con );
	    pan. add( lblEntryType );
	    
	    lblKeyPattern = new JLabel(Globals.lang("Key pattern"));
	    lblKeyPattern.setFont(f);
	    con.gridx = 1;
	    con.gridy = 0;
	    //con.gridwidth = 2;
	    con.gridheight = 1;
	    con.fill = GridBagConstraints.BOTH;
	    con.anchor = GridBagConstraints.WEST;
	    con.insets = new Insets( 5,5,10,0 );
	    gbl.setConstraints( lblKeyPattern, con );
	    pan.add( lblKeyPattern );

	    int y = 1;

	    Iterator i=BibtexEntryType.ALL_TYPES.keySet().iterator();
	    while (i.hasNext()) {
		String s = (String)i.next();
		textFields.put(s, addEntryType(pan, s, y));
		y++;
	    }

	    con.fill = GridBagConstraints.BOTH;
	    con.gridx = 0;
	    con.gridy = 1;
	    con.gridwidth = 3;
	    con.weightx = 1;
	    con.weighty = 1;
	    gbl.setConstraints(sp, con );
	    add(sp);

	    // A help button
	    con.gridwidth = 1;
	    con.gridx = 1;
	    con.gridy = 2;
	    con.fill = GridBagConstraints.HORIZONTAL;
	    //con.fill = GridBagConstraints.BOTH;
	    con.weightx = 0;
	    con.weighty = 0;
	    con.anchor = GridBagConstraints.SOUTHEAST;
	    con.insets = new Insets( 0,5,0,5 );
	    JButton hlb = new JButton(new ImageIcon(GUIGlobals.helpSmallIconFile));
	    hlb.setToolTipText(Globals.lang("Help on key patterns"));
	    gbl.setConstraints( hlb, con );
	    add(hlb);
	    hlb.addActionListener(help);
	    
	    // And finally a button to reset everything
	    btnDefaultAll = new JButton(Globals.lang("Reset all"));
	    con.gridx = 2;
	    con.gridy = 2;
	    //con.fill = GridBagConstraints.BOTH;
	    con.weightx = 0;
	    con.weighty = 0;
	    con.anchor = GridBagConstraints.SOUTHEAST;
	    con.insets = new Insets( 20,5,0,5 );
	    gbl.setConstraints( btnDefaultAll, con );
	    btnDefaultAll.addActionListener(new buttonHandler());
	    add( btnDefaultAll );		

	}
	
	/**
	 * Method for filling the text fields with user defined key patterns or default.
	 * The method used (<code>getValue(key)</code>) to get the ArrayList 
	 * corrosponding to an entry type throws a <code>NullPointerException</code>
	 * and <code>?</code> if an entry cannot be found. It really shouln't be
	 * nessesary to catch those exceptions here... 
	 */
    /*	private void fillTextfields(){
		txtArticle.setText(_keypatterns.getValue("article").get(0).toString());
		txtBook.setText(_keypatterns.getValue("book").get(0).toString());
		txtBooklet.setText(_keypatterns.getValue("booklet").get(0).toString());
		txtConference.setText(_keypatterns.getValue("conference").get(0).toString());
		txtInbook.setText(_keypatterns.getValue("inbook").get(0).toString());
		txtIncollection.setText(_keypatterns.getValue("incollection").get(0).toString());
		txtInproceedings.setText(_keypatterns.getValue("inproceedings").get(0).toString());
		txtManual.setText(_keypatterns.getValue("manual").get(0).toString());
		txtMastersthesis.setText(_keypatterns.getValue("mastersthesis").get(0).toString());
		txtMisc.setText(_keypatterns.getValue("misc").get(0).toString());
		txtPhdthesis.setText(_keypatterns.getValue("phdthesis").get(0).toString());
		txtProceedings.setText(_keypatterns.getValue("proceedings").get(0).toString());
		txtTechreport.setText(_keypatterns.getValue("techreport").get(0).toString());
		txtUnpublished.setText(_keypatterns.getValue("unpublished").get(0).toString());
	}

    */
	/**
	 * An inner class to handle button actions
	 * @author Ulrik Stervbo (ulriks AT ruc.dk)
	 */
	class buttonHandler implements ActionListener{
		public void actionPerformed(ActionEvent evt){

		    if (evt.getSource() == btnDefaultAll) {
			// All to default
			Iterator i=textFields.keySet().iterator();
			while (i.hasNext()) {
			    String s = (String)i.next();
			    //_keypatterns.removeLabelPattern(s);
			    JTextField tf = (JTextField)textFields.get(s);
			    tf.setText(_keypatterns.getParent()
				       .getValue(s).get(0).toString());
			}

			return;
		    }

		    //_keypatterns.removeLabelPattern(evt.getActionCommand());
		    JTextField tf = (JTextField)textFields.get(evt.getActionCommand());
		    tf.setText(_keypatterns.getParent()
			       .getValue(evt.getActionCommand()).get(0).toString());
		}
	    
	}

    public boolean readyToClose() {
	return true;
    }

    public void setValues() {
	for (Iterator i=textFields.keySet().iterator(); i.hasNext();) {
	    String name = (String)i.next();
	    JTextField tf = (JTextField)textFields.get(name);
	    setValue(tf, name);
	}
    }
}
