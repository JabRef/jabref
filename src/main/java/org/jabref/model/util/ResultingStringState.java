package org.jabref.model.util;

public class ResultingStringState {
    public final int caretPos;
    public final String text;

    public ResultingStringState(int pos, String text) {
        this.caretPos = pos;
        this.text = text;
    }
}
