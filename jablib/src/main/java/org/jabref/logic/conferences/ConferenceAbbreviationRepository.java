/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/
package org.jabref.logic.conferences;

import java.nio.file.Path;

import org.jabref.logic.journals.AbbreviationRepository;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ConferenceAbbreviationRepository extends AbbreviationRepository {
    public ConferenceAbbreviationRepository() {
    }

    public ConferenceAbbreviationRepository(Path conferenceList) {
        super(conferenceList);
    }
}
