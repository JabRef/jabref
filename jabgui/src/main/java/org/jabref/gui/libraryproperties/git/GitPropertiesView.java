package org.jabref.gui.libraryproperties.git;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;

import org.jabref.gui.libraryproperties.AbstractPropertiesTabView;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * View controller for the "Git" tab in the Library Properties dialog.
 * <p>
 * This class handles the UI logic for configuring Git integration settings,
 * specifically the option to automatically commit and push changes on save.
 */
public class GitPropertiesView extends AbstractPropertiesTabView<GitPropertiesViewModel> {

    @FXML private CheckBox autoCommitCheckBox;

    public GitPropertiesView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
        this.viewModel = new GitPropertiesViewModel(databaseContext);
    }

    @Override
    public String getTabName() {
        return "Git";
    }

    @Override
    public Node getBuilder() {
        return ViewLoader.view(this).load().getView();
    }

    @FXML
    public void initialize() {
        autoCommitCheckBox.selectedProperty().bindBidirectional(viewModel.autoCommitProperty());
    }

    @Override
    public void setValues() {
        viewModel.setValues();
    }

    @Override
    public void storeSettings() {
        viewModel.storeSettings();
    }
}
