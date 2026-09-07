package org.jabref.gui.git;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;

import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrychange.EntryWithPreviewAndSourceDetailsView;
import org.jabref.gui.collab.groupchange.GroupChange;
import org.jabref.gui.collab.groupchange.GroupChangeDetailsView;
import org.jabref.gui.collab.metedatachange.MetadataChange;
import org.jabref.gui.collab.metedatachange.MetadataChangeDetailsView;
import org.jabref.gui.collab.preamblechange.PreambleChange;
import org.jabref.gui.collab.preamblechange.PreambleChangeDetailsView;
import org.jabref.gui.collab.stringadd.BibTexStringAdd;
import org.jabref.gui.collab.stringadd.BibTexStringAddDetailsView;
import org.jabref.gui.collab.stringchange.BibTexStringChange;
import org.jabref.gui.collab.stringchange.BibTexStringChangeDetailsView;
import org.jabref.gui.collab.stringdelete.BibTexStringDelete;
import org.jabref.gui.collab.stringdelete.BibTexStringDeleteDetailsView;
import org.jabref.gui.collab.stringrename.BibTexStringRename;
import org.jabref.gui.collab.stringrename.BibTexStringRenameDetailsView;
import org.jabref.gui.mergeentries.threewaymerge.diffhighlighter.DiffHighlighter;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class GitDiffDialogView extends BaseDialog<Void> {

    @FXML private TableView<DatabaseChange> changesTableView;
    @FXML private TableColumn<DatabaseChange, String> changeName;
    @FXML private BorderPane changeInfoPane;

    @FXML private RadioButton highlightWordsRadioButton;
    @FXML private RadioButton highlightCharactersRadioButton;
    @FXML private ToggleGroup diffHighlightingMethodToggleGroup;

    @Inject private org.jabref.gui.DialogService dialogService;
    @Inject private GuiPreferences preferences;
    @Inject private BibEntryTypesManager entryTypesManager;
    @Inject private TaskExecutor taskExecutor;

    private final List<DatabaseChange> changes;
    private final BibDatabaseContext headDatabase;
    private final BibDatabaseContext workingTreeDatabase;
    private final String oldVersionLabel;
    private final String newVersionLabel;
    private final Map<DatabaseChange, Node> detailsViewCache = new HashMap<>();

    /// Diff dialog labeled for the Git commit use case: committed version (left) vs. saved file (right).
    ///
    /// @param changes             the changes to list, as computed by `DatabaseChangeList.compareAndGetChanges`
    /// @param headDatabase        the committed version (left side)
    /// @param workingTreeDatabase the saved file (right side)
    public GitDiffDialogView(List<DatabaseChange> changes,
                             BibDatabaseContext headDatabase,
                             BibDatabaseContext workingTreeDatabase) {
        this(changes, headDatabase, workingTreeDatabase, Localization.lang("Committed version"), Localization.lang("Saved file"));
    }

    /// @param changes             the changes to list, as computed by `DatabaseChangeList.compareAndGetChanges`
    /// @param headDatabase        the older version (left side)
    /// @param workingTreeDatabase the newer version (right side)
    /// @param oldVersionLabel     heading shown above the older version
    /// @param newVersionLabel     heading shown above the newer version
    public GitDiffDialogView(List<DatabaseChange> changes,
                             BibDatabaseContext headDatabase,
                             BibDatabaseContext workingTreeDatabase,
                             String oldVersionLabel,
                             String newVersionLabel) {
        this.changes = changes;
        this.headDatabase = headDatabase;
        this.workingTreeDatabase = workingTreeDatabase;
        this.oldVersionLabel = oldVersionLabel;
        this.newVersionLabel = newVersionLabel;

        setTitle(Localization.lang("Diff view"));
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        PreviewViewer previewViewer = new PreviewViewer(dialogService, preferences, taskExecutor);

        changeName.setText(Localization.lang("Change"));
        changeName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        changesTableView.setItems(javafx.collections.FXCollections.observableArrayList(changes));
        changesTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        changesTableView.getSelectionModel().selectedItemProperty().addListener((_, _, selectedChange) -> {
            if (selectedChange != null) {
                changeInfoPane.setCenter(detailsViewCache.computeIfAbsent(selectedChange, change -> createDetailsNode(change, previewViewer)));
            } else {
                changeInfoPane.setCenter(null);
            }
        });

        if (!changes.isEmpty()) {
            changesTableView.getSelectionModel().selectFirst();
        }

        diffHighlightingMethodToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            detailsViewCache.clear();
            DatabaseChange selectedChange = changesTableView.getSelectionModel().getSelectedItem();
            if (selectedChange != null) {
                changeInfoPane.setCenter(createDetailsNode(selectedChange, previewViewer));
            }
        });
    }

    private Node createDetailsNode(DatabaseChange change, PreviewViewer previewViewer) {
        return switch (change) {
            case EntryChange entryChange ->
                    new GitEntryChangeDetailsView(
                            entryChange.getOldEntry(),
                            entryChange.getNewEntry(),
                            headDatabase,
                            workingTreeDatabase,
                            preferences,
                            entryTypesManager,
                            oldVersionLabel,
                            newVersionLabel,
                            getDiffMethod()
                    );
            case org.jabref.gui.collab.entryadd.EntryAdd entryAdd ->
                    new EntryWithPreviewAndSourceDetailsView(
                            entryAdd.getAddedEntry(),
                            workingTreeDatabase,
                            preferences,
                            entryTypesManager,
                            previewViewer
                    );
            case org.jabref.gui.collab.entrydelete.EntryDelete entryDelete ->
                    new EntryWithPreviewAndSourceDetailsView(
                            entryDelete.getDeletedEntry(),
                            headDatabase,
                            preferences,
                            entryTypesManager,
                            previewViewer
                    );
            case MetadataChange metadataChange ->
                    new MetadataChangeDetailsView(
                            metadataChange,
                            preferences.getCitationKeyPatternPreferences().getKeyPatterns(),
                            oldVersionLabel,
                            newVersionLabel,
                            getDiffMethod()
                    );
            case GroupChange groupChange ->
                    new GroupChangeDetailsView(groupChange, groupChange.getName() + '.', oldVersionLabel, newVersionLabel, getDiffMethod());
            case PreambleChange preambleChange ->
                    new PreambleChangeDetailsView(preambleChange);
            case BibTexStringAdd stringAdd ->
                    new BibTexStringAddDetailsView(stringAdd);
            case BibTexStringDelete stringDelete ->
                    new BibTexStringDeleteDetailsView(stringDelete);
            case BibTexStringChange stringChange ->
                    new BibTexStringChangeDetailsView(stringChange);
            case BibTexStringRename stringRename ->
                    new BibTexStringRenameDetailsView(stringRename);
            default -> {
                Label label = new Label(change.getName());
                label.setWrapText(true);
                yield label;
            }
        };
    }

    private DiffHighlighter.BasicDiffMethod getDiffMethod() {
        return highlightWordsRadioButton.isSelected()
               ? DiffHighlighter.BasicDiffMethod.WORDS
               : DiffHighlighter.BasicDiffMethod.CHARS;
    }
}
