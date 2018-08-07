package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.Scene;

import javax.swing.JPanel;

import org.jabref.Globals;
import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.preferences.JabRefPreferences;

class TablePrefsTab extends JPanel implements PrefsTab {

    private final JabRefPreferences prefs;

    private final CheckBox autoResizeMode;
    private final CheckBox priDesc;
    private final CheckBox secDesc;
    private final CheckBox terDesc;

    private final RadioButton namesAsIs;
    private final RadioButton namesFf;
    private final RadioButton namesFl;
    private final RadioButton namesNatbib;
    private final RadioButton abbrNames;
    private final RadioButton noAbbrNames;
    private final RadioButton lastNamesOnly;

    private final TextField priField;
    private final TextField secField;
    private final TextField terField;
    private final TextField numericFields;
    private final ComboBox priSort;
    private final ComboBox secSort;
    private final ComboBox terSort;


    /**
     * Customization of external program paths.
     *
     * @param prefs
     *            a <code>JabRefPreferences</code> value
     */
    public TablePrefsTab(JabRefPreferences prefs) {
        this.prefs = prefs;
        setLayout(new BorderLayout());

        /**
         * Added Bibtexkey to combobox.
         *
         * [ 1540646 ] default sort order: bibtexkey
         *
         * http://sourceforge.net/tracker/index.php?func=detail&aid=1540646&group_id=92314&atid=600306
         */
        List<String> fieldNames = InternalBibtexFields.getAllPublicFieldNames();
        fieldNames.add(BibEntry.KEY_FIELD);
        Collections.sort(fieldNames);
        String[] allPlusKey = fieldNames.toArray(new String[fieldNames.size()]);
        priSort = new ComboBox<>(FXCollections.observableArrayList(allPlusKey));
        secSort = new ComboBox<>(FXCollections.observableArrayList(allPlusKey));
        terSort = new ComboBox<>(FXCollections.observableArrayList(allPlusKey));

        autoResizeMode = new CheckBox(Localization.lang("Fit table horizontally on screen"));

        namesAsIs = new RadioButton(Localization.lang("Show names unchanged"));
        namesFf = new RadioButton(Localization.lang("Show 'Firstname Lastname'"));
        namesFl = new RadioButton(Localization.lang("Show 'Lastname, Firstname'"));
        namesNatbib = new RadioButton(Localization.lang("Natbib style"));
        noAbbrNames = new RadioButton(Localization.lang("Do not abbreviate names"));
        abbrNames = new RadioButton(Localization.lang("Abbreviate names"));
        lastNamesOnly = new RadioButton(Localization.lang("Show last names only"));

        priField = new TextField();
        secField = new TextField();
        terField = new TextField();

        numericFields = new TextField();

        priSort.setValue("<select>");
        secSort.setValue("<select>");
        terSort.setValue("<select>");

        priSort.setOnAction(e -> {
            if (priSort.getBaselineOffset() > 0) {
                priField.setText(priSort.getItems().toString());
            }
        });
        secSort.setOnAction(e -> {
            if (secSort.getBaselineOffset() > 0) {
                secField.setText(secSort.getItems().toString());
            }
        });
        terSort.setOnAction(e -> {
            if (terSort.getBaselineOffset() > 0) {
                terField.setText(terSort.getItems().toString());
            }
        });

        priDesc = new CheckBox(Localization.lang("Descending"));
        secDesc = new CheckBox(Localization.lang("Descending"));
        terDesc = new CheckBox(Localization.lang("Descending"));

        GridPane builder = new GridPane();
        builder.add(new Label(Localization.lang("Format of author and editor name:")),1,1);
        builder.add(namesAsIs,1,2);
        builder.add(noAbbrNames,2,2);
        builder.add(namesFf,1,3);
        builder.add(abbrNames,2,3);
        builder.add(namesFl,1,4);
        builder.add(lastNamesOnly,2,4);

        builder.add(namesNatbib,1,5);


        builder.add(new Label(Localization.lang("Default sort criteria")),1,6);
        // Create a new panel with its own FormLayout for these items:
        Label lab = new Label();
        lab = new Label(Localization.lang("    Primary sort criterion"));
        builder.add(lab,1,7);
        builder.add(priSort,2,7);
        builder.add(priField,3,7);
        builder.add(priDesc,4,7);

        lab = new Label(Localization.lang("    Secondary sort criterion"));
        builder.add(lab,1,8);
        builder.add(secSort,2,8);
        builder.add(secField,3,8);
        builder.add(secDesc,4,8);

        lab = new Label(Localization.lang("    Tertiary sort criterion"));
        builder.add(lab,1,9);
        builder.add(terSort,2,9);
        builder.add(terField,3,9);
        builder.add(terDesc,4,9);


        builder.add(new Label(Localization.lang("    Sort the following fields as numeric fields") + ':'),1,10);
        builder.add(numericFields,2,10);

        builder.add(new Label(Localization.lang("General")),1,11);
        builder.add(autoResizeMode,1,12);
        JFXPanel panel = CustomJFXPanel.wrap(new Scene(builder));
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);

