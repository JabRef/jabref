package org.jabref.model.ojs;

import java.util.Arrays;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;

/// Maps the numeric `stageId` returned by the OJS 3.1+ `submissions` REST endpoint
/// to a human-readable workflow status.
///
/// 1 = Submission, 2 = Internal Review, 3 = External (Peer) Review, 4 = Editing (Copyediting), 5 = Production.
/// Internal and external review are both surfaced as "Peer Review" here.
@NullMarked
public enum OjsSubmissionStage {
    SUBMISSION(1, "Submission"),
    PEER_REVIEW_INTERNAL(2, "Peer Review"),
    PEER_REVIEW_EXTERNAL(3, "Peer Review"),
    COPYEDITING(4, "Copyediting"),
    PRODUCTION(5, "Production");

    private final int stageId;
    private final String displayName;

    OjsSubmissionStage(int stageId, String displayName) {
        this.stageId = stageId;
        this.displayName = displayName;
    }

    public int getStageId() {
        return stageId;
    }

    public String getDisplayName() {
        return displayName;
    }

    /// Resolves a raw OJS `stageId` to a known stage.
    ///
    /// @param stageId the numeric stage id as returned by the OJS API
    /// @return the matching stage, or [Optional#empty] if the id is unknown (e.g. a future OJS version)
    public static Optional<OjsSubmissionStage> fromStageId(int stageId) {
        return Arrays.stream(values())
                     .filter(stage -> stage.stageId == stageId)
                     .findFirst();
    }
}
