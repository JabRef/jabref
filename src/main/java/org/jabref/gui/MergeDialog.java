package org.jabref.gui;

import org.jabref.logic.l10n.Localization;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
/**
 * Asks for details about merge database operation.
 */
public class MergeDialog extends FXDialog {

    private final Pane panel1 = new Pane();
    private final Pane Panel1 = new Pane();
    private final Pane Panel2 = new Pane();
    private final Button ok = new Button();
    private final Button cancel = new Button();
    private final CheckBox entries = new CheckBox();
    private final CheckBox strings = new CheckBox();
    private final GridPane gridPane1 = new GridPane();
    private final CheckBox groups = new CheckBox();
    private final CheckBox selector = new CheckBox();


    private boolean okPressed;

    public MergeDialog(JabRefFrame frame, String title, boolean modal) {
      super(AlertType.NONE, Localization.lang("Append Library"));
        jbInit();

    }

    public boolean isOkPressed() {
        return okPressed;
    }

    private void jbInit() {

        ok.setText(Localization.lang("OK"));
        ok.setOnAction(oa->{
          okPressed = true;
          dispose();
        });
        ok.setPrefWidth(100);
        ok.setPrefHeight(30);
        cancel.setText(Localization.lang("Cancel"));
        cancel.setOnAction(oa-> dispose());
        cancel.setPrefWidth(100);
        cancel.setPrefHeight(30);
        entries.setSelected(true);
        entries.setText(Localization.lang("Import entries"));
        strings.setSelected(true);
        strings.setText(Localization.lang("Import strings"));
        groups.setText(Localization.lang("Import group definitions"));
        selector.setText(Localization.lang("Import word selector definitions"));


        GridPane gp= new GridPane();
        HBox optionButtons = new HBox();
        getDialogPane().setContent(gp);
        gp.setHgap(10);
        gp.setVgap(10);
        gp.setPadding(new Insets(15,5,0,0));
        gp.add(entries, 0, 0);
        gp.add(strings, 0, 1);
        gp.add(groups, 0, 2);
        gp.add(selector, 0, 3);
        gp.add(optionButtons,0,4);
        gp.setPadding(new Insets(15,5,0,0));
        gp.setGridLinesVisible(false);

        optionButtons.setAlignment(Pos.BOTTOM_CENTER);
        optionButtons.setPadding(new Insets(15, 0, 0, 0));
        optionButtons.setSpacing(35);
        optionButtons.getChildren().add(ok);
        optionButtons.getChildren().add(cancel);

    }

    public boolean importEntries() {
        return entries.isSelected();
    }

    public boolean importGroups() {
        return groups.isSelected();
    }

    public boolean importStrings() {
        return strings.isSelected();
    }

    public boolean importSelectorWords() {
        return selector.isSelected();
    }

    public void setVisible(boolean b) {
      if (b) {
          show();
      } else {
          hide();
      }
  }
    private void dispose() {
      ((Stage) (getDialogPane().getScene().getWindow())).close();
  }
}
