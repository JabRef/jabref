package org.jabref.logic.msc;

import java.io.Serial;
import java.io.Serializable;

public record MscCodeEntry(String code, String text, String description) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
}
