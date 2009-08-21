/*
 * Created on 09-Dec-2003
 */
package net.sf.jabref;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.labelPattern.LabelPattern;
import net.sf.jabref.labelPattern.LabelPatternUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The Preferences panel for key generation.
 */
public class TabLabelPattern extends JPanel implements PrefsTab{
	
    private String def = Globals.lang("Default");
    private GridBagLayout gbl = new GridBagLayout();
    private GridBagConstraints con = new GridBagConstraints();
    private HashMap<String, JTextField> textFields = new HashMap<String, JTextField>();

	private JabRefPreferences _prefs;
	private LabelPattern _keypatterns = null;
	
    private JCheckBox dontOverwrite = new JCheckBox(Globals.lang("Do not overwrite existing keys")),
        warnBeforeOverwriting = new JCheckBox(Globals.lang("Warn before overwriting existing keys")),
        generateOnSave = new JCheckBox(Globals.lang("Generate keys before saving (for entries without a key)")),
        autoGenerateOnImport = new JCheckBox(Globals.lang("Generate keys for imported entries"));


    private JLabel lblEntryType, lblKeyPattern;

    private JTextField defaultPat = new JTextField();

    //private JTextField basenamePatternRegex = new JTextField(20);
    //private JTextField basenamePatternReplacement = new JTextField(20);
    private JTextField KeyPatternRegex = new JTextField(20);
    private JTextField KeyPatternReplacement = new JTextField(20);

	private JButton btnDefaultAll, btnDefault;


    private HelpAction help;
	
	/**
	 * The constructor
	 */
	public TabLabelPattern(JabRefPreferences prefs, HelpDialog helpDiag) {
		_prefs = prefs;
		//_keypatterns = _prefs.getKeyPattern();
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

         // Set the default value:
         Globals.prefs.put("defaultLabelPattern", defaultPat.getText());

         Globals.prefs.putBoolean("warnBeforeOverwritingKey", warnBeforeOverwriting.isSelected());
         Globals.prefs.putBoolean("avoidOverwritingKey", dontOverwrite.isSelected());

         //Globals.prefs.put("basenamePatternRegex", basenamePatternRegex.getText());
         //Globals.prefs.put("basenamePatternReplacement", basenamePatternReplacement.getText());
         Globals.prefs.put("KeyPatternRegex", KeyPatternRegex.getText());
         Globals.prefs.put("KeyPatternReplacement", KeyPatternReplacement.getText());
         Globals.prefs.putBoolean("generateKeysAfterInspection", autoGenerateOnImport.isSelected());
         Globals.prefs.putBoolean("generateKeysBeforeSaving", generateOnSave.isSelected());
         LabelPatternUtil.updateDefaultPattern();


	    LabelPattern defKeyPattern = _keypatterns.getParent();
	    _keypatterns = new LabelPattern(defKeyPattern);
	    
	    // then we rebuild... 
	    Iterator<String> i=textFields.keySet().iterator();
	    //String defa = (String)LabelPatternUtil.DEFAULT_LABELPATTERN.get(0);
	    while (i.hasNext()) {
		String s = i.next(),
		    text = textFields.get(s).getText();
		if (!"".equals(text.trim())) //(!defa.equals(text))
		    _keypatterns.addLabelPattern(s, text);
	    }

	    _prefs.putKeyPattern(_keypatterns);

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
        if (_keypatterns.isDefaultValue(fieldName))
            tf.setText("");
        else {
            //System.out.println(":: "+_keypatterns.getValue(fieldName).get(0).toString());
            tf.setText(_keypatterns.getValue(fieldName).get(0).toString());
        }
    }

