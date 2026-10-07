package org.jabref.gui.ai.summary;

import java.util.Optional;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.util.ChatModelFactory;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.InMemorySummaryCache;
import org.jabref.logic.ai.summarization.SummarizationTaskAggregator;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@NullMarked
class AiSummaryParametersDialogTest extends JavaFxTest {

    private AiPreferences aiPreferences;
    private DialogService dialogService;
    private SummarizationTaskAggregator aggregator;
    private FullBibEntry fullEntry;

    @BeforeEach
    void setUp() {
        Localization.setLanguage(Language.ENGLISH);
        aiPreferences = AiPreferences.getDefault();
        GuiPreferences guiPreferences = mock(GuiPreferences.class);
        when(guiPreferences.getAiPreferences()).thenReturn(aiPreferences);
        Injector.setModelOrService(GuiPreferences.class, guiPreferences);

        dialogService = mock(DialogService.class);
        aggregator = new SummarizationTaskAggregator(
                mock(TaskExecutor.class),
                new InMemorySummaryCache(mock(SummariesRepository.class)));
        fullEntry = new FullBibEntry(new BibDatabaseContext(), new BibEntry());
    }

    @AfterEach
    void tearDown() {
        Injector.forgetAll();
    }

    @Test
    void generateButtonReturnsAcceptedResult() {
        interact(() -> {
            AiSummaryParametersDialog dialog = new AiSummaryParametersDialog();

            assertEquals(true, dialog.getResultConverter().call(button(dialog, ButtonBar.ButtonData.OK_DONE)));
            assertEquals(false, dialog.getResultConverter().call(button(dialog, ButtonBar.ButtonData.CANCEL_CLOSE)));
        });
    }

    @Test
    void generateStartsRegenerationWithSelectedAlgorithm() {
        when(dialogService.showCustomDialogAndWait(any(AiSummaryParametersDialog.class)))
                .thenAnswer(invocation -> {
                    AiSummaryParametersDialog dialog = invocation.getArgument(0);
                    ComboBox<?> combo = JavaFxExtension.lookup(dialog.getDialogPane(), "#summarizatorCombo", ComboBox.class);
                    combo.getSelectionModel().select(combo.getItems().indexOf(SummarizatorKind.FULL_DOCUMENT));
                    return Optional.of(dialog.getResultConverter().call(button(dialog, ButtonBar.ButtonData.OK_DONE)));
                });

        interact(() -> {
            try (MockedStatic<ChatModelFactory> chatModelFactory = mockStatic(ChatModelFactory.class)) {
                chatModelFactory.when(() -> ChatModelFactory.create(aiPreferences)).thenReturn(mock(ChatModel.class));
                AiSummaryViewModel viewModel = viewModel();
                viewModel.setEntry(fullEntry);
                viewModel.regenerateCustom();

                assertEquals(SummarizatorKind.FULL_DOCUMENT,
                        aggregator.getTask(fullEntry.entry()).orElseThrow().getRequest().summarizator().getKind());
            }
        });
    }

    @Test
    void cancelDoesNotStartRegeneration() {
        when(dialogService.showCustomDialogAndWait(any(AiSummaryParametersDialog.class)))
                .thenAnswer(invocation -> {
                    AiSummaryParametersDialog dialog = invocation.getArgument(0);
                    return Optional.of(dialog.getResultConverter().call(button(dialog, ButtonBar.ButtonData.CANCEL_CLOSE)));
                });

        interact(() -> {
            AiSummaryViewModel viewModel = viewModel();
            viewModel.setEntry(fullEntry);
            viewModel.regenerateCustom();

            assertEquals(Optional.empty(), aggregator.getTask(fullEntry.entry()));
        });
    }

    private AiSummaryViewModel viewModel() {
        return new AiSummaryViewModel(
                aiPreferences,
                mock(FilePreferences.class),
                mock(SummariesRepository.class),
                new InMemorySummaryCache(mock(SummariesRepository.class)),
                aggregator,
                dialogService);
    }

    private ButtonType button(AiSummaryParametersDialog dialog, ButtonBar.ButtonData buttonData) {
        return dialog.getDialogPane().getButtonTypes().stream()
                     .filter(button -> button.getButtonData() == buttonData)
                     .findFirst()
                     .orElseThrow();
    }
}
