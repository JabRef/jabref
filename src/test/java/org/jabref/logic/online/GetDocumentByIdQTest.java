package org.jabref.logic.online;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import org.jabref.jabrefonline.GetDocumentByIdQuery;
import org.jabref.jabrefonline.GetDocumentByIdQuery.Author;
import org.jabref.jabrefonline.GetDocumentByIdQuery.Data;
import org.jabref.jabrefonline.GetDocumentByIdQuery.OnPerson;
import org.jabref.jabrefonline.GetDocumentByIdQuery.UserDocument;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.runtime.java.ApolloCall;
import com.apollographql.apollo3.runtime.java.ApolloClient;
import com.apollographql.apollo3.rx3.java.Rx3Apollo;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetDocumentByIdQTest {

    @Test
    void start() {
        ApolloClient apolloClient = new ApolloClient.Builder()
                                                              .serverUrl("https://mango-pebble-0224c3803-dev.westeurope.1.azurestaticapps.net/api")
                                                              .build();
        ApolloCall<Data> queryCall = apolloClient.query(new GetDocumentByIdQuery("ckondtcaf000101mh7x9g4gia"));
        ApolloResponse<Data> response = Rx3Apollo.single(queryCall, BackpressureStrategy.BUFFER).blockingGet();

        assertEquals(response.data, new Data(new UserDocument(
                                                              "ckondtcaf000101mh7x9g4gia",
                                                              Collections.emptyList(),
                                                              ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.of("Z")),
                                                              ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("Z")),
                                                              "Cocoa and Cardiovascular Health",
                                                              null,
                                                              null,
                                                              "Epidemiological data demonstrate that regular dietary intake of plant-derived foods and beverages reduces the risk of coronary heart disease and stroke. Among many ingredients, cocoa might be an important mediator. Indeed, recent research demonstrates a beneficial effect of cocoa on blood pressure, insulin resistance, and vascular and platelet function. Although still debated, a range of potential mechanisms through which cocoa might exert its benefits on cardiovascular health have been proposed, including activation of nitric oxide and antioxidant and antiinflammatory effects. This review summarizes the available data on the cardiovascular effects of cocoa, outlines potential mechanisms involved in the response to cocoa, and highlights the potential clinical implications associated with its consumption. ( Circulation. 2009; 119: 1433-1441.)",
                                                              List.of(
                                                                      new Author("Person", new OnPerson("Corti, Roberto", "TODOCorti, Roberto"), null),
                                                                      new Author("Person", new OnPerson("Flammer, Andreas J.", "TODOFlammer, Andreas J."), null),
                                                                      new Author("Person", new OnPerson("Hollenberg, Norman K.", "TODOHollenberg, Norman K."), null),
                                                                      new Author("Person", new OnPerson("Luscher, Thomas F.", "TODOLuscher, Thomas F."), null)

                                                              ),
                                                              null,
                                                              Collections.emptyList(),
                                                              null,
                                                              List.of("cocoa", "endothelium", "hypertension", "platelets")
                                                            )));
    }
}
