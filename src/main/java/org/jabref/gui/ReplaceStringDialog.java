package org.jabref.gui;

import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

/**
 * Dialog for replacing strings.
 */
class ReplaceStringDialog extends FXDialog {
    private final TextField fieldsField = new TextField();
    private final TextField fromField = new TextField();
    private final TextField toField = new TextField();

    private final CheckBox selOnly = new CheckBox(Localization.lang("Limit to selected entries"));
    private final RadioButton allFi = new RadioButton(Localization.lang("All fields"));
    private final RadioButton field = new RadioButton(Localization.lang("Limit to fields") + ":");
    private boolean okPressed;
    private String[] fieldStrings;
    private String fromString;
    private String toString;


    public ReplaceStringDialog(JabRefFrame parent) {
        super(AlertType.NONE, Localization.lang("Replace string"));

        ToggleGroup tg = new ToggleGroup();
        selOnly.setSelected(false);
        allFi.setSelected(true);
        allFi.setToggleGroup(tg);
        field.setToggleGroup(tg);
        fieldsField.setPrefColumnCount(30);
        fromField.setPrefColumnCount(30);
        toField.setPrefColumnCount(30);

        Button ok = new Button(Localization.lang("OK"));
        ok.setOnAction(oa -> {
            fromString = fromField.getText();
            toString = toField.getText();
            if ("".equals(fromString)) {
                return;
            }
            okPressed = true;
            fieldStrings = fieldsField.getText().toLowerCase(Locale.ROOT).split(";");
            dispose();
        });
        ok.setPrefSize(100, 30);
        Button cancel = new Button(Localization.lang("Cancel"));
        cancel.setOnAction(oa -> dispose());
        cancel.setPrefSize(100, 30);

        // Layout starts here.
        GridPane main = new GridPane();
        GridPane settings = new GridPane();
        HBox title1 = new HBox();
        HBox title2 = new HBox();
        GridPane gp = new GridPane();
        HBox optionButtons = new HBox();
        getDialogPane().setContent(gp);
        gp.add(title1, 0, 0);
        gp.add(main, 0, 1);
        gp.add(title2, 0, 2);
        gp.add(settings, 0, 3);
        gp.add(optionButtons, 0, 4);
        gp.setPadding(new Insets(15, 5, 0, 0));
        gp.setGridLinesVisible(false);

        // Border title:
        Label str = new Label(Localization.lang("Strings"));
        Label rplStr = new Label(Localization.lang("Replace string"));
        str.setTextFill(Color.web("#778899"));
        rplStr.setTextFill(Color.web("#778899"));
        title1.getChildren().add(str);
        title1.setPadding(new Insets(10, 0, 0, 0));
        title2.getChildren().add(rplStr);
        title2.setPadding(new Insets(10, 0, 0, 0));

        // Settings panel:
        settings.setHgap(10);
        settings.setVgap(10);
        settings.setPadding(new Insets(5, 15, 5, 15));
        settings.add(selOnly, 0, 0, 2, 1);
        settings.add(allFi, 0, 2);
        settings.add(field, 0, 3);
        settings.add(fieldsField, 1, 3);
        settings.setStyle("-fx-content-display:top;"
                          + "-fx-border-insets:0 0 0 0;"
                          + "-fx-border-color:#D3D3D3");

        // Main panel:
        main.setHgap(34);
        main.setVgap(5);
        main.setPadding(new Insets(5, 15, 5, 15));
        main.add(new Label(Localization.lang("Search for") + ":"), 0, 0);
        main.add(new Label(Localization.lang("Replace with") + ":"), 0, 1);
        main.add(fromField, 1, 0);
        main.add(toField, 1, 1);
        main.setStyle("-fx-content-display:top;"
                      + "-fx-border-insets:0 0 0 0;"
                      + "-fx-border-color:#D3D3D3");

        //Option buttons:
        optionButtons.setAlignment(Pos.BOTTOM_CENTER);
        optionButtons.setPadding(new Insets(15, 0, 0, 0));
        optionButtons.setSpacing(35);
        optionButtons.getChildren().add(ok);
        optionButtons.getChildren().add(cancel);
    }

    public void setVisible(boolean b) {
        if (b) {
            show();
        } else {
            hide();
        }
    }

    public boolean okPressed() {
        return okPressed;
    }

    private boolean allFields() {
        return allFi.isSelected();
    }

    public boolean selOnly() {
        return selOnly.isSelected();
    }

    /**
     * Does the actual operation on a Bibtex entry based on the
     * settings specified in this same dialog. Returns the number of
     * occurences replaced.
     */
    public int replace(BibEntry be, NamedCompound ce) {
        int counter = 0;
        if (allFields()) {
            for (String s : be.getFieldNames()) {
                counter += replaceField(be, s, ce);
            }
        } else {
            for (String fld : fieldStrings) {
                counter += replaceField(be, fld, ce);
            }
        }
        return counter;
    }

    private int replaceField(BibEntry be, String fieldname, NamedCompound ce) {
        if (!be.hasField(fieldname)) {
            return 0;
        }
        String txt = be.getField(fieldname).get();
        StringBuilder sb = new StringBuilder();
        int ind;
        int piv = 0;
        int counter = 0;
        int len1 = fromString.length();
        while ((ind = txt.indexOf(fromString, piv)) >= 0) {
            counter++;
            sb.append(txt.substring(piv, ind)); // Text leading up to s1
            sb.append(toString); // Insert s2
            piv = ind + len1;
        }
        sb.append(txt.substring(piv));
        String newStr = sb.toString();
        be.setField(fieldname, newStr);
        ce.addEdit(new UndoableFieldChange(be, fieldname, txt, newStr));
        return counter;
    }

    /**
     * close the dialog
     */
    private void dispose() {
        ((Stage) (getDialogPane().getScene().getWindow())).close();
    }
}
