package org.jabref.gui.texparser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelTreeCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import org.controlsfx.control.CheckTreeView;

public class ParseTexDialogView extends BaseDialog<Void> {

    @FXML private TextField texDirectoryField;
    @FXML private Button browseButton;
    @FXML private Button searchButton;
    @FXML private CheckTreeView<FileNode> fileTreeView;
    @FXML private Button selectAllButton;
    @FXML private Button unselectAllButton;
    @FXML private ButtonType parseButtonType;

    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;
    @Inject private PreferencesService preferencesService;

    private BibDatabaseContext databaseContext;
    private Button parseButton;
    private ParseTexDialogViewModel viewModel;

    public ParseTexDialogView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;

        this.setTitle(Localization.lang("LaTeX integration tools"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        viewModel.getSearchPathList().addListener((ListChangeListener<Path>) c -> showTreeView());

        parseButton = (Button) this.getDialogPane().lookupButton(parseButtonType);
        parseButton.disableProperty().bindBidirectional(viewModel.parseDisableProperty());

        setResultConverter(button -> {
            if (button == parseButtonType) {
                viewModel.parse();
            }
            return null;
        });
    }

    @FXML
    private void initialize() {
        viewModel = new ParseTexDialogViewModel(databaseContext, dialogService, taskExecutor, preferencesService);

        texDirectoryField.textProperty().bindBidirectional(viewModel.texDirectoryProperty());

        browseButton.disableProperty().bindBidirectional(viewModel.browseDisableProperty());
        searchButton.disableProperty().bindBidirectional(viewModel.searchDisableProperty());

        new ViewModelTreeCellFactory<FileNode>()
                .withText(node -> node.toString())
                .install(fileTreeView);

        selectAllButton.disableProperty().bindBidirectional(viewModel.selectAllDisableProperty());
        unselectAllButton.disableProperty().bindBidirectional(viewModel.unselectAllDisableProperty());
    }

    private void showTreeView() {
        if (viewModel.getSearchPathList().isEmpty()) {
            fileTreeView.setRoot(null);
            return;
        }

        List<CheckBoxTreeItem<FileNode>> checkItems = new ArrayList<>();
        CheckBoxTreeItem<FileNode> rootItem = new CheckBoxTreeItem<>(new FileNode(Paths.get(texDirectoryField.getText())));
        checkItems.add(rootItem);

        for (Path currentPath : viewModel.getSearchPathList()) {
            CheckBoxTreeItem<FileNode> nodeItem = new CheckBoxTreeItem<>(new FileNode(currentPath));

            checkItems.stream()
                      .filter(item -> item.getValue().getPath().equals(currentPath.getParent()))
                      .findFirst()
                      .ifPresent(parent -> {
                          parent.getChildren().add(nodeItem);
                          parent.getValue().incFileCount();
                      });

            if (Files.isDirectory(currentPath)) {
                checkItems.add(nodeItem);
            }
        }

        fileTreeView.setRoot(rootItem);

        fileTreeView.getCheckModel()
                    .getCheckedItems()
                    .addListener((ListChangeListener<TreeItem<FileNode>>) c ->
                            viewModel.getSelectedFileList()
                                     .setAll(fileTreeView.getCheckModel()
                                                         .getCheckedItems()
                                                         .stream()
                                                         .map(item -> item.getValue().getPath())
                                                         .filter(path -> Files.isRegularFile(path))
                                                         .collect(Collectors.toList())));

        rootItem.setSelected(true);
        rootItem.setExpanded(true);
    }

    @FXML
    private void browseButtonClicked() {
        viewModel.browseButtonClicked();
    }

    @FXML
    private void searchButtonClicked() {
        viewModel.searchButtonClicked();
    }

    @FXML
    private void selectAll() {
        fileTreeView.getCheckModel().checkAll();
    }

    @FXML
    private void unselectAll() {
        fileTreeView.getCheckModel().clearChecks();
    }
}
