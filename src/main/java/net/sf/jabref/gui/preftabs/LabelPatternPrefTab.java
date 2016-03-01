/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.gui.preftabs;

import java.awt.GridBagConstraints;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.labelpattern.LabelPatternPanel;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelpattern.GlobalLabelPattern;
import net.sf.jabref.logic.labelpattern.LabelPatternUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The Preferences panel for key generation.
 */
class LabelPatternPrefTab extends LabelPatternPanel implements PrefsTab {

    private final JabRefPreferences prefs;

    private final JCheckBox dontOverwrite = new JCheckBox(Localization.lang("Do not overwrite existing keys"));
    private final JCheckBox warnBeforeOverwriting = new JCheckBox(Localization.lang("Warn before overwriting existing keys"));
    private final JCheckBox generateOnSave = new JCheckBox(Localization.lang("Generate keys before saving (for entries without a key)"));
    private final JCheckBox autoGenerateOnImport = new JCheckBox(Localization.lang("Generate keys for imported entries"));

    private final JRadioButton letterStartA = new JRadioButton(Localization.lang("Ensure unique keys using letters (a, b, ...)"));
    private final JRadioButton letterStartB = new JRadioButton(Localization.lang("Ensure unique keys using letters (b, c, ...)"));
    private final JRadioButton alwaysAddLetter = new JRadioButton(Localization.lang("Always add letter (a, b, ...) to generated keys"));

    private final JTextField KeyPatternRegex = new JTextField(20);
    private final JTextField KeyPatternReplacement = new JTextField(20);


    public LabelPatternPrefTab(JabRefPreferences prefs, BasePanel panel) {
        super(panel);
        this.prefs = prefs;
        appendKeyGeneratorSettings();
    }

    /**
     * Store changes to table preferences. This method is called when the user clicks Ok.
     *
     */
    @Override
    public void storeSettings() {

        // Set the default value:
        Globals.prefs.put(JabRefPreferences.DEFAULT_LABEL_PATTERN, defaultPat.getText());

        Globals.prefs.putBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY, warnBeforeOverwriting.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY, dontOverwrite.isSelected());

        Globals.prefs.put(JabRefPreferences.KEY_PATTERN_REGEX, KeyPatternRegex.getText());
        Globals.prefs.put(JabRefPreferences.KEY_PATTERN_REPLACEMENT, KeyPatternReplacement.getText());
        Globals.prefs.putBoolean(JabRefPreferences.GENERATE_KEYS_AFTER_INSPECTION, autoGenerateOnImport.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.GENERATE_KEYS_BEFORE_SAVING, generateOnSave.isSelected());

        if (alwaysAddLetter.isSelected()) {
            Globals.prefs.putBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER, true);
        } else if (letterStartA.isSelected()) {
            Globals.prefs.putBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A, true);
            Globals.prefs.putBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER, false);
        }
        else {
            Globals.prefs.putBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A, false);
            Globals.prefs.putBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER, false);
        }

        LabelPatternUtil.updateDefaultPattern();

        // fetch entries from GUI
        GlobalLabelPattern keypatterns = getLabelPatternAsGlobalLabelPattern();
        // store new patterns globally
        prefs.putKeyPattern(keypatterns);
    }

    private void appendKeyGeneratorSettings() {
        ButtonGroup bg = new ButtonGroup();
        bg.add(letterStartA);
        bg.add(letterStartB);
        bg.add(alwaysAddLetter);

        // Build a panel for checkbox settings:
        FormLayout layout = new FormLayout
                ("1dlu, 8dlu, left:pref, 8dlu, left:pref", "");
        JPanel pan = new JPanel();
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.appendSeparator(Localization.lang("Key generator settings"));

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
        builder.append(Localization.lang("Replace (regular expression)") + ':');
        builder.append(Localization.lang("by") + ':');

        builder.nextLine();
        builder.append(pan);
        builder.append(KeyPatternRegex);
        builder.append(KeyPatternReplacement);

        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        con.gridx = 1;
        con.gridy = 3;
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weightx = 1;
        con.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(builder.getPanel(), con);
        add(builder.getPanel());

        dontOverwrite.addChangeListener(e ->
        // Warning before overwriting is only relevant if overwriting can happen:
        warnBeforeOverwriting.setEnabled(!dontOverwrite.isSelected()));
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public void setValues() {
        super.setValues(Globals.prefs.getKeyPattern());
        defaultPat.setText(Globals.prefs.get(JabRefPreferences.DEFAULT_LABEL_PATTERN));
        dontOverwrite.setSelected(Globals.prefs.getBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY));
        generateOnSave.setSelected(Globals.prefs.getBoolean(JabRefPreferences.GENERATE_KEYS_BEFORE_SAVING));
        autoGenerateOnImport.setSelected(Globals.prefs.getBoolean(JabRefPreferences.GENERATE_KEYS_AFTER_INSPECTION));
        warnBeforeOverwriting.setSelected(Globals.prefs.getBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY));

        boolean prefAlwaysAddLetter = Globals.prefs.getBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER);
        boolean firstLetterA = Globals.prefs.getBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A);
        if (prefAlwaysAddLetter) {
            this.alwaysAddLetter.setSelected(true);
        } else if (firstLetterA) {
            this.letterStartA.setSelected(true);
        } else {
            this.letterStartB.setSelected(true);
        }

        // Warning before overwriting is only relevant if overwriting can happen:
        warnBeforeOverwriting.setEnabled(!dontOverwrite.isSelected());

        KeyPatternRegex.setText(Globals.prefs.get(JabRefPreferences.KEY_PATTERN_REGEX));
        KeyPatternReplacement.setText(Globals.prefs.get(JabRefPreferences.KEY_PATTERN_REPLACEMENT));

        //basenamePatternRegex.setText(Globals.prefs.get("basenamePatternRegex"));
        //basenamePatternReplacement.setText(Globals.prefs.get("basenamePatternReplacement"));
    }

    @Override
    public String getTabName() {
        return Localization.lang("BibTeX key generator");
    }
}
