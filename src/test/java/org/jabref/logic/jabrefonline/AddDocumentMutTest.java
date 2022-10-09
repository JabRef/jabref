package org.jabref.logic.jabrefonline;

import com.apollographql.apollo3.runtime.java.ApolloClient;
import org.junit.jupiter.api.Test;

public class AddDocumentMutTest {

    @Test
    void update() {
        ApolloClient.Builder apolloClient = new ApolloClient.Builder()
                                                                      .serverUrl("https://jabref-dev.azurewebsites.net/api");

        //var optionalPersonInoput = new Optional.Present<>(new AddPersonInput("JabRef"));

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
