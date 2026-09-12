package org.jabref.gui.whatsnew;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NullMarked;

/// The non-modal "What's new" window: the pending changelog entries, *Later* and *Restart to update*.
/// It opens on a fetch — "Checking remote …" with a bar, the restart disabled — and [#checked] brings the answer,
/// so nobody restarts into a version that is already stale.
// [impl->req~whats-new.checkout-news~1]
@NullMarked
public class WhatsNewDialog extends BaseDialog<Boolean> {

    private final BorderPane root = new BorderPane();
    private final HBox checking;
    private final ButtonType restart;
    private final Consumer<String> openUrl;

    public WhatsNewDialog(String title, List<WhatsNew.Item> items, Consumer<String> openUrl) {
        this.openUrl = openUrl;
        setTitle(title);
        ButtonType later = new ButtonType(Localization.lang("Later"), ButtonBar.ButtonData.CANCEL_CLOSE);
        restart = new ButtonType(Localization.lang("Restart to update"), ButtonBar.ButtonData.APPLY);
        getDialogPane().getButtonTypes().addAll(later, restart);
        setResultConverter(button -> button == restart);

        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(120);
        checking = new HBox(8, bar, new Label(Localization.lang("Checking remote...")));
        checking.setAlignment(Pos.CENTER_LEFT);
        checking.setPadding(new Insets(0, 16, 8, 16));
        root.setCenter(body(items));
        root.setBottom(checking);
        root.setPrefSize(900, 650);
        getDialogPane().setContent(root);
        getDialogPane().lookupButton(restart).setDisable(true);
    }

    /// The fetch has answered: the freshly projected entries replace the body and *Restart to update* goes live.
    public void checked(String title, List<WhatsNew.Item> items) {
        setTitle(title);
        root.setCenter(body(items));
        root.setBottom(null);
        getDialogPane().lookupButton(restart).setDisable(false);
    }

    /// The projected entries, or a line saying that there are none — a blank sheet reads as a rendering failure.
    private Node body(List<WhatsNew.Item> items) {
        if (items.isEmpty()) {
            Label empty = new Label(Localization.lang("Nothing new since the running version."));
            empty.setPadding(new Insets(16));
            return empty;
        }
        return WhatsNew.view(items, openUrl);
    }

    /// Whether the user chose *Restart to update*.
    public boolean restartChosen() {
        return Optional.ofNullable(getResult()).orElse(false);
    }
}
