package org.jabref.logic.git;

public class GitConflictException extends GitException {
    public GitConflictException(String message, String localizedMessage) {
        super(message, localizedMessage);
    }
}
