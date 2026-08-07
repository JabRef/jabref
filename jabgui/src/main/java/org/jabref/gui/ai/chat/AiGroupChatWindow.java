package org.jabref.gui.ai.chat;

import java.nio.file.Path;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;

import org.jabref.gui.groups.GroupNodeViewModel;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;

public class AiGroupChatWindow extends BaseDialog<Void> {
    @FXML private AiGroupChatView chatView;

    public AiGroupChatWindow() {
        super();

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.titleProperty().bind(Bindings.createObjectBinding(
                this::makeWindowTitle,
                chatView.databaseContextProperty(),
                chatView.groupNodeProperty()
        ));
    }

    private String makeWindowTitle() {
        BibDatabaseContext context = chatView.databaseContextProperty().get();
        GroupNodeViewModel group = chatView.groupNodeProperty().get();

        if (context == null || group == null) {
            return "";
        }

        String groupName = group.getGroupNode().getGroup().getName();
        String libraryName = context.getDatabasePath()
                                    .map(Path::getFileName)
                                    .map(Path::toString)
                                    .orElse(Localization.lang("Untitled"));

        // [impl->req~ai.chat.groups.display-names~1]
        return Localization.lang("%0 — %1", groupName, libraryName);
    }

    public ObjectProperty<GroupNodeViewModel> groupNodeProperty() {
        return chatView.groupNodeProperty();
    }

    public ObjectProperty<BibDatabaseContext> databaseContextProperty() {
        return chatView.databaseContextProperty();
    }
}
