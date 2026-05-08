package org.jabref.gui.ai.summary;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.AiPrivacyNoticeView;
import org.jabref.gui.ai.statuspane.UniversalStatusPaneView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.ExceptionsUtil;
import org.jabref.logic.ai.AiNamingUtils;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiSummaryView extends StackPane {
    @FXML private AiPrivacyNoticeView privacyNotice;

    @FXML private UniversalStatusPaneView processingPane;
    @FXML private UniversalStatusPaneView errorPane;
    @FXML private UniversalStatusPaneView cancelledPane;

    @FXML private UniversalStatusPaneView noFilesPane;
    @FXML private UniversalStatusPaneView noSupportedFileTypesPane;

    @FXML private AiSummaryShowingView summaryShowing;

    @Inject private GuiPreferences preferences;
    @Inject private AiService aiService;
    @Inject private DialogService dialogService;
    @Inject private BibEntryTypesManager entryTypesManager;

    private AiSummaryViewModel viewModel;

    public AiSummaryView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new AiSummaryViewModel(
                preferences.getAiPreferences(),
                preferences.getFilePreferences(),
                aiService.getSummariesRepository(),
                aiService.getSummaryCache(),
                aiService.getSummarizationTaskAggregator(),
                dialogService
        );

        setupBindings();
    }

    private void setupBindings() {
        errorPane.textAreaContentProperty().bind(viewModel.errorProperty().map(ExceptionsUtil::generateExceptionMessage));

        summaryShowing.summaryProperty().bind(viewModel.summaryProperty());
        summaryShowing.entryProperty().bind(viewModel.entryProperty());

        processingPane.descriptionProperty().bind(Bindings.createObjectBinding(
                this::generateDescription,
                viewModel.summarizatorProperty(), viewModel.chatModelProperty()
        ));

        privacyNotice.managedProperty().bind(privacyNotice.visibleProperty());
        processingPane.managedProperty().bind(processingPane.visibleProperty());
        errorPane.managedProperty().bind(errorPane.visibleProperty());
        cancelledPane.managedProperty().bind(cancelledPane.visibleProperty());
        noFilesPane.managedProperty().bind(noFilesPane.visibleProperty());
        noSupportedFileTypesPane.managedProperty().bind(noSupportedFileTypesPane.visibleProperty());
        summaryShowing.managedProperty().bind(summaryShowing.visibleProperty());

        // [pp->feat~ai.summarization.entries~1]
        privacyNotice.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiSummaryViewModel.State.AI_TURNED_OFF));
        processingPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiSummaryViewModel.State.PROCESSING));
        errorPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiSummaryViewModel.State.ERROR_WHILE_GENERATING));
        cancelledPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiSummaryViewModel.State.CANCELLED));
        noFilesPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiSummaryViewModel.State.NO_FILES));
        noSupportedFileTypesPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiSummaryViewModel.State.NO_SUPPORTED_FILE_TYPES));
        summaryShowing.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiSummaryViewModel.State.DONE));
    }

    private String generateDescription() {
        Summarizator summarizator = viewModel.summarizatorProperty().get();
        ChatModel chatModel = viewModel.chatModelProperty().get();

        if (summarizator == null || chatModel == null) {
            return "";
        }

        return Localization.lang(
                "Your entry is being summarized by %0 %1 using algorithm %2",
                AiNamingUtils.getDisplayName(chatModel.getAiProvider()),
                chatModel.getName(),
                AiNamingUtils.getDisplayName(summarizator.getKind())
        );
    }

    public ObjectProperty<FullBibEntry> entryProperty() {
        return viewModel.entryProperty();
    }

    @FXML
    private void regenerate() {
        viewModel.regenerate();
    }

    @FXML
    private void regenerateCustom() {
        viewModel.regenerateCustom();
    }

    @FXML
    private void cancel() {
        viewModel.cancel();
    }
}
