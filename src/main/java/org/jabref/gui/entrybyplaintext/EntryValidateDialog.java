package org.jabref.gui.entrybyplaintext;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javax.inject.Inject;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;

public class EntryValidateDialog extends BaseDialog<Void> {

    private final Button buttonValidate;
    @FXML private TextArea inputAuthor;
    @FXML private TextArea inputTitle;
    @FXML private TextArea inputJournal;
    @FXML private TextArea inputBibtex;
    @FXML private TextArea inputYear;
    @FXML private ButtonType validateButton;

    @Inject private StateManager stateManager;

    public EntryValidateDialog(){

      ViewLoader.view(this).
                     load().
                     setAsDialogPane(this);

      this.setTitle("Is this your BibTex Entry?");
      buttonValidate = (Button) getDialogPane().lookupButton(validateButton);
      //buttonValidate.setOnAction(event -> );
      buttonValidate.setTooltip(new Tooltip());

    }




}
