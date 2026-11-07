package org.jabref.gui.preferences.websearch;

import java.util.function.Consumer;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.jabref.gui.preferences.websearch.WebSearchTabViewModel.FetcherViewModel;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class ApiKeyDialogTest extends JavaFxTest {
    private ApiKeyDialog dialog;
    private Consumer<Boolean> resultHandler;

    @Override
    public void start(Stage stage) {
        Localization.setLanguage(Language.ENGLISH);
        WebSearchTabViewModel viewModel = mock(WebSearchTabViewModel.class);
        FetcherViewModel fetcher = mock(FetcherViewModel.class);
        when(fetcher.getName()).thenReturn("Fetcher");
        when(fetcher.getApiKey()).thenReturn("");
        when(viewModel.getApikeyPersistProperty()).thenReturn(new SimpleBooleanProperty());
        when(viewModel.apiKeyPersistAvailable()).thenReturn(new SimpleBooleanProperty());
        doAnswer(invocation -> {
            resultHandler = invocation.getArgument(2);
            return null;
        }).when(viewModel).checkApiKey(eq(fetcher), anyString(), any());
        dialog = new ApiKeyDialog(viewModel, fetcher);
        dialog.initOwner(stage);
    }

    // [utest->req~ux.preferences.api-key-test-result~1]
    @Test
    void successfulResultDoesNotValidateAnEditedKey() {
        interact(() -> {
            keyField().setText("first key");
            assertTrue(okButton().isDisabled());
            testButton().fire();
            resultHandler.accept(true);
            keyField().setText("different key");
        });
        awaitEvents();

        interact(() -> {
            assertTrue(okButton().isDisabled());
            assertFalse(testButton().isDisabled());
            assertEquals("Test connection", testButton().getText());
        });
    }

    // [utest->req~ux.preferences.api-key-test-result~1]
    @Test
    void clearingTheKeyRemainsAllowedAfterAnOutdatedFailure() {
        interact(() -> {
            keyField().setText("first key");
            testButton().fire();
            resultHandler.accept(false);
            keyField().clear();
        });
        awaitEvents();

        interact(() -> {
            assertFalse(okButton().isDisabled());
            assertFalse(testButton().isDisabled());
            assertEquals("Test connection", testButton().getText());
        });
    }

    @Test
    void successfulResultValidatesTheUnchangedKey() {
        interact(() -> {
            keyField().setText("first key");
            testButton().fire();
            resultHandler.accept(true);
        });
        awaitEvents();

        interact(() -> {
            assertFalse(okButton().isDisabled());
            assertFalse(testButton().isDisabled());
            assertEquals("API Key Valid", testButton().getText());
        });
    }

    private TextField keyField() {
        return (TextField) dialog.getDialogPane().lookup("#apiKeyField");
    }

    private Button testButton() {
        return (Button) dialog.getDialogPane().lookup("#testButton");
    }

    private Button okButton() {
        return (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
    }
}
