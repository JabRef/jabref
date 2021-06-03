package org.jabref.logic.remote.online.dto;

public class GraphQLResponseDto<T> {

    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
