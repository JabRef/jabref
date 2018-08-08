package org.jabref.gui.bibtexkeypattern;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import org.jabref.model.bibtexkeypattern.DatabaseBibtexKeyPattern;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.EntryType;
import org.jabref.preferences.JabRefPreferences;

public class BibtexKeyPatternPanel extends JPanel {

    // used by both BibtexKeyPatternPanel and TabLabelPAttern
    protected final GridBagLayout gbl = new GridBagLayout();
    protected final GridBagConstraints con = new GridBagConstraints();

    // default pattern
    protected final JTextField defaultPat = new JTextField();
    private final TextField textField = new TextField();

    private final HelpAction help;

    // one field for each type
    private final Map<String, JTextField> textFields = new HashMap<>();
    private final TextField[] textFieldArray = new TextField[19];
    private final BasePanel panel;
    private final GridPane gridPane = new GridPane();


    public BibtexKeyPatternPanel(BasePanel panel) {
        this.panel = panel;
        help = new HelpAction(Localization.lang("Help on key patterns"), HelpFile.BIBTEX_KEY_PATTERN);
        buildGUI();
    }

    private void buildGUI() {
        JPanel pan = new JPanel();
        JScrollPane sp = new JScrollPane(pan);
        sp.setPreferredSize(new Dimension(100, 100));
        sp.setBorder(BorderFactory.createEmptyBorder());
        pan.setLayout(gbl);
        setLayout(gbl);

        // The header - can be removed
        JLabel lblEntryType = new JLabel(Localization.lang("Entry type"));
        Label label = new Label(Localization.lang("Entry type"));
        gridPane.add(label,1,1);
        Font f = new Font("plain", Font.BOLD, 12);
        lblEntryType.setFont(f);
        con.gridx = 0;
        con.gridy = 0;
        con.gridwidth = 1;
        con.gridheight = 1;
        con.fill = GridBagConstraints.VERTICAL;
        con.anchor = GridBagConstraints.WEST;
        con.insets = new Insets(5, 5, 10, 0);
        gbl.setConstraints(lblEntryType, con);
        pan.add(lblEntryType);

        JLabel lblKeyPattern = new JLabel(Localization.lang("Key pattern"));
        Label label1 = new Label(Localization.lang("Key pattern"));
        gridPane.add(label1,2,1);
        lblKeyPattern.setFont(f);
        con.gridx = 1;
        con.gridy = 0;
        con.gridheight = 1;
        con.fill = GridBagConstraints.HORIZONTAL;
        con.anchor = GridBagConstraints.WEST;
        con.insets = new Insets(5, 5, 10, 5);
        gbl.setConstraints(lblKeyPattern, con);
        pan.add(lblKeyPattern);

        con.gridy = 1;
        con.gridx = 0;
        JLabel lab = new JLabel(Localization.lang("Default pattern"));
        Label label2 = new Label(Localization.lang("Default pattern"));
        gridPane.add(label2, 1,2);
        gridPane.add(textField,2,2);
        gbl.setConstraints(lab, con);
        pan.add(lab);
        con.gridx = 1;
        gbl.setConstraints(defaultPat, con);
        pan.add(defaultPat);
        con.insets = new Insets(5, 5, 10, 5);
        JButton btnDefault = new JButton(Localization.lang("Default"));
        btnDefault.addActionListener(
                e -> defaultPat.setText((String) Globals.prefs.defaults.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN)));

