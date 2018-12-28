package org.jabref.gui.preferences;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

class TablePrefsTab extends Pane implements PrefsTab {

    private final JabRefPreferences prefs;

    private final CheckBox autoResizeMode;

    private final RadioButton namesAsIs;
    private final RadioButton namesFf;
    private final RadioButton namesFl;
    private final RadioButton namesNatbib;
    private final RadioButton abbrNames;
    private final RadioButton noAbbrNames;
    private final RadioButton lastNamesOnly;

    private final GridPane builder = new GridPane();

    /**
     * Customization of external program paths.
     *
     * @param prefs
     *            a <code>JabRefPreferences</code> value
     */
    public TablePrefsTab(JabRefPreferences prefs) {
        this.prefs = prefs;
        /**
         * Added Bibtexkey to combobox.
         *
         * [ 1540646 ] default sort order: bibtexkey
         *
         * http://sourceforge.net/tracker/index.php?func=detail&aid=1540646&group_id=92314&atid=600306
         */

        autoResizeMode = new CheckBox(Localization.lang("Fit table horizontally on screen"));
        namesAsIs = new RadioButton(Localization.lang("Show names unchanged"));
        namesFf = new RadioButton(Localization.lang("Show 'Firstname Lastname'"));
        namesFl = new RadioButton(Localization.lang("Show 'Lastname, Firstname'"));
        namesNatbib = new RadioButton(Localization.lang("Natbib style"));
        noAbbrNames = new RadioButton(Localization.lang("Do not abbreviate names"));
        abbrNames = new RadioButton(Localization.lang("Abbreviate names"));
        lastNamesOnly = new RadioButton(Localization.lang("Show last names only"));

        Label formatOfAuthor = new Label(Localization.lang("Format of author and editor names"));
        formatOfAuthor.getStyleClass().add("sectionHeader");
        builder.add(formatOfAuthor, 1, 1);
        final ToggleGroup formatNamesToggleGroup = new ToggleGroup();
        final ToggleGroup nameAbbrevToggleGroup = new ToggleGroup();
        builder.add(namesAsIs, 1, 2);
        namesAsIs.setToggleGroup(formatNamesToggleGroup);
        builder.add(noAbbrNames, 2, 2);
        noAbbrNames.setToggleGroup(nameAbbrevToggleGroup);
        builder.add(namesFf, 1, 3);
        namesFf.setToggleGroup(formatNamesToggleGroup);
        builder.add(abbrNames, 2, 3);
        abbrNames.setToggleGroup(nameAbbrevToggleGroup);
        builder.add(namesFl, 1, 4);
        namesFl.setToggleGroup(formatNamesToggleGroup);
        builder.add(lastNamesOnly, 2, 4);
        lastNamesOnly.setToggleGroup(nameAbbrevToggleGroup);
        builder.add(namesNatbib, 1, 5);
        namesNatbib.setToggleGroup(formatNamesToggleGroup);
        Label label1 = new Label("");
        builder.add(label1, 1, 6);
        Label general = new Label(Localization.lang("General"));
        general.getStyleClass().add("sectionHeader");
        builder.add(general, 1, 7);
        builder.add(autoResizeMode, 1, 8);

        abbrNames.disableProperty().bind(namesNatbib.selectedProperty());
        lastNamesOnly.disableProperty().bind(namesNatbib.selectedProperty());
        noAbbrNames.disableProperty().bind(namesNatbib.selectedProperty());

    }

    @Override
    public Node getBuilder() {
        return builder;
    }

    @Override
    public void setValues() {
        autoResizeMode.setSelected(prefs.getBoolean(JabRefPreferences.AUTO_RESIZE_MODE));

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
