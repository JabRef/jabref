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

import org.jabref.logic.journals.AbbreviationLoader;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ConferenceAbbreviationLoader extends AbbreviationLoader<ConferenceAbbreviationRepository> {
    public ConferenceAbbreviationLoader() {
        super("conferences", "conference-list.mv");
    }

    public static ConferenceAbbreviationRepository loadRepository(ConferenceAbbreviationPreferences preferences) {
        return new ConferenceAbbreviationLoader().loadRepositorySynchronously(preferences);
    }

    public static ConferenceAbbreviationRepository loadBuiltInRepository() {
        return loadRepository(ConferenceAbbreviationPreferences.getDefault());
    }

    @Override
    protected ConferenceAbbreviationRepository createRepository(Path file) {
        return new ConferenceAbbreviationRepository(file);
    }

    @Override
    protected ConferenceAbbreviationRepository createFallbackRepository() {
        return new ConferenceAbbreviationRepository();
    }
}
