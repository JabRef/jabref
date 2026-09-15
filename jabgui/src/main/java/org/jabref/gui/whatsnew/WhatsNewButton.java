package org.jabref.gui.whatsnew;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.whatsnew.AnnouncedEntries;
import org.jabref.logic.whatsnew.Checkout;
import org.jabref.logic.whatsnew.CheckoutNews;
import org.jabref.logic.whatsnew.PullRequestAuthors;
import org.jabref.logic.whatsnew.RestartMarker;

import org.jspecify.annotations.Nullable;

/// The toolbar's "What's new" button, present only while JabRef runs out of a git checkout, i.e. started by the
/// Gradle `run` task, which names the checkout in [#CHECKOUT_PROPERTY]: its glyph turns
/// blue once the checkout is behind its upstream, a badge on it counts the pending entries, its tooltip lists the news, and a click opens the
/// [WhatsNewDialog] on a fresh fetch. Binds the button to the [WhatsNewViewModel]; holds no state of its own
/// beyond the window that is open.
// [impl->req~whats-new.checkout-news~1]
public final class WhatsNewButton {

    /// The source checkout JabRef was started from; set by `jabgui/build.gradle.kts` for the `run` task only.
    static final String CHECKOUT_PROPERTY = "jabref.checkout";

    /// Whether `just run-loop` waits to pull, rebuild and start JabRef again; only then is a restart offered.
    static final String RESTART_LOOP_PROPERTY = "jabref.restart.loop";

    private final WhatsNewViewModel viewModel;
    private final DialogService dialogService;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final Button button;
    private final Label badge = new Label();
    private @Nullable WhatsNewDialog openDialog;

    private WhatsNewButton(WhatsNewViewModel viewModel,
                           ActionFactory factory,
                           DialogService dialogService,
                           ExternalApplicationsPreferences externalApplicationsPreferences) {
        this.viewModel = viewModel;
        this.dialogService = dialogService;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.button = factory.createIconButton(StandardActions.WHATS_NEW, new OpenCommand());
        // The button's tooltip is the action's description plus the command's status message.
        badge.getStyleClass().add("whats-new-badge");
        badge.textProperty().bind(viewModel.pendingCountProperty().asString());
        badge.visibleProperty().bind(viewModel.pendingCountProperty().greaterThan(0));
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        viewModel.updateAvailableProperty().subscribe(this::showGlyph);
    }

    /// The button for the checkout JabRef was started from, watching it from now on; empty for a packaged JabRef
    /// or an IDE run, which have nothing `just run-loop` could update.
    ///
    /// @param quit closes JabRef the ordinary way, for *Restart to update*; `false` when the user keeps it open
    public static Optional<Button> create(ActionFactory factory,
                                          TaskExecutor taskExecutor,
                                          DialogService dialogService,
                                          ExternalApplicationsPreferences externalApplicationsPreferences,
                                          GitHandlerRegistry gitHandlerRegistry,
                                          BooleanSupplier quit) {
        Optional<WhatsNewViewModel> viewModel = Optional.ofNullable(System.getProperty(CHECKOUT_PROPERTY))
                                                        .flatMap(checkout -> Checkout.around(Path.of(checkout), gitHandlerRegistry))
                                                        .flatMap(checkout -> checkout.gitDir().map(gitDir -> new WhatsNewViewModel(
                                                                new CheckoutNews(checkout, AnnouncedEntries.inGitDir(gitDir), PullRequestAuthors.forCheckout(checkout, gitDir)),
                                                                RestartMarker.inGitDir(gitDir),
                                                                taskExecutor,
                                                                quit)));
        viewModel.ifPresent(WhatsNewViewModel::startWatching);
        return viewModel.map(model -> new WhatsNewButton(model, factory, dialogService, externalApplicationsPreferences).button);
    }

    private void showGlyph(boolean updateAvailable) {
        IconTheme.JabRefIcons icon = IconTheme.JabRefIcons.WHATS_NEW;
        Node glyph = updateAvailable ? icon.withColor(IconTheme.SELECTED_COLOR).getGraphicNode() : icon.getGraphicNode();
        button.setGraphic(new StackPane(glyph, badge));
    }

    /// Opens the window on a fetch; a second click brings the open window to the front instead.
    private void open() {
        if (openDialog != null) {
            openDialog.getDialogPane().getScene().getWindow().requestFocus();
            return;
        }
        WhatsNewDialog dialog = new WhatsNewDialog(viewModel.getPending(),
                viewModel.shownBeforeProperty(),
                Boolean.getBoolean(RESTART_LOOP_PROPERTY) ? Optional.of(viewModel.updateAvailableProperty()) : Optional.empty(),
                url -> NativeDesktop.openBrowserShowPopup(url, dialogService, externalApplicationsPreferences));
        dialog.titleProperty().bind(viewModel.titleProperty());
        openDialog = dialog;
        dialogService.showCustomDialog(dialog);
        BackgroundTask<?> presentation = viewModel.present(() -> openDialog == dialog, dialog::checked, dialog::checkFailed);
        dialog.setOnHidden(_ -> {
            // A window closed before the answer: the news in it stay unseen, and the window is not touched again.
            openDialog = null;
            presentation.cancel();
            if (dialog.restartChosen()) {
                WhatsNewViewModel.RestartRequest request = viewModel.requestRestart();
                if (request == WhatsNewViewModel.RestartRequest.MARKER_NOT_WRITTEN) {
                    dialogService.notify(Localization.lang("Cannot request the restart (see the log) - JabRef keeps running."));
                } else if (request == WhatsNewViewModel.RestartRequest.MARKER_NOT_WITHDRAWN) {
                    dialogService.notify(Localization.lang("Cannot withdraw the restart request (see the log) - the next quit restarts JabRef."));
                }
            }
        });
    }

    /// The click; the status message becomes the tooltip's second part.
    private final class OpenCommand extends SimpleCommand {

        private OpenCommand() {
            statusMessage.bind(viewModel.tooltipProperty());
        }

        @Override
        public void execute() {
            open();
        }
    }
}
