package org.jabref.gui.relatedwork;

import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.relatedwork.RelatedWorkMatchResult;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class RelatedWorkDialogView extends BaseDialog<Void> {

    private final BibDatabaseContext databaseContext;
    private final BibEntry sourceEntry;
    private final LinkedFile linkedFile;
    private final String sourceEntryCitationKey;

    @FXML private TextField entryField;
    @FXML private TextField linkedFileField;
    @FXML private TextField userNameField;
    @FXML private TextArea relatedWorkTextArea;
    @FXML private ButtonType parseButtonType;

    @Inject private DialogService dialogService;
    @Inject private CliPreferences preferences;
    @Inject private BibEntryTypesManager entryTypesManager;

    private RelatedWorkDialogViewModel viewModel;

    public RelatedWorkDialogView(BibDatabaseContext databaseContext,
                                 BibEntry sourceEntry,
                                 LinkedFile linkedPdfFile,
                                 String sourceEntryCitationKey) {
        this.databaseContext = databaseContext;
        this.sourceEntry = sourceEntry;
        this.linkedFile = linkedPdfFile;
        this.sourceEntryCitationKey = sourceEntryCitationKey;

        setTitle(Localization.lang("Insert related work text"));

        ViewLoader.view(this).load().setAsDialogPane(this);

        ControlHelper.setAction(parseButtonType, getDialogPane(), event -> viewModel.matchRelatedWork().ifPresent(this::openResultDialog));

        Button parseButton = (Button) getDialogPane().lookupButton(parseButtonType);
        parseButton.disableProperty().bind(viewModel.parseDisabledProperty());
    }

    @FXML
    private void initialize() {
        this.viewModel = new RelatedWorkDialogViewModel(
                databaseContext,
                sourceEntry,
                linkedFile,
                sourceEntryCitationKey,
                dialogService,
                preferences,
                entryTypesManager
        );

        this.entryField.textProperty().bind(viewModel.sourceEntryCitationKeyProperty());
        this.linkedFileField.textProperty().bind(viewModel.linkedPDFFileProperty());
        this.userNameField.textProperty().bind(viewModel.userNameProperty());
        this.relatedWorkTextArea.textProperty().bindBidirectional(viewModel.relatedWorkTextProperty());

        // [impl->req~textinput.clipboard.autofocus~1]
        String clipboardText = ClipBoardManager.getContents().trim();
        if (!StringUtil.isBlank(clipboardText)) {
            relatedWorkTextArea.setText(clipboardText);
            relatedWorkTextArea.selectAll();
        }

        Platform.runLater(relatedWorkTextArea::requestFocus);
    }

    private void openResultDialog(List<RelatedWorkMatchResult> matchedResults) {
        dialogService.showCustomDialogAndWait(new RelatedWorkResultDialogView(sourceEntry, matchedResults, viewModel.userNameProperty().get()));
    }
}
