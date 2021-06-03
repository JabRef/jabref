package org.jabref.logic.remote.online;

import java.util.Map;

public class GetByIdGraphQLQuery extends GraphQLQuery {
    private static final String ID_GRAPH_QL_FIELD = "id";
    private static final String OPERATION_NAME = "id";

    private static final String QUERY = "query getDocumentById($id: ID!) {" +
            "getUserDocumentRaw(id: $id) {" +
            "  type" +
            "    citationKey" +
            "    fields { field, value }" +
            "  }" +
            "}";

    protected Map<String, String> variables;

    public GetByIdGraphQLQuery(String id) {
        super(OPERATION_NAME, QUERY);
        variables.put(ID_GRAPH_QL_FIELD, id);
    }

    public String getId() {
        return variables.get(ID_GRAPH_QL_FIELD);
    }

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
