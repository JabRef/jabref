package org.jabref.gui;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class ReplaceStringView extends BaseDialog<Void>
{

    @FXML private ButtonType replaceButton;
    @FXML private TextField limitFieldInput;
    @FXML private TextField findField;
    @FXML private TextField replaceField;
    @FXML private DialogPane pane;

    private boolean allFieldReplace;
    private boolean selOnly;
    private BasePanel panel;
    private Stage stage;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public ReplaceStringView(BasePanel basePanel)  {
        this.setTitle(Localization.lang("Replace String"));

        allFieldReplace = true;
        selOnly = false;
        panel = basePanel;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        stage = (Stage) this.pane.getScene().getWindow();
        ControlHelper.setAction(replaceButton, getDialogPane(), event -> buttonReplace());
    }

    @FXML
    public void initialize() {
        visualizer.setDecoration(new IconValidationDecorator());
    }

    @FXML
    public void buttonReplace() {
        String findString = findField.getText();
        String replaceString = replaceField.getText();
        String[] fieldStrings = limitFieldInput.getText().toLowerCase().split(";");
        if ("".equals(findString))
        {
            stage.close();
            return;
        }
        ReplaceStringViewModel viewModel = new ReplaceStringViewModel(panel, fieldStrings, findString, replaceString, selOnly, allFieldReplace);
        viewModel.replace();
        stage.close();
    }

    @FXML
    public void radioAll() {
        allFieldReplace = true;
    }

    @FXML
    public void radioLimit() {
        allFieldReplace = false;
    }

    @FXML
    public void selectOnly() {
        selOnly = !selOnly;
    }

}
