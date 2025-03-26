package org.jabref.logic.service;

/**
 * Service interface for displaying notifications and error messages.
 * This abstraction allows the logic layer to request user notifications
 * without directly depending on the GUI layer, maintaining proper architectural layering.
 */
public interface NotificationService {
    /**
     * Display an informational notification to the user
     *
     * @param message The message to display
     */
    void notify(String message);
    
    /**
     * Display an error dialog with the specified message
     *
     * @param message The error message to display
     */
    void showErrorDialog(String message);
    
    /**
     * Display an error dialog with title and message
     *
     * @param title The dialog title
     * @param message The error message to display
     */
    void showErrorDialog(String title, String message);
}
