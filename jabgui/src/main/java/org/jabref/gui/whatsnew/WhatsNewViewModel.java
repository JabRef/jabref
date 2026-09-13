package org.jabref.gui.whatsnew;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
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
import org.jabref.logic.whatsnew.CheckoutNews;
import org.jabref.logic.whatsnew.CheckoutNews.Look;
import org.jabref.logic.whatsnew.CheckoutNews.Mode;
import org.jabref.logic.whatsnew.News;
import org.jabref.logic.whatsnew.RestartMarker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The state behind the "What's new" button: the news not announced yet, how far the checkout is behind its
/// upstream, and the title both the button and the window show. Schedules the looks of [CheckoutNews] and
/// projects what they find into properties.
///
/// A look runs in the background and lands here on the FX thread; every property is read and written on the
/// FX thread only. [CheckoutNews] runs looks one after the other, but not necessarily in the order they were
/// due: a click right after the start may take the checkout before the first look does. So every look is
/// numbered by the moment it is due, and an answer older than the one shown last is dropped.
// [impl->req~whats-new.checkout-news~1]
public class WhatsNewViewModel extends AbstractViewModel {

    /// What [#requestRestart()] achieved.
    public enum RestartRequest {
        /// The marker is in place and JabRef is closing.
        REQUESTED,
        /// The user kept JabRef open (a library to save first); the marker is withdrawn.
        DECLINED_BY_USER,
        /// The user kept JabRef open, but the marker could not be withdrawn: the next quit restarts JabRef.
        MARKER_NOT_WITHDRAWN,
        /// The marker could not be written; JabRef keeps running.
        MARKER_NOT_WRITTEN
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsNewViewModel.class);
    private static final Duration CHECK_INTERVAL = Duration.ofMinutes(5);

    private final CheckoutNews news;
    private final RestartMarker restartMarker;
    private final TaskExecutor taskExecutor;
    private final BooleanSupplier quit;

    private final ObjectProperty<News> pending = new SimpleObjectProperty<>(News.NONE);
    private final IntegerProperty commitsBehind = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty(Localization.lang("What's new"));
    private final BooleanBinding updateAvailable = commitsBehind.greaterThan(0);
    private final StringBinding tooltip = Bindings.createStringBinding(this::tooltipText, pending, commitsBehind, title);

    /// One look's answer with the number of the look. FX thread.
    private record Answer(long due, Look look) {
    }

    private long looksDue;
    private long lastShown;

    /// @param quit closes JabRef the ordinary way, so unsaved libraries are asked about; `false` when the user
    ///             keeps JabRef open
    public WhatsNewViewModel(CheckoutNews news, RestartMarker restartMarker, TaskExecutor taskExecutor, BooleanSupplier quit) {
        this.news = news;
        this.restartMarker = restartMarker;
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
        lookNow(Mode.WITHOUT_FETCH, this::show)
                .onFinished(this::scheduleNextLook)
                .executeWith(taskExecutor);
    }

    /// A look on demand: fetches, hands the news found to `onChecked` — or to `onFailed` when the upstream could
    /// not be reached, so no restart is offered — and only then makes them old: the tooltip drops them and the
    /// announced entries take them. Should the look itself fail, `onFailed` gets the news known so far and
    /// nothing is made old. Nothing happens once `windowOpen` says the window is gone: cancelling the returned
    /// task covers a look still running, this covers an answer already on its way.
    public BackgroundTask<?> present(BooleanSupplier windowOpen, Consumer<News> onChecked, Consumer<News> onFailed) {
        BackgroundTask<Answer> presentation = lookNow(Mode.WITH_FETCH, answer -> {
            if (!windowOpen.getAsBoolean()) {
                return;
            }
            show(answer);
            Look look = answer.look();
            if (look.fetched()) {
                onChecked.accept(look.news());
            } else {
                onFailed.accept(look.news());
            }
            pending.set(pending.get().without(look.seen()));
            announce(look);
        })
                .onFailure(e -> {
                    LOGGER.warn("Cannot look at the checkout", e);
                    if (windowOpen.getAsBoolean()) {
                        onFailed.accept(pending.get());
                    }
                });
        presentation.executeWith(taskExecutor);
        return presentation;
    }

