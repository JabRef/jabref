package org.jabref.model.ai.chatting;

import java.io.Serializable;

/// Old record. Used only for migration.
public record ChatHistoryRecord(String className, String content) implements Serializable {
}
