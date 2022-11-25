package org.jabref.gui.edit;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class ReplaceStringView extends BaseDialog<Void> {

    @FXML private RadioButton allReplace;
    @FXML private CheckBox selectFieldOnly;
    @FXML private ButtonType replaceButton;
    @FXML private TextField limitFieldInput;
    @FXML private TextField findField;
    @FXML private TextField replaceField;

    private ReplaceStringViewModel viewModel;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public ReplaceStringView(LibraryTab libraryTab) {
        this.setTitle(Localization.lang("Replace String"));

        viewModel = new ReplaceStringViewModel(libraryTab);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(replaceButton, getDialogPane(), event -> buttonReplace());
    }

    @FXML
    public void initialize() {
        visualizer.setDecoration(new IconValidationDecorator());

        viewModel.findStringProperty().bind(findField.textProperty());
        viewModel.replaceStringProperty().bind(replaceField.textProperty());
        viewModel.fieldStringProperty().bind(limitFieldInput.textProperty());
        viewModel.selectOnlyProperty().bind(selectFieldOnly.selectedProperty());
        viewModel.allFieldReplaceProperty().bind(allReplace.selectedProperty());
    }

    @FXML
    private void buttonReplace() {
        String findString = findField.getText();
        if ("".equals(findString)) {
            this.close();
            return;
        }
        viewModel.replace();
        this.close();
    }
}
