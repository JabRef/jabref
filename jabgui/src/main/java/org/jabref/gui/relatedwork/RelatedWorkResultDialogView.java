package org.jabref.gui.relatedwork;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.relatedwork.RelatedWorkMatchResult;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class RelatedWorkResultDialogView extends BaseDialog<Void> {

    private final BibEntry sourceEntry;
    private final List<RelatedWorkMatchResult> matchedResults;
    private final String userName;

    @FXML private TableView<RelatedWorkMatchResult> matchedReferenceTable;
    @FXML private TableColumn<RelatedWorkMatchResult, String> citationMarkerColumn;
    @FXML private TableColumn<RelatedWorkMatchResult, String> citationKeyColumn;
    @FXML private TableColumn<RelatedWorkMatchResult, String> commentPreviewColumn;
    @FXML private ButtonType insertButtonType;

    @Inject private DialogService dialogService;
    @Inject private UndoManager undoManager;

    private RelatedWorkResultDialogViewModel viewModel;

    public RelatedWorkResultDialogView(BibEntry sourceEntry,
                                       List<RelatedWorkMatchResult> matchedResults,
                                       String userName) {
        this.sourceEntry = sourceEntry;
        this.matchedResults = matchedResults;
        this.userName = userName;

        setTitle(Localization.lang("Insert related work text"));

        ViewLoader.view(this).load().setAsDialogPane(this);

        ControlHelper.setAction(insertButtonType, getDialogPane(), event -> {
            if (viewModel.insertComments()) {
                close();
            }
        });
    }

    @FXML
    private void initialize() {
        this.viewModel = new RelatedWorkResultDialogViewModel(
                sourceEntry,
                matchedResults,
                userName,
                dialogService,
                undoManager
        );

        this.matchedReferenceTable.setItems(viewModel.matchedReferencesProperty());

        this.citationMarkerColumn.setCellValueFactory(cellData -> BindingsHelper.constantOf(cellData.getValue().citationKey()));
        this.citationKeyColumn.setCellValueFactory(cellData -> BindingsHelper.constantOf(getCitationKey(cellData.getValue())));
        this.commentPreviewColumn.setCellValueFactory(cellData -> BindingsHelper.constantOf(getCommentPreview(cellData.getValue())));

        new ValueTableCellFactory<RelatedWorkMatchResult, String>().withText(item -> item).withTooltip(item -> item).install(citationMarkerColumn);
        new ValueTableCellFactory<RelatedWorkMatchResult, String>().withText(item -> item).withTooltip(item -> item).install(citationKeyColumn);
        new ValueTableCellFactory<RelatedWorkMatchResult, String>().withText(item -> item).withTooltip(item -> item).install(commentPreviewColumn);
    }

    private String getCitationKey(RelatedWorkMatchResult matchResult) {
        return matchResult.matchedLibraryBibEntry()
                          .flatMap(BibEntry::getCitationKey)
                          .orElse("undefined");
    }

    private String getCommentPreview(RelatedWorkMatchResult matchResult) {
        String sourceCitationKey = sourceEntry.getCitationKey().orElse("undefined");
        return "[%s]: %s".formatted(sourceCitationKey, matchResult.contextText());
    }
}
