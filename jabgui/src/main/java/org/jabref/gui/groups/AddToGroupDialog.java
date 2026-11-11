package org.jabref.gui.groups;

import java.util.Optional;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.GroupTreeNode;

import com.airhacks.afterburner.views.ViewLoader;
import org.jspecify.annotations.NullMarked;

/// Asks for the group to assign the selected entries to. The keyboard stays in the search field:
/// typing filters, the arrow keys move the selection and Enter confirms.
///
/// The dialog's result tells whether the user confirmed; the assignment itself is left to the caller.
@NullMarked
public class AddToGroupDialog extends BaseDialog<Boolean> {

    @FXML private TextField searchField;
    @FXML private ListView<GroupTreeNode> groupList;

    private final AddToGroupDialogViewModel viewModel;
    private final ButtonType assignButtonType = new ButtonType(Localization.lang("Assign"), ButtonBar.ButtonData.OK_DONE);

    public AddToGroupDialog(AddToGroupDialogViewModel viewModel) {
        this.viewModel = viewModel;
        this.setTitle(Localization.lang("Assign entries to group"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.getDialogPane().getButtonTypes().setAll(assignButtonType, ButtonType.CANCEL);
        this.getDialogPane().lookupButton(assignButtonType).disableProperty().bind(viewModel.selectedGroupProperty().isNull());
        this.setResultConverter(button -> button == assignButtonType);

        Platform.runLater(() -> searchField.requestFocus());
    }

    @FXML
    private void initialize() {
        searchField.textProperty().bindBidirectional(viewModel.searchTextProperty());
        searchField.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case DOWN -> {
                    viewModel.selectNext();
                    event.consume();
                }
                case UP -> {
                    viewModel.selectPrevious();
                    event.consume();
                }
            }
        });
        searchField.setOnAction(event -> {
            confirm();
            event.consume();
        });

        new ViewModelListCellFactory<GroupTreeNode>()
                .withText(GroupTreeNode::getPath)
                .withOnMouseClickedEvent((_, event) -> {
                    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                        confirm();
                    }
                })
                .install(groupList);
        groupList.setItems(viewModel.getGroups());

        groupList.getSelectionModel().selectedItemProperty().addListener((_, _, group) ->
                Optional.ofNullable(group).ifPresent(viewModel.selectedGroupProperty()::set));
        viewModel.selectedGroupProperty().addListener((_, _, _) -> showSelectedGroup());
        // Filtering may drop the list's selection even when the view model keeps its group
        viewModel.getGroups().addListener((ListChangeListener<GroupTreeNode>) _ -> showSelectedGroup());
        showSelectedGroup();
    }

    private void showSelectedGroup() {
        viewModel.getSelectedIndex().ifPresentOrElse(
                index -> {
                    groupList.getSelectionModel().select(index);
                    groupList.scrollTo(index);
                },
                groupList.getSelectionModel()::clearSelection);
    }

    private void confirm() {
        if (getDialogPane().lookupButton(assignButtonType) instanceof Button assignButton && !assignButton.isDisabled()) {
            assignButton.fire();
        }
    }
}
