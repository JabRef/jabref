package org.jabref.logic.remote.online;

public class GraphQLResponseDto<T> {

    private GraphQLResponseData<T> data;

    public GraphQLResponseData<T> getData() {
        return data;
    }

    public void setData(GraphQLResponseData<T> data) {
        this.data = data;
    }
}
