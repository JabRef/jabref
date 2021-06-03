package org.jabref.logic.remote.online.dto;

import java.util.HashMap;
import java.util.Map;

import org.jabref.logic.remote.online.dto.EntryDto;
import org.jabref.logic.remote.online.dto.GraphQLQuery;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GetByIdGraphQLQuery extends GraphQLQuery {
    private static final String ID_GRAPH_QL_FIELD = "id";
    private static final String OPERATION_NAME = "getDocumentById";

    private static final String QUERY = "query getDocumentById($id: ID!) {" +
            "getUserDocumentRaw(id: $id) {" +
            "  type" +
            "    citationKey" +
            "    fields { field, value }" +
            "  }" +
            "}";

    protected Map<String, String> variables = new HashMap<>();

    public GetByIdGraphQLQuery(String id) {
        super(OPERATION_NAME, QUERY);
        variables.put(ID_GRAPH_QL_FIELD, id);
    }

    @JsonIgnore
    public String getId() {
        return variables.get(ID_GRAPH_QL_FIELD);
    }

    @JsonIgnore
    public void setId(String id) {
        variables.put(ID_GRAPH_QL_FIELD, id);
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }

    public static class Variable {
        private EntryDto entryDto;

        public Variable(EntryDto entryDto) {
            this.entryDto = entryDto;
        }

        public EntryDto getEntryDto() {
            return entryDto;
        }

        public void setEntryDto(EntryDto entryDto) {
            this.entryDto = entryDto;
        }
    }
}
