/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/
package org.jabref.logic.conferences;

import java.util.List;

import org.jabref.logic.journals.AbbreviationPreferences;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ConferenceAbbreviationPreferences extends AbbreviationPreferences {
    public ConferenceAbbreviationPreferences(List<String> externalLists) {
        super(externalLists);
    }

    public static ConferenceAbbreviationPreferences getDefault() {
        return new ConferenceAbbreviationPreferences(List.of());
    }
}
