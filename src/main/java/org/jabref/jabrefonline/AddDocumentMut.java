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
        ApolloClient apolloClient = ApolloClient.builder()
                                                .serverUrl("https://jabref-dev.azurewebsites.net/api")
                                                .build();


        var optionalPersonInoput  = new Optional.Present<>(new AddPersonInput("JabRef"));

        /*
        new AddJournalArticleInput(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)

        var article = AddJournalArticleInput.builder()
                                            .title("JabRef is great!")
                                            .authors(List.of(authorField)).build();
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