    /// Leaves the marker for `just run-loop` and quits. A user who keeps JabRef open takes the marker back, so
    /// an ordinary quit later does not restart JabRef.
    public RestartRequest requestRestart() {
        if (!restartMarker.place()) {
            return RestartRequest.MARKER_NOT_WRITTEN;
        }
        if (quit.getAsBoolean()) {
            return RestartRequest.REQUESTED;
        }
        return restartMarker.withdraw() ? RestartRequest.DECLINED_BY_USER : RestartRequest.MARKER_NOT_WITHDRAWN;
    }

    /// The next periodic look; none once the executor is shut down, i.e. while JabRef quits.
    private void scheduleNextLook() {
        try {
            lookLater(Mode.WITH_FETCH, this::show)
                    .onFinished(this::scheduleNextLook)
                    .scheduleWith(taskExecutor, CHECK_INTERVAL.toMinutes(), TimeUnit.MINUTES);
        } catch (RejectedExecutionException e) {
            LOGGER.debug("No further look: the task executor is shut down", e);
        }
    }

    /// The task for a look due now, numbered as such; a failure is logged. FX thread.
    private BackgroundTask<Answer> lookNow(Mode mode, Consumer<Answer> onSuccess) {
        long due = ++looksDue;
        return BackgroundTask.wrap(() -> new Answer(due, news.look(mode)))
                             .onSuccess(onSuccess)
                             .onFailure(e -> LOGGER.warn("Cannot look at the checkout", e));
    }

    /// The task for a look due later: numbered when it starts, so the looks due meanwhile come first. FX thread.
    private BackgroundTask<Answer> lookLater(Mode mode, Consumer<Answer> onSuccess) {
        AtomicLong due = new AtomicLong();
        return BackgroundTask.wrap(() -> new Answer(due.get(), news.look(mode)))
                             .onRunning(() -> due.set(++looksDue))
                             .onSuccess(onSuccess)
                             .onFailure(e -> LOGGER.warn("Cannot look at the checkout", e));
    }

    /// Makes the news of `look` old, in the background. A look that ran meanwhile has read the old announced
    /// entries and shown them again, so the pending news drop them once more when the write is done. FX thread.
    private void announce(Look look) {
        BackgroundTask.wrap(() -> {
                          news.announce(look);
                          return look;
                      })
                      .onSuccess(announced -> pending.set(pending.get().without(announced.seen())))
                      .onFailure(e -> LOGGER.warn("Cannot remember the announced entries", e))
                      .executeWith(taskExecutor);
    }

    /// Shows the answer unless a look due later was shown already. FX thread.
    private void show(Answer answer) {
        if (answer.due() < lastShown) {
            return;
        }
        lastShown = answer.due();
        Look look = answer.look();
        pending.set(look.news());
        commitsBehind.set(look.commitsBehind());
        title.set(title(look));
    }

    /// `What's new - 3 pending change(s) since <running commit> - now at <upstream commit>`: the count only with
    /// news, the commits only while behind — each variant one sentence, so translators may order it.
    private static String title(Look look) {
        String count = String.valueOf(look.news().size());
        if (look.head().isEmpty() || look.upstream().isEmpty()) {
            return look.news().isEmpty()
                   ? Localization.lang("What's new")
                   : Localization.lang("What's new - %0 pending change(s)", count);
        }
        return look.news().isEmpty()
               ? Localization.lang("What's new since %0 - now at %1", look.head().get(), look.upstream().get())
               : Localization.lang("What's new - %0 pending change(s) since %1 - now at %2", count, look.head().get(), look.upstream().get());
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
