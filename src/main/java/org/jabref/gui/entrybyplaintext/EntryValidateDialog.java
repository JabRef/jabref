package org.jabref.gui.entrybyplaintext;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javax.inject.Inject;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.BaseDialog;
import org.jabref.model.database.BibDatabaseContext;
//TODO: call dialogservice to notify about added article
public class EntryValidateDialog extends BaseDialog<Void> {

    private final Button buttonValidate;
    @FXML private ButtonType validateButton;
    private EntryByPlainTextViewModel textViewModel;
    @FXML private TabPane tabPane;
    @Inject private StateManager stateManager;
    private static JabRefFrame jabRefFrame;
    private EntryEditor entryEditor;
    private static BasePanel basePanel;

    public EntryValidateDialog(){


      entryEditor = new EntryEditor(basePanel,ExternalFileTypes.getInstance());
      ViewLoader.view(this).
                     load().
                     setAsDialogPane(this);

      this.setTitle("Is this your BibTex Entry?");
      buttonValidate = (Button) getDialogPane().lookupButton(validateButton);
      //buttonValidate.setOnAction(event -> );
      buttonValidate.setTooltip(new Tooltip());

    }

  public static void setJabRefFrame(JabRefFrame frame){
      jabRefFrame = frame;
  }

  public static void setBasePanel(BasePanel panel){

      basePanel = panel;

  }

  @FXML
  private void initialize() {
    BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
    this.textViewModel = new EntryByPlainTextViewModel(database);
    tabPane.getTabs().addAll();


  }

}
