package org.jabref.logic.ai.chatting;

import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.util.List;

import dev.langchain4j.data.message.UserMessage;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
class JvmOpenAiChatLanguageModelTest {

    // [utest->req~ai.llms.base-url-redacted~1]
    @Test
    @Timeout(30)
    void connectionFailureNamesBaseUrlAndReason() {
        // ".invalid" never resolves (RFC 6761), so no server or port is involved
        String baseUrl = "http://user:secret@does-not-exist.invalid/v1/";
        JvmOpenAiChatLanguageModel model = new JvmOpenAiChatLanguageModel("key", "model", 0.7, baseUrl, HttpClient.newHttpClient());

        UncheckedIOException exception = assertThrows(UncheckedIOException.class, () -> model.chat(List.of(UserMessage.from("hi"))));

        assertEquals("Could not connect to http://does-not-exist.invalid/v1/.\n\nUnresolvedAddressException", exception.getMessage());
    }
}
