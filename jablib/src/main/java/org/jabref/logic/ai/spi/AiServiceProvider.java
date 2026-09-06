package org.jabref.logic.ai.spi;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;

import org.jspecify.annotations.NullMarked;

/// Service Provider Interface for discovering and instantiating [AiService].
@NullMarked
public interface AiServiceProvider {
    AiService createService(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            NotificationService notificationService,
            TaskExecutor taskExecutor
    );
}
