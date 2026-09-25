package org.jabref.model.ojs;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;

/// Simple model object holding one submission as returned by the OJS 3.1+
/// `GET /api/v1/submissions` REST endpoint, reduced to the fields JabRef needs
/// for the submission tracker dashboard and the Entry Editor "OJS Tracking" tab.
///
/// This is a plain data holder; see [org.jabref.logic.ojs.OjsSubmissionParser]
/// for how instances are built from the API's JSON response.
@NullMarked
public record OjsSubmission(
        int id,
        String journalName,
        String title,
        Optional<String> doi,
        Optional<String> volume,
        Optional<OjsSubmissionStage> stage) {
}


