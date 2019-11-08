package org.jabref.gui.entryeditor;

import javafx.scene.input.TransferMode;

public enum FileDragDropPreferenceType {
    COPY(TransferMode.COPY),
    LINK(TransferMode.LINK),
    MOVE(TransferMode.MOVE);

    private final TransferMode transferMode;

    /**
     * Initializes the enum with the mapping to JavaFX's TransferMode. We use this straight-forward implementation as
     * this class resides in `org.jabref.gui` and thus has access to JavaFX classes. The alternative is to use an
     * <a hreF="https://docs.oracle.com/javase/8/docs/api/java/util/EnumMap.html">EnumMap</a>.
     *
     * @param transferMode the JavaFX TransferMode which the enum corresponds to.
     */
    FileDragDropPreferenceType(TransferMode transferMode) {
        this.transferMode = transferMode;
    }

    public TransferMode getTransferMode() {
        return transferMode;
    }
}
