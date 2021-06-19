package org.jabref.jabrefonline.graphql;

import java.util.Optional;

import org.jabref.jabrefonline.GetDocumentByIdQuery;
import org.jabref.jabrefonline.GetDocumentByIdQuery.Data;

import com.apollographql.apollo.ApolloCall.Callback;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Testgraplql {

    private static final Logger LOGGER = LoggerFactory.getLogger(Testgraplql.class);

    void start() {

        // First, create an `ApolloClient`
        // Replace the serverUrl with your GraphQL endpoint
        ApolloClient apolloClient = ApolloClient.builder()
                                                .serverUrl("https://your.domain/graphql/endpoint")
                                                .build();

        // Then enqueue your query
        apolloClient.query(new GetDocumentByIdQuery("83")).enqueue(new Callback<Optional<Data>>() {

            @Override
            public void onResponse(Response<Optional<Data>> arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onFailure(ApolloException arg0) {
                // TODO Auto-generated method stub

            }
        });

    }

}
