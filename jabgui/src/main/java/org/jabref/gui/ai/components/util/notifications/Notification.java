package org.jabref.gui.ai.components.util.notifications;

/**
 * Record that is used to display errors and warnings in the AI chat. If you need global notifications,
 * see {@link org.jabref.gui.DialogService#notify(String)}.
 * <p>
 * This type is used to represent errors for: no files in {@link org.jabref.model.entry.BibEntry}, files are processing,
 * etc. This is made via notifications to support chat with groups: on one hand we need to be able to notify users
 * about possible problems with entries (because that will affect LLM output), but on the other hand the user would
 * like to chat with all available entries in the group, even if some of them are not valid.
 */
public record Notification(String title, String message) {
}
