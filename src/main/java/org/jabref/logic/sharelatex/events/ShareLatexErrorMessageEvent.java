package org.jabref.logic.sharelatex.events;

public class ShareLatexErrorMessageEvent {

    String errorMessage;

    public ShareLatexErrorMessageEvent(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
