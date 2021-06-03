package org.jabref.logic.remote.online;

public class GraphQLResponseData <T>{
    private T response;

    public T getResponse() {
        return response;
    }

    public void setResponse(T response) {
        this.response = response;
    }
}