	/**
	 * Method to build GUI
	 *
	 */
	private void buildGUI(){

	    JPanel pan = new JPanel();
	    JScrollPane sp = new JScrollPane(pan);
        sp.setPreferredSize(new Dimension(100,100));
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
	    con.fill = GridBagConstraints.HORIZONTAL;
	    con.anchor = GridBagConstraints.WEST;
	    con.insets = new Insets( 5,5,10,5 );
	    gbl.setConstraints( lblKeyPattern, con );
	    pan.add( lblKeyPattern );


            con.gridy = 1;
            con.gridx = 0;
            JLabel lab = new JLabel(Globals.lang("Default pattern"));
            gbl.setConstraints(lab, con);
            pan.add(lab);
            con.gridx = 1;
            gbl.setConstraints(defaultPat, con);
            pan.add(defaultPat);
        con.insets = new Insets( 5,5,10,5 );
        btnDefault = new JButton(Globals.lang("Default"));
        btnDefault.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                defaultPat.setText((String)Globals.prefs.defaults.get("defaultLabelPattern"));
            }
        });
        con.gridx = 2;
	    int y = 2;
        gbl.setConstraints(btnDefault, con);
        pan.add(btnDefault);

        for (String s : BibtexEntryType.ALL_TYPES.keySet()) {
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
	    //
	    con.weightx = 0;
	    con.weighty = 0;
	    con.anchor = GridBagConstraints.SOUTHEAST;
	    con.insets = new Insets( 0,5,0,5 );
	    JButton hlb = new JButton(GUIGlobals.getImage("helpSmall"));
	    hlb.setToolTipText(Globals.lang("Help on key patterns"));
	    gbl.setConstraints( hlb, con );
	    add(hlb);
	    hlb.addActionListener(help);
	    
	    // And finally a button to reset everything
	    btnDefaultAll = new JButton(Globals.lang("Reset all"));
	    con.gridx = 2;
	    con.gridy = 2;

	    //con.fill = GridBagConstraints.BOTH;
	    con.weightx = 1;
	    con.weighty = 0;
	    con.anchor = GridBagConstraints.SOUTHEAST;
	    con.insets = new Insets( 20,5,0,5 );
	    gbl.setConstraints( btnDefaultAll, con );
	    btnDefaultAll.addActionListener(new buttonHandler());
	    add( btnDefaultAll );


        // Build a panel for checkbox settings:
        FormLayout layout = new FormLayout
	        ("1dlu, 8dlu, left:pref, 8dlu, left:pref", "");//, 8dlu, 20dlu, 8dlu, fill:pref", "");
        pan = new JPanel();
	    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.appendSeparator(Globals.lang("Key generator settings"));

        builder.nextLine();
        builder.append(pan);
        builder.append(autoGenerateOnImport);
        builder.nextLine();
        builder.append(pan);
        builder.append(warnBeforeOverwriting);
        builder.append(dontOverwrite);
        builder.nextLine();
        builder.append(pan);
        builder.append(generateOnSave);        
        builder.nextLine();
        builder.append(pan);
        builder.append(Globals.lang("Replace (regular expression)")+":");
        builder.append(Globals.lang("by")+":");

        builder.nextLine();
        builder.append(pan);
        builder.append(KeyPatternRegex);
        builder.append(KeyPatternReplacement);

        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        con.gridx = 1;
	    con.gridy = 3;
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weightx = 1;
        con.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(builder.getPanel(), con);
        add(builder.getPanel());

        dontOverwrite.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                // Warning before overwriting is only relevant if overwriting can happen:
                warnBeforeOverwriting.setEnabled(!dontOverwrite.isSelected());
            }
        });

      /*
       Simon Fischer's patch for replacing a regexp in keys before converting to filename:

	layout = new FormLayout
	        ("left:pref, 8dlu, left:pref, left:pref", "");
	builder = new DefaultFormBuilder(layout);
        builder.appendSeparator(Globals.lang("Bibkey to filename conversion"));
        builder.nextLine();
	builder.append(Globals.lang("Replace"), basenamePatternRegex);
        builder.nextLine();
	builder.append(Globals.lang("by"), basenamePatternReplacement);
        builder.nextLine();

        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        con.gridx = 2;
 	con.gridy = 3;
	con.gridwidth = GridBagConstraints.REMAINDER;
	con.weightx = 1;
	con.fill = GridBagConstraints.BOTH;
	gbl.setConstraints(builder.getPanel(), con);
        add(builder.getPanel());
        */
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
			Iterator<String> i=textFields.keySet().iterator();
			while (i.hasNext()) {
			    String s = i.next();
			    //_keypatterns.removeLabelPattern(s);
			    JTextField tf = textFields.get(s);
                            tf.setText("");
    			    /*tf.setText(_keypatterns.getParent()
				       .getValue(s).get(0).toString());*/
			}

			return;
		    }

		    //_keypatterns.removeLabelPattern(evt.getActionCommand());
		    JTextField tf = textFields.get(evt.getActionCommand());
                    tf.setText("");
		    /*tf.setText(_keypatterns.getParent()
			       .getValue(evt.getActionCommand()).get(0).toString());*/
		}
	    
	}

    public boolean readyToClose() {
	return true;
    }

    public void setValues() {
        _keypatterns = _prefs.getKeyPattern();
        defaultPat.setText(Globals.prefs.get("defaultLabelPattern"));
        dontOverwrite.setSelected(Globals.prefs.getBoolean("avoidOverwritingKey"));
        generateOnSave.setSelected(Globals.prefs.getBoolean("generateKeysBeforeSaving"));
        autoGenerateOnImport.setSelected(Globals.prefs.getBoolean("generateKeysAfterInspection"));
        warnBeforeOverwriting.setSelected(Globals.prefs.getBoolean("warnBeforeOverwritingKey"));
        // Warning before overwriting is only relevant if overwriting can happen:
        warnBeforeOverwriting.setEnabled(!dontOverwrite.isSelected());
	    for (Iterator<String> i=textFields.keySet().iterator(); i.hasNext();) {
            String name = i.next();
            JTextField tf = textFields.get(name);
    	    setValue(tf, name);
    	}

        KeyPatternRegex.setText(Globals.prefs.get("KeyPatternRegex"));
        KeyPatternReplacement.setText(Globals.prefs.get("KeyPatternReplacement"));

	    //basenamePatternRegex.setText(Globals.prefs.get("basenamePatternRegex"));
	    //basenamePatternReplacement.setText(Globals.prefs.get("basenamePatternReplacement"));
    }

	public String getTabName() {
	    return Globals.lang("BibTeX key generator");
	}
}
