package org.jabref.gui.whatsnew;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.whatsnew.AnnouncedEntries;
import org.jabref.logic.whatsnew.AttributedEntry;
import org.jabref.logic.whatsnew.BlamedChangelog;
import org.jabref.logic.whatsnew.ChangelogEntry;
import org.jabref.logic.whatsnew.Checkout;
import org.jabref.logic.whatsnew.Contributor;
import org.jabref.logic.whatsnew.News;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// [utest->req~whats-new.checkout-news~1]
class WhatsNewViewModelTest {

    private static final ChangelogEntry OLD = new ChangelogEntry("Unreleased", "Added", "An old entry.");
    private static final ChangelogEntry MINE = new ChangelogEntry("Unreleased", "Added", "An entry of mine.");
    private static final ChangelogEntry PUSHED = new ChangelogEntry("Unreleased", "Fixed", "An entry pushed from elsewhere.");

    /// Runs a task at once when executed, but only records it when scheduled: the test decides when the
    /// next periodic look happens.
    private static final class RecordingTaskExecutor extends CurrentThreadTaskExecutor {
        private final List<BackgroundTask<?>> scheduled = new ArrayList<>();

        @Override
        public <V> Future<?> schedule(BackgroundTask<V> task, long delay, TimeUnit unit) {
            scheduled.add(task);
            return CompletableFuture.completedFuture(null);
        }

        void runScheduled() {
            List<BackgroundTask<?>> due = List.copyOf(scheduled);
            scheduled.clear();
            due.forEach(this::execute);
        }
    }

    @TempDir
    Path gitDir;

    private final Checkout checkout = mock(Checkout.class);
    private final RecordingTaskExecutor taskExecutor = new RecordingTaskExecutor();
    private final AtomicReference<Boolean> quitRequested = new AtomicReference<>(false);
    private final AtomicReference<Boolean> quitAllowed = new AtomicReference<>(true);
    private WhatsNewViewModel viewModel;

    private static BlamedChangelog changelog(Contributor by, ChangelogEntry... entries) {
        List<BlamedChangelog.Line> lines = new ArrayList<>();
        for (ChangelogEntry entry : entries) {
            lines.add(new BlamedChangelog.Line("## [" + entry.section() + "]", by));
            lines.add(new BlamedChangelog.Line("### " + entry.heading(), by));
            lines.add(new BlamedChangelog.Line("- " + entry.text(), by));
        }
        return new BlamedChangelog(lines);
    }

    @BeforeEach
    void setUp() {
        when(checkout.fetch()).thenReturn(true);
        when(checkout.blameWorkingTree()).thenReturn(Optional.of(changelog(new Contributor.Other("Somebody"), OLD)));
        viewModel = new WhatsNewViewModel(checkout, gitDir, taskExecutor, () -> {
            quitRequested.set(true);
            return quitAllowed.get();
        });
    }

    private AnnouncedEntries announced() {
        return AnnouncedEntries.inGitDir(gitDir);
    }

    @Test
    void theFirstLookAnnouncesEverythingSilently() throws IOException {
        viewModel.startWatching();

        assertEquals(News.NONE, viewModel.getPending());
        assertEquals(Optional.of(Set.of(OLD)), announced().read());
        assertEquals("", viewModel.tooltipProperty().get());
    }

    @Test
    void aLaterLookShowsWhatWasNotAnnounced() throws IOException {
        announced().write(Set.of(OLD));
        when(checkout.blameWorkingTree()).thenReturn(Optional.of(changelog(Contributor.Me.LOCAL, OLD, MINE)));

        viewModel.startWatching();

        assertEquals(new News(List.of(new AttributedEntry(Contributor.Me.LOCAL, MINE))), viewModel.getPending());
        assertEquals("What's new - 1 pending change(s)", viewModel.titleProperty().get());
        assertEquals("\n\nWhat's new - 1 pending change(s):\nChanges by me\n• An entry of mine.", viewModel.tooltipProperty().get());
        assertFalse(viewModel.updateAvailableProperty().get());
    }

    @Test
    void aFirstLookWithoutAChangelogAnnouncesNothing() throws IOException {
        when(checkout.blameWorkingTree()).thenReturn(Optional.empty());

        viewModel.startWatching();

        assertEquals(News.NONE, viewModel.getPending());
        assertEquals(Optional.empty(), announced().read());
    }

