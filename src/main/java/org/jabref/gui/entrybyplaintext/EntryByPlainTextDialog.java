package org.jabref.gui.entrybyplaintext;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javax.inject.Inject;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;

public class EntryByPlainTextDialog extends BaseDialog<Void> {

    private final Button buttonParse;
    @FXML private TextArea input;
    @FXML private ButtonType buttonParseType;

    @Inject private StateManager stateManager;

    public EntryByPlainTextDialog(){

      ViewLoader.view(this).
                     load().
                     setAsDialogPane(this);

      this.setTitle("Input a plain text to parse");
      buttonParse = (Button) getDialogPane().lookupButton(buttonParseType);
      //buttonParse.setTooltip();
      //buttonParse.setOnAction(event -> );


    }




}
