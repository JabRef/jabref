package org.jabref.logic.search.indexing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.jabref.logic.search.PostgreServer;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.PostgreConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BibFieldsIndexerTest {

    private PostgreServer postgreServer;

    private final BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);

    @BeforeEach
    void setUp() {
        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');
        postgreServer = new PostgreServer();
    }

    @AfterEach
    void tearDown() {
        if (postgreServer != null) {
            postgreServer.shutdown();
        }
    }

    @Test
    void addToIndexIsIdempotentForSameEntry() throws Exception {
        BibDatabaseContext databaseContext = BibDatabaseContext.empty();
        Connection connection = postgreServer.getConnection();
        BibFieldsIndexer indexer = new BibFieldsIndexer(bibEntryPreferences, databaseContext, connection);

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.withCitationKey("https://doi.org/10.48550/arxiv.2405.02318");
        entry.withField(StandardField.TITLE, "Cool Paper");
        entry.withField(StandardField.AUTHOR, "Doe, John");
        entry.withField(StandardField.GROUPS, "Imported entries, Other");

        BackgroundTask<?> dummyTask = new BackgroundTask<>() {
            @Override
            public Object call() {
                return null;
            }
        };

        // Index the same entry twice - this used to throw due to a duplicate key on (entryid, field_name)
        indexer.addToIndex(List.of(entry), dummyTask);
        indexer.addToIndex(List.of(entry), dummyTask);

        // Verify that there's exactly one row for (entryid, 'citationkey')
        String tableRef = PostgreConstants.getMainTableSchemaReference(indexer.getTable());
        String sql = "SELECT COUNT(*) FROM " + tableRef + " WHERE \"" + PostgreConstants.ENTRY_ID + "\" = ? AND \"" + PostgreConstants.FIELD_NAME + "\" = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, entry.getId());
            ps.setString(2, "citationkey");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int count = rs.getInt(1);
                assertEquals(1, count);
            }
        }

        // Verify that there's exactly one row for (entryid, 'groups') in the main table as well
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, entry.getId());
            ps.setString(2, "groups");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int count = rs.getInt(1);
                assertEquals(1, count);
            }
        }

        // Cleanup resources gracefully
        indexer.closeAndWait();
    }
}
