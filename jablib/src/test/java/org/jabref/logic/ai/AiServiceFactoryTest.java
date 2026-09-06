package org.jabref.logic.ai;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@NullMarked
class AiServiceFactoryTest {

    @Test
    void createServiceReturnsAvailableServiceWhenProviderPresent() {
        AiPreferences aiPreferences = AiPreferences.getDefault();
        FilePreferences filePreferences = mock(FilePreferences.class);
        NotificationService notificationService = mock(NotificationService.class);
        TaskExecutor taskExecutor = mock(TaskExecutor.class);

        AiService aiService = AiServiceFactory.createService(
                aiPreferences,
                filePreferences,
                notificationService,
                taskExecutor
        );

        assertNotNull(aiService);
        assertTrue(aiService.isAvailable());
    }

    @Test
    void createNoOpServiceReturnsUnavailableService() {
        AiService aiService = AiServiceFactory.createNoOpService();

        assertNotNull(aiService);
        assertFalse(aiService.isAvailable());
    }
}
