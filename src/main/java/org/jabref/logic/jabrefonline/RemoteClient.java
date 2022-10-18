package org.jabref.logic.jabrefonline;

import java.util.Optional;
import java.util.function.Function;

import org.jabref.preferences.JabRefOnlinePreferences;

public class RemoteClient {

    private Optional<String> loggedInUser = Optional.empty();

    public RemoteClient(JabRefOnlinePreferences jabRefOnlinePreferences) {
    }

    public Optional<String> getUserId() {
        return loggedInUser;
    }

    public <U> U assertLoggedIn(Function<String, U> after) {
        return loggedInUser.map(after).orElseThrow(() -> new UnsupportedOperationException("Not logged in"));
    }
}
