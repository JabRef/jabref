package org.jabref.jabrefonline;

import org.jabref.jabrefonline.GetDocumentByIdQuery.Data;

import com.apollographql.apollo3.ApolloCall;
import com.apollographql.apollo3.ApolloClient;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.rx3.Rx3Apollo;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetDocumentByIdQ {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetDocumentByIdQ.class);

    void start() {

        // First, create an `ApolloClient`
        // Replace the serverUrl with your GraphQL endpoint
        ApolloClient apolloClient = ApolloClient.builder()
                                                .serverUrl("https://your.domain/graphql/endpoint")
                                                .build();

        // Then enqueue your query
        ApolloCall<Data> queryCall = apolloClient.query(new GetDocumentByIdQuery("83"));

        Single<ApolloResponse<Data>> queryResponse = Rx3Apollo.single(queryCall);

        queryResponse.doOnSuccess(resp -> {
            LOGGER.debug("Response {}", resp.data.userDocument);
        });
    }

}
