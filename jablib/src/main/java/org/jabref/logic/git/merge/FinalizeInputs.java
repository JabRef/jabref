package org.jabref.logic.git.merge;

import org.eclipse.jgit.revwalk.RevCommit;

public record FinalizeInputs(RevCommit localHead, RevCommit remoteHead) {
}
