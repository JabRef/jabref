package org.jabref.logic.whatsnew;

import java.util.Comparator;

import org.jspecify.annotations.NullMarked;

/// Who wrote a changelog entry, seen from the developer running JabRef: somebody else by name, or me.
///
/// A sealed type instead of a name with two reserved values: a `switch` over it is checked for completeness by
/// the compiler, and no real contributor can collide with a marker string.
@NullMarked
public sealed interface Contributor {

    /// The order of the news: every other contributor as they first appear, then my entries pushed from another
    /// machine, then my entries in this checkout.
    Comparator<Contributor> DISPLAY_ORDER = Comparator.comparingInt(Contributor::displayRank);

    /// Somebody else, by the name recorded in git.
    record Other(String name) implements Contributor {
    }

    /// The developer running JabRef: an entry committed under the checkout's `user.email`, or not committed yet.
    enum Me implements Contributor {
        /// Written in this checkout.
        LOCAL,
        /// Pushed from another machine and only fetched so far.
        REMOTE
    }

    private static int displayRank(Contributor contributor) {
        return switch (contributor) {
            case Other _ ->
                    0;
            case Me.REMOTE ->
                    1;
            case Me.LOCAL ->
                    2;
        };
    }
}
