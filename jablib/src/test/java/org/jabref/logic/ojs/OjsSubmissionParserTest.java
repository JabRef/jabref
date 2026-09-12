package org.jabref.logic.ojs;

import java.util.Optional;

import org.jabref.model.ojs.OjsSubmission;
import org.jabref.model.ojs.OjsSubmissionStage;

import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OjsSubmissionParserTest {

    @Test
    void parsesLocalizedTitleAndDoiFromCurrentPublication() {
        JSONObject submission = new JSONObject("""
                {
                  "id": 42,
                  "stageId": 3,
                  "currentPublicationId": 100,
                  "publications": [
                    {"id": 99, "title": {"en_US": "Old Draft Title"}},
                    {"id": 100, "title": {"en_US": "Final Paper Title"}, "doi": "10.1234/example.42"}
                  ]
                }
                """);

        OjsSubmission result = OjsSubmissionParser.parse(submission, "My Journal");

        assertEquals(42, result.id());
        assertEquals("My Journal", result.journalName());
        assertEquals("Final Paper Title", result.title());
        assertEquals(Optional.of("10.1234/example.42"), result.doi());
        assertEquals(Optional.of(OjsSubmissionStage.PEER_REVIEW_EXTERNAL), result.stage());
    }

    @Test
    void fallsBackToLastPublicationWhenCurrentPublicationIdMissing() {
        JSONObject submission = new JSONObject("""
                {
                  "id": 7,
                  "stageId": 1,
                  "publications": [
                    {"id": 1, "title": {"en_US": "Draft One"}
                    },
                    {"id": 2, "title": {"en_US": "Draft Two"}
                    }
                  ]
                }
                """);

        OjsSubmission result = OjsSubmissionParser.parse(submission, "My Journal");

        assertEquals("Draft Two", result.title());
        assertEquals(Optional.of(OjsSubmissionStage.SUBMISSION), result.stage());
    }

    @Test
    void handlesPlainStringTitleInsteadOfLocalizedObject() {
        JSONObject submission = new JSONObject("""
                {
                  "id": 7,
                  "stageId": 4,
                  "publications": [
                    {"id": 1, "title": "Plain Title"}
                  ]
                }
                """);

        OjsSubmission result = OjsSubmissionParser.parse(submission, "My Journal");

        assertEquals("Plain Title", result.title());
        assertEquals(Optional.of(OjsSubmissionStage.COPYEDITING), result.stage());
    }

    @Test
    void missingPublicationsYieldsEmptyTitleAndNoDoi() {
        JSONObject submission = new JSONObject("""
                {
                  "id": 5,
                  "stageId": 5
                }
                """);

        OjsSubmission result = OjsSubmissionParser.parse(submission, "My Journal");

        assertEquals("", result.title());
        assertEquals(Optional.empty(), result.doi());
    }
}
