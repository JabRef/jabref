package org.jabref.model.util;

public class ResultingStringState {
    public final int caretPosition;
    public final String text;

    public ResultingStringState(int caretPosition, String text) {
        this.caretPosition = caretPosition;
        this.text = text;
    }
}
