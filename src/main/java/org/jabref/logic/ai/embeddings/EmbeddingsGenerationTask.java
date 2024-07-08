package org.jabref.logic.ai.embeddings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import org.checkerframework.checker.units.qual.A;

/**
 * This class manages a queue of embedding generation tasks for one {@link BibEntry} in a {@link org.jabref.model.database.BibDatabase}.
 * <p>
 * {@link org.jabref.model.database.BibDatabase} will listen for entries updates and add them to the queue. This class
 * will start a background task for generating the embeddings for each entry in the queue.
 * <p>
 * This class also provides an ability to prioritize the entry in the queue. But it seems not to work well.
 */
public class EmbeddingsGenerationTask extends BackgroundTask<Void> {
    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final AiService aiService;
    private final TaskExecutor taskExecutor;

    private final AiIngestor aiIngestor;

    // We use an {@link ArrayList} as a queue to implement prioritization of the {@link LinkedFile}s as it provides more
    // methods to manipulate the collection.
    private final List<LinkedFile> linkedFileQueue = Collections.synchronizedList(new ArrayList<>());
    private int numOfProcessedFiles = 0;

    private final Object lock = new Object();
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private AtomicBoolean isBlockingNewTasks = new AtomicBoolean(false);

    public EmbeddingsGenerationTask(BibDatabaseContext databaseContext, FilePreferences filePreferences, AiService aiService, TaskExecutor taskExecutor) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
        this.aiService = aiService;
        this.taskExecutor = taskExecutor;

        this.aiIngestor = new AiIngestor(aiService);

        configure();

        setupListeningToPreferencesChanges();
    }

    private void configure() {
        showToUser(true);
        willBeRecoveredAutomatically(true);
        updateProgress(1, 1);
        titleProperty().set(Localization.lang("Embeddings generation"));
    }

    private void setupListeningToPreferencesChanges() {
        aiService.getPreferences().onEmbeddingsParametersChange(this::invalidate);
    }

    public void addToStore(Collection<BibEntry> bibEntries) {
        bibEntries.forEach(this::addToStore);
    }

    public void addToStore(BibEntry bibEntry) {
        addToStore(bibEntry.getFiles());
    }

    public void addToStore(List<LinkedFile> linkedFiles) {
        linkedFiles.forEach(this::addToStore);
    }

    public void addToStore(LinkedFile linkedFile) {
        if (!isBlockingNewTasks.get()) {
            linkedFileQueue.add(linkedFile);

            if (!isRunning.get()) {
                this.executeWith(taskExecutor);
                showToUser(false);
            }
        }
    }

    public AutoCloseable blockNewTasks() {
        isBlockingNewTasks.set(true);

        return () -> {
            isBlockingNewTasks.set(false);
        };
    }

    public void removeFromStore(Collection<BibEntry> bibEntries) {
        bibEntries.forEach(this::removeFromStore);
    }

    public void removeFromStore(BibEntry bibEntry) {
        removeFromStore(bibEntry.getFiles());
    }

    public void removeFromStore(List<LinkedFile> linkedFiles) {
        linkedFiles.forEach(this::removeFromStore);
    }

    public void removeFromStore(LinkedFile linkedFile) {
        aiService.getEmbeddingsManager().removeIngestedFile(linkedFile.getLink());
    }

    public void removeFromStore(Set<String> linksToRemove) {
        linksToRemove.forEach(this::removeFromStore);
    }

    public void removeFromStore(String link) {
        aiService.getEmbeddingsManager().removeIngestedFile(link);
    }

    public void updateEmbeddings(BibDatabaseContext bibDatabaseContext) {
        Set<String> linksToRemove = aiService.getEmbeddingsManager().getIngestedFilesTracker().getIngestedLinkedFiles();
        bibDatabaseContext.getEntries().stream()
                       .flatMap(entry -> entry.getFiles().stream())
                       .map(LinkedFile::getLink)
                       .forEach(linksToRemove::remove);

        removeFromStore(linksToRemove);

        addToStore(bibDatabaseContext.getEntries());
    }

    @Override
    protected Void call() throws Exception {
        isRunning.set(true);

        updateProgress();

        while (!linkedFileQueue.isEmpty() && !isCanceled()) {
            LinkedFile linkedFile = linkedFileQueue.removeLast();

            ingestLinkedFile(linkedFile);

            ++numOfProcessedFiles;
            updateProgress();
        }

        isRunning.set(false);

        return null;
    }

    private void ingestLinkedFile(LinkedFile linkedFile) {
        aiIngestor.ingestLinkedFile(linkedFile, databaseContext, filePreferences);
    }

    private void updateProgress() {
        updateMessage(Localization.lang("Generated embeddings %0 of %1 linked files", numOfProcessedFiles, numOfProcessedFiles + linkedFileQueue.size()));
        updateProgress(numOfProcessedFiles, numOfProcessedFiles + linkedFileQueue.size());
    }

    public void invalidate() {
        // TODO: Is this method right?
        // 1. How to stop if running.
        // 2. Is it okay to clear queue?
        // 3. Is it okay to 1) remove, 2) add to queue everything?

        linkedFileQueue.clear();
        databaseContext.getEntries().forEach(entry -> {
            removeFromStore(entry);
            addToStore(entry);
        });
    }

    public void moveToFront(Collection<LinkedFile> linkedFiles) {
        linkedFiles.forEach(this::moveToFront);
    }

    /**
     * This method is responsible for prioritizing a {@link LinkedFile} in the queue.
     * <p>
     * Queue is thought like a stack, so we just push the {@link LinkedFile} to the top of the stack.
     * <p>
     * It is not an error to add a {@link LinkedFile} if it is already in the queue or in process. In that case, it will be ignored by {@link AiIngestor}.
     */
    public void moveToFront(LinkedFile linkedFile) {
        linkedFileQueue.add(linkedFile);
    }

    public void updateDatabaseName(String name) {
        UiTaskExecutor.runInJavaFXThread(() -> this.titleProperty().set(Localization.lang("Generating embeddings for %0", name)));
    }

    public void shutdown() {
        linkedFileQueue.clear();
        // TODO: Stop the AiIngestor.
    }
}
