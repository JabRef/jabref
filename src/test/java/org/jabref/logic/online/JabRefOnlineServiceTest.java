package org.jabref.logic.online;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({SnapshotExtension.class})
public class JabRefOnlineServiceTest {

    private Expect expect;

    @Test
    void testCreateEntries() {

    }

    @Test
    void getChangesSinceCheckpoint() {
        var changes = new JabRefOnlineService().getChanges(
                                                           "ckn4oul7100004cv7y3t94n8j",
                                                           Optional.of(new SyncCheckpoint(ZonedDateTime.of(2022, 10, 1, 0, 0, 0, 0, ZoneId.of("Z")), "1")));
        expect.serializer("json").toMatchSnapshot(changes);
    }

    @Test
    void getAllChanges() {
        var changes = new JabRefOnlineService().getChanges(
                                                           "ckn4oul7100004cv7y3t94n8j",
                                                           Optional.empty());
        expect.serializer("json").toMatchSnapshot(changes);
    }

    @Test
    void testUpdateEntries() {

    }
}
