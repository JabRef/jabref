package net.sf.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import net.sf.jabref.gui.keyboard.EmacsKeyBindings;
import net.sf.jabref.logic.autocompleter.AutoCompleteFirstNameMode;
import net.sf.jabref.logic.autocompleter.AutoCompletePreferences;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

class EntryEditorPrefsTab extends JPanel implements PrefsTab {

    private final JCheckBox autoOpenForm;
    private final JCheckBox defSource;
    private final JCheckBox emacsMode;
    private final JCheckBox emacsRebindCtrlA;
    private final JCheckBox emacsRebindCtrlF;
    private final JCheckBox autoComplete;
    private final JRadioButton autoCompBoth;
    private final JRadioButton autoCompFF;
    private final JRadioButton autoCompLF;
    private final JRadioButton firstNameModeFull;
    private final JRadioButton firstNameModeAbbr;
    private final JRadioButton firstNameModeBoth;
    private final JSpinner shortestToComplete;

    private final JTextField autoCompFields;
    private final JabRefPreferences prefs;
    private final AutoCompletePreferences autoCompletePreferences;


    public EntryEditorPrefsTab(JabRefPreferences prefs) {
        this.prefs = prefs;
        autoCompletePreferences = new AutoCompletePreferences(prefs);
        setLayout(new BorderLayout());

        autoOpenForm = new JCheckBox(Localization.lang("Open editor when a new entry is created"));
        defSource = new JCheckBox(Localization.lang("Show BibTeX source by default"));
        emacsMode = new JCheckBox(Localization.lang("Use Emacs key bindings"));
        emacsRebindCtrlA = new JCheckBox(Localization.lang("Rebind C-a, too"));
        emacsRebindCtrlF = new JCheckBox(Localization.lang("Rebind C-f, too"));
        autoComplete = new JCheckBox(Localization.lang("Enable word/name autocompletion"));

        shortestToComplete = new JSpinner(
                new SpinnerNumberModel(autoCompletePreferences.getShortestLengthToComplete(), 1, 5, 1));

        // allowed name formats
        autoCompFF = new JRadioButton(Localization.lang("Autocomplete names in 'Firstname Lastname' format only"));
        autoCompLF = new JRadioButton(Localization.lang("Autocomplete names in 'Lastname, Firstname' format only"));
        autoCompBoth = new JRadioButton(Localization.lang("Autocomplete names in both formats"));
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(autoCompLF);
        buttonGroup.add(autoCompFF);
        buttonGroup.add(autoCompBoth);

        // treatment of first name
        firstNameModeFull = new JRadioButton(Localization.lang("Use full firstname whenever possible"));
        firstNameModeAbbr = new JRadioButton(Localization.lang("Use abbreviated firstname whenever possible"));
        firstNameModeBoth = new JRadioButton(Localization.lang("Use abbreviated and full firstname"));
        ButtonGroup firstNameModeButtonGroup = new ButtonGroup();
        firstNameModeButtonGroup.add(firstNameModeFull);
        firstNameModeButtonGroup.add(firstNameModeAbbr);
        firstNameModeButtonGroup.add(firstNameModeBoth);

        Insets marg = new Insets(0, 20, 3, 0);

        emacsRebindCtrlA.setMargin(marg);
        // We need a listener on showSource to enable and disable the source panel-related choices:
        emacsMode.addChangeListener(event -> emacsRebindCtrlA.setEnabled(emacsMode.isSelected()));

        emacsRebindCtrlF.setMargin(marg);
        // We need a listener on showSource to enable and disable the source panel-related choices:
        emacsMode.addChangeListener(event -> emacsRebindCtrlF.setEnabled(emacsMode.isSelected()));


        autoCompFields = new JTextField(40);
        // We need a listener on autoComplete to enable and disable the
        // autoCompFields text field:
        autoComplete.addChangeListener(event -> setAutoCompleteElementsEnabled(autoComplete.isSelected()));

        FormLayout layout = new FormLayout
                (// columns
                 "8dlu, left:pref, 8dlu, fill:150dlu, 4dlu, fill:pref", // 4dlu, left:pref, 4dlu",
                 // rows  1 to 10
                 "pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, 6dlu, " +
                // rows 11 to 16
                        "pref, 6dlu, pref, 6dlu, pref, 6dlu, " +
                        // rows 17 to 27
                 "pref, 6dlu, pref, pref, pref, pref, 6dlu, pref, pref, pref, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addSeparator(Localization.lang("Editor options"), cc.xyw(1, 1, 5));
        builder.add(autoOpenForm, cc.xy(2, 3));
        builder.add(defSource, cc.xy(2, 5));
        builder.add(emacsMode, cc.xy(2, 7));
        builder.add(emacsRebindCtrlA, cc.xy(2, 9));
        builder.add(emacsRebindCtrlF, cc.xy(2, 11));

        builder.addSeparator(Localization.lang("Autocompletion options"), cc.xyw(1, 13, 5));
        builder.add(autoComplete, cc.xy(2, 15));

        DefaultFormBuilder builder3 = new DefaultFormBuilder(new FormLayout("left:pref, 4dlu, fill:150dlu",""));
        JLabel label = new JLabel(Localization.lang("Use autocompletion for the following fields")+":");

        builder3.append(label);
        builder3.append(autoCompFields);
        JLabel label2 = new JLabel(Localization.lang("Autocomplete after following number of characters") + ":");
        builder3.append(label2);
        builder3.append(shortestToComplete);
        builder.add(builder3.getPanel(), cc.xyw(2, 17, 3));

        builder.addSeparator(Localization.lang("Name format used for autocompletion"), cc.xyw(2, 19, 4));
        builder.add(autoCompFF, cc.xy(2, 20));
        builder.add(autoCompLF, cc.xy(2, 21));
        builder.add(autoCompBoth, cc.xy(2, 22));

        builder.addSeparator(Localization.lang("Treatment of first names"), cc.xyw(2, 24, 4));
        builder.add(firstNameModeAbbr, cc.xy(2, 25));
        builder.add(firstNameModeFull, cc.xy(2, 26));
        builder.add(firstNameModeBoth, cc.xy(2, 27));

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
    }

    private void setAutoCompleteElementsEnabled(boolean enabled) {
        autoCompFields.setEnabled(enabled);
        autoCompLF.setEnabled(enabled);
        autoCompFF.setEnabled(enabled);
        autoCompBoth.setEnabled(enabled);
        firstNameModeAbbr.setEnabled(enabled);
        firstNameModeFull.setEnabled(enabled);
        firstNameModeBoth.setEnabled(enabled);
        shortestToComplete.setEnabled(enabled);
    }

    @Override
    public void setValues() {
        autoOpenForm.setSelected(prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM));
        defSource.setSelected(prefs.getBoolean(JabRefPreferences.DEFAULT_SHOW_SOURCE));
        emacsMode.setSelected(prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS));
        emacsRebindCtrlA.setSelected(prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CA));
        emacsRebindCtrlF.setSelected(prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CF));
        autoComplete.setSelected(prefs.getBoolean(JabRefPreferences.AUTO_COMPLETE));
        autoCompFields.setText(autoCompletePreferences.getCompleteNamesAsString());
        shortestToComplete.setValue(autoCompletePreferences.getShortestLengthToComplete());

        if (autoCompletePreferences.getOnlyCompleteFirstLast()) {
            autoCompFF.setSelected(true);
        } else if (autoCompletePreferences.getOnlyCompleteLastFirst()) {
            autoCompLF.setSelected(true);
        } else {
            autoCompBoth.setSelected(true);
        }

        switch (autoCompletePreferences.getFirstnameMode()) {
        case ONLY_ABBREVIATED:
            firstNameModeAbbr.setSelected(true);
            break;
        case ONLY_FULL:
            firstNameModeFull.setSelected(true);
            break;
        default:
            firstNameModeBoth.setSelected(true);
            break;
        }

        // similar for emacs CTRL-a and emacs mode
        emacsRebindCtrlA.setEnabled(emacsMode.isSelected());
        // Autocomplete fields is only enabled when autocompletion is selected
        setAutoCompleteElementsEnabled(autoComplete.isSelected());
    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.AUTO_OPEN_FORM, autoOpenForm.isSelected());
        prefs.putBoolean(JabRefPreferences.DEFAULT_SHOW_SOURCE, defSource.isSelected());
        boolean emacsModeChanged = prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS) != emacsMode.isSelected();
        boolean emacsRebindCtrlAChanged = prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CA) != emacsRebindCtrlA.isSelected();
        boolean emacsRebindCtrlFChanged = prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CF) != emacsRebindCtrlF.isSelected();
        if (emacsModeChanged || emacsRebindCtrlAChanged || emacsRebindCtrlFChanged) {
            prefs.putBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS, emacsMode.isSelected());
            prefs.putBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CA, emacsRebindCtrlA.isSelected());
            prefs.putBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CF, emacsRebindCtrlF.isSelected());
            // immediately apply the change
            if (emacsModeChanged) {
                if (emacsMode.isSelected()) {
                    EmacsKeyBindings.load();
                } else {
                    EmacsKeyBindings.unload();
                }
            } else {
                // only rebinding of CTRL+a or CTRL+f changed
                assert emacsMode.isSelected();
                // we simply reload the emacs mode to activate the CTRL+a/CTRL+f change
                EmacsKeyBindings.unload();
                EmacsKeyBindings.load();
            }
        }
        autoCompletePreferences.setShortestLengthToComplete((Integer) shortestToComplete.getValue());
        prefs.putBoolean(JabRefPreferences.AUTO_COMPLETE, autoComplete.isSelected());
        autoCompletePreferences.setCompleteNames(autoCompFields.getText());
        if (autoCompBoth.isSelected()) {
            autoCompletePreferences.setOnlyCompleteFirstLast(false);
            autoCompletePreferences.setOnlyCompleteLastFirst(false);
        }
        else if (autoCompFF.isSelected()) {
            autoCompletePreferences.setOnlyCompleteFirstLast(true);
            autoCompletePreferences.setOnlyCompleteLastFirst(false);
        }
        else {
            autoCompletePreferences.setOnlyCompleteFirstLast(false);
            autoCompletePreferences.setOnlyCompleteLastFirst(true);
        }
        if (firstNameModeAbbr.isSelected()) {
            autoCompletePreferences.setFirstnameMode(AutoCompleteFirstNameMode.ONLY_ABBREVIATED);
        } else if (firstNameModeFull.isSelected()) {
            autoCompletePreferences.setFirstnameMode(AutoCompleteFirstNameMode.ONLY_FULL);
        } else {
            autoCompletePreferences.setFirstnameMode(AutoCompleteFirstNameMode.BOTH);
        }
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry editor");
    }
}
