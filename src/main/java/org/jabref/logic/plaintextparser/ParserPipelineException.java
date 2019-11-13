package org.jabref.logic.plaintextparser;

import org.jabref.JabRefException;

public class ParserPipelineException extends JabRefException {

    public ParserPipelineException(String reason) {
        super(reason);
    }

}
