package org.jabref.gui.whatsnew;

import java.util.Optional;
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.whatsnew.News;

import org.jspecify.annotations.Nullable;

/// The non-modal "What's new" window: the news, or a line saying there are none above the news shown before,
/// greyed; *Later*, or *Close* when there is neither news nor a restart to offer.
///
/// When something waits to restart JabRef, *Restart to update* is offered while the window checks the remote —
/// "Checking remote…" with a bar; `just loop` pulls anyway, so the answer need not be awaited — and, once
/// [#checked] brings the answer, exactly while commits landed upstream, whether or not they touched the changelog.
/// After [#checkFailed] no restart is offered.
// [impl->req~whats-new.checkout-news~1]
public class WhatsNewDialog extends BaseDialog<Boolean> {

    private static final double WIDTH = 900;
    private static final double HEIGHT = 650;
    private static final double SHOWN_BEFORE_OPACITY = 0.6;

    private enum Check {
        RUNNING,
        DONE,
        FAILED
    }

    private final ButtonType restart = new ButtonType(Localization.lang("Restart to update"), ButtonBar.ButtonData.APPLY);
    private final ObjectProperty<News> news;
    private final ObjectProperty<Check> check = new SimpleObjectProperty<>(Check.RUNNING);
    private final Consumer<String> openUrl;

    /// @param shownBefore     the news presented last, shown greyed while nothing is pending
    /// @param updateAvailable whether the checkout is behind its upstream, as of the latest look; empty when
    ///                        nothing would restart JabRef (no `just loop`), so no restart is offered
    public WhatsNewDialog(News news, ObservableValue<News> shownBefore, Optional<ObservableBooleanValue> updateAvailable, Consumer<String> openUrl) {
        this.news = new SimpleObjectProperty<>(news);
        this.openUrl = openUrl;
        // JavaFX dialogs are application-modal unless told otherwise; this one must not block JabRef.
        initModality(Modality.NONE);
        BooleanExpression restartOffered = updateAvailable.<BooleanExpression>map(available -> check.isEqualTo(Check.RUNNING)
                                                                                                    .or(check.isEqualTo(Check.DONE).and(available)))
                                                          .orElseGet(() -> new SimpleBooleanProperty(false));
        BooleanExpression laterFits = BooleanExpression.booleanExpression(this.news.map(shown -> !shown.isEmpty())).or(restartOffered);

        ButtonType later = new ButtonType(Localization.lang("Later"), ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().add(later);
        ((Button) getDialogPane().lookupButton(later)).textProperty()
                                                      .bind(Bindings.when(laterFits).then(Localization.lang("Later")).otherwise(Localization.lang("Close")));
        if (updateAvailable.isPresent()) {
            getDialogPane().getButtonTypes().add(restart);
            Node restartButton = getDialogPane().lookupButton(restart);
            restartButton.visibleProperty().bind(restartOffered);
            restartButton.managedProperty().bind(restartOffered);
        }
        setResultConverter(restart::equals);

        BorderPane root = new BorderPane();
        root.centerProperty().bind(Bindings.createObjectBinding(() -> body(this.news.get(), shownBefore.getValue()), this.news, shownBefore));
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

    /// The news; without any, a line saying so — a blank sheet reads as a rendering failure — above the news
    /// shown before, greyed.
    private Node body(News news, News shownBefore) {
        if (!news.isEmpty()) {
            return new WhatsNewView(news, openUrl);
        }
        Label nothing = new Label(Localization.lang("Nothing new since the running version."));
        nothing.setPadding(new Insets(16));
        if (shownBefore.isEmpty()) {
            return nothing;
        }
        Label heading = new Label(Localization.lang("Shown before"));
        heading.getStyleClass().addAll("h3", "bold");
        heading.setPadding(new Insets(0, 16, 0, 16));
        WhatsNewView earlier = new WhatsNewView(shownBefore, openUrl);
        earlier.setOpacity(SHOWN_BEFORE_OPACITY);
        VBox.setVgrow(earlier, Priority.ALWAYS);
        return new VBox(nothing, heading, earlier);
    }
}