        Button button = new Button("Default");
        button.setOnAction(e-> defaultPat.setText((String) Globals.prefs.defaults.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN)));
        gridPane.add(button,3,2);
        con.gridx = 2;
        int y = 2;
        gbl.setConstraints(btnDefault, con);
        pan.add(btnDefault);

        BibDatabaseMode mode;
        // check mode of currently used DB
        if (panel != null) {
            mode = panel.getBibDatabaseContext().getMode();
        } else {
            // use preferences value if no DB is open
            mode = Globals.prefs.getDefaultBibDatabaseMode();
        }
        addExtraText();

        for (EntryType type : EntryTypes.getAllValues(mode)) {
            textFields.put(type.getName().toLowerCase(Locale.ROOT), addEntryType(pan, type, y));
            y++;
        }

        con.fill = GridBagConstraints.BOTH;
        con.gridx = 0;
        con.gridy = 1;
        con.gridwidth = 3;
        con.weightx = 1;
        con.weighty = 1;
        gbl.setConstraints(sp, con);
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
        con.insets = new Insets(0, 5, 0, 5);
        JButton hlb = new JButton(IconTheme.JabRefIcons.HELP.getSmallIcon());

        Button help1 = new Button("Help");
        help1.setOnAction(e->new HelpAction(Localization.lang("Help on key patterns"), HelpFile.BIBTEX_KEY_PATTERN).getHelpButton().doClick());
        gridPane.add(help1,1,24);

        hlb.setToolTipText(Localization.lang("Help on key patterns"));
        gbl.setConstraints(hlb, con);
        add(hlb);
        hlb.addActionListener(help);

        // And finally a button to reset everything
        JButton btnDefaultAll = new JButton(Localization.lang("Reset all"));
        con.gridx = 2;
        con.gridy = 2;

        con.weightx = 1;
        con.weighty = 0;
        con.anchor = GridBagConstraints.SOUTHEAST;
        con.insets = new Insets(20, 5, 0, 5);
        gbl.setConstraints(btnDefaultAll, con);
        btnDefaultAll.addActionListener(e -> {
            // reset all fields
            for (TextField textField : textFieldArray) {
                textField.setText("");
            }

            // also reset the default pattern
            defaultPat.setText((String) Globals.prefs.defaults.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN));
        });
        add(btnDefaultAll);

        Button btnDefaultAll1 = new Button(Localization.lang("Reset all"));
        btnDefaultAll1.setOnAction(e-> {
            // reset all fields
            for (JTextField field : textFields.values()) {
                field.setText("");
            }
            textField.setText((String) Globals.prefs.defaults.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN));
        });
        gridPane.add(btnDefaultAll1,2,24);
    }

    private void addExtraText() {
        Label []label = new Label[19];
        Button []button = new Button[19];
        for (int i=0; i <= 18; i++) {
            textFieldArray[i] = new TextField();
            button[i] = new Button("Default");
            button[i].setOnAction(e-> defaultPat.setText((String) Globals.prefs.defaults.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN)));
        }
        label[0] = new Label("Article");
        label[1] = new Label("Book");
        label[2] = new Label("Booklet");
        label[3] = new Label("Conference");
        label[4] = new Label("Electronic");
        label[5] = new Label("IEEEtranBSTCTI");
        label[6] = new Label("InBook");
        label[7] = new Label("InCollection");
        label[8] = new Label("InProceedings");
        label[9] = new Label("Manual");
        label[10] = new Label("MasterThesis");
        label[11] = new Label("Misc");
        label[12] = new Label("Patent");
        label[13] = new Label("Periodical");
        label[14] = new Label("PhdThesis");
        label[15] = new Label("Proceedings");
        label[16] = new Label("Standard");
        label[17] = new Label("TechReport");
        label[18] = new Label("Unpublished");
        for (int i = 0; i <= 18; i++) {
            gridPane.add(label[i],1,i + 3);
            gridPane.add(textFieldArray[i],2,i + 3);
            gridPane.add(button[i],3,i + 3);
        }
    }

    private JTextField addEntryType(Container c, EntryType type, int y) {

        JLabel lab = new JLabel(type.getName());
        con.gridx = 0;
        con.gridy = y;
        con.fill = GridBagConstraints.BOTH;
        con.weightx = 0;
        con.weighty = 0;
        con.anchor = GridBagConstraints.WEST;
        con.insets = new Insets(0, 5, 0, 5);
        gbl.setConstraints(lab, con);
        c.add(lab);

        JTextField tf = new JTextField();
        tf.setColumns(15);
        con.gridx = 1;
        con.fill = GridBagConstraints.HORIZONTAL;
        con.weightx = 1;
        con.weighty = 0;
        con.anchor = GridBagConstraints.CENTER;
        con.insets = new Insets(0, 5, 0, 5);
        gbl.setConstraints(tf, con);
        c.add(tf);

        JButton but = new JButton(Localization.lang("Default"));
        con.gridx = 2;
        con.fill = GridBagConstraints.BOTH;
        con.weightx = 0;
        con.weighty = 0;
        con.anchor = GridBagConstraints.CENTER;
        con.insets = new Insets(0, 5, 0, 5);
        gbl.setConstraints(but, con);
        but.setActionCommand(type.getName().toLowerCase(Locale.ROOT));
        but.addActionListener(e -> {
            JTextField tField = textFields.get(e.getActionCommand());
            tField.setText("");
        });
        c.add(but);

        return tf;
    }

    /**
     * fill the given LabelPattern by values generated from the text fields
     */
    private void fillPatternUsingPanelData(AbstractBibtexKeyPattern keypatterns) {
        // each entry type
        for (Map.Entry<String, JTextField> entry : textFields.entrySet()) {
            String text = entry.getValue().getText();
            if (!text.trim().isEmpty()) {
                keypatterns.addBibtexKeyPattern(entry.getKey(), text);
            }
        }

        // default value
        String text = defaultPat.getText();
        if (!text.trim().isEmpty()) { // we do not trim the value at the assignment to enable users to have spaces at the beginning and at the end of the pattern
            keypatterns.setDefaultValue(text);
        }
    }

    protected GlobalBibtexKeyPattern getKeyPatternAsGlobalBibtexKeyPattern() {
        GlobalBibtexKeyPattern res = GlobalBibtexKeyPattern.fromPattern(
                JabRefPreferences.getInstance().get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN)
        );
        fillPatternUsingPanelData(res);
        return res;
    }

    public DatabaseBibtexKeyPattern getKeyPatternAsDatabaseBibtexKeyPattern() {
        DatabaseBibtexKeyPattern res = new DatabaseBibtexKeyPattern(Globals.prefs.getKeyPattern());
        fillPatternUsingPanelData(res);
        return res;
    }

    /**
     * Fills the current values to the text fields
     *
     * @param keyPattern the BibtexKeyPattern to use as initial value
     */
    public void setValues(AbstractBibtexKeyPattern keyPattern) {
        for (Map.Entry<String, JTextField> entry : textFields.entrySet()) {
            setValue(entry.getValue(), entry.getKey(), keyPattern);
        }

        if (keyPattern.getDefaultValue() == null || keyPattern.getDefaultValue().isEmpty()) {
            defaultPat.setText("");
        } else {
            defaultPat.setText(keyPattern.getDefaultValue().get(0));
        }
    }

    private static void setValue(JTextField tf, String fieldName, AbstractBibtexKeyPattern keyPattern) {
        if (keyPattern.isDefaultValue(fieldName)) {
            tf.setText("");
        } else {
            tf.setText(keyPattern.getValue(fieldName).get(0));
        }
    }

    public GridPane getPanel() {
        return gridPane;
    }
}
