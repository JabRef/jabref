package org.jabref.gui.git;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Pulls remote changes for a library at the interval configured in the Git preferences.
///
/// A scheduler is only started for a library stored inside a Git repository. The "Regularly pull
/// remote changes" setting is read on every run, so switching it takes effect without reopening
/// the library, the interval is read once, when the scheduler starts.
@NullMarked
public class GitPullScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitPullScheduler.class);

    private static final Set<GitPullScheduler> RUNNING_INSTANCES = new HashSet<>();

    private final BibDatabaseContext bibDatabaseContext;
    private final ScheduledThreadPoolExecutor executor;
    private final BooleanSupplier hasUnsavedChanges;

    private GitPullScheduler(BibDatabaseContext bibDatabaseContext, GitAutoSync gitAutoSync, BooleanSupplier hasUnsavedChanges, int intervalInMinutes) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.hasUnsavedChanges = hasUnsavedChanges;
        this.executor = new ScheduledThreadPoolExecutor(1);
        this.executor.scheduleAtFixedRate(
                () -> pull(gitAutoSync),
                intervalInMinutes,
                intervalInMinutes,
                TimeUnit.MINUTES);
    }

    /// Runs one scheduled pull. Skips while the library has unsaved changes, because a merge would be
    /// applied underneath the user's edits. Exceptions are caught, because scheduleAtFixedRate
    /// silently cancels the schedule for the rest of the session if the task throws.
    private void pull(GitAutoSync gitAutoSync) {
        try {
            if (hasUnsavedChanges.getAsBoolean() || !bibDatabaseContext.getMetaData().isGitAutoPull()) {
                return;
            }
            bibDatabaseContext.getDatabasePath().ifPresent(path -> gitAutoSync.pull(path, bibDatabaseContext, hasUnsavedChanges));
        } catch (RuntimeException e) {
            LOGGER.warn("Scheduled Git pull failed", e);
        }
    }

    /// Starts the scheduler for the given library, if it is stored inside a Git repository and has no scheduler yet.
    ///
    /// This method is not thread-safe. The caller has to ensure that this method is not called in parallel.
    ///
    /// @param bibDatabaseContext Associated [BibDatabaseContext]
    public static void start(BibDatabaseContext bibDatabaseContext,
                             DialogService dialogService,
                             GuiPreferences preferences,
                             StateManager stateManager,
                             TaskExecutor taskExecutor,
                             GitHandlerRegistry gitHandlerRegistry,
                             BooleanSupplier hasUnsavedChanges) {

        Optional<Path> databasePath = bibDatabaseContext.getDatabasePath();
        if (databasePath.isEmpty() || GitHandler.findRepositoryRoot(databasePath.get()).isEmpty()) {
            return;
        }

        if (isRunning(bibDatabaseContext)) {
            return;
        }

        int intervalInMinutes = preferences.getGitPreferences().getPullIntervalInMinutes();
        if (intervalInMinutes <= 0) {
            LOGGER.warn("Ignoring non-positive Git pull interval {}", intervalInMinutes);
            return;
        }

        GitAutoSync gitAutoSync = new GitAutoSync(dialogService,
                gitHandlerRegistry,
                taskExecutor,
                preferences,
                stateManager);
        RUNNING_INSTANCES.add(new GitPullScheduler(bibDatabaseContext, gitAutoSync, hasUnsavedChanges, intervalInMinutes));
    }

    static boolean isRunning(BibDatabaseContext bibDatabaseContext) {
        return RUNNING_INSTANCES.stream().anyMatch(instance -> instance.bibDatabaseContext == bibDatabaseContext);
    }

    /// Shuts down the scheduler which is associated with the given [BibDatabaseContext].
    ///
    /// @param bibDatabaseContext Associated [BibDatabaseContext]
    public static void shutdown(BibDatabaseContext bibDatabaseContext) {
        RUNNING_INSTANCES.removeIf(instance -> {
            if (instance.bibDatabaseContext != bibDatabaseContext) {
                return false;
            }
            instance.executor.shutdown();
            return true;
        });
    }
}
