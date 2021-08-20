package org.jabref.jabrefonline;

import java.util.List;
import java.util.Optional;

import org.jabref.jabrefonline.AddUserDocumentMutation.Data;
import org.jabref.jabrefonline.type.AddEntityInput;
import org.jabref.jabrefonline.type.AddJournalArticleInput;
import org.jabref.jabrefonline.type.AddPersonInput;
import org.jabref.jabrefonline.type.AddUserDocumentInput;

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
                                                .serverUrl("https://jabref-dev.azurewebsites.net/api")
                                                .build();

        var person = AddPersonInput.builder()
                                   .name("JabRef")
                                   .build();
        var authorField = AddEntityInput.builder().person(person)
                                        .build();

        var article = AddJournalArticleInput.builder()
                                            .title("JabRef is great!")
                                            .authors(List.of(authorField)).build();
        var docInput = AddUserDocumentInput.builder()
                                           .journalArticle(article)
                                           .build();

        apolloClient.mutate(new AddUserDocumentMutation(docInput)).enqueue(new Callback<Optional<Data>>() {

            @Override
            public void onResponse(Response<Optional<Data>> resp) {
                LOGGER.info("resp", resp);

            }

            @Override
            public void onFailure(ApolloException ex) {
                LOGGER.error("error", ex);

            }
        });

    }

}
