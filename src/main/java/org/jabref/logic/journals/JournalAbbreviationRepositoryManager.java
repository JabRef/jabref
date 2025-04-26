package org.jabref.logic.journals;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages caching of JournalAbbreviationRepository instances to improve performance.
 * This class uses a thread-safe approach with read-write locks to ensure 
 * concurrent access while minimizing repository rebuilds.
 */
public class JournalAbbreviationRepositoryManager {

    private static final JournalAbbreviationRepositoryManager INSTANCE = new JournalAbbreviationRepositoryManager();
    private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();

    private JournalAbbreviationRepository repository;
    private JournalAbbreviationPreferences lastUsedPreferences;

    /**
     * Private constructor for singleton pattern
     */
    private JournalAbbreviationRepositoryManager() {
        // Private constructor to prevent instantiation
    }

    /**
     * Returns the singleton instance of the repository manager
     *
     * @return The singleton instance
     */
    public static JournalAbbreviationRepositoryManager getInstance() {
        return INSTANCE;
    }

    /**
     * Gets a repository instance for the given preferences. This method implements
     * caching to avoid rebuilding the repository if preferences haven't changed.
     * Uses double-checked locking for thread safety and efficiency.
     *
     * @param preferences The journal abbreviation preferences
     * @return A repository instance configured with the given preferences
     */
    public JournalAbbreviationRepository getRepository(JournalAbbreviationPreferences preferences) {
        Objects.requireNonNull(preferences);
        
        LOCK.readLock().lock();
        try {
            if (repository != null && !preferencesChanged(preferences)) {
                return repository;
            }
        } finally {
            LOCK.readLock().unlock();
        }
        
        LOCK.writeLock().lock();
        try {
            if (repository != null && !preferencesChanged(preferences)) {
                return repository;
            }
            
            repository = JournalAbbreviationLoader.loadRepository(preferences);
            lastUsedPreferences = clonePreferences(preferences);
            return repository;
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    /**
     * Checks if the preferences have changed since the last repository build
     *
     * @param preferences The current preferences to check
     * @return true if preferences have changed, false otherwise
     */
    private boolean preferencesChanged(JournalAbbreviationPreferences preferences) {
        if (lastUsedPreferences == null) {
            return true;
        }
        
        if (lastUsedPreferences.shouldUseFJournalField() != preferences.shouldUseFJournalField()) {
            return true;
        }
        
        List<String> oldLists = lastUsedPreferences.getExternalJournalLists();
        List<String> newLists = preferences.getExternalJournalLists();
        
        if (oldLists.size() != newLists.size()) {
            return true;
        }
        
        for (int i = 0; i < oldLists.size(); i++) {
            if (!Objects.equals(oldLists.get(i), newLists.get(i))) {
                return true;
            }
        }
        
        Map<String, Boolean> oldEnabled = lastUsedPreferences.getEnabledExternalLists();
        Map<String, Boolean> newEnabled = preferences.getEnabledExternalLists();
        
        if (oldEnabled.size() != newEnabled.size()) {
            return true;
        }
        
        for (Map.Entry<String, Boolean> entry : newEnabled.entrySet()) {
            Boolean oldValue = oldEnabled.get(entry.getKey());
            if (!Objects.equals(oldValue, entry.getValue())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Creates a clone of the preferences for comparison
     *
     * @param preferences The preferences to clone
     * @return A new preferences instance with the same settings
     */
    private JournalAbbreviationPreferences clonePreferences(JournalAbbreviationPreferences preferences) {
        return new JournalAbbreviationPreferences(
                preferences.getExternalJournalLists(),
                preferences.shouldUseFJournalField(),
                preferences.getEnabledExternalLists()
        );
    }
    
    /**
     * For testing purposes only - clears the cached repository
     */
    public void clear() {
        LOCK.writeLock().lock();
        try {
            repository = null;
            lastUsedPreferences = null;
        } finally {
            LOCK.writeLock().unlock();
        }
    }
}
