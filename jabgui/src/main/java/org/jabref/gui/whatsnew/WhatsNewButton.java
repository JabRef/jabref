package org.jabref.gui.whatsnew;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.scene.control.Button;

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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The toolbar's "What's new" button, present only while JabRef runs out of a git checkout.
///
/// Every five minutes it fetches; once the checkout is behind its upstream the glyph turns blue and the tooltip
/// lists the changelog entries not yet seen — from the working tree and from the fetched upstream, against the
/// copy announced last (in the checkout's git directory, so nothing is missed while JabRef is closed and nothing
/// is shown twice). Clicking opens the [WhatsNewDialog] on a fresh fetch; *Restart to update* leaves a marker
/// file for `just run-loop`, which pulls, rebuilds and starts JabRef again.
// [impl->req~whats-new.checkout-news~1]
@NullMarked
public class WhatsNewButton {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsNewButton.class);
    private static final int CHECK_MINUTES = 5;

    private final CheckoutGit git;
    private final Path announcedCopy;
    private final Path restartMarker;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final Runnable quit;
    private final OpenCommand command = new OpenCommand();
    private final Button button;

    /// The blamed changelogs seen last: the working tree under `true`, the fetched upstream under `false`.
    /// Both stay in the projection, so upstream entries arriving while a local edit is pending hide nothing.
    private final Map<Boolean, WhatsNew.Source> sources = new ConcurrentHashMap<>();

    private List<WhatsNew.Item> pending = List.of();
    private Optional<String> range = Optional.empty();
    private int behind;
    private @Nullable WhatsNewDialog dialog;

    private WhatsNewButton(CheckoutGit git,
                           Path gitDir,
                           ActionFactory factory,
                           TaskExecutor taskExecutor,
                           DialogService dialogService,
                           ExternalApplicationsPreferences externalApplicationsPreferences,
                           Runnable quit) {
        this.git = git;
        this.announcedCopy = gitDir.resolve("whats-new-announced.md");
        this.restartMarker = gitDir.resolve("restart-requested");
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.quit = quit;
        this.button = factory.createIconButton(StandardActions.WHATS_NEW, command);
        BackgroundTask.wrap(() -> refreshNews(true, git.blame("", WhatsNew.ME)))
                      .onFinished(this::scheduleCheck)
                      .executeWith(taskExecutor);
    }

    /// The button for the checkout around the working directory, or empty for a packaged JabRef.
    public static Optional<Button> create(ActionFactory factory,
                                          TaskExecutor taskExecutor,
                                          DialogService dialogService,
                                          ExternalApplicationsPreferences externalApplicationsPreferences,
                                          GitHandlerRegistry gitHandlerRegistry,
                                          Runnable quit) {
        return CheckoutGit.around(Path.of(""), gitHandlerRegistry)
                          .flatMap(git -> git.gitDir().map(gitDir -> new WhatsNewButton(git, gitDir, factory, taskExecutor, dialogService, externalApplicationsPreferences, quit)))
                          .map(whatsNew -> whatsNew.button);
    }

    private void scheduleCheck() {
        BackgroundTask.wrap(this::check)
                      .onFinished(this::scheduleCheck)
                      .scheduleWith(taskExecutor, CHECK_MINUTES, TimeUnit.MINUTES);
    }

    /// One fetch: records how far behind the checkout is and re-projects the news. Any thread but FX.
    private void check() {
        int count = git.commitsBehind();
        if (count > 0) {
            refreshNews(false, git.blame(CheckoutGit.UPSTREAM, WhatsNew.ME_REMOTELY));
        } else {
            refreshNews(true, git.blame("", WhatsNew.ME));
        }
        Platform.runLater(() -> {
            behind = count;
            refreshButton();
        });
    }

    /// Records `source` as the latest changelog of its trigger and shows the entries no announced copy holds.
    /// Without a copy yet (first run) the current changelog becomes it silently. Any thread but FX.
    private void refreshNews(boolean local, Optional<WhatsNew.Source> source) {
        source.ifPresent(text -> sources.put(local, text));
        if (!Files.exists(announcedCopy)) {
            announce();
            return;
        }
        List<WhatsNew.Item> items = pendingNews();
        Optional<String> newRange = newsRange();
        Platform.runLater(() -> {
            pending = items;
            range = newRange;
            refreshButton();
        });
    }

    private List<WhatsNew.Item> pendingNews() {
        List<WhatsNew.Source> ordered = new ArrayList<>();
        Optional.ofNullable(sources.get(true)).ifPresent(ordered::add);
        Optional.ofNullable(sources.get(false)).ifPresent(ordered::add);
        return WhatsNew.pending(announcedCopy, ordered);
    }

    private void announce() {
        try {
            WhatsNew.announce(announcedCopy, new ArrayList<>(sources.values()));
        } catch (IOException e) {
            LOGGER.warn("Cannot write {}", announcedCopy, e);
        }
    }

    /// `since <running commit> — now at <upstream commit>`, or empty when both are the same commit or git cannot say.
    private Optional<String> newsRange() {
        Optional<String> head = git.describe("HEAD");
        Optional<String> upstream = git.describe(CheckoutGit.UPSTREAM);
        if (head.isEmpty() || upstream.isEmpty() || head.equals(upstream)) {
            return Optional.empty();
        }
        return Optional.of(Localization.lang("since %0 - now at %1", head.get(), upstream.get()));
    }

    private String title() {
        String count = pending.isEmpty()
                       ? Localization.lang("What's new")
                       : Localization.lang("What's new - %0 pending change(s)", String.valueOf(pending.size()));
        return range.map(r -> count + " " + r).orElse(count);
    }

    /// The tooltip's news and the glyph's colour. FX thread.
    private void refreshButton() {
        List<String> parts = new ArrayList<>();
        if (!pending.isEmpty()) {
            parts.add(title() + ":\n" + WhatsNew.plainText(pending));
        }
        if (behind > 0) {
            parts.add(Localization.lang("A new version is available (%0 commit(s)) - restart to update.", String.valueOf(behind)));
        }
        // The button's tooltip is bound to the action's description plus the command's status message.
        command.setNews(parts.isEmpty() ? "" : "\n\n" + String.join("\n\n", parts));
        button.setGraphic(behind > 0
                          ? IconTheme.JabRefIcons.WHATS_NEW.withColor(IconTheme.SELECTED_COLOR).getGraphicNode()
                          : IconTheme.JabRefIcons.WHATS_NEW.getGraphicNode());
    }

    /// The click: opens the window on a fetch and hands it what the fetch found; a second click brings the open
    /// window to the front instead of starting a second fetch. FX thread.
    private void open() {
        WhatsNewDialog openDialog = dialog;
        if (openDialog != null) {
            openDialog.getDialogPane().getScene().getWindow().requestFocus();
            return;
        }
        WhatsNewDialog newDialog = new WhatsNewDialog(title(), pending,
                url -> NativeDesktop.openBrowserShowPopup(url, dialogService, externalApplicationsPreferences));
        dialog = newDialog;
        newDialog.setOnHidden(_ -> {
            if (dialog == newDialog) {
                dialog = null;
            }
            if (newDialog.restartChosen()) {
                requestRestart();
            }
        });
        dialogService.showCustomDialog(newDialog);
        BackgroundTask.wrap(() -> {
                          check();
                          return pendingNews();
                      })
                      .onSuccess(items -> {
                          pending = items;
                          range = newsRange();
                          newDialog.checked(title(), items);
                          announceShown();
                      })
                      .onFailure(_ -> newDialog.checked(title(), pending))
                      .executeWith(taskExecutor);
    }

    /// Makes the entries on screen old: the announced copy is replaced and the tooltip drops them. FX thread.
    private void announceShown() {
        BackgroundTask.wrap(this::announce).executeWith(taskExecutor);
        pending = List.of();
        refreshButton();
    }

    /// The click, with the pending news as the status message the tooltip shows after the action's description.
    private class OpenCommand extends SimpleCommand {

        @Override
        public void execute() {
            open();
        }

        void setNews(String news) {
            statusMessage.set(news);
        }
    }

    /// Leaves a marker for `just run-loop` and quits the ordinary way, so unsaved libraries are asked about.
    private void requestRestart() {
        try {
            Files.writeString(restartMarker, "");
        } catch (IOException e) {
            LOGGER.warn("Cannot write {}", restartMarker, e);
        }
        quit.run();
    }
}
