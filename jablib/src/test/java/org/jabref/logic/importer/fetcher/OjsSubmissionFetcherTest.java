package org.jabref.logic.importer.fetcher;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.ojs.OjsSubmission;
import org.jabref.model.ojs.OjsSubmissionStage;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OjsSubmissionFetcherTest {

    private static final String RESPONSE_BODY = """
            {
              "itemsMax": 1,
              "items": [
                {
                  "id": 42,
                  "stageId": 3,
                  "publications": [
                    {"id": 100, "title": {"en_US": "Final Paper Title"}, "doi": "10.1234/example.42"}
                  ]
                }
              ]
            }
            """;

    @Test
    void fetchSubmissionsSendsBearerTokenAndParsesResponse() throws Exception {
        AtomicReference<String> receivedAuthHeader = new AtomicReference<>();
        AtomicReference<String> receivedPath = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/index.php/myjournal/api/v1/submissions", exchange -> {
            try (exchange) {
                receivedAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
                receivedPath.set(exchange.getRequestURI().getPath());
                byte[] responseBody = RESPONSE_BODY.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBody.length);
                try (OutputStream body = exchange.getResponseBody()) {
                    body.write(responseBody);
                }
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            String journalUrl = "http://127.0.0.1:" + port + "/index.php/myjournal";

            OjsSubmissionFetcher fetcher = new OjsSubmissionFetcher();
            List<OjsSubmission> submissions = fetcher.fetchSubmissions(journalUrl, "s3cr3t-token");

            assertEquals("Bearer s3cr3t-token", receivedAuthHeader.get());
            assertEquals("/index.php/myjournal/api/v1/submissions", receivedPath.get());
            assertEquals(1, submissions.size());
            OjsSubmission submission = submissions.getFirst();
            assertEquals(42, submission.id());
            assertEquals("myjournal", submission.journalName());
            assertEquals("Final Paper Title", submission.title());
            assertEquals("10.1234/example.42", submission.doi().orElseThrow());
            assertEquals(OjsSubmissionStage.PEER_REVIEW_EXTERNAL, submission.stage().orElseThrow());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchSubmissionsReturnsEmptyListWhenNoItemsField() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/index.php/myjournal/api/v1/submissions", exchange -> {
            try (exchange) {
                byte[] responseBody = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBody.length);
                try (OutputStream body = exchange.getResponseBody()) {
                    body.write(responseBody);
                }
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            String journalUrl = "http://127.0.0.1:" + port + "/index.php/myjournal";

            OjsSubmissionFetcher fetcher = new OjsSubmissionFetcher();
            List<OjsSubmission> submissions = fetcher.fetchSubmissions(journalUrl, "s3cr3t-token");

            assertEquals(List.of(), submissions);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchSubmissionsFollowsPaginationUntilItemsMaxReached() throws Exception {
        // 150 submissions, 100 per page (PAGE_SIZE) -> two requests: offset=0, offset=100.
        List<AtomicReference<String>> receivedQueries = List.of(new AtomicReference<>(), new AtomicReference<>());
        AtomicInteger requestCount = new AtomicInteger(0);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/index.php/myjournal/api/v1/submissions", exchange -> {
            try (exchange) {
                int requestIndex = requestCount.getAndIncrement();
                if (requestIndex < receivedQueries.size()) {
                    receivedQueries.get(requestIndex).set(exchange.getRequestURI().getQuery());
                }
                int pageSize = requestIndex == 0 ? 100 : 50;
                StringBuilder items = new StringBuilder();
                for (int i = 0; i < pageSize; i++) {
                    if (i > 0) {
                        items.append(",");
                    }
                    int id = requestIndex * 100 + i;
                    items.append("{\"id\": ").append(id).append(", \"stageId\": 1, \"publications\": [{\"id\": 1, \"title\": {\"en_US\": \"T").append(id).append("\"}}]}");
                }
                String responseBody = "{\"itemsMax\": 150, \"items\": [" + items + "]}";
                byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream body = exchange.getResponseBody()) {
                    body.write(responseBytes);
                }
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            String journalUrl = "http://127.0.0.1:" + port + "/index.php/myjournal";

            OjsSubmissionFetcher fetcher = new OjsSubmissionFetcher();
            List<OjsSubmission> submissions = fetcher.fetchSubmissions(journalUrl, "s3cr3t-token");

            assertEquals(2, requestCount.get());
            assertEquals("count=100&offset=0", receivedQueries.getFirst().get());
            assertEquals("count=100&offset=100", receivedQueries.get(1).get());
            assertEquals(150, submissions.size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchSubmissionsThrowsFetcherExceptionOnUnparsableResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/index.php/myjournal/api/v1/submissions", exchange -> {
            try (exchange) {
                byte[] responseBody = "not json".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBody.length);
                try (OutputStream body = exchange.getResponseBody()) {
                    body.write(responseBody);
                }
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            String journalUrl = "http://127.0.0.1:" + port + "/index.php/myjournal";

            OjsSubmissionFetcher fetcher = new OjsSubmissionFetcher();
            assertThrows(FetcherException.class, () -> fetcher.fetchSubmissions(journalUrl, "s3cr3t-token"));
        } finally {
            server.stop(0);
        }
    }
}

