package org.jabref.logic.jabrefonline;

import java.util.List;
import java.util.Optional;

import org.jabref.jabrefonline.UserChangesQuery;
import org.jabref.jabrefonline.UserChangesQuery.Node;

import com.apollographql.apollo3.runtime.java.ApolloClient;
import com.apollographql.apollo3.rx3.java.Rx3Apollo;
import io.reactivex.rxjava3.core.BackpressureStrategy;

public class JabRefOnlineService implements RemoteCommunicationService {

    private final ApolloClient apolloClient;

    public JabRefOnlineService() {
        apolloClient = new ApolloClient.Builder()
                                                 .serverUrl("https://mango-pebble-0224c3803-dev.westeurope.1.azurestaticapps.net/api")
                                                 .build();
    }

    @Override
    public UserChangesQuery.Changes getChanges(String clientId, Optional<SyncCheckpoint> since) {
        var queryCall = apolloClient.query(new UserChangesQuery("ckondtcaf000101mh7x9g4gia"));
        var response = Rx3Apollo.single(queryCall, BackpressureStrategy.BUFFER).blockingGet();
        return response.data.user.changes;
    }

    @Override
    public List<Node> updateEntries(String clientId, List<Node> entries) {
        // TODO Auto-generated method stub
        return entries;
    }

    @Override
    public List<Node> createEntries(String clientId, List<Node> entries) {
        // TODO Auto-generated method stub
        return entries;
    }
}
