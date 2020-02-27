package org.jabref.model.util;

public class ResultingEmacsState {
    public final int caretPos;
    public final String text;


    public ResultingEmacsState(int pos, String text) {
        this.caretPos = pos;
        this.text = text;
    }
}
