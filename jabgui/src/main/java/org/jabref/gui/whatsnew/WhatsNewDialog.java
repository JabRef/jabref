package org.jabref.gui.whatsnew;

import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.whatsnew.News;

/// The non-modal "What's new" window: the news, *Later* and *Restart to update*.
///
/// It opens on a fetch — "Checking remote…" with a bar, the restart disabled — and [#checked] brings the answer,
/// so nobody restarts into a version that is already stale. From then on the restart is offered exactly while
/// an update is available; after [#checkFailed] it stays disabled.
// [impl->req~whats-new.checkout-news~1]
public class WhatsNewDialog extends BaseDialog<Boolean> {

    private static final double WIDTH = 900;
    private static final double HEIGHT = 650;

    private final BorderPane root = new BorderPane();
    private final ButtonType restart = new ButtonType(Localization.lang("Restart to update"), ButtonBar.ButtonData.APPLY);
    private final ObservableBooleanValue updateAvailable;
    private final Consumer<String> openUrl;

    /// @param updateAvailable whether the checkout is behind its upstream, as of the latest look
    public WhatsNewDialog(News news, ObservableBooleanValue updateAvailable, Consumer<String> openUrl) {
        this.updateAvailable = updateAvailable;
        this.openUrl = openUrl;
        // JavaFX dialogs are application-modal unless told otherwise; this one must not block JabRef.
        initModality(Modality.NONE);
        ButtonType later = new ButtonType(Localization.lang("Later"), ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(later, restart);
        setResultConverter(restart::equals);

        root.setCenter(body(news));
        root.setBottom(checkingRow());
        root.setPrefSize(WIDTH, HEIGHT);
        getDialogPane().setContent(root);
        getDialogPane().lookupButton(restart).setDisable(true);
    }

    /// The fetch has answered: `news` replaces the body, and *Restart to update* follows the update availability.
    public void checked(News news) {
        root.setCenter(body(news));
        root.setBottom(null);
        getDialogPane().lookupButton(restart).disableProperty().bind(Bindings.not(updateAvailable));
    }

    /// The upstream could not be reached: `news` is what is known so far, and no restart is offered.
    public void checkFailed(News news) {
        root.setCenter(body(news));
        Label failed = new Label(Localization.lang("Checking remote failed - no restart offered."));
        failed.setPadding(new Insets(0, 16, 8, 16));
        root.setBottom(failed);
    }

    /// Whether the user chose *Restart to update*; `false` before the window closes.
    public boolean restartChosen() {
        return Boolean.TRUE.equals(getResult());
    }

    private static Node checkingRow() {
        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(120);
        HBox row = new HBox(8, bar, new Label(Localization.lang("Checking remote...")));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 16, 8, 16));
        return row;
    }

    /// The news, or a line saying that there are none — a blank sheet reads as a rendering failure.
    private Node body(News news) {
        if (news.isEmpty()) {
            Label nothing = new Label(Localization.lang("Nothing new since the running version."));
            nothing.setPadding(new Insets(16));
            return nothing;
        }
        return new WhatsNewView(news, openUrl);
    }
}
