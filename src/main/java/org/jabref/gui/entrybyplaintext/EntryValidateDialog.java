package org.jabref.gui.entrybyplaintext;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tooltip;
import javax.inject.Inject;
import org.jabref.gui.StateManager;
import org.jabref.gui.bibtexextractor.BibtexExtractorViewModel;
import org.jabref.gui.fieldeditors.EditorTextField;
import org.jabref.gui.util.BaseDialog;
import org.jabref.model.database.BibDatabaseContext;
//TODO: call dialogservice to notify about added article
public class EntryValidateDialog extends BaseDialog<Void> {

    private final Button buttonValidate;
    @FXML private EditorTextField inputAuthor;
    @FXML private EditorTextField inputTitle;
    @FXML private EditorTextField inputJournal;
    @FXML private EditorTextField inputBibtex;
    @FXML private EditorTextField inputYear;
    @FXML private ButtonType validateButton;
    private EntryByPlainTextViewModel textViewModel;
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



  @FXML
  private void initialize() {
    BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
    this.textViewModel = new EntryByPlainTextViewModel(database);

    inputAuthor.textProperty().bindBidirectional(textViewModel.inputTextProperty());
    inputYear.textProperty().bindBidirectional(textViewModel.inputTextProperty());
    inputTitle.textProperty().bindBidirectional(textViewModel.inputTextProperty());
    inputJournal.textProperty().bindBidirectional(textViewModel.inputTextProperty());
    inputBibtex.textProperty().bindBidirectional(textViewModel.inputTextProperty());



  }

}