        namesNatbib.setOnAction(e -> {
            abbrNames.setDisable(namesNatbib.isSelected());
            lastNamesOnly.setDisable(namesNatbib.isSelected());
            noAbbrNames.setDisable(namesNatbib.isSelected());
        });
    }

    @Override
    public void setValues() {
        autoResizeMode.setSelected(prefs.getBoolean(JabRefPreferences.AUTO_RESIZE_MODE));

        priField.setText(prefs.get(JabRefPreferences.TABLE_PRIMARY_SORT_FIELD));
        secField.setText(prefs.get(JabRefPreferences.TABLE_SECONDARY_SORT_FIELD));
        terField.setText(prefs.get(JabRefPreferences.TABLE_TERTIARY_SORT_FIELD));


        if (prefs.getBoolean(JabRefPreferences.NAMES_AS_IS)) {
            namesAsIs.setSelected(true);
        } else if (prefs.getBoolean(JabRefPreferences.NAMES_FIRST_LAST)) {
            namesFf.setSelected(true);
        } else if (prefs.getBoolean(JabRefPreferences.NAMES_NATBIB)) {
            namesNatbib.setSelected(true);
        } else {
            namesFl.setSelected(true);
        }
        if (prefs.getBoolean(JabRefPreferences.ABBR_AUTHOR_NAMES)) {
            abbrNames.setSelected(true);
        } else if (prefs.getBoolean(JabRefPreferences.NAMES_LAST_ONLY)) {
            lastNamesOnly.setSelected(true);
        } else {
            noAbbrNames.setSelected(true);
        }
        priDesc.setSelected(prefs.getBoolean(JabRefPreferences.TABLE_PRIMARY_SORT_DESCENDING));
        secDesc.setSelected(prefs.getBoolean(JabRefPreferences.TABLE_SECONDARY_SORT_DESCENDING));
        terDesc.setSelected(prefs.getBoolean(JabRefPreferences.TABLE_TERTIARY_SORT_DESCENDING));

        abbrNames.setDisable(namesNatbib.isSelected());
        lastNamesOnly.setDisable(namesNatbib.isSelected());
        noAbbrNames.setDisable(namesNatbib.isSelected());

        String numF = prefs.get(JabRefPreferences.NUMERIC_FIELDS);
        if (numF == null) {
            numericFields.setText("");
        } else {
            numericFields.setText(numF);
        }

    }

    /**
     * Store changes to table preferences. This method is called when the user
     * clicks Ok.
     *
     */
    @Override
    public void storeSettings() {

        prefs.putBoolean(JabRefPreferences.NAMES_AS_IS, namesAsIs.isSelected());
        prefs.putBoolean(JabRefPreferences.NAMES_FIRST_LAST, namesFf.isSelected());
        prefs.putBoolean(JabRefPreferences.NAMES_NATBIB, namesNatbib.isSelected());
        prefs.putBoolean(JabRefPreferences.NAMES_LAST_ONLY, lastNamesOnly.isSelected());
        prefs.putBoolean(JabRefPreferences.ABBR_AUTHOR_NAMES, abbrNames.isSelected());

        prefs.putBoolean(JabRefPreferences.AUTO_RESIZE_MODE, autoResizeMode.isSelected());
        prefs.putBoolean(JabRefPreferences.TABLE_PRIMARY_SORT_DESCENDING, priDesc.isSelected());
        prefs.putBoolean(JabRefPreferences.TABLE_SECONDARY_SORT_DESCENDING, secDesc.isSelected());
        prefs.putBoolean(JabRefPreferences.TABLE_TERTIARY_SORT_DESCENDING, terDesc.isSelected());
        prefs.put(JabRefPreferences.TABLE_PRIMARY_SORT_FIELD, priField.getText().toLowerCase(Locale.ROOT).trim());
        prefs.put(JabRefPreferences.TABLE_SECONDARY_SORT_FIELD, secField.getText().toLowerCase(Locale.ROOT).trim());
        prefs.put(JabRefPreferences.TABLE_TERTIARY_SORT_FIELD, terField.getText().toLowerCase(Locale.ROOT).trim());

        String oldVal = prefs.get(JabRefPreferences.NUMERIC_FIELDS);
        String newVal = numericFields.getText().trim();
        if (newVal.isEmpty()) {
            newVal = null;
        }
        if (!Objects.equals(oldVal, newVal)) {
            prefs.put(JabRefPreferences.NUMERIC_FIELDS, newVal);
            InternalBibtexFields.setNumericFields(Globals.prefs.getStringList(JabRefPreferences.NUMERIC_FIELDS));
        }

    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry table");
    }
}
