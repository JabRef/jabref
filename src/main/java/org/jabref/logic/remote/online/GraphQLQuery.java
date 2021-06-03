package org.jabref.logic.remote.online;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public abstract class GraphQLQuery {

    protected final String operationName;
    protected final String query;
    protected Map<String, Object> variables;

    public GraphQLQuery(String operationName, String query) {
        this.operationName = operationName;
        this.query = query;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getQuery() {
        return query;
    }

    public String asJson() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "";
        }
    }
}