    @Test
    void thePeriodicLookFetchesAndReportsTheUpstream() throws IOException {
        announced().write(Set.of(OLD));
        viewModel.startWatching();
        when(checkout.commitsBehind()).thenReturn(2);
        when(checkout.blameUpstream()).thenReturn(Optional.of(changelog(Contributor.Me.REMOTE, OLD, PUSHED)));
        when(checkout.describeHead()).thenReturn(Optional.of("1111111 (2026-09-13 10:00)"));
        when(checkout.describeUpstream()).thenReturn(Optional.of("2222222 (2026-09-13 11:00)"));

        taskExecutor.runScheduled();

        assertEquals(new News(List.of(new AttributedEntry(Contributor.Me.REMOTE, PUSHED))), viewModel.getPending());
        assertEquals("What's new - 1 pending change(s) since 1111111 (2026-09-13 10:00) - now at 2222222 (2026-09-13 11:00)", viewModel.titleProperty().get());
        assertTrue(viewModel.updateAvailableProperty().get());
        assertEquals("\n\nWhat's new - 1 pending change(s) since 1111111 (2026-09-13 10:00) - now at 2222222 (2026-09-13 11:00):\n"
                + "Changes by me (pushed from another machine)\n• An entry pushed from elsewhere.\n\n"
                + "A new version is available (2 commit(s)) - restart to update.", viewModel.tooltipProperty().get());
    }

    @Test
    void presentingTheNewsMakesThemOld() throws IOException {
        announced().write(Set.of(OLD));
        when(checkout.blameWorkingTree()).thenReturn(Optional.of(changelog(Contributor.Me.LOCAL, OLD, MINE)));
        AtomicReference<News> presented = new AtomicReference<>();

        viewModel.present(presented::set, _ -> fail("the look must not fail"));

        assertEquals(new News(List.of(new AttributedEntry(Contributor.Me.LOCAL, MINE))), presented.get());
        assertEquals(News.NONE, viewModel.getPending());
        assertEquals(Optional.of(Set.of(OLD, MINE)), announced().read());
    }

    @Test
    void anUnreachableUpstreamPresentsTheNewsButKeepsThemPending() throws IOException {
        announced().write(Set.of(OLD));
        when(checkout.fetch()).thenReturn(false);
        when(checkout.blameWorkingTree()).thenReturn(Optional.of(changelog(Contributor.Me.LOCAL, OLD, MINE)));
        AtomicReference<News> presented = new AtomicReference<>();

        viewModel.present(_ -> fail("the upstream was not reached"), presented::set);

        News mine = new News(List.of(new AttributedEntry(Contributor.Me.LOCAL, MINE)));
        assertEquals(mine, presented.get());
        assertEquals(mine, viewModel.getPending());
        assertEquals(Optional.of(Set.of(OLD)), announced().read());
    }

    @Test
    void aFailedLookPresentsTheNewsKnownSoFar() throws IOException {
        announced().write(Set.of(OLD));
        when(checkout.blameWorkingTree()).thenReturn(Optional.of(changelog(Contributor.Me.LOCAL, OLD, MINE)));
        viewModel.startWatching();
        News known = viewModel.getPending();
        Files.delete(gitDir.resolve("whats-new-announced.tsv"));
        Files.createDirectory(gitDir.resolve("whats-new-announced.tsv"));
        AtomicReference<News> presented = new AtomicReference<>();

        viewModel.present(_ -> fail("the look must fail"), presented::set);

        assertEquals(known, presented.get());
        assertEquals(known, viewModel.getPending());
    }

    @Test
    void aRestartLeavesTheMarkerAndQuits() {
        assertEquals(WhatsNewViewModel.RestartRequest.REQUESTED, viewModel.requestRestart());

        assertTrue(Files.exists(gitDir.resolve(WhatsNewViewModel.RESTART_MARKER)));
        assertTrue(quitRequested.get());
    }

    @Test
    void aRestartTheUserDeclinesTakesTheMarkerBack() {
        quitAllowed.set(false);

        assertEquals(WhatsNewViewModel.RestartRequest.DECLINED_BY_USER, viewModel.requestRestart());

        assertFalse(Files.exists(gitDir.resolve(WhatsNewViewModel.RESTART_MARKER)));
    }

    @Test
    void aRestartWhoseMarkerCannotBeWrittenKeepsJabRefRunning() throws IOException {
        Files.createDirectory(gitDir.resolve(WhatsNewViewModel.RESTART_MARKER));

        assertEquals(WhatsNewViewModel.RestartRequest.MARKER_NOT_WRITTEN, viewModel.requestRestart());

        assertFalse(quitRequested.get());
    }
}
