package org.jabref.jabrefonline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddDocumentMut {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddDocumentMut.class);

    void update() {
 // TODO No idea on this mutation
        /*
        // First, create an `ApolloClient`
        // Replace the serverUrl with your GraphQL endpoint
        ApolloClient apolloClient = ApolloClient.builder()
                                                .serverUrl("https://your.domain/graphql/endpoint")
                                                .build();


        var authorField = FieldValueTupleInput.builder()
                                              .field("author")
                                              .value("JabRef")
                                              .build();

        var docInput = AddUserDocumentInput.builder()
            .journalArticle(null)
                                       .type("article")
                                       .citationKey("citeme")
                                       .fields(List.of(authorField))
                                       .build();

        apolloClient.mutate(new AddUserDocumentMutation(docInput)).enqueue(new Callback<Optional<Data>>() {

            @Override
            public void onResponse(Response<Optional<Data>> resp) {
                LOGGER.debug("{0}", resp);
            }

            @Override
            public void onFailure(ApolloException ex) {
                LOGGER.error("exception", ex);

            }
        });*/

    }

}
