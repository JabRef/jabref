package org.jabref.gui.bibtexkeypattern;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.help.HelpAction;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import org.jabref.model.bibtexkeypattern.DatabaseBibtexKeyPattern;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.EntryType;
import org.jabref.preferences.JabRefPreferences;

public class BibtexKeyPatternPanel extends Pane {

    // used by both BibtexKeyPatternPanel and TabLabelPAttern
    protected final GridBagLayout gbl = new GridBagLayout();
    protected final GridBagConstraints con = new GridBagConstraints();

    // default pattern
    protected final TextField defaultPat = new TextField();
    private final HelpAction help;

    // one field for each type
    private final Map<String, TextField> textFields = new HashMap<>();
    private final BasePanel panel;
    private final GridPane gridPane = new GridPane();

    public BibtexKeyPatternPanel(BasePanel panel) {
        this.panel = panel;
        help = new HelpAction(Localization.lang("Help on key patterns"), HelpFile.BIBTEX_KEY_PATTERN);
        buildGUI();
    }

    private void buildGUI() {
        // The header - can be removed
        Label label = new Label(Localization.lang("Entry type"));
        gridPane.add(label, 1, 1);

        Label keyPattern = new Label(Localization.lang("Key pattern"));
        gridPane.add(keyPattern, 3, 1);

        Label defaultPattern = new Label(Localization.lang("Default pattern"));
        gridPane.add(defaultPattern, 1, 2);
        gridPane.add(defaultPat, 3, 2);

        Button button = new Button("Default");
        button.setOnAction(e-> defaultPat.setText((String) Globals.prefs.defaults.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN)));
        gridPane.add(button, 4, 2);

        BibDatabaseMode mode;
        // check mode of currently used DB
        if (panel != null) {
            mode = panel.getBibDatabaseContext().getMode();
        } else {
            // use preferences value if no DB is open
            mode = Globals.prefs.getDefaultBibDatabaseMode();
        }

        int rowIndex = 3;
        for (EntryType type : EntryTypes.getAllValues(mode)) {
            Label label1 = new Label(type.getName());

            TextField textField = new TextField();

            Button button1 = new Button("Default");
            button1.setOnAction(e1 -> textField.setText((String) Globals.prefs.defaults.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN)));

            gridPane.add(label1, 1, rowIndex);
            gridPane.add(textField, 3, rowIndex);
            gridPane.add(button1, 4, rowIndex);

            textFields.put(type.getName().toLowerCase(Locale.ROOT), textField);

            rowIndex++;
        }

        Button help1 = new Button("?");
        help1.setOnAction(e->new HelpAction(Localization.lang("Help on key patterns"), HelpFile.BIBTEX_KEY_PATTERN).getHelpButton().doClick());
        gridPane.add(help1, 1, rowIndex);

        Button btnDefaultAll1 = new Button(Localization.lang("Reset all"));
        btnDefaultAll1.setOnAction(e-> {
            // reset all fields
            for (TextField field : textFields.values()) {
                field.setText("");
            }
            defaultPat.setText((String) Globals.prefs.defaults.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN));
        });
        gridPane.add(btnDefaultAll1, 3, rowIndex);
    }

    /**
     * fill the given LabelPattern by values generated from the text fields
     */
    private void fillPatternUsingPanelData(AbstractBibtexKeyPattern keypatterns) {
        // each entry type
        for (Map.Entry<String, TextField> entry : textFields.entrySet()) {
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
        for (Map.Entry<String, TextField> entry : textFields.entrySet()) {
            setValue(entry.getValue(), entry.getKey(), keyPattern);
        }

        if (keyPattern.getDefaultValue() == null || keyPattern.getDefaultValue().isEmpty()) {
            defaultPat.setText("");
        } else {
            defaultPat.setText(keyPattern.getDefaultValue().get(0));
        }
    }

    private static void setValue(TextField tf, String fieldName, AbstractBibtexKeyPattern keyPattern) {
        if (keyPattern.isDefaultValue(fieldName)) {
            tf.setText("");
        } else {
            tf.setText(keyPattern.getValue(fieldName).get(0));
        }
    }

    public Node getPanel() {
        return gridPane;
    }
}
