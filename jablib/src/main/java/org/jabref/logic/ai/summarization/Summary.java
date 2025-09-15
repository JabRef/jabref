package org.jabref.logic.ai.summarization;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.jabref.model.ai.AiProvider;

public record Summary(LocalDateTime timestamp, AiProvider aiProvider, String model, String content) implements Serializable {
}
