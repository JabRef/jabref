package org.jabref.logic.ai.chatting.chathistory;

import java.nio.file.Path;

import org.jabref.logic.ai.chatting.chathistory.storages.MVStoreChatHistoryStorage;
import org.jabref.logic.util.NotificationService;

import static org.mockito.Mockito.mock;

class MVStoreChatHistoryStorageTest extends ChatHistoryStorageTest {
    @Override
    ChatHistoryStorage makeStorage(Path path) {
        return new MVStoreChatHistoryStorage(path, mock(NotificationService.class));
    }

    @Override
    void close(ChatHistoryStorage storage) {
        ((MVStoreChatHistoryStorage) storage).close();
    }
}
