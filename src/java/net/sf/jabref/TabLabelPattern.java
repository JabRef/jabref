/*
 * Created on 09-Dec-2003
 */
package net.sf.jabref;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ImageIcon;

import net.sf.jabref.labelPattern.LabelPattern;

/**
 * This is the panel for the key pattern definition tab in 'Preferences'. So far
 * it is only possible to edit default entry types.
 * 
 * Labels and buttons does not yet draw from a resource file.
 *   
 * @author Ulrik Stervbo (ulriks AT ruc.dk)
 */
public class TabLabelPattern extends JPanel implements PrefsTab{
	
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
		fillTextfields();
	}

	/**
	 * Store changes to table preferences. This method is called when
	 * the user clicks Ok.
	 *
	 */
	public void storeSettings() {
		// I'm not too frilled about doing this, but its easy :-)
		// first we erase everything!
		LabelPattern defKeyPattern = _keypatterns.getParent();
		_keypatterns = new LabelPattern(defKeyPattern);
		
		// then we rebuild... tons of redundant data I know
		// I originally thought of just adding changes patterns, but the
		// checks one has to do... this is SO much faster
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
		
		_prefs.putKeyPattern(_keypatterns);		
	}
	
	/**
	 * Method to build GUI
	 *
	 */
	private void buildGUI(){
	    String def = Globals.lang("Default");
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints con = new GridBagConstraints();
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
		con.insets = new Insets( 0,5,10,0 );
		gbl.setConstraints( lblEntryType, con );
		add( lblEntryType );

		lblKeyPattern = new JLabel(Globals.lang("Key pattern"));
		lblKeyPattern.setFont(f);
		con.gridx = 1;
		con.gridy = 0;
		//con.gridwidth = 2;
		con.gridheight = 1;
		con.fill = GridBagConstraints.BOTH;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,10,0 );
		gbl.setConstraints( lblKeyPattern, con );
		add( lblKeyPattern );
		
		//Here's the textfields for the key pattern
		// They're of the form: label - text field - reset button
		// The first one is for article 
		lblArticle = new JLabel( "Article" );
		con.gridx = 0;
		con.gridy = 1;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( lblArticle, con );
		add( lblArticle );
		
		txtArticle = new JTextField();
		txtArticle.setColumns( 15 );
		con.gridx = 1;
		con.gridy = 1;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( txtArticle, con );
		add( txtArticle );
		

		btnArticleDefault = new JButton( def );
		con.gridx = 2;
		con.gridy = 1;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( btnArticleDefault, con );
		btnArticleDefault.addActionListener(new buttonHandler());
		add( btnArticleDefault );		

		// Then its the  Book 
		lblBook = new JLabel( "Book" );
		con.gridx = 0;
		con.gridy = 2;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( lblBook, con );
		add( lblBook );
		
		txtBook = new JTextField();
		txtBook.setColumns( 15 );
		con.gridx = 1;
		con.gridy = 2;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( txtBook, con );
		add( txtBook );
		

		btnBookDefault = new JButton( def );
		con.gridx = 2;
		con.gridy = 2;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( btnBookDefault, con );
		btnBookDefault.addActionListener(new buttonHandler());
		add( btnBookDefault );

		// Then its the  Booklet 
		lblBooklet = new JLabel( "Booklet" );
		con.gridx = 0;
		con.gridy = 3;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( lblBooklet, con );
		add( lblBooklet );
		
		txtBooklet = new JTextField();
		txtBooklet.setColumns( 15 );
		con.gridx = 1;
		con.gridy = 3;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( txtBooklet, con );
		add( txtBooklet );
		

		btnBookletDefault = new JButton( def );
		con.gridx = 2;
		con.gridy = 3;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( btnBookletDefault, con );
		btnBookletDefault.addActionListener(new buttonHandler());
		add( btnBookletDefault );

		// Then its the  Conference 
		lblConference = new JLabel( "Conference" );
		con.gridx = 0;
		con.gridy = 4;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( lblConference, con );
		add( lblConference );
		
		txtConference = new JTextField();
		txtConference.setColumns( 15 );
		con.gridx = 1;
		con.gridy = 4;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( txtConference, con );
		add( txtConference );
		
		btnConferenceDefault = new JButton( def );
		con.gridx = 2;
		con.gridy = 4;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( btnConferenceDefault, con );
		btnConferenceDefault.addActionListener(new buttonHandler());
		add( btnConferenceDefault );		

		// Then its the  Inbook 
		lblInbook = new JLabel( "Inbook" );
		con.gridx = 0;
		con.gridy = 5;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( lblInbook, con );
		add( lblInbook );
		
		txtInbook = new JTextField();
		txtInbook.setColumns( 15 );
		con.gridx = 1;
		con.gridy = 5;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( txtInbook, con );
		add( txtInbook );
		
		btnInbookDefault = new JButton( def );
		con.gridx = 2;
		con.gridy = 5;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( btnInbookDefault, con );
		btnInbookDefault.addActionListener(new buttonHandler());
		add( btnInbookDefault );

		// Then its the  Incollection 
		lblIncollection = new JLabel( "Incollection" );
		con.gridx = 0;
		con.gridy = 6;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( lblIncollection, con );
		add( lblIncollection );
		
		txtIncollection = new JTextField();
		txtIncollection.setColumns( 15 );
		con.gridx = 1;
		con.gridy = 6;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( txtIncollection, con );
		add( txtIncollection );

		btnIncollectionDefault = new JButton( def );
		con.gridx = 2;
		con.gridy = 6;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( btnIncollectionDefault, con );
		btnIncollectionDefault.addActionListener(new buttonHandler());
		add( btnIncollectionDefault );

		// Then its the  Inproceedings 
		lblInproceedings = new JLabel( "Inproceedings" );
		con.gridx = 0;
		con.gridy = 7;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( lblInproceedings, con );
		add( lblInproceedings );
		
		txtInproceedings = new JTextField();
		txtInproceedings.setColumns( 15 );
		con.gridx = 1;
		con.gridy = 7;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( txtInproceedings, con );
		add( txtInproceedings );
		
		btnInproceedingsDefault = new JButton( def );
		con.gridx = 2;
		con.gridy = 7;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( btnInproceedingsDefault, con );
		btnInproceedingsDefault.addActionListener(new buttonHandler());
		add( btnInproceedingsDefault );

		// Then its the  Manual 
		lblManual = new JLabel( "Manual" );
		con.gridx = 0;
		con.gridy = 8;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( lblManual, con );
		add( lblManual );
		
		txtManual = new JTextField();
		txtManual.setColumns( 15 );
		con.gridx = 1;
		con.gridy = 8;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( txtManual, con );
		add( txtManual );
		
		btnManualDefault = new JButton( def );
		con.gridx = 2;
		con.gridy = 8;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( btnManualDefault, con );
		btnManualDefault.addActionListener(new buttonHandler());
		add( btnManualDefault );

		// Then its the  Mastersthesis 
		lblMastersthesis = new JLabel( "Mastersthesis" );
		con.gridx = 0;
		con.gridy = 9;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( lblMastersthesis, con );
		add( lblMastersthesis );
		
		txtMastersthesis = new JTextField();
		txtMastersthesis.setColumns( 15 );
		con.gridx = 1;
		con.gridy = 9;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( txtMastersthesis, con );
		add( txtMastersthesis );
		
		btnMastersthesisDefault = new JButton( def );
		con.gridx = 2;
		con.gridy = 9;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( btnMastersthesisDefault, con );
		btnMastersthesisDefault.addActionListener(new buttonHandler());
		add( btnMastersthesisDefault );
		
		// Then its the  Misc 
		lblMisc = new JLabel( "Misc" );
		con.gridx = 0;
		con.gridy = 10;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( lblMisc, con );
		add( lblMisc );
		
		txtMisc = new JTextField();
		txtMisc.setColumns( 15 );
		con.gridx = 1;
		con.gridy = 10;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( txtMisc, con );
		add( txtMisc );
		
		btnMiscDefault = new JButton( def );
		con.gridx = 2;
		con.gridy = 10;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( btnMiscDefault, con );
		btnMiscDefault.addActionListener(new buttonHandler());
		add( btnMiscDefault );
		
		// Then its the  Phdthesis 
		lblPhdthesis = new JLabel( "Phdthesis" );
		con.gridx = 0;
		con.gridy = 11;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( lblPhdthesis, con );
		add( lblPhdthesis );
		
		txtPhdthesis = new JTextField();
		txtPhdthesis.setColumns( 15 );
		con.gridx = 1;
		con.gridy = 11;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( txtPhdthesis, con );
		add( txtPhdthesis );
		
		btnPhdthesisDefault = new JButton( def );
		con.gridx = 2;
		con.gridy = 11;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( btnPhdthesisDefault, con );
		btnPhdthesisDefault.addActionListener(new buttonHandler());
		add( btnPhdthesisDefault );		

		// Then its the  Proceedings 
		lblProceedings = new JLabel( "Proceedings" );
		con.gridx = 0;
		con.gridy = 12;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( lblProceedings, con );
		add( lblProceedings );
		
		txtProceedings = new JTextField();
		txtProceedings.setColumns( 15 );
		con.gridx = 1;
		con.gridy = 12;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( txtProceedings, con );
		add( txtProceedings );
		
		btnProceedingsDefault = new JButton( def );
		con.gridx = 2;
		con.gridy = 12;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( btnProceedingsDefault, con );
		btnProceedingsDefault.addActionListener(new buttonHandler());
		add( btnProceedingsDefault );		

		// Then its the  Techreport 
		lblTechreport = new JLabel( "Techreport" );
		con.gridx = 0;
		con.gridy = 13;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( lblTechreport, con );
		add( lblTechreport );
		
		txtTechreport = new JTextField();
		txtTechreport.setColumns( 15 );
		con.gridx = 1;
		con.gridy = 13;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( txtTechreport, con );
		add( txtTechreport );
		
		btnTechreportDefault = new JButton( def );
		con.gridx = 2;
		con.gridy = 13;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( btnTechreportDefault, con );
		btnTechreportDefault.addActionListener(new buttonHandler());
		add( btnTechreportDefault );		

		// Then its the  Unpublished 
		lblUnpublished = new JLabel( "Unpublished" );
		con.gridx = 0;
		con.gridy = 14;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( lblUnpublished, con );
		add( lblUnpublished );
		
		txtUnpublished = new JTextField();
		txtUnpublished.setColumns( 15 );
		con.gridx = 1;
		con.gridy = 14;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( txtUnpublished, con );
		add( txtUnpublished );
		
		btnUnpublishedDefault = new JButton( def );
		con.gridx = 2;
		con.gridy = 14;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
		con.insets = new Insets( 0,5,0,5 );
		gbl.setConstraints( btnUnpublishedDefault, con );
		btnUnpublishedDefault.addActionListener(new buttonHandler());
		add( btnUnpublishedDefault );		
		
		// A help button
		con.gridx = 1;
		con.gridy = 15;
		con.fill = GridBagConstraints.NONE;
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
		con.gridy = 15;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.anchor = GridBagConstraints.CENTER;
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
	private void fillTextfields(){
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

	
	/**
	 * An inner class to handle button actions
	 * @author Ulrik Stervbo (ulriks AT ruc.dk)
	 */
	class buttonHandler implements ActionListener{
		public void actionPerformed(ActionEvent evt){
			// we know its a button, so casting is quite OK
			JButton button = (JButton)evt.getSource();
			
			// is the calling button the 'btnArticleDefault'?
			if(button.equals(btnArticleDefault)){
				// because of the structure or KeyPatterns with a default as a very parent
				// resetting a pattern really just means to remove the pattern from the child
				_keypatterns.removeLabelPattern("article");
			}
//		is the calling button the 'btnBookDefault'?
			else if(button.equals(btnBookDefault)){
				_keypatterns.removeLabelPattern("book");
			}
			else if(button.equals(btnBookletDefault)){
				_keypatterns.removeLabelPattern("booklet");
			}
			else if(button.equals(btnConferenceDefault)){
				_keypatterns.removeLabelPattern("conference");
			}
			else if(button.equals(btnInbookDefault)){
				_keypatterns.removeLabelPattern("inbook");
			}
			else if(button.equals(btnIncollectionDefault)){
				_keypatterns.removeLabelPattern("incollection");
			}
			else if(button.equals(btnInproceedingsDefault)){
				_keypatterns.removeLabelPattern("inproceedings");
			}
			else if(button.equals(btnManualDefault)){
				_keypatterns.removeLabelPattern("manual");
			}
			else if(button.equals(btnMastersthesisDefault)){
				_keypatterns.removeLabelPattern("mastersthesis");
			}
			else if(button.equals(btnMiscDefault)){
				_keypatterns.removeLabelPattern("misc");
			}
			else if(button.equals(btnPhdthesisDefault)){
				_keypatterns.removeLabelPattern("phdthesis");
			}
			else if(button.equals(btnProceedingsDefault)){
				_keypatterns.removeLabelPattern("proceedings");
			}
			else if(button.equals(btnTechreportDefault)){
				_keypatterns.removeLabelPattern("techreport");
			}
			else if(button.equals(btnUnpublishedDefault)){
				_keypatterns.removeLabelPattern("unpublished");
			}
			else if(button.equals(btnDefaultAll)){
				LabelPattern defKeyPattern = _keypatterns.getParent();
				_keypatterns = new LabelPattern(defKeyPattern);
			}
			// This one should never be fired! If it is, then someone added a button
			// but did not add any action for it....
			else{
				System.err.println("Danger Will Robinson! " +
										"If you see this something is very wrong!\n" +
										"Ask someone clever to look at the method \'actionPerformed\' " +
										" in \'TabLabelPattern\' - I think a line is missing");
			}
			fillTextfields();
			
		}
		
	}

}
