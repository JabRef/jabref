package org.jabref.gui.whatsnew;

import java.util.Optional;
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
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

import org.jspecify.annotations.Nullable;

/// The non-modal "What's new" window: the news with *Later* and, when something waits to restart JabRef,
/// *Restart to update*; without news only *Close*, as a restart would bring nothing to read about.
///
/// It opens on a fetch — "Checking remote…" with a bar, the restart disabled — and [#checked] brings the answer,
/// so nobody restarts into a version that is already stale. From then on the restart is offered exactly while
/// an update is available; after [#checkFailed] it stays disabled.
// [impl->req~whats-new.checkout-news~1]
public class WhatsNewDialog extends BaseDialog<Boolean> {

    private static final double WIDTH = 900;
    private static final double HEIGHT = 650;

    private enum Check {
        RUNNING,
        DONE,
        FAILED
    }

    private final ButtonType restart = new ButtonType(Localization.lang("Restart to update"), ButtonBar.ButtonData.APPLY);
    private final ObjectProperty<News> news;
    private final ObjectProperty<Check> check = new SimpleObjectProperty<>(Check.RUNNING);
    private final Consumer<String> openUrl;

    /// @param updateAvailable whether the checkout is behind its upstream, as of the latest look; empty when
    ///                        nothing would restart JabRef (no `just run-loop`), so no restart is offered
    public WhatsNewDialog(News news, Optional<ObservableBooleanValue> updateAvailable, Consumer<String> openUrl) {
        this.news = new SimpleObjectProperty<>(news);
        this.openUrl = openUrl;
        // JavaFX dialogs are application-modal unless told otherwise; this one must not block JabRef.
        initModality(Modality.NONE);
        ObservableValue<Boolean> hasNews = this.news.map(shown -> !shown.isEmpty());

        ButtonType later = new ButtonType(Localization.lang("Later"), ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().add(later);
        ((Button) getDialogPane().lookupButton(later)).textProperty()
                                                      .bind(hasNews.map(some -> some ? Localization.lang("Later") : Localization.lang("Close")));
        updateAvailable.ifPresent(available -> {
            getDialogPane().getButtonTypes().add(restart);
            Node restartButton = getDialogPane().lookupButton(restart);
            restartButton.visibleProperty().bind(hasNews);
            restartButton.managedProperty().bind(hasNews);
            restartButton.disableProperty().bind(check.isNotEqualTo(Check.DONE).or(Bindings.not(available)));
        });
        setResultConverter(restart::equals);

        BorderPane root = new BorderPane();
        root.centerProperty().bind(this.news.map(this::body));
        root.bottomProperty().bind(check.map(WhatsNewDialog::statusRow));
        root.setPrefSize(WIDTH, HEIGHT);
        getDialogPane().setContent(root);
    }

    /// The fetch has answered with `news`.
    public void checked(News news) {
        this.news.set(news);
        check.set(Check.DONE);
    }

    /// The upstream could not be reached: `news` is what the working tree holds, and no restart is offered.
    public void checkFailed(News news) {
        this.news.set(news);
        check.set(Check.FAILED);
    }

    /// Whether the user chose *Restart to update*; `false` before the window closes.
    public boolean restartChosen() {
        return Boolean.TRUE.equals(getResult());
    }

    private static @Nullable Node statusRow(Check check) {
        return switch (check) {
            case RUNNING ->
                    checkingRow();
            case DONE ->
                    null;
            case FAILED -> {
                Label failed = new Label(Localization.lang("Checking remote failed - no restart offered."));
                failed.setPadding(new Insets(0, 16, 8, 16));
                yield failed;
            }
        };
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
