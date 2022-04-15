package org.jabref.logic.bst;

public class VMException extends RuntimeException {
    private String statusCode;
    public VMException(String string) {
        super(string);
    }
    public VMException(String message, String statusCode){
        super(message);
        this.statusCode = statusCode;
    }

    public String getStatusCode(){
        return this.statusCode;
    }
}
