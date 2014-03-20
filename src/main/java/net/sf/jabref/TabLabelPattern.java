/*  Copyright (C) 2003-2012 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref;

import java.awt.GridBagConstraints;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.help.HelpDialog;
import net.sf.jabref.labelPattern.LabelPattern;
import net.sf.jabref.labelPattern.LabelPatternPanel;
import net.sf.jabref.labelPattern.LabelPatternUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The Preferences panel for key generation.
 */
public class TabLabelPattern extends LabelPatternPanel implements PrefsTab {
	
	private JabRefPreferences _prefs;
	
    private JCheckBox dontOverwrite = new JCheckBox(Globals.lang("Do not overwrite existing keys")),
        warnBeforeOverwriting = new JCheckBox(Globals.lang("Warn before overwriting existing keys")),
        generateOnSave = new JCheckBox(Globals.lang("Generate keys before saving (for entries without a key)")),
        autoGenerateOnImport = new JCheckBox(Globals.lang("Generate keys for imported entries"));

    private JRadioButton
    letterStartA = new JRadioButton(Globals.lang("Ensure unique keys using letters (a, b, ...)")),
    letterStartB = new JRadioButton(Globals.lang("Ensure unique keys using letters (b, c, ...)")),
    alwaysAddLetter = new JRadioButton(Globals.lang("Always add letter (a, b, ...) to generated keys"));

    private JTextField KeyPatternRegex = new JTextField(20);
    private JTextField KeyPatternReplacement = new JTextField(20);

	public TabLabelPattern(JabRefPreferences prefs, HelpDialog helpDiag) {
	    super(helpDiag);
		_prefs = prefs;
		appendKeyGeneratorSettings();
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

         if (alwaysAddLetter.isSelected())
             Globals.prefs.putBoolean("keyGenAlwaysAddLetter", true);
         else if (letterStartA.isSelected()) {
             Globals.prefs.putBoolean("keyGenFirstLetterA", true);
             Globals.prefs.putBoolean("keyGenAlwaysAddLetter", false);
         }
         else {
            Globals.prefs.putBoolean("keyGenFirstLetterA", false);
            Globals.prefs.putBoolean("keyGenAlwaysAddLetter", false);
         }
        
         LabelPatternUtil.updateDefaultPattern();

        // fetch the old parent from the currently stored patterns
	    LabelPattern defKeyPattern = _prefs.getKeyPattern().getParent();
	    // fetch entries from GUI
	    LabelPattern keypatterns = getLabelPattern();
	    // restore old parent
	    keypatterns.setParent(defKeyPattern);
	    // store new patterns globally
	    _prefs.putKeyPattern(keypatterns);
	}
	
    private void appendKeyGeneratorSettings() {
        ButtonGroup bg = new ButtonGroup();
        bg.add(letterStartA);
        bg.add(letterStartB);
        bg.add(alwaysAddLetter);

        // Build a panel for checkbox settings:
        FormLayout layout = new FormLayout
	        ("1dlu, 8dlu, left:pref, 8dlu, left:pref", "");//, 8dlu, 20dlu, 8dlu, fill:pref", "");
        JPanel pan = new JPanel();
	    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.appendSeparator(Globals.lang("Key generator settings"));

        builder.nextLine();
        builder.append(pan);
        builder.append(autoGenerateOnImport);
        builder.append(letterStartA);
        builder.nextLine();
        builder.append(pan);
        builder.append(warnBeforeOverwriting);
        builder.append(letterStartB);
        builder.nextLine();
        builder.append(pan);
        builder.append(dontOverwrite);
        builder.append(alwaysAddLetter);
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
	}
	
    public boolean readyToClose() {
        return true;
    }

    public void setValues() {
        super.setValues(_prefs.getKeyPattern());
        defaultPat.setText(Globals.prefs.get("defaultLabelPattern"));
        dontOverwrite.setSelected(Globals.prefs.getBoolean("avoidOverwritingKey"));
        generateOnSave.setSelected(Globals.prefs.getBoolean("generateKeysBeforeSaving"));
        autoGenerateOnImport.setSelected(Globals.prefs.getBoolean("generateKeysAfterInspection"));
        warnBeforeOverwriting.setSelected(Globals.prefs.getBoolean("warnBeforeOverwritingKey"));

        boolean alwaysAddLetter = Globals.prefs.getBoolean("keyGenAlwaysAddLetter"),
                firstLetterA = Globals.prefs.getBoolean("keyGenFirstLetterA");
        if (alwaysAddLetter)
            this.alwaysAddLetter.setSelected(true);
        else if (firstLetterA)
            this.letterStartA.setSelected(true);
        else
            this.letterStartB.setSelected(true);

        // Warning before overwriting is only relevant if overwriting can happen:
        warnBeforeOverwriting.setEnabled(!dontOverwrite.isSelected());

        KeyPatternRegex.setText(Globals.prefs.get("KeyPatternRegex"));
        KeyPatternReplacement.setText(Globals.prefs.get("KeyPatternReplacement"));

	    //basenamePatternRegex.setText(Globals.prefs.get("basenamePatternRegex"));
	    //basenamePatternReplacement.setText(Globals.prefs.get("basenamePatternReplacement"));
    }

	public String getTabName() {
	    return Globals.lang("BibTeX key generator");
	}
}
