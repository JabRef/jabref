package org.jabref.logic.remote.online;

public class SaveEntryGraphQLQuery extends GraphQLQuery {
    private static final String SAVE_ENTRY_QUERY = "mutation addUserDocumentRaw($doc: DocumentRawInput!) {" +
            "  addUserDocumentRaw(document: $doc) {" +
            "    id" +
            "  }" +
            "}";

    private Variable variables;

    public SaveEntryGraphQLQuery(EntryDto entryDto) {
        super("addUserDocumentRaw", SAVE_ENTRY_QUERY);
        this.variables = new Variable(entryDto);
    }

    public static String getSaveEntryQuery() {
        return SAVE_ENTRY_QUERY;
    }

    public void setVariables(Variable variables) {
        this.variables = variables;
    }

    public Variable getVariables() {
        return variables;
    }

    public static class Variable {
        private EntryDto doc;

        public Variable(EntryDto doc) {
            this.doc = doc;
        }

        public EntryDto getDoc() {
            return doc;
        }

        public void setDoc(EntryDto doc) {
            this.doc = doc;
        }
    }
}
