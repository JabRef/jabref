package org.jabref.gui.libraryproperties.git;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

import org.jabref.gui.libraryproperties.AbstractPropertiesTabView;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class GitPropertiesView extends AbstractPropertiesTabView<GitPropertiesViewModel> {
    @FXML private CheckBox autoCommit;
    @FXML private CheckBox autoPush;
    @FXML private CheckBox autoPull;

    public GitPropertiesView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Git");
    }

    public void initialize() {
        this.viewModel = new GitPropertiesViewModel(databaseContext);

        autoCommit.selectedProperty().bindBidirectional(viewModel.autoCommitProperty());
        autoPush.selectedProperty().bindBidirectional(viewModel.autoPushProperty());
        autoPush.disableProperty().bind(autoCommit.selectedProperty().not());
        autoPull.selectedProperty().bindBidirectional(viewModel.autoPullProperty());
    }
}
