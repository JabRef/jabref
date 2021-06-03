package org.jabref.logic.remote.online;

public class SaveEntryGraphQLQuery extends GraphQLQuery {
    private static final String SAVE_ENTRY_QUERY = "mutation addUserDocumentRaw($doc: DocumentRawInput!) {" +
            "  addUserDocumentRaw(document: $doc) {" +
            "    id" +
            "  }" +
            "}";

    private final Variable variable;

    public SaveEntryGraphQLQuery(EntryDto entryDto) {
        super("addUserDocumentRaw", SAVE_ENTRY_QUERY);
        this.variable = new Variable(entryDto);
    }

    public EntryDto getEntryDto() {
        return variable.getEntryDto();
    }

    public void setEntryDto(EntryDto entryDto) {
        variable.setEntryDto(entryDto);
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
