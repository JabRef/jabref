package org.jabref.jabrefonline;

import org.jabref.jabrefonline.type.AddPersonInput;

import com.apollographql.apollo3.ApolloClient;
import com.apollographql.apollo3.api.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddDocumentMut {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddDocumentMut.class);

    void update() {

        // First, create an `ApolloClient`
        // Replace the serverUrl with your GraphQL endpoint
        ApolloClient.Builder apolloClient = new ApolloClient.Builder()
                                                                      .serverUrl("https://jabref-dev.azurewebsites.net/api");

        var optionalPersonInoput = new Optional.Present<>(new AddPersonInput("JabRef"));

        /*
        // new AddJournalArticleInput() //DO I really need to provide all f*cking paramesters as new Optional(?)
        new AddJournalArticleInput(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)



        AddUserDocumentInput docInput = AddUserDocumentInput.builder()
                                           .journalArticle(article)
                                           .build();

        apolloClient.mutation(new AddUserDocumentMutation(docInput)).enqueue(new Callback() {

            @Override
            public void onResponse(Response<Optional<Data>> resp) {
                LOGGER.info("resp", resp);

            }

            @Override
            public void onFailure(ApolloException ex) {
                LOGGER.error("error", ex);

            }
        });
         s*/
    }

}
