package org.jabref.gui.git;

 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;

 import javax.swing.undo.UndoManager;

 import javafx.beans.property.SimpleStringProperty;
 import javafx.fxml.FXML;
 import javafx.scene.control.Button;
 import javafx.scene.control.SelectionMode;
 import javafx.scene.control.TableColumn;
 import javafx.scene.control.TableView;
 import javafx.scene.layout.BorderPane;

 import org.jabref.gui.DialogService;
 import org.jabref.gui.StateManager;
 import org.jabref.gui.collab.ExternalChangesResolverViewModel;
 import org.jabref.gui.preview.PreviewViewer;
 import org.jabref.gui.theme.ThemeManager;
 import org.jabref.gui.util.BaseDialog;
 import org.jabref.gui.util.TaskExecutor;
 import org.jabref.model.entry.BibEntryTypesManager;
 import org.jabref.preferences.PreferencesService;

 import com.airhacks.afterburner.views.ViewLoader;
 import com.tobiasdiez.easybind.EasyBind;
 import jakarta.inject.Inject;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 public class GitChangesResolverDialog extends BaseDialog<Boolean> {
     private final static Logger LOGGER = LoggerFactory.getLogger(GitChangesResolverDialog.class);
     /**
      * Reconstructing the details view to preview an {@link GitChange} every time it's selected is a heavy operation.
      * It is also useless because changes are static and if the change data is static then the view doesn't have to change
      * either. This cache is used to ensure that we only create the detail view instance once for each {@link GitChange}.
      */
     private final Map<GitChange, GitChangeDetailsView> DETAILS_VIEW_CACHE = new HashMap<>();

     @FXML
     private TableView<GitChange> changesTableView;
     @FXML
     private TableColumn<GitChange, String> changeName;
     @FXML
     private Button askUserToResolveChangeButton;
     @FXML
     private BorderPane changeInfoPane;

     private final List<GitChange> changes;
     private final BibGitContext git;

     private ExternalChangesResolverViewModel viewModel;

     @Inject private UndoManager undoManager;
     @Inject private StateManager stateManager;
     @Inject private DialogService dialogService;
     @Inject private PreferencesService preferencesService;
     @Inject private ThemeManager themeManager;
     @Inject private BibEntryTypesManager entryTypesManager;
     @Inject private TaskExecutor taskExecutor;

     /**
      * A dialog going through given <code>changes</code>, which are diffs to the provided <code>git</code>.
      * Each accepted change is written to the provided <code>git</code>.
      *
      * @param changes The list of changes
      * @param git The git to apply the changes to
      */
     public GitChangesResolverDialog(List<GitChange> changes, BibGitContext git, String dialogTitle) {
         this.changes = changes;
         this.git = git;

         this.setTitle(dialogTitle);
         ViewLoader.view(this)
                 .load()
                 .setAsDialogPane(this);

         this.setResultConverter(button -> {
             if (viewModel.areAllChangesResolved()) {
                 LOGGER.info("External changes are resolved successfully");
                 return true;
             } else {
                 LOGGER.info("External changes aren't resolved");
                 return false;
             }
         });
     }

     @FXML
     private void initialize() {
         PreviewViewer previewViewer = new PreviewViewer(git, dialogService, preferencesService, stateManager, themeManager, taskExecutor);
         GitChangeDetailsViewFactory gitChangeDetailsViewFactory = new GitChangeDetailsViewFactory(git, dialogService, stateManager, themeManager, preferencesService, entryTypesManager, previewViewer, taskExecutor);

         viewModel = new ExternalChangesResolverViewModel(changes, undoManager);

         changeName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
         askUserToResolveChangeButton.disableProperty().bind(viewModel.canAskUserToResolveChangeProperty().not());

         changesTableView.setItems(viewModel.getVisibleChanges());
         // Think twice before setting this to MULTIPLE...
         changesTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
         changesTableView.getSelectionModel().selectFirst();

         viewModel.selectedChangeProperty().bind(changesTableView.getSelectionModel().selectedItemProperty());
         EasyBind.subscribe(viewModel.selectedChangeProperty(), selectedChange -> {
             if (selectedChange != null) {
                 GitChangeDetailsView detailsView = DETAILS_VIEW_CACHE.computeIfAbsent(selectedChange, gitChangeDetailsViewFactory::create);
                 changeInfoPane.setCenter(detailsView);
             }
         });

         EasyBind.subscribe(viewModel.areAllChangesResolvedProperty(), isResolved -> {
             if (isResolved) {
                 viewModel.applyChanges();
                 close();
             }
         });
     }

     @FXML
     public void denyChanges() {
         viewModel.denyChange();
     }

     @FXML
     public void acceptChanges() {
         viewModel.acceptChange();
     }

     @FXML
     public void askUserToResolveChange() {
         viewModel.getSelectedChange().flatMap(GitChange::getExternalChangeResolver)
                  .flatMap(GitChangesResolver::askUserToResolveChange).ifPresent(viewModel::acceptMergedChange);
     }
 }