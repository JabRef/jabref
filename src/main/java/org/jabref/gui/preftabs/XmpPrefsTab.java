package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.JPanel;

import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;

import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

/**
 * Preference Tab for XMP.
 *
 * Allows the user to enable and configure the XMP privacy filter.
 */
class XmpPrefsTab extends JPanel implements PrefsTab {

    private final JabRefPreferences prefs;
    private boolean tableChanged;
    private int rowCount;
    private final ArrayList<TextField> textFields = new ArrayList<>(10);
    private final VBox vBox = new VBox();
    private final CheckBox privacyFilterCheckBox = new CheckBox(
            Localization.lang("Do not write the following fields to XMP Metadata:"));

    private final List<Object> tableRows = new ArrayList<>(10);


    /**
     * Customization of external program paths.
     */
    public XmpPrefsTab(JabRefPreferences prefs) {
        this.prefs = Objects.requireNonNull(prefs);
        setLayout(new BorderLayout());

        GridPane builder = new GridPane();

        BorderPane tablePanel = new BorderPane();
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setMaxHeight(400);
        scrollPane.setMaxWidth(170);
        textFields.add(new TextField("pdf"));
        vBox.getChildren().add(new Label("field to filter"));
        vBox.getChildren().addAll(textFields);
        scrollPane.setContent(vBox);
        tablePanel.setCenter(scrollPane);

        Button add = new Button("+");
        add.setOnAction(e->new AddRowAction());
        Button delete = new Button("-");
        delete.setOnAction(e->new DeleteRowAction());
        VBox toolbar = new VBox(add,delete);
        tablePanel.setRight(toolbar);


        // Build Prefs Tabs
        builder.add(new Label(Localization.lang("XMP export privacy settings")),1,1);

        builder.add(privacyFilterCheckBox,1,2);

        builder.add(tablePanel,1,3);

        JFXPanel panel = CustomJFXPanel.wrap(new Scene(builder));
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    class DeleteRowAction {
        public DeleteRowAction() {
            textFields.remove(textFields.get(textFields.size() - 1));
            rowCount--;
            vBox.getChildren().clear();
            vBox.getChildren().add(new Label("field to filter"));
            vBox.getChildren().addAll(textFields);
            tableChanged = true;
        }
    }

    class AddRowAction {

        public AddRowAction() {
            rowCount++;
            textFields.add(new TextField(""));
            vBox.getChildren().clear();
            vBox.getChildren().add(new Label("field to filter"));
            vBox.getChildren().addAll(textFields);
            tableChanged = true;
        }
    }
    /**
     * Load settings from the preferences and initialize the table.
     */
    @Override
    public void setValues() {
        tableRows.clear();
        //List<String> names = JabRefPreferences.getInstance().getStringList(JabRefPreferences.XMP_PRIVACY_FILTERS);
        List<String>names = new ArrayList<>();
        for (TextField textField : textFields){
           names.add(textField.getText());
        }
        tableRows.addAll(names);
        rowCount = tableRows.size() + 5;

        privacyFilterCheckBox.setSelected(JabRefPreferences.getInstance().getBoolean(
                JabRefPreferences.USE_XMP_PRIVACY_FILTER));
    }

    /**
     * Store changes to table preferences. This method is called when the user
     * clicks Ok.
     *
     */
    @Override
    public void storeSettings() {
        // Now we need to make sense of the contents the user has made to the
        // table setup table. This needs to be done either if changes were made, or
        // if the checkbox is checked and no field values have been stored previously:
        if (tableChanged ||
                (privacyFilterCheckBox.isSelected() && !prefs.hasKey(JabRefPreferences.XMP_PRIVACY_FILTERS))) {

            // First we remove all rows with empty names.
            for (int i = tableRows.size() - 1; i >= 0; i--) {
                if ((tableRows.get(i) == null) || tableRows.get(i).toString().isEmpty()) {
                    tableRows.remove(i);
                }
            }
            // Finally, we store the new preferences.
            JabRefPreferences.getInstance().putStringList(JabRefPreferences.XMP_PRIVACY_FILTERS,
                    tableRows.stream().map(Object::toString).collect(Collectors.toList()));
        }

        JabRefPreferences.getInstance().putBoolean(JabRefPreferences.USE_XMP_PRIVACY_FILTER, privacyFilterCheckBox.isSelected());
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("XMP-metadata");
    }
}
