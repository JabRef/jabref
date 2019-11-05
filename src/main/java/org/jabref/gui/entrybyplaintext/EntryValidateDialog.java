package org.jabref.gui.entrybyplaintext;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javax.inject.Inject;
import org.jabref.gui.StateManager;
import org.jabref.gui.bibtexextractor.BibtexExtractorViewModel;
import org.jabref.gui.fieldeditors.EditorTextField;
import org.jabref.gui.util.BaseDialog;
import org.jabref.model.database.BibDatabaseContext;

public class EntryValidateDialog extends BaseDialog<Void> {

    private final Button buttonValidate;
    @FXML private EditorTextField inputAuthor;
    @FXML private EditorTextField inputTitle;
    @FXML private EditorTextField inputJournal;
    @FXML private EditorTextField inputBibtex;
    @FXML private EditorTextField inputYear;
    @FXML private ButtonType validateButton;
    private BibtexExtractorViewModel viewModel;
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
    this.viewModel = new BibtexExtractorViewModel(database);

    inputAuthor.textProperty().bindBidirectional(viewModel.inputTextProperty());
    inputYear.textProperty().bindBidirectional(viewModel.inputTextProperty());
    inputTitle.textProperty().bindBidirectional(viewModel.inputTextProperty());
    inputJournal.textProperty().bindBidirectional(viewModel.inputTextProperty());
    inputBibtex.textProperty().bindBidirectional(viewModel.inputTextProperty());



  }

}
