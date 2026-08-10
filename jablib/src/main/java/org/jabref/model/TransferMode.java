package org.jabref.model;

public enum TransferMode {
    COPY,
    MOVE,
    NONE;

    public static TransferMode from(javafx.scene.input.TransferMode javafxTransferMode) {
        if (javafxTransferMode == null) {
            return NONE;
        }
        switch (javafxTransferMode) {
            case COPY -> {
                return COPY;
            }
            case MOVE -> {
                return MOVE;
            }
            default ->
                    throw new IllegalStateException("Unexpected transfer mode: " + javafxTransferMode);
        }
    }
}

