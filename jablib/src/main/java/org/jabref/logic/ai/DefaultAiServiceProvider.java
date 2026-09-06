package org.jabref.logic.ai;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.spi.AiServiceProvider;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class DefaultAiServiceProvider implements AiServiceProvider {

    @Override
    public AiService createService(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            NotificationService notificationService,
            TaskExecutor taskExecutor) {
        return new DefaultAiService(aiPreferences, filePreferences, notificationService, taskExecutor);
    }
}
