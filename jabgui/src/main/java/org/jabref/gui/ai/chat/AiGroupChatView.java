package org.jabref.gui.ai.chat;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

import org.jabref.gui.ai.AiPrivacyNoticeView;
import org.jabref.gui.groups.GroupNodeViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

// [impl->feat~ai.chatting.groups~1]
public class AiGroupChatView extends StackPane {
    @FXML private AiPrivacyNoticeView privacyNotice;
    @FXML private AiChatView aiChatView;

    @Inject private GuiPreferences preferences;
    @Inject private AiService aiService;

    private AiGroupChatViewModel viewModel;

    public AiGroupChatView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new AiGroupChatViewModel(preferences.getAiPreferences(), aiService);

        setupBindings();
    }

    private void setupBindings() {
        // [pp->feat~ai.chatting.groups~1]
        privacyNotice.managedProperty().bind(privacyNotice.visibleProperty());
        aiChatView.managedProperty().bind(aiChatView.visibleProperty());

        privacyNotice.visibleProperty().bind(viewModel.enabledProperty().not());
        aiChatView.visibleProperty().bind(viewModel.enabledProperty());

        aiChatView.chatHistoryProperty().bind(viewModel.chatHistoryProperty());
        aiChatView.entriesProperty().bind(viewModel.entriesProperty());
    }

    public ObjectProperty<GroupNodeViewModel> groupNodeProperty() {
        return viewModel.groupNodeProperty();
    }

    public ObjectProperty<BibDatabaseContext> databaseContextProperty() {
        return viewModel.databaseContextProperty();
    }
}
