package org.jabref.logic.jabrefonline;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jabref.preferences.JabRefOnlinePreferences;

public class RemoteClient {

    private Optional<String> loggedInUser = Optional.empty();

    public RemoteClient(JabRefOnlinePreferences jabRefOnlinePreferences) {
        // TODO: Init from prefs
        loggedInUser = Optional.of("ckondtcaf000101mh7x9g4gia");
    }

    public Optional<String> getUserId() {
        return loggedInUser;
    }

    public <U> U assertLoggedIn(Function<String, ? extends U> after) {
        return loggedInUser.map(after).orElseThrow(() -> new UnsupportedOperationException("Not logged in"));
    }

    public void assertLoggedIn(Consumer<String> after) {
        loggedInUser.ifPresentOrElse(after, () -> new UnsupportedOperationException("Not logged in"));
    }
}
