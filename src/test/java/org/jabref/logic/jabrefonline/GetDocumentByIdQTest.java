package org.jabref.logic.jabrefonline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jabref.jabrefonline.GetDocumentByIdQuery;
import org.jabref.jabrefonline.GetDocumentByIdQuery.Data;
import org.jabref.jabrefonline.GetDocumentByIdQuery.UserDocument;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.runtime.java.ApolloCall;
import com.apollographql.apollo3.runtime.java.ApolloClient;
import com.apollographql.apollo3.rx3.java.Rx3Apollo;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import org.junit.jupiter.api.Test;

public class GetDocumentByIdQTest {

    @Test
    void start() {
        ApolloClient apolloClient = new ApolloClient.Builder()
                                                              .serverUrl("https://mango-pebble-0224c3803-dev.westeurope.1.azurestaticapps.net/api")
                                                              .build();
        ApolloCall<Data> queryCall = apolloClient.query(new GetDocumentByIdQuery("ckondtcaf000101mh7x9g4gia"));
        ApolloResponse<Data> response = Rx3Apollo.single(queryCall, BackpressureStrategy.BUFFER).blockingGet();

        assertEquals(response.data, new Data(new UserDocument(null, null, null, null, null, null, null, null, null, null, null, null, null)));
    }
}
