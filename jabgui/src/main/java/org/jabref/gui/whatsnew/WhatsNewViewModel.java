package org.jabref.gui.whatsnew;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.whatsnew.AnnouncedEntries;
import org.jabref.logic.whatsnew.BlamedChangelog;
import org.jabref.logic.whatsnew.ChangelogEntry;
import org.jabref.logic.whatsnew.Checkout;
import org.jabref.logic.whatsnew.News;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The state behind the "What's new" button: the news not announced yet, how far the checkout is behind its
/// upstream, and the title both the button and the window show.
///
/// A look at the checkout runs in the background and lands here on the FX thread; every property is read and
/// written on the FX thread only.
// [impl->req~whats-new.checkout-news~1]
public class WhatsNewViewModel extends AbstractViewModel {

    /// The file `just run-loop` looks for after JabRef quits: present, it pulls, rebuilds and starts JabRef again.
    static final String RESTART_MARKER = "restart-requested";
    static final String ANNOUNCED_FILE = "whats-new-announced.tsv";

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsNewViewModel.class);
    private static final Duration CHECK_INTERVAL = Duration.ofMinutes(5);

    private final Checkout checkout;
    private final AnnouncedEntries announced;
    private final Path restartMarker;
    private final TaskExecutor taskExecutor;
    private final Runnable quit;

    private final ObjectProperty<News> pending = new SimpleObjectProperty<>(News.NONE);
    private final IntegerProperty commitsBehind = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty(Localization.lang("What's new"));
    private final BooleanBinding updateAvailable = commitsBehind.greaterThan(0);
    private final StringBinding tooltip = Bindings.createStringBinding(this::tooltipText, pending, commitsBehind, title);

    /// Counts the looks that started running; a look whose answer arrives after a later look started is dropped,
    /// so a slow scheduled look never replaces what a click just found. FX thread.
    private long looksStarted;

    /// What one look at the checkout found; `fetched` is false when the upstream could not be reached.
    private record Look(boolean fetched, int commitsBehind, String title, News news) {
    }

    /// @param gitDir the checkout's git directory, where the announced entries and the restart marker live
    /// @param quit   closes JabRef the ordinary way, so unsaved libraries are asked about
    public WhatsNewViewModel(Checkout checkout, Path gitDir, TaskExecutor taskExecutor, Runnable quit) {
        this.checkout = checkout;
        this.announced = new AnnouncedEntries(gitDir.resolve(ANNOUNCED_FILE));
        this.restartMarker = gitDir.resolve(RESTART_MARKER);
        this.taskExecutor = taskExecutor;
        this.quit = quit;
    }

    public ReadOnlyStringProperty titleProperty() {
        return title;
    }

    /// The news not announced yet, as of the last look.
    public News getPending() {
        return pending.get();
    }

    /// Whether the checkout is behind its upstream, i.e. a restart would bring a newer JabRef.
    public ObservableBooleanValue updateAvailableProperty() {
        return updateAvailable;
    }

    /// The tooltip after the action's description: the pending news, then the update offer; empty without either.
    public ObservableStringValue tooltipProperty() {
        return tooltip;
    }

    /// One look now without fetching (`just run-loop` has just pulled), then a fetch every five minutes.
    public void startWatching() {
        startLook(false, false, this::show)
                .onFinished(this::scheduleNextLook)
                .executeWith(taskExecutor);
    }

    /// A look on demand: fetches, hands the news found to `onChecked` and makes them old — the tooltip drops
    /// them and the announced entries take them. When the upstream cannot be reached or the look fails,
    /// `onFailed` gets the news known so far instead, and nothing is made old.
    public void present(Consumer<News> onChecked, Consumer<News> onFailed) {
        startLook(true, true, look -> {
            show(look);
            if (look.fetched()) {
                onChecked.accept(look.news());
                pending.set(News.NONE);
            } else {
                onFailed.accept(look.news());
            }
        })
                .onFailure(e -> {
                    LOGGER.warn("Cannot look at the checkout", e);
                    onFailed.accept(pending.get());
                })
                .executeWith(taskExecutor);
    }

    /// Leaves the marker for `just run-loop` and quits; `false` when the marker cannot be written, in which
    /// case JabRef keeps running.
    public boolean requestRestart() {
        try {
            Files.writeString(restartMarker, "");
        } catch (IOException e) {
            LOGGER.warn("Cannot write {}", restartMarker, e);
            return false;
        }
        quit.run();
        return true;
    }

    private void scheduleNextLook() {
        startLook(true, false, this::show)
                .onFinished(this::scheduleNextLook)
                .scheduleWith(taskExecutor, CHECK_INTERVAL.toMinutes(), TimeUnit.MINUTES);
    }

    /// The task for one look, not started yet. Its answer reaches `onSuccess` only while it is the latest look
    /// that started running; a failure is logged. FX thread.
    private BackgroundTask<Look> startLook(boolean fetch, boolean announce, Consumer<Look> onSuccess) {
        AtomicLong thisLook = new AtomicLong();
        return BackgroundTask.wrap(() -> look(fetch, announce))
                             .onRunning(() -> thisLook.set(++looksStarted))
                             .onSuccess(look -> {
                                 if (thisLook.get() == looksStarted) {
                                     onSuccess.accept(look);
                                 }
                             })
                             .onFailure(e -> LOGGER.warn("Cannot look at the checkout", e));
    }

    /// Reads the checkout: the working tree's changelog, plus the upstream's once the checkout is behind, so an
    /// entry arriving upstream while a local edit is pending hides nothing. Without announced entries yet (the
    /// first run in a checkout) everything seen is announced now and nothing is news: a fresh checkout is not
    /// greeted with the whole changelog. With `announce`, everything seen is announced once the upstream was
    /// fetched — in this task, so no other look reads the announced entries in between. Any thread but FX.
    private Look look(boolean fetch, boolean announce) throws IOException {
        boolean fetched = fetch && checkout.fetch();
        int behind = checkout.commitsBehind();
        List<BlamedChangelog> changelogs = new ArrayList<>();
        checkout.blameWorkingTree().ifPresent(changelogs::add);
        if (behind > 0) {
            checkout.blameUpstream().ifPresent(changelogs::add);
        }
        Set<ChangelogEntry> seen = News.allEntries(changelogs);
        Optional<Set<ChangelogEntry>> announcedSoFar = announced.read();
        News news = announcedSoFar.map(old -> News.pending(old, changelogs)).orElse(News.NONE);
        if (announcedSoFar.isEmpty() || (announce && fetched)) {
            announced.write(seen);
        }
        return new Look(fetched, behind, title(behind, news), news);
    }

    /// `What's new - 3 pending change(s) since <running commit> - now at <upstream commit>`, the count only with
    /// news, the commits only while behind.
    private String title(int behind, News news) {
        String title = news.isEmpty()
                       ? Localization.lang("What's new")
                       : Localization.lang("What's new - %0 pending change(s)", String.valueOf(news.size()));
        if (behind == 0) {
            return title;
        }
        Optional<String> head = checkout.describeHead();
        Optional<String> upstream = checkout.describeUpstream();
        if (head.isEmpty() || upstream.isEmpty()) {
            return title;
        }
        return title + " " + Localization.lang("since %0 - now at %1", head.get(), upstream.get());
    }

    /// FX thread.
    private void show(Look look) {
        pending.set(look.news());
        commitsBehind.set(look.commitsBehind());
        title.set(look.title());
    }

    private String tooltipText() {
        List<String> parts = new ArrayList<>();
        if (!pending.get().isEmpty()) {
            parts.add(title.get() + ":\n" + pending.get().asPlainText());
        }
        if (updateAvailable.get()) {
            parts.add(Localization.lang("A new version is available (%0 commit(s)) - restart to update.", String.valueOf(commitsBehind.get())));
        }
        return parts.isEmpty() ? "" : "\n\n" + String.join("\n\n", parts);
    }
}
