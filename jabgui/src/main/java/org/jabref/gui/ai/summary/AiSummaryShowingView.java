package org.jabref.gui.ai.summary;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.gui.util.component.MarkdownTextFlow;
import org.jabref.logic.ai.AiNamingUtils;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiSummaryShowingView extends VBox {
    @FXML private CheckBox markdownCheckbox;
    @FXML private Text summaryInfoText;

    private StackPane summaryContentPane;
    private MarkdownTextFlow markdownTextFlow;

    private AiSummaryShowingViewModel viewModel;

    /// Monotonically increasing stamp identifying the most recently scheduled content update.
    ///
    /// Each [#updateContent()] call increments this counter and captures its own value before
    /// scheduling the UI mutation on the JavaFX thread. When the scheduled callback runs, it compares
    /// the captured value against the current counter and skips the mutation if a newer update has been
    /// scheduled in the meantime, preventing a stale summary from overwriting a newer one.
    private int updateVersion;

    @Inject private GuiPreferences preferences;
    @Inject private DialogService dialogService;
    @Inject private BibEntryTypesManager entryTypesManager;

    public AiSummaryShowingView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    private static String formatSummaryInfo(AiSummary summary) {
        if (summary == null) {
            return "";
        }

        return Localization.lang("Generated at %0 by %1 (algorithm %2)")
                           .replaceAll("%0", formatTimestamp(summary.metadata().timestamp()))
                           .replaceAll("%1", AiNamingUtils.getDisplayName(summary.metadata().aiProvider()) + " " + summary.metadata().model())
                           .replaceAll("%2", AiNamingUtils.getDisplayName(summary.summarizationAlgorithm()));
    }

    private static String formatTimestamp(Instant timestamp) {
        return timestamp
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                         .withLocale(Locale.getDefault()));
    }

    public ObjectProperty<AiSummary> summaryProperty() {
        return viewModel.summaryProperty();
    }

    public ObjectProperty<FullBibEntry> entryProperty() {
        return viewModel.entryProperty();
    }

    @FXML
    private void initialize() {
        viewModel = new AiSummaryShowingViewModel(
                preferences.getAiPreferences(),
                preferences.getFieldPreferences(),
                entryTypesManager,
                dialogService
        );
        initializeMarkdownTextFlow();

        setupBindings();
        setupListeners();
    }

    private void initializeMarkdownTextFlow() {
        summaryContentPane = new StackPane();
        markdownTextFlow = new MarkdownTextFlow(summaryContentPane);
        summaryContentPane.getChildren().add(markdownTextFlow);

        ScrollPane scrollPane = new ScrollPane(summaryContentPane);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addFirst(scrollPane);
    }

    private void setupBindings() {
        viewModel.isMarkdownProperty().bindBidirectional(markdownCheckbox.selectedProperty());

        summaryInfoText.textProperty().bind(viewModel.summaryProperty().map(AiSummaryShowingView::formatSummaryInfo));
    }

    private void setupListeners() {
        BindingsHelper.listen(viewModel.summaryProperty(), _ -> updateContent());
        BindingsHelper.listen(viewModel.isMarkdownProperty(), _ -> updateContent());
    }

    private void updateContent() {
        int requestVersion = ++updateVersion;
        UiTaskExecutor.runInJavaFXThread(() -> {
            if (requestVersion != updateVersion) {
                return;
            }
            String content = StringUtil.makeSafe(viewModel.summaryProperty().map(AiSummary::content).getValue());
            if (viewModel.isMarkdownProperty().get()) {
                markdownTextFlow.setMarkdown(content);
            } else {
                markdownTextFlow.setPlainText(content);
            }
        });
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRegenerateProperty() {
        return viewModel.onRegenerateProperty();
    }

    public EventHandler<ActionEvent> getOnRegenerate() {
        return viewModel.onRegenerateProperty().get();
    }

    public void setOnRegenerate(EventHandler<ActionEvent> onRegenerate) {
        viewModel.onRegenerateProperty().set(onRegenerate);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRegenerateCustomProperty() {
        return viewModel.onRegenerateCustomProperty();
    }

    public EventHandler<ActionEvent> getOnRegenerateCustom() {
        return viewModel.onRegenerateCustomProperty().get();
    }

    public void setOnRegenerateCustom(EventHandler<ActionEvent> onRegenerateCustom) {
        viewModel.onRegenerateCustomProperty().set(onRegenerateCustom);
    }

    @FXML
    private void regenerate() {
        viewModel.regenerate();
    }

    @FXML
    private void regenerateCustom() {
        viewModel.regenerateCustom();
    }

    // [impl->req~ai.summarization.general.export~1]
    @FXML
    private void exportMarkdown() {
        viewModel.exportMarkdown();
    }

    // [impl->req~ai.summarization.general.export~1]
    @FXML
    private void exportJson() {
        viewModel.exportJson();
    }
}
