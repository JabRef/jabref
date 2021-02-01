package org.jabref.gui.citationkeypattern;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.logic.citationkeypattern.AbstractCitationKeyPattern;
import org.jabref.logic.citationkeypattern.DatabaseCitationKeyPattern;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.EntryType;
import org.jabref.preferences.JabRefPreferences;

public class CitationKeyPatternPanel extends Pane {

    // default pattern
    protected final TextField defaultPat = new TextField();

    // one field for each type
    private final Map<EntryType, TextField> textFields = new HashMap<>();
    private final BibDatabaseContext databaseContext;
    private final GridPane gridPane = new GridPane();

    public CitationKeyPatternPanel(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
        gridPane.setHgap(10);
        gridPane.setVgap(5);
        buildGUI();
    }

    private static void setValue(TextField tf, EntryType fieldName, AbstractCitationKeyPattern keyPattern) {
        if (keyPattern.isDefaultValue(fieldName)) {
            tf.setText("");
        } else {
            tf.setText(keyPattern.getValue(fieldName).get(0));
        }
    }

    private void buildGUI() {
        BibDatabaseMode mode = databaseContext.getMode();

        // The following got irrelevant  - global settings for CitationKeyPattern are handled by
        // commonfxcontrols/CitationKeyPatternPanel.java
        // ToDo: this one should be abandoned
        /*
        // check mode of currently used DB
        if (panel != null) {
            mode = panel.getBibDatabaseContext().getMode();
        } else {
            // use preferences value if no DB is open
            mode = Globals.prefs.getDefaultBibDatabaseMode();
        }
        */

        int rowIndex = 1;
        int columnIndex = 0;
        // The header - can be removed
        int columnsNumber = 2;
        for (int i = 0; i < columnsNumber; i++) {
            Label label = new Label(Localization.lang("Entry type"));
            Label keyPattern = new Label(Localization.lang("Key pattern"));
            gridPane.add(label, ++columnIndex, rowIndex);
            gridPane.add(keyPattern, ++columnIndex, rowIndex);
            columnIndex++;
        }

        rowIndex++;
        Label defaultPattern = new Label(Localization.lang("Default pattern"));
        Button button = new Button("Default");
        button.setOnAction(e -> defaultPat.setText((String) Globals.prefs.defaults.get(JabRefPreferences.DEFAULT_CITATION_KEY_PATTERN)));
        gridPane.add(defaultPattern, 1, rowIndex);
        gridPane.add(defaultPat, 2, rowIndex);
        gridPane.add(button, 3, rowIndex);

        columnIndex = 1;
        for (BibEntryType type : Globals.entryTypesManager.getAllTypes(mode)) {
            Label label1 = new Label(type.getType().getDisplayName());
            TextField textField = new TextField();
            Button button1 = new Button("Default");
            button1.setOnAction(e1 -> textField.setText((String) Globals.prefs.defaults.get(JabRefPreferences.DEFAULT_CITATION_KEY_PATTERN)));

            gridPane.add(label1, 1 + (columnIndex * 3), rowIndex);
            gridPane.add(textField, 2 + (columnIndex * 3), rowIndex);
            gridPane.add(button1, 3 + (columnIndex * 3), rowIndex);

            textFields.put(type.getType(), textField);

            if (columnIndex == (columnsNumber - 1)) {
                columnIndex = 0;
                rowIndex++;
            } else {
                columnIndex++;
            }
        }

        rowIndex++;

        ActionFactory factory = new ActionFactory(Globals.prefs.getKeyBindingRepository());
        Button help = factory.createIconButton(StandardActions.HELP_KEY_PATTERNS, new HelpAction(HelpFile.CITATION_KEY_PATTERN));
        gridPane.add(help, 1, rowIndex);

        Button btnDefaultAll1 = new Button(Localization.lang("Reset all"));
        btnDefaultAll1.setOnAction(e -> {
            // reset all fields
            for (TextField field : textFields.values()) {
                field.setText("");
            }
            defaultPat.setText((String) Globals.prefs.defaults.get(JabRefPreferences.DEFAULT_CITATION_KEY_PATTERN));
        });
        gridPane.add(btnDefaultAll1, 2, rowIndex);
    }

    public DatabaseCitationKeyPattern getKeyPatternAsDatabaseKeyPattern() {
        DatabaseCitationKeyPattern res = new DatabaseCitationKeyPattern(Globals.prefs.getGlobalCitationKeyPattern());
        fillPatternUsingPanelData(res);
        return res;
    }

    /**
     * fill the given LabelPattern by values generated from the text fields
     */
    private void fillPatternUsingPanelData(AbstractCitationKeyPattern keypatterns) {
        // each entry type
        for (Map.Entry<EntryType, TextField> entry : textFields.entrySet()) {
            String text = entry.getValue().getText();
            if (!text.trim().isEmpty()) {
                keypatterns.addCitationKeyPattern(entry.getKey(), text);
            }
        }

        // default value
        String text = defaultPat.getText();
        if (!text.trim().isEmpty()) { // we do not trim the value at the assignment to enable users to have spaces at the beginning and at the end of the pattern
            keypatterns.setDefaultValue(text);
        }
    }

    /**
     * Fills the current values to the text fields
     *
     * @param keyPattern the CitationKeyPattern to use as initial value
     */
    public void setValues(AbstractCitationKeyPattern keyPattern) {
        for (Map.Entry<EntryType, TextField> entry : textFields.entrySet()) {
            setValue(entry.getValue(), entry.getKey(), keyPattern);
        }

        if ((keyPattern.getDefaultValue() == null) || keyPattern.getDefaultValue().isEmpty()) {
            defaultPat.setText("");
        } else {
            defaultPat.setText(keyPattern.getDefaultValue().get(0));
        }
    }

    public Node getPanel() {
        return gridPane;
    }
}
