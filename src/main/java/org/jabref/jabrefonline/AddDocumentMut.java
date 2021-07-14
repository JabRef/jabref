package org.jabref.jabrefonline;

import java.util.List;
import java.util.Optional;

import org.jabref.jabrefonline.AddUserDocumentMutation.Data;
import org.jabref.jabrefonline.type.DocumentRawInput;
import org.jabref.jabrefonline.type.FieldValueTupleInput;

import com.apollographql.apollo.ApolloCall.Callback;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddDocumentMut {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddDocumentMut.class);

    void update() {
        // First, create an `ApolloClient`
        // Replace the serverUrl with your GraphQL endpoint
        ApolloClient apolloClient = ApolloClient.builder()
                                                .serverUrl("https://your.domain/graphql/endpoint")
                                                .build();

        var authorField = FieldValueTupleInput.builder()
                                              .field("author")
                                              .value("JabRef")
                                              .build();

        var docInput = DocumentRawInput.builder()
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
        });
    }
}
