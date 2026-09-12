package org.jabref.gui.whatsnew;

import java.nio.file.Path;
import java.util.Optional;

import javafx.scene.control.Button;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.whatsnew.Checkout;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// The toolbar's "What's new" button, present only while JabRef runs out of a git checkout: its glyph turns
/// blue once the checkout is behind its upstream, its tooltip lists the news, and a click opens the
/// [WhatsNewDialog] on a fresh fetch. Binds the button to the [WhatsNewViewModel]; holds no state of its own
/// beyond the window that is open.
// [impl->req~whats-new.checkout-news~1]
@NullMarked
public final class WhatsNewButton {

    private final WhatsNewViewModel viewModel;
    private final DialogService dialogService;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final Button button;
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
        viewModel.updateAvailableProperty().subscribe(this::showGlyph);
    }

    /// The button for the checkout around the working directory, watching it from now on; empty for a packaged
    /// JabRef, which has nothing to update from.
    ///
    /// @param quit closes JabRef the ordinary way, for *Restart to update*
    public static Optional<Button> create(ActionFactory factory,
                                          TaskExecutor taskExecutor,
                                          DialogService dialogService,
                                          ExternalApplicationsPreferences externalApplicationsPreferences,
                                          GitHandlerRegistry gitHandlerRegistry,
                                          Runnable quit) {
        Optional<WhatsNewViewModel> viewModel = Checkout.around(Path.of(""), gitHandlerRegistry)
                                                        .flatMap(checkout -> checkout.gitDir().map(gitDir -> new WhatsNewViewModel(checkout, gitDir, taskExecutor, quit)));
        viewModel.ifPresent(WhatsNewViewModel::startWatching);
        return viewModel.map(model -> new WhatsNewButton(model, factory, dialogService, externalApplicationsPreferences).button);
    }

    private void showGlyph(boolean updateAvailable) {
        IconTheme.JabRefIcons icon = IconTheme.JabRefIcons.WHATS_NEW;
        button.setGraphic(updateAvailable ? icon.withColor(IconTheme.SELECTED_COLOR).getGraphicNode() : icon.getGraphicNode());
    }

    /// Opens the window on a fetch; a second click brings the open window to the front instead.
    private void open() {
        if (openDialog != null) {
            openDialog.getDialogPane().getScene().getWindow().requestFocus();
            return;
        }
        WhatsNewDialog dialog = new WhatsNewDialog(viewModel.getPending(),
                url -> NativeDesktop.openBrowserShowPopup(url, dialogService, externalApplicationsPreferences));
        dialog.titleProperty().bind(viewModel.titleProperty());
        dialog.setOnHidden(_ -> {
            openDialog = null;
            if (dialog.restartChosen()) {
                viewModel.requestRestart();
            }
        });
        openDialog = dialog;
        dialogService.showCustomDialog(dialog);
        viewModel.present(dialog::checked);
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
