package org.jabref.logic.ai;

import java.util.ServiceLoader;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.spi.AiServiceProvider;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class AiServiceFactory {

    private AiServiceFactory() {
    }

    public static AiService createService(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            NotificationService notificationService,
            TaskExecutor taskExecutor) {
        return ServiceLoader.load(AiServiceProvider.class)
                            .findFirst()
                            .map(provider -> provider.createService(aiPreferences, filePreferences, notificationService, taskExecutor))
                            .orElseGet(NoOpAiService::new);
    }

    public static AiService createNoOpService() {
        return new NoOpAiService();
    }
}
